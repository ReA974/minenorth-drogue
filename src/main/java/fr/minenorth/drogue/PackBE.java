package fr.minenorth.drogue;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
 * Contenu de la table d'ensachage : poudre de cocaïne, cristaux de meth et sachets vides. Les sachets se font par un mini-jeu de timing (voir MiniGame) :
 * à chaque manche réussie, 3 unités de produit et 1 sachet vide deviennent 1 sachet ; une manche ratée déchire le sachet (on perd le sachet vide et 1 unité).
 * Toutes les manches réussies = un sachet en bonus.
 */
public class PackBE extends BlockEntity {
    private static final int PER_BAG = 3, OUT_MAX = 64, ROUNDS_MAX = 5;

    private int powder, crystals, bags;
    private ItemStack output = ItemStack.EMPTY;

    public PackBE(BlockPos pos, BlockState state) {
        super(ModRegistry.PACK_BE.get(), pos, state);
    }

    public void interact(Player pl) {
        if (!(pl instanceof ServerPlayer p)) return;
        if (MiniGame.active(p.getUUID())) { MiniGame.stop(p); return; }
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
            p.displayClientMessage(Component.literal("§7" + status() + " — main vide : lancer l'ensachage"), true);
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
            powder = crystals = bags = 0;
            setChanged();
            p.displayClientMessage(Component.literal("§7Table vidée."), true);
            return;
        }
        if (hand.isEmpty() && !startGame(p)) {
            p.displayClientMessage(Component.literal("§7" + status() + " — " + (bags < 1 ? "il manque des sachets vides" : "il faut " + PER_BAG + " poudres (ou " + PER_BAG + " cristaux) par sachet")
                    + " (accroupi, main vide : tout reprendre)"), true);
        }
    }

    private String status() {
        return "Table : " + powder + " poudre, " + crystals + " cristaux, " + bags + " sachets vides";
    }

    private boolean useCocaine() { return powder >= PER_BAG; }

    /** Nombre de manches possibles avec ce qu'il y a sur la table. */
    private int possibleRounds() {
        int raw = useCocaine() ? powder : crystals;
        return Math.min(ROUNDS_MAX, Math.min(bags, raw / PER_BAG));
    }

    private boolean startGame(ServerPlayer p) {
        int rounds = possibleRounds();
        if (rounds < 1) return false;
        boolean coke = useCocaine();
        Item product = coke ? ModRegistry.COCAINE.get() : ModRegistry.METH.get();
        int[] made = {0};
        MiniGame.start(p, worldPosition, rounds, new MiniGame.Handler() {
            @Override
            public boolean round(ServerPlayer pl, boolean success, int index) {
                if (isRemoved()) return false;
                bags--;
                if (success) {
                    if (coke) powder -= PER_BAG; else crystals -= PER_BAG;
                    add(product, 1);
                    made[0]++;
                } else {
                    if (coke) powder -= 1; else crystals -= 1;           // sachet déchiré
                }
                setChanged();
                return bags >= 1 && (coke ? powder : crystals) >= PER_BAG && (output.isEmpty() || output.getCount() < OUT_MAX);
            }

            @Override
            public void end(ServerPlayer pl, int ok, int total) {
                if (!isRemoved() && total >= 3 && ok == total) {
                    add(product, 1);
                    made[0]++;
                    pl.displayClientMessage(Component.literal("§6Ensachage parfait : un sachet en bonus ! §f(" + made[0] + " sachets)"), true);
                } else {
                    pl.displayClientMessage(Component.literal("§7Ensachage terminé : §f" + made[0] + " sachet(s)§7 (" + ok + " réussi(s) sur " + total + "). Récupérez-les à main nue."), true);
                }
            }
        });
        if (level != null && level.getBlockState(worldPosition).getBlock() instanceof PackBlock pb) pb.startAnim(level, worldPosition);
        return true;
    }

    private void add(Item it, int n) {
        if (output.isEmpty()) output = new ItemStack(it, n);
        else if (output.getItem() == it) output.grow(n);
        setChanged();
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
        tag.put("output", output.save(new CompoundTag()));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        powder = tag.getInt("powder");
        crystals = tag.getInt("crystals");
        bags = tag.getInt("bags");
        output = ItemStack.of(tag.getCompound("output"));
    }
}
