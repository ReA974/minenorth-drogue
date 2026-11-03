package fr.minenorth.drogue;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/** Réseau du mod : application « Deal » du téléphone MRPhone (demandes de clients, rendez-vous en cours) et ses boutons. */
@Mod.EventBusSubscriber(modid = MineNorthDrogue.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class DrogueNet {
    private DrogueNet() {}

    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(MineNorthDrogue.MOD_ID, "main"),
            () -> VERSION, VERSION::equals, VERSION::equals);

    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent e) {
        e.enqueueWork(() -> {
            CHANNEL.registerMessage(0, PhoneData.class, PhoneData::encode, PhoneData::decode, PhoneData::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
            CHANNEL.registerMessage(1, PhoneAction.class, PhoneAction::encode, PhoneAction::decode, PhoneAction::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
            CHANNEL.registerMessage(2, AppState.class, AppState::encode, AppState::decode, AppState::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        });
    }

    /** Une ligne du téléphone : demande (ou rendez-vous en cours si active). */
    public record OfferView(int id, String name, String item, int count, int minQuality, long cents, int loyalty, int x, int y, int z, int secondsLeft) {
        void write(FriendlyByteBuf b) {
            b.writeVarInt(id); b.writeUtf(name); b.writeUtf(item); b.writeVarInt(count); b.writeVarInt(minQuality); b.writeVarLong(cents);
            b.writeVarInt(loyalty); b.writeInt(x); b.writeInt(y); b.writeInt(z); b.writeVarInt(secondsLeft);
        }

        static OfferView read(FriendlyByteBuf b) {
            return new OfferView(b.readVarInt(), b.readUtf(), b.readUtf(), b.readVarInt(), b.readVarInt(), b.readVarLong(), b.readVarInt(), b.readInt(), b.readInt(), b.readInt(), b.readVarInt());
        }
    }

    /** Serveur -> client : contenu du téléphone. */
    public record PhoneData(List<OfferView> offers, OfferView active) {
        void encode(FriendlyByteBuf b) {
            b.writeVarInt(offers.size());
            for (OfferView o : offers) o.write(b);
            b.writeBoolean(active != null);
            if (active != null) active.write(b);
        }

        static PhoneData decode(FriendlyByteBuf b) {
            int n = b.readVarInt();
            List<OfferView> l = new ArrayList<>();
            for (int i = 0; i < n; i++) l.add(OfferView.read(b));
            return new PhoneData(l, b.readBoolean() ? OfferView.read(b) : null);
        }

        void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> fr.minenorth.drogue.client.PhoneDeals.receive(this)));
            ctx.get().setPacketHandled(true);
        }
    }

    /** Serveur -> client : l'icône Plans du téléphone est-elle visible (le joueur a déjà eu de la drogue sur lui) ? */
    public record AppState(boolean visible) {
        void encode(FriendlyByteBuf b) { b.writeBoolean(visible); }

        static AppState decode(FriendlyByteBuf b) { return new AppState(b.readBoolean()); }

        void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> fr.minenorth.drogue.client.PhoneDeals.setVisible(visible)));
            ctx.get().setPacketHandled(true);
        }
    }

    /** Client -> serveur : 0 = accepter, 1 = refuser, 2 = annuler le rendez-vous, 3 = rafraîchir. */
    public record PhoneAction(int action, int id) {
        void encode(FriendlyByteBuf b) { b.writeVarInt(action); b.writeVarInt(id); }

        static PhoneAction decode(FriendlyByteBuf b) { return new PhoneAction(b.readVarInt(), b.readVarInt()); }

        void handle(Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer p = ctx.get().getSender();
            ctx.get().enqueueWork(() -> {
                if (p == null || !DealerFlag.has(p)) return;
                long now = p.level().getGameTime();                       // anti-martelage : 10 ticks entre deux demandes
                if (now - p.getPersistentData().getLong(LAST) < 10) return;
                p.getPersistentData().putLong(LAST, now);
                switch (action) {
                    case 0 -> Customers.accept(p, id);
                    case 1 -> Customers.decline(p, id);
                    case 2 -> Customers.cancel(p);
                    default -> {}
                }
                sendPhone(p);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    private static final String LAST = "minenorthdrogue_phone";

    private static OfferView view(ServerPlayer p, Customers.Offer o, boolean active) {
        long left = active ? o.deadline - System.currentTimeMillis() : o.expires - System.currentTimeMillis();
        return new OfferView(o.id, o.name, Customers.id(o.item), o.count, o.minQuality, o.cents, Customers.loyalty(p.server, p.getUUID(), o.name),
                active ? o.spot.getX() : 0, active ? o.spot.getY() : 0, active ? o.spot.getZ() : 0, (int) Math.max(0, left / 1000));
    }

    /** Envoie le contenu de l'application au joueur (qui l'affiche si elle est ouverte). */
    public static void sendPhone(ServerPlayer p) {
        List<OfferView> l = new ArrayList<>();
        for (Customers.Offer o : Customers.offers(p)) l.add(view(p, o, false));
        Customers.Offer a = Customers.active(p);
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new PhoneData(l, a == null ? null : view(p, a, true)));
    }
}
