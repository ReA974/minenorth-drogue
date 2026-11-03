package fr.minenorth.drogue.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

import java.util.function.BooleanSupplier;

/**
 * Base des écrans de machine (labo, four, tables) : emplacements de la machine, puis l'inventaire du joueur.
 * Côté client, la machine est représentée par un inventaire vide de même taille (rempli par la synchro vanilla des emplacements).
 */
public abstract class StationMenu extends AbstractContainerMenu {
    /** Largeur de l'écran (inventaire du joueur centré). */
    public static final int WIDTH = 248;

    public final BlockPos pos;
    public final ContainerData data;
    protected final Level level;
    private final Block block;
    private int machineSlots;
    /** Haut de l'inventaire du joueur dans l'écran. */
    public int invY;

    protected StationMenu(MenuType<?> type, int id, Inventory inv, BlockPos pos, Block block, ContainerData data) {
        super(type, id);
        this.pos = pos;
        this.level = inv.player.level();
        this.block = block;
        this.data = data;
        addDataSlots(data);
    }

    /** Emplacement de la machine ; output = on ne peut que reprendre ; locked = verrouillé (mini-jeu en cours). */
    protected void machineSlot(IItemHandler h, int index, int x, int y, boolean output, BooleanSupplier locked) {
        addSlot(new MachineSlot(h, index, x, y, output, locked));
        machineSlots++;
    }

    protected void playerInventory(Inventory inv, int y) {
        invY = y;
        int x = (WIDTH - 162) / 2;
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c + r * 9 + 9, x + c * 18, y + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, x + c * 18, y + 58));
    }

    public boolean isMachineSlot(Slot s) { return s.index < machineSlots; }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack st = slot.getItem();
        ItemStack copy = st.copy();
        if (index < machineSlots) {
            if (!moveItemStackTo(st, machineSlots, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(st, 0, machineSlots, false)) {
            return ItemStack.EMPTY;
        }
        if (st.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return level.getBlockState(pos).getBlock() == block && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64;
    }

    public static class MachineSlot extends SlotItemHandler {
        private final boolean output;
        private final BooleanSupplier locked;

        public MachineSlot(IItemHandler h, int index, int x, int y, boolean output, BooleanSupplier locked) {
            super(h, index, x, y);
            this.output = output;
            this.locked = locked;
        }

        public boolean isOutput() { return output; }

        @Override
        public boolean mayPlace(ItemStack stack) { return !output && !locked.getAsBoolean() && super.mayPlace(stack); }

        @Override
        public boolean mayPickup(Player player) { return !locked.getAsBoolean() && super.mayPickup(player); }
    }
}
