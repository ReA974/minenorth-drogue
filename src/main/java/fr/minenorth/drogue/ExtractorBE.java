package fr.minenorth.drogue;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Extracteur d'air : pas de données, sert seulement à dessiner l'hélice qui tourne (client/ExtractorRenderer). */
public class ExtractorBE extends BlockEntity {
    public ExtractorBE(BlockPos pos, BlockState state) { super(ModRegistry.EXTRACTOR_BE.get(), pos, state); }
}
