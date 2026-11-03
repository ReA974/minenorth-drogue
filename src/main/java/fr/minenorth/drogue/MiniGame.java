package fr.minenorth.drogue;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mini-jeu de timing des tables (ensachage, roulage), joué dans l'écran de la table : une barre avec une zone verte et un curseur qui va et vient ;
 * le bouton « Stop » (ou Espace) arrête le curseur. Dans la zone = réussi. Plusieurs manches de suite, de plus en plus rapides.
 * Comme le vol d'animaux : le curseur avance à l'horloge (millisecondes), pas aux ticks du serveur, donc il reste fluide même quand le serveur rame.
 * L'écran l'anime lui-même dès qu'il reçoit la manche (voir view), juge l'appui et envoie la position vue ; le serveur la revérifie.
 */
@Mod.EventBusSubscriber(modid = MineNorthDrogue.MOD_ID)
public final class MiniGame {
    private MiniGame() {}

    /** Ce que fait une table à chaque manche et à la fin. */
    public interface Handler {
        /** Résultat d'une manche. Renvoie faux pour arrêter (plus de matière). */
        boolean round(ServerPlayer p, boolean success, int index);

        /** Fin de partie ; renvoie le nombre d'objets fabriqués (affiché dans l'écran). */
        int end(ServerPlayer p, int success, int total);
    }

    /** Positions envoyées par l'écran : 0 à STEPS (curseur de 0 à 1). */
    public static final int STEPS = 100;
    /** Valeurs envoyées à l'écran : en cours, manches, faites, réussies, centre de zone ‰, largeur ‰, vitesse (traversées / s × 100), phase ‰,
     *  pause, dernier résultat, bilan (réussies, manches, fabriqués), table occupée par un autre joueur. */
    public static final int DATA = 14;
    private static final int MAX_DISTANCE = 6;
    private static final long FIRST_DELAY_MS = 700, PAUSE_MS = 650;
    /** Latence tolérée : la position vue par le joueur est retenue si le curseur du serveur y est passé dans cette fenêtre. */
    private static final long SEEN_MS = 1000;
    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();
    private static final Map<UUID, Summary> LAST = new ConcurrentHashMap<>();
    private static final Random RANDOM = new Random();

    private record Summary(BlockPos table, int ok, int total, int made) {}

    private static final class Session {
        final BlockPos table;
        final int total;
        final Handler handler;
        int done, ok, last;
        float center, width, speed, phase;
        long roundStart, nextAt;

        Session(BlockPos table, int total, Handler handler) {
            this.table = table;
            this.total = total;
            this.handler = handler;
        }
    }

    public static boolean active(UUID id) { return SESSIONS.containsKey(id); }

    /** Vrai si un mini-jeu est en cours sur cette table (case d'origine). */
    public static boolean activeAt(BlockPos table) {
        for (Session s : SESSIONS.values()) if (s.table.equals(table)) return true;
        return false;
    }

    /** Vrai si ce joueur joue sur cette table. */
    public static boolean playing(UUID id, BlockPos table) {
        Session s = SESSIONS.get(id);
        return s != null && s.table.equals(table);
    }

    public static void start(ServerPlayer p, BlockPos table, int rounds, Handler handler) {
        Session s = new Session(table, rounds, handler);
        SESSIONS.put(p.getUUID(), s);
        LAST.remove(p.getUUID());
        s.nextAt = System.currentTimeMillis() + FIRST_DELAY_MS;
    }

    private static void newRound(Session s, long now) {
        s.width = s.done < 3 ? 0.26f : s.done < 6 ? 0.2f : 0.16f;       // zone plus étroite au fil des manches
        s.center = s.width / 2 + 0.06f + RANDOM.nextFloat() * (1 - s.width - 0.12f);
        s.speed = Math.min(1.3f, 0.75f + 0.07f * s.done);               // traversées de la barre par seconde
        s.phase = RANDOM.nextFloat() * 2f;
        s.roundStart = now;
        s.nextAt = 0;
    }

    /** Position du curseur (0 à 1) après ms millisecondes : aller-retour régulier. Même calcul que l'écran. */
    public static float cursor(float speed, float phase, long ms) {
        double u = (ms / 1000.0 * speed + phase) % 2.0;
        return (float) (u < 1.0 ? u : 2.0 - u);
    }

    /** Bouton « Stop » sans position vue par le client : jugé sur l'horloge du serveur. */
    public static void stop(ServerPlayer p) { stop(p, -1); }

    /**
     * Bouton « Stop » : on arrête le curseur. seen = position (0 à STEPS) où l'écran du joueur montrait le curseur (-1 si inconnue). Elle est
     * retenue si le curseur du serveur y est passé récemment (latence) : le joueur est jugé sur ce qu'il voyait, sans pouvoir annoncer une
     * position inventée.
     */
    public static void stop(ServerPlayer p, int seen) {
        Session s = SESSIONS.get(p.getUUID());
        if (s == null || s.nextAt != 0) return;                         // pause entre deux manches
        long now = System.currentTimeMillis(), ms = now - s.roundStart;
        float pos = cursor(s.speed, s.phase, ms);
        if (seen >= 0) {
            float claimed = seen / (float) STEPS;
            for (long t = ms; t >= Math.max(0, ms - SEEN_MS); t -= 10) {
                if (Math.abs(cursor(s.speed, s.phase, t) - claimed) <= 0.025f) { pos = claimed; break; }
            }
        }
        boolean ok = Math.abs(pos - s.center) <= s.width / 2 + 0.01f;
        if (ok) s.ok++;
        s.done++;
        s.last = ok ? 1 : 2;
        ServerLevel level = p.serverLevel();
        level.playSound(null, p.blockPosition(), ok ? SoundEvents.EXPERIENCE_ORB_PICKUP : SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.7f, ok ? 1.3f : 0.9f);
        boolean more = s.handler.round(p, ok, s.done);
        if (!more || s.done >= s.total) {
            finish(p, s);
            return;
        }
        s.nextAt = now + PAUSE_MS;
    }

    private static void finish(ServerPlayer p, Session s) {
        SESSIONS.remove(p.getUUID());
        int made = s.handler.end(p, s.ok, s.done);
        LAST.put(p.getUUID(), new Summary(s.table, s.ok, s.done, made));
    }

    /** Écran fermé ou joueur parti : la partie s'arrête (les manches jouées restent acquises). */
    public static void cancel(ServerPlayer p) {
        Session s = SESSIONS.get(p.getUUID());
        if (s != null) finish(p, s);
    }

    /** Valeurs de l'écran de la table pour ce joueur (voir DATA). */
    public static int view(UUID id, BlockPos table, int i) {
        Session s = SESSIONS.get(id);
        boolean mine = s != null && s.table.equals(table);
        if (i == 13) return !mine && activeAt(table) ? 1 : 0;
        if (i >= 10) {
            Summary l = LAST.get(id);
            if (l == null || !l.table().equals(table)) return 0;
            return i == 10 ? l.ok() : i == 11 ? l.total() : l.made();
        }
        if (!mine) return 0;
        return switch (i) {
            case 0 -> 1;
            case 1 -> s.total;
            case 2 -> s.done;
            case 3 -> s.ok;
            case 4 -> Math.round(s.center * 1000);
            case 5 -> Math.round(s.width * 1000);
            case 6 -> Math.round(s.speed * 100);
            case 7 -> Math.round(s.phase * 1000);
            case 8 -> s.nextAt != 0 ? 1 : 0;
            case 9 -> s.last;
            default -> 0;
        };
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || SESSIONS.isEmpty()) return;
        long now = System.currentTimeMillis();
        for (Iterator<Map.Entry<UUID, Session>> it = SESSIONS.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Session> en = it.next();
            Session s = en.getValue();
            ServerPlayer p = e.getServer().getPlayerList().getPlayer(en.getKey());
            if (p == null) { it.remove(); continue; }
            if (p.distanceToSqr(s.table.getX() + 0.5, s.table.getY() + 0.5, s.table.getZ() + 0.5) > MAX_DISTANCE * MAX_DISTANCE) {
                p.displayClientMessage(Component.literal("§7Vous vous êtes éloigné de la table : mini-jeu arrêté."), true);
                finish(p, s);
                continue;
            }
            if (s.nextAt != 0 && now >= s.nextAt) newRound(s, now);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        SESSIONS.remove(e.getEntity().getUUID());
        LAST.remove(e.getEntity().getUUID());
    }
}
