package mx.jume.aquahunger.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.event.events.ecs.GameModeTypeEnterEvent;
import com.hypixel.hytale.server.core.event.events.ecs.GameModeTypeExitEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import mx.jume.aquahunger.ui.HudService;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

/**
 * Hides the hunger/thirst bars while a player is in a spectator game mode type
 * (Spectator, SpectatorInteractive, HardcoreSpectator) and shows them again afterwards.
 */
public final class GameModeTypeHudSystems {
    private GameModeTypeHudSystems() {
    }

    public static class Enter extends EntityEventSystem<EntityStore, GameModeTypeEnterEvent> {
        public Enter() {
            super(GameModeTypeEnterEvent.class);
        }

        @Override
        public void handle(int i, @NonNullDecl ArchetypeChunk<EntityStore> archetypeChunk,
                @NonNullDecl Store<EntityStore> store, @NonNullDecl CommandBuffer<EntityStore> commandBuffer,
                @NonNullDecl GameModeTypeEnterEvent event) {
            scheduleRefresh(archetypeChunk.getComponent(i, PlayerRef.getComponentType()), store);
        }

        @NullableDecl
        @Override
        public Query<EntityStore> getQuery() {
            return PlayerRef.getComponentType();
        }
    }

    public static class Exit extends EntityEventSystem<EntityStore, GameModeTypeExitEvent> {
        public Exit() {
            super(GameModeTypeExitEvent.class);
        }

        @Override
        public void handle(int i, @NonNullDecl ArchetypeChunk<EntityStore> archetypeChunk,
                @NonNullDecl Store<EntityStore> store, @NonNullDecl CommandBuffer<EntityStore> commandBuffer,
                @NonNullDecl GameModeTypeExitEvent event) {
            scheduleRefresh(archetypeChunk.getComponent(i, PlayerRef.getComponentType()), store);
        }

        @NullableDecl
        @Override
        public Query<EntityStore> getQuery() {
            return PlayerRef.getComponentType();
        }
    }

    // The events fire before the change is applied (and can be cancelled), so read the
    // resulting game mode type once the current task is done.
    private static void scheduleRefresh(PlayerRef playerRef, Store<EntityStore> store) {
        if (playerRef == null) {
            return;
        }
        store.getExternalData().getWorld().execute(() -> HudService.refreshSpectating(playerRef));
    }
}
