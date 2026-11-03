package fr.minenorth.drogue;

import fr.minenorth.api.MineNorth;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Revente aux clients, par l'application « Deal » du téléphone MRPhone. Des clients (PNJ) envoient des demandes : produit, quantité, qualité minimum, prix proposé. On en
 * accepte une, un rendez-vous est donné quelque part en ville (60 à 180 blocs) ; le client y apparaît quand on approche. Clic droit sur lui : la
 * marchandise est prise dans l'inventaire et le prix versé sur le compte (EuroBank, source « drogue:vente »), avec un bonus si la qualité dépasse
 * ce qu'il demandait. Un client satisfait devient fidèle (jusqu'à 5) : il commande plus et paie mieux. À chaque deal, un témoin peut prévenir la
 * police. Demandes et rendez-vous en cours sont en mémoire (perdus au redémarrage) ; la fidélité est sauvegardée.
 */
@Mod.EventBusSubscriber(modid = MineNorthDrogue.MOD_ID)
public final class Customers {
    private Customers() {}

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MineNorthDrogue.MOD_ID);
    public static final RegistryObject<EntityType<CustomerEntity>> CUSTOMER = ENTITIES.register("customer",
            () -> EntityType.Builder.<CustomerEntity>of(CustomerEntity::new, MobCategory.MISC).sized(0.6f, 1.8f).clientTrackingRange(10)
                    .build(MineNorthDrogue.MOD_ID + ":customer"));

    @Mod.EventBusSubscriber(modid = MineNorthDrogue.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Attributes {
        private Attributes() {}

        @SubscribeEvent
        public static void attributes(EntityAttributeCreationEvent e) { e.put(CUSTOMER.get(), CustomerEntity.attributes().build()); }
    }

    private static final String[] NAMES = {"Kévin", "Jessy", "Momo", "Lulu", "Le Grec", "Tonton", "Sandra", "Dédé", "Bichette", "Rico", "Nono", "La Fouine",
            "Max", "Inès", "Yanis", "Steph", "Vanessa", "Le Chauve", "Gégé", "Mélo", "Bastien", "Coco", "Ptit Louis", "Nadia"};
    private static final int SPAWN_RANGE = 40, LOYALTY_MAX = 5;
    private static final long OFFER_LIFE_MS = 15 * 60_000L;

    /** Une demande de client. */
    public static final class Offer {
        public final int id;
        public final String name;
        public final Item item;
        public final int count, minQuality, skin;
        public final long cents, expires;
        public BlockPos spot;
        public long deadline;
        public UUID entity;

        Offer(int id, String name, Item item, int count, int minQuality, long cents, int skin, long expires) {
            this.id = id;
            this.name = name;
            this.item = item;
            this.count = count;
            this.minQuality = minQuality;
            this.cents = cents;
            this.skin = skin;
            this.expires = expires;
        }
    }

    private static final class State {
        final List<Offer> offers = new ArrayList<>();
        Offer active;
        long nextOfferAt;
        int ids;
    }

    private static final Map<UUID, State> STATES = new HashMap<>();
    private static final RandomSource RND = RandomSource.create();

    private static State state(ServerPlayer p) { return STATES.computeIfAbsent(p.getUUID(), k -> new State()); }

    // ------------------------------------------------------------------ demandes

    /** Demandes à jour (expirées retirées, nouvelles ajoutées selon le temps écoulé). */
    public static List<Offer> offers(ServerPlayer p) {
        State s = state(p);
        long now = System.currentTimeMillis();
        s.offers.removeIf(o -> o.expires < now);
        int max = DrogueConfig.get(DrogueConfig.CLIENT_OFFERS);
        long every = DrogueConfig.get(DrogueConfig.CLIENT_REFRESH_MINUTES) * 60_000L;
        if (s.nextOfferAt == 0) {   // premier allumage : le téléphone vibre déjà de quelques messages
            for (int i = 0; i < Math.min(2, max); i++) s.offers.add(newOffer(p, s, now));
            s.nextOfferAt = now + every;
        }
        while (now >= s.nextOfferAt && s.offers.size() < max) {
            s.offers.add(newOffer(p, s, now));
            s.nextOfferAt += every;
        }
        if (now >= s.nextOfferAt) s.nextOfferAt = now + every;
        return s.offers;
    }

    public static Offer active(ServerPlayer p) { return state(p).active; }

    public static int loyalty(MinecraftServer s, UUID player, String name) { return Loyalty.get(s).get(player, name); }

    private static Offer newOffer(ServerPlayer p, State s, long now) {
        List<Item> items = DrogueConfig.sellable();
        String name = NAMES[RND.nextInt(NAMES.length)];
        int loyal = loyalty(p.server, p.getUUID(), name);
        // chaque client a son produit préféré (selon son nom), mais commande parfois autre chose
        Item item = items.isEmpty() ? ModRegistry.JOINT.get() : RND.nextInt(3) == 0 ? items.get(RND.nextInt(items.size())) : items.get(Math.floorMod(name.hashCode(), items.size()));
        int count = 1 + RND.nextInt(3) + loyal / 2;
        int minQ = Mth.clamp(1 + RND.nextInt(3) + (loyal >= 3 ? 1 : 0), 1, 4);
        double factor = Quality.priceFactor(minQ) * (1 + 0.06 * loyal) * (0.9 + RND.nextDouble() * 0.25);
        long cents = Math.max(100, Math.round(DrogueConfig.price(item) * count * factor / 100.0) * 100);
        return new Offer(++s.ids, name, item, count, minQ, cents, Math.floorMod(name.hashCode(), 9), now + OFFER_LIFE_MS);
    }

    /** Bouton « Accepter » du téléphone : un seul rendez-vous à la fois. */
    public static void accept(ServerPlayer p, int id) {
        State s = state(p);
        if (s.active != null) { p.displayClientMessage(Component.literal("§cTerminez ou annulez d'abord votre rendez-vous en cours."), true); return; }
        Offer o = s.offers.stream().filter(x -> x.id == id).findFirst().orElse(null);
        if (o == null) { p.displayClientMessage(Component.literal("§7Cette demande n'est plus d'actualité."), true); return; }
        BlockPos spot = meetingSpot(p.serverLevel(), p.blockPosition());
        if (spot == null) { p.displayClientMessage(Component.literal("§cAucun endroit discret trouvé, réessayez ailleurs."), true); return; }
        s.offers.remove(o);
        o.spot = spot;
        o.deadline = System.currentTimeMillis() + DrogueConfig.get(DrogueConfig.CLIENT_DEAL_MINUTES) * 60_000L;
        s.active = o;
        p.sendSystemMessage(Component.literal("§d§l[Deal] §r§d" + o.name + " : §f« Ok, on se retrouve en " + spot.getX() + " " + spot.getY() + " " + spot.getZ()
                + ". Viens seul. » §7(" + DrogueConfig.get(DrogueConfig.CLIENT_DEAL_MINUTES) + " min)"));
        p.playNotifySound(SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 0.8f, 1.6f);
    }

    public static void decline(ServerPlayer p, int id) { state(p).offers.removeIf(o -> o.id == id); }

    public static void cancel(ServerPlayer p) {
        State s = state(p);
        if (s.active == null) return;
        p.sendSystemMessage(Component.literal("§d§l[Deal] §r§d" + s.active.name + " : §f« Laisse tomber alors. »"));
        dropActive(p.server, s);
    }

    private static void dropActive(MinecraftServer server, State s) {
        if (s.active != null && s.active.entity != null) {
            for (ServerLevel l : server.getAllLevels()) if (l.getEntity(s.active.entity) instanceof CustomerEntity c) c.leave();
        }
        s.active = null;
    }

    /** Point de rendez-vous en surface, au sec, à 60–180 blocs. */
    private static BlockPos meetingSpot(ServerLevel level, BlockPos from) {
        for (int i = 0; i < 16; i++) {
            double a = RND.nextDouble() * Math.PI * 2, d = 60 + RND.nextDouble() * 120;
            int x = from.getX() + (int) (Math.cos(a) * d), z = from.getZ() + (int) (Math.sin(a) * d);
            BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
            if (top.getY() <= level.getMinBuildHeight() + 1) continue;
            if (!level.getFluidState(top.below()).isEmpty() || !level.getBlockState(top.below()).isSolidRender(level, top.below())) continue;
            if (!level.getBlockState(top).isAir() || !level.getBlockState(top.above()).isAir()) continue;
            return top;
        }
        return null;
    }

    // ------------------------------------------------------------------ rendez-vous

    /** Le client est-il encore attendu ? (sinon il disparaît) */
    static boolean expects(CustomerEntity c) {
        if (c.dealer() == null) return false;
        State s = STATES.get(c.dealer());
        return s != null && s.active != null && c.getUUID().equals(s.active.entity);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || STATES.isEmpty() || e.getServer().getTickCount() % 20 != 0) return;
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, State> en : STATES.entrySet()) {
            State s = en.getValue();
            Offer o = s.active;
            if (o == null) continue;
            ServerPlayer p = e.getServer().getPlayerList().getPlayer(en.getKey());
            if (p == null) continue;
            if (now > o.deadline) {
                p.sendSystemMessage(Component.literal("§d§l[Deal] §r§d" + o.name + " : §f« T'es où ? Je me casse. »"));
                dropActive(e.getServer(), s);
                continue;
            }
            double dist = Math.sqrt(p.blockPosition().distSqr(o.spot));
            if (o.entity == null && dist < SPAWN_RANGE && p.level() == p.server.overworld()) {
                CustomerEntity c = CUSTOMER.get().create(p.serverLevel());
                if (c != null) {
                    c.moveTo(o.spot.getX() + 0.5, o.spot.getY(), o.spot.getZ() + 0.5, RND.nextFloat() * 360f, 0);
                    c.setup(p.getUUID(), o.name, o.skin);
                    p.serverLevel().addFreshEntity(c);
                    o.entity = c.getUUID();
                }
            }
            p.displayClientMessage(Component.literal("§dRendez-vous avec " + o.name + " : §f" + (int) dist + " m " + direction(p, o.spot) + " §7· " + left(o.deadline - now)), true);
        }
    }

    private static String direction(ServerPlayer p, BlockPos to) {
        double dx = to.getX() + 0.5 - p.getX(), dz = to.getZ() + 0.5 - p.getZ();
        double target = Math.toDegrees(Math.atan2(-dx, dz));
        double rel = Mth.wrapDegrees(target - p.getYRot());
        String[] arrows = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};
        return arrows[Math.floorMod((int) Math.round(rel / 45.0), 8)];
    }

    private static String left(long ms) { return ms / 60_000 + " min " + (ms / 1000) % 60 + " s"; }

    /** Clic droit sur le client : remise de la marchandise et paiement. */
    static void deliver(ServerPlayer p, CustomerEntity c) {
        State s = STATES.get(p.getUUID());
        Offer o = s == null ? null : s.active;
        if (o == null || !c.getUUID().equals(o.entity)) {
            p.displayClientMessage(Component.literal("§7« J'te connais pas, toi. »"), true);
            return;
        }
        // on prend d'abord les doses de plus basse qualité qui conviennent
        List<ItemStack> stacks = new ArrayList<>();
        for (ItemStack st : p.getInventory().items) if (st.is(o.item) && Quality.get(st) >= o.minQuality) stacks.add(st);
        stacks.sort((a, b) -> Integer.compare(Quality.get(a), Quality.get(b)));
        int have = stacks.stream().mapToInt(ItemStack::getCount).sum();
        if (have < o.count) {
            p.displayClientMessage(Component.literal("§c« Il m'en faut " + o.count + " de " + new ItemStack(o.item).getHoverName().getString() + ", au moins "
                    + Quality.stars(o.minQuality) + ". T'as pas ça. »"), true);
            return;
        }
        int need = o.count, qualitySum = 0;
        for (ItemStack st : stacks) {
            int n = Math.min(need, st.getCount());
            qualitySum += Quality.get(st) * n;
            st.shrink(n);
            need -= n;
            if (need == 0) break;
        }
        double avg = qualitySum / (double) o.count;
        long bonus = Math.round(o.cents * 0.1 * Math.max(0, avg - o.minQuality) / 100.0) * 100;
        long total = o.cents + bonus;
        boolean paid = MineNorth.bank().refund(p.server, p.getUUID(), total, "drogue:vente");
        Loyalty.get(p.server).bump(p.getUUID(), o.name);
        int loyal = loyalty(p.server, p.getUUID(), o.name);
        p.sendSystemMessage(Component.literal("§d§l[Deal] §r§a" + (paid ? "+" + euros(total) + " sur votre compte" : "Payé... mais vous n'avez pas de compte bancaire")
                + (bonus > 0 ? " §6(dont " + euros(bonus) + " de bonus qualité)" : "") + " §7· fidélité " + "♥".repeat(loyal) + "♡".repeat(LOYALTY_MAX - loyal)));
        p.level().playSound(null, c.blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 0.8f, 1.0f);
        p.serverLevel().sendParticles(ParticleTypes.HAPPY_VILLAGER, c.getX(), c.getY() + 2.0, c.getZ(), 6, 0.3, 0.2, 0.3, 0);
        if (RND.nextInt(100) < DrogueConfig.get(DrogueConfig.CLIENT_POLICE_CHANCE))
            Odor.alertPolice(p.server, "Un témoin signale un échange suspect (" + new ItemStack(o.item).getHoverName().getString() + ").", c.blockPosition());
        s.active = null;
        c.leave();
    }

    public static String euros(long cents) { return cents % 100 == 0 ? cents / 100 + " €" : String.format("%.2f €", cents / 100.0); }

    // ------------------------------------------------------------------ fidélité (sauvegardée)

    static final class Loyalty extends SavedData {
        private final Map<UUID, Map<String, Integer>> data = new HashMap<>();

        static Loyalty get(MinecraftServer s) {
            return s.overworld().getDataStorage().computeIfAbsent(Loyalty::load, Loyalty::new, "minenorthdrogue_clients");
        }

        int get(UUID p, String name) { return data.getOrDefault(p, Map.of()).getOrDefault(name, 0); }

        void bump(UUID p, String name) {
            data.computeIfAbsent(p, k -> new HashMap<>()).merge(name, 1, (a, b) -> Math.min(LOYALTY_MAX, a + b));
            setDirty();
        }

        static Loyalty load(CompoundTag tag) {
            Loyalty l = new Loyalty();
            for (Tag t : tag.getList("players", Tag.TAG_COMPOUND)) {
                CompoundTag c = (CompoundTag) t;
                Map<String, Integer> m = new HashMap<>();
                CompoundTag names = c.getCompound("names");
                for (String k : names.getAllKeys()) m.put(k, names.getInt(k));
                l.data.put(c.getUUID("id"), m);
            }
            return l;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            ListTag list = new ListTag();
            for (Map.Entry<UUID, Map<String, Integer>> e : data.entrySet()) {
                CompoundTag c = new CompoundTag();
                c.putUUID("id", e.getKey());
                CompoundTag names = new CompoundTag();
                e.getValue().forEach(names::putInt);
                c.put("names", names);
                list.add(c);
            }
            tag.put("players", list);
            return tag;
        }
    }

    /** Identifiant d'objet (pour le téléphone). */
    public static String id(Item item) {
        ResourceLocation k = ForgeRegistries.ITEMS.getKey(item);
        return k == null ? "minecraft:air" : k.toString();
    }
}
