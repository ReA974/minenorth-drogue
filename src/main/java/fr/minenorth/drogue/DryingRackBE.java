package fr.minenorth.drogue;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Contenu du séchoir : feuilles de cannabis suspendues (une seule qualité à la fois) et heure de fin du séchage (temps de jeu). */
public class DryingRackBE extends BlockEntity {
    public static final int MAX = 16;

    private ItemStack leaves = ItemStack.EMPTY;
    private long doneAt;

    public DryingRackBE(BlockPos pos, BlockState state) {
        super(ModRegistry.DRYING_BE.get(), pos, state);
    }

    public ItemStack leaves() { return leaves; }

    public boolean dried() { return !leaves.isEmpty() && leaves.is(ModRegistry.DRIED_CANNABIS.get()); }

    public long remainingTicks(long now) { return Math.max(0, doneAt - now); }

    /** Ajoute des feuilles fraîches (même qualité que celles déjà suspendues) ; renvoie le nombre pris. Le séchage repart de zéro. */
    public int add(ItemStack st, long now) {
        if (dried()) return 0;
        if (!leaves.isEmpty() && !ItemStack.isSameItemSameTags(leaves, st)) return 0;
        int n = Math.min(st.getCount(), MAX - leaves.getCount());
        if (n <= 0) return 0;
        leaves = leaves.isEmpty() ? st.copyWithCount(n) : leaves.copyWithCount(leaves.getCount() + n);
        doneAt = now + DrogueConfig.get(DrogueConfig.DRY_SECONDS) * 20L;
        setChanged();
        return n;
    }

    /** Fin du séchage : les feuilles deviennent des têtes séchées (+1 étoile). Vrai si quelque chose a changé. */
    public boolean tryFinish(long now) {
        if (leaves.isEmpty() || dried() || now < doneAt) return false;
        leaves = Quality.from(leaves, ModRegistry.DRIED_CANNABIS.get(), leaves.getCount(), 1);
        setChanged();
        return true;
    }

    public ItemStack take() {
        ItemStack out = leaves;
        leaves = ItemStack.EMPTY;
        doneAt = 0;
        setChanged();
        return out;
    }

    public void dropAll(Level level, BlockPos pos) {
        if (!leaves.isEmpty()) Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), leaves);
        leaves = ItemStack.EMPTY;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("leaves", leaves.save(new CompoundTag()));
        tag.putLong("doneAt", doneAt);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        leaves = ItemStack.of(tag.getCompound("leaves"));
        doneAt = tag.getLong("doneAt");
    }
}
