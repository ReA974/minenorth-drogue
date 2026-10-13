package fr.minenorth.drogue;

import net.minecraft.world.item.BlockItem;
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

    // ---- objets (déclarés avant les blocs : les cultures y lisent leurs graines)
    public static final RegistryObject<Item> CANNABIS_LEAF = ITEMS.register("cannabis_leaf", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> COCA_LEAF = ITEMS.register("coca_leaf", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> EPHEDRA = ITEMS.register("ephedra", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> CHEMICAL = ITEMS.register("chemical", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> JOINT = ITEMS.register("joint", DrugItem::joint);
    public static final RegistryObject<Item> COCAINE = ITEMS.register("cocaine", DrugItem::cocaine);
    public static final RegistryObject<Item> METH = ITEMS.register("meth", DrugItem::meth);

    // ---- cultures
    public static final RegistryObject<Block> CANNABIS_CROP = BLOCKS.register("cannabis_crop", () -> new DrugCropBlock(() -> ModRegistry.CANNABIS_SEEDS.get()));
    public static final RegistryObject<Block> COCA_CROP = BLOCKS.register("coca_crop", () -> new DrugCropBlock(() -> ModRegistry.COCA_SEEDS.get()));
    public static final RegistryObject<Block> EPHEDRA_CROP = BLOCKS.register("ephedra_crop", () -> new DrugCropBlock(() -> ModRegistry.EPHEDRA_SEEDS.get()));
    public static final RegistryObject<Item> CANNABIS_SEEDS = ITEMS.register("cannabis_seeds", () -> new ItemNameBlockItem(CANNABIS_CROP.get(), new Item.Properties()));
    public static final RegistryObject<Item> COCA_SEEDS = ITEMS.register("coca_seeds", () -> new ItemNameBlockItem(COCA_CROP.get(), new Item.Properties()));
    public static final RegistryObject<Item> EPHEDRA_SEEDS = ITEMS.register("ephedra_seeds", () -> new ItemNameBlockItem(EPHEDRA_CROP.get(), new Item.Properties()));

    // ---- laboratoire
    public static final RegistryObject<Block> LAB = BLOCKS.register("lab", LabBlock::new);
    public static final RegistryObject<Item> LAB_ITEM = ITEMS.register("lab", () -> new BlockItem(LAB.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<LabBE>> LAB_BE = BLOCK_ENTITIES.register("lab",
            () -> BlockEntityType.Builder.of(LabBE::new, LAB.get()).build(null));

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
    }
}
