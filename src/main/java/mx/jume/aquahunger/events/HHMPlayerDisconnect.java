package mx.jume.aquahunger.events;

import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import mx.jume.aquahunger.ui.HudService;

/**
 * Drops the per-player HUD state when a player leaves.
 */
public class HHMPlayerDisconnect {
    public static void handle(PlayerDisconnectEvent event) {
        PlayerRef playerRef = event.getPlayerRef();
        if (playerRef == null) return;

        HudService.detach(playerRef);
    }
}
