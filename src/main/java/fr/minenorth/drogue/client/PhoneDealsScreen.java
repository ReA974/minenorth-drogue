package fr.minenorth.drogue.client;

import com.mikasrevs.newphone.client.gui.PhoneModernText;
import com.mikasrevs.newphone.client.zphone.gui.AbstractZPhoneScreen;
import fr.minenorth.drogue.Customers;
import fr.minenorth.drogue.DrogueNet;
import fr.minenorth.drogue.Quality;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * Application « Deal » dessinée DANS le téléphone MRPhone (barre d'état, cadre, retour à l'accueil par la barre du bas ou Échap), aux couleurs
 * MineNorth : rendez-vous en cours en haut (client, marchandise, lieu, temps restant, Annuler), puis les messages des clients (produit, quantité,
 * qualité minimum, prix, fidélité) avec Accepter / ✕. N'affiche que ce que le serveur envoie et ne décide de rien. Coordonnées en unités du
 * téléphone (300 de large), converties par x() / y() / size(). Ne jamais référencer cette classe sans le téléphone : passer par {@link PhoneDeals}.
 */
public class PhoneDealsScreen extends AbstractZPhoneScreen {
    private static final int BG = 0xFF161048, CARD = 0xFF0E0A34, HOVER = 0xFF2E2480, CYAN = 0xFF20AAEB, PINK = 0xFFC83CF0, GREEN = 0xFF1E9E52,
            RED = 0xFFC8344E, TEXT = 0xFFCFE3FF, MUTED = 0xFF8FA8E0, WARN = 0xFFFFE066, OK = 0xFF6CFF9A, WHITE = 0xFFFFFFFF, DISABLED = 0xFF2A2468;
    private static final float L = 14, R = 286, CB = 580;

    private record Hit(float l, float t, float r, float b, Runnable action) {}

    private DrogueNet.PhoneData data;
    private float scroll, contentBottom, listTop;
    private int ticks;
    private final List<Hit> hits = new ArrayList<>();
    private float clipT = 0, clipB = 1000;

    public PhoneDealsScreen() { super(Component.literal("Deal")); }

    public void update(DrogueNet.PhoneData d) { this.data = d; }

    @Override
    public void tick() {
        if (++ticks % 100 == 0) send(3, 0);
    }

    private static void send(int action, int id) { DrogueNet.CHANNEL.sendToServer(new DrogueNet.PhoneAction(action, id)); }

    // ------------------------------------------------------------------ dessin (unités du téléphone)

    private void box(GuiGraphics g, float l, float t, float r, float b, float rad, int c) { drawRoundedRect(g, x(l), y(t), x(r), y(b), size(rad), c); }

    private void text(GuiGraphics g, String s, float tx, float ty, float sz, float maxW, int c) {
        PhoneModernText.drawColoredFitted(g, s, x(tx), y(ty), size(sz), size(maxW), c);
    }

    private void center(GuiGraphics g, String s, float cx, float ty, float sz, float maxW, int c) {
        PhoneModernText.drawCenteredColoredFitted(g, s, x(cx), y(ty), size(sz), size(maxW), c);
    }

    private void right(GuiGraphics g, String s, float rx, float ty, float sz, int c) {
        int w = PhoneModernText.width(s, size(sz));
        PhoneModernText.drawColoredFitted(g, s, x(rx) - w, y(ty), size(sz), w + 2, c);
    }

    private void icon(GuiGraphics g, String id, float l, float t) {
        var item = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(id));
        if (item == null) return;
        g.pose().pushPose();
        g.pose().translate(x(l), y(t), 0);
        float sc = size(26) / 16f;
        g.pose().scale(sc, sc, 1f);
        g.renderItem(new ItemStack(item), 0, 0);
        g.pose().popPose();
    }

    private static String itemName(String id) {
        var item = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(id));
        return item == null ? id : new ItemStack(item).getHoverName().getString();
    }

    private void button(GuiGraphics g, int mx, int my, float l, float t, float r, float b, String label, float sz, int color, Runnable act) {
        boolean visible = b > clipT && t < clipB;
        boolean hov = visible && inside(mx, my, l, Math.max(t, clipT), r, Math.min(b, clipB));
        box(g, l, t, r, b, Math.min(10, (b - t) / 2), hov ? lighten(color) : color);
        center(g, label, (l + r) / 2, t + (b - t - sz) / 2, sz, r - l - 6, WHITE);
        if (visible) hits.add(new Hit(l, Math.max(t, clipT), r, Math.min(b, clipB), act));
    }

    private static int lighten(int c) {
        int r = Math.min(255, ((c >> 16) & 255) + 35), gg = Math.min(255, ((c >> 8) & 255) + 35), b = Math.min(255, (c & 255) + 35);
        return 0xFF000000 | (r << 16) | (gg << 8) | b;
    }

    private static String time(int sec) { return (sec / 60) + ":" + String.format("%02d", sec % 60); }

    // ------------------------------------------------------------------ rendu

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        hits.clear();
        clipT = 0;
        clipB = 1000;
        renderBackground(g);
        g.fill(x(3), y(8), x(297), y(606), BG);
        drawStatusBar(g);
        drawText(g, "Deal", 22, 56, 20, WHITE, false);
        if (data != null) right(g, data.offers().size() + " message(s)", R, 64, 9, MUTED);
        float y = 92;
        if (data == null) {
            center(g, "Chargement…", 150, 300, 11, 260, MUTED);
        } else {
            var a = data.active();
            if (a != null) {
                box(g, L, y, R, y + 92, 14, PINK);
                box(g, L + 3, y, R, y + 92, 14, CARD);
                icon(g, a.item(), L + 12, y + 10);
                text(g, "Rendez-vous · " + a.name(), L + 46, y + 10, 11, R - L - 130, WHITE);
                right(g, Customers.euros(a.cents()), R - 12, y + 10, 11, OK);
                text(g, a.count() + " × " + itemName(a.item()) + " · min " + Quality.stars(a.minQuality()), L + 46, y + 27, 9, R - L - 60, TEXT);
                text(g, a.x() + "  " + a.y() + "  " + a.z(), L + 12, y + 50, 11, 160, CYAN);
                text(g, "reste " + time(a.secondsLeft()), L + 12, y + 70, 9, 120, MUTED);
                button(g, mx, my, R - 92, y + 62, R - 12, y + 84, "Annuler", 9, RED, () -> send(2, 0));
                y += 102;
            }
            text(g, a == null ? "Messages de clients" : "Messages (un rendez-vous à la fois)", L, y, 9.5f, R - L, MUTED);
            y += 16;
            listTop = y;
            clipT = y;
            clipB = CB;
            g.enableScissor(x(3), y(clipT), x(297), y(clipB));
            float cy = y - scroll;
            if (data.offers().isEmpty()) text(g, "Pas de nouveau message… Repasse plus tard.", L, cy + 6, 10, R - L, MUTED);
            for (var o : data.offers()) {
                box(g, L, cy, R, cy + 70, 14, CYAN);
                box(g, L + 3, cy, R, cy + 70, 14, CARD);
                icon(g, o.item(), L + 12, cy + 10);
                text(g, o.name(), L + 46, cy + 9, 11, 120, WHITE);
                if (o.loyalty() > 0) right(g, "♥".repeat(o.loyalty()), R - 12, cy + 26, 9, PINK);
                right(g, Customers.euros(o.cents()), R - 12, cy + 9, 11, OK);
                text(g, o.count() + " × " + itemName(o.item()), L + 46, cy + 26, 9, R - L - 120, TEXT);
                text(g, "min " + Quality.stars(o.minQuality()), L + 12, cy + 48, 10, 110, WARN);
                if (a == null) {
                    final int id = o.id();
                    button(g, mx, my, R - 130, cy + 42, R - 50, cy + 62, "Accepter", 9, GREEN, () -> send(0, id));
                    button(g, mx, my, R - 44, cy + 42, R - 12, cy + 62, "✕", 9, DISABLED, () -> send(1, id));
                }
                cy += 78;
            }
            contentBottom = cy + scroll;
            g.disableScissor();
            clipT = 0;
            clipB = 1000;
        }
        drawHomeIndicator(g);
        drawFrame(g);
    }

    // ------------------------------------------------------------------ souris

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            for (int i = hits.size() - 1; i >= 0; i--) {
                Hit h = hits.get(i);
                if (inside(mx, my, h.l, h.t, h.r, h.b)) {
                    h.action.run();
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        float max = Math.max(0, contentBottom - CB + 8);
        scroll = Math.max(0, Math.min(scroll - (float) delta * 24, max));
        return true;
    }
}
