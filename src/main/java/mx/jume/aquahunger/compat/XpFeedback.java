package mx.jume.aquahunger.compat;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import mx.jume.aquahunger.components.HungerComponent;
import mx.jume.aquahunger.components.ThirstComponent;
import mx.jume.aquahunger.ui.HudService;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Shows the +XP / -XP marks on the bars for a few seconds after an XP award or a level-up.
 * Every step re-reads the player's current components on the world the player is in when it
 * runs, so it stays correct after a respawn or a world change in between.
 */
final class XpFeedback {
    private static final long DISPLAY_SECONDS = 3;
    private static final Map<UUID, Long> generation = new ConcurrentHashMap<>();

    private XpFeedback() {
    }

    /** Sends the flags already set on the components and clears them after a few seconds. */
    static void show(PlayerRef playerRef) {
        long token = generation.merge(playerRef.getUuid(), 1L, Long::sum);
        HudService.runOnPlayerWorld(playerRef, () -> HudService.refreshValues(playerRef));
        CompletableFuture.delayedExecutor(DISPLAY_SECONDS, TimeUnit.SECONDS).execute(() ->
                HudService.runOnPlayerWorld(playerRef, () -> {
                    // A newer award restarted the timer: let that one clear the marks.
                    if (generation.remove(playerRef.getUuid(), token)) {
                        clear(playerRef);
                    }
                }));
    }

    /** Level-up feedback: +XP on the hunger bar. World thread only. */
    static void showLevelUp(PlayerRef playerRef) {
        HungerComponent hunger = playerRef.getComponent(HungerComponent.getComponentType());
        if (hunger != null) {
            hunger.setXpUp(true);
        }
        show(playerRef);
    }

    private static void clear(PlayerRef playerRef) {
        HungerComponent hunger = playerRef.getComponent(HungerComponent.getComponentType());
        if (hunger != null) {
            hunger.setXpUp(false);
            hunger.setXpDown(false);
        }
        ThirstComponent thirst = playerRef.getComponent(ThirstComponent.getComponentType());
        if (thirst != null) {
            thirst.setXpUp(false);
            thirst.setXpDown(false);
        }
        HudService.refreshValues(playerRef);
    }
}
