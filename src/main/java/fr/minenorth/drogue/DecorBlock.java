package fr.minenorth.drogue;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Matériel de ventilation, sans écran : l'extracteur d'air (caisson + hélice, empêche les accidents de labo à 5 blocs) et le filtre à charbon
 * (gros cylindre, supprime l'odeur à 4 blocs ; voir Odor). Il suffit de les poser près des installations. L'hélice de l'extracteur tourne
 * (bloc-entité sans données + client/ExtractorRenderer).
 */
public class DecorBlock extends HorizontalDirectionalBlock implements net.minecraft.world.level.block.EntityBlock {
    public enum Shape { EXTRACTOR, FILTER }

    private static final VoxelShape EXTRACTOR = Block.box(1, 0, 1, 15, 14, 15), FILTER = Block.box(3, 0, 3, 13, 15, 13);
    private final Shape shape;

    public DecorBlock(Shape shape) {
        super(BlockBehaviour.Properties.of().strength(1.5f).sound(SoundType.METAL).noOcclusion());
        this.shape = shape;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING); }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) { return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite()); }

    @Override @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext ctx) { return shape == Shape.EXTRACTOR ? EXTRACTOR : FILTER; }

    @Override
    public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return shape == Shape.EXTRACTOR ? new ExtractorBE(pos, state) : null;
    }

    /** L'extracteur souffle un léger courant d'air par sa face. */
    @Override
    public void animateTick(BlockState s, Level level, BlockPos pos, RandomSource rnd) {
        if (shape != Shape.EXTRACTOR || rnd.nextInt(3) != 0) return;
        Direction f = s.getValue(FACING);
        level.addParticle(ParticleTypes.WHITE_ASH, pos.getX() + 0.5 + f.getStepX() * 0.55, pos.getY() + 0.5 + rnd.nextDouble() * 0.2,
                pos.getZ() + 0.5 + f.getStepZ() * 0.55, f.getStepX() * 0.05, 0.01, f.getStepZ() * 0.05);
    }
}
