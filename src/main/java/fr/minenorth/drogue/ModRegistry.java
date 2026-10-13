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

    // ---- cultures : cannabis et coca ne poussent que sous tente ; l'éphédra pousse en plein champ
    public static final RegistryObject<Item> CANNABIS_SEEDS = ITEMS.register("cannabis_seeds", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> COCA_SEEDS = ITEMS.register("coca_seeds", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Block> EPHEDRA_CROP = BLOCKS.register("ephedra_crop", () -> new DrugCropBlock(() -> ModRegistry.EPHEDRA_SEEDS.get()));
    public static final RegistryObject<Item> EPHEDRA_SEEDS = ITEMS.register("ephedra_seeds", () -> new ItemNameBlockItem(EPHEDRA_CROP.get(), new Item.Properties()));

    // ---- tente de culture 2 x 2 et lampe UV
    public static final RegistryObject<Block> TENT = BLOCKS.register("grow_tent", TentBlock::new);
    public static final RegistryObject<Item> GROW_TENT = ITEMS.register("grow_tent", () -> new TentItem(TENT.get(), new Item.Properties().stacksTo(8)));
    public static final RegistryObject<Item> UV_LAMP = ITEMS.register("uv_lamp", () -> new Item(new Item.Properties().stacksTo(8)));

    // ---- laboratoire
    public static final RegistryObject<Block> LAB = BLOCKS.register("lab", LabBlock::new);
    public static final RegistryObject<Item> LAB_ITEM = ITEMS.register("lab", () -> new LabItem(LAB.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<LabBE>> LAB_BE = BLOCK_ENTITIES.register("lab",
            () -> BlockEntityType.Builder.of(LabBE::new, LAB.get()).build(null));

    /** Objets du mod dans l'onglet créatif commun « MineNorth » (voir SharedTab). */
    public static final net.minecraft.world.item.CreativeModeTab.DisplayItemsGenerator TAB_ITEMS =
            (params, out) -> ITEMS.getEntries().forEach(i -> out.accept(i.get()));

    // ---- table d'ensachage
    public static final RegistryObject<Block> PACK = BLOCKS.register("pack_table", PackBlock::new);
    public static final RegistryObject<Item> PACK_ITEM = ITEMS.register("pack_table", () -> new BlockItem(PACK.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<PackBE>> PACK_BE = BLOCK_ENTITIES.register("pack_table",
            () -> BlockEntityType.Builder.of(PackBE::new, PACK.get()).build(null));

    // ---- table à rouler
    public static final RegistryObject<Block> ROLL = BLOCKS.register("roll_table", RollBlock::new);
    public static final RegistryObject<Item> ROLL_ITEM = ITEMS.register("roll_table", () -> new BlockItem(ROLL.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<RollBE>> ROLL_BE = BLOCK_ENTITIES.register("roll_table",
            () -> BlockEntityType.Builder.of(RollBE::new, ROLL.get()).build(null));

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
    }
}
