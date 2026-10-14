package fr.minenorth.drogue;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.registries.ForgeRegistries;

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

    /** Meth (cristaux) et pâte de coca ; la pâte se cuit ensuite dans le four de cuisson pour donner la poudre de cocaïne. */
    public static final List<Recipe> RECIPES = List.of(
            new Recipe("paste", List.of(new Need(ModRegistry.COCA_LEAF, 4), new Need(ModRegistry.CHEMICAL, 1)), ModRegistry.COCA_PASTE,
                    () -> DrogueConfig.get(DrogueConfig.PASTE_YIELD), () -> DrogueConfig.get(DrogueConfig.PASTE_LAB_SECONDS)),
            new Recipe("meth", List.of(new Need(ModRegistry.EPHEDRA, 3), new Need(ModRegistry.CHEMICAL, 1)), ModRegistry.METH_CRYSTALS,
                    () -> DrogueConfig.get(DrogueConfig.METH_YIELD), () -> DrogueConfig.get(DrogueConfig.METH_LAB_SECONDS)));

    private final Map<Item, Integer> contents = new LinkedHashMap<>();
    private ItemStack output = ItemStack.EMPTY;
    private String activeId = "";
    private int progress, activeLots = 1;

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
            int total = r == null ? 1 : totalTicks(r, activeLots);
            p.displayClientMessage(Component.literal("§7Fabrication en cours (" + activeLots + " lot(s))… " + Math.min(99, progress * 100 / total) + " %"), true);
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
            } else if (contents.isEmpty()) {
                p.displayClientMessage(Component.literal("§7Laboratoire vide : voir le chat."), true);
                chat(p, "§6Laboratoire", "§7Meth : §f3 éphédra + 1 produit chimique", "§7Pâte de coca : §f4 feuilles de coca + 1 produit chimique",
                        "§7Jusqu'à §f" + lotMax() + " lots§7 d'un coup : ajoutez plus d'ingrédients,", "§7puis main vide pour lancer.");
            } else {
                startBatch(p);
            }
            return;
        }
        Item it = hand.getItem();
        Map<Item, Integer> next = new LinkedHashMap<>(contents);
        next.merge(it, 1, Integer::sum);
        Recipe match = null;
        for (Recipe r : RECIPES) {
            boolean fits = true;
            for (Map.Entry<Item, Integer> e : next.entrySet()) if (r.needed(e.getKey()) * lotMax() < e.getValue()) { fits = false; break; }
            if (fits) { match = r; break; }
        }
        if (match == null) {
            p.displayClientMessage(Component.literal("§cCet ingrédient ne va pas avec le contenu du laboratoire."), true);
            return;
        }
        hand.shrink(1);
        contents.clear();
        contents.putAll(next);
        int lots = lotsReady(match);
        p.displayClientMessage(Component.literal("§7Ajouté : " + describe() + (lots > 0 ? " §8| §a" + lots + " lot(s) prêt(s)" : "")), true);
        if (lots > 0) chat(p, "§a" + lots + " lot(s) prêt(s)§7 : main vide pour lancer.");
        setChanged();
    }

    private static int lotMax() { return Math.max(1, DrogueConfig.get(DrogueConfig.LAB_LOT_MAX)); }

    private static int totalTicks(Recipe r, int lots) {
        return (int) (Math.max(1, r.seconds().get()) * 20 * (1 + 0.5 * (lots - 1)));
    }

    /** Nombre de lots complets que permet le contenu pour cette recette (0 s'il manque un ingrédient). */
    private int lotsReady(Recipe r) {
        int lots = lotMax();
        for (Need n : r.needs()) lots = Math.min(lots, contents.getOrDefault(n.item().get(), 0) / n.count());
        return lots;
    }

    /** Lance la fabrication avec tous les lots complets : plus de lots = plus de produit, mais plus long (+50 % de temps par lot en plus). */
    private void startBatch(Player p) {
        for (Recipe r : RECIPES) {
            boolean only = true;
            for (Map.Entry<Item, Integer> e : contents.entrySet()) if (r.needed(e.getKey()) == 0) { only = false; break; }
            if (!only) continue;
            int lots = lotsReady(r);
            if (lots < 1) continue;
            for (Need n : r.needs()) contents.merge(n.item().get(), -n.count() * lots, Integer::sum);
            contents.values().removeIf(v -> v <= 0);
            activeId = r.id();
            activeLots = lots;
            progress = 0;
            p.displayClientMessage(Component.literal("§aLa fabrication démarre : " + lots + " lot(s)."), true);
            level.playSound(null, worldPosition, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0f, 0.8f);
            setChanged();
            return;
        }
        p.displayClientMessage(Component.literal("§cIl manque des ingrédients pour un lot."), true);
        chat(p, "§7Contient : §f" + describe(), "§7Accroupi + main vide : tout reprendre.");
    }

    /** Messages longs : une phrase courte par ligne dans le chat (la barre d'action déborde de l'écran). */
    private static void chat(Player p, String... lines) {
        for (String l : lines) p.sendSystemMessage(Component.literal(l));
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
        Direction f = state.getValue(LabBlock.FACING);
        Direction right = LabBlock.right(f);
        double cx = pos.getX() + 0.5 + right.getStepX() * 0.5, cz = pos.getZ() + 0.5 + right.getStepZ() * 0.5;
        boolean meth = r.id().equals("meth");
        // bec bunsen sous le ballon + bulles colorées dans le ballon + fumée du condenseur
        if (be.progress % 4 == 0) sl.sendParticles(ParticleTypes.SMALL_FLAME, cx - right.getStepX() * 0.5, pos.getY() + 0.95, cz - right.getStepZ() * 0.5, 1, 0.05, 0.02, 0.05, 0.002);
        if (be.progress % 3 == 0) sl.sendParticles(ParticleTypes.BUBBLE_POP, cx, pos.getY() + 1.5, cz, 2, 0.12, 0.1, 0.12, 0.0);
        if (be.progress % 6 == 0) sl.sendParticles(new net.minecraft.core.particles.DustParticleOptions(meth ? new org.joml.Vector3f(0.35f, 0.8f, 1.0f) : new org.joml.Vector3f(0.85f, 0.75f, 0.5f), 1.0f),
                cx, pos.getY() + 1.7, cz, 3, 0.12, 0.12, 0.12, 0.0);
        if (be.progress % 10 == 0) sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, cx, pos.getY() + 2.05, cz, 1, 0.08, 0.05, 0.08, 0.01);
        if (be.progress % 30 == 0) sl.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.5f, 1.0f);
        if (be.progress % 50 == 0) sl.playSound(null, pos, SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundSource.BLOCKS, 0.6f, 1.2f);
        if (be.progress >= totalTicks(r, be.activeLots)) {
            be.output = new ItemStack(r.result().get(), Math.max(1, r.yield().get()) * be.activeLots);
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
        tag.putInt("lots", activeLots);
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
        activeLots = Math.max(1, tag.getInt("lots"));
    }
}
