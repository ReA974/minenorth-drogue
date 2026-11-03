package fr.minenorth.drogue.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import fr.minenorth.drogue.DecorBlock;
import fr.minenorth.drogue.ExtractorBE;
import fr.minenorth.drogue.MineNorthDrogue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Hélice de l'extracteur d'air (modèle block/air_extractor_blades) qui tourne en continu derrière la grille. */
@OnlyIn(Dist.CLIENT)
public class ExtractorRenderer implements BlockEntityRenderer<ExtractorBE> {
    public static final ResourceLocation BLADES = new ResourceLocation(MineNorthDrogue.MOD_ID, "block/air_extractor_blades");

    public ExtractorRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(ExtractorBE be, float partial, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        if (be.getLevel() == null) return;
        Direction f = be.getBlockState().getValue(DecorBlock.FACING);
        float spin = ((be.getLevel().getGameTime() % 360) + partial) * 24f;   // un peu plus d'un tour par seconde
        ps.pushPose();
        ps.translate(0.5, 0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(-f.toYRot() + 180f));       // même rotation que le bloc (modèle tourné vers le nord)
        ps.translate(-0.5, 0, -0.5);
        ps.translate(0.5, 8.5 / 16.0, 0);
        ps.mulPose(Axis.ZP.rotationDegrees(spin));
        ps.translate(-0.5, -8.5 / 16.0, 0);
        var model = Minecraft.getInstance().getModelManager().getModel(BLADES);
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(ps.last(), buf.getBuffer(RenderType.cutout()), be.getBlockState(),
                model, 1f, 1f, 1f, light, overlay);
        ps.popPose();
    }
}
