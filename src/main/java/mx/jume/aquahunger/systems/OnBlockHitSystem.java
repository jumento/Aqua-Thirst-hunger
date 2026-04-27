package mx.jume.aquahunger.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.event.events.ecs.DamageBlockEvent;
import com.hypixel.hytale.server.core.modules.entity.component.Invulnerable;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import mx.jume.aquahunger.components.HungerComponent;
import mx.jume.aquahunger.components.ThirstComponent;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

public class OnBlockHitSystem extends EntityEventSystem<EntityStore, DamageBlockEvent> {
    public OnBlockHitSystem() {
        super(DamageBlockEvent.class);
    }

    @Override
    public void handle(
            int i,
            @NonNullDecl ArchetypeChunk<EntityStore> archetypeChunk,
            @NonNullDecl Store<EntityStore> store,
            @NonNullDecl CommandBuffer<EntityStore> commandBuffer,
            @NonNullDecl DamageBlockEvent event) {
        HungerComponent hunger = archetypeChunk.getComponent(i, HungerComponent.getComponentType());
        if (hunger != null)
            hunger.incrementBlockHits();

        ThirstComponent thirst = archetypeChunk.getComponent(i, ThirstComponent.getComponentType());
        if (thirst != null)
            thirst.incrementBlockHits();
    }

    @NullableDecl
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(
                Query.or(HungerComponent.getComponentType(), ThirstComponent.getComponentType()),
                Query.not(DeathComponent.getComponentType()),
                Query.not(Invulnerable.getComponentType()));
    }
}
