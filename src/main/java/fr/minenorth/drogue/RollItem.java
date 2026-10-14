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

/** Pose la table à rouler d'un coup : 2 blocs côte à côte (vers la droite du joueur), face tournée vers lui. */
public class RollItem extends BlockItem {
    public RollItem(Block block, Properties props) { super(block, props); }

    @Override
    public InteractionResult place(BlockPlaceContext ctx) {
        Level level = ctx.getLevel();
        if (level.isClientSide) return InteractionResult.SUCCESS;
        Direction facing = ctx.getHorizontalDirection().getOpposite();
        BlockPos o = ctx.getClickedPos();
        BlockPos second = o.relative(RollBlock.right(facing));
        if (!level.getBlockState(o).canBeReplaced() || !level.getBlockState(second).canBeReplaced()) {
            if (ctx.getPlayer() instanceof ServerPlayer sp) sp.displayClientMessage(Component.literal("§cIl faut 2 blocs libres côte à côte (à votre droite)."), true);
            return InteractionResult.FAIL;
        }
        level.setBlock(o, getBlock().defaultBlockState().setValue(RollBlock.FACING, facing).setValue(RollBlock.PART, 0), 3);
        level.setBlock(second, getBlock().defaultBlockState().setValue(RollBlock.FACING, facing).setValue(RollBlock.PART, 1), 3);
        level.playSound(null, o, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0f, 0.9f);
        if (ctx.getPlayer() != null && !ctx.getPlayer().isCreative()) ctx.getItemInHand().shrink(1);
        return InteractionResult.SUCCESS;
    }
}
