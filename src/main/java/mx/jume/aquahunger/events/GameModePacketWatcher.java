package mx.jume.aquahunger.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.player.SetGameMode;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketWatcher;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import mx.jume.aquahunger.components.HungerComponent;
import mx.jume.aquahunger.AquaThirstHunger;
import mx.jume.aquahunger.ui.HHMHud;
import mx.jume.aquahunger.ui.HHMThirstHud;
import mx.jume.aquahud.AquaHudBridge;

public class GameModePacketWatcher implements PlayerPacketWatcher {
    @Override
    public void accept(PlayerRef playerRef, Packet packet) {
        if (!(packet instanceof SetGameMode setGameMode))
            return;

        // SYNCHRONOUS in accept() — suppress ECS ticks immediately
        AquaHudBridge.invalidateDom(playerRef);

        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null)
            return;
        Store<EntityStore> store = ref.getStore();
        World world = store.getExternalData().getWorld();

        world.execute(() -> {
            GameMode gameMode = setGameMode.gameMode;

            // 1. Update internal HUD state
            HHMHud hungerHud = HHMHud.getHud(playerRef);
            if (hungerHud != null) {
                hungerHud.setGameMode(gameMode);
                // TRANSPARENT: forzar show() para que build() regenere con el gameMode actual.
                AquaHudBridge.forceTransparentRebuild(playerRef, HHMHud.hudIdentifier, hungerHud);
            }

            HHMThirstHud thirstHud = HHMThirstHud.getHud(playerRef);
            if (thirstHud != null) {
                thirstHud.setGameMode(gameMode);
                // TRANSPARENT: forzar show() para que build() regenere con el gameMode actual.
                AquaHudBridge.forceTransparentRebuild(playerRef, HHMThirstHud.hudIdentifier, thirstHud);
            }

            // 2. Rebuild — covers unpredictable motor timing (COORDINATED/DELEGATED)
            AquaHudBridge.rebuildAllDeferred(playerRef, world);

            // 3. Hunger/thirst restoration in creative
            HungerComponent hunger = store.getComponent(ref, HungerComponent.getComponentType());
            mx.jume.aquahunger.components.ThirstComponent thirst = store.getComponent(ref,
                    mx.jume.aquahunger.components.ThirstComponent.getComponentType());

            float initialHunger = AquaThirstHunger.get().getHungerConfig().getInitialHungerLevel();
            float initialThirst = AquaThirstHunger.get().getThirstConfig().getMaxThirst();

            if (gameMode == GameMode.Creative) {
                // CORRECTO: componente ECS directo, SIN pasar por HUD update.
                // HHMUtils.setPlayerHungerLevel/ThirstLevel llaman AquaHudBridge.update()
                // que será suprimido y causará auto-rebuilds que pierden la skin.
                // Ver advertencia #10 en SPEC_AQUAHUD.md.
                if (hunger != null && hunger.getHungerLevel() < initialHunger) {
                    hunger.setHungerLevel(initialHunger);
                }
                if (thirst != null && thirst.getThirstLevel() < initialThirst) {
                    thirst.setThirstLevel(initialThirst);
                }
                // El próximo tick de StarveSystem/ThirstSystem actualizará el HUD
            }
        });
    }
}
