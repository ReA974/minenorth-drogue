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
import net.minecraftforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;

/** Contenu du four (écran OvenMenu) : pâtes de coca en attente, cuisson en cours (1 pâte = 1 poudre), poudre dans l'emplacement de sortie. */
public class OvenBE extends BlockEntity {
    public static final int PASTE = 0, POWDER = 1;
    /** Données envoyées à l'écran : progression (%), pâtes en cuisson, pâtes maximum. */
    public static final int DATA = 3;

    private final ItemStackHandler items = newHandler(this::setChanged);
    private final List<ItemStack> overflow = new ArrayList<>();
    private int cooking, progress, quality = Quality.DEFAULT;

    public OvenBE(BlockPos pos, BlockState state) {
        super(ModRegistry.OVEN_BE.get(), pos, state);
    }

    public static ItemStackHandler newHandler(Runnable changed) {
        return new ItemStackHandler(2) {
            @Override protected void onContentsChanged(int slot) { changed.run(); }

            @Override public boolean isItemValid(int slot, ItemStack st) { return slot == PASTE && st.is(ModRegistry.COCA_PASTE.get()); }

            @Override public int getSlotLimit(int slot) { return slot == PASTE ? Math.min(64, max()) : 64; }
        };
    }

    public ItemStackHandler items() { return items; }

    public static int max() { return Math.max(1, DrogueConfig.get(DrogueConfig.OVEN_MAX)); }

    private static int totalTicks() { return Math.max(1, DrogueConfig.get(DrogueConfig.OVEN_SECONDS)) * 20; }

    public boolean busy() { return cooking > 0; }

    /** Poudre obtenue à partir de cette pâte (même qualité, même coupe). */
    public static ItemStack powderOf(ItemStack paste) { return Quality.from(paste, ModRegistry.COCAINE_POWDER.get(), 1, 0); }

    /** Bouton « Lancer la cuisson » : toutes les pâtes déposées (dans la limite de la place en sortie). */
    public void start(Player p) {
        if (busy()) return;
        int n = Math.min(items.getStackInSlot(PASTE).getCount(), Stations.room(items, POWDER, powderOf(items.getStackInSlot(PASTE))));
        if (n <= 0) {
            p.displayClientMessage(Component.literal(items.getStackInSlot(PASTE).isEmpty() ? "§7Déposez d'abord de la pâte de coca." : "§cVidez d'abord la poudre du four."), true);
            return;
        }
        quality = Quality.get(items.getStackInSlot(PASTE));
        items.extractItem(PASTE, n, false);
        cooking = n;
        progress = 0;
        level.playSound(null, worldPosition, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.8f, 0.9f);
        setChanged();
    }

    public int data(int i) {
        return switch (i) {
            case 0 -> cooking <= 0 ? 0 : Math.min(100, progress * 100 / totalTicks());
            case 1 -> cooking;
            case 2 -> max();
            default -> 0;
        };
    }

    @Override
    public void onLoad() {
        super.onLoad();
        Stations.dropOverflow(level, worldPosition, overflow);
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
            Stations.addOutput(be.items, POWDER, Quality.set(new ItemStack(ModRegistry.COCAINE_POWDER.get()), be.quality), be.cooking);
            be.cooking = 0;
            be.progress = 0;
            sl.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.6f, 1.4f);
            be.setChanged();
        }
    }

    public void dropAll(Level level, BlockPos pos) {
        Stations.dropAll(level, pos, items);
        if (cooking > 0) Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), Quality.set(new ItemStack(ModRegistry.COCA_PASTE.get(), cooking), quality));
        cooking = 0;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("items", items.serializeNBT());
        tag.putInt("cooking", cooking);
        tag.putInt("progress", progress);
        tag.putInt("quality", quality);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("items")) items.deserializeNBT(tag.getCompound("items"));
        else {   // ancienne sauvegarde : compteurs
            Stations.legacy(items, PASTE, ModRegistry.COCA_PASTE.get(), tag.getInt("paste"), overflow);
            Stations.legacy(items, POWDER, ModRegistry.COCAINE_POWDER.get(), tag.getInt("powder"), overflow);
        }
        cooking = tag.getInt("cooking");
        progress = tag.getInt("progress");
        quality = tag.contains("quality") ? tag.getInt("quality") : Quality.DEFAULT;
    }
}
