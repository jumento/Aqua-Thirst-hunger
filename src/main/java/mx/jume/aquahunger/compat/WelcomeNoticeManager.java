package mx.jume.aquahunger.compat;

import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import mx.jume.aquahunger.AquaThirstHunger;
import mx.jume.aquahunger.ui.HHMHud;
import mx.jume.aquahud.AquaHudBridge;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;


public class WelcomeNoticeManager {

    private static boolean isAquaSanityPresent() {
        return AquaThirstHunger.get().isAquaSanityPresent();
    }

    public static void showNoticeDelayed(PlayerRef playerRef, World world) {
        // Paso 1: Si esta AquaSanity, se desactiva el mensaje de bienvenida sin importar otros mods
        if (isAquaSanityPresent()) {
            //AquaThirstHunger.logInfo("[aquahunger] AquaSanity detected — welcome notice disabled unconditionally.");
            return;
        }

        // Paso 2.5: Al conectarse, se lee si el jugador esta en la lista de UUIDs para evaluar si muestra el mensaje
        if (NotifiedPlayersManager.isNotified(playerRef.getUuid().toString())) {
            //AquaThirstHunger.logInfo("[aquahunger] Notice skipped: " + playerRef.getUsername() + " is already notified.");
            return;
        }

        //AquaThirstHunger.logInfo("[aquahunger] Scheduling welcome notice for " + playerRef.getUsername() + " (3s delay)");
        
        // Paso 3: Esta accion se dispara 3 segundos despues de que el jugador se conecta
        CompletableFuture.delayedExecutor(3, TimeUnit.SECONDS)
            .execute(() -> world.execute(() -> showNotice(playerRef)));
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

            UICommandBuilder cmd = new UICommandBuilder();
            cmd.append("Hungry/HUD/WelcomeNotice.ui");
            
            String lang = playerRef.getLanguage();
            cmd.set("#WelcomeNoticeLine1.Text", LangManager.getForLanguage(lang, prefix + "line1"));
            cmd.set("#WelcomeNoticeLine2.Text", LangManager.getForLanguage(lang, prefix + "line2"));
            cmd.set("#WelcomeNoticeLine3.Text", LangManager.getForLanguage(lang, prefix + "line3"));
            cmd.set("#WelcomeNoticeLine4.Text", LangManager.getForLanguage(lang, prefix + "line4"));
            cmd.set("#WelcomeNoticeLine5.Text", LangManager.getForLanguage(lang, prefix + "line5"));
            cmd.set("#WelcomeNoticeDismiss.Text", LangManager.getForLanguage(lang, prefix + "dismiss"));
            
            // Paso 3 (cont): Se muestra el mensaje y se queda fijado en pantalla
            cmd.set("#WelcomeNoticeRoot.Visible", true);
            
            //AquaThirstHunger.logInfo("[aquahunger] Attempting to display notice UI for " + playerRef.getUsername());

            HHMHud hud = HHMHud.getHud(playerRef);
            if (hud != null) {
                AquaHudBridge.update(playerRef, HHMHud.hudIdentifier, cmd, hud);
                //AquaThirstHunger.logInfo("[aquahunger] Notice UI injected successfully via AquaHudBridge.");
            } else {
                //AquaThirstHunger.logWarning("[aquahunger] FAILED to show notice: No HUD instance found.");
            }
        } catch (Exception e) {
            AquaThirstHunger.logSevere("Critical error showing welcome notice: " + e.getMessage());
        }
    }

    public static boolean dismissNotice(PlayerRef playerRef) {
        UICommandBuilder cmd = new UICommandBuilder();
        cmd.set("#WelcomeNoticeRoot.Visible", false);
        
        boolean success = false;
        HHMHud hud = HHMHud.getHud(playerRef);
        if (hud != null) {
            AquaHudBridge.update(playerRef, HHMHud.hudIdentifier, cmd, hud);
            success = true;
        }
        
        if (success) {
            // Paso 4: Cierra el cartel y escribe el UUID independientemente en el archivo config
            NotifiedPlayersManager.markNotified(playerRef.getUuid().toString());
            //AquaThirstHunger.logInfo("[aquahunger] Welcome notice dismissed for " + playerRef.getUsername());
        }
        
        return success;
    }
}
