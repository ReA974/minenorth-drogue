package fr.minenorth.drogue;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

/** Listes d'objets du mod : ce qui sent (flairé par les chiens K9), ce qui porte une qualité, les additifs de coupe. */
public final class DrugItems {
    private DrugItems() {}

    /** Objets qui sentent la drogue (chemins d'objets du mod). */
    public static final Set<String> SMELLY = Set.of("cannabis_leaf", "dried_cannabis", "coca_leaf", "coca_paste", "cocaine_powder", "meth_crystals",
            "heroin_powder", "poppy_pod", "magic_mushroom", "mdma_crystals", "ephedra");

    /** Objet qui porte une qualité (étoiles dans l'infobulle). */
    public static boolean hasQuality(ItemStack st) {
        Item i = st.getItem();
        return i instanceof DrugItem || i == ModRegistry.CANNABIS_LEAF.get() || i == ModRegistry.DRIED_CANNABIS.get() || i == ModRegistry.COCA_LEAF.get()
                || i == ModRegistry.EPHEDRA.get() || i == ModRegistry.POPPY_POD.get() || i == ModRegistry.COCA_PASTE.get()
                || i == ModRegistry.COCAINE_POWDER.get() || i == ModRegistry.METH_CRYSTALS.get() || i == ModRegistry.HEROIN_POWDER.get()
                || i == ModRegistry.MDMA.get() || i == ModRegistry.LSD_LIQUID.get() || i == ModRegistry.ERGOT.get();
    }

    /** Poudre ou cristaux qu'on peut couper (table de coupe). */
    public static boolean cuttable(ItemStack st) {
        return st.is(ModRegistry.COCAINE_POWDER.get()) || st.is(ModRegistry.METH_CRYSTALS.get()) || st.is(ModRegistry.HEROIN_POWDER.get()) || st.is(ModRegistry.MDMA.get());
    }

    /** Additif de coupe : bicarbonate (volume seul), sucre (un peu de vitesse), paracétamol (un peu de régénération), caféine (un peu de célérité). */
    public static boolean additive(ItemStack st) {
        return st.is(ModRegistry.BAKING_SODA.get()) || st.is(net.minecraft.world.item.Items.SUGAR) || st.is(ModRegistry.PARACETAMOL.get()) || st.is(ModRegistry.CAFFEINE.get());
    }

    /** Nom court d'un additif (infobulle). */
    public static String additiveName(String id) {
        return switch (id) {
            case "minecraft:sugar" -> "sucre";
            case "minenorthdrogue:paracetamol" -> "paracétamol";
            case "minenorthdrogue:caffeine" -> "caféine";
            default -> "bicarbonate";
        };
    }

    /** Lignes d'infobulle : qualité (étoiles) et additifs de coupe. */
    public static void tooltip(ItemStack st, java.util.List<net.minecraft.network.chat.Component> tip) {
        if (!hasQuality(st)) return;
        int q = Quality.get(st);
        tip.add(net.minecraft.network.chat.Component.literal("§6" + Quality.stars(q) + " §7" + Quality.name(q)));
        var cuts = Quality.cuts(st);
        if (!cuts.isEmpty())
            tip.add(net.minecraft.network.chat.Component.literal("§8Coupé : " + String.join(", ", cuts.stream().map(DrugItems::additiveName).toList())));
    }
}
