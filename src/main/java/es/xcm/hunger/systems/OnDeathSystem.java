package es.xcm.hunger.systems;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import es.xcm.hunger.components.HungerComponent;
import es.xcm.hunger.ui.HHMHud;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

public class OnDeathSystem extends DeathSystems.OnDeathSystem {
    @NullableDecl
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(
            HungerComponent.getComponentType(),
            PlayerRef.getComponentType()
        );
    }

    @Override
    public void onComponentAdded(
        @NonNullDecl Ref<EntityStore> ref,
        @NonNullDecl DeathComponent deathComponent,
        @NonNullDecl Store<EntityStore> store,
        @NonNullDecl CommandBuffer<EntityStore> commandBuffer
    ) {
        // No action needed on death, however method must be overridden
    }

    @Override
    public void onComponentRemoved(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl DeathComponent component, @NonNullDecl Store<EntityStore> store, @NonNullDecl CommandBuffer<EntityStore> commandBuffer) {
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        HungerComponent hungerComponent = store.getComponent(ref, HungerComponent.getComponentType());
        if (playerRef == null || hungerComponent == null) return;
        hungerComponent.setHungerLevel(100.0f);
        HHMHud.updatePlayerHud(playerRef, 100.0f);
    }
}
