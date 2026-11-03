package fr.minenorth.drogue.menu;

import fr.minenorth.drogue.CutBE;
import fr.minenorth.drogue.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraftforge.items.ItemStackHandler;

/** Table de coupe : produit, additif, bouton Couper, produit coupé. */
public class CutMenu extends StationMenu {
    public static final int BTN_CUT = 0;
    private final CutBE be;
    public final ItemStackHandler items;

    public CutMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, buf.readBlockPos(), CutBE.newHandler(() -> {}), null);
    }

    public CutMenu(int id, Inventory inv, CutBE be) {
        this(id, inv, be.getBlockPos(), be.items(), be);
    }

    private CutMenu(int id, Inventory inv, BlockPos pos, ItemStackHandler h, CutBE be) {
        super(ModMenus.CUT.get(), id, inv, pos, ModRegistry.CUT_TABLE.get(), new SimpleContainerData(0));
        this.be = be;
        this.items = h;
        machineSlot(h, CutBE.PRODUCT, 26, 62, false, () -> false);
        machineSlot(h, CutBE.ADDITIVE, 26, 98, false, () -> false);
        machineSlot(h, CutBE.OUT, 204, 80, true, () -> false);
        playerInventory(inv, 140);
    }

    @Override
    public boolean clickMenuButton(Player p, int id) {
        if (id == BTN_CUT && be != null) be.cut(p);
        return true;
    }
}
