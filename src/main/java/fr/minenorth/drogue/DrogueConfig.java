package fr.minenorth.drogue;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * config/minenorth_drogue.toml (serveur dédié). Effets : lignes « effet;secondes;niveau » (niveau 0 = I). Que des effets positifs par défaut ;
 * la limite est le délai d'attente entre deux prises et le coût de production, pas un effet négatif.
 */
public final class DrogueConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> JOINT_EFFECTS, COCAINE_EFFECTS, METH_EFFECTS;
    public static final ForgeConfigSpec.IntValue JOINT_COOLDOWN, COCAINE_COOLDOWN, METH_COOLDOWN;
    public static final ForgeConfigSpec.IntValue PACK_SECONDS, COCAINE_LAB_SECONDS, METH_LAB_SECONDS, COCAINE_YIELD, METH_YIELD;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.comment("Table d'ensachage").push("ensachage");
        PACK_SECONDS = b.comment("Secondes pour faire un sachet (3 unités + 1 sachet vide)").defineInRange("secondesParSachet", 3, 1, 600);
        b.pop();
        b.comment("Marijuana (joint)").push("marijuana");
        JOINT_EFFECTS = b.comment("Effets : effet;secondes;niveau (0 = niveau I)")
                .defineList("effets", List.of("minecraft:regeneration;20;0"), o -> o instanceof String);
        JOINT_COOLDOWN = b.comment("Délai avant de pouvoir en reprendre (secondes)").defineInRange("delaiSecondes", 120, 0, 36000);
        b.pop();
        b.comment("Cocaïne").push("cocaine");
        COCAINE_EFFECTS = b.defineList("effets", List.of("minecraft:speed;60;0", "minecraft:haste;60;0"), o -> o instanceof String);
        COCAINE_COOLDOWN = b.defineInRange("delaiSecondes", 240, 0, 36000);
        COCAINE_LAB_SECONDS = b.comment("Durée de fabrication au laboratoire (4 feuilles de coca + 1 produit chimique)").defineInRange("laboSecondes", 30, 1, 3600);
        COCAINE_YIELD = b.comment("Unités de poudre produites par fabrication (3 unités + 1 sachet vide = 1 sachet)").defineInRange("rendement", 6, 1, 64);
        b.pop();
        b.comment("Méthamphétamine").push("meth");
        METH_EFFECTS = b.defineList("effets", List.of("minecraft:speed;45;1", "minecraft:haste;45;1", "minecraft:resistance;45;0"), o -> o instanceof String);
        METH_COOLDOWN = b.defineInRange("delaiSecondes", 360, 0, 36000);
        METH_LAB_SECONDS = b.comment("Durée de fabrication au laboratoire (3 éphédra + 1 produit chimique + 1 redstone)").defineInRange("laboSecondes", 45, 1, 3600);
        METH_YIELD = b.comment("Unités de cristaux produites par fabrication (3 unités + 1 sachet vide = 1 sachet)").defineInRange("rendement", 6, 1, 64);
        b.pop();
        SPEC = b.build();
    }

    private DrogueConfig() {}

    /** Lecture sûre : valeur par défaut si la config n'est pas chargée (client, menu principal). */
    public static int get(ForgeConfigSpec.IntValue v) {
        try { return v.get(); } catch (Exception e) { return v.getDefault(); }
    }

    public static List<MobEffectInstance> effects(ForgeConfigSpec.ConfigValue<List<? extends String>> v) {
        List<? extends String> lines;
        try { lines = v.get(); } catch (Exception e) { lines = v.getDefault(); }
        List<MobEffectInstance> out = new ArrayList<>();
        for (String line : lines) {
            String[] p = line.trim().split(";");
            ResourceLocation id = ResourceLocation.tryParse(p[0].trim());
            MobEffect eff = id == null ? null : ForgeRegistries.MOB_EFFECTS.getValue(id);
            if (eff == null) continue;
            try {
                int sec = p.length > 1 ? Integer.parseInt(p[1].trim()) : 30;
                int lvl = p.length > 2 ? Integer.parseInt(p[2].trim()) : 0;
                out.add(new MobEffectInstance(eff, Math.max(1, sec) * 20, Math.max(0, lvl)));
            } catch (NumberFormatException ignored) {}
        }
        return out;
    }
}
