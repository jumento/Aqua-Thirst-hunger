package mx.jume.aquahunger.compat;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import mx.jume.aquahunger.AquaThirstHunger;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Bridge para Endless Leveling ({@code Airijko:EndlessLevelingCore}). Checked against 14.1.0.
 *
 * <p>Paquete real de la API: {@code com.airijko.endlessleveling.api}
 *
 * <p>Usa reflexión para no importar directamente clases de EndlessLeveling,
 * garantizando que Aqua-Thirst-hunger arranque correctamente aunque el mod no esté presente.
 *
 * <p>Funciona de manera idéntica al bridge de RPGLeveling:
 * <ul>
 *   <li>XP otorgada con multiplicadores según estado de hambre/sed</li>
 *   <li>Nivel EL reduce pérdida de hambre/sed gradualmente</li>
 *   <li>Level-up dispara feedback visual en HUD</li>
 * </ul>
 */
public class EndlessLevelingBridge implements HungerThirstIntegrationBridge {

    private static final String API_CLASS = "com.airijko.endlessleveling.api.EndlessLevelingAPI";

    private boolean available = false;
    private Method grantXpMethod;
    private Method getLevelMethod;
    private Method apiGetMethod;
    private Consumer<Object> levelUpListener;
    private Method removeListenerMethod;

    public EndlessLevelingBridge() {
        // Lightweight: initialize() runs once Endless Leveling has started (from our start()).
    }

    public synchronized void initialize() {
        if (available) {
            return;
        }
        try {
            Class<?> apiClass = OptionalPlugins.loadClass(OptionalPlugins.ENDLESS_LEVELING, API_CLASS);

            // EndlessLevelingAPI.get() — singleton (no tiene isAvailable estático)
            apiGetMethod = apiClass.getMethod("get");
            if (apiGetMethod.invoke(null) == null) {
                AquaThirstHunger.logWarning("[endless] EndlessLevelingAPI.get() returned null.");
                return;
            }

            // grantXp(UUID, double) se mantiene en la 14.x (fuente API_GRANT).
            grantXpMethod = apiClass.getMethod("grantXp", UUID.class, double.class);
            getLevelMethod = apiClass.getMethod("getPlayerLevel", UUID.class);

            registerLevelUpListener(apiClass);

            available = true;
        } catch (ClassNotFoundException e) {
            // Endless Leveling is not installed.
        } catch (Exception e) {
            AquaThirstHunger.logWarning("[endless] Could not connect to Endless Leveling: " + e);
        }
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    /** True when Endless Leveling's level-up events reach the HUD. */
    public boolean hasLevelUpListener() {
        return levelUpListener != null;
    }

    @Override
    public void awardXP(Ref<EntityStore> ref, Store<EntityStore> store, String skillId, float amount, String reason) {
        if (!available || ref == null || store == null)
            return;
        try {
            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            if (playerRef != null) {
                int elLevel = getSkillLevel(ref, store, "GLOBAL");

                float hungerMod = calculateComponentModifier(ref, store, true, elLevel);
                float thirstMod = calculateComponentModifier(ref, store, false, elLevel);

                float finalMultiplier = 1.0f + hungerMod + thirstMod;
                double finalXp = amount * finalMultiplier;

                // Set flags in components for HUD feedback
                mx.jume.aquahunger.components.HungerComponent hunger = store.getComponent(ref, mx.jume.aquahunger.components.HungerComponent.getComponentType());
                mx.jume.aquahunger.components.ThirstComponent thirst = store.getComponent(ref, mx.jume.aquahunger.components.ThirstComponent.getComponentType());

                boolean isEatingReason = reason != null && reason.contains("EATING");
                boolean isDrinkingReason = reason != null && reason.contains("DRINKING");
                boolean isResilienceH = reason != null && reason.equals("RESILIENCE");
                boolean isResilienceT = reason != null && reason.equals("RESILIENCE_THIRST");

                if (hunger != null) {
                    if (isEatingReason || (hungerMod > 0 && !isDrinkingReason && !isResilienceT)) hunger.setXpUp(true);
                    else if (isResilienceH || (hungerMod < 0 && !isDrinkingReason && !isResilienceT)) hunger.setXpDown(true);
                }
                if (thirst != null) {
                    if (isDrinkingReason || (thirstMod > 0 && !isEatingReason && !isResilienceH)) thirst.setXpUp(true);
                    else if (isResilienceT || (thirstMod < 0 && !isEatingReason && !isResilienceH)) thirst.setXpDown(true);
                }

                int levelBefore = elLevel;

                // Diferir grantXp a world.execute() para evitar conflictos con el
                // procesamiento interno de EndlessLeveling durante ticks del ECS.
                com.hypixel.hytale.server.core.universe.world.World world = store.getExternalData().getWorld();
                if (world != null) {
                    world.execute(() -> {
                        try {
                            grantXpMethod.invoke(apiGetMethod.invoke(null), playerRef.getUuid(), finalXp);
                        } catch (Exception e) {
                            AquaThirstHunger.logDebug("[endless] grantXp failed: " + e);
                        }
                    });

                    // +XP / -XP en el HUD durante unos segundos
                    XpFeedback.show(playerRef);

                    // Sin listener (API vieja): detectar el level-up comparando niveles después del grantXp.
                    if (levelUpListener == null) {
                        java.util.concurrent.CompletableFuture.delayedExecutor(200, java.util.concurrent.TimeUnit.MILLISECONDS)
                                .execute(() -> mx.jume.aquahunger.ui.HudService.runOnPlayerWorld(playerRef,
                                        () -> checkLevelUp(playerRef, levelBefore)));
                    }
                }
            }
        } catch (Exception e) {
            AquaThirstHunger.logDebug("[endless] Could not award XP: " + e);
        }
    }

    /**
     * Detecta level-up comparando nivel actual con el nivel previo.
     * Si hubo level-up, dispara feedback visual como en RPGLevelingBridge.
     */
    private void checkLevelUp(PlayerRef playerRef, int levelBefore) {
        try {
            if (getLevelByUuid(playerRef.getUuid()) > levelBefore) {
                XpFeedback.showLevelUp(playerRef);
            }
        } catch (Exception ignored) {}
    }

    private float calculateComponentModifier(Ref<EntityStore> ref, Store<EntityStore> store, boolean isHunger, int elLevel) {
        float barValue;
        float maxValue;
        if (isHunger) {
            mx.jume.aquahunger.components.HungerComponent h = store.getComponent(ref,
                    mx.jume.aquahunger.components.HungerComponent.getComponentType());
            if (h == null)
                return 0.0f;
            barValue = h.getHungerLevel();
            maxValue = h.getMaxHunger();
        } else {
            mx.jume.aquahunger.components.ThirstComponent t = store.getComponent(ref,
                    mx.jume.aquahunger.components.ThirstComponent.getComponentType());
            if (t == null)
                return 0.0f;
            barValue = t.getThirstLevel();
            maxValue = t.getMaxThirst();
        }

        float percent = (barValue / maxValue) * 100f;

        float highThreshold = isHunger ? AquaThirstHunger.get().getHungerConfig().getSatiatedThreshold() : AquaThirstHunger.get().getThirstConfig().getSatiatedThreshold();
        float lowThreshold = isHunger ? AquaThirstHunger.get().getHungerConfig().getStarveResilienceThreshold() : AquaThirstHunger.get().getThirstConfig().getStarveResilienceThreshold();

        if (percent >= highThreshold) {
            float playerFactor = Math.min(elLevel / 100f, 1.0f);
            return 0.01f + (playerFactor * 0.10f);
        } else if (percent <= lowThreshold) {
            float playerFactor = Math.min(elLevel / 100f, 1.0f);
            return -0.05f - (playerFactor * 0.15f);
        }
        return 0.0f;
    }

    @Override
    public int getSkillLevel(Ref<EntityStore> ref, Store<EntityStore> store, String skillId) {
        if (!available || ref == null || store == null)
            return 0;
        try {
            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            if (playerRef != null) {
                return getLevelByUuid(playerRef.getUuid());
            }
        } catch (Exception e) {
            return 0;
        }
        return 0;
    }

    @Override
    public int getLevelingLevel(Ref<EntityStore> ref, Store<EntityStore> store) {
        return getSkillLevel(ref, store, "GLOBAL");
    }

    @Override
    public float getStaminaLossMultiplier(Ref<EntityStore> ref, Store<EntityStore> store) {
        return getLossMultiplier(ref, store);
    }

    @Override
    public float getWorkLossMultiplier(Ref<EntityStore> ref, Store<EntityStore> store) {
        return getLossMultiplier(ref, store);
    }

    @Override
    public float getMetabolismLossMultiplier(Ref<EntityStore> ref, Store<EntityStore> store) {
        return getLossMultiplier(ref, store);
    }

    @Override
    public float getHealCostMultiplier(Ref<EntityStore> ref, Store<EntityStore> store) {
        return getLossMultiplier(ref, store);
    }

    private float getLossMultiplier(Ref<EntityStore> ref, Store<EntityStore> store) {
        int level = getSkillLevel(ref, store, "GLOBAL");
        float reduction = Math.min(level / 10, 10) * 0.02f;
        return Math.max(0.1f, 1.0f - reduction);
    }

    private int getLevelByUuid(UUID uuid) {
        if (!available || getLevelMethod == null) return 0;
        try {
            Object result = getLevelMethod.invoke(apiGetMethod.invoke(null), uuid);
            return result instanceof Integer ? Math.max(0, (Integer) result) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public synchronized void shutdown() {
        if (levelUpListener == null || removeListenerMethod == null) {
            return;
        }
        try {
            removeListenerMethod.invoke(apiGetMethod.invoke(null), levelUpListener);
        } catch (Exception ignored) {
            // Endless Leveling may already be shutting down.
        }
        levelUpListener = null;
    }

    /**
     * Endless Leveling 14+ publishes level-ups through addLevelUpListener(Consumer<LevelUpEvent>).
     * The event is a record with playerUuid(); a plain Consumer avoids depending on its type.
     */
    private void registerLevelUpListener(Class<?> apiClass) {
        try {
            Method addMethod = apiClass.getMethod("addLevelUpListener", Consumer.class);
            Consumer<Object> listener = this::handleLevelUpEvent;
            addMethod.invoke(apiGetMethod.invoke(null), listener);
            levelUpListener = listener;
            removeListenerMethod = apiClass.getMethod("removeLevelUpListener", Consumer.class);
        } catch (NoSuchMethodException e) {
            // Older Endless Leveling: level-ups are detected by comparing levels after our grants.
        } catch (Exception e) {
            AquaThirstHunger.logWarning("[endless] Could not register the level-up listener: " + e);
        }
    }

    private void handleLevelUpEvent(Object event) {
        try {
            Object uuid = event.getClass().getMethod("playerUuid").invoke(event);
            if (!(uuid instanceof UUID playerUuid)) {
                return;
            }
            PlayerRef playerRef = Universe.get().getPlayer(playerUuid);
            if (playerRef != null) {
                mx.jume.aquahunger.ui.HudService.runOnPlayerWorld(playerRef,
                        () -> XpFeedback.showLevelUp(playerRef));
            }
        } catch (Exception e) {
            AquaThirstHunger.logDebug("[endless] Level-up event not handled: " + e);
        }
    }
}
