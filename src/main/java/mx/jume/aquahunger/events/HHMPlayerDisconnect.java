package mx.jume.aquahunger.events;

import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import mx.jume.aquahunger.ui.HHMHud;
import mx.jume.aquahunger.ui.HHMThirstHud;

/**
 * Cleanup handler for player disconnect.
 * Invariante #14: CADA mod DEBE registrar PlayerDisconnectEvent
 * y limpiar su hudMap + llamar AquaHudBridge.onPlayerDisconnect().
 */
public class HHMPlayerDisconnect {
    public static void handle(PlayerDisconnectEvent event) {
        PlayerRef playerRef = event.getPlayerRef();
        if (playerRef == null) return;

        HHMHud.removeHud(playerRef);
        HHMThirstHud.removeHud(playerRef);
        mx.jume.aquahud.AquaHudBridge.onPlayerDisconnect(playerRef);
    }
}
