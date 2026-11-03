package fr.minenorth.drogue.client;

import fr.minenorth.drogue.ModRegistry;
import fr.minenorth.drogue.OvenBE;
import fr.minenorth.drogue.Stations;
import fr.minenorth.drogue.menu.OvenMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import static fr.minenorth.drogue.client.DrogueStyle.*;

/** Four de cuisson : pâte de coca à gauche, cuisson au centre (flammes), poudre à droite. */
@OnlyIn(Dist.CLIENT)
public class OvenScreen extends DrogueScreen<OvenMenu> {
    public OvenScreen(OvenMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, "Pâte de coca → poudre de cocaïne", new ItemStack(ModRegistry.OVEN_ITEM.get()), 220);
    }

    @Override
    protected void drawContents(GuiGraphics g) {
        int x = leftPos, y = topPos;
        label(g, "Pâte", x + 23, y + 62, TEXT);
        label(g, "Poudre", x + 196, y + 62, OK);

        int pct = menu.data.get(0), cooking = menu.data.get(1), max = menu.data.get(2);
        int cx = x + 66, cw = 116;
        card(g, cx - 4, y + 52, cw + 8, 70, cooking > 0 ? 0xFFFF8A1F : DARK);
        if (cooking > 0) {
            // flammes qui dansent sous la plaque
            long t = minecraft.level == null ? 0 : minecraft.level.getGameTime();
            for (int i = 0; i < 7; i++) {
                float k = (float) Math.sin((t + partial) * 0.45f + i * 1.7f) * 0.5f + 0.5f;
                int h = 4 + Math.round(k * 7), fx = cx + 8 + i * 13;
                rfill(g, fx, y + 70 - h, fx + 7, y + 70, 3, mix(0xFFFFD630, 0xFFFF5A1F, k));
            }
            progress(g, cx + 4, y + 76, cw - 8, 6, pct / 100f, 0xFFFF8A1F);
            label(g, "Cuisson… " + pct + " %", cx + 4, y + 86, WARN);
            label(g, cooking + " pâte(s) au four", cx + 4, y + 98, MUTED);
        } else {
            ItemStack paste = menu.items.getStackInSlot(OvenBE.PASTE);
            int n = Math.min(paste.getCount(), Stations.room(menu.items, OvenBE.POWDER, OvenBE.powderOf(paste)));
            label(g, "1 pâte = 1 poudre", cx + 4, y + 58, MUTED);
            label(g, "Jusqu'à " + max + " pâtes", cx + 4, y + 70, MUTED);
            String tip = paste.isEmpty() ? "Déposez de la pâte de coca (laboratoire)." : n <= 0 ? "Videz d'abord la poudre." : "";
            button(g, cx + 2, y + 96, cw - 4, 18, n > 0 ? "Cuire " + n + " pâte(s)" : "Cuire", 0xFFD9661F, n > 0, OvenMenu.BTN_START, tip);
        }
    }
}
