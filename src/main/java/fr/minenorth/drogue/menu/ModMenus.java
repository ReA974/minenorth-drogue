package fr.minenorth.drogue.menu;

import fr.minenorth.drogue.MineNorthDrogue;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Écrans des machines : tout se fait depuis un écran (dépôt, lancement, mini-jeu, récupération). */
public final class ModMenus {
    private ModMenus() {}

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MineNorthDrogue.MOD_ID);

    public static final RegistryObject<MenuType<LabMenu>> LAB = MENUS.register("lab", () -> IForgeMenuType.create(LabMenu::new));
    public static final RegistryObject<MenuType<OvenMenu>> OVEN = MENUS.register("cooking_oven", () -> IForgeMenuType.create(OvenMenu::new));
    public static final RegistryObject<MenuType<PackMenu>> PACK = MENUS.register("pack_table", () -> IForgeMenuType.create(PackMenu::new));
    public static final RegistryObject<MenuType<RollMenu>> ROLL = MENUS.register("roll_table", () -> IForgeMenuType.create(RollMenu::new));
    public static final RegistryObject<MenuType<CutMenu>> CUT = MENUS.register("cut_table", () -> IForgeMenuType.create(CutMenu::new));
    public static final RegistryObject<MenuType<PressMenu>> PRESS = MENUS.register("pill_press", () -> IForgeMenuType.create(PressMenu::new));

    public static void register(IEventBus bus) { MENUS.register(bus); }
}
