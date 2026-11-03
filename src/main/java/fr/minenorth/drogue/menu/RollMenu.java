package fr.minenorth.drogue.menu;

import fr.minenorth.drogue.MiniGame;
import fr.minenorth.drogue.ModRegistry;
import fr.minenorth.drogue.RollBE;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraftforge.items.ItemStackHandler;

/** Table à rouler : feuilles de cannabis, papier, mini-jeu, joints. */
public class RollMenu extends GameMenu {
    private final RollBE be;
    public final ItemStackHandler items;

    public RollMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, buf.readBlockPos(), RollBE.newHandler(() -> {}), new SimpleContainerData(MiniGame.DATA), null);
    }

    public RollMenu(int id, Inventory inv, RollBE be) {
        this(id, inv, be.getBlockPos(), be.items(), gameData(inv, be.getBlockPos()), be);
    }

    private RollMenu(int id, Inventory inv, BlockPos pos, ItemStackHandler h, ContainerData data, RollBE be) {
        super(ModMenus.ROLL.get(), id, inv, pos, ModRegistry.ROLL.get(), data);
        this.be = be;
        this.items = h;
        machineSlot(h, RollBE.LEAVES, 14, 62, false, this::locked);
        machineSlot(h, RollBE.PAPER, 14, 92, false, this::locked);
        machineSlot(h, RollBE.OUT, 216, 77, true, this::locked);
        playerInventory(inv, 142);
    }

    @Override
    protected void start(ServerPlayer p) { if (be != null) be.startGame(p); }
}
