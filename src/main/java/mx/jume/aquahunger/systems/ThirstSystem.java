package mx.jume.aquahunger.systems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.modules.entity.component.Invulnerable;
import com.hypixel.hytale.server.core.modules.entity.damage.*;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import mx.jume.aquahunger.config.HHMThirstConfig;
import mx.jume.aquahunger.HHMUtils;
import mx.jume.aquahunger.AquaThirstHunger;
import mx.jume.aquahunger.ui.HHMThirstHud;
import mx.jume.aquahunger.components.ThirstComponent;
import mx.jume.aquahunger.components.HungerComponent;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ThirstSystem extends EntityTickingSystem<EntityStore> {
    @Nonnull
    private final HHMThirstConfig config;

    private ThirstSystem(@NonNullDecl HHMThirstConfig config) {
        this.config = config;
    }

    public static ThirstSystem create() {
        HHMThirstConfig config = AquaThirstHunger.get().getThirstConfig();
        return new ThirstSystem(config);
    }

    @Nullable
    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getGatherDamageGroup();
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(
                ThirstComponent.getComponentType(),
                // HungerComponent is needed if we want to share block hit counts?
                // Actually OnBlockHitSystem should manipulate ThirstComponent directly if
                // possible,
                // or we add tracking fields to ThirstComponent similar to HungerComponent.
                // For now, let's assume we read from ThirstComponent's own tracking.
                EntityStatMap.getComponentType(),
                PlayerRef.getComponentType(),
                Query.not(DeathComponent.getComponentType()),
                Query.not(Invulnerable.getComponentType()));
    }

    @Override
    public void tick(
            float dt,
            int index,
            @NonNullDecl ArchetypeChunk<EntityStore> archetypeChunk,
            @NonNullDecl Store<EntityStore> store,
            @NonNullDecl CommandBuffer<EntityStore> commandBuffer) {
        ThirstComponent thirst = archetypeChunk.getComponent(index, ThirstComponent.getComponentType());
        EntityStatMap entityStatMap = archetypeChunk.getComponent(index, EntityStatMap.getComponentType());
        if (thirst == null || entityStatMap == null)
            return;

        thirst.addElapsedTime(dt);
        if (thirst.getElapsedTime() < this.config.getDepletionTickRate())
            return;
        thirst.resetElapsedTime();

        float depletion = this.config.getDepletionPerTick();

        // Stamina logic (simplified, assume always enabled for now or check config)
        // If we want config toggle: if (config.isStaminaEnabled()) ...
        // For now user said "mirror triggers... BUT make each trigger togglable".
        // I need to add block hits tracking to ThirstComponent first!
        // For this first pass, I'll stick to base depletion to get it compiling,
        // as ThirstComponent doesn't have block hit counters yet.
        // Wait, I designed ThirstComponent but missed the transient counters block
        // hits/stamina.
        // I should add those fields to ThirstComponent to fully mirror HungerComponent.

        // Let's assume purely time-based for this exact file write,
        // and I will update ThirstComponent in a separate step if needed.
        // Or I can add the math here if I have access to external triggers, but
        // typically components hold the state.

        thirst.dehydrate(depletion);

        float thirstLevel = thirst.getThirstLevel();
        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);

        if (thirstLevel == 0) {
            // Dehydration damage
            if (config.getDehydrationDamage() > 0) {
                // Reusing starvation damage cause for now as I can't create assets easily
                Damage damage = new Damage(Damage.NULL_SOURCE, HHMUtils.getStarvationDamageCause(),
                        this.config.getDehydrationDamage());
                DamageSystems.executeDamage(ref, commandBuffer, damage);
            }
        }

        PlayerRef playerRef = archetypeChunk.getComponent(index, PlayerRef.getComponentType());
        if (playerRef == null)
            return;

        // Update HUD
        HHMThirstHud.updatePlayerThirstLevel(playerRef, thirstLevel);
    }
}
