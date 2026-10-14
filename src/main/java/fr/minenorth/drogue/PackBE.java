package fr.minenorth.drogue;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * Contenu de la table d'ensachage : poudre de cocaïne, cristaux de meth et sachets vides. Toutes les quelques secondes, 3 unités de produit
 * et 1 sachet vide deviennent 1 sachet à consommer, qui attend d'être récupéré à main nue.
 */
public class PackBE extends BlockEntity {
    private static final int PER_BAG = 3, OUT_MAX = 64;

    private int powder, crystals, bags, progress;
    private ItemStack output = ItemStack.EMPTY;

    public PackBE(BlockPos pos, BlockState state) {
        super(ModRegistry.PACK_BE.get(), pos, state);
    }

    public void interact(Player p) {
        ItemStack hand = p.getMainHandItem();
        Item it = hand.getItem();
        if (!hand.isEmpty() && (it == ModRegistry.COCAINE_POWDER.get() || it == ModRegistry.METH_CRYSTALS.get() || it == ModRegistry.EMPTY_BAG.get())) {
            int n = hand.getCount();
            if (it == ModRegistry.COCAINE_POWDER.get()) powder += n;
            else if (it == ModRegistry.METH_CRYSTALS.get()) crystals += n;
            else bags += n;
            hand.shrink(n);
            setChanged();
            level.playSound(null, worldPosition, SoundEvents.BUNDLE_INSERT, SoundSource.BLOCKS, 0.8f, 1.1f);
            p.displayClientMessage(Component.literal("§7" + status()), true);
            return;
        }
        if (!output.isEmpty()) {
            ItemHandlerHelper.giveItemToPlayer(p, output);
            output = ItemStack.EMPTY;
            setChanged();
            p.displayClientMessage(Component.literal("§aSachets récupérés."), true);
            return;
        }
        if (hand.isEmpty() && p.isShiftKeyDown()) {
            if (powder > 0) ItemHandlerHelper.giveItemToPlayer(p, new ItemStack(ModRegistry.COCAINE_POWDER.get(), powder));
            if (crystals > 0) ItemHandlerHelper.giveItemToPlayer(p, new ItemStack(ModRegistry.METH_CRYSTALS.get(), crystals));
            if (bags > 0) ItemHandlerHelper.giveItemToPlayer(p, new ItemStack(ModRegistry.EMPTY_BAG.get(), bags));
            powder = crystals = bags = progress = 0;
            setChanged();
            p.displayClientMessage(Component.literal("§7Table vidée."), true);
            return;
        }
        p.displayClientMessage(Component.literal("§7" + status() + " (accroupi, main vide : tout reprendre)"), true);
    }

    private String status() {
        return "Table : " + powder + " poudre, " + crystals + " cristaux, " + bags + " sachets vides" + (output.isEmpty() ? "" : ", " + output.getCount() + " sachet(s) prêt(s)") + " — " + progressText();
    }

    /** Où en est l'ensachage (ou pourquoi il ne démarre pas). */
    private String progressText() {
        Item n = next();
        if (n != null) {
            int total = Math.max(1, DrogueConfig.get(DrogueConfig.PACK_SECONDS)) * 20;
            return "ensachage en cours : " + Math.min(99, progress * 100 / total) + " %";
        }
        if (!output.isEmpty() && (powder >= PER_BAG || crystals >= PER_BAG) && bags >= 1) return "récupérez d'abord les sachets prêts (clic à main nue)";
        if (bags < 1) return "il manque des sachets vides";
        return "il faut " + PER_BAG + " poudres (ou " + PER_BAG + " cristaux) par sachet";
    }

    /** Produit à ensacher maintenant, ou null. */
    private Item next() {
        if (bags < 1) return null;
        if (powder >= PER_BAG && canOutput(ModRegistry.COCAINE.get())) return ModRegistry.COCAINE.get();
        if (crystals >= PER_BAG && canOutput(ModRegistry.METH.get())) return ModRegistry.METH.get();
        return null;
    }

    private boolean canOutput(Item it) {
        return output.isEmpty() || (output.getItem() == it && output.getCount() < OUT_MAX);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PackBE be) {
        if (!(level instanceof ServerLevel sl)) return;
        Item next = be.next();
        if (next == null) { be.progress = 0; return; }
        be.progress++;
        if (be.progress % 20 == 0) sl.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.6f, 1.3f);
        if (be.progress >= Math.max(1, DrogueConfig.get(DrogueConfig.PACK_SECONDS)) * 20) {
            be.progress = 0;
            be.bags--;
            if (next == ModRegistry.COCAINE.get()) be.powder -= PER_BAG; else be.crystals -= PER_BAG;
            if (be.output.isEmpty()) be.output = new ItemStack(next); else be.output.grow(1);
            be.setChanged();
        }
    }

    public void dropAll(Level level, BlockPos pos) {
        if (powder > 0) Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(ModRegistry.COCAINE_POWDER.get(), powder));
        if (crystals > 0) Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(ModRegistry.METH_CRYSTALS.get(), crystals));
        if (bags > 0) Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(ModRegistry.EMPTY_BAG.get(), bags));
        if (!output.isEmpty()) Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), output);
        powder = crystals = bags = 0;
        output = ItemStack.EMPTY;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("powder", powder);
        tag.putInt("crystals", crystals);
        tag.putInt("bags", bags);
        tag.putInt("progress", progress);
        tag.put("output", output.save(new CompoundTag()));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        powder = tag.getInt("powder");
        crystals = tag.getInt("crystals");
        bags = tag.getInt("bags");
        progress = tag.getInt("progress");
        output = ItemStack.of(tag.getCompound("output"));
    }
}
