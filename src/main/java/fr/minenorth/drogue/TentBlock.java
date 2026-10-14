package fr.minenorth.drogue;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
 * (objet « lampe UV », clic droit) qui double la vitesse de pousse. La tente se ferme (toile noire opaque) et s'ouvre (filet transparent, on y accède)
 * par un clic droit à main nue sur le haut. Plante mûre : clic droit à main nue sur la case du bas, tente ouverte.
 * Casser un bloc retire toute la tente (la case d'origine rend la tente, la lampe est rendue aussi).
 */
public class TentBlock extends Block {
    public enum Plant implements StringRepresentable {
        NONE, CANNABIS, COCA, EPHEDRA;

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
    /** Eau de la terre (0 = sèche) : un stade de pousse consomme 1 ; seau d'eau = 3, bouteille d'eau = 1, arrosoir = 3 pour toute la tente. */
    public static final IntegerProperty WATER = IntegerProperty.create("water", 0, 3);

    public TentBlock() {
        super(BlockBehaviour.Properties.of().strength(2.0f).sound(SoundType.WOOL).noOcclusion().randomTicks()
                .lightLevel(s -> s.getValue(TOP) && s.getValue(LAMP) ? 12 : 0));
        registerDefaultState(stateDefinition.any().setValue(LAYOUT, Layout.S22).setValue(TOP, false).setValue(CORNER, 0).setValue(ALONGX, false)
                .setValue(PLANT, Plant.NONE).setValue(AGE, 0).setValue(LAMP, false).setValue(OPEN, false).setValue(WATER, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(LAYOUT, TOP, CORNER, ALONGX, PLANT, AGE, LAMP, OPEN, WATER); }

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
        if (lamp || rnd.nextInt(2) == 0) level.setBlock(pos, s.setValue(AGE, s.getValue(AGE) + 1).setValue(WATER, s.getValue(WATER) - 1), 2);
    }

    // ------------------------------------------------------------------ interaction

    @Override @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState s, Level level, BlockPos pos, Player p, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        ItemStack st = p.getMainHandItem();
        BlockPos o = origin(pos, s);
        BlockState topOfOrigin = level.getBlockState(o.above());
        boolean lamp = topOfOrigin.getBlock() == this && topOfOrigin.getValue(LAMP);
        boolean open = s.getValue(OPEN);

        if (st.is(ModRegistry.UV_LAMP.get())) {
            if (lamp) { msg(p, "§7Une lampe UV est déjà installée."); return InteractionResult.CONSUME; }
            setLamp(level, o, s, true);
            st.shrink(1);
            level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.6f, 1.6f);
            msg(p, "§dLampe UV installée : les plantes poussent deux fois plus vite.");
            return InteractionResult.CONSUME;
        }
        if (st.isEmpty() && p.isShiftKeyDown() && lamp) {
            setLamp(level, o, s, false);
            ItemHandlerHelper.giveItemToPlayer(p, new ItemStack(ModRegistry.UV_LAMP.get()));
            msg(p, "§7Lampe UV retirée.");
            return InteractionResult.CONSUME;
        }
        if (s.getValue(TOP)) {
            if (st.isEmpty()) {          // ouvrir / fermer la tente
                setOpen(level, o, s, !open);
                level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.9f, open ? 0.8f : 1.3f);
                msg(p, open ? "§7Tente fermée." : "§aTente ouverte : plantez, arrosez, récoltez (clic droit sur le bas).");
            } else {
                msg(p, lamp ? "§dLampe UV allumée. Main vide : ouvrir / fermer ; accroupi : retirer la lampe." : "§7Main vide : ouvrir / fermer la tente. Objet « Lampe UV » : l'installer.");
            }
            return InteractionResult.CONSUME;
        }
        if (!open) { msg(p, "§7Tente fermée : clic droit à main nue sur le haut pour l'ouvrir."); return InteractionResult.CONSUME; }

        // arrosoir infini : toute la tente d'un coup
        if (st.is(ModRegistry.WATERING_CAN.get())) {
            int count = 0;
            for (BlockPos c : cells(o, s)) {
                BlockState cs = level.getBlockState(c);
                if (cs.getBlock() == this && !cs.getValue(TOP) && cs.getValue(WATER) < 3) { level.setBlock(c, cs.setValue(WATER, 3), 3); count++; }
            }
            if (count == 0) { msg(p, "§bLa terre est déjà bien humide."); return InteractionResult.CONSUME; }
            level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.7f, 1.4f);
            WateringCanItem.pour(st, level);                         // l'arrosoir se penche (modèles inclinés côté client)
            if (level instanceof ServerLevel sl) {
                for (BlockPos c : cells(o, s)) {
                    if (level.getBlockState(c).getBlock() == this && !level.getBlockState(c).getValue(TOP))
                        sl.sendParticles(net.minecraft.core.particles.ParticleTypes.FALLING_WATER, c.getX() + 0.5, c.getY() + 1.4, c.getZ() + 0.5, 14, 0.22, 0.15, 0.22, 0.0);
                }
            }
            return InteractionResult.CONSUME;
        }
        // arrosage : seau d'eau (3 stades) ou bouteille d'eau (1 stade)
        if (st.is(Items.WATER_BUCKET) || (st.is(Items.POTION) && PotionUtils.getPotion(st) == Potions.WATER)) {
            boolean bucket = st.is(Items.WATER_BUCKET);
            int now = s.getValue(WATER);
            if (now >= 3) { msg(p, "§bLa terre est déjà bien humide."); return InteractionResult.CONSUME; }
            level.setBlock(pos, s.setValue(WATER, bucket ? 3 : now + 1), 3);
            level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.8f, 1.2f);
            if (!p.getAbilities().instabuild) p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(bucket ? Items.BUCKET : Items.GLASS_BOTTLE));
            return InteractionResult.CONSUME;
        }
        Plant plant = s.getValue(PLANT);
        if (plant == Plant.NONE) {
            Plant want = st.is(ModRegistry.CANNABIS_SEEDS.get()) ? Plant.CANNABIS : st.is(ModRegistry.COCA_SEEDS.get()) ? Plant.COCA
                    : st.is(ModRegistry.EPHEDRA_SEEDS.get()) ? Plant.EPHEDRA : Plant.NONE;
            if (want == Plant.NONE) { msg(p, "§7Emplacement libre : plantez des graines de cannabis, de coca ou d'éphédra (et arrosez)."); return InteractionResult.CONSUME; }
            st.shrink(1);
            level.setBlock(pos, s.setValue(PLANT, want).setValue(AGE, 0), 3);
            level.playSound(null, pos, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0f, 1.0f);
            return InteractionResult.CONSUME;
        }
        int age = s.getValue(AGE);
        if (age < 3) {
            int w = s.getValue(WATER);
            msg(p, w == 0 ? "§cLa terre est sèche : arrosez (arrosoir, seau ou bouteille d'eau) pour que la plante pousse."
                    : "§7Pousse en cours (" + (age + 1) + "/4), eau : " + w + "/3" + (lamp ? "" : " — plus lente sans lampe UV") + ".");
            return InteractionResult.CONSUME;
        }
        // récolte
        net.minecraft.world.item.Item crop = plant == Plant.CANNABIS ? ModRegistry.CANNABIS_LEAF.get() : plant == Plant.COCA ? ModRegistry.COCA_LEAF.get() : ModRegistry.EPHEDRA.get();
        net.minecraft.world.item.Item seed = plant == Plant.CANNABIS ? ModRegistry.CANNABIS_SEEDS.get() : plant == Plant.COCA ? ModRegistry.COCA_SEEDS.get() : ModRegistry.EPHEDRA_SEEDS.get();
        ItemHandlerHelper.giveItemToPlayer(p, new ItemStack(crop, leafCount(level.random)));
        if (level.random.nextInt(2) == 0) ItemHandlerHelper.giveItemToPlayer(p, new ItemStack(seed));
        level.setBlock(pos, s.setValue(PLANT, Plant.NONE).setValue(AGE, 0), 3);
        level.playSound(null, pos, SoundEvents.GRASS_BREAK, SoundSource.BLOCKS, 1.0f, 1.0f);
        return InteractionResult.CONSUME;
    }

    /** Feuilles récoltées sur une plante mûre (config : feuillesMin / feuillesMax). */
    private static int leafCount(net.minecraft.util.RandomSource rnd) {
        int min = Math.max(1, DrogueConfig.get(DrogueConfig.HARVEST_MIN)), max = Math.max(min, DrogueConfig.get(DrogueConfig.HARVEST_MAX));
        return min + rnd.nextInt(max - min + 1);
    }

    private static void msg(Player p, String text) { p.displayClientMessage(Component.literal(text), true); }

    /** Allume / éteint la lampe sur les cases du haut. */
    private void setLamp(Level level, BlockPos origin, BlockState any, boolean on) {
        for (BlockPos p : cells(origin, any)) {
            BlockState t = level.getBlockState(p);
            if (t.getBlock() == this && t.getValue(TOP)) level.setBlock(p, t.setValue(LAMP, on), 3);
        }
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
