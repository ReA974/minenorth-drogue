package fr.minenorth.drogue.client;

import fr.minenorth.drogue.MiniGame;
import fr.minenorth.drogue.menu.GameMenu;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

import static fr.minenorth.drogue.client.DrogueStyle.*;

/**
 * Table à mini-jeu : matière à gauche, mini-jeu au centre, produits à droite. Le curseur est animé ici à partir des données de la manche
 * (zone, vitesse, phase) et de l'horloge ; l'appui fige le curseur et envoie la position vue, le serveur la revérifie. Espace = Stop.
 */
@OnlyIn(Dist.CLIENT)
public abstract class GameScreen<T extends GameMenu> extends DrogueScreen<T> {
    protected GameScreen(T menu, Inventory inv, Component title, String subtitle, ItemStack icon) {
        this(menu, inv, title, subtitle, icon, 226);
    }

    protected GameScreen(T menu, Inventory inv, Component title, String subtitle, ItemStack icon, int height) {
        super(menu, inv, title, subtitle, icon, height);
    }

    /** Manches possibles avec ce qu'il y a sur la table (0 = impossible). */
    protected abstract int rounds();

    /** Raison affichée quand on ne peut pas lancer. */
    protected abstract String missing();

    protected abstract String startLabel(int rounds);

    protected abstract String madeLabel(int made);

    @Override
    protected void drawContents(GuiGraphics g) {
        int x = leftPos + 84, y = topPos + 50, w = 120, h = 78;
        boolean playing = menu.playing();
        card(g, x - 4, y, w + 8, h, playing ? CYAN : DARK);

        int total = menu.data.get(1), done = menu.data.get(2), ok = menu.data.get(3);
        if (menu.busy()) {
            label(g, "Table occupée", x + 4, y + 6, WARN);
            label(g, fit(font, "Un autre joueur l'utilise.", w - 8), x + 4, y + 20, MUTED);
        } else if (playing) {
            label(g, "Manche " + Math.min(total, done + 1) + "/" + total, x + 4, y + 6, WHITE);
            String s = "réussies " + ok;
            label(g, s, x + w - 4 - font.width(s), y + 6, OK);
        } else {
            int r = rounds();
            label(g, r > 0 ? startLabel(r) : "Prêt ?", x + 4, y + 6, r > 0 ? WHITE : MUTED);
            if (r <= 0 && menu.data.get(11) == 0) label(g, fit(font, missing(), w - 8), x + 4, y + 42, ALERT);
        }

        drawBar(g, x + 2, y + 22, w - 4, playing);

        // dernier résultat ou bilan
        int last = menu.data.get(9), sumTotal = menu.data.get(11);
        if (playing && menu.data.get(8) == 1 && done == 0) label(g, "Préparez-vous…", x + 4, y + 42, WARN);
        else if (playing && last != 0) label(g, last == 1 ? "Parfait !" : "Raté…", x + 4, y + 42, last == 1 ? OK : ALERT);
        else if (!playing && sumTotal > 0) {
            int sumOk = menu.data.get(10);
            label(g, fit(font, madeLabel(menu.data.get(12)) + " · " + sumOk + "/" + sumTotal, w - 8), x + 4, y + 42, sumOk == sumTotal && sumTotal >= 3 ? WARN : TEXT);
        }

        if (playing) button(g, x + 2, y + h - 22, w - 4, 18, "STOP  (Espace)", PINK, menu.data.get(8) == 0, GameMenu.BTN_STOP, "");
        else button(g, x + 2, y + h - 22, w - 4, 18, "Lancer", GREEN, rounds() > 0 && !menu.busy(), GameMenu.BTN_START,
                "Arrêtez le curseur dans la zone verte. Chaque manche va un peu plus vite.");
    }

    /** Manche animée en ce moment (-1 : aucune), heure de son début côté client, position figée après un appui (-1 : le curseur bouge). */
    private int shownRound = -1;
    private long roundStartMs;
    private float frozen = -1, shown = -1;
    private boolean localHit;
    private long flashUntil;

    private void stop() {
        if (!menu.playing() || menu.data.get(8) != 0 || frozen >= 0) return;
        if (shown < 0) { send(GameMenu.BTN_STOP); return; }
        frozen = shown;
        float center = menu.data.get(4) / 1000f, width = menu.data.get(5) / 1000f;
        localHit = Math.abs(frozen - center) <= width / 2;
        flashUntil = Util.getMillis() + 400;
        send(GameMenu.BTN_SEEN + Math.round(frozen * MiniGame.STEPS));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        // le bouton Stop envoie la position vue (voir stop) : intercepté avant le bouton générique
        int x = leftPos + 84, y = topPos + 50, w = 120, h = 78;
        if (menu.playing() && inside(mx, my, x + 2, y + h - 22, w - 4, 18)) {
            stop();
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    /** Barre du mini-jeu : piste, zone verte, curseur animé à l'horloge (comme le vol d'animaux, fluide même si le serveur rame). */
    private void drawBar(GuiGraphics g, int x, int y, int w, boolean playing) {
        long now = Util.getMillis();
        boolean waiting = menu.data.get(8) == 1;
        int round = playing && !waiting ? menu.data.get(2) : -1;
        if (round != shownRound) {                                      // nouvelle manche reçue : le chrono part maintenant
            shownRound = round;
            roundStartMs = now;
            frozen = -1;
        }
        rfill(g, x - 1, y - 1, x + w + 1, y + 13, 6, FRAME);
        rfill(g, x, y, x + w, y + 12, 5, DISABLED);
        shown = -1;
        if (!playing) return;
        float center = menu.data.get(4) / 1000f, width = menu.data.get(5) / 1000f;
        int zx1 = x + Math.round((center - width / 2) * w), zx2 = x + Math.round((center + width / 2) * w);
        rfill(g, zx1, y, zx2, y + 12, 4, GREEN);
        rfill(g, zx1 + 2, y + 1, zx2 - 2, y + 3, 1, mix(OK, GREEN, 0.5f));
        if (round < 0 && frozen < 0) return;
        float pos;
        if (frozen >= 0) pos = frozen;
        else {
            pos = MiniGame.cursor(menu.data.get(6) / 100f, menu.data.get(7) / 1000f, now - roundStartMs);
            shown = pos;
        }
        int cx = x + Math.round(pos * w);
        boolean in = Math.abs(pos - center) <= width / 2;
        int color = now < flashUntil ? (localHit ? OK : ALERT) : in ? WARN : WHITE;
        rfill(g, cx - 5, y - 6, cx + 5, y + 18, 4, mix(color, PANEL, 0.65f));
        rfill(g, cx - 2, y - 4, cx + 2, y + 16, 2, color);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_SPACE && menu.playing()) {
            stop();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }
}
