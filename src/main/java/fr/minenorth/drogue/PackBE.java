package fr.minenorth.drogue;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Contenu de la table d'ensachage (écran PackMenu) : poudre de cocaïne, cristaux de meth, héroïne, sachets vides et sachets prêts. Les sachets se font par
 * le mini-jeu de timing de l'écran (voir MiniGame) : à chaque manche réussie, 3 unités de produit et 1 sachet vide deviennent 1 sachet ; une manche
 * ratée déchire le sachet (on perd le sachet vide et 1 unité). Toutes les manches réussies = un sachet en bonus.
 */
public class PackBE extends BlockEntity {
    public static final int POWDER = 0, CRYSTALS = 1, BAGS = 2, OUT = 3, HEROIN = 4;
    public static final int PER_BAG = 3, ROUNDS_MAX = 5;

    private final ItemStackHandler items = newHandler(this::setChanged);
    private final List<ItemStack> overflow = new ArrayList<>();

    public PackBE(BlockPos pos, BlockState state) {
        super(ModRegistry.PACK_BE.get(), pos, state);
    }

    public static ItemStackHandler newHandler(Runnable changed) {
        return new ItemStackHandler(5) {
            @Override protected void onContentsChanged(int slot) { changed.run(); }

            @Override public boolean isItemValid(int slot, ItemStack st) {
                return switch (slot) {
                    case POWDER -> st.is(ModRegistry.COCAINE_POWDER.get());
                    case CRYSTALS -> st.is(ModRegistry.METH_CRYSTALS.get());
                    case BAGS -> st.is(ModRegistry.EMPTY_BAG.get());
                    case HEROIN -> st.is(ModRegistry.HEROIN_POWDER.get());
                    default -> false;
                };
            }
        };
    }

    public ItemStackHandler items() { return items; }

    private int count(int slot) { return items.getStackInSlot(slot).getCount(); }

    /** Produit ensaché : le premier qui a de quoi faire un sachet (cocaïne, puis meth, puis héroïne). */
    public static int rawSlot(ItemStackHandler h) {
        for (int s : new int[]{POWDER, CRYSTALS, HEROIN}) if (h.getStackInSlot(s).getCount() >= PER_BAG) return s;
        return CRYSTALS;
    }

    public static Item productOf(int rawSlot) {
        return rawSlot == POWDER ? ModRegistry.COCAINE.get() : rawSlot == HEROIN ? ModRegistry.HEROIN.get() : ModRegistry.METH.get();
    }

    /** Sachet obtenu à partir de cette matière (même qualité, même coupe). */
    public static ItemStack bagOf(ItemStackHandler h, int rawSlot) { return Quality.from(h.getStackInSlot(rawSlot), productOf(rawSlot), 1, 0); }

    /** Nombre de manches possibles avec ce qu'il y a sur la table (et la place en sortie). */
    public static int possibleRounds(ItemStackHandler h) {
        int rs = rawSlot(h);
        int raw = h.getStackInSlot(rs).getCount();
        int room = Stations.room(h, OUT, bagOf(h, rs)) - 1;                 // une place pour le sachet bonus
        return Math.max(0, Math.min(Math.min(ROUNDS_MAX, room), Math.min(h.getStackInSlot(BAGS).getCount(), raw / PER_BAG)));
    }

    /** Bouton « Lancer » de l'écran. */
    public void startGame(ServerPlayer p) {
        if (MiniGame.activeAt(worldPosition)) return;
        int rounds = possibleRounds(items);
        if (rounds < 1) {
            p.displayClientMessage(Component.literal(count(BAGS) < 1 ? "§cIl manque des sachets vides." : "§cIl faut " + PER_BAG + " doses du même produit par sachet."), true);
            return;
        }
        int raw = rawSlot(items);
        ItemStack product = bagOf(items, raw);
        int[] made = {0};
        MiniGame.start(p, worldPosition, rounds, new MiniGame.Handler() {
            @Override
            public boolean round(ServerPlayer pl, boolean success, int index) {
                if (isRemoved() || count(BAGS) < 1 || count(raw) < (success ? PER_BAG : 1)) return false;
                items.extractItem(BAGS, 1, false);
                if (success) {
                    items.extractItem(raw, PER_BAG, false);
                    if (Stations.addOutput(items, OUT, product, 1)) made[0]++;
                    level.playSound(null, worldPosition, SoundEvents.BUNDLE_INSERT, SoundSource.BLOCKS, 0.8f, 1.2f);
                } else {
                    items.extractItem(raw, 1, false);                     // sachet déchiré
                    level.playSound(null, worldPosition, SoundEvents.WOOL_BREAK, SoundSource.BLOCKS, 0.8f, 1.4f);
                }
                return count(BAGS) >= 1 && count(raw) >= PER_BAG && Stations.room(items, OUT, product) > 1;
            }

            @Override
            public int end(ServerPlayer pl, int ok, int total) {
                if (!isRemoved() && total >= 3 && ok == total && Stations.addOutput(items, OUT, product, 1)) made[0]++;
                return made[0];
            }
        });
        if (level != null && level.getBlockState(worldPosition).getBlock() instanceof PackBlock pb) pb.startAnim(level, worldPosition);
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
            Stations.legacy(items, POWDER, ModRegistry.COCAINE_POWDER.get(), tag.getInt("powder"), overflow);
            Stations.legacy(items, CRYSTALS, ModRegistry.METH_CRYSTALS.get(), tag.getInt("crystals"), overflow);
            Stations.legacy(items, BAGS, ModRegistry.EMPTY_BAG.get(), tag.getInt("bags"), overflow);
            Stations.legacy(items, OUT, ItemStack.of(tag.getCompound("output")), overflow);
        }
    }
}
