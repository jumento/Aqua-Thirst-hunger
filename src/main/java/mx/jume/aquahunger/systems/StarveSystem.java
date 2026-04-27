package mx.jume.aquahunger.systems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.entity.effect.ActiveEntityEffect;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.modules.entity.component.Invulnerable;
import com.hypixel.hytale.server.core.modules.entity.damage.*;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import mx.jume.aquahunger.config.HHMHungerConfig;
import mx.jume.aquahunger.HHMUtils;
import mx.jume.aquahunger.AquaThirstHunger;
import mx.jume.aquahunger.ui.HHMHud;
import mx.jume.aquahunger.components.HungerComponent;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class StarveSystem extends EntityTickingSystem<EntityStore> {

    private StarveSystem() {
    }

    public static StarveSystem create() {
        return new StarveSystem();
    }

    private HHMHungerConfig getConfig() {
        return AquaThirstHunger.get().getHungerConfig();
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
                HungerComponent.getComponentType(),
                EntityStatMap.getComponentType(),
                PlayerRef.getComponentType(),
                TransformComponent.getComponentType(),
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
        HungerComponent hunger = archetypeChunk.getComponent(index, HungerComponent.getComponentType());
        EntityStatMap entityStatMap = archetypeChunk.getComponent(index, EntityStatMap.getComponentType());
        if (hunger == null || entityStatMap == null)
            return;

        PlayerRef playerRef = archetypeChunk.getComponent(index, PlayerRef.getComponentType());
        if (playerRef == null)
            return;

        HHMHungerConfig config = getConfig();

        if (!config.isEnableHunger()) {
            return;
        }

        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        float hungerLevel = hunger.getHungerLevel();

        // --- Stomach Growl System (Per-frame check for threshold crossing) ---
        hunger.addGrowlCooldown(dt);
        float[] thresholds = { 30f, 25f, 22f, 18f, 15f, 10f, 5f };
        float lastLevel = hunger.getLastHungerLevel();

        if (hungerLevel > lastLevel) {
            // If hunger increases (eating), update lastLevel to track new descent
            hunger.setLastHungerLevel(hungerLevel);
        } else if (hunger.getGrowlCooldownTimer() >= 3.0f) {
            boolean soundTriggered = false;
            for (float t : thresholds) {
                if (hungerLevel <= t && lastLevel > t) {
                    int soundIdx = SoundEvent.getAssetMap().getIndex("SFX_Stomach_Growl");
                    if (soundIdx >= 0) {
                        TransformComponent transform = archetypeChunk.getComponent(index, TransformComponent.getComponentType());
                        if (transform != null) {
                            Vector3d pos = transform.getPosition();
                            SoundUtil.playSoundEvent3d(ref, soundIdx, pos.getX(), pos.getY(), pos.getZ(), false, store);
                            soundTriggered = true;
                        }
                    }
                    break;
                }
            }
            if (soundTriggered) {
                hunger.setGrowlCooldownTimer(0.0f);
                hunger.setLastHungerLevel(hungerLevel); // Update checkpoint only on trigger
            }
            // Note: If crossing was detected but no trigger (cooldown), 
            // we DON'T update lastHungerLevel, preserving the crossing for later.
        }

        hunger.setStaminaSeen(getStaminaValue(entityStatMap));
        hunger.addElapsedTime(dt);
        if (hunger.getElapsedTime() < config.getStarvationTickRate())
            return;
        hunger.resetElapsedTime();

        float lowestStaminaSeen = hunger.getAndResetLowestStaminaSeen();
        float staminaModifier = ((10.0f - lowestStaminaSeen) / 10.0f) * config.getStarvationStaminaModifier();
        staminaModifier *= mx.jume.aquahunger.compat.IntegrationManager.getStaminaLossMultiplier(ref, store);

        float damagedBlocksModifier = hunger.getAndResetBlockHits() * config.getStarvationPerBlockHit();
        damagedBlocksModifier *= mx.jume.aquahunger.compat.IntegrationManager.getWorkLossMultiplier(ref, store);

        float baseTick = config.getStarvationPerTick() * mx.jume.aquahunger.compat.IntegrationManager.getMetabolismLossMultiplier(ref, store);
        float depletion = baseTick + staminaModifier + damagedBlocksModifier;

        Velocity velocity = archetypeChunk.getComponent(index, Velocity.getComponentType());
        if (velocity != null && velocity.getSpeed() < 0.1) {
            depletion *= 0.1f;
        }

        hunger.starve(depletion);

        hungerLevel = hunger.getHungerLevel();

        EffectControllerComponent effectController = commandBuffer.getComponent(ref,
                EffectControllerComponent.getComponentType());

        // Apply hungry effect when hunger level is below threshold
        if (hungerLevel != 0 && hungerLevel < config.getHungryThreshold()) {
            if (effectController != null) {
                EntityEffect hungryEffect = HHMUtils.getHungryEntityEffect();
                effectController.addEffect(ref, hungryEffect, commandBuffer);
            }
        }

        // Apply starvation effect and damage when hunger reaches 0
        float starveResilienceThreshold = config.getStarveResilienceThreshold();
        if (hungerLevel <= starveResilienceThreshold) {
            // XP Trigger: Resilience
            if (!hunger.hasAwardedResilienceXP()) {
                mx.jume.aquahunger.compat.IntegrationManager.awardXP(ref, store, "NUTRITION", 50.0f, "RESILIENCE");
                hunger.incrementTimesEmptied();
                mx.jume.aquahunger.compat.SuperlativeManager.checkHungry(playerRef, hunger.getTimesEmptiedTotal());
                hunger.setAwardedResilienceXP(true);
            }
        } else {
            // Reset resilience XP flag
            if (hungerLevel > starveResilienceThreshold + 5.0f && hunger.hasAwardedResilienceXP()) {
                hunger.setAwardedResilienceXP(false);
            }
        }

        if (effectController != null && hungerLevel == 0) {
            // remove all buffs when starving
            HHMUtils.removeActiveEffects(ref, commandBuffer, effectController,
                    StarveSystem::shouldRemoveEffectOnStarvation);
            // apply starving effect
            EntityEffect starvingEffect = HHMUtils.getStarvingEntityEffect();
            effectController.addEffect(ref, starvingEffect, commandBuffer);
        }

        // --- MMO Glutton Trigger (Based on base 100 hunger, ignoring saturation) ---
        float effectiveHunger = Math.min(hungerLevel, 100.0f);
        float hungerPercent = (effectiveHunger / 100.0f) * 100f;

        if (hungerPercent <= 50f && !hunger.isGluttonTriggered()) {
            //AquaThirstHunger.logInfo(String.format("[mmo] Glutton Triggered for %s (%.1f/100.0 - %.1f%%)",
            //        playerRef.getUsername(), effectiveHunger, hungerPercent));
            mx.jume.aquahunger.compat.IntegrationManager.onEatingAction(ref, store, commandBuffer, playerRef);
            hunger.setGluttonTriggered(true);
        }

        if (hungerPercent >= 80f && hunger.isGluttonTriggered()) {
            //AquaThirstHunger.logInfo(String.format("[mmo] Glutton Reset for %s (%.1f/100.0 - %.1f%%)",
            //        playerRef.getUsername(), effectiveHunger, hungerPercent));
            hunger.setGluttonTriggered(false);
        }

        // Starvation Damage logic
        if (hungerLevel <= 0.01f && config.getStarvationDamage() > 0) {
            Damage damage = new Damage(Damage.NULL_SOURCE, HHMUtils.getStarvationDamageCause(),
                    config.getStarvationDamage());
            DamageSystems.executeDamage(ref, commandBuffer, damage);
        }

        HHMHud.updatePlayerHungerLevel(playerRef, hungerLevel);
    }

    public static boolean shouldRemoveEffectOnStarvation(ActiveEntityEffect effect) {
        if (HHMUtils.activeEntityEffectIsHungry(effect))
            return true;
        if (effect.isInfinite())
            return false;
        return !effect.isDebuff();
    }

    public static float getStaminaValue(@NonNullDecl EntityStatMap entityStatMap) {
        final int staminaRef = DefaultEntityStatTypes.getStamina();
        final EntityStatValue statValue = entityStatMap.get(staminaRef);
        if (statValue == null)
            return 10.0f; // Default stamina (max) value if not found
        return statValue.get();
    }
}
