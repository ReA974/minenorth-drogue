package fr.minenorth.drogue.menu;

import fr.minenorth.drogue.MiniGame;
import fr.minenorth.drogue.ModRegistry;
import fr.minenorth.drogue.PackBE;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraftforge.items.ItemStackHandler;

/** Table d'ensachage : poudre, cristaux, sachets vides, mini-jeu, sachets prêts. */
public class PackMenu extends GameMenu {
    private final PackBE be;
    public final ItemStackHandler items;

    public PackMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, buf.readBlockPos(), PackBE.newHandler(() -> {}), new SimpleContainerData(MiniGame.DATA), null);
    }

    public PackMenu(int id, Inventory inv, PackBE be) {
        this(id, inv, be.getBlockPos(), be.items(), gameData(inv, be.getBlockPos()), be);
    }

    private PackMenu(int id, Inventory inv, BlockPos pos, ItemStackHandler h, ContainerData data, PackBE be) {
        super(ModMenus.PACK.get(), id, inv, pos, ModRegistry.PACK.get(), data);
        this.be = be;
        this.items = h;
        machineSlot(h, PackBE.POWDER, 12, 56, false, this::locked);
        machineSlot(h, PackBE.CRYSTALS, 12, 78, false, this::locked);
        machineSlot(h, PackBE.HEROIN, 12, 100, false, this::locked);
        machineSlot(h, PackBE.BAGS, 12, 122, false, this::locked);
        machineSlot(h, PackBE.OUT, 216, 80, true, this::locked);
        playerInventory(inv, 152);
    }

    @Override
    protected void start(ServerPlayer p) { if (be != null) be.startGame(p); }
}
