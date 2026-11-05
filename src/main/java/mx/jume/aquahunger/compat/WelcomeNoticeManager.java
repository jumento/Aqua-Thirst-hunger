package mx.jume.aquahunger.compat;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import mx.jume.aquahunger.AquaThirstHunger;
import mx.jume.aquahunger.ui.HHMHud;
import mx.jume.aquahunger.ui.HudService;
import mx.jume.aquahunger.ui.WelcomeNoticeHud;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;


public class WelcomeNoticeManager {

    private static boolean isAquaSanityPresent() {
        return AquaThirstHunger.get().isAquaSanityPresent();
    }

    public static void showNoticeDelayed(PlayerRef playerRef) {
        // Paso 1: Si esta AquaSanity, se desactiva el mensaje de bienvenida sin importar otros mods
        if (isAquaSanityPresent()) {
            return;
        }

        // Paso 2.5: Al conectarse, se lee si el jugador esta en la lista de UUIDs para evaluar si muestra el mensaje
        if (NotifiedPlayersManager.isNotified(playerRef.getUuid().toString())) {
            return;
        }

        // Paso 3: Esta accion se dispara 3 segundos despues de que el jugador se conecta.
        // Se resuelve el mundo al ejecutarse, por si el jugador cambio de mundo durante la espera.
        CompletableFuture.delayedExecutor(3, TimeUnit.SECONDS)
            .execute(() -> HudService.runOnPlayerWorld(playerRef, () -> showNotice(playerRef)));
    }

    public static void showNotice(PlayerRef playerRef) {
        // Doble check rapido de estado (por si se notifico en la espera)
        if (NotifiedPlayersManager.isNotified(playerRef.getUuid().toString())) return;

        try {
            // Paso 2: Como ya sabemos que no esta AquaSanity, valoramos si esta RPG, Endless o MMO
            boolean rpgPresent = AquaThirstHunger.get().isRpgLevelingPresent();
            boolean endlessPresent = AquaThirstHunger.get().isEndlessLevelingPresent();
            boolean mmoPresent = AquaThirstHunger.get().isHytaleMMOPresent();

            if (!rpgPresent && !endlessPresent && !mmoPresent) {
                return;
            }

            // Dependiendo de eso, elige el mensaje a mostrar
            String prefix;
            if ((rpgPresent || endlessPresent) && mmoPresent) {
                prefix = "integration.welcome.unified.thirst.";
            } else if (mmoPresent) {
                prefix = "integration.welcome.mmo.thirst.";
            } else if (endlessPresent) {
                prefix = "integration.welcome.endless.thirst.";
            } else {
                prefix = "integration.welcome.rpg.thirst.";
            }

            Ref<EntityStore> ref = playerRef.getReference();
            if (ref == null || !ref.isValid()) return;
            Player player = ref.getStore().getComponent(ref, Player.getComponentType());
            if (player == null) return;

            String lang = playerRef.getLanguage();
            String[] lines = new String[5];
            for (int i = 0; i < lines.length; i++) {
                lines[i] = LangManager.getForLanguage(lang, prefix + "line" + (i + 1));
            }
            String dismiss = LangManager.getForLanguage(lang, prefix + "dismiss");

            // Paso 3 (cont): Se muestra el mensaje y se queda fijado en pantalla
            HudService.showNotice(player, playerRef, new WelcomeNoticeHud(playerRef, lines, dismiss));
        } catch (Exception e) {
            AquaThirstHunger.logSevere("Critical error showing welcome notice: " + e.getMessage());
        }
    }

    /**
     * @return true if the player is in game; the notice (if visible) is closed and the
     *         player is never notified again.
     */
    public static boolean dismissNotice(Player player, PlayerRef playerRef) {
        if (HHMHud.getHud(playerRef) == null) {
            return false;
        }
        HudService.dismissNotice(player, playerRef);

        // Paso 4: Cierra el cartel y escribe el UUID independientemente en el archivo config
        NotifiedPlayersManager.markNotified(playerRef.getUuid().toString());
        return true;
    }
}
