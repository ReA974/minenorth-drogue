package fr.minenorth.drogue.client;

import fr.minenorth.drogue.DrugEffects;
import fr.minenorth.drogue.DrugItems;
import fr.minenorth.drogue.MineNorthDrogue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Filtres visuels de la défonce (shaders d'après-traitement de Minecraft) selon l'effet actif : joint = traînées douces (phosphor), héroïne =
 * monde délavé (desaturate), ecstasy = couleurs vives (color_convolve), LSD = ondulation (wobble), champignons = couleurs décalées (deconverge),
 * overdose = flou (blur). Le filtre est retiré à la fin de l'effet ; on ne touche pas à un filtre posé par autre chose (vue d'une créature…).
 * Ajoute aussi la qualité (étoiles) et la coupe dans l'infobulle des produits.
 */
@Mod.EventBusSubscriber(modid = MineNorthDrogue.MOD_ID, value = Dist.CLIENT)
public final class DrugVisuals {
    private DrugVisuals() {}

    private static ResourceLocation current;

    private static ResourceLocation wanted(LocalPlayer p) {
        if (has(p, DrugEffects.OVERDOSE.get())) return post("blur");
        if (has(p, DrugEffects.TRIP.get())) return post("wobble");
        if (has(p, DrugEffects.SHROOMS.get())) return post("deconverge");
        if (has(p, DrugEffects.ROLL.get())) return post("color_convolve");
        if (has(p, DrugEffects.NOD.get())) return post("desaturate");
        if (has(p, DrugEffects.HIGH.get())) return post("phosphor");
        return null;
    }

    private static boolean has(LocalPlayer p, MobEffect e) { return p.hasEffect(e); }

    private static ResourceLocation post(String name) { return new ResourceLocation("minecraft", "shaders/post/" + name + ".json"); }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) { current = null; return; }
        ResourceLocation want = wanted(mc.player);
        if (want != null && !want.equals(current)) {
            mc.gameRenderer.loadEffect(want);
            current = want;
        } else if (want == null && current != null) {
            var effect = mc.gameRenderer.currentEffect();
            if (effect != null && effect.getName().equals(current.toString())) mc.gameRenderer.shutdownEffect();
            current = null;
        }
    }

    /** Changement de serveur : l'icône Plans redevient cachée jusqu'au prochain état envoyé. */
    @SubscribeEvent
    public static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut e) { PhoneDeals.setVisible(false); }

    @SubscribeEvent
    public static void tooltip(ItemTooltipEvent e) { DrugItems.tooltip(e.getItemStack(), e.getToolTip()); }
}
