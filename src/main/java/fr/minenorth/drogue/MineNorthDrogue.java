package fr.minenorth.drogue;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;

/**
 * MineNorth Drogue : marijuana, cocaïne, méthamphétamine. Cultures (cannabis, coca, éphédra), laboratoire clandestin et effets positifs
 * limités par un délai d'attente. Chaque consommation est notée pour le test salivaire du mod Police (données joueur « minenorthpolice_drogues »).
 */
@Mod(MineNorthDrogue.MOD_ID)
public class MineNorthDrogue {
    public static final String MOD_ID = "minenorthdrogue";

    public MineNorthDrogue() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModRegistry.register(bus);
        bus.addListener(MineNorthDrogue::fillTab);
        // Config du serveur dédié seulement, dans config/ (le client ne crée aucun fichier).
        if (FMLEnvironment.dist == Dist.DEDICATED_SERVER) {
            ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, DrogueConfig.SPEC, "minenorth_drogue.toml");
        }
    }

    /** Les objets vont dans l'onglet créatif commun « MineNorth » (fourni par minenorth_api) s'il est présent. */
    private static void fillTab(BuildCreativeModeTabContentsEvent e) {
        if (!e.getTabKey().location().equals(new ResourceLocation("minenorthapi", "minenorth"))) return;
        ModRegistry.ITEMS.getEntries().forEach(i -> e.accept(i.get()));
    }
}
