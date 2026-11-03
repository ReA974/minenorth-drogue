package fr.minenorth.drogue.client;

import fr.minenorth.drogue.ModRegistry;
import fr.minenorth.drogue.PressBE;
import fr.minenorth.drogue.menu.PressMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import static fr.minenorth.drogue.client.DrogueStyle.*;

@OnlyIn(Dist.CLIENT)
public class PressScreen extends GameScreen<PressMenu> {
    public PressScreen(PressMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, "1 dose de MDMA = 2 pilules", new ItemStack(ModRegistry.PILL_PRESS_ITEM.get()));
    }

    @Override
    protected void drawContents(GuiGraphics g) {
        label(g, "MDMA", leftPos + 12, topPos + 69, TEXT);
        label(g, "Pilules", leftPos + 210, topPos + 64, OK);
        super.drawContents(g);
    }

    @Override protected int rounds() { return PressBE.possibleRounds(menu.items); }

    @Override
    protected String missing() { return menu.items.getStackInSlot(PressBE.MDMA).isEmpty() ? "Déposez du MDMA." : "Videz les pilules."; }

    @Override protected String startLabel(int rounds) { return rounds + " pressée(s)"; }

    @Override protected String madeLabel(int made) { return made + " pilule(s)"; }
}
