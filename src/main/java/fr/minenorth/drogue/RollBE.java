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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * Contenu de la table à rouler : feuilles de cannabis et papier. Les joints se roulent par un mini-jeu de timing (voir MiniGame) : à chaque manche réussie,
 * 3 feuilles et 1 papier font 2 joints ; une manche ratée donne un joint mal roulé (1 joint). Toutes les manches réussies = un joint en bonus.
 */
public class RollBE extends BlockEntity {
    private static final int LEAVES_PER_LOT = 3, JOINTS_PER_LOT = 2, OUT_MAX = 64, ROUNDS_MAX = 6;

    private int leaves, papers;
    private ItemStack output = ItemStack.EMPTY;

    public RollBE(BlockPos pos, BlockState state) {
        super(ModRegistry.ROLL_BE.get(), pos, state);
    }

    public void interact(Player pl) {
        if (!(pl instanceof ServerPlayer p)) return;
        if (MiniGame.active(p.getUUID())) { MiniGame.stop(p); return; }
        ItemStack hand = p.getMainHandItem();
        Item it = hand.getItem();
        if (!hand.isEmpty() && (it == ModRegistry.CANNABIS_LEAF.get() || it == Items.PAPER)) {
            int n = hand.getCount();
            if (it == Items.PAPER) papers += n; else leaves += n;
            hand.shrink(n);
            setChanged();
            level.playSound(null, worldPosition, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 0.8f, 1.1f);
            p.displayClientMessage(Component.literal("§7" + status() + " — main vide : rouler"), true);
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
            leaves = papers = 0;
            setChanged();
            p.displayClientMessage(Component.literal("§7Table vidée."), true);
            return;
        }
        if (hand.isEmpty() && !startGame(p)) {
            p.displayClientMessage(Component.literal("§7" + status() + " — " + (papers < 1 ? "il manque du papier" : "il faut " + LEAVES_PER_LOT + " feuilles de cannabis par lot")
                    + " (accroupi, main vide : tout reprendre)"), true);
        }
    }

    private String status() {
        return "Table : " + leaves + " feuille(s), " + papers + " papier(s)";
    }

    private int possibleRounds() { return Math.min(ROUNDS_MAX, Math.min(papers, leaves / LEAVES_PER_LOT)); }

    private boolean startGame(ServerPlayer p) {
        int rounds = possibleRounds();
        if (rounds < 1) return false;
        int[] made = {0};
        MiniGame.start(p, worldPosition, rounds, new MiniGame.Handler() {
            @Override
            public boolean round(ServerPlayer pl, boolean success, int index) {
                if (isRemoved()) return false;
                papers--;
                leaves -= LEAVES_PER_LOT;
                int n = success ? JOINTS_PER_LOT : 1;
                add(n);
                made[0] += n;
                pl.level().playSound(null, worldPosition, SoundEvents.LEASH_KNOT_PLACE, SoundSource.BLOCKS, 0.7f, 1.4f);
                return papers >= 1 && leaves >= LEAVES_PER_LOT && (output.isEmpty() || output.getCount() + JOINTS_PER_LOT <= OUT_MAX);
            }

            @Override
            public void end(ServerPlayer pl, int ok, int total) {
                if (!isRemoved() && total >= 3 && ok == total) {
                    add(1);
                    made[0]++;
                    pl.displayClientMessage(Component.literal("§6Roulage parfait : un joint en bonus ! §f(" + made[0] + " joints)"), true);
                } else {
                    pl.displayClientMessage(Component.literal("§7Roulage terminé : §f" + made[0] + " joint(s)§7 (" + ok + " bien roulé(s) sur " + total + "). Récupérez-les à main nue."), true);
                }
            }
        });
        return true;
    }

    private void add(int n) {
        if (output.isEmpty()) output = new ItemStack(ModRegistry.JOINT.get(), n); else output.grow(n);
        setChanged();
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
        tag.put("output", output.save(new CompoundTag()));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        leaves = tag.getInt("leaves");
        papers = tag.getInt("papers");
        output = ItemStack.of(tag.getCompound("output"));
    }
}
