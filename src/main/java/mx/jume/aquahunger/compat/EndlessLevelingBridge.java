package mx.jume.aquahunger.compat;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import mx.jume.aquahunger.AquaThirstHunger;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bridge para EndlessLeveling (airijko:EndlessLeveling).
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
    private Object apiInstance;
    private Method grantXpMethod;
    private Method getLevelMethod;
    private Method apiGetMethod;

    // Cache de niveles para detectar level-ups (EndlessLeveling no expone listener)
    private final ConcurrentHashMap<UUID, Integer> cachedLevels = new ConcurrentHashMap<>();

    public EndlessLevelingBridge() {
        // Constructor ligero — la detección se hace en isAvailable()
    }

    @Override
    public boolean isAvailable() {
        if (available) return true;
        try {
            Class<?> apiClass = Class.forName(API_CLASS);

            // EndlessLevelingAPI.get() — singleton (no tiene isAvailable estático)
            Method getMethod = apiClass.getMethod("get");
            apiInstance = getMethod.invoke(null);
            if (apiInstance == null) {
                return false;
            }

            // Cache API methods
            grantXpMethod = apiClass.getMethod("grantXp", UUID.class, double.class);
            getLevelMethod = apiClass.getMethod("getPlayerLevel", UUID.class);
            apiGetMethod = apiClass.getMethod("get");

            available = true;
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
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

                // Capturar nivel antes para detectar level-up
                int levelBefore = elLevel;
                cachedLevels.put(playerRef.getUuid(), levelBefore);

                // Diferir grantXp a world.execute() para evitar conflictos con el
                // procesamiento interno de EndlessLeveling durante ticks del ECS.
                com.hypixel.hytale.server.core.universe.world.World world = store.getExternalData().getWorld();
                if (world != null) {
                    world.execute(() -> {
                        try {
                            Object freshApi = apiGetMethod != null ? apiGetMethod.invoke(null) : apiInstance;
                            if (freshApi == null) freshApi = apiInstance;
                            grantXpMethod.invoke(freshApi, playerRef.getUuid(), finalXp);
                        } catch (Exception ignored) {}
                    });

                    java.util.concurrent.CompletableFuture.delayedExecutor(500, java.util.concurrent.TimeUnit.MILLISECONDS)
                            .execute(() -> world.execute(() -> {
                                try {
                                    if (playerRef != null && playerRef.getReference() != null) {
                                        float hLvl = hunger != null ? hunger.getHungerLevel() : 100.0f;
                                        float tLvl = thirst != null ? thirst.getThirstLevel() : 100.0f;
                                        mx.jume.aquahunger.ui.HHMHud.updatePlayerHungerLevel(playerRef, hLvl);
                                        mx.jume.aquahunger.ui.HHMThirstHud.updatePlayerThirstLevel(playerRef, tLvl);
                                    }
                                } catch (Exception ignored) {}
                            }));

                    // Limpieza (TTL) a los 3 segundos
                    java.util.concurrent.CompletableFuture.delayedExecutor(3000, java.util.concurrent.TimeUnit.MILLISECONDS)
                            .execute(() -> world.execute(() -> {
                                try {
                                    if (playerRef != null && playerRef.getReference() != null) {
                                        if (hunger != null) {
                                            hunger.setXpUp(false);
                                            hunger.setXpDown(false);
                                        }
                                        if (thirst != null) {
                                            thirst.setXpUp(false);
                                            thirst.setXpDown(false);
                                        }
                                        float hLvl = hunger != null ? hunger.getHungerLevel() : 100.0f;
                                        float tLvl = thirst != null ? thirst.getThirstLevel() : 100.0f;
                                        mx.jume.aquahunger.ui.HHMHud.updatePlayerHungerLevel(playerRef, hLvl);
                                        mx.jume.aquahunger.ui.HHMThirstHud.updatePlayerThirstLevel(playerRef, tLvl);
                                    }
                                } catch (Exception ignored) {}
                            }));

                    // Detectar level-up (diferido para que grantXp ya se haya procesado)
                    java.util.concurrent.CompletableFuture.delayedExecutor(200, java.util.concurrent.TimeUnit.MILLISECONDS)
                            .execute(() -> world.execute(() -> {
                                checkLevelUp(playerRef, store, world, hunger, thirst, levelBefore);
                            }));
                }
            }
        } catch (Exception e) {
            // Silenciado como en RPGLevelingBridge
        }
    }

    /**
     * Detecta level-up comparando nivel actual con el nivel previo.
     * Si hubo level-up, dispara feedback visual como en RPGLevelingBridge.
     */
    private void checkLevelUp(PlayerRef playerRef, Store<EntityStore> store,
            com.hypixel.hytale.server.core.universe.world.World world,
            mx.jume.aquahunger.components.HungerComponent hunger,
            mx.jume.aquahunger.components.ThirstComponent thirst,
            int levelBefore) {
        try {
            UUID uuid = playerRef.getUuid();
            int levelAfter = getLevelByUuid(uuid);
            cachedLevels.put(uuid, levelAfter);

            if (levelAfter > levelBefore) {
                // Forzar feedback visual WOW retrasado (idéntico a RPGLevelingBridge)
                if (hunger != null) hunger.setXpUp(true);

                java.util.concurrent.CompletableFuture.delayedExecutor(500, java.util.concurrent.TimeUnit.MILLISECONDS)
                        .execute(() -> world.execute(() -> {
                            try {
                                if (playerRef != null && playerRef.getReference() != null && playerRef.getReference().getStore() != null) {
                                    float hLvl = hunger != null ? hunger.getHungerLevel() : 100.0f;
                                    float tLvl = thirst != null ? thirst.getThirstLevel() : 100.0f;
                                    mx.jume.aquahunger.ui.HHMHud.updatePlayerHungerLevel(playerRef, hLvl);
                                    mx.jume.aquahunger.ui.HHMThirstHud.updatePlayerThirstLevel(playerRef, tLvl);
                                }
                            } catch (Exception ignored) {}
                        }));

                // Limpieza (TTL) a los 3 segundos
                java.util.concurrent.CompletableFuture.delayedExecutor(3000, java.util.concurrent.TimeUnit.MILLISECONDS)
                        .execute(() -> world.execute(() -> {
                            try {
                                if (playerRef != null && playerRef.getReference() != null && playerRef.getReference().getStore() != null) {
                                    if (hunger != null) {
                                        hunger.setXpUp(false);
                                        hunger.setXpDown(false);
                                    }
                                    if (thirst != null) {
                                        thirst.setXpUp(false);
                                        thirst.setXpDown(false);
                                    }
                                    float hLvl = hunger != null ? hunger.getHungerLevel() : 100.0f;
                                    float tLvl = thirst != null ? thirst.getThirstLevel() : 100.0f;
                                    mx.jume.aquahunger.ui.HHMHud.updatePlayerHungerLevel(playerRef, hLvl);
                                    mx.jume.aquahunger.ui.HHMThirstHud.updatePlayerThirstLevel(playerRef, tLvl);
                                }
                            } catch (Exception ignored) {}
                        }));
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
            Object freshApi = apiGetMethod != null ? apiGetMethod.invoke(null) : apiInstance;
            if (freshApi == null) freshApi = apiInstance;
            Object result = getLevelMethod.invoke(freshApi, uuid);
            return result instanceof Integer ? Math.max(0, (Integer) result) : 0;
        } catch (Exception e) {
            return 0;
        }
    }
}
