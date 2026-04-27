package mx.jume.aquahunger.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import mx.jume.aquahunger.ui.HHMHud;
import mx.jume.aquahunger.compat.WelcomeNoticeManager;
import mx.jume.aquahunger.AquaThirstHunger;

public class HHMPlayerReady {
    public static void handle(PlayerReadyEvent event) {
        Ref<EntityStore> ref = event.getPlayerRef();
        if (ref == null) return;
        Store<EntityStore> store = ref.getStore();
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) return;

        Player player = event.getPlayer();
        if (player == null) return;

        // Synchronous suppression to block early tick updates
        mx.jume.aquahud.AquaHudBridge.invalidateDom(playerRef);

        World world = store.getExternalData().getWorld();

        world.execute(() -> {
            boolean hungerExists = HHMHud.hasHud(playerRef);
            boolean thirstExists = mx.jume.aquahunger.ui.HHMThirstHud.hasHud(playerRef);

            if (hungerExists && thirstExists) {
                // World travel — re-registrar con instancias existentes, NO crear nuevas
                HHMHud existingHunger = HHMHud.getHud(playerRef);
                if (existingHunger != null) {
                    existingHunger.setGameMode(player.getGameMode());
                    mx.jume.aquahud.AquaHudBridge.reRegister(player, playerRef, HHMHud.hudIdentifier, existingHunger);
                }
                mx.jume.aquahunger.ui.HHMThirstHud existingThirst = mx.jume.aquahunger.ui.HHMThirstHud.getHud(playerRef);
                if (existingThirst != null) {
                    existingThirst.setGameMode(player.getGameMode());
                    mx.jume.aquahud.AquaHudBridge.reRegister(player, playerRef, mx.jume.aquahunger.ui.HHMThirstHud.hudIdentifier, existingThirst);
                }
                mx.jume.aquahud.AquaHudBridge.rebuildAllDeferred(playerRef, world);
                WelcomeNoticeManager.showNoticeDelayed(playerRef, world);
                return;
            }

            // EndlessLeveling registra su HUD a los ~300ms — demorar nuestro registro
            boolean delayForEndless = AquaThirstHunger.get().isEndlessLevelingPresent();
            if (delayForEndless) {
                // AquaThirstHunger.logInfo("[aquahunger] EndlessLeveling detected — delaying HUD registration 500ms");
                java.util.concurrent.CompletableFuture.delayedExecutor(500, java.util.concurrent.TimeUnit.MILLISECONDS)
                        .execute(() -> world.execute(() -> {
                            if (HHMHud.hasHud(playerRef)) return;
                            HHMHud.createPlayerHud(store, ref, playerRef, player);
                            mx.jume.aquahunger.ui.HHMThirstHud.createPlayerHud(store, ref, playerRef, player);
                            mx.jume.aquahud.AquaHudBridge.rebuildAllDeferred(playerRef, world);
                            WelcomeNoticeManager.showNoticeDelayed(playerRef, world);
                        }));
                return;
            }

            // Sin EndlessLeveling — registro inmediato
            if (!hungerExists && !thirstExists) {
                HHMHud.createPlayerHud(store, ref, playerRef, player);
                mx.jume.aquahunger.ui.HHMThirstHud.createPlayerHud(store, ref, playerRef, player);
            } else {
                // Estado mixto (inconsistente) — limpiar y re-crear todo como login
                if (hungerExists) HHMHud.removeHud(playerRef);
                if (thirstExists) mx.jume.aquahunger.ui.HHMThirstHud.removeHud(playerRef);
                HHMHud.createPlayerHud(store, ref, playerRef, player);
                mx.jume.aquahunger.ui.HHMThirstHud.createPlayerHud(store, ref, playerRef, player);
            }

            mx.jume.aquahud.AquaHudBridge.rebuildAllDeferred(playerRef, world);
            WelcomeNoticeManager.showNoticeDelayed(playerRef, world);
        });
    }
}
