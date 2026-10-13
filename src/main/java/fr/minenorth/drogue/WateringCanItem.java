package fr.minenorth.drogue;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Arrosoir infini. Quand on arrose, l'objet note la fin d'un petit « versage » (tag pour_end, en ticks de jeu) : le client choisit alors un des modèles
 * inclinés (prédicat minenorthdrogue:pour, voir ClientSetup) et l'arrosoir se penche pendant une seconde environ.
 */
public class WateringCanItem extends Item {
    /** Durée du versage, en ticks. */
    public static final int POUR_TICKS = 24;
    public static final String TAG = "pour_end";

    public WateringCanItem() { super(new Item.Properties().stacksTo(1)); }

    public static void pour(ItemStack stack, Level level) {
        stack.getOrCreateTag().putLong(TAG, level.getGameTime() + POUR_TICKS);
    }

    /** Le simple changement du tag de versage ne doit pas refaire l'animation de sortie de l'objet en main. */
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }
}
