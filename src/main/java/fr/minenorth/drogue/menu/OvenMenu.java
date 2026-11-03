package fr.minenorth.drogue.menu;

import fr.minenorth.drogue.ModRegistry;
import fr.minenorth.drogue.OvenBE;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraftforge.items.ItemStackHandler;

/** Four de cuisson : pâte de coca, bouton « Cuire », poudre de cocaïne. */
public class OvenMenu extends StationMenu {
    public static final int BTN_START = 0;
    private final OvenBE be;
    public final ItemStackHandler items;

    public OvenMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, buf.readBlockPos(), OvenBE.newHandler(() -> {}), new SimpleContainerData(OvenBE.DATA), null);
    }

    public OvenMenu(int id, Inventory inv, OvenBE be) {
        this(id, inv, be.getBlockPos(), be.items(), new ContainerData() {
            @Override public int get(int i) { return be.data(i); }
            @Override public void set(int i, int v) {}
            @Override public int getCount() { return OvenBE.DATA; }
        }, be);
    }

    private OvenMenu(int id, Inventory inv, BlockPos pos, ItemStackHandler h, ContainerData data, OvenBE be) {
        super(ModMenus.OVEN.get(), id, inv, pos, ModRegistry.OVEN.get(), data);
        this.be = be;
        this.items = h;
        machineSlot(h, OvenBE.PASTE, 26, 75, false, () -> false);
        machineSlot(h, OvenBE.POWDER, 204, 75, true, () -> false);
        playerInventory(inv, 136);
    }

    @Override
    public boolean clickMenuButton(Player p, int id) {
        if (id == BTN_START && be != null) be.start(p);
        return true;
    }
}
