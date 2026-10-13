package fr.minenorth.drogue;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Pose la tente d'un coup (fermée) : 2 × 2, 1 × 2 ou 1 × 1 au sol et 2 de haut, vers le sud-est de la case visée.
 * La 1 × 2 suit l'axe vers lequel regarde le joueur (nord-sud ou est-ouest). Il faut toutes les cases libres.
 */
public class TentItem extends BlockItem {
    private final TentBlock.Layout layout;

    public TentItem(Block block, TentBlock.Layout layout, Properties props) {
        super(block, props);
        this.layout = layout;
    }

    @Override
    public InteractionResult place(BlockPlaceContext ctx) {
        Level level = ctx.getLevel();
        if (level.isClientSide) return InteractionResult.SUCCESS;
        boolean alongx = layout == TentBlock.Layout.S12 && ctx.getHorizontalDirection().getAxis() == Direction.Axis.X;
        int w = TentBlock.width(layout, alongx), d = TentBlock.depth(layout, alongx);
        BlockPos o = ctx.getClickedPos();
        for (int i = 0; i < w; i++) {
            for (int y = 0; y < 2; y++) {
                for (int j = 0; j < d; j++) {
                    BlockPos p = o.offset(i, y, j);
                    if (level.isOutsideBuildHeight(p) || !level.getBlockState(p).canBeReplaced()) {
                        if (ctx.getPlayer() instanceof ServerPlayer sp)
                            sp.displayClientMessage(Component.literal("§cIl faut " + w + " × " + d + " blocs libres, sur 2 de haut (vers le sud-est)."), true);
                        return InteractionResult.FAIL;
                    }
                }
            }
        }
        for (int i = 0; i < w; i++) {
            for (int y = 0; y < 2; y++) {
                for (int j = 0; j < d; j++) {
                    BlockState st = getBlock().defaultBlockState().setValue(TentBlock.LAYOUT, layout).setValue(TentBlock.TOP, y == 1)
                            .setValue(TentBlock.CORNER, TentBlock.cornerOf(layout, alongx, i, j)).setValue(TentBlock.ALONGX, alongx);
                    level.setBlock(o.offset(i, y, j), st, 3);
                }
            }
        }
        level.playSound(null, o, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1.0f, 0.9f);
        if (ctx.getPlayer() != null && !ctx.getPlayer().isCreative()) ctx.getItemInHand().shrink(1);
        return InteractionResult.SUCCESS;
    }
}
