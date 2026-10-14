package fr.minenorth.drogue;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemHandlerHelper;

/** Contenu du four : pâtes de coca déposées, cuisson en cours (1 pâte = 1 poudre), poudre prête à récupérer à main nue. */
public class OvenBE extends BlockEntity {
    private int paste, cooking, progress, powder;

    public OvenBE(BlockPos pos, BlockState state) {
        super(ModRegistry.OVEN_BE.get(), pos, state);
    }

    private static int max() { return Math.max(1, DrogueConfig.get(DrogueConfig.OVEN_MAX)); }

    private static int totalTicks() { return Math.max(1, DrogueConfig.get(DrogueConfig.OVEN_SECONDS)) * 20; }

    public void interact(Player p) {
        if (cooking > 0) {
            p.displayClientMessage(Component.literal("§7Cuisson en cours (" + cooking + " pâte(s))… " + Math.min(99, progress * 100 / totalTicks()) + " %"), true);
            return;
        }
        ItemStack hand = p.getMainHandItem();
        if (!hand.isEmpty() && hand.getItem() == ModRegistry.COCA_PASTE.get()) {
            int n = Math.min(hand.getCount(), max() - paste);
            if (n <= 0) { p.displayClientMessage(Component.literal("§cLe four est plein (" + max() + " pâtes maximum)."), true); return; }
            paste += n;
            hand.shrink(n);
            setChanged();
            level.playSound(null, worldPosition, SoundEvents.BUNDLE_INSERT, SoundSource.BLOCKS, 0.8f, 1.0f);
            p.displayClientMessage(Component.literal("§7Four : " + paste + " pâte(s) de coca — main vide pour lancer la cuisson"), true);
            return;
        }
        if (powder > 0) {
            ItemHandlerHelper.giveItemToPlayer(p, new ItemStack(ModRegistry.COCAINE_POWDER.get(), powder));
            powder = 0;
            setChanged();
            p.displayClientMessage(Component.literal("§aPoudre de cocaïne récupérée."), true);
            return;
        }
        if (!hand.isEmpty()) {
            p.displayClientMessage(Component.literal("§cLe four ne cuit que la pâte de coca (à fabriquer au laboratoire)."), true);
            return;
        }
        if (p.isShiftKeyDown() && paste > 0) {
            ItemHandlerHelper.giveItemToPlayer(p, new ItemStack(ModRegistry.COCA_PASTE.get(), paste));
            paste = 0;
            setChanged();
            p.displayClientMessage(Component.literal("§7Pâtes reprises."), true);
        } else if (paste > 0) {
            cooking = paste;
            paste = 0;
            progress = 0;
            setChanged();
            level.playSound(null, worldPosition, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.8f, 0.9f);
            p.displayClientMessage(Component.literal("§aLa cuisson démarre : " + cooking + " pâte(s)."), true);
        } else {
            p.displayClientMessage(Component.literal("§7Four vide. Déposez de la pâte de coca (laboratoire : 4 feuilles de coca + 1 produit chimique), puis main vide pour cuire."), true);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, OvenBE be) {
        if (be.cooking <= 0 || !(level instanceof ServerLevel sl)) return;
        be.progress++;
        Direction f = state.getValue(OvenBlock.FACING);
        double fx = pos.getX() + 0.5 + f.getStepX() * 0.52, fz = pos.getZ() + 0.5 + f.getStepZ() * 0.52;
        if (be.progress % 3 == 0) sl.sendParticles(ParticleTypes.FLAME, fx, pos.getY() + 0.3, fz, 1, 0.12, 0.05, 0.12, 0.003);
        if (be.progress % 8 == 0) sl.sendParticles(ParticleTypes.SMOKE, fx, pos.getY() + 0.5, fz, 1, 0.1, 0.05, 0.1, 0.01);
        if (be.progress % 10 == 0) sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 1, 0.05, 0.05, 0.05, 0.01);
        if (be.progress % 40 == 0) sl.playSound(null, pos, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 0.8f, 1.0f);
        if (be.progress >= totalTicks()) {
            be.powder += be.cooking;
            be.cooking = 0;
            be.progress = 0;
            sl.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.6f, 1.4f);
            be.setChanged();
        }
    }

    public void dropAll(Level level, BlockPos pos) {
        int pa = paste + cooking;
        if (pa > 0) Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(ModRegistry.COCA_PASTE.get(), pa));
        if (powder > 0) Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(ModRegistry.COCAINE_POWDER.get(), powder));
        paste = cooking = powder = 0;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("paste", paste);
        tag.putInt("cooking", cooking);
        tag.putInt("progress", progress);
        tag.putInt("powder", powder);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        paste = tag.getInt("paste");
        cooking = tag.getInt("cooking");
        progress = tag.getInt("progress");
        powder = tag.getInt("powder");
    }
}
