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
import net.minecraft.world.phys.Vec3;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Contenu du laboratoire (écran LabMenu) : plante + produit chimique déposés dans leurs emplacements ; le bouton « Lancer » fabrique
 * tous les lots complets d'un coup (ingrédients consommés), le produit arrive dans l'emplacement de sortie.
 */
public class LabBE extends BlockEntity {
    public record Need(Supplier<? extends Item> item, int count) {}

    public record Recipe(String id, List<Need> needs, Supplier<? extends Item> result, Supplier<Integer> yield, Supplier<Integer> seconds) {
        int needed(Item it) {
            for (Need n : needs) if (n.item().get() == it) return n.count();
            return 0;
        }
    }

    /** Meth (cristaux), héroïne (poudre, à partir des capsules de pavot) et pâte de coca ; la pâte se cuit ensuite dans le four de cuisson pour donner la poudre de cocaïne. */
    public static final List<Recipe> RECIPES = List.of(
            new Recipe("paste", List.of(new Need(ModRegistry.COCA_LEAF, 4), new Need(ModRegistry.CHEMICAL, 1)), ModRegistry.COCA_PASTE,
                    () -> DrogueConfig.get(DrogueConfig.PASTE_YIELD), () -> DrogueConfig.get(DrogueConfig.PASTE_LAB_SECONDS)),
            new Recipe("meth", List.of(new Need(ModRegistry.EPHEDRA, 3), new Need(ModRegistry.CHEMICAL, 1)), ModRegistry.METH_CRYSTALS,
                    () -> DrogueConfig.get(DrogueConfig.METH_YIELD), () -> DrogueConfig.get(DrogueConfig.METH_LAB_SECONDS)),
            new Recipe("heroin", List.of(new Need(ModRegistry.POPPY_POD, 4), new Need(ModRegistry.CHEMICAL, 1)), ModRegistry.HEROIN_POWDER,
                    () -> DrogueConfig.get(DrogueConfig.HEROIN_YIELD), () -> DrogueConfig.get(DrogueConfig.HEROIN_LAB_SECONDS)),
            new Recipe("mdma", List.of(new Need(ModRegistry.SASSAFRAS_OIL, 2), new Need(ModRegistry.CHEMICAL, 1)), ModRegistry.MDMA,
                    () -> DrogueConfig.get(DrogueConfig.MDMA_YIELD), () -> DrogueConfig.get(DrogueConfig.MDMA_LAB_SECONDS)),
            new Recipe("lsd", List.of(new Need(ModRegistry.ERGOT, 4), new Need(ModRegistry.CHEMICAL, 1)), ModRegistry.LSD_LIQUID,
                    () -> DrogueConfig.get(DrogueConfig.LSD_YIELD), () -> DrogueConfig.get(DrogueConfig.LSD_LAB_SECONDS)));

    public static final int PLANT = 0, CHEM = 1, OUT = 2;
    /** Données envoyées à l'écran : progression (%), recette en cours (0 = aucune, sinon index + 1), lots en cours, lots prêts, lots maximum. */
    public static final int DATA = 5;

    private final ItemStackHandler items = newHandler(this::setChanged);
    private final List<ItemStack> overflow = new ArrayList<>();
    private String activeId = "";
    private int progress, activeLots = 1, activeQuality = Quality.DEFAULT;

    public LabBE(BlockPos pos, BlockState state) {
        super(ModRegistry.LAB_BE.get(), pos, state);
    }

    /** Emplacements : plante (feuilles de coca ou éphédra), produit chimique, produit fini. Même règle côté client et serveur. */
    public static ItemStackHandler newHandler(Runnable changed) {
        return new ItemStackHandler(3) {
            @Override protected void onContentsChanged(int slot) { changed.run(); }

            @Override public boolean isItemValid(int slot, ItemStack st) {
                return slot == PLANT ? recipeFor(st.getItem()) != null : slot == CHEM && st.is(ModRegistry.CHEMICAL.get());
            }
        };
    }

    public ItemStackHandler items() { return items; }

    private static Recipe byId(String id) {
        for (Recipe r : RECIPES) if (r.id().equals(id)) return r;
        return null;
    }

    /** Recette correspondant à la plante déposée (coca -> pâte, éphédra -> meth). */
    public static Recipe recipeFor(Item plant) {
        for (Recipe r : RECIPES) if (r.needs().get(0).item().get() == plant) return r;
        return null;
    }

    public static int lotMax() { return Math.max(1, DrogueConfig.get(DrogueConfig.LAB_LOT_MAX)); }

    private static int totalTicks(Recipe r, int lots) {
        return (int) (Math.max(1, r.seconds().get()) * 20 * (1 + 0.5 * (lots - 1)));
    }

    /** Nombre de lots complets que permet le contenu (0 s'il manque un ingrédient ou si la sortie est pleine). */
    public static int lotsReady(Recipe r, ItemStack plant, ItemStack chem, int room) {
        if (r == null) return 0;
        int lots = Math.min(lotMax(), Math.min(plant.getCount() / r.needs().get(0).count(), chem.getCount() / r.needs().get(1).count()));
        int yield = Math.max(1, r.yield().get());
        return Math.max(0, Math.min(lots, room / yield));
    }

    private int lotsReady() {
        Recipe r = recipeFor(items.getStackInSlot(PLANT).getItem());
        return r == null ? 0 : lotsReady(r, items.getStackInSlot(PLANT), items.getStackInSlot(CHEM), Stations.room(items, OUT, product(r, items.getStackInSlot(PLANT), 1)));
    }

    public boolean busy() { return !activeId.isEmpty(); }

    /** Qualité du produit : celle de la plante, une étoile de moins si on fait 3 lots ou plus d'un coup (travail bâclé). */
    public static int outputQuality(int plantQuality, int lots) { return Quality.clamp(plantQuality - (lots >= 3 ? 1 : 0)); }

    /** Modèle du produit fini pour cette plante (qualité comprise). */
    public static ItemStack product(Recipe r, ItemStack plant, int lots) { return Quality.set(new ItemStack(r.result().get()), outputQuality(Quality.get(plant), lots)); }

    /** Bouton « Lancer » de l'écran : fabrique tous les lots complets (plus de lots = plus de produit, +50 % de temps par lot en plus). */
    public void start(Player p) {
        if (busy()) return;
        Recipe r = recipeFor(items.getStackInSlot(PLANT).getItem());
        int lots = lotsReady();
        if (r == null || lots < 1) {
            p.displayClientMessage(Component.literal("§cIl manque des ingrédients pour un lot (ou la sortie est pleine)."), true);
            return;
        }
        activeQuality = outputQuality(Quality.get(items.getStackInSlot(PLANT)), lots);
        items.extractItem(PLANT, r.needs().get(0).count() * lots, false);
        items.extractItem(CHEM, r.needs().get(1).count() * lots, false);
        activeId = r.id();
        activeLots = lots;
        progress = 0;
        level.playSound(null, worldPosition, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0f, 0.8f);
        setChanged();
    }

    /** Valeurs lues par l'écran (voir DATA). */
    public int data(int i) {
        Recipe r = byId(activeId);
        return switch (i) {
            case 0 -> r == null ? 0 : Math.min(100, progress * 100 / Math.max(1, totalTicks(r, activeLots)));
            case 1 -> r == null ? 0 : RECIPES.indexOf(r) + 1;
            case 2 -> activeLots;
            case 3 -> lotsReady();
            case 4 -> lotMax();
            default -> 0;
        };
    }

    @Override
    public void onLoad() {
        super.onLoad();
        Stations.dropOverflow(level, worldPosition, overflow);
    }

    // ------------------------------------------------------------------ fabrication

    public static void serverTick(Level level, BlockPos pos, BlockState state, LabBE be) {
        if (!(level instanceof ServerLevel sl)) return;
        if (be.activeId.isEmpty()) {
            if (state.getValue(LabBlock.ANIM) != 0) LabBlock.setAnim(sl, pos, state, 0);
            return;
        }
        Recipe r = byId(be.activeId);
        if (r == null) { be.activeId = ""; return; }
        be.progress++;
        if (be.progress % 4 == 1) LabBlock.setAnim(sl, pos, state, be.progress / 4 % 3 + 1);
        Direction f = state.getValue(LabBlock.FACING);
        Direction right = LabBlock.right(f);
        boolean meth = r.id().equals("meth") || r.id().equals("mdma"), heroin = r.id().equals("heroin") || r.id().equals("lsd");
        // flamme sous le ballon (gauche), bulles colorées dans le ballon, vapeur de la cuve (droite)
        if (be.progress % 4 == 0) { Vec3 v = spot(pos, f, right, 7.0, 19.8, 9.0); sl.sendParticles(ParticleTypes.SMALL_FLAME, v.x, v.y, v.z, 1, 0.08, 0.02, 0.08, 0.002); }
        if (be.progress % 3 == 0) { Vec3 v = spot(pos, f, right, 7.0, 23.0, 9.0); sl.sendParticles(ParticleTypes.BUBBLE_POP, v.x, v.y, v.z, 2, 0.12, 0.1, 0.12, 0.0); }
        if (be.progress % 6 == 0) { Vec3 v = spot(pos, f, right, 7.0, 25.0, 9.0);
            sl.sendParticles(new net.minecraft.core.particles.DustParticleOptions(meth ? new org.joml.Vector3f(0.35f, 0.8f, 1.0f) : heroin ? new org.joml.Vector3f(0.55f, 0.36f, 0.18f) : new org.joml.Vector3f(0.85f, 0.75f, 0.5f), 1.0f), v.x, v.y, v.z, 3, 0.12, 0.12, 0.12, 0.0); }
        if (be.progress % 10 == 0) { Vec3 v = spot(pos, f, right, 27.6, 30.5, 8.4); sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, v.x, v.y, v.z, 1, 0.08, 0.05, 0.08, 0.01); }
        if (be.progress % 30 == 0) sl.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.5f, 1.0f);
        if (be.progress % 50 == 0) sl.playSound(null, pos, SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundSource.BLOCKS, 0.6f, 1.2f);
        if (be.progress >= totalTicks(r, be.activeLots)) {
            int made = Math.max(1, r.yield().get()) * be.activeLots;
            boolean fire = be.activeLots >= 3 && !Odor.vented(sl, pos) && sl.random.nextInt(100) < DrogueConfig.get(DrogueConfig.LAB_FIRE_CHANCE);
            if (fire) made -= made / 4;
            Stations.addOutput(be.items, OUT, Quality.set(new ItemStack(r.result().get()), be.activeQuality), made);
            be.activeId = "";
            be.progress = 0;
            sl.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.6f, 1.4f);
            be.setChanged();
            if (fire) accident(sl, pos);
        }
    }

    /**
     * Accident : les vapeurs s'enflamment (3 lots ou plus sans extracteur d'air). Explosion sans dégâts aux blocs, feu autour du labo, un quart du
     * produit perdu, les pompiers sont prévenus par les voisins.
     */
    private static void accident(ServerLevel sl, BlockPos pos) {
        sl.explode(null, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 1.6f, true, Level.ExplosionInteraction.NONE);
        for (int i = 0; i < 6; i++) {
            BlockPos f = pos.offset(sl.random.nextInt(5) - 2, sl.random.nextInt(2), sl.random.nextInt(5) - 2);
            if (sl.getBlockState(f).isAir() && net.minecraft.world.level.block.BaseFireBlock.canBePlacedAt(sl, f, Direction.UP))
                sl.setBlockAndUpdate(f, net.minecraft.world.level.block.BaseFireBlock.getState(sl, f));
        }
        Odor.alertSecours(sl.getServer(), "Explosion et incendie signalés par des voisins (fumées chimiques).", pos);
    }

    /** Point du laboratoire en coordonnées du modèle (x vers la droite 0 à 32, y 0 à 32, z 0 = fond à 16 = face), converti en position du monde. */
    private static Vec3 spot(BlockPos o, Direction f, Direction right, double x, double y, double z) {
        double u = x / 16.0 - 0.5, d = z / 16.0 - 0.5;
        return new Vec3(o.getX() + 0.5 + right.getStepX() * u + f.getStepX() * d, o.getY() + y / 16.0, o.getZ() + 0.5 + right.getStepZ() * u + f.getStepZ() * d);
    }

    public void dropAll(Level level, BlockPos pos) {
        Stations.dropAll(level, pos, items);
        Recipe r = byId(activeId);
        if (r != null) {   // fabrication interrompue : les ingrédients consommés sont rendus
            for (Need n : r.needs()) {
                ItemStack back = new ItemStack(n.item().get(), n.count() * activeLots);
                if (n == r.needs().get(0)) Quality.set(back, activeQuality);
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), back);
            }
            activeId = "";
        }
    }

    // ------------------------------------------------------------------ sauvegarde

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("items", items.serializeNBT());
        tag.putString("active", activeId);
        tag.putInt("progress", progress);
        tag.putInt("lots", activeLots);
        tag.putInt("quality", activeQuality);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("items")) items.deserializeNBT(tag.getCompound("items"));
        else {   // ancienne sauvegarde : ingrédients en vrac + produit
            for (Tag t : tag.getList("contents", Tag.TAG_COMPOUND)) {
                CompoundTag c = (CompoundTag) t;
                ItemStack st = ItemStack.of(c.getCompound("item"));
                if (st.isEmpty()) continue;
                int slot = st.is(ModRegistry.CHEMICAL.get()) ? CHEM : recipeFor(st.getItem()) != null ? PLANT : -1;
                if (slot >= 0 && items.getStackInSlot(slot).isEmpty()) Stations.legacy(items, slot, st.getItem(), c.getInt("n"), overflow);
                else overflow.add(new ItemStack(st.getItem(), Math.min(64, c.getInt("n"))));
            }
            Stations.legacy(items, OUT, ItemStack.of(tag.getCompound("output")), overflow);
        }
        activeId = tag.getString("active");
        progress = tag.getInt("progress");
        activeLots = Math.max(1, tag.getInt("lots"));
        activeQuality = tag.contains("quality") ? tag.getInt("quality") : Quality.DEFAULT;
    }
}
