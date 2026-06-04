package mx.jume.aquahunger.systems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.modules.entity.component.Invulnerable;
import com.hypixel.hytale.server.core.modules.entity.damage.*;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import mx.jume.aquahunger.config.HHMThirstConfig;
import mx.jume.aquahunger.HHMUtils;
import mx.jume.aquahunger.AquaThirstHunger;
import mx.jume.aquahunger.ui.HHMThirstHud;
import mx.jume.aquahunger.components.ThirstComponent;
import mx.jume.aquahunger.components.HungerComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.joml.Vector3d;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ThirstSystem extends EntityTickingSystem<EntityStore> {

    private ThirstSystem() {
    }

    public static ThirstSystem create() {
        return new ThirstSystem();
    }

    private HHMThirstConfig getConfig() {
        return AquaThirstHunger.get().getThirstConfig();
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
        ThirstComponent thirst = archetypeChunk.getComponent(index, ThirstComponent.getComponentType());
        EntityStatMap entityStatMap = archetypeChunk.getComponent(index, EntityStatMap.getComponentType());
        if (thirst == null || entityStatMap == null)
            return;

        PlayerRef playerRef = archetypeChunk.getComponent(index, PlayerRef.getComponentType());
        if (playerRef == null)
            return;

        HHMThirstConfig config = getConfig();

        if (!config.isEnableThirst()) {
            return;
        }

        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        float thirstLevel = thirst.getThirstLevel();

        thirst.addCoughCooldown(dt);
        float[] thresholds = { 30f, 25f, 22f, 18f, 15f, 10f, 5f };
        float lastLevel = thirst.getLastThirstLevel();

        if (thirstLevel > lastLevel) {
            thirst.setLastThirstLevel(thirstLevel);
        } else if (thirst.getCoughCooldownTimer() >= 3.0f) {
            boolean soundTriggered = false;
            for (float t : thresholds) {
                if (thirstLevel <= t && lastLevel > t) {
                    int soundIdx = SoundEvent.getAssetMap().getIndex("SFX_Cough");
                    if (soundIdx >= 0) {
                        TransformComponent transform = archetypeChunk.getComponent(index, TransformComponent.getComponentType());
                        if (transform != null) {
                            Vector3d pos = transform.getPosition();
                            SoundUtil.playSoundEvent3d(ref, soundIdx, pos.x(), pos.y(), pos.z(), false, store);
                            soundTriggered = true;
                        }
                    }
                    break;
                }
            }
            if (soundTriggered) {
                thirst.setLastThirstLevel(thirstLevel);
                thirst.setCoughCooldownTimer(0);
            }
        }

        thirst.setStaminaSeen(StarveSystem.getStaminaValue(entityStatMap));
        thirst.addElapsedTime(dt);
        if (thirst.getElapsedTime() < config.getDepletionTickRate())
            return;
        thirst.resetElapsedTime();

        float lowestStaminaSeen = thirst.getAndResetLowestStaminaSeen();
        float staminaModifier = ((10.0f - lowestStaminaSeen) / 10.0f) * config.getSprintDepletionModifier();
        staminaModifier *= mx.jume.aquahunger.compat.IntegrationManager.getThirstStaminaLossMultiplier(ref, store);

        float damagedBlocksModifier = thirst.getAndResetBlockHits() * config.getDepletionPerBlockHit();
        damagedBlocksModifier *= mx.jume.aquahunger.compat.IntegrationManager.getThirstWorkLossMultiplier(ref, store);

        float baseTick = config.getDepletionPerTick() * mx.jume.aquahunger.compat.IntegrationManager.getThirstMetabolismLossMultiplier(ref, store);
        float depletion = baseTick + staminaModifier + damagedBlocksModifier;

        Velocity velocity = archetypeChunk.getComponent(index, Velocity.getComponentType());
        if (velocity != null && velocity.getSpeed() < 0.1) {
            depletion *= 0.1f;
        }

        thirst.dehydrate(depletion);
        thirstLevel = thirst.getThirstLevel();

        int survivalLevel = mx.jume.aquahunger.compat.IntegrationManager.getSkillLevel(ref, store, "SURVIVAL");
        float newMax = ThirstComponent.DEFAULT_MAX_THIRST + (survivalLevel * 2.5f);
        if (thirst.getMaxThirst() != newMax) {
            thirst.setMaxThirst(newMax);
        }

        EffectControllerComponent effectController = archetypeChunk.getComponent(index,
                EffectControllerComponent.getComponentType());

        float thirstResilienceThreshold = config.getStarveResilienceThreshold();
        if (thirstLevel <= thirstResilienceThreshold) {
            if (!thirst.hasAwardedResilienceXP()) {
                mx.jume.aquahunger.compat.IntegrationManager.awardXP(ref, store, "THIRST", 50.0f, "RESILIENCE_THIRST");
                thirst.incrementTimesEmptied();
                mx.jume.aquahunger.compat.SuperlativeManager.checkDehydrated(playerRef, thirst.getTimesEmptiedTotal());
                thirst.setAwardedResilienceXP(true);
            }
        } else {
            if (thirstLevel > thirstResilienceThreshold + 5.0f && thirst.hasAwardedResilienceXP()) {
                thirst.setAwardedResilienceXP(false);
            }
        }

        if (thirstLevel == 0) {

            if (effectController != null) {
                EntityEffect dehydrationEffect = HHMUtils.getDehydrationEntityEffect();
                if (dehydrationEffect != null) {
                    effectController.addEffect(ref, dehydrationEffect, commandBuffer);
                }
            }

            if (config.getDehydrationDamage() > 0) {
                Damage damage = new Damage(Damage.NULL_SOURCE, HHMUtils.getStarvationDamageCause(),
                        config.getDehydrationDamage());
                DamageSystems.executeDamage(ref, commandBuffer, damage);
            }
        }

        if (thirstLevel > 0) {
            if (effectController != null) {
                HHMUtils.removeActiveEffects(ref, commandBuffer, effectController,
                        HHMUtils::activeEntityEffectIsDehydration);
            }
        }

        if (effectController != null) {
            float thirstPercent = (thirstLevel / config.getMaxThirst()) * 100f;
            if (thirstPercent >= config.getStaminaBoostThreshold() && config.getStaminaBoostThreshold() <= 100f) {
                EntityEffect boostEffect = HHMUtils.getStaminaBoostEntityEffect();
                if (boostEffect != null) {
                    effectController.addEffect(ref, boostEffect, commandBuffer);
                }
            } else {
                HHMUtils.removeActiveEffects(ref, commandBuffer, effectController,
                        HHMUtils::activeEntityEffectIsStaminaBoost);
            }
        }

        HHMThirstHud.updatePlayerThirstLevel(playerRef, thirstLevel);
    }
}
