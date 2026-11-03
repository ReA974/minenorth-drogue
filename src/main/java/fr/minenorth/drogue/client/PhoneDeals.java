package fr.minenorth.drogue.client;

import com.mikasrevs.newphone.client.zphone.PhoneNavigationState;
import fr.minenorth.drogue.DrogueNet;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.ModList;

/**
 * Application « Deal » du téléphone MRPhone, appelée par réflexion par le téléphone patché (fr.minenorth.phonepatch.JobsHook) :
 * visible() pour afficher l'icône, openFromPhone() au clic. Seul point d'entrée vers {@link PhoneDealsScreen} (qui hérite d'une classe du
 * téléphone) : sans le téléphone, rien n'est chargé. Côté client uniquement.
 */
public final class PhoneDeals {
    private PhoneDeals() {}

    private static boolean present() { return ModList.get().isLoaded("newphone"); }

    private static volatile boolean visible;

    /** Icône visible seulement une fois qu'on a eu de la drogue sur soi (état envoyé par le serveur, voir DealerFlag). */
    public static boolean visible() { return visible; }

    public static void setVisible(boolean v) { visible = v; }

    /** Clic sur l'icône : ouvre l'application dans l'écran du téléphone puis demande les messages au serveur. */
    public static void openFromPhone() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !present()) return;
        PhoneNavigationState.open(mc, mc.screen, new PhoneDealsScreen());
        DrogueNet.CHANNEL.sendToServer(new DrogueNet.PhoneAction(3, 0));
    }

    /** Réponse du serveur : mise à jour de l'application si elle est affichée. */
    public static void receive(DrogueNet.PhoneData data) {
        if (present() && Minecraft.getInstance().screen instanceof PhoneDealsScreen s) s.update(data);
    }
}
