package fr.minenorth.drogue;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * Client au rendez-vous (application Plans du téléphone). Il attend sur place, regarde autour de lui, ne se laisse ni frapper ni pousser. Clic droit : le
 * dealer lui remet la marchandise (Customers.deliver). Il disparaît après l'échange, si le rendez-vous est annulé ou expiré, ou s'il n'est plus
 * attendu (redémarrage du serveur).
 */
public class CustomerEntity extends PathfinderMob {
    private static final EntityDataAccessor<Integer> SKIN = SynchedEntityData.defineId(CustomerEntity.class, EntityDataSerializers.INT);
    private UUID dealer;
    private int leaving = -1;

    public CustomerEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder attributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 20).add(Attributes.MOVEMENT_SPEED, 0.25);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(SKIN, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 10f, 1f));
        goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    public int skin() { return entityData.get(SKIN); }

    public void setup(UUID dealer, String name, int skin) {
        this.dealer = dealer;
        entityData.set(SKIN, skin);
        setCustomName(net.minecraft.network.chat.Component.literal(name));
        setCustomNameVisible(true);
    }

    public UUID dealer() { return dealer; }

    /** Repart après l'échange : disparaît au bout de 3 s. */
    public void leave() { if (leaving < 0) leaving = 60; }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (leaving >= 0 && --leaving <= 0) { discard(); return; }
        if (tickCount % 40 == 0 && !Customers.expects(this)) discard();
    }

    @Override
    protected InteractionResult mobInteract(Player p, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!level().isClientSide && p instanceof ServerPlayer sp) Customers.deliver(sp, this);
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override public boolean isInvulnerableTo(DamageSource source) { return !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY); }

    @Override public boolean isPushable() { return false; }

    @Override public boolean removeWhenFarAway(double d) { return false; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (dealer != null) tag.putUUID("dealer", dealer);
        tag.putInt("skin", skin());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("dealer")) dealer = tag.getUUID("dealer");
        entityData.set(SKIN, tag.getInt("skin"));
    }
}
