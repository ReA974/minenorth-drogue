package fr.minenorth.drogue.menu;

import fr.minenorth.drogue.MiniGame;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;

/** Table à mini-jeu (ensachage, roulage) : emplacements verrouillés pendant la partie, boutons Lancer / Stop, partie arrêtée à la fermeture. */
public abstract class GameMenu extends StationMenu {
    public static final int BTN_START = 0, BTN_STOP = 1;
    /** Stop avec la position vue à l'écran : BTN_SEEN + position (0 à MiniGame.STEPS ; les numéros de bouton tiennent sur un octet). */
    public static final int BTN_SEEN = 10;

    protected GameMenu(MenuType<?> type, int id, Inventory inv, BlockPos pos, Block block, ContainerData data) {
        super(type, id, inv, pos, block, data);
    }

    /** Données du mini-jeu côté serveur, propres au joueur qui regarde l'écran. */
    protected static ContainerData gameData(Inventory inv, BlockPos pos) {
        Player p = inv.player;
        return new ContainerData() {
            @Override public int get(int i) { return MiniGame.view(p.getUUID(), pos, i); }
            @Override public void set(int i, int v) {}
            @Override public int getCount() { return MiniGame.DATA; }
        };
    }

    public boolean playing() { return data.get(0) == 1; }

    /** Un autre joueur joue sur cette table. */
    public boolean busy() { return data.get(13) == 1; }

    /** Matière verrouillée tant qu'une partie est en cours sur la table. */
    protected boolean locked() { return playing() || busy(); }

    protected abstract void start(ServerPlayer p);

    @Override
    public boolean clickMenuButton(Player p, int id) {
        if (!(p instanceof ServerPlayer sp)) return true;
        if (id == BTN_START && !locked()) start(sp);
        else if (id == BTN_STOP && MiniGame.playing(sp.getUUID(), pos)) MiniGame.stop(sp);
        else if (id >= BTN_SEEN && id <= BTN_SEEN + MiniGame.STEPS && MiniGame.playing(sp.getUUID(), pos)) MiniGame.stop(sp, id - BTN_SEEN);
        return true;
    }

    @Override
    public void removed(Player p) {
        super.removed(p);
        if (p instanceof ServerPlayer sp && MiniGame.playing(sp.getUUID(), pos)) MiniGame.cancel(sp);
    }
}
