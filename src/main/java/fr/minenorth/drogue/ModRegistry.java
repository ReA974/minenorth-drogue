package fr.minenorth.drogue;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Enregistrements : cultures, graines, produits, laboratoire. */
public final class ModRegistry {
    private ModRegistry() {}

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MineNorthDrogue.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MineNorthDrogue.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MineNorthDrogue.MOD_ID);

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MineNorthDrogue.MOD_ID);

    // ---- objets (déclarés avant les blocs : les cultures y lisent leurs graines)
    public static final RegistryObject<Item> CANNABIS_LEAF = ITEMS.register("cannabis_leaf", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> COCA_LEAF = ITEMS.register("coca_leaf", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> EPHEDRA = ITEMS.register("ephedra", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> CHEMICAL = ITEMS.register("chemical", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> EMPTY_BAG = ITEMS.register("empty_bag", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> COCAINE_POWDER = ITEMS.register("cocaine_powder", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> METH_CRYSTALS = ITEMS.register("meth_crystals", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> JOINT = ITEMS.register("joint", DrugItem::joint);
    public static final RegistryObject<Item> COCAINE = ITEMS.register("cocaine", DrugItem::cocaine);
    public static final RegistryObject<Item> METH = ITEMS.register("meth", DrugItem::meth);
    public static final RegistryObject<Item> HEROIN_POWDER = ITEMS.register("heroin_powder", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> HEROIN = ITEMS.register("heroin", DrugItem::heroin);
    public static final RegistryObject<Item> POPPY_POD = ITEMS.register("poppy_pod", () -> new Item(new Item.Properties()));

    // ---- cultures : cannabis et coca ne poussent que sous tente ; l'éphédra pousse en plein champ
    public static final RegistryObject<Item> CANNABIS_SEEDS = ITEMS.register("cannabis_seeds", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> COCA_SEEDS = ITEMS.register("coca_seeds", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> POPPY_SEEDS = ITEMS.register("poppy_seeds", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Block> EPHEDRA_CROP = BLOCKS.register("ephedra_crop", () -> new DrugCropBlock(() -> ModRegistry.EPHEDRA_SEEDS.get()));
    public static final RegistryObject<Item> EPHEDRA_SEEDS = ITEMS.register("ephedra_seeds", () -> new ItemNameBlockItem(EPHEDRA_CROP.get(), new Item.Properties()));

    // ---- tente de culture 2 x 2 et lampe UV
    public static final RegistryObject<Block> TENT = BLOCKS.register("grow_tent", TentBlock::new);
    public static final RegistryObject<Item> GROW_TENT_SMALL = ITEMS.register("grow_tent_small", () -> new TentItem(TENT.get(), TentBlock.Layout.S11, new Item.Properties().stacksTo(8)));
    public static final RegistryObject<Item> GROW_TENT_LONG = ITEMS.register("grow_tent_long", () -> new TentItem(TENT.get(), TentBlock.Layout.S12, new Item.Properties().stacksTo(8)));
    public static final RegistryObject<Item> WATERING_CAN = ITEMS.register("watering_can", WateringCanItem::new);
    public static final RegistryObject<Item> COCA_PASTE = ITEMS.register("coca_paste", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SOIL = ITEMS.register("potting_soil", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> UV_LAMP = ITEMS.register("uv_lamp", () -> new Item(new Item.Properties().stacksTo(8)));

    // ---- laboratoire
    public static final RegistryObject<Block> LAB = BLOCKS.register("lab", LabBlock::new);
    public static final RegistryObject<Item> LAB_ITEM = ITEMS.register("lab", () -> new LabItem(LAB.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<LabBE>> LAB_BE = BLOCK_ENTITIES.register("lab",
            () -> BlockEntityType.Builder.of(LabBE::new, LAB.get()).build(null));

    // ---- four de cuisson (pâte de coca -> poudre)
    public static final RegistryObject<Block> OVEN = BLOCKS.register("cooking_oven", OvenBlock::new);
    public static final RegistryObject<Item> OVEN_ITEM = ITEMS.register("cooking_oven", () -> new net.minecraft.world.item.BlockItem(OVEN.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<OvenBE>> OVEN_BE = BLOCK_ENTITIES.register("cooking_oven",
            () -> BlockEntityType.Builder.of(OvenBE::new, OVEN.get()).build(null));

    /** Objets du mod dans l'onglet créatif commun « MineNorth » (voir SharedTab). */
    public static final net.minecraft.world.item.CreativeModeTab.DisplayItemsGenerator TAB_ITEMS =
            (params, out) -> ITEMS.getEntries().forEach(i -> out.accept(i.get()));

    // ---- table d'ensachage
    public static final RegistryObject<Block> PACK = BLOCKS.register("pack_table", PackBlock::new);
    public static final RegistryObject<Item> PACK_ITEM = ITEMS.register("pack_table", () -> new PackItem(PACK.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<PackBE>> PACK_BE = BLOCK_ENTITIES.register("pack_table",
            () -> BlockEntityType.Builder.of(PackBE::new, PACK.get()).build(null));

    // ---- table à rouler
    public static final RegistryObject<Block> ROLL = BLOCKS.register("roll_table", RollBlock::new);
    public static final RegistryObject<Item> ROLL_ITEM = ITEMS.register("roll_table", () -> new RollItem(ROLL.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<RollBE>> ROLL_BE = BLOCK_ENTITIES.register("roll_table",
            () -> BlockEntityType.Builder.of(RollBE::new, ROLL.get()).build(null));

    // ---- qualité et matériel de culture
    public static final RegistryObject<Item> FERTILIZER = ITEMS.register("fertilizer", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> LED_LAMP = ITEMS.register("led_lamp", () -> new Item(new Item.Properties().stacksTo(8)));
    public static final RegistryObject<Item> DRIP_KIT = ITEMS.register("drip_kit", () -> new Item(new Item.Properties().stacksTo(8)));
    public static final RegistryObject<Item> DRIED_CANNABIS = ITEMS.register("dried_cannabis", () -> new Item(new Item.Properties()));

    // ---- champignons, ecstasy, LSD
    public static final RegistryObject<Item> MUSHROOM_SPORES = ITEMS.register("mushroom_spores", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> MAGIC_MUSHROOM = ITEMS.register("magic_mushroom", DrugItem::mushroom);
    public static final RegistryObject<Item> SASSAFRAS_OIL = ITEMS.register("sassafras_oil", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> MDMA = ITEMS.register("mdma_crystals", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> ECSTASY = ITEMS.register("ecstasy", DrugItem::ecstasy);
    public static final RegistryObject<Item> ERGOT = ITEMS.register("ergot", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> LSD_LIQUID = ITEMS.register("lsd_liquid", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> LSD = ITEMS.register("lsd_blotter", DrugItem::lsd);

    // ---- coupe
    public static final RegistryObject<Item> BAKING_SODA = ITEMS.register("baking_soda", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> PARACETAMOL = ITEMS.register("paracetamol", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> CAFFEINE = ITEMS.register("caffeine", () -> new Item(new Item.Properties()));

    // ---- revente et santé
    public static final RegistryObject<Item> NALOXONE = ITEMS.register("naloxone", NaloxoneItem::new);

    // ---- table de coupe
    public static final RegistryObject<Block> CUT_TABLE = BLOCKS.register("cut_table", CutBlock::new);
    public static final RegistryObject<Item> CUT_TABLE_ITEM = ITEMS.register("cut_table", () -> new BlockItem(CUT_TABLE.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<CutBE>> CUT_BE = BLOCK_ENTITIES.register("cut_table",
            () -> BlockEntityType.Builder.of(CutBE::new, CUT_TABLE.get()).build(null));

    // ---- séchoir à cannabis
    public static final RegistryObject<Block> DRYING_RACK = BLOCKS.register("drying_rack", DryingRackBlock::new);
    public static final RegistryObject<Item> DRYING_RACK_ITEM = ITEMS.register("drying_rack", () -> new BlockItem(DRYING_RACK.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<DryingRackBE>> DRYING_BE = BLOCK_ENTITIES.register("drying_rack",
            () -> BlockEntityType.Builder.of(DryingRackBE::new, DRYING_RACK.get()).build(null));

    // ---- presse à pilules (ecstasy)
    public static final RegistryObject<Block> PILL_PRESS = BLOCKS.register("pill_press", PressBlock::new);
    public static final RegistryObject<Item> PILL_PRESS_ITEM = ITEMS.register("pill_press", () -> new BlockItem(PILL_PRESS.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<PressBE>> PRESS_BE = BLOCK_ENTITIES.register("pill_press",
            () -> BlockEntityType.Builder.of(PressBE::new, PILL_PRESS.get()).build(null));

    // ---- ventilation : extracteur (pas d'accident de labo) et filtre à charbon (pas d'odeur)
    public static final RegistryObject<Block> AIR_EXTRACTOR = BLOCKS.register("air_extractor", () -> new DecorBlock(DecorBlock.Shape.EXTRACTOR));
    public static final RegistryObject<BlockEntityType<ExtractorBE>> EXTRACTOR_BE = BLOCK_ENTITIES.register("air_extractor",
            () -> BlockEntityType.Builder.of(ExtractorBE::new, AIR_EXTRACTOR.get()).build(null));
    public static final RegistryObject<Item> AIR_EXTRACTOR_ITEM = ITEMS.register("air_extractor", () -> new BlockItem(AIR_EXTRACTOR.get(), new Item.Properties()));
    public static final RegistryObject<Block> CARBON_FILTER = BLOCKS.register("carbon_filter", () -> new DecorBlock(DecorBlock.Shape.FILTER));
    public static final RegistryObject<Item> CARBON_FILTER_ITEM = ITEMS.register("carbon_filter", () -> new BlockItem(CARBON_FILTER.get(), new Item.Properties()));

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        DrugEffects.EFFECTS.register(bus);
        Customers.ENTITIES.register(bus);
        fr.minenorth.drogue.menu.ModMenus.register(bus);
    }
}
