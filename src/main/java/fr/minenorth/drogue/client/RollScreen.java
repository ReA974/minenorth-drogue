package fr.minenorth.drogue.client;

import fr.minenorth.drogue.ModRegistry;
import fr.minenorth.drogue.RollBE;
import fr.minenorth.drogue.menu.RollMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import static fr.minenorth.drogue.client.DrogueStyle.*;

@OnlyIn(Dist.CLIENT)
public class RollScreen extends GameScreen<RollMenu> {
    public RollScreen(RollMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, "Joints ou buvards de LSD (1 papier par lot)", new ItemStack(ModRegistry.ROLL_ITEM.get()));
    }

    @Override
    protected void drawContents(GuiGraphics g) {
        label(g, "Feuilles / LSD", leftPos + 12, topPos + 51, TEXT);
        label(g, "Papier", leftPos + 12, topPos + 81, TEXT);
        label(g, "Prêts", leftPos + 212, topPos + 64, OK);
        super.drawContents(g);
    }

    @Override protected int rounds() { return RollBE.possibleRounds(menu.items); }

    @Override
    protected String missing() {
        if (menu.items.getStackInSlot(RollBE.PAPER).isEmpty()) return "Il manque du papier.";
        return "3 feuilles ou 1 fiole de LSD par lot.";
    }

    @Override protected String startLabel(int rounds) { return rounds + " lot(s) à rouler"; }

    @Override protected String madeLabel(int made) { return made + (RollBE.lsd(menu.items) ? " buvard(s)" : " joint(s)"); }
}
