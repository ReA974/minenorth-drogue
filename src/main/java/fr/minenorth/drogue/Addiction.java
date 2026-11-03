package fr.minenorth.drogue;

import fr.minenorth.api.MineNorth;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Dépendance, manque et overdose (données du joueur « minenorthdrogue_sante », gardées à la mort).
 * <ul>
 * <li>Chaque prise ajoute de la dépendance (rien pour le LSD et les champignons, peu pour le joint, beaucoup pour l'héroïne) ; elle baisse de 1 par
 * minute. Dépendant (40 et plus) et sans drogue dure depuis manqueMinutes : manque (nausée, lenteur, faiblesse) toutes les 3 minutes.</li>
 * <li>Overdose : deux drogues dures différentes à moins de 3 minutes (une chance sur trois), héroïne 5 étoiles sans accoutumance, ou dépendance
 * extrême. Le joueur s'effondre à moitié (aveugle, très lent) ; un passant appelle les secours. Sans naloxone avant la fin du délai, il tombe
 * inconscient (dégâts pris en charge par le mod Secours, qui le met dans le coma au lieu de le tuer).</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = MineNorthDrogue.MOD_ID)
public final class Addiction {
    private Addiction() {}

    private static final String KEY = "minenorthdrogue_sante";
    private static final long COMBO_MS = 180_000, WITHDRAWAL_EVERY_MS = 180_000;
    private static final int DEPENDENT = 40;

    private static CompoundTag data(ServerPlayer p) {
        CompoundTag root = p.getPersistentData();
        if (!root.contains(KEY)) root.put(KEY, new CompoundTag());
        return root.getCompound(KEY);
    }

    private static boolean hard(DrugItem.Kind k) { return k == DrugItem.Kind.COKE || k == DrugItem.Kind.METH || k == DrugItem.Kind.HEROIN || k == DrugItem.Kind.PILL; }

    private static int addictiveness(DrugItem.Kind k) {
        return switch (k) {
            case JOINT -> 3;
            case PILL -> 7;
            case COKE -> 12;
            case METH -> 15;
            case HEROIN -> 20;
            default -> 0;
        };
    }

    /** Dépendance actuelle (0 à 100), après la baisse de 1 par minute depuis la dernière mise à jour. */
    public static int dependence(ServerPlayer p) {
        CompoundTag d = data(p);
        long now = System.currentTimeMillis();
        int dep = d.getInt("dep");
        long since = now - d.getLong("depAt");
        if (dep > 0 && since >= 60_000) {
            dep = Math.max(0, dep - (int) (since / 60_000));
            d.putInt("dep", dep);
            d.putLong("depAt", now - since % 60_000);
        } else if (dep == 0) d.putLong("depAt", now);
        return dep;
    }

    /** Appelé à chaque prise (DrugItem.finishUsingItem). */
    public static void dose(ServerPlayer p, DrugItem.Kind kind, int quality) {
        CompoundTag d = data(p);
        long now = System.currentTimeMillis();
        int dep = dependence(p);
        if (DrogueConfig.get(DrogueConfig.ADDICTION)) d.putInt("dep", Math.min(100, dep + addictiveness(kind)));
        boolean od = false;
        if (hard(kind) && DrogueConfig.get(DrogueConfig.OVERDOSE)) {
            String last = d.getString("lastHard");
            boolean combo = !last.isEmpty() && !last.equals(kind.name()) && now - d.getLong("lastHardAt") < COMBO_MS;
            if (combo && p.getRandom().nextInt(3) == 0) od = true;
            else if (kind == DrugItem.Kind.HEROIN && quality >= 5 && dep < 20 && p.getRandom().nextInt(100) < 15) od = true;
            else if (dep >= 90 && p.getRandom().nextInt(100) < 10) od = true;
        }
        if (hard(kind)) {
            d.putString("lastHard", kind.name());
            d.putLong("lastHardAt", now);
            if (p.hasEffect(DrugEffects.WITHDRAWAL.get())) {
                p.removeEffect(DrugEffects.WITHDRAWAL.get());
                p.removeEffect(MobEffects.CONFUSION);
            }
        }
        if (od) overdose(p);
        else if (DrogueConfig.get(DrogueConfig.ADDICTION) && dep + addictiveness(kind) >= DEPENDENT && dep < DEPENDENT)
            p.sendSystemMessage(Component.literal("§8Vous sentez que votre corps commence à en réclamer…"));
    }

    public static boolean overdosing(ServerPlayer p) { return data(p).getLong("odUntil") > 0; }

    private static void overdose(ServerPlayer p) {
        int sec = DrogueConfig.get(DrogueConfig.OVERDOSE_SECONDS);
        data(p).putLong("odUntil", System.currentTimeMillis() + sec * 1000L);
        p.addEffect(new MobEffectInstance(DrugEffects.OVERDOSE.get(), sec * 20, 0, false, false, true));
        p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, sec * 20, 0, false, false));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, sec * 20, 3, false, false));
        p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, sec * 20, 0, false, false));
        p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, sec * 20, 2, false, false));
        p.sendSystemMessage(Component.literal("§4§lOVERDOSE §r§c— votre cœur s'emballe, tout devient noir. Il vous faut de la naloxone (" + sec + " s)."));
        p.level().playSound(null, p.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.0f, 1.3f);
        if (p.getRandom().nextInt(10) < 7)
            Odor.alertSecours(p.server, "Un passant signale une personne en overdose : " + MineNorth.displayName(p) + ". Naloxone requise.", p.blockPosition());
    }

    /** Naloxone : arrête l'overdose (et le manque, brutalement). Vrai s'il y avait quelque chose à soigner. */
    public static boolean naloxone(ServerPlayer p) {
        boolean od = overdosing(p);
        data(p).putLong("odUntil", 0);
        for (var e : new net.minecraft.world.effect.MobEffect[]{DrugEffects.OVERDOSE.get(), MobEffects.BLINDNESS, MobEffects.MOVEMENT_SLOWDOWN, MobEffects.CONFUSION,
                MobEffects.WEAKNESS, DrugEffects.NOD.get()}) p.removeEffect(e);
        if (od) p.sendSystemMessage(Component.literal("§aLa naloxone fait effet : vous reprenez votre souffle."));
        return od;
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p) || p.tickCount % 20 != 0) return;
        CompoundTag d = data(p);
        long now = System.currentTimeMillis();
        long od = d.getLong("odUntil");
        if (od > 0) {
            if (now >= od) {
                d.putLong("odUntil", 0);
                p.removeEffect(DrugEffects.OVERDOSE.get());
                p.sendSystemMessage(Component.literal("§4Vous perdez connaissance…"));
                p.hurt(p.damageSources().magic(), p.getHealth() + 40f);
            } else if (p.tickCount % 60 == 0) p.level().playSound(null, p.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 0.9f, 1.5f);
            return;
        }
        if (!DrogueConfig.get(DrogueConfig.ADDICTION) || p.tickCount % 200 != 0 || p.isCreative()) return;
        long lastHard = d.getLong("lastHardAt");
        if (dependence(p) < DEPENDENT || lastHard == 0 || now - lastHard < DrogueConfig.get(DrogueConfig.WITHDRAWAL_MINUTES) * 60_000L) return;
        if (now - d.getLong("withdrawalAt") < WITHDRAWAL_EVERY_MS) return;
        d.putLong("withdrawalAt", now);
        p.addEffect(new MobEffectInstance(DrugEffects.WITHDRAWAL.get(), 20 * 60, 0, false, false, true));
        p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 20 * 8, 0, false, false));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 * 40, 0, false, false));
        p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 40, 0, false, false));
        p.displayClientMessage(Component.literal("§8Vous êtes en manque : sueurs froides, mains qui tremblent…"), true);
    }

    /** La santé (dépendance) survit à la mort. */
    @SubscribeEvent
    public static void clone(PlayerEvent.Clone e) {
        CompoundTag old = e.getOriginal().getPersistentData();
        if (old.contains(KEY)) {
            CompoundTag c = old.getCompound(KEY).copy();
            c.putLong("odUntil", 0);
            e.getEntity().getPersistentData().put(KEY, c);
        }
    }
}
