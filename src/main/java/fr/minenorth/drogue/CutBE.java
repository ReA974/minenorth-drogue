package fr.minenorth.drogue;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Table de coupe (écran CutMenu) : on mélange une poudre (cocaïne, meth, héroïne, MDMA) avec un additif pour en avoir plus.
 * 2 doses + 1 additif = 3 doses, une étoile de moins ; chaque additif laisse un petit effet (voir DrugItem). Deux coupes au plus par produit.
 */
public class CutBE extends BlockEntity {
    public static final int PRODUCT = 0, ADDITIVE = 1, OUT = 2;
    public static final int PER_BATCH = 2, MADE = 3, MAX_CUTS = 2;

    private final ItemStackHandler items = newHandler(this::setChanged);

    public CutBE(BlockPos pos, BlockState state) {
        super(ModRegistry.CUT_BE.get(), pos, state);
    }

    public static ItemStackHandler newHandler(Runnable changed) {
        return new ItemStackHandler(3) {
            @Override protected void onContentsChanged(int slot) { changed.run(); }

            @Override public boolean isItemValid(int slot, ItemStack st) {
                return slot == PRODUCT ? DrugItems.cuttable(st) : slot == ADDITIVE && DrugItems.additive(st);
            }
        };
    }

    public ItemStackHandler items() { return items; }

    /** Produit coupé obtenu (qualité - 1, additif ajouté), ou vide si on ne peut pas couper. */
    public static ItemStack result(ItemStackHandler h) {
        ItemStack prod = h.getStackInSlot(PRODUCT), add = h.getStackInSlot(ADDITIVE);
        if (prod.isEmpty() || add.isEmpty() || Quality.cuts(prod).size() >= MAX_CUTS) return ItemStack.EMPTY;
        ItemStack out = Quality.from(prod, prod.getItem(), 1, -1);
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(add.getItem());
        Quality.addCut(out, id == null ? "minenorthdrogue:baking_soda" : id.toString());
        return out;
    }

    /** Lots possibles avec ce qui est posé (et la place en sortie). */
    public static int batches(ItemStackHandler h) {
        ItemStack r = result(h);
        if (r.isEmpty()) return 0;
        int room = Stations.room(h, OUT, r) / MADE;
        return Math.max(0, Math.min(room, Math.min(h.getStackInSlot(PRODUCT).getCount() / PER_BATCH, h.getStackInSlot(ADDITIVE).getCount())));
    }

    /** Bouton « Couper » : tous les lots possibles d'un coup. */
    public void cut(Player p) {
        ItemStack r = result(items);
        int n = batches(items);
        if (n < 1) {
            String why = items.getStackInSlot(PRODUCT).isEmpty() ? "Déposez de la poudre ou des cristaux."
                    : Quality.cuts(items.getStackInSlot(PRODUCT)).size() >= MAX_CUTS ? "Ce produit est déjà coupé deux fois."
                    : items.getStackInSlot(ADDITIVE).isEmpty() ? "Il manque un additif (bicarbonate, sucre, paracétamol, caféine)." : "Videz d'abord la sortie.";
            p.displayClientMessage(Component.literal("§c" + why), true);
            return;
        }
        ItemStack powder = items.getStackInSlot(PRODUCT).copy();
        items.extractItem(PRODUCT, n * PER_BATCH, false);
        items.extractItem(ADDITIVE, n, false);
        Stations.addOutput(items, OUT, r, n * MADE);
        if (level instanceof ServerLevel sl) {
            sl.playSound(null, worldPosition, SoundEvents.SAND_PLACE, SoundSource.BLOCKS, 0.8f, 1.6f);
            sl.playSound(null, worldPosition, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 0.6f, 1.8f);
            sl.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, powder), worldPosition.getX() + 0.5, worldPosition.getY() + 1.05, worldPosition.getZ() + 0.5,
                    12, 0.2, 0.02, 0.2, 0.02);
        }
        p.displayClientMessage(Component.literal("§a" + n * MADE + " doses coupées (" + Quality.stars(Quality.get(r)) + ")."), true);
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
