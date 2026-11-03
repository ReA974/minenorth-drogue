package fr.minenorth.drogue;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Contenu de la table à rouler (écran RollMenu) : feuilles de cannabis (fraîches ou séchées), ou fiole de LSD ; papier ; produits prêts.
 * Mini-jeu de timing de l'écran (voir MiniGame) : à chaque manche réussie, 3 feuilles et 1 papier font 2 joints (1 si raté) ; 1 fiole de LSD et
 * 1 papier font 4 buvards (2 si raté). Toutes les manches réussies = un produit en bonus. La qualité suit celle de la matière.
 */
public class RollBE extends BlockEntity {
    public static final int LEAVES = 0, PAPER = 1, OUT = 2;
    public static final int LEAVES_PER_LOT = 3, JOINTS_PER_LOT = 2, ROUNDS_MAX = 6;

    private final ItemStackHandler items = newHandler(this::setChanged);
    private final List<ItemStack> overflow = new ArrayList<>();

    public RollBE(BlockPos pos, BlockState state) {
        super(ModRegistry.ROLL_BE.get(), pos, state);
    }

    public static ItemStackHandler newHandler(Runnable changed) {
        return new ItemStackHandler(3) {
            @Override protected void onContentsChanged(int slot) { changed.run(); }

            @Override public boolean isItemValid(int slot, ItemStack st) {
                return slot == LEAVES ? st.is(ModRegistry.CANNABIS_LEAF.get()) || st.is(ModRegistry.DRIED_CANNABIS.get()) || st.is(ModRegistry.LSD_LIQUID.get())
                        : slot == PAPER && st.is(Items.PAPER);
            }
        };
    }

    public ItemStackHandler items() { return items; }

    private int count(int slot) { return items.getStackInSlot(slot).getCount(); }

    public static boolean lsd(ItemStackHandler h) { return h.getStackInSlot(LEAVES).is(ModRegistry.LSD_LIQUID.get()); }

    /** Matière par manche et produits par manche réussie. */
    public static int perLot(ItemStackHandler h) { return lsd(h) ? 1 : LEAVES_PER_LOT; }

    public static int perSuccess(ItemStackHandler h) { return lsd(h) ? 4 : JOINTS_PER_LOT; }

    /** Produit obtenu (joint ou buvard), avec la qualité de la matière. */
    public static ItemStack productOf(ItemStackHandler h) {
        return Quality.from(h.getStackInSlot(LEAVES), lsd(h) ? ModRegistry.LSD.get() : ModRegistry.JOINT.get(), 1, 0);
    }

    /** Nombre de manches possibles (matière et place pour les produits, bonus compris). */
    public static int possibleRounds(ItemStackHandler h) {
        int room = (Stations.room(h, OUT, productOf(h)) - 1) / perSuccess(h);
        return Math.max(0, Math.min(Math.min(ROUNDS_MAX, room), Math.min(h.getStackInSlot(PAPER).getCount(), h.getStackInSlot(LEAVES).getCount() / perLot(h))));
    }

    /** Bouton « Rouler » de l'écran. */
    public void startGame(ServerPlayer p) {
        if (MiniGame.activeAt(worldPosition)) return;
        int rounds = possibleRounds(items);
        if (rounds < 1) {
            p.displayClientMessage(Component.literal(count(PAPER) < 1 ? "§cIl manque du papier." : "§cIl faut " + LEAVES_PER_LOT + " feuilles de cannabis (ou 1 fiole de LSD) par lot."), true);
            return;
        }
        int[] made = {0};
        ItemStack product = productOf(items);
        int per = perLot(items), win = perSuccess(items);
        MiniGame.start(p, worldPosition, rounds, new MiniGame.Handler() {
            @Override
            public boolean round(ServerPlayer pl, boolean success, int index) {
                if (isRemoved() || count(PAPER) < 1 || count(LEAVES) < per) return false;
                items.extractItem(PAPER, 1, false);
                items.extractItem(LEAVES, per, false);
                int n = success ? win : win / 2;
                if (Stations.addOutput(items, OUT, product, n)) made[0] += n;
                level.playSound(null, worldPosition, SoundEvents.LEASH_KNOT_PLACE, SoundSource.BLOCKS, 0.7f, 1.4f);
                return count(PAPER) >= 1 && count(LEAVES) >= per && Stations.room(items, OUT, product) > win;
            }

            @Override
            public int end(ServerPlayer pl, int ok, int total) {
                if (!isRemoved() && total >= 3 && ok == total && Stations.addOutput(items, OUT, product, 1)) made[0]++;
                return made[0];
            }
        });
    }

    @Override
    public void onLoad() {
        super.onLoad();
        Stations.dropOverflow(level, worldPosition, overflow);
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
        else {   // ancienne sauvegarde : compteurs
            Stations.legacy(items, LEAVES, ModRegistry.CANNABIS_LEAF.get(), tag.getInt("leaves"), overflow);
            Stations.legacy(items, PAPER, Items.PAPER, tag.getInt("papers"), overflow);
            Stations.legacy(items, OUT, ItemStack.of(tag.getCompound("output")), overflow);
        }
    }
}
