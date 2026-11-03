package fr.minenorth.drogue.menu;

import fr.minenorth.drogue.MiniGame;
import fr.minenorth.drogue.ModRegistry;
import fr.minenorth.drogue.PressBE;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraftforge.items.ItemStackHandler;

/** Presse à pilules : MDMA, mini-jeu, pilules d'ecstasy. */
public class PressMenu extends GameMenu {
    private final PressBE be;
    public final ItemStackHandler items;

    public PressMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, buf.readBlockPos(), PressBE.newHandler(() -> {}), new SimpleContainerData(MiniGame.DATA), null);
    }

    public PressMenu(int id, Inventory inv, PressBE be) {
        this(id, inv, be.getBlockPos(), be.items(), gameData(inv, be.getBlockPos()), be);
    }

    private PressMenu(int id, Inventory inv, BlockPos pos, ItemStackHandler h, ContainerData data, PressBE be) {
        super(ModMenus.PRESS.get(), id, inv, pos, ModRegistry.PILL_PRESS.get(), data);
        this.be = be;
        this.items = h;
        machineSlot(h, PressBE.MDMA, 14, 80, false, this::locked);
        machineSlot(h, PressBE.OUT, 216, 77, true, this::locked);
        playerInventory(inv, 142);
    }

    @Override
    protected void start(ServerPlayer p) { if (be != null) be.startGame(p); }
}
