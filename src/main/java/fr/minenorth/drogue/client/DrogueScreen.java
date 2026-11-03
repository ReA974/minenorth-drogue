package fr.minenorth.drogue.client;

import fr.minenorth.drogue.menu.StationMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static fr.minenorth.drogue.client.DrogueStyle.*;

/**
 * Base des écrans du mod : panneau MineNorth, cadres des emplacements, boutons plats (envoyés au serveur comme boutons de menu vanilla),
 * infobulles. Les boutons sont redessinés à chaque image par drawContents.
 */
@OnlyIn(Dist.CLIENT)
public abstract class DrogueScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
    private record Btn(int x, int y, int w, int h, int id, boolean enabled, String tip) {}

    private final List<Btn> buttons = new ArrayList<>();
    private final String subtitle;
    private final ItemStack icon;
    protected float partial;
    protected int mouseX, mouseY;

    protected DrogueScreen(T menu, Inventory inv, Component title, String subtitle, ItemStack icon, int height) {
        super(menu, inv, title);
        this.subtitle = subtitle;
        this.icon = icon;
        this.imageWidth = StationMenu.WIDTH;
        this.imageHeight = height;
    }

    /** Contenu propre à l'écran (coordonnées absolues : leftPos / topPos). */
    protected abstract void drawContents(GuiGraphics g);

    /** Bouton plat ; id = numéro de bouton de menu envoyé au serveur (-1 = décoratif). */
    protected boolean button(GuiGraphics g, int x, int y, int w, int h, String label, int color, boolean enabled, int id, String tip) {
        boolean hov = inside(mouseX, mouseY, x, y, w, h);
        DrogueStyle.button(g, x, y, w, h, label, color, hov && enabled, enabled);
        buttons.add(new Btn(x, y, w, h, id, enabled, tip));
        return hov;
    }

    protected void send(int id) {
        Minecraft mc = Minecraft.getInstance();
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        if (mc.gameMode != null) mc.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    /** Nombre d'objets du joueur qui conviennent (sac + main secondaire). */
    protected int count(Predicate<ItemStack> f) {
        Inventory inv = Minecraft.getInstance().player.getInventory();
        int n = 0;
        for (int i = 0; i < 36; i++) if (f.test(inv.getItem(i))) n += inv.getItem(i).getCount();
        if (f.test(inv.offhand.get(0))) n += inv.offhand.get(0).getCount();
        return n;
    }

    protected void item(GuiGraphics g, ItemStack st, int x, int y, float scale) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1f);
        g.renderItem(st, 0, 0);
        g.pose().popPose();
    }

    /** Petit texte gris (étiquettes d'emplacement). */
    protected void label(GuiGraphics g, String s, int x, int y, int color) {
        g.drawString(font, s, x, y, color, false);
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        buttons.clear();
        partial = pt;
        mouseX = mx;
        mouseY = my;
        panel(g, leftPos, topPos, imageWidth, imageHeight, title.getString(), subtitle);
        if (!icon.isEmpty()) item(g, icon, leftPos + 10, topPos + 8, 1.5f);
        for (Slot s : menu.slots) {
            int x = leftPos + s.x - 1, y = topPos + s.y - 1;
            boolean out = s instanceof StationMenu.MachineSlot ms && ms.isOutput();
            if (out) rfill(g, x - 3, y - 3, x + 21, y + 21, 6, mix(OK, LIST, 0.6f));
            rfill(g, x, y, x + 18, y + 18, 4, s instanceof StationMenu.MachineSlot ? HOVER : LIST);
        }
        if (menu instanceof StationMenu sm) label(g, "Inventaire", leftPos + (imageWidth - 162) / 2, topPos + sm.invY - 11, MUTED);
        drawContents(g);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {}

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        super.render(g, mx, my, pt);
        renderTooltip(g, mx, my);
        for (Btn b : buttons) {
            if (b.tip() != null && !b.tip().isEmpty() && inside(mx, my, b.x(), b.y(), b.w(), b.h()))
                g.renderTooltip(font, font.split(Component.literal(b.tip()), 180), mx, my);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for (Btn b : buttons) {
            if (b.id() >= 0 && b.enabled() && inside(mx, my, b.x(), b.y(), b.w(), b.h())) {
                send(b.id());
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }
}
