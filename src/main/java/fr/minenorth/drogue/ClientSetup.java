package fr.minenorth.drogue;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Prédicat d'objet « minenorthdrogue:pour » : 0 au repos, puis monte jusqu'à 1 et redescend pendant le versage de l'arrosoir (choix du modèle incliné). */
@Mod.EventBusSubscriber(modid = MineNorthDrogue.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {}

    private static float useProgress(net.minecraft.world.item.ItemStack stack, net.minecraft.world.entity.LivingEntity entity) {
        if (entity == null || !entity.isUsingItem() || entity.getUseItem().getItem() != stack.getItem()) return 0f;
        int total = stack.getUseDuration();
        return total <= 0 ? 0f : Math.min(1f, Math.max(0.01f, entity.getTicksUsingItem() / (float) total));
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent e) {
        // progression de la prise (0 au repos, 0 à 1 pendant la prise) : choix du modèle « miroir » (cocaïne) ou « seringue » (méthamphétamine)
        e.enqueueWork(() -> {
            ItemProperties.register(ModRegistry.COCAINE.get(), new ResourceLocation(MineNorthDrogue.MOD_ID, "snort"), (stack, level, entity, seed) -> useProgress(stack, entity));
            ItemProperties.register(ModRegistry.METH.get(), new ResourceLocation(MineNorthDrogue.MOD_ID, "inject"), (stack, level, entity, seed) -> useProgress(stack, entity));
        });
        e.enqueueWork(() -> ItemProperties.register(ModRegistry.WATERING_CAN.get(), new ResourceLocation(MineNorthDrogue.MOD_ID, "pour"), (stack, level, entity, seed) -> {
            CompoundTag t = stack.getTag();
            if (t == null || !t.contains(WateringCanItem.TAG)) return 0f;
            long now = level != null ? level.getGameTime() : Minecraft.getInstance().level == null ? 0 : Minecraft.getInstance().level.getGameTime();
            long remaining = t.getLong(WateringCanItem.TAG) - now;
            if (remaining <= 0 || remaining > WateringCanItem.POUR_TICKS) return 0f;
            float p = 1f - remaining / (float) WateringCanItem.POUR_TICKS;
            return (float) Math.sin(Math.PI * p);
        }));
    }
}
