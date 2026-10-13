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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * Contenu de la table à rouler : feuilles de cannabis et papier. Toutes les quelques secondes, 3 feuilles et 1 papier deviennent 2 joints
 * (comme à l'établi), qui attendent d'être récupérés à main nue.
 */
public class RollBE extends BlockEntity {
    private static final int LEAVES_PER_LOT = 3, JOINTS_PER_LOT = 2, OUT_MAX = 64;

    private int leaves, papers, progress;
    private ItemStack output = ItemStack.EMPTY;

    public RollBE(BlockPos pos, BlockState state) {
        super(ModRegistry.ROLL_BE.get(), pos, state);
    }

    public void interact(Player p) {
        ItemStack hand = p.getMainHandItem();
        Item it = hand.getItem();
        if (!hand.isEmpty() && (it == ModRegistry.CANNABIS_LEAF.get() || it == Items.PAPER)) {
            int n = hand.getCount();
            if (it == Items.PAPER) papers += n; else leaves += n;
            hand.shrink(n);
            setChanged();
            level.playSound(null, worldPosition, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 0.8f, 1.1f);
            p.displayClientMessage(Component.literal("§7" + status()), true);
            return;
        }
        if (!output.isEmpty()) {
            ItemHandlerHelper.giveItemToPlayer(p, output);
            output = ItemStack.EMPTY;
            setChanged();
            p.displayClientMessage(Component.literal("§aJoints récupérés."), true);
            return;
        }
        if (hand.isEmpty() && p.isShiftKeyDown()) {
            if (leaves > 0) ItemHandlerHelper.giveItemToPlayer(p, new ItemStack(ModRegistry.CANNABIS_LEAF.get(), leaves));
            if (papers > 0) ItemHandlerHelper.giveItemToPlayer(p, new ItemStack(Items.PAPER, papers));
            leaves = papers = progress = 0;
            setChanged();
            p.displayClientMessage(Component.literal("§7Table vidée."), true);
            return;
        }
        p.displayClientMessage(Component.literal("§7" + status() + " — déposez feuilles de cannabis et papier (accroupi, main vide : tout reprendre)."), true);
    }

    private String status() {
        return "Table : " + leaves + " feuille(s), " + papers + " papier(s)";
    }

    private boolean canRoll() {
        if (leaves < LEAVES_PER_LOT || papers < 1) return false;
        return output.isEmpty() || (output.getItem() == ModRegistry.JOINT.get() && output.getCount() + JOINTS_PER_LOT <= OUT_MAX);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, RollBE be) {
        if (!(level instanceof ServerLevel sl)) return;
        if (!be.canRoll()) { be.progress = 0; return; }
        be.progress++;
        if (be.progress % 20 == 0) sl.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 0.5f, 1.5f);
        if (be.progress >= Math.max(1, DrogueConfig.get(DrogueConfig.ROLL_SECONDS)) * 20) {
            be.progress = 0;
            be.leaves -= LEAVES_PER_LOT;
            be.papers--;
            if (be.output.isEmpty()) be.output = new ItemStack(ModRegistry.JOINT.get(), JOINTS_PER_LOT); else be.output.grow(JOINTS_PER_LOT);
            sl.playSound(null, pos, SoundEvents.LEASH_KNOT_PLACE, SoundSource.BLOCKS, 0.7f, 1.4f);
            be.setChanged();
        }
    }

    public void dropAll(Level level, BlockPos pos) {
        if (leaves > 0) Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(ModRegistry.CANNABIS_LEAF.get(), leaves));
        if (papers > 0) Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(Items.PAPER, papers));
        if (!output.isEmpty()) Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), output);
        leaves = papers = 0;
        output = ItemStack.EMPTY;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("leaves", leaves);
        tag.putInt("papers", papers);
        tag.putInt("progress", progress);
        tag.put("output", output.save(new CompoundTag()));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        leaves = tag.getInt("leaves");
        papers = tag.getInt("papers");
        progress = tag.getInt("progress");
        output = ItemStack.of(tag.getCompound("output"));
    }
}
