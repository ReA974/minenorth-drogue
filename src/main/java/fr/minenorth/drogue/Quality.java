package fr.minenorth.drogue;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Qualité du produit, de 1 à 5 étoiles, portée par l'objet (NBT « mn_q »). Elle naît à la récolte (lampe, LED, engrais), suit la chaîne
 * (labo, four, ensachage, roulage), baisse quand on coupe le produit (« mn_cut » : additifs ajoutés) et monte au séchage du cannabis.
 * Plus elle est haute, plus les effets durent et plus les clients paient. Un objet sans qualité (anciennes sauvegardes, créatif) vaut 3 étoiles.
 */
public final class Quality {
    private Quality() {}

    public static final String TAG = "mn_q", CUT = "mn_cut";
    public static final int MIN = 1, MAX = 5, DEFAULT = 3;
    private static final String[] NAMES = {"Médiocre", "Passable", "Correcte", "Excellente", "Pure"};

    public static int get(ItemStack st) {
        CompoundTag t = st.getTag();
        return t != null && t.contains(TAG) ? clamp(t.getInt(TAG)) : DEFAULT;
    }

    public static boolean has(ItemStack st) { return st.getTag() != null && st.getTag().contains(TAG); }

    public static ItemStack set(ItemStack st, int q) {
        st.getOrCreateTag().putInt(TAG, clamp(q));
        return st;
    }

    public static int clamp(int q) { return Math.max(MIN, Math.min(MAX, q)); }

    public static String name(int q) { return NAMES[clamp(q) - 1]; }

    public static String stars(int q) {
        q = clamp(q);
        return "★".repeat(q) + "☆".repeat(MAX - q);
    }

    /** Durée des effets : 3 étoiles = normale, 1 étoile = deux tiers, 5 étoiles = un tiers de plus. */
    public static double effectFactor(int q) { return 0.5 + clamp(q) / 6.0; }

    /** Prix payé par les clients : 3 étoiles = prix de base, de 60 % (1 étoile) à 140 % (5 étoiles). */
    public static double priceFactor(int q) { return 0.4 + 0.2 * clamp(q); }

    /** Additifs de coupe (identifiants d'objets). */
    public static List<String> cuts(ItemStack st) {
        List<String> out = new ArrayList<>();
        CompoundTag t = st.getTag();
        if (t != null) for (Tag e : t.getList(CUT, Tag.TAG_STRING)) out.add(e.getAsString());
        return out;
    }

    public static void addCut(ItemStack st, String additive) {
        ListTag l = st.getOrCreateTag().getList(CUT, Tag.TAG_STRING);
        l.add(StringTag.valueOf(additive));
        st.getOrCreateTag().put(CUT, l);
    }

    /** Nouvel objet qui reprend la qualité et la coupe de la matière (q + delta). */
    public static ItemStack from(ItemStack source, net.minecraft.world.item.Item item, int count, int delta) {
        ItemStack out = new ItemStack(item, count);
        set(out, get(source) + delta);
        for (String c : cuts(source)) addCut(out, c);
        return out;
    }

    /** Moyenne arrondie vers le bas de deux qualités (mélange). */
    public static int mix(int a, int b) { return clamp((a + b) / 2); }
}
