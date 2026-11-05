package mx.jume.aquahunger.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import mx.jume.aquahunger.compat.WelcomeNoticeManager;
import mx.jume.aquahunger.ui.HudService;

public class HHMPlayerReady {
    public static void handle(PlayerReadyEvent event) {
        Ref<EntityStore> ref = event.getPlayerRef();
        if (ref == null) return;
        Store<EntityStore> store = ref.getStore();
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) return;

        Player player = event.getPlayer();
        if (player == null) return;

        World world = store.getExternalData().getWorld();

        // Fires on login and after every world change; the engine drops all custom HUDs on a
        // world change, so the bars are attached again each time.
        world.execute(() -> {
            if (!ref.isValid()) return;
            HudService.attach(store, ref, playerRef, player);
            WelcomeNoticeManager.showNoticeDelayed(playerRef);
        });
    }
}
