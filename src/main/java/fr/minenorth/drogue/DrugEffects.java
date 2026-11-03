package fr.minenorth.drogue;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Effets propres au mod. Les effets « défonce » ne font rien par eux-mêmes : ils signalent au client quel filtre visuel appliquer (voir
 * client/DrugVisuals). Overdose et manque sont gérés par Addiction.
 */
public final class DrugEffects {
    private DrugEffects() {}

    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, MineNorthDrogue.MOD_ID);

    public static final RegistryObject<MobEffect> HIGH = EFFECTS.register("high", () -> new Marker(MobEffectCategory.NEUTRAL, 0x6FBF3A));
    public static final RegistryObject<MobEffect> NOD = EFFECTS.register("nod", () -> new Marker(MobEffectCategory.NEUTRAL, 0x8A5A2E));
    public static final RegistryObject<MobEffect> ROLL = EFFECTS.register("roll", () -> new Marker(MobEffectCategory.NEUTRAL, 0xE85AD0));
    public static final RegistryObject<MobEffect> TRIP = EFFECTS.register("trip", () -> new Marker(MobEffectCategory.NEUTRAL, 0xF2D13A));
    public static final RegistryObject<MobEffect> SHROOMS = EFFECTS.register("shrooms", () -> new Marker(MobEffectCategory.NEUTRAL, 0x9B6BE0));
    public static final RegistryObject<MobEffect> OVERDOSE = EFFECTS.register("overdose", () -> new Marker(MobEffectCategory.HARMFUL, 0x8B0000));
    public static final RegistryObject<MobEffect> WITHDRAWAL = EFFECTS.register("withdrawal", () -> new Marker(MobEffectCategory.HARMFUL, 0x5A6A7A));

    /** Effet sans action propre (repère pour le client, icône dans l'inventaire). */
    public static class Marker extends MobEffect {
        public Marker(MobEffectCategory cat, int color) { super(cat, color); }
    }
}
