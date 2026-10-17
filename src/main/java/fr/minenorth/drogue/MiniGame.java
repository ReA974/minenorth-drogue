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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mini-jeu de timing pour les tables (ensachage, roulage) : une barre dans la barre d'action avec une zone verte et un curseur qui va et vient ;
 * un clic droit sur la table arrête le curseur. Dans la zone = réussi. Plusieurs manches de suite, de plus en plus rapides.
 * Aucune interface à installer côté client : tout passe par le message de la barre d'action.
 */
@Mod.EventBusSubscriber(modid = MineNorthDrogue.MOD_ID)
public final class MiniGame {
    private MiniGame() {}

    /** Ce que fait une table à chaque manche et à la fin. */
    public interface Handler {
        /** Résultat d'une manche. Renvoie faux pour arrêter (plus de matière). */
        boolean round(ServerPlayer p, boolean success, int index);

        void end(ServerPlayer p, int success, int total);
    }

    private static final int CELLS = 22, PAUSE = 14, MAX_DISTANCE = 6;
    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    private static final class Session {
        final BlockPos table;
        final int total;
        final Handler handler;
        int done, ok, zone, half, sweep;
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

    public static void start(ServerPlayer p, BlockPos table, int rounds, Handler handler) {
        Session s = new Session(table, rounds, handler);
        SESSIONS.put(p.getUUID(), s);
        newRound(s, p.serverLevel().getGameTime() + 6);
        p.displayClientMessage(Component.literal("§eMini-jeu : clic droit sur la table quand le curseur est dans la zone verte !"), true);
    }

    private static void newRound(Session s, long startAt) {
        s.half = s.done < 3 ? 3 : 2;                                   // zone plus étroite à partir de la 4e manche
        s.zone = s.half + 1 + (int) (Math.abs(startAt * 7919L + s.done * 31L) % (CELLS - 2 * s.half - 2));
        s.sweep = Math.max(24, 44 - 3 * s.done);                        // ticks pour traverser la barre : lent, un peu plus rapide à chaque manche
        s.roundStart = startAt;
        s.nextAt = 0;
    }

    private static int cursor(Session s, long now) {
        long t = Math.max(0, now - s.roundStart);
        int phase = (int) (t % (2L * s.sweep));
        return phase < s.sweep ? phase * (CELLS - 1) / s.sweep : (2 * s.sweep - phase) * (CELLS - 1) / s.sweep;
    }

    /** Clic droit sur la table pendant le jeu : on arrête le curseur. */
    public static void stop(ServerPlayer p) {
        Session s = SESSIONS.get(p.getUUID());
        if (s == null) return;
        ServerLevel level = p.serverLevel();
        long now = level.getGameTime();
        if (s.nextAt != 0 || now < s.roundStart) return;                // pause entre deux manches
        boolean ok = Math.abs(cursor(s, now) - s.zone) <= s.half;
        if (ok) s.ok++;
        s.done++;
        level.playSound(null, p.blockPosition(), ok ? SoundEvents.EXPERIENCE_ORB_PICKUP : SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.7f, ok ? 1.3f : 0.9f);
        p.displayClientMessage(Component.literal(ok ? "§a§lParfait !" : "§c§lRaté…"), true);
        boolean more = s.handler.round(p, ok, s.done);
        if (!more || s.done >= s.total) {
            SESSIONS.remove(p.getUUID());
            s.handler.end(p, s.ok, s.done);
            return;
        }
        s.nextAt = now + PAUSE;
    }

    private static Component bar(Session s, long now) {
        int c = cursor(s, now);
        StringBuilder b = new StringBuilder("§7[");
        for (int i = 0; i < CELLS; i++) {
            boolean zone = Math.abs(i - s.zone) <= s.half;
            if (i == c) b.append(zone ? "§e§l▮" : "§f§l▮");
            else b.append(zone ? "§a▮" : "§8▮");
        }
        b.append("§7]  §fManche ").append(s.done + 1).append("/").append(s.total).append("  §7réussies : §a").append(s.ok);
        return Component.literal(b.toString());
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || SESSIONS.isEmpty() || e.getServer().getTickCount() % 2 != 0) return;
        for (Iterator<Map.Entry<UUID, Session>> it = SESSIONS.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Session> en = it.next();
            Session s = en.getValue();
            ServerPlayer p = e.getServer().getPlayerList().getPlayer(en.getKey());
            if (p == null) { it.remove(); continue; }
            if (p.distanceToSqr(s.table.getX() + 0.5, s.table.getY() + 0.5, s.table.getZ() + 0.5) > MAX_DISTANCE * MAX_DISTANCE) {
                it.remove();
                p.displayClientMessage(Component.literal("§7Vous vous êtes éloigné de la table : mini-jeu annulé."), true);
                s.handler.end(p, s.ok, s.done);
                continue;
            }
            long now = p.serverLevel().getGameTime();
            if (s.nextAt != 0) {
                if (now >= s.nextAt) newRound(s, now + 4);
                continue;
            }
            if (now >= s.roundStart) p.displayClientMessage(bar(s, now), true);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        SESSIONS.remove(e.getEntity().getUUID());
    }
}
