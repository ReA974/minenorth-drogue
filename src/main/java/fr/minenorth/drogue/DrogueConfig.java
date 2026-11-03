package fr.minenorth.drogue;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * config/minenorth_drogue.toml (serveur dédié). Effets : lignes « effet;secondes;niveau » (niveau 0 = I). Que des effets positifs par défaut ;
 * la limite est le délai d'attente entre deux prises et le coût de production, pas un effet négatif.
 */
public final class DrogueConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> JOINT_EFFECTS, COCAINE_EFFECTS, METH_EFFECTS, HEROIN_EFFECTS, SHROOM_EFFECTS, ECSTASY_EFFECTS, LSD_EFFECTS;
    public static final ForgeConfigSpec.IntValue JOINT_COOLDOWN, COCAINE_COOLDOWN, METH_COOLDOWN, HEROIN_COOLDOWN, SHROOM_COOLDOWN, ECSTASY_COOLDOWN, LSD_COOLDOWN;
    public static final ForgeConfigSpec.IntValue MDMA_LAB_SECONDS, MDMA_YIELD, LSD_LAB_SECONDS, LSD_YIELD, ERGOT_CHANCE, DRY_SECONDS, LAB_FIRE_CHANCE, VISUAL_SECONDS;
    public static final ForgeConfigSpec.BooleanValue VISUALS, ADDICTION, OVERDOSE;
    public static final ForgeConfigSpec.IntValue OVERDOSE_SECONDS, WITHDRAWAL_MINUTES, CLIENT_OFFERS, CLIENT_REFRESH_MINUTES, CLIENT_POLICE_CHANCE, CLIENT_DEAL_MINUTES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> CLIENT_PRICES;
    public static final ForgeConfigSpec.IntValue PACK_SECONDS, ROLL_SECONDS, LAB_LOT_MAX, HARVEST_MIN, HARVEST_MAX, METH_LAB_SECONDS, METH_YIELD, PASTE_LAB_SECONDS, PASTE_YIELD, OVEN_SECONDS, OVEN_MAX, HEROIN_LAB_SECONDS, HEROIN_YIELD;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.comment("Récolte").push("recolte");
        HARVEST_MIN = b.comment("Feuilles minimum par plante mûre (tentes)").defineInRange("feuillesMin", 6, 1, 64);
        HARVEST_MAX = b.comment("Feuilles maximum par plante mûre (tentes)").defineInRange("feuillesMax", 10, 1, 64);
        b.pop();
        b.comment("Laboratoire").push("labo");
        LAB_LOT_MAX = b.comment("Nombre maximum de lots fabriqués d'un coup (chaque lot en plus ajoute 50 % de temps)").defineInRange("lotsMax", 4, 1, 16);
        PASTE_LAB_SECONDS = b.comment("Durée de fabrication de la pâte de coca au laboratoire (4 feuilles de coca + 1 produit chimique)").defineInRange("pateSecondes", 30, 1, 3600);
        PASTE_YIELD = b.comment("Pâtes de coca produites par fabrication").defineInRange("pateRendement", 6, 1, 64);
        b.pop();
        b.comment("Four de cuisson (pâte de coca -> poudre de cocaïne)").push("four");
        OVEN_SECONDS = b.comment("Durée d'une cuisson (secondes)").defineInRange("secondes", 30, 1, 3600);
        OVEN_MAX = b.comment("Nombre maximum de pâtes cuites d'un coup").defineInRange("pateMax", 16, 1, 64);
        b.pop();
        b.comment("Table d'ensachage").push("ensachage");
        PACK_SECONDS = b.comment("Secondes pour faire un sachet (3 unités + 1 sachet vide)").defineInRange("secondesParSachet", 3, 1, 600);
        b.pop();
        b.comment("Table à rouler").push("roulage");
        ROLL_SECONDS = b.comment("Secondes pour rouler un lot (3 feuilles de cannabis + 1 papier = 2 joints)").defineInRange("secondesParLot", 4, 1, 600);
        b.pop();
        b.comment("Marijuana (joint)").push("marijuana");
        JOINT_EFFECTS = b.comment("Effets : effet;secondes;niveau (0 = niveau I)")
                .defineList("effets", List.of("minecraft:regeneration;20;0"), o -> o instanceof String);
        JOINT_COOLDOWN = b.comment("Délai avant de pouvoir en reprendre (secondes)").defineInRange("delaiSecondes", 120, 0, 36000);
        b.pop();
        b.comment("Cocaïne").push("cocaine");
        COCAINE_EFFECTS = b.defineList("effets", List.of("minecraft:speed;60;0", "minecraft:haste;60;0"), o -> o instanceof String);
        COCAINE_COOLDOWN = b.defineInRange("delaiSecondes", 240, 0, 36000);
        b.pop();
        b.comment("Méthamphétamine").push("meth");
        METH_EFFECTS = b.defineList("effets", List.of("minecraft:speed;45;1", "minecraft:haste;45;1", "minecraft:resistance;45;0"), o -> o instanceof String);
        METH_COOLDOWN = b.defineInRange("delaiSecondes", 360, 0, 36000);
        METH_LAB_SECONDS = b.comment("Durée de fabrication au laboratoire (3 éphédra + 1 produit chimique)").defineInRange("laboSecondes", 45, 1, 3600);
        METH_YIELD = b.comment("Unités de cristaux produites par fabrication (3 unités + 1 sachet vide = 1 sachet)").defineInRange("rendement", 6, 1, 64);
        b.pop();
        b.comment("Héroïne (pavot)").push("heroine");
        HEROIN_EFFECTS = b.defineList("effets", List.of("minecraft:resistance;60;0", "minecraft:regeneration;25;0"), o -> o instanceof String);
        HEROIN_COOLDOWN = b.defineInRange("delaiSecondes", 420, 0, 36000);
        HEROIN_LAB_SECONDS = b.comment("Durée de fabrication au laboratoire (4 capsules de pavot + 1 produit chimique)").defineInRange("laboSecondes", 50, 1, 3600);
        HEROIN_YIELD = b.comment("Doses de poudre produites par fabrication (3 doses + 1 sachet vide = 1 sachet)").defineInRange("rendement", 6, 1, 64);
        b.pop();
        b.comment("Champignons hallucinogènes (tente fermée, sans lampe)").push("champignons");
        SHROOM_EFFECTS = b.defineList("effets", List.of("minecraft:night_vision;90;0", "minecraft:jump_boost;60;0"), o -> o instanceof String);
        SHROOM_COOLDOWN = b.defineInRange("delaiSecondes", 300, 0, 36000);
        b.pop();
        b.comment("Ecstasy (huile de sassafras au labo, puis presse à pilules)").push("ecstasy");
        ECSTASY_EFFECTS = b.defineList("effets", List.of("minecraft:speed;90;0", "minecraft:jump_boost;90;0", "minecraft:saturation;2;0"), o -> o instanceof String);
        ECSTASY_COOLDOWN = b.defineInRange("delaiSecondes", 360, 0, 36000);
        MDMA_LAB_SECONDS = b.comment("Durée de fabrication au labo (2 huiles de sassafras + 1 produit chimique)").defineInRange("laboSecondes", 45, 1, 3600);
        MDMA_YIELD = b.comment("Doses de MDMA par fabrication (1 dose = 2 pilules à la presse)").defineInRange("rendement", 6, 1, 64);
        b.pop();
        b.comment("LSD (ergot de seigle au labo, puis buvards à la table à rouler)").push("lsd");
        LSD_EFFECTS = b.defineList("effets", List.of("minecraft:night_vision;180;0", "minecraft:luck;180;0"), o -> o instanceof String);
        LSD_COOLDOWN = b.defineInRange("delaiSecondes", 480, 0, 36000);
        LSD_LAB_SECONDS = b.comment("Durée de fabrication au labo (4 ergots + 1 produit chimique)").defineInRange("laboSecondes", 60, 1, 3600);
        LSD_YIELD = b.comment("Fioles de LSD par fabrication (1 fiole + 1 papier = 4 buvards)").defineInRange("rendement", 3, 1, 64);
        ERGOT_CHANCE = b.comment("Chance (sur 1000) de trouver de l'ergot en récoltant du blé mûr").defineInRange("ergotPourMille", 40, 0, 1000);
        b.pop();
        b.comment("Séchoir à cannabis").push("sechage");
        DRY_SECONDS = b.comment("Durée du séchage des feuilles (secondes) : +1 étoile").defineInRange("secondes", 300, 10, 36000);
        b.pop();
        b.comment("Accidents de labo").push("accidents");
        LAB_FIRE_CHANCE = b.comment("Chance (%) d'incendie à la fin d'une fabrication de 3 lots ou plus sans extracteur d'air à moins de 5 blocs").defineInRange("incendiePourcent", 15, 0, 100);
        b.pop();
        b.comment("Effets visuels, dépendance, overdose").push("sante");
        VISUALS = b.comment("Filtres visuels pendant la défonce (joint, héroïne, ecstasy, LSD, champignons)").define("filtresVisuels", true);
        VISUAL_SECONDS = b.comment("Durée du filtre visuel à 3 étoiles (secondes)").defineInRange("filtreSecondes", 45, 5, 600);
        ADDICTION = b.comment("Dépendance et manque").define("dependance", true);
        WITHDRAWAL_MINUTES = b.comment("Minutes sans drogue dure avant le manque (si dépendant)").defineInRange("manqueMinutes", 20, 1, 1440);
        OVERDOSE = b.comment("Overdose possible (mélange de drogues dures, héroïne très pure, dépendance extrême)").define("overdose", true);
        OVERDOSE_SECONDS = b.comment("Secondes pour administrer la naloxone avant le malaise (inconscience)").defineInRange("overdoseSecondes", 60, 10, 600);
        b.pop();
        b.comment("Clients (application Deal du téléphone MRPhone)").push("clients");
        CLIENT_OFFERS = b.comment("Demandes de clients en même temps").defineInRange("demandes", 3, 1, 8);
        CLIENT_REFRESH_MINUTES = b.comment("Minutes entre deux nouvelles demandes").defineInRange("nouvelleDemandeMinutes", 4, 1, 120);
        CLIENT_DEAL_MINUTES = b.comment("Minutes pour se rendre au rendez-vous").defineInRange("rendezVousMinutes", 10, 2, 60);
        CLIENT_POLICE_CHANCE = b.comment("Chance (%) qu'un témoin prévienne la police pendant un deal").defineInRange("policePourcent", 12, 0, 100);
        CLIENT_PRICES = b.comment("Prix de base par unité à 3 étoiles, en euros : objet;prix")
                .defineList("prix", List.of("minenorthdrogue:joint;35", "minenorthdrogue:cocaine;120", "minenorthdrogue:meth;140", "minenorthdrogue:heroin;160",
                        "minenorthdrogue:magic_mushroom;45", "minenorthdrogue:ecstasy;60", "minenorthdrogue:lsd_blotter;50"), o -> o instanceof String);
        b.pop();
        SPEC = b.build();
    }

    private DrogueConfig() {}

    public static boolean get(ForgeConfigSpec.BooleanValue v) {
        try { return v.get(); } catch (Exception e) { return v.getDefault(); }
    }

    public static boolean visuals() { return get(VISUALS); }

    /** Prix de base (centimes) d'un produit pour les clients, 0 s'il n'est pas dans la liste. */
    public static long price(net.minecraft.world.item.Item item) {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
        List<? extends String> lines;
        try { lines = CLIENT_PRICES.get(); } catch (Exception e) { lines = CLIENT_PRICES.getDefault(); }
        for (String l : lines) {
            String[] p = l.split(";");
            if (p.length == 2 && key != null && key.toString().equals(p[0].trim())) {
                try { return Math.round(Double.parseDouble(p[1].trim()) * 100); } catch (NumberFormatException ignored) {}
            }
        }
        return 0;
    }

    /** Produits vendables aux clients (ordre de la config). */
    public static List<net.minecraft.world.item.Item> sellable() {
        List<? extends String> lines;
        try { lines = CLIENT_PRICES.get(); } catch (Exception e) { lines = CLIENT_PRICES.getDefault(); }
        List<net.minecraft.world.item.Item> out = new ArrayList<>();
        for (String l : lines) {
            ResourceLocation id = ResourceLocation.tryParse(l.split(";")[0].trim());
            net.minecraft.world.item.Item it = id == null ? null : ForgeRegistries.ITEMS.getValue(id);
            if (it != null && it != net.minecraft.world.item.Items.AIR) out.add(it);
        }
        return out;
    }

    /** Lecture sûre : valeur par défaut si la config n'est pas chargée (client, menu principal). */
    public static int get(ForgeConfigSpec.IntValue v) {
        try { return v.get(); } catch (Exception e) { return v.getDefault(); }
    }

    public static List<MobEffectInstance> effects(ForgeConfigSpec.ConfigValue<List<? extends String>> v) {
        List<? extends String> lines;
        try { lines = v.get(); } catch (Exception e) { lines = v.getDefault(); }
        List<MobEffectInstance> out = new ArrayList<>();
        for (String line : lines) {
            String[] p = line.trim().split(";");
            ResourceLocation id = ResourceLocation.tryParse(p[0].trim());
            MobEffect eff = id == null ? null : ForgeRegistries.MOB_EFFECTS.getValue(id);
            if (eff == null) continue;
            try {
                int sec = p.length > 1 ? Integer.parseInt(p[1].trim()) : 30;
                int lvl = p.length > 2 ? Integer.parseInt(p[2].trim()) : 0;
                out.add(new MobEffectInstance(eff, Math.max(1, sec) * 20, Math.max(0, lvl)));
            } catch (NumberFormatException ignored) {}
        }
        return out;
    }
}
