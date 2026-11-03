package fr.minenorth.drogue;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.MenuConstructor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkHooks;

import java.util.List;

/** Outils communs aux machines : ouverture d'un écran, reprise des anciennes sauvegardes (compteurs) dans les emplacements. */
public final class Stations {
    private Stations() {}

    /** Ouvre l'écran d'une machine ; le client reçoit la position de la case d'origine. */
    public static void open(ServerPlayer p, BlockPos origin, String title, MenuConstructor menu) {
        NetworkHooks.openScreen(p, new SimpleMenuProvider(menu, Component.literal(title)), buf -> buf.writeBlockPos(origin));
    }

    /** Ancienne sauvegarde : n objets dans l'emplacement ; ce qui dépasse une pile est mis de côté (lâché au chargement). */
    public static void legacy(ItemStackHandler h, int slot, Item item, int n, List<ItemStack> overflow) {
        if (n <= 0) return;
        int max = Math.min(item.getMaxStackSize(), h.getSlotLimit(slot));
        int in = Math.min(n, max);
        h.setStackInSlot(slot, new ItemStack(item, in));
        for (int rest = n - in; rest > 0; rest -= max) overflow.add(new ItemStack(item, Math.min(rest, max)));
    }

    public static void legacy(ItemStackHandler h, int slot, ItemStack st, List<ItemStack> overflow) {
        if (st.isEmpty()) return;
        if (h.getStackInSlot(slot).isEmpty()) h.setStackInSlot(slot, st);
        else overflow.add(st);
    }

    public static void dropOverflow(Level level, BlockPos pos, List<ItemStack> overflow) {
        if (level == null || level.isClientSide || overflow.isEmpty()) return;
        for (ItemStack st : overflow) Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, st);
        overflow.clear();
    }

    public static void dropAll(Level level, BlockPos pos, ItemStackHandler h) {
        for (int i = 0; i < h.getSlots(); i++) {
            ItemStack st = h.getStackInSlot(i);
            if (!st.isEmpty()) Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), st);
            h.setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    /** Ajoute au produit fini ; faux si l'emplacement de sortie n'a pas la place. */
    public static boolean addOutput(ItemStackHandler h, int slot, Item item, int n) {
        ItemStack out = h.getStackInSlot(slot);
        if (out.isEmpty()) { h.setStackInSlot(slot, new ItemStack(item, n)); return true; }
        if (out.getItem() != item || out.getCount() + n > out.getMaxStackSize()) return false;
        h.setStackInSlot(slot, new ItemStack(item, out.getCount() + n));
        return true;
    }

    /** Ajoute au produit fini un objet avec ses données (qualité, coupe) ; faux s'il n'y a pas la place ou si la sortie contient autre chose. */
    public static boolean addOutput(ItemStackHandler h, int slot, ItemStack template, int n) {
        ItemStack out = h.getStackInSlot(slot);
        if (out.isEmpty()) { h.setStackInSlot(slot, template.copyWithCount(n)); return true; }
        if (!ItemStack.isSameItemSameTags(out, template) || out.getCount() + n > out.getMaxStackSize()) return false;
        h.setStackInSlot(slot, out.copyWithCount(out.getCount() + n));
        return true;
    }

    /** Place libre en sortie pour cet objet avec ses données (0 si la sortie contient un autre produit ou une autre qualité). */
    public static int room(ItemStackHandler h, int slot, ItemStack template) {
        ItemStack out = h.getStackInSlot(slot);
        if (out.isEmpty()) return template.getMaxStackSize();
        return ItemStack.isSameItemSameTags(out, template) ? out.getMaxStackSize() - out.getCount() : 0;
    }

    /** Place libre dans l'emplacement de sortie pour cet objet. */
    public static int room(ItemStackHandler h, int slot, Item item) {
        ItemStack out = h.getStackInSlot(slot);
        if (out.isEmpty()) return item.getMaxStackSize();
        return out.getItem() == item ? out.getMaxStackSize() - out.getCount() : 0;
    }
}
