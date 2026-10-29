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
import mx.jume.aquahunger.ui.HudService;

/**
 * The SetGameMode packet is only sent once a game mode change has actually been applied
 * (ChangeGameModeEvent fires before it and can be cancelled), so it is the reliable signal
 * to switch the bars between their adventure and creative look.
 */
public class GameModePacketWatcher implements PlayerPacketWatcher {
    @Override
    public void accept(PlayerRef playerRef, Packet packet) {
        if (!(packet instanceof SetGameMode setGameMode))
            return;

        GameMode gameMode = setGameMode.gameMode;
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null)
            return;
        Store<EntityStore> store = ref.getStore();
        World world = store.getExternalData().getWorld();

        world.execute(() -> {
            if (!ref.isValid())
                return;

            HudService.setGameMode(playerRef, gameMode);

            // Hunger/thirst restoration in creative
            if (gameMode == GameMode.Creative) {
                HungerComponent hunger = store.getComponent(ref, HungerComponent.getComponentType());
                mx.jume.aquahunger.components.ThirstComponent thirst = store.getComponent(ref,
                        mx.jume.aquahunger.components.ThirstComponent.getComponentType());

                float initialHunger = AquaThirstHunger.get().getHungerConfig().getInitialHungerLevel();
                float initialThirst = AquaThirstHunger.get().getThirstConfig().getMaxThirst();

                if (hunger != null && hunger.getHungerLevel() < initialHunger) {
                    hunger.setHungerLevel(initialHunger);
                }
                if (thirst != null && thirst.getThirstLevel() < initialThirst) {
                    thirst.setThirstLevel(initialThirst);
                }
                HudService.refreshValues(playerRef);
            }
        });
    }
}
