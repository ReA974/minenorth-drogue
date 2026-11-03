package fr.minenorth.drogue.client;

import fr.minenorth.drogue.LabBE;
import fr.minenorth.drogue.ModRegistry;
import fr.minenorth.drogue.Stations;
import fr.minenorth.drogue.menu.LabMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import static fr.minenorth.drogue.client.DrogueStyle.*;

/** Laboratoire : plante + produit chimique à gauche, recette et progression au centre, produit à droite. */
@OnlyIn(Dist.CLIENT)
public class LabScreen extends DrogueScreen<LabMenu> {
    public LabScreen(LabMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, "Coca, éphédra, pavot, sassafras, ergot", new ItemStack(ModRegistry.LAB_ITEM.get()), 230);
    }

    private String name(ItemStack st) { return st.getHoverName().getString(); }

    @Override
    protected void drawContents(GuiGraphics g) {
        int x = leftPos, y = topPos;
        label(g, "Plante", x + 12, y + 53, TEXT);
        label(g, "Chimie", x + 12, y + 89, TEXT);
        label(g, "Produit", x + 210, y + 69, OK);

        int active = menu.data.get(1), pct = menu.data.get(0), ready = menu.data.get(3), max = menu.data.get(4);
        ItemStack plant = menu.items.getStackInSlot(LabBE.PLANT);
        LabBE.Recipe r = active > 0 ? LabBE.RECIPES.get(active - 1) : LabBE.recipeFor(plant.getItem());

        int cx = x + 52, cw = 148, top = y + 50;
        card(g, cx - 6, top, cw + 8, 82, active > 0 ? CYAN : DARK);
        if (r == null) {
            label(g, "Recettes (par lot)", cx, top + 6, WHITE);
            int ly = top + 18;
            for (LabBE.Recipe rec : LabBE.RECIPES) {
                String line = rec.needs().get(0).count() + " " + name(new ItemStack(rec.needs().get(0).item().get())) + " → " + name(new ItemStack(rec.result().get()));
                label(g, fit(font, line, cw - 4), cx, ly, MUTED);
                ly += 12;
            }
        } else {
            label(g, fit(font, name(new ItemStack(r.result().get())), plant.isEmpty() ? cw - 4 : 76), cx, top + 6, WHITE);
            label(g, fit(font, r.needs().get(0).count() + " plante + " + r.needs().get(1).count() + " chimique par lot", cw - 4), cx, top + 19, MUTED);
            if (!plant.isEmpty()) label(g, fit(font, "Qualité : " + fr.minenorth.drogue.Quality.stars(LabBE.outputQuality(fr.minenorth.drogue.Quality.get(plant), Math.max(1, ready))), cw - 4), cx + 80, top + 6, WARN);
            label(g, fit(font, "→ " + r.yield().get() + " par lot, jusqu'à " + max + " lots", cw - 4), cx, top + 30, MUTED);
        }
        if (active > 0) {
            progress(g, cx, top + 46, cw - 4, 6, pct / 100f, CYAN);
            label(g, fit(font, "Fabrication… " + pct + " % (" + menu.data.get(2) + " lot(s))", cw - 4), cx, top + 58, CYAN);
            return;
        }
        if (r == null) return;                                         // la liste des recettes prend toute la carte
        boolean full = Stations.room(menu.items, LabBE.OUT, LabBE.product(r, plant, Math.max(1, ready))) < r.yield().get();
        String status = ready > 0 ? ready + " lot(s) prêt(s)" + (ready >= 3 ? " · -1 étoile" : "") : full ? "Videz d'abord le produit" : "Il manque des ingrédients";
        label(g, fit(font, status, cw - 4), cx, top + 46, ready > 0 ? OK : ALERT);
        button(g, cx, top + 58, cw - 4, 18, "Lancer", GREEN, ready > 0, LabMenu.BTN_START,
                ready > 0 ? "Plus de lots = plus de produit, mais 50 % de temps en plus par lot et une étoile de moins à partir de 3 lots (risque d'incendie sans extracteur d'air)." : "Déposez la plante et le produit chimique.");
    }
}
