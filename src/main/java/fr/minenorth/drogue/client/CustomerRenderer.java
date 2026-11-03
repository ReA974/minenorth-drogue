package fr.minenorth.drogue.client;

import fr.minenorth.drogue.CustomerEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Client PNJ : corps de joueur avec un des 9 skins par défaut de Minecraft (Steve, Alex, Ari, Efe, Kai, Makena, Noor, Sunny, Zuri). */
@OnlyIn(Dist.CLIENT)
public class CustomerRenderer extends HumanoidMobRenderer<CustomerEntity, PlayerModel<CustomerEntity>> {
    private static final String[] SKINS = {"steve", "alex", "ari", "efe", "kai", "makena", "noor", "sunny", "zuri"};

    public CustomerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(CustomerEntity e) {
        String name = SKINS[Math.floorMod(e.skin(), SKINS.length)];
        return new ResourceLocation("minecraft", "textures/entity/player/wide/" + name + ".png");
    }
}
