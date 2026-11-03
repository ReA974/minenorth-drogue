package fr.minenorth.drogue;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;

/**
 * Presse à pilules (écran PressMenu) : MDMA en cristaux -> pilules d'ecstasy, par le mini-jeu de timing (voir MiniGame). Manche réussie :
 * 1 dose = 2 pilules ; ratée : 1 pilule ébréchée. Toutes réussies = une pilule en bonus. La qualité suit celle du MDMA.
 */
public class PressBE extends BlockEntity {
    public static final int MDMA = 0, OUT = 1;
    public static final int PILLS = 2, ROUNDS_MAX = 6;

    private final ItemStackHandler items = newHandler(this::setChanged);

    public PressBE(BlockPos pos, BlockState state) {
        super(ModRegistry.PRESS_BE.get(), pos, state);
    }

    public static ItemStackHandler newHandler(Runnable changed) {
        return new ItemStackHandler(2) {
            @Override protected void onContentsChanged(int slot) { changed.run(); }

            @Override public boolean isItemValid(int slot, ItemStack st) { return slot == MDMA && st.is(ModRegistry.MDMA.get()); }
        };
    }

    public ItemStackHandler items() { return items; }

    public static ItemStack pillOf(ItemStackHandler h) { return Quality.from(h.getStackInSlot(MDMA), ModRegistry.ECSTASY.get(), 1, 0); }

    public static int possibleRounds(ItemStackHandler h) {
        int room = (Stations.room(h, OUT, pillOf(h)) - 1) / PILLS;
        return Math.max(0, Math.min(Math.min(ROUNDS_MAX, room), h.getStackInSlot(MDMA).getCount()));
    }

    /** Bouton « Presser » de l'écran. */
    public void startGame(ServerPlayer p) {
        if (MiniGame.activeAt(worldPosition)) return;
        int rounds = possibleRounds(items);
        if (rounds < 1) {
            p.displayClientMessage(Component.literal(items.getStackInSlot(MDMA).isEmpty() ? "§cDéposez du MDMA (laboratoire)." : "§cVidez d'abord les pilules."), true);
            return;
        }
        ItemStack pill = pillOf(items);
        int[] made = {0};
        MiniGame.start(p, worldPosition, rounds, new MiniGame.Handler() {
            @Override
            public boolean round(ServerPlayer pl, boolean success, int index) {
                if (isRemoved() || items.getStackInSlot(MDMA).isEmpty()) return false;
                items.extractItem(MDMA, 1, false);
                int n = success ? PILLS : 1;
                if (Stations.addOutput(items, OUT, pill, n)) made[0] += n;
                level.playSound(null, worldPosition, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.25f, success ? 1.8f : 1.3f);
                return !items.getStackInSlot(MDMA).isEmpty() && Stations.room(items, OUT, pill) > PILLS;
            }

            @Override
            public int end(ServerPlayer pl, int ok, int total) {
                if (!isRemoved() && total >= 3 && ok == total && Stations.addOutput(items, OUT, pill, 1)) made[0]++;
                return made[0];
            }
        });
    }

    public void dropAll(Level level, BlockPos pos) { Stations.dropAll(level, pos, items); }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("items", items.serializeNBT());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("items")) items.deserializeNBT(tag.getCompound("items"));
    }
}
