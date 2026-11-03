package fr.minenorth.drogue;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

/**
 * L'application « Deal » du téléphone n'apparaît qu'aux joueurs qui ont déjà eu de la drogue sur eux (plante, produit, sachet : tout objet du mod
 * qui porte une qualité). Le drapeau est gardé dans les données du joueur (survit à la mort) et envoyé au client à la connexion et dès qu'il change.
 * Le serveur refuse aussi les demandes de l'application tant que le drapeau n'est pas posé.
 */
@Mod.EventBusSubscriber(modid = MineNorthDrogue.MOD_ID)
public final class DealerFlag {
    private DealerFlag() {}

    private static final String KEY = "minenorthdrogue_dealer";

    public static boolean has(ServerPlayer p) { return p.getPersistentData().getBoolean(KEY); }

    private static void send(ServerPlayer p) { DrogueNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new DrogueNet.AppState(has(p))); }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent e) { if (e.getEntity() instanceof ServerPlayer p) send(p); }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone e) {
        if (e.getOriginal().getPersistentData().getBoolean(KEY)) e.getEntity().getPersistentData().putBoolean(KEY, true);
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p) || p.tickCount % 40 != 13 || has(p)) return;
        boolean found = DrugItems.hasQuality(p.getOffhandItem());
        for (ItemStack st : p.getInventory().items) if (!found && DrugItems.hasQuality(st)) found = true;
        if (!found) return;
        p.getPersistentData().putBoolean(KEY, true);
        send(p);
    }
}
