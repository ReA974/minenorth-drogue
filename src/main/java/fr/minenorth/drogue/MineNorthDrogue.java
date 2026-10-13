package fr.minenorth.drogue;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

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
        // Config du serveur dédié seulement, dans config/ (le client ne crée aucun fichier).
        if (FMLEnvironment.dist == Dist.DEDICATED_SERVER) {
            ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, DrogueConfig.SPEC, "minenorth_drogue.toml");
        }
    }
}
