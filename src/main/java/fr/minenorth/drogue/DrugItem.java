package fr.minenorth.drogue;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.UseAnim;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Une dose (joint, cocaïne, méthamphétamine, héroïne, champignons, ecstasy, LSD) : on la consomme en maintenant clic droit ; effets positifs de la
 * config (durée selon la qualité, petits effets des additifs de coupe), filtre visuel (DrugEffects), puis délai d'attente avant de pouvoir en
 * reprendre. La prise compte pour la dépendance et le risque d'overdose (Addiction) et est notée pour le test salivaire du mod Police.
 */
public class DrugItem extends Item {
    private static final String POLICE_KEY = "minenorthpolice_drogues";

    /** Type de prise : fumer un joint, sniffer une ligne sur un miroir, se faire une piqûre. Chacun a sa propre animation. */
    public enum Kind { JOINT, COKE, METH, HEROIN, SHROOM, PILL, BLOTTER }

    private final Kind kind;
    private final int useTicks;
    private final Supplier<List<MobEffectInstance>> effects;
    private final Supplier<Integer> cooldownSeconds;
    private final String feeling;

    public DrugItem(Kind kind, int useTicks, Supplier<List<MobEffectInstance>> effects, Supplier<Integer> cooldownSeconds, String feeling) {
        super(new Item.Properties().stacksTo(16));
        this.kind = kind;
        this.useTicks = useTicks;
        this.effects = effects;
        this.cooldownSeconds = cooldownSeconds;
        this.feeling = feeling;
    }

    public static DrugItem joint() {
        return new DrugItem(Kind.JOINT, 64, () -> DrogueConfig.effects(DrogueConfig.JOINT_EFFECTS), () -> DrogueConfig.get(DrogueConfig.JOINT_COOLDOWN),
                "§aVous vous détendez…");
    }

    public static DrugItem cocaine() {
        return new DrugItem(Kind.COKE, 44, () -> DrogueConfig.effects(DrogueConfig.COCAINE_EFFECTS), () -> DrogueConfig.get(DrogueConfig.COCAINE_COOLDOWN),
                "§fUn coup de fouet : vous vous sentez plus vif.");
    }

    public static DrugItem meth() {
        return new DrugItem(Kind.METH, 44, () -> DrogueConfig.effects(DrogueConfig.METH_EFFECTS), () -> DrogueConfig.get(DrogueConfig.METH_COOLDOWN),
                "§bVotre corps s'emballe…");
    }

    public static DrugItem heroin() {
        return new DrugItem(Kind.HEROIN, 50, () -> DrogueConfig.effects(DrogueConfig.HEROIN_EFFECTS), () -> DrogueConfig.get(DrogueConfig.HEROIN_COOLDOWN),
                "§6Une chaleur lourde vous envahit…");
    }

    public static DrugItem mushroom() {
        return new DrugItem(Kind.SHROOM, 32, () -> DrogueConfig.effects(DrogueConfig.SHROOM_EFFECTS), () -> DrogueConfig.get(DrogueConfig.SHROOM_COOLDOWN),
                "§dLes couleurs se mettent à respirer…");
    }

    public static DrugItem ecstasy() {
        return new DrugItem(Kind.PILL, 24, () -> DrogueConfig.effects(DrogueConfig.ECSTASY_EFFECTS), () -> DrogueConfig.get(DrogueConfig.ECSTASY_COOLDOWN),
                "§dUne vague d'euphorie vous traverse…");
    }

    public static DrugItem lsd() {
        return new DrugItem(Kind.BLOTTER, 30, () -> DrogueConfig.effects(DrogueConfig.LSD_EFFECTS), () -> DrogueConfig.get(DrogueConfig.LSD_COOLDOWN),
                "§eLe monde commence à onduler…");
    }

    @Override public int getUseDuration(ItemStack st) { return useTicks; }

    /** Pas d'animation d'origine (manger, boire…) : poses et mouvements propres, voir DrugClient. */
    @Override public UseAnim getUseAnimation(ItemStack st) { return UseAnim.NONE; }

    public Kind kind() { return kind; }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(fr.minenorth.drogue.client.DrugClient.INSTANCE);
    }

    /** Bruitages et fumée pendant la prise. */
    @Override
    public void onUseTick(Level level, LivingEntity living, ItemStack st, int remaining) {
        if (level.isClientSide || !(level instanceof ServerLevel sl)) return;
        int used = useTicks - remaining;
        double mx = living.getX() + living.getLookAngle().x * 0.35, my = living.getEyeY() - 0.12, mz = living.getZ() + living.getLookAngle().z * 0.35;
        switch (kind) {
            case JOINT -> {
                if (used == 2) level.playSound(null, living.blockPosition(), SoundEvents.FLINTANDSTEEL_USE, SoundSource.PLAYERS, 0.5f, 1.4f);
                if (used > 10 && used % 8 == 0) {
                    sl.sendParticles(ParticleTypes.SMOKE, mx, my, mz, 2, 0.03, 0.03, 0.03, 0.005);
                    level.playSound(null, living.blockPosition(), SoundEvents.CAMPFIRE_CRACKLE, SoundSource.PLAYERS, 0.25f, 1.5f);
                }
            }
            case COKE -> {
                if (used == 14 || used == 28) level.playSound(null, living.blockPosition(), SoundEvents.FOX_SNIFF, SoundSource.PLAYERS, 0.7f, 1.6f);
                if (used == 16 || used == 30) sl.sendParticles(ParticleTypes.POOF, mx, my - 0.05, mz, 3, 0.02, 0.02, 0.02, 0.0);
            }
            case SHROOM, PILL -> {
                if (used % 8 == 4) level.playSound(null, living.blockPosition(), SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 0.5f, 1.2f);
            }
            case BLOTTER -> {
                if (used == 10) level.playSound(null, living.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.5f, 1.6f);
            }
            case METH, HEROIN -> {
                if (used == 20) level.playSound(null, living.blockPosition(), SoundEvents.BEE_STING, SoundSource.PLAYERS, 0.25f, 1.9f);
                if (used == 26) level.playSound(null, living.blockPosition(), SoundEvents.SLIME_SQUISH_SMALL, SoundSource.PLAYERS, 0.6f, 1.4f);
            }
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack st = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            if (!level.isClientSide) player.displayClientMessage(Component.literal("§7Votre corps n'est pas prêt pour une autre dose."), true);
            return InteractionResultHolder.fail(st);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(st);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack st, Level level, LivingEntity entity) {
        if (!level.isClientSide && entity instanceof ServerPlayer p) {
            int q = Quality.get(st);
            double f = Quality.effectFactor(q);
            for (MobEffectInstance e : effects.get()) p.addEffect(new MobEffectInstance(e.getEffect(), Math.max(20, (int) (e.getDuration() * f)), e.getAmplifier()));
            for (String cut : Quality.cuts(st)) {
                switch (cut) {
                    case "minecraft:sugar" -> p.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 200, 0));
                    case "minenorthdrogue:paracetamol" -> p.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION, 160, 0));
                    case "minenorthdrogue:caffeine" -> p.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.DIG_SPEED, 400, 0));
                    default -> {}
                }
            }
            net.minecraft.world.effect.MobEffect visual = visualOf(kind);
            if (visual != null && DrogueConfig.visuals()) p.addEffect(new MobEffectInstance(visual, (int) (DrogueConfig.get(DrogueConfig.VISUAL_SECONDS) * 20 * f), 0, false, false, true));
            int cd = cooldownSeconds.get();
            if (cd > 0) p.getCooldowns().addCooldown(this, cd * 20);
            p.displayClientMessage(Component.literal(feeling), true);
            if (level instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, p.getX(), p.getEyeY(), p.getZ(), 6, 0.2, 0.1, 0.2, 0.01);
            markForPolice(p, st);
            Addiction.dose(p, kind, q);
            if (!p.getAbilities().instabuild) st.shrink(1);
        }
        return st;
    }

    /** Filtre visuel de la défonce (voir client/DrugVisuals). Cocaïne et meth : pas de filtre (effets physiques seulement). */
    private static net.minecraft.world.effect.MobEffect visualOf(Kind k) {
        return switch (k) {
            case JOINT -> DrugEffects.HIGH.get();
            case HEROIN -> DrugEffects.NOD.get();
            case PILL -> DrugEffects.ROLL.get();
            case BLOTTER -> DrugEffects.TRIP.get();
            case SHROOM -> DrugEffects.SHROOMS.get();
            default -> null;
        };
    }

    /** Écrit la prise dans les données du joueur au format lu par le test salivaire du mod Police (objet -> {t: date, n: nom}). */
    private static void markForPolice(ServerPlayer p, ItemStack st) {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(st.getItem());
        if (key == null) return;
        CompoundTag root = p.getPersistentData();
        CompoundTag drugs = root.getCompound(POLICE_KEY);
        CompoundTag d = new CompoundTag();
        d.putLong("t", System.currentTimeMillis());
        d.putString("n", st.getHoverName().getString());
        drugs.put(key.toString(), d);
        root.put(POLICE_KEY, drugs);
    }
}
