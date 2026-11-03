package fr.minenorth.drogue;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Tente de culture, 2 blocs de haut, en trois tailles : 2 × 2 (4 plantes), 1 × 2 (2 plantes, dans le sens est-ouest ou nord-sud) et 1 × 1 (1 plante).
 * Les cases du bas portent chacune une plante (cannabis ou coca, graines posées en clic droit, terre à arroser), les cases du haut portent la lampe UV
 * qui double la vitesse de pousse. Tout se fait objet en main, comme sur un vrai plant : clic droit à main nue sur le haut = ouvrir / fermer
 * (toile noire opaque ou filet transparent) ; sur un pot : terreau, graines (cannabis, coca, éphédra, pavot), eau, puis récolte à main nue.
 * Casser un bloc retire toute la tente (la case d'origine rend la tente, la lampe est rendue aussi).
 */
public class TentBlock extends Block {
    public enum Plant implements StringRepresentable {
        NONE, CANNABIS, COCA, EPHEDRA, POPPY, MUSHROOM;

        @Override public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
    }

    /** Taille : S22 = 2 × 2, S11 = 1 × 1, S12 = 1 × 2 (sens nord-sud, ou est-ouest si ALONGX). */
    public enum Layout implements StringRepresentable {
        S22, S11, S12;

        @Override public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
    }

    public static final EnumProperty<Layout> LAYOUT = EnumProperty.create("layout", Layout.class);
    public static final BooleanProperty TOP = BooleanProperty.create("top");
    /** Case dans la tente : 2 × 2 : 0 = nord-ouest, 1 = nord-est, 2 = sud-ouest, 3 = sud-est (c = x + 2 z) ; 1 × 2 : 0 ou 1 le long de l'axe. */
    public static final IntegerProperty CORNER = IntegerProperty.create("corner", 0, 3);
    /** Tente 1 × 2 posée dans le sens est-ouest. */
    public static final BooleanProperty ALONGX = BooleanProperty.create("alongx");
    public static final EnumProperty<Plant> PLANT = EnumProperty.create("plant", Plant.class);
    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
    public static final BooleanProperty LAMP = BooleanProperty.create("lamp");
    /** Tente ouverte (filet transparent, accès aux plantes) ou fermée (toile opaque). */
    public static final BooleanProperty OPEN = BooleanProperty.create("open");
    /** Terreau dans le pot : il faut en mettre pour planter ; il est consommé à la récolte. */
    public static final BooleanProperty SOIL = BooleanProperty.create("soil");
    /** Eau de la terre (0 = sèche) : un stade de pousse consomme 1 ; seau d'eau = 3, bouteille d'eau = 1, arrosoir = 3 pour toute la tente. */
    public static final IntegerProperty WATER = IntegerProperty.create("water", 0, 3);
    /*
     * Équipements sans nouvelle propriété (chaque propriété en plus multiplie les états du bloc) : on réutilise celles qui ne servent pas sur la case.
     * Engrais dans un pot = LAMP sur une case du bas ; lampe LED = SOIL sur une case du haut (avec LAMP) ; goutte-à-goutte = WATER > 0 sur une case du haut.
     */
    public static boolean fert(BlockState s) { return !s.getValue(TOP) && s.getValue(LAMP); }

    public static boolean led(BlockState top) { return top.getValue(TOP) && top.getValue(LAMP) && top.getValue(SOIL); }

    public static boolean drip(BlockState top) { return top.getValue(TOP) && top.getValue(WATER) > 0; }

    public TentBlock() {
        super(BlockBehaviour.Properties.of().strength(2.0f).sound(SoundType.WOOL).noOcclusion().randomTicks()
                .lightLevel(s -> s.getValue(TOP) && s.getValue(LAMP) ? 12 : 0));
        registerDefaultState(stateDefinition.any().setValue(LAYOUT, Layout.S22).setValue(TOP, false).setValue(CORNER, 0).setValue(ALONGX, false)
                .setValue(PLANT, Plant.NONE).setValue(AGE, 0).setValue(LAMP, false).setValue(OPEN, false).setValue(WATER, 0).setValue(SOIL, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(LAYOUT, TOP, CORNER, ALONGX, PLANT, AGE, LAMP, OPEN, WATER, SOIL); }

    // ------------------------------------------------------------------ géométrie

    /** Largeur (axe x) de la tente. */
    public static int width(Layout l, boolean alongx) { return l == Layout.S22 ? 2 : l == Layout.S12 && alongx ? 2 : 1; }

    /** Profondeur (axe z) de la tente. */
    public static int depth(Layout l, boolean alongx) { return l == Layout.S22 ? 2 : l == Layout.S12 && !alongx ? 2 : 1; }

    /** Numéro de case pour la position (i, j) dans la tente. */
    public static int cornerOf(Layout l, boolean alongx, int i, int j) { return l == Layout.S22 ? i + 2 * j : l == Layout.S12 ? (alongx ? i : j) : 0; }

    private static int cellX(BlockState s) {
        int c = s.getValue(CORNER);
        return switch (s.getValue(LAYOUT)) { case S22 -> c % 2; case S12 -> s.getValue(ALONGX) ? c : 0; default -> 0; };
    }

    private static int cellZ(BlockState s) {
        int c = s.getValue(CORNER);
        return switch (s.getValue(LAYOUT)) { case S22 -> c / 2; case S12 -> s.getValue(ALONGX) ? 0 : c; default -> 0; };
    }

    /** Case d'origine (bas, coin nord-ouest) de la tente à laquelle appartient ce bloc. */
    public static BlockPos origin(BlockPos pos, BlockState s) {
        return pos.offset(-cellX(s), s.getValue(TOP) ? -1 : 0, -cellZ(s));
    }

    /** Toutes les cases (bas et haut) de la tente. */
    private static List<BlockPos> cells(BlockPos origin, BlockState s) {
        Layout l = s.getValue(LAYOUT);
        boolean ax = s.getValue(ALONGX);
        List<BlockPos> out = new ArrayList<>();
        for (int i = 0; i < width(l, ax); i++) for (int y = 0; y < 2; y++) for (int j = 0; j < depth(l, ax); j++) out.add(origin.offset(i, y, j));
        return out;
    }

    // ------------------------------------------------------------------ pousse

    @Override @SuppressWarnings("deprecation")
    public boolean isRandomlyTicking(BlockState s) { return !s.getValue(TOP) && s.getValue(PLANT) != Plant.NONE && s.getValue(AGE) < 3 && s.getValue(WATER) > 0; }

    @Override @SuppressWarnings("deprecation")
    public void randomTick(BlockState s, ServerLevel level, BlockPos pos, RandomSource rnd) {
        if (!isRandomlyTicking(s)) return;
        BlockState above = level.getBlockState(pos.above());
        boolean lamp = above.getBlock() == this && above.getValue(LAMP);
        boolean drip = above.getBlock() == this && drip(above);
        if (s.getValue(PLANT) == Plant.MUSHROOM) {
            // les champignons veulent le noir : tente fermée, sans lampe
            if (lamp || s.getValue(OPEN)) return;
            if (rnd.nextInt(2) == 0) level.setBlock(pos, s.setValue(AGE, s.getValue(AGE) + 1).setValue(WATER, drip ? 3 : s.getValue(WATER) - 1), 2);
            return;
        }
        // lampe LED : pousse encore plus vite (une fois sur trois, la plante gagne deux stades d'un coup)
        int grow = above.getBlock() == this && led(above) && rnd.nextInt(3) == 0 ? 2 : 1;
        if (lamp || rnd.nextInt(2) == 0) level.setBlock(pos, s.setValue(AGE, Math.min(3, s.getValue(AGE) + grow)).setValue(WATER, drip ? 3 : s.getValue(WATER) - 1), 2);
    }

    // ------------------------------------------------------------------ interaction (clics directs, objet en main)

    @Override @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState s, Level level, BlockPos pos, Player p, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        ItemStack st = p.getMainHandItem();
        boolean creative = p.getAbilities().instabuild;
        BlockPos o = origin(pos, s);
        BlockState topOfOrigin = level.getBlockState(o.above());
        boolean lamp = topOfOrigin.getBlock() == this && topOfOrigin.getValue(LAMP);
        boolean open = s.getValue(OPEN);

        boolean led = topOfOrigin.getBlock() == this && led(topOfOrigin);
        boolean drip = topOfOrigin.getBlock() == this && drip(topOfOrigin);
        if (st.is(ModRegistry.UV_LAMP.get()) || st.is(ModRegistry.LED_LAMP.get())) {
            boolean wantLed = st.is(ModRegistry.LED_LAMP.get());
            if (lamp && led == wantLed) { msg(p, "§7Cette lampe est déjà installée."); return InteractionResult.CONSUME; }
            if (lamp && !creative) ItemHandlerHelper.giveItemToPlayer(p, new ItemStack(led ? ModRegistry.LED_LAMP.get() : ModRegistry.UV_LAMP.get()));
            setTop(level, o, s, LAMP, true);
            setTop(level, o, s, SOIL, wantLed);
            if (!creative) st.shrink(1);
            level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.6f, 1.6f);
            msg(p, wantLed ? "§bLampe LED installée : pousse encore plus rapide et récolte de meilleure qualité (+2 étoiles)."
                    : "§dLampe UV installée : les plantes poussent deux fois plus vite (+1 étoile à la récolte).");
            return InteractionResult.CONSUME;
        }
        if (st.is(ModRegistry.DRIP_KIT.get())) {
            if (drip) { msg(p, "§7Un goutte-à-goutte est déjà installé."); return InteractionResult.CONSUME; }
            setTopWater(level, o, s, 1);
            for (BlockPos c : pots(o, s)) {
                BlockState cs = level.getBlockState(c);
                if (cs.getBlock() == this && hasSoil(cs)) level.setBlock(c, cs.setValue(WATER, 3), 3);
            }
            if (!creative) st.shrink(1);
            level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.6f, 1.6f);
            msg(p, "§bGoutte-à-goutte installé : la terre reste humide toute seule.");
            return InteractionResult.CONSUME;
        }
        if (st.isEmpty() && p.isShiftKeyDown() && (lamp || drip)) {
            if (lamp) {
                setTop(level, o, s, LAMP, false);
                setTop(level, o, s, SOIL, false);
                ItemHandlerHelper.giveItemToPlayer(p, new ItemStack(led ? ModRegistry.LED_LAMP.get() : ModRegistry.UV_LAMP.get()));
                msg(p, "§7Lampe retirée.");
            } else {
                setTopWater(level, o, s, 0);
                ItemHandlerHelper.giveItemToPlayer(p, new ItemStack(ModRegistry.DRIP_KIT.get()));
                msg(p, "§7Goutte-à-goutte retiré.");
            }
            return InteractionResult.CONSUME;
        }
        if (s.getValue(TOP)) {
            if (st.isEmpty()) {          // ouvrir / fermer la tente
                setOpen(level, o, s, !open);
                level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.9f, open ? 0.8f : 1.3f);
                msg(p, open ? "§7Tente fermée." : "§aTente ouverte : terreau, graines, eau et récolte sur le bas.");
            } else {
                msg(p, lamp ? "§dLampe allumée. Main vide : ouvrir / fermer ; accroupi : retirer la lampe (puis le goutte-à-goutte)."
                        : "§7Main vide : ouvrir / fermer la tente. Lampe UV ou LED, goutte-à-goutte en main : l'installer.");
            }
            return InteractionResult.CONSUME;
        }
        if (!open) { msg(p, "§7Tente fermée : clic droit à main nue sur le haut pour l'ouvrir."); return InteractionResult.CONSUME; }

        // terreau : à mettre dans un pot vide avant de planter
        if (st.is(ModRegistry.SOIL.get())) {
            if (hasSoil(s)) { msg(p, "§7Ce pot a déjà du terreau."); return InteractionResult.CONSUME; }
            if (!creative) st.shrink(1);
            level.setBlock(pos, s.setValue(SOIL, true), 3);
            level.playSound(null, pos, SoundEvents.ROOTED_DIRT_PLACE, SoundSource.BLOCKS, 0.9f, 0.9f);
            return InteractionResult.CONSUME;
        }
        if (st.is(ModRegistry.FERTILIZER.get())) {
            if (!hasSoil(s)) { msg(p, "§7Pot vide : mettez d'abord du terreau."); return InteractionResult.CONSUME; }
            if (fert(s)) { msg(p, "§7Ce pot a déjà de l'engrais."); return InteractionResult.CONSUME; }
            if (!creative) st.shrink(1);
            level.setBlock(pos, s.setValue(LAMP, true), 3);
            level.playSound(null, pos, SoundEvents.BONE_MEAL_USE, SoundSource.BLOCKS, 0.9f, 1.0f);
            msg(p, "§aEngrais ajouté : +1 étoile à la récolte.");
            return InteractionResult.CONSUME;
        }
        boolean watering = st.is(ModRegistry.WATERING_CAN.get()) || st.is(Items.WATER_BUCKET) || waterBottle(st);
        if (watering && !hasSoil(s)) { msg(p, "§7Pot vide : mettez d'abord du terreau."); return InteractionResult.CONSUME; }
        // arrosoir infini : toute la tente d'un coup
        if (st.is(ModRegistry.WATERING_CAN.get())) {
            int count = 0;
            for (BlockPos c : pots(o, s)) {
                BlockState cs = level.getBlockState(c);
                if (cs.getBlock() == this && hasSoil(cs) && cs.getValue(WATER) < 3) { level.setBlock(c, cs.setValue(WATER, 3), 3); splash(level, c); count++; }
            }
            if (count == 0) { msg(p, "§bLa terre est déjà bien humide."); return InteractionResult.CONSUME; }
            level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.7f, 1.4f);
            WateringCanItem.pour(st, level);                         // l'arrosoir se penche (modèles inclinés côté client)
            return InteractionResult.CONSUME;
        }
        // arrosage : seau d'eau (3 stades) ou bouteille d'eau (1 stade)
        if (watering) {
            boolean bucket = st.is(Items.WATER_BUCKET);
            int now = s.getValue(WATER);
            if (now >= 3) { msg(p, "§bLa terre est déjà bien humide."); return InteractionResult.CONSUME; }
            level.setBlock(pos, s.setValue(WATER, bucket ? 3 : now + 1), 3);
            level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.8f, 1.2f);
            splash(level, pos);
            if (!creative) p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(bucket ? Items.BUCKET : Items.GLASS_BOTTLE));
            return InteractionResult.CONSUME;
        }
        Plant plant = s.getValue(PLANT);
        if (plant == Plant.NONE) {
            if (!s.getValue(SOIL)) { msg(p, "§7Pot vide : mettez d'abord du terreau, puis plantez."); return InteractionResult.CONSUME; }
            Plant want = Plant.NONE;
            for (Plant k : Plant.values()) if (k != Plant.NONE && st.is(seedOf(k))) want = k;
            if (want == Plant.NONE) { msg(p, "§7Pot avec terreau : plantez des graines (cannabis, coca, éphédra, pavot) ou des spores, puis arrosez."); return InteractionResult.CONSUME; }
            if (!creative) st.shrink(1);
            level.setBlock(pos, s.setValue(PLANT, want).setValue(AGE, 0), 3);
            level.playSound(null, pos, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0f, 1.0f);
            return InteractionResult.CONSUME;
        }
        int age = s.getValue(AGE);
        if (age < 3) {
            int w = s.getValue(WATER);
            boolean dark = plant == Plant.MUSHROOM;
            msg(p, w == 0 ? "§cLa terre est sèche : arrosez (arrosoir, seau ou bouteille d'eau) pour que la plante pousse."
                    : dark && (lamp || open) ? "§7Les champignons ne poussent que dans le noir : fermez la tente et retirez la lampe."
                    : "§7Pousse en cours (" + (age + 1) + "/4), eau : " + w + "/3 · qualité prévue " + Quality.stars(harvestQuality(plant, lamp, led, fert(s)))
                    + (lamp || dark ? "" : " — plus lente sans lampe") + ".");
            return InteractionResult.CONSUME;
        }
        // récolte à main nue (ou avec n'importe quel objet qui n'a pas d'usage ici)
        int q = harvestQuality(plant, lamp, led, fert(s));
        ItemHandlerHelper.giveItemToPlayer(p, Quality.set(new ItemStack(cropOf(plant), leafCount(level.random)), q));
        if (level.random.nextInt(2) == 0) ItemHandlerHelper.giveItemToPlayer(p, new ItemStack(seedOf(plant)));
        msg(p, "§aRécolte : qualité §6" + Quality.stars(q) + " §a(" + Quality.name(q) + ").");
        level.setBlock(pos, s.setValue(PLANT, Plant.NONE).setValue(AGE, 0).setValue(SOIL, false).setValue(LAMP, false).setValue(WATER, 0), 3);
        level.playSound(null, pos, SoundEvents.GRASS_BREAK, SoundSource.BLOCKS, 1.0f, 1.0f);
        if (level instanceof ServerLevel sl)
            sl.sendParticles(new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK,
                    net.minecraft.world.level.block.Blocks.GRASS.defaultBlockState()), pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 16, 0.2, 0.25, 0.2, 0.0);
        return InteractionResult.CONSUME;
    }

    /** Pots (cases du bas) de la tente. */
    public static List<BlockPos> pots(BlockPos origin, BlockState s) {
        Layout l = s.getValue(LAYOUT);
        boolean ax = s.getValue(ALONGX);
        List<BlockPos> out = new ArrayList<>();
        for (int i = 0; i < width(l, ax); i++) for (int j = 0; j < depth(l, ax); j++) out.add(origin.offset(i, 0, j));
        return out;
    }

    private static boolean waterBottle(ItemStack st) { return st.is(Items.POTION) && PotionUtils.getPotion(st) == Potions.WATER; }

    private static boolean hasSoil(BlockState c) { return c.getValue(SOIL) || c.getValue(PLANT) != Plant.NONE; }

    public static net.minecraft.world.item.Item seedOf(Plant plant) {
        return switch (plant) {
            case CANNABIS -> ModRegistry.CANNABIS_SEEDS.get();
            case COCA -> ModRegistry.COCA_SEEDS.get();
            case POPPY -> ModRegistry.POPPY_SEEDS.get();
            case MUSHROOM -> ModRegistry.MUSHROOM_SPORES.get();
            default -> ModRegistry.EPHEDRA_SEEDS.get();
        };
    }

    public static net.minecraft.world.item.Item cropOf(Plant plant) {
        return switch (plant) {
            case CANNABIS -> ModRegistry.CANNABIS_LEAF.get();
            case COCA -> ModRegistry.COCA_LEAF.get();
            case POPPY -> ModRegistry.POPPY_POD.get();
            case MUSHROOM -> ModRegistry.MAGIC_MUSHROOM.get();
            default -> ModRegistry.EPHEDRA.get();
        };
    }

    private static void splash(Level level, BlockPos c) {
        if (level instanceof ServerLevel sl)
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.FALLING_WATER, c.getX() + 0.5, c.getY() + 1.4, c.getZ() + 0.5, 14, 0.22, 0.15, 0.22, 0.0);
    }

    /** Feuilles récoltées sur une plante mûre (config : feuillesMin / feuillesMax). */
    private static int leafCount(net.minecraft.util.RandomSource rnd) {
        int min = Math.max(1, DrogueConfig.get(DrogueConfig.HARVEST_MIN)), max = Math.max(min, DrogueConfig.get(DrogueConfig.HARVEST_MAX));
        return min + rnd.nextInt(max - min + 1);
    }

    private static void msg(Player p, String text) { p.displayClientMessage(Component.literal(text), true); }

    /** Qualité de la récolte : 2 étoiles de base, +1 lampe UV (+2 LED), +1 engrais. Champignons : 3 de base (le noir leur suffit), +1 engrais. */
    public static int harvestQuality(Plant plant, boolean lamp, boolean led, boolean fert) {
        int q = plant == Plant.MUSHROOM ? 3 : 2 + (lamp ? 1 : 0) + (led ? 1 : 0);
        return Quality.clamp(q + (fert ? 1 : 0));
    }

    /** Goutte-à-goutte : WATER des cases du haut (0 = absent, 1 = installé). */
    private void setTopWater(Level level, BlockPos origin, BlockState any, int w) {
        for (BlockPos p : cells(origin, any)) {
            BlockState t = level.getBlockState(p);
            if (t.getBlock() == this && t.getValue(TOP)) level.setBlock(p, t.setValue(WATER, w), 3);
        }
    }

    /** Règle une propriété (lampe, LED) sur les cases du haut. */
    private void setTop(Level level, BlockPos origin, BlockState any, BooleanProperty prop, boolean on) {
        for (BlockPos p : cells(origin, any)) {
            BlockState t = level.getBlockState(p);
            if (t.getBlock() == this && t.getValue(TOP)) level.setBlock(p, t.setValue(prop, on), 3);
        }
    }

    // ------------------------------------------------------------------ odeur (voir Odor)

    /** Une plante qui pousse sent fort : volutes vertes au-dessus de la tente, sauf filtre à charbon à proximité. */
    @Override
    public void animateTick(BlockState s, Level level, BlockPos pos, RandomSource rnd) {
        if (s.getValue(TOP) || s.getValue(PLANT) == Plant.NONE || s.getValue(AGE) < 1 || rnd.nextInt(6) != 0) return;
        if (Odor.filtered(level, pos)) return;
        Odor.puff(level, pos.above(2), rnd);
    }

    /** Ouvre / ferme toute la tente. */
    private void setOpen(Level level, BlockPos origin, BlockState any, boolean open) {
        for (BlockPos p : cells(origin, any)) {
            BlockState t = level.getBlockState(p);
            if (t.getBlock() == this) level.setBlock(p, t.setValue(OPEN, open), 3);
        }
    }

    // ------------------------------------------------------------------ casse

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState s, Player player) {
        if (!level.isClientSide) {
            for (BlockPos p : cells(origin(pos, s), s)) {
                if (!p.equals(pos) && level.getBlockState(p).getBlock() == this) level.destroyBlock(p, !player.isCreative());
            }
        }
        super.playerWillDestroy(level, pos, s, player);
    }
}
