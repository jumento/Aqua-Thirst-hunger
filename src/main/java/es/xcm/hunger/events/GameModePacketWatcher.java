package es.xcm.hunger.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.server.core.auth.PlayerAuthentication;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.io.adapter.PacketWatcher;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import es.xcm.hunger.HHMUtils;
import es.xcm.hunger.components.HungerComponent;
import es.xcm.hunger.ui.HHMHud;

import java.util.UUID;

public class GameModePacketWatcher implements PacketWatcher {
    static private final int SET_GAMEMODE_PACKET_ID = 101;

    @Override
    public void accept(PacketHandler packetHandler, Packet packet) {
        if (packet.getId() != SET_GAMEMODE_PACKET_ID) return;

        PlayerAuthentication playerAuthentication = packetHandler.getAuth();
        if (playerAuthentication == null) return;

        // Don't trust the gameMode packet data directly, fetch it from the player entity later on instead
        // SetGameMode setGameMode = (SetGameMode) packet;

        UUID userUuid = playerAuthentication.getUuid();
        PlayerRef playerRef = Universe.get().getPlayer(userUuid);
        if (playerRef == null) return;

        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null) return;

        Store<EntityStore> store = ref.getStore();
        World world = store.getExternalData().getWorld();

        world.execute(() -> {
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player == null) return;
            GameMode gameMode = player.getGameMode();
            HHMHud.updatePlayerGameMode(playerRef, gameMode);
            if (gameMode == GameMode.Creative) {
                HHMUtils.removeHungerRelatedEffectsFromEntity(ref, store);
                HHMUtils.setPlayerHungerLevel(ref, store, HungerComponent.maxHungerLevel);
            }
        });
    }
}