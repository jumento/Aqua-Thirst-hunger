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
import mx.jume.aquahunger.HHMUtils;
import mx.jume.aquahunger.components.HungerComponent;
import mx.jume.aquahunger.AquaThirstHunger;
import mx.jume.aquahunger.ui.HHMHud;

public class GameModePacketWatcher implements PlayerPacketWatcher {
    @Override
    public void accept(PlayerRef playerRef, Packet packet) {
        if (!(packet instanceof SetGameMode setGameMode))
            return;

        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null)
            return;
        Store<EntityStore> store = ref.getStore();
        World world = store.getExternalData().getWorld();

        world.execute(() -> {
            GameMode gameMode = setGameMode.gameMode;
            HHMHud.updatePlayerGameMode(playerRef, gameMode);
            mx.jume.aquahunger.ui.HHMThirstHud.updatePlayerGameMode(playerRef, gameMode);
            HungerComponent hunger = store.getComponent(ref, HungerComponent.getComponentType());
            mx.jume.aquahunger.components.ThirstComponent thirst = store.getComponent(ref,
                    mx.jume.aquahunger.components.ThirstComponent.getComponentType());

            float initialHunger = AquaThirstHunger.get().getHungerConfig().getInitialHungerLevel();
            float initialThirst = AquaThirstHunger.get().getThirstConfig().getMaxThirst();

            if (gameMode == GameMode.Creative) {
                if (hunger != null && hunger.getHungerLevel() < initialHunger) {
                    HHMUtils.setPlayerHungerLevel(ref, store, initialHunger);
                }
                if (thirst != null && thirst.getThirstLevel() < initialThirst) {
                    HHMUtils.setPlayerThirstLevel(ref, store, initialThirst);
                }
            }
        });
    }
}
