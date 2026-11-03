package fr.minenorth.drogue.client;

import fr.minenorth.drogue.CutBE;
import fr.minenorth.drogue.ModRegistry;
import fr.minenorth.drogue.Quality;
import fr.minenorth.drogue.menu.CutMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import static fr.minenorth.drogue.client.DrogueStyle.*;

/** Table de coupe : poudre et additif à gauche, aperçu du résultat au centre, produit coupé à droite. */
@OnlyIn(Dist.CLIENT)
public class CutScreen extends DrogueScreen<CutMenu> {
    public CutScreen(CutMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, "2 doses + 1 additif = 3 doses (-1 étoile)", new ItemStack(ModRegistry.CUT_TABLE_ITEM.get()), 224);
    }

    @Override
    protected void drawContents(GuiGraphics g) {
        int x = leftPos, y = topPos;
        label(g, "Produit", x + 20, y + 51, TEXT);
        label(g, "Additif", x + 20, y + 87, TEXT);
        label(g, "Coupé", x + 198, y + 69, OK);

        int cx = x + 62, cw = 124, top = y + 50;
        ItemStack prod = menu.items.getStackInSlot(CutBE.PRODUCT), res = CutBE.result(menu.items);
        int n = CutBE.batches(menu.items);
        card(g, cx - 4, top, cw + 8, 82, n > 0 ? PINK : DARK);
        if (prod.isEmpty()) {
            label(g, "Additifs", cx, top + 6, WHITE);
            label(g, fit(font, "Bicarbonate : volume seul", cw), cx, top + 18, MUTED);
            label(g, fit(font, "Sucre : + vitesse", cw), cx, top + 28, MUTED);
            label(g, fit(font, "Paracétamol : + régénération", cw), cx, top + 38, MUTED);
            label(g, fit(font, "Caféine : + célérité", cw), cx, top + 48, MUTED);
        } else {
            int q = Quality.get(prod);
            label(g, fit(font, prod.getHoverName().getString(), cw), cx, top + 6, WHITE);
            label(g, "Avant : " + Quality.stars(q), cx, top + 19, WARN);
            label(g, "Après : " + Quality.stars(res.isEmpty() ? q : Quality.get(res)), cx, top + 30, res.isEmpty() ? MUTED : ALERT);
            String info = Quality.cuts(prod).size() >= CutBE.MAX_CUTS ? "Déjà coupé deux fois" : n > 0 ? "+" + n + " dose(s) en plus" : "Il manque un additif";
            label(g, fit(font, info, cw), cx, top + 43, n > 0 ? OK : ALERT);
        }
        button(g, cx, top + 60, cw, 18, n > 0 ? "Couper (" + n * CutBE.MADE + " doses)" : "Couper", PINK, n > 0, CutMenu.BTN_CUT,
                "Plus de doses à vendre, mais moins pures : les clients exigeants paieront moins.");
    }
}
