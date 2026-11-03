package fr.minenorth.drogue;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.level.block.Block;

/** Ergot de seigle (pour le LSD) : un champignon parasite qu'on trouve parfois en récoltant du blé mûr (config lsd.ergotPourMille). */
@Mod.EventBusSubscriber(modid = MineNorthDrogue.MOD_ID)
public final class Ergot {
    private Ergot() {}

    @SubscribeEvent
    public static void harvest(BlockEvent.BreakEvent e) {
        var s = e.getState();
        if (e.getLevel().isClientSide() || !s.is(Blocks.WHEAT) || !(s.getBlock() instanceof CropBlock crop) || !crop.isMaxAge(s)) return;
        if (e.getPlayer().getRandom().nextInt(1000) >= DrogueConfig.get(DrogueConfig.ERGOT_CHANCE)) return;
        if (e.getLevel() instanceof net.minecraft.world.level.Level level)
            Block.popResource(level, e.getPos(), Quality.set(new ItemStack(ModRegistry.ERGOT.get()), 2 + e.getPlayer().getRandom().nextInt(3)));
    }
}
