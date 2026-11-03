package fr.minenorth.drogue.menu;

import fr.minenorth.drogue.LabBE;
import fr.minenorth.drogue.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraftforge.items.ItemStackHandler;

/** Laboratoire : plante + produit chimique, bouton « Lancer », produit fini. */
public class LabMenu extends StationMenu {
    public static final int BTN_START = 0;
    private final LabBE be;
    public final ItemStackHandler items;

    public LabMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, buf.readBlockPos(), LabBE.newHandler(() -> {}), new SimpleContainerData(LabBE.DATA), null);
    }

    public LabMenu(int id, Inventory inv, LabBE be) {
        this(id, inv, be.getBlockPos(), be.items(), new ContainerData() {
            @Override public int get(int i) { return be.data(i); }
            @Override public void set(int i, int v) {}
            @Override public int getCount() { return LabBE.DATA; }
        }, be);
    }

    private LabMenu(int id, Inventory inv, BlockPos pos, ItemStackHandler h, ContainerData data, LabBE be) {
        super(ModMenus.LAB.get(), id, inv, pos, ModRegistry.LAB.get(), data);
        this.be = be;
        this.items = h;
        machineSlot(h, LabBE.PLANT, 14, 64, false, () -> false);
        machineSlot(h, LabBE.CHEM, 14, 100, false, () -> false);
        machineSlot(h, LabBE.OUT, 218, 82, true, () -> false);
        playerInventory(inv, 146);
    }

    @Override
    public boolean clickMenuButton(Player p, int id) {
        if (id == BTN_START && be != null) be.start(p);
        return true;
    }
}
