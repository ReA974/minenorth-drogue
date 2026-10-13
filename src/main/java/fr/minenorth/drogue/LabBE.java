package fr.minenorth.drogue;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Contenu du laboratoire : ingrédients déposés un à un. Dès que la combinaison complète d'une recette est là, la fabrication démarre
 * (les ingrédients sont consommés), puis le produit attend d'être récupéré à main nue.
 */
public class LabBE extends BlockEntity {
    public record Need(Supplier<? extends Item> item, int count) {}

    public record Recipe(String id, List<Need> needs, Supplier<? extends Item> result, Supplier<Integer> yield, Supplier<Integer> seconds) {
        int needed(Item it) {
            for (Need n : needs) if (n.item().get() == it) return n.count();
            return 0;
        }
    }

    public static final List<Recipe> RECIPES = List.of(
            new Recipe("cocaine", List.of(new Need(ModRegistry.COCA_LEAF, 4), new Need(ModRegistry.CHEMICAL, 1)), ModRegistry.COCAINE_POWDER,
                    () -> DrogueConfig.get(DrogueConfig.COCAINE_YIELD), () -> DrogueConfig.get(DrogueConfig.COCAINE_LAB_SECONDS)),
            new Recipe("meth", List.of(new Need(ModRegistry.EPHEDRA, 3), new Need(ModRegistry.CHEMICAL, 1), new Need(() -> Items.REDSTONE, 1)), ModRegistry.METH_CRYSTALS,
                    () -> DrogueConfig.get(DrogueConfig.METH_YIELD), () -> DrogueConfig.get(DrogueConfig.METH_LAB_SECONDS)));

    private final Map<Item, Integer> contents = new LinkedHashMap<>();
    private ItemStack output = ItemStack.EMPTY;
    private String activeId = "";
    private int progress;

    public LabBE(BlockPos pos, BlockState state) {
        super(ModRegistry.LAB_BE.get(), pos, state);
    }

    private static Recipe byId(String id) {
        for (Recipe r : RECIPES) if (r.id().equals(id)) return r;
        return null;
    }

    // ------------------------------------------------------------------ interaction

    public void interact(Player p) {
        if (!activeId.isEmpty()) {
            Recipe r = byId(activeId);
            int total = r == null ? 1 : Math.max(1, r.seconds().get() * 20);
            p.displayClientMessage(Component.literal("§7Fabrication en cours… " + Math.min(99, progress * 100 / total) + " %"), true);
            return;
        }
        if (!output.isEmpty()) {
            ItemHandlerHelper.giveItemToPlayer(p, output);
            output = ItemStack.EMPTY;
            setChanged();
            p.displayClientMessage(Component.literal("§aProduit récupéré."), true);
            return;
        }
        ItemStack hand = p.getMainHandItem();
        if (hand.isEmpty()) {
            if (p.isShiftKeyDown() && !contents.isEmpty()) {
                for (Map.Entry<Item, Integer> e : contents.entrySet()) ItemHandlerHelper.giveItemToPlayer(p, new ItemStack(e.getKey(), e.getValue()));
                contents.clear();
                setChanged();
                p.displayClientMessage(Component.literal("§7Ingrédients repris."), true);
            } else {
                p.displayClientMessage(Component.literal(contents.isEmpty() ? "§7Laboratoire vide. Cocaïne : 4 feuilles de coca + produit chimique. Meth : 3 éphédra + produit chimique + redstone."
                        : "§7Contient : " + describe() + " (accroupi, main vide : tout reprendre)"), true);
            }
            return;
        }
        Item it = hand.getItem();
        Map<Item, Integer> next = new LinkedHashMap<>(contents);
        next.merge(it, 1, Integer::sum);
        Recipe match = null;
        for (Recipe r : RECIPES) {
            boolean fits = true;
            for (Map.Entry<Item, Integer> e : next.entrySet()) if (r.needed(e.getKey()) < e.getValue()) { fits = false; break; }
            if (fits) { match = r; break; }
        }
        if (match == null) {
            p.displayClientMessage(Component.literal("§cCet ingrédient ne va pas avec le contenu du laboratoire."), true);
            return;
        }
        hand.shrink(1);
        contents.clear();
        contents.putAll(next);
        boolean complete = true;
        for (Need n : match.needs()) if (contents.getOrDefault(n.item().get(), 0) < n.count()) { complete = false; break; }
        if (complete) {
            activeId = match.id();
            progress = 0;
            contents.clear();
            p.displayClientMessage(Component.literal("§aLa fabrication démarre."), true);
            level.playSound(null, worldPosition, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0f, 0.8f);
        } else {
            p.displayClientMessage(Component.literal("§7Ajouté : " + describe()), true);
        }
        setChanged();
    }

    private String describe() {
        List<String> parts = new ArrayList<>();
        for (Map.Entry<Item, Integer> e : contents.entrySet()) parts.add(e.getValue() + " × " + new ItemStack(e.getKey()).getHoverName().getString());
        return String.join(", ", parts);
    }

    // ------------------------------------------------------------------ fabrication

    public static void serverTick(Level level, BlockPos pos, BlockState state, LabBE be) {
        if (be.activeId.isEmpty() || !(level instanceof ServerLevel sl)) return;
        Recipe r = byId(be.activeId);
        if (r == null) { be.activeId = ""; return; }
        be.progress++;
        if (be.progress % 10 == 0) sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 2, 0.15, 0.05, 0.15, 0.01);
        if (be.progress % 40 == 0) sl.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.5f, 1.0f);
        if (be.progress >= Math.max(1, r.seconds().get() * 20)) {
            be.output = new ItemStack(r.result().get(), Math.max(1, r.yield().get()));
            be.activeId = "";
            be.progress = 0;
            sl.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.6f, 1.4f);
            be.setChanged();
        }
    }

    public void dropAll(Level level, BlockPos pos) {
        for (Map.Entry<Item, Integer> e : contents.entrySet()) Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(e.getKey(), e.getValue()));
        if (!output.isEmpty()) Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), output);
        contents.clear();
        output = ItemStack.EMPTY;
    }

    // ------------------------------------------------------------------ sauvegarde

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ListTag list = new ListTag();
        for (Map.Entry<Item, Integer> e : contents.entrySet()) {
            CompoundTag c = new CompoundTag();
            c.put("item", new ItemStack(e.getKey()).save(new CompoundTag()));
            c.putInt("n", e.getValue());
            list.add(c);
        }
        tag.put("contents", list);
        tag.put("output", output.save(new CompoundTag()));
        tag.putString("active", activeId);
        tag.putInt("progress", progress);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        contents.clear();
        for (Tag t : tag.getList("contents", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            ItemStack st = ItemStack.of(c.getCompound("item"));
            if (!st.isEmpty()) contents.put(st.getItem(), c.getInt("n"));
        }
        output = ItemStack.of(tag.getCompound("output"));
        activeId = tag.getString("active");
        progress = tag.getInt("progress");
    }
}
