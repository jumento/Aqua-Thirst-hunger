package mx.jume.aquahunger.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import mx.jume.aquahunger.AquaThirstHunger;
import mx.jume.aquahunger.components.HungerComponent;
import mx.jume.aquahunger.config.HHMHungerConfig;
import mx.jume.aquahunger.ui.HHMHud;

import javax.annotation.Nonnull;

public class HungerLifeSystem extends EntityTickingSystem<EntityStore> {
    // The following properties were hardcoded before but are now retrieved from
    // config on tick

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(
                HungerComponent.getComponentType(),
                mx.jume.aquahunger.components.ThirstComponent.getComponentType(),
                EntityStatMap.getComponentType(),
                PlayerRef.getComponentType(),
                Query.not(DeathComponent.getComponentType()));
    }

    @Override
    public void tick(
            float dt,
            int index,
            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer) {

        PlayerRef playerRef = archetypeChunk.getComponent(index, PlayerRef.getComponentType());

        HHMHungerConfig config = AquaThirstHunger.get().getHungerConfig();
        if (!config.isLifePerHunger() || !config.isEnableHunger()) {
            return;
        }

        HungerComponent hunger = archetypeChunk.getComponent(index, HungerComponent.getComponentType());
        mx.jume.aquahunger.components.ThirstComponent thirst = archetypeChunk.getComponent(index,
                mx.jume.aquahunger.components.ThirstComponent.getComponentType());
        EntityStatMap entityStatMap = archetypeChunk.getComponent(index, EntityStatMap.getComponentType());

        if (hunger == null || thirst == null || entityStatMap == null)
            return;

        hunger.setHealTimer(hunger.getHealTimer() + dt);

        if (hunger.getHealTimer() >= config.getPulseInterval()) {
            hunger.setHealTimer(0.0f);

            // Check resources (Hunger + Saturation must be >= 4.0, Thirst >= 1.0)
            if (hunger.getHungerLevel() < config.getHungerCost() || thirst.getThirstLevel() < config.getThirstCost()) {
                return;
            }

            // Check health
            int healthRef = DefaultEntityStatTypes.getHealth();
            EntityStatValue healthVal = entityStatMap.get(healthRef);

            if (healthVal != null) {
                float currentHealth = healthVal.get();
                float maxHealth = healthVal.getMax();

                if (currentHealth < maxHealth) {
                    // Logic: Increase health utilizing EntityStatMap public API
                    // Using setStatValue as requested option (b)
                    float newHealth = Math.min(currentHealth + config.getHealthPerPulse(), maxHealth);
                    entityStatMap.setStatValue(healthRef, newHealth);

                    // Drain hunger (effectively drains saturation first as it's the top of the bar)
                    float newHunger = hunger.getHungerLevel() - config.getHungerCost();
                    hunger.setHungerLevel(newHunger);

                    // Drain thirst
                    thirst.dehydrate(config.getThirstCost());

                    // Update HUD immediately
                    if (playerRef != null) {
                        HHMHud.updatePlayerHungerLevel(playerRef, newHunger);
                        mx.jume.aquahunger.ui.HHMThirstHud.updatePlayerThirstLevel(playerRef, thirst.getThirstLevel());
                    }
                }
            }
        }
    }
}
