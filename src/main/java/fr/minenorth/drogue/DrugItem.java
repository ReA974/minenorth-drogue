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
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.function.Supplier;

/**
 * Une dose (joint, cocaïne, méthamphétamine) : on la consomme en maintenant clic droit ; effets positifs de la config, puis délai d'attente
 * avant de pouvoir en reprendre (c'est la limite d'équilibrage). La prise est notée pour le test salivaire du mod Police.
 */
public class DrugItem extends Item {
    private static final String POLICE_KEY = "minenorthpolice_drogues";

    private final int useTicks;
    private final UseAnim anim;
    private final Supplier<List<MobEffectInstance>> effects;
    private final Supplier<Integer> cooldownSeconds;
    private final String feeling;

    public DrugItem(int useTicks, UseAnim anim, Supplier<List<MobEffectInstance>> effects, Supplier<Integer> cooldownSeconds, String feeling) {
        super(new Item.Properties().stacksTo(16));
        this.useTicks = useTicks;
        this.anim = anim;
        this.effects = effects;
        this.cooldownSeconds = cooldownSeconds;
        this.feeling = feeling;
    }

    public static DrugItem joint() {
        return new DrugItem(40, UseAnim.EAT, () -> DrogueConfig.effects(DrogueConfig.JOINT_EFFECTS), () -> DrogueConfig.get(DrogueConfig.JOINT_COOLDOWN),
                "§aVous vous détendez…");
    }

    public static DrugItem cocaine() {
        return new DrugItem(20, UseAnim.BRUSH, () -> DrogueConfig.effects(DrogueConfig.COCAINE_EFFECTS), () -> DrogueConfig.get(DrogueConfig.COCAINE_COOLDOWN),
                "§fUn coup de fouet : vous vous sentez plus vif.");
    }

    public static DrugItem meth() {
        return new DrugItem(40, UseAnim.TOOT_HORN, () -> DrogueConfig.effects(DrogueConfig.METH_EFFECTS), () -> DrogueConfig.get(DrogueConfig.METH_COOLDOWN),
                "§bVotre corps s'emballe…");
    }

    @Override public int getUseDuration(ItemStack st) { return useTicks; }

    @Override public UseAnim getUseAnimation(ItemStack st) { return anim; }

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
            for (MobEffectInstance e : effects.get()) p.addEffect(new MobEffectInstance(e));
            int cd = cooldownSeconds.get();
            if (cd > 0) p.getCooldowns().addCooldown(this, cd * 20);
            p.displayClientMessage(Component.literal(feeling), true);
            if (level instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, p.getX(), p.getEyeY(), p.getZ(), 6, 0.2, 0.1, 0.2, 0.01);
            markForPolice(p, st);
            if (!p.getAbilities().instabuild) st.shrink(1);
        }
        return st;
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
