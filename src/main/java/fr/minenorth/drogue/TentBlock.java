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

import java.util.Locale;

/**
 * Tente de culture 2 × 2 × 2 (8 blocs identiques). Les 4 cases du bas portent chacune une plante (cannabis ou coca, graines posées en clic droit),
 * les 4 du haut portent la lampe UV (objet « lampe UV », clic droit) qui double la vitesse de pousse. Plante mûre : clic droit à main nue.
 * Casser un bloc retire toute la tente (la case d'origine rend la tente, la lampe est rendue aussi).
 */
public class TentBlock extends Block {
    public enum Plant implements StringRepresentable {
        NONE, CANNABIS, COCA;

        @Override public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
    }

    public static final BooleanProperty TOP = BooleanProperty.create("top");
    /** Case dans le carré 2 × 2 : 0 = nord-ouest, 1 = nord-est, 2 = sud-ouest, 3 = sud-est (c = x + 2 z). */
    public static final IntegerProperty CORNER = IntegerProperty.create("corner", 0, 3);
    public static final EnumProperty<Plant> PLANT = EnumProperty.create("plant", Plant.class);
    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
    public static final BooleanProperty LAMP = BooleanProperty.create("lamp");

    public TentBlock() {
        super(BlockBehaviour.Properties.of().strength(2.0f).sound(SoundType.WOOL).noOcclusion().randomTicks()
                .lightLevel(s -> s.getValue(TOP) && s.getValue(LAMP) ? 12 : 0));
        registerDefaultState(stateDefinition.any().setValue(TOP, false).setValue(CORNER, 0).setValue(PLANT, Plant.NONE).setValue(AGE, 0).setValue(LAMP, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(TOP, CORNER, PLANT, AGE, LAMP); }

    /** Case d'origine (bas, nord-ouest) de la tente à laquelle appartient ce bloc. */
    public static BlockPos origin(BlockPos pos, BlockState s) {
        int c = s.getValue(CORNER);
        return pos.offset(-(c % 2), s.getValue(TOP) ? -1 : 0, -(c / 2));
    }

    // ------------------------------------------------------------------ pousse

    @Override @SuppressWarnings("deprecation")
    public boolean isRandomlyTicking(BlockState s) { return !s.getValue(TOP) && s.getValue(PLANT) != Plant.NONE && s.getValue(AGE) < 3; }

    @Override @SuppressWarnings("deprecation")
    public void randomTick(BlockState s, ServerLevel level, BlockPos pos, RandomSource rnd) {
        if (!isRandomlyTicking(s)) return;
        BlockState above = level.getBlockState(pos.above());
        boolean lamp = above.getBlock() == this && above.getValue(LAMP);
        if (lamp || rnd.nextInt(2) == 0) level.setBlock(pos, s.setValue(AGE, s.getValue(AGE) + 1), 2);
    }

    // ------------------------------------------------------------------ interaction

    @Override @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState s, Level level, BlockPos pos, Player p, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        ItemStack st = p.getMainHandItem();
        BlockPos o = origin(pos, s);
        boolean lamp = level.getBlockState(o.above()).getBlock() == this && level.getBlockState(o.above()).getValue(LAMP);

        if (st.is(ModRegistry.UV_LAMP.get())) {
            if (lamp) { msg(p, "§7Une lampe UV est déjà installée."); return InteractionResult.CONSUME; }
            setLamp(level, o, true);
            st.shrink(1);
            level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.6f, 1.6f);
            msg(p, "§dLampe UV installée : les plantes poussent deux fois plus vite.");
            return InteractionResult.CONSUME;
        }
        if (st.isEmpty() && p.isShiftKeyDown() && lamp) {
            setLamp(level, o, false);
            ItemHandlerHelper.giveItemToPlayer(p, new ItemStack(ModRegistry.UV_LAMP.get()));
            msg(p, "§7Lampe UV retirée.");
            return InteractionResult.CONSUME;
        }
        if (s.getValue(TOP)) {
            msg(p, lamp ? "§dLampe UV allumée. Accroupi, main vide : la retirer." : "§7Pas de lampe UV (objet « Lampe UV », clic droit).");
            return InteractionResult.CONSUME;
        }
        Plant plant = s.getValue(PLANT);
        if (plant == Plant.NONE) {
            Plant want = st.is(ModRegistry.CANNABIS_SEEDS.get()) ? Plant.CANNABIS : st.is(ModRegistry.COCA_SEEDS.get()) ? Plant.COCA : Plant.NONE;
            if (want == Plant.NONE) { msg(p, "§7Emplacement libre : plantez des graines de cannabis ou de coca."); return InteractionResult.CONSUME; }
            st.shrink(1);
            level.setBlock(pos, s.setValue(PLANT, want).setValue(AGE, 0), 3);
            level.playSound(null, pos, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0f, 1.0f);
            return InteractionResult.CONSUME;
        }
        int age = s.getValue(AGE);
        if (age < 3) { msg(p, "§7Pousse en cours (" + (age + 1) + "/4)" + (lamp ? "" : " — plus lent sans lampe UV") + "."); return InteractionResult.CONSUME; }
        // récolte
        boolean weed = plant == Plant.CANNABIS;
        ItemHandlerHelper.giveItemToPlayer(p, new ItemStack(weed ? ModRegistry.CANNABIS_LEAF.get() : ModRegistry.COCA_LEAF.get(), 2 + level.random.nextInt(3)));
        if (level.random.nextInt(2) == 0) ItemHandlerHelper.giveItemToPlayer(p, new ItemStack(weed ? ModRegistry.CANNABIS_SEEDS.get() : ModRegistry.COCA_SEEDS.get()));
        level.setBlock(pos, s.setValue(PLANT, Plant.NONE).setValue(AGE, 0), 3);
        level.playSound(null, pos, SoundEvents.GRASS_BREAK, SoundSource.BLOCKS, 1.0f, 1.0f);
        return InteractionResult.CONSUME;
    }

    private static void msg(Player p, String text) { p.displayClientMessage(Component.literal(text), true); }

    /** Allume / éteint la lampe sur les 4 cases du haut. */
    private void setLamp(Level level, BlockPos origin, boolean on) {
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                BlockPos p = origin.offset(i, 1, j);
                BlockState t = level.getBlockState(p);
                if (t.getBlock() == this) level.setBlock(p, t.setValue(LAMP, on), 3);
            }
        }
    }

    // ------------------------------------------------------------------ casse

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState s, Player player) {
        if (!level.isClientSide) {
            BlockPos o = origin(pos, s);
            for (int i = 0; i < 2; i++) {
                for (int y = 0; y < 2; y++) {
                    for (int j = 0; j < 2; j++) {
                        BlockPos p = o.offset(i, y, j);
                        if (!p.equals(pos) && level.getBlockState(p).getBlock() == this) level.destroyBlock(p, !player.isCreative());
                    }
                }
            }
        }
        super.playerWillDestroy(level, pos, s, player);
    }
}
