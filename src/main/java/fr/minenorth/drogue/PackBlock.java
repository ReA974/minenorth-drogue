package fr.minenorth.drogue;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Table d'ensachage 1 × 2 : deux blocs côte à côte (2 de large, 1 de profondeur, 1 de haut), face tournée vers le joueur. Clic droit sur n'importe quelle partie :
 * on y dépose poudre / cristaux et sachets vides (pile entière), elle fait les sachets toute seule, on les récupère à main nue.
 * La case de gauche (vue de face) porte les données ; casser un bloc retire toute la table.
 */
public class PackBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** 0 = gauche, 1 = droite (vu de face). */
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 1);
    /** 0 = repos ; 1 à 3 = images de l'animation (balance qui oscille, levier de la soudeuse) pendant l'ensachage. */
    public static final IntegerProperty ANIM = IntegerProperty.create("anim", 0, 3);

    public PackBlock() {
        super(BlockBehaviour.Properties.of().strength(2.0f).sound(SoundType.WOOD).noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH).setValue(PART, 0).setValue(ANIM, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING, PART, ANIM); }

    /** Vers la droite, vu de face. */
    public static Direction right(Direction facing) { return facing.getCounterClockWise(); }

    public static BlockPos origin(BlockPos pos, BlockState s) { return pos.relative(right(s.getValue(FACING)), -s.getValue(PART)); }

    /** Passe les deux cases de la table à l'image d'animation voulue. */
    public static void setAnim(Level level, BlockPos origin, int anim) {
        BlockState o = level.getBlockState(origin);
        if (!(o.getBlock() instanceof PackBlock)) return;
        for (int i = 0; i < 2; i++) {
            BlockPos p = origin.relative(right(o.getValue(FACING)), i);
            BlockState st = level.getBlockState(p);
            if (st.getBlock() instanceof PackBlock && st.getValue(ANIM) != anim) level.setBlock(p, st.setValue(ANIM, anim), 2);
        }
    }

    /** Lance l'animation (appelé quand le mini-jeu démarre) : elle se poursuit par ticks planifiés tant que la partie dure. */
    public void startAnim(Level level, BlockPos origin) {
        setAnim(level, origin, 1);
        level.scheduleTick(origin, this, 4);
    }

    @Override @SuppressWarnings("deprecation")
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(PART) != 0) return;
        if (MiniGame.activeAt(pos)) {
            setAnim(level, pos, state.getValue(ANIM) % 3 + 1);
            level.scheduleTick(pos, this, 4);
        } else if (state.getValue(ANIM) != 0) {
            setAnim(level, pos, 0);
        }
    }

    /** Filet de sécurité : une table restée en animation après un redémarrage du serveur revient au repos. */
    @Override public boolean isRandomlyTicking(BlockState state) { return state.getValue(ANIM) != 0; }

    @Override @SuppressWarnings("deprecation")
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockPos o = origin(pos, state);
        if (state.getValue(ANIM) != 0 && !MiniGame.activeAt(o)) setAnim(level, o, 0);
    }

    @Override @SuppressWarnings("deprecation")
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new PackBE(pos, state); }

    @Override @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (hand == InteractionHand.MAIN_HAND && level.getBlockEntity(origin(pos, state)) instanceof PackBE be) be.interact(player);
        return InteractionResult.CONSUME;
    }

    @Override @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!level.isClientSide && !state.is(newState.getBlock()) && state.getValue(PART) == 0 && level.getBlockEntity(pos) instanceof PackBE be) be.dropAll(level, pos);
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            BlockPos other = pos.relative(right(state.getValue(FACING)), state.getValue(PART) == 0 ? 1 : -1);
            if (level.getBlockState(other).getBlock() == this) level.destroyBlock(other, !player.isCreative());
        }
        super.playerWillDestroy(level, pos, state, player);
    }
}
