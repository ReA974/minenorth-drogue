package fr.minenorth.drogue;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

import java.util.function.Supplier;

/** Culture de drogue : 4 stades (0 à 3) sur terre labourée, comme le blé ; mûre au stade 3 (récolte : feuilles + graines, voir les tables de butin). */
public class DrugCropBlock extends CropBlock {
    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
    private final Supplier<? extends Item> seeds;

    public DrugCropBlock(Supplier<? extends Item> seeds) {
        super(BlockBehaviour.Properties.copy(net.minecraft.world.level.block.Blocks.WHEAT).sound(SoundType.CROP).noCollission().randomTicks().instabreak());
        this.seeds = seeds;
    }

    @Override public IntegerProperty getAgeProperty() { return AGE; }

    @Override public int getMaxAge() { return 3; }

    @Override protected ItemLike getBaseSeedId() { return seeds.get(); }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(AGE); }
}
