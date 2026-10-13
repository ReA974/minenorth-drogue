package fr.minenorth.drogue;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
 * Laboratoire de chimie : 2 de large, 1 de profondeur, 2 de haut (4 blocs). Établi en bas, montage (ballon, condenseur, bécher) en haut.
 * On y dépose les ingrédients un à un (clic droit sur n'importe quelle partie), il fabrique tout seul, on récupère le produit à main nue.
 * La case d'origine (en bas à gauche, vu de face) porte les données ; casser un bloc retire tout le labo.
 */
public class LabBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** 0 = bas gauche, 1 = bas droite, 2 = haut gauche, 3 = haut droite (vu de face) : part = i + 2 y. */
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 3);

    public LabBlock() {
        super(BlockBehaviour.Properties.of().strength(2.5f, 6.0f).sound(SoundType.METAL).noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH).setValue(PART, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING, PART); }

    /** Vers la droite, vu de face. */
    public static Direction right(Direction facing) { return facing.getCounterClockWise(); }

    public static BlockPos cell(BlockPos origin, Direction facing, int i, int y) { return origin.relative(right(facing), i).above(y); }

    public static BlockPos origin(BlockPos pos, BlockState s) {
        int p = s.getValue(PART);
        return pos.relative(right(s.getValue(FACING)), -(p % 2)).below(p / 2);
    }

    @Override @SuppressWarnings("deprecation")
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new LabBE(pos, state); }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide || state.getValue(PART) != 0 ? null : createTickerHelper(type, ModRegistry.LAB_BE.get(), LabBE::serverTick);
    }

    @Override @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (hand == InteractionHand.MAIN_HAND && level.getBlockEntity(origin(pos, state)) instanceof LabBE be) be.interact(player);
        return InteractionResult.CONSUME;
    }

    @Override @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!level.isClientSide && !state.is(newState.getBlock()) && state.getValue(PART) == 0 && level.getBlockEntity(pos) instanceof LabBE be) be.dropAll(level, pos);
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            BlockPos o = origin(pos, state);
            Direction f = state.getValue(FACING);
            for (int i = 0; i < 2; i++) {
                for (int y = 0; y < 2; y++) {
                    BlockPos p = cell(o, f, i, y);
                    if (!p.equals(pos) && level.getBlockState(p).getBlock() == this) level.destroyBlock(p, !player.isCreative());
                }
            }
        }
        super.playerWillDestroy(level, pos, state, player);
    }
}
