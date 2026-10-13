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

/** Pose le laboratoire d'un coup : 2 de large (vers la droite du joueur), 1 de profondeur, 2 de haut, face tournée vers le joueur. */
public class LabItem extends BlockItem {
    public LabItem(Block block, Properties props) { super(block, props); }

    @Override
    public InteractionResult place(BlockPlaceContext ctx) {
        Level level = ctx.getLevel();
        if (level.isClientSide) return InteractionResult.SUCCESS;
        Direction facing = ctx.getHorizontalDirection().getOpposite();
        BlockPos o = ctx.getClickedPos();
        for (int i = 0; i < 2; i++) {
            for (int y = 0; y < 2; y++) {
                BlockPos p = LabBlock.cell(o, facing, i, y);
                if (level.isOutsideBuildHeight(p) || !level.getBlockState(p).canBeReplaced()) {
                    if (ctx.getPlayer() instanceof ServerPlayer sp) sp.displayClientMessage(Component.literal("§cIl faut 2 blocs de large et 2 de haut libres (à votre droite)."), true);
                    return InteractionResult.FAIL;
                }
            }
        }
        for (int i = 0; i < 2; i++) {
            for (int y = 0; y < 2; y++) {
                BlockState st = getBlock().defaultBlockState().setValue(LabBlock.FACING, facing).setValue(LabBlock.PART, i + 2 * y);
                level.setBlock(LabBlock.cell(o, facing, i, y), st, 3);
            }
        }
        level.playSound(null, o, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 0.8f, 0.9f);
        if (ctx.getPlayer() != null && !ctx.getPlayer().isCreative()) ctx.getItemInHand().shrink(1);
        return InteractionResult.SUCCESS;
    }
}
