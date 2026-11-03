package fr.minenorth.drogue.client;

import fr.minenorth.drogue.ModRegistry;
import fr.minenorth.drogue.PackBE;
import fr.minenorth.drogue.menu.PackMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import static fr.minenorth.drogue.client.DrogueStyle.*;

@OnlyIn(Dist.CLIENT)
public class PackScreen extends GameScreen<PackMenu> {
    public PackScreen(PackMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, "3 doses + 1 sachet vide = 1 sachet", new ItemStack(ModRegistry.PACK_ITEM.get()), 236);
    }

    @Override
    protected void drawContents(GuiGraphics g) {
        label(g, "Coke", leftPos + 32, topPos + 61, TEXT);
        label(g, "Meth", leftPos + 32, topPos + 83, TEXT);
        label(g, "Héro", leftPos + 32, topPos + 105, TEXT);
        label(g, "Sachets", leftPos + 32, topPos + 127, TEXT);
        label(g, "Prêts", leftPos + 212, topPos + 67, OK);
        super.drawContents(g);
    }

    @Override protected int rounds() { return PackBE.possibleRounds(menu.items); }

    @Override
    protected String missing() {
        if (menu.items.getStackInSlot(PackBE.BAGS).isEmpty()) return "Il manque des sachets vides.";
        return "3 doses du même produit / sachet.";
    }

    @Override
    protected String startLabel(int rounds) {
        int rs = PackBE.rawSlot(menu.items);
        return rounds + " sachet(s) de " + (rs == PackBE.POWDER ? "coke" : rs == PackBE.HEROIN ? "héroïne" : "meth");
    }

    @Override protected String madeLabel(int made) { return made + " sachet(s)"; }
}
