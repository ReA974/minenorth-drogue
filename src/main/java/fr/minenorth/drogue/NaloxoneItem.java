package fr.minenorth.drogue;

import fr.minenorth.api.MineNorth;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Spray nasal de naloxone : clic droit sur un joueur en overdose (ou sur soi) pour l'arrêter. Pour le SAMU, mais tout le monde peut en avoir. */
public class NaloxoneItem extends Item {
    public NaloxoneItem() { super(new Item.Properties().stacksTo(8)); }

    @Override
    public InteractionResult interactLivingEntity(ItemStack st, Player user, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof ServerPlayer patient) || !(user instanceof ServerPlayer rescuer)) return InteractionResult.sidedSuccess(user.level().isClientSide);
        treat(st, rescuer, patient);
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack st = player.getItemInHand(hand);
        if (player instanceof ServerPlayer sp) treat(st, sp, sp);
        return InteractionResultHolder.sidedSuccess(st, level.isClientSide);
    }

    private static void treat(ItemStack st, ServerPlayer rescuer, ServerPlayer patient) {
        if (!Addiction.overdosing(patient)) {
            rescuer.displayClientMessage(Component.literal("§7" + (rescuer == patient ? "Vous n'êtes" : MineNorth.displayName(patient) + " n'est") + " pas en overdose."), true);
            return;
        }
        Addiction.naloxone(patient);
        patient.level().playSound(null, patient.blockPosition(), SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 0.6f, 1.8f);
        if (rescuer != patient) rescuer.displayClientMessage(Component.literal("§aNaloxone administrée à " + MineNorth.displayName(patient) + "."), true);
        if (!rescuer.getAbilities().instabuild) st.shrink(1);
    }
}
