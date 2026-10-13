package fr.minenorth.drogue;

import net.minecraft.core.BlockPos;
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

/** Pose la tente d'un coup : 2 × 2 au sol et 2 de haut, vers le sud-est de la case visée (il faut 8 cases libres). */
public class TentItem extends BlockItem {
    public TentItem(Block block, Properties props) { super(block, props); }

    @Override
    public InteractionResult place(BlockPlaceContext ctx) {
        Level level = ctx.getLevel();
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockPos o = ctx.getClickedPos();
        for (int i = 0; i < 2; i++) {
            for (int y = 0; y < 2; y++) {
                for (int j = 0; j < 2; j++) {
                    BlockPos p = o.offset(i, y, j);
                    if (level.isOutsideBuildHeight(p) || !level.getBlockState(p).canBeReplaced()) {
                        if (ctx.getPlayer() instanceof ServerPlayer sp) sp.displayClientMessage(Component.literal("§cIl faut 2 × 2 blocs libres, sur 2 de haut (vers le sud-est)."), true);
                        return InteractionResult.FAIL;
                    }
                }
            }
        }
        for (int i = 0; i < 2; i++) {
            for (int y = 0; y < 2; y++) {
                for (int j = 0; j < 2; j++) {
                    BlockState st = getBlock().defaultBlockState().setValue(TentBlock.TOP, y == 1).setValue(TentBlock.CORNER, i + 2 * j);
                    level.setBlock(o.offset(i, y, j), st, 3);
                }
            }
        }
        level.playSound(null, o, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1.0f, 0.9f);
        if (ctx.getPlayer() != null && !ctx.getPlayer().isCreative()) ctx.getItemInHand().shrink(1);
        return InteractionResult.SUCCESS;
    }
}
