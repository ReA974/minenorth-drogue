package fr.minenorth.drogue;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

/**
 * Séchoir à cannabis, sans écran (comme une vraie corde à linge) : feuilles en main, clic droit pour les suspendre (16 au plus) ; après le
 * séchage (config sechage.secondes) elles deviennent des têtes séchées, une étoile de plus. Main vide : voir le temps restant ou décrocher.
 * Le nombre de bottes visibles (FILL) et leur couleur (DRY) suivent le contenu.
 */
public class DryingRackBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty FILL = IntegerProperty.create("fill", 0, 3);
    public static final BooleanProperty DRY = BooleanProperty.create("dry");
    private static final VoxelShape SHAPE = Block.box(0, 0, 5, 16, 16, 11);

    public DryingRackBlock() {
        super(BlockBehaviour.Properties.of().strength(1.5f).sound(SoundType.WOOD).noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(FILL, 0).setValue(DRY, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING, FILL, DRY); }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) { return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite()); }

    @Override @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return s.getValue(FACING).getAxis() == Direction.Axis.Z ? SHAPE : Block.box(5, 0, 0, 11, 16, 16);
    }

    @Override @SuppressWarnings("deprecation")
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new DryingRackBE(pos, state); }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModRegistry.DRYING_BE.get(), (l, p, s, be) -> {
            if (l.getGameTime() % 20 == 0 && be.tryFinish(l.getGameTime())) {
                refresh(l, p, be);
                l.playSound(null, p, SoundEvents.AZALEA_LEAVES_BREAK, SoundSource.BLOCKS, 0.7f, 0.8f);
            }
        });
    }

    static void refresh(Level level, BlockPos pos, DryingRackBE be) {
        BlockState s = level.getBlockState(pos);
        if (!(s.getBlock() instanceof DryingRackBlock)) return;
        int n = be.leaves().getCount();
        int fill = n == 0 ? 0 : n <= 5 ? 1 : n <= 10 ? 2 : 3;
        BlockState ns = s.setValue(FILL, fill).setValue(DRY, be.dried());
        if (ns != s) level.setBlock(pos, ns, 3);
    }

    @Override @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player p, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (hand != InteractionHand.MAIN_HAND || !(level.getBlockEntity(pos) instanceof DryingRackBE be)) return InteractionResult.PASS;
        ItemStack st = p.getMainHandItem();
        long now = level.getGameTime();
        if (st.is(ModRegistry.CANNABIS_LEAF.get())) {
            int n = be.add(st, now);
            if (n == 0) {
                p.displayClientMessage(Component.literal(be.dried() ? "§7Décrochez d'abord les têtes séchées (main vide)."
                        : be.leaves().getCount() >= DryingRackBE.MAX ? "§7Le séchoir est plein." : "§7Une seule qualité de feuilles à la fois."), true);
                return InteractionResult.CONSUME;
            }
            if (!p.getAbilities().instabuild) st.shrink(n);
            refresh(level, pos, be);
            level.playSound(null, pos, SoundEvents.AZALEA_LEAVES_PLACE, SoundSource.BLOCKS, 0.8f, 1.1f);
            p.displayClientMessage(Component.literal("§a" + be.leaves().getCount() + " feuille(s) suspendue(s) : séchage en " + (be.remainingTicks(now) / 20 + 1) + " s."), true);
            return InteractionResult.CONSUME;
        }
        if (!st.isEmpty()) {
            p.displayClientMessage(Component.literal("§7Feuilles de cannabis en main pour les suspendre ; main vide pour décrocher."), true);
            return InteractionResult.CONSUME;
        }
        if (be.leaves().isEmpty()) {
            p.displayClientMessage(Component.literal("§7Séchoir vide : suspendez des feuilles de cannabis (+1 étoile une fois sèches)."), true);
            return InteractionResult.CONSUME;
        }
        if (!be.dried() && !p.isShiftKeyDown()) {
            p.displayClientMessage(Component.literal("§7Séchage en cours : encore " + (be.remainingTicks(now) / 20 + 1) + " s. Accroupi : décrocher quand même."), true);
            return InteractionResult.CONSUME;
        }
        ItemHandlerHelper.giveItemToPlayer(p, be.take());
        refresh(level, pos, be);
        level.playSound(null, pos, SoundEvents.AZALEA_LEAVES_BREAK, SoundSource.BLOCKS, 0.8f, 1.2f);
        return InteractionResult.CONSUME;
    }

    @Override
    public void animateTick(BlockState s, Level level, BlockPos pos, RandomSource rnd) {
        if (s.getValue(FILL) > 0 && rnd.nextInt(5) == 0 && !Odor.filtered(level, pos)) Odor.puff(level, pos.above(), rnd);
    }

    @Override @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!level.isClientSide && !state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof DryingRackBE be) be.dropAll(level, pos);
        super.onRemove(state, level, pos, newState, moved);
    }
}
