package fr.minenorth.drogue;

import fr.minenorth.api.MineNorth;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Vector3f;

/**
 * Odeur de la drogue. Une tente où une plante pousse, un labo qui tourne ou un séchoir chargé sentent fort : volutes vertes visibles par tous,
 * sauf filtre à charbon à moins de 4 blocs. Les chiens K9 de la police (mod Animaux) la flairent à 7 blocs et marquent l'arrêt : leur maître est
 * prévenu. Ils flairent aussi un joueur qui porte de la drogue à 4 blocs. Alertes police / secours : voir alertPolice et alertSecours.
 */
@Mod.EventBusSubscriber(modid = MineNorthDrogue.MOD_ID)
public final class Odor {
    private Odor() {}

    private static final int FILTER_RADIUS = 4, VENT_RADIUS = 5, SNIFF_RADIUS = 7, SNIFF_PLAYER = 4;
    private static final String K9_TAG = "minenorthpets_k9", LAST_MARK = "minenorthdrogue_mark";
    private static final DustParticleOptions SMELL = new DustParticleOptions(new Vector3f(0.52f, 0.66f, 0.34f), 1.4f);

    /** Filtre à charbon à moins de 4 blocs : pas d'odeur. */
    public static boolean filtered(Level level, BlockPos pos) { return near(level, pos, FILTER_RADIUS, ModRegistry.CARBON_FILTER.get()); }

    /** Extracteur d'air à moins de 5 blocs : pas d'accident de labo. */
    public static boolean vented(Level level, BlockPos pos) { return near(level, pos, VENT_RADIUS, ModRegistry.AIR_EXTRACTOR.get()); }

    private static boolean near(Level level, BlockPos pos, int r, net.minecraft.world.level.block.Block block) {
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-r, -2, -r), pos.offset(r, 3, r))) if (level.getBlockState(p).is(block)) return true;
        return false;
    }

    /** Volute d'odeur (côté client, dans animateTick). */
    public static void puff(Level level, BlockPos pos, RandomSource rnd) {
        level.addParticle(SMELL, pos.getX() + 0.2 + rnd.nextDouble() * 0.6, pos.getY() + rnd.nextDouble() * 0.3, pos.getZ() + 0.2 + rnd.nextDouble() * 0.6, 0, 0.02, 0);
    }

    /** Ce bloc sent-il la drogue en ce moment ? */
    public static boolean smelly(BlockState s) {
        if (s.getBlock() instanceof TentBlock) return !s.getValue(TentBlock.TOP) && s.getValue(TentBlock.PLANT) != TentBlock.Plant.NONE && s.getValue(TentBlock.AGE) >= 1;
        if (s.getBlock() instanceof LabBlock) return s.getValue(LabBlock.ANIM) != 0;
        if (s.getBlock() instanceof DryingRackBlock) return s.getValue(DryingRackBlock.FILL) > 0;
        return false;
    }

    /** Objet que flaire un chien : plantes, produits, sachets (tout ce qui a une qualité ou un effet de drogue). */
    public static boolean drug(ItemStack st) {
        if (st.isEmpty()) return false;
        if (st.getItem() instanceof DrugItem) return true;
        ResourceLocation k = ForgeRegistries.ITEMS.getKey(st.getItem());
        return k != null && MineNorthDrogue.MOD_ID.equals(k.getNamespace()) && DrugItems.SMELLY.contains(k.getPath());
    }

    public static boolean carriesDrugs(Player p) {
        for (ItemStack st : p.getInventory().items) if (drug(st)) return true;
        return drug(p.getOffhandItem());
    }

    // ------------------------------------------------------------------ chiens K9

    @SubscribeEvent
    public static void dogTick(LivingEvent.LivingTickEvent e) {
        LivingEntity dog = e.getEntity();
        if (dog.tickCount % 40 != 7 || !(dog.level() instanceof ServerLevel sl) || !dog.getPersistentData().getBoolean(K9_TAG)) return;
        long now = sl.getGameTime();
        if (now - dog.getPersistentData().getLong(LAST_MARK) < 20 * 45) return;
        BlockPos found = null;
        String what = null;
        for (Player p : sl.getEntitiesOfClass(Player.class, dog.getBoundingBox().inflate(SNIFF_PLAYER))) {
            if (p.isSpectator() || p.isCreative() || isOwner(dog, p) || !carriesDrugs(p)) continue;
            found = p.blockPosition();
            what = "sur " + (p instanceof ServerPlayer sp ? MineNorth.displayName(sp) : p.getName().getString());
            break;
        }
        if (found == null) {
            BlockPos c = dog.blockPosition();
            for (BlockPos p : BlockPos.betweenClosed(c.offset(-SNIFF_RADIUS, -3, -SNIFF_RADIUS), c.offset(SNIFF_RADIUS, 4, SNIFF_RADIUS))) {
                if (smelly(sl.getBlockState(p)) && !filtered(sl, p)) { found = p.immutable(); what = "en " + p.getX() + " " + p.getY() + " " + p.getZ(); break; }
            }
        }
        if (found == null) return;
        dog.getPersistentData().putLong(LAST_MARK, now);
        sl.playSound(null, dog.blockPosition(), SoundEvents.WOLF_GROWL, SoundSource.NEUTRAL, 1.0f, 1.0f);
        sl.sendParticles(ParticleTypes.NOTE, dog.getX(), dog.getY() + dog.getBbHeight() + 0.4, dog.getZ(), 3, 0.3, 0.1, 0.3, 0.5);
        if (dog instanceof net.minecraft.world.entity.Mob mob) mob.getLookControl().setLookAt(found.getX() + 0.5, found.getY() + 0.5, found.getZ() + 0.5);
        if (dog instanceof OwnableEntity own && own.getOwner() instanceof ServerPlayer owner)
            owner.sendSystemMessage(Component.literal("§9§l[K9] §r§9Votre chien marque l'arrêt : odeur de drogue §f" + what + "§9."));
    }

    private static boolean isOwner(LivingEntity dog, Player p) { return dog instanceof OwnableEntity own && p.getUUID().equals(own.getOwnerUUID()); }

    // ------------------------------------------------------------------ alertes

    /** Prévient les policiers en service (ou tous les policiers connectés si personne n'est en service). */
    public static void alertPolice(MinecraftServer s, String text, BlockPos pos) {
        var police = MineNorth.police();
        var all = police.online(s);
        var duty = all.stream().filter(p -> police.onDuty(s, p.getUUID())).toList();
        for (ServerPlayer p : duty.isEmpty() ? all : duty) {
            p.sendSystemMessage(Component.literal("§c§l[Police] §r§c" + text + " §7(" + pos.getX() + " " + pos.getY() + " " + pos.getZ() + ")"));
            p.playNotifySound(SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.8f, 1.2f);
        }
    }

    /** Prévient les pompiers / SAMU en service (ou tous ceux connectés si personne n'est en service). */
    public static void alertSecours(MinecraftServer s, String text, BlockPos pos) {
        var sec = MineNorth.secours();
        var all = s.getPlayerList().getPlayers().stream().filter(sec::isSecours).toList();
        var duty = all.stream().filter(p -> sec.onDuty(s, p.getUUID())).toList();
        for (ServerPlayer p : duty.isEmpty() ? all : duty) {
            p.sendSystemMessage(Component.literal("§6§l[Secours] §r§6" + text + " §7(" + pos.getX() + " " + pos.getY() + " " + pos.getZ() + ")"));
            p.playNotifySound(SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.8f, 0.8f);
        }
    }
}
