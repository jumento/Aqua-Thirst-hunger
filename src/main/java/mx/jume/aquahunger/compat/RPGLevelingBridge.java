package mx.jume.aquahunger.compat;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import mx.jume.aquahunger.AquaThirstHunger;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;

public class RPGLevelingBridge implements HungerThirstIntegrationBridge {
    private boolean available = false;
    private Object apiInstance;
    private Method addXpMethod;
    private Method getLevelMethod;
    private Object xpSource;

    public RPGLevelingBridge() {
        // Constructor remains lightweight to allow lazy initialization later
    }

    public synchronized void initialize() {
        if (available)
            return; // Already initialized successfully

        try {
            //AquaThirstHunger.logInfo("[rpg] Initializing RPG Integration...");

            // DIAGNOSTIC START
            ClassLoader cl = AquaThirstHunger.class.getClassLoader();
            //AquaThirstHunger.logInfo("[rpg] ClassLoader: " + cl.getClass().getName());
            //AquaThirstHunger.logInfo("[rpg] Parent CL: " + (cl.getParent() != null ? cl.getParent().getClass().getName() : "null"));

            // Intentar con el context classloader del thread
            try {
                Class<?> test = Thread.currentThread().getContextClassLoader()
                        .loadClass("com.zuxaw.rpgleveling.api.RPGLevelingAPI");
                //AquaThirstHunger.logInfo("[rpg] Package com.zuxaw.rpgleveling found via context CL: " + test);
            } catch (ClassNotFoundException e) {
                //AquaThirstHunger.logInfo("[rpg] Package com.zuxaw.rpgleveling not found via context CL");
            }
            // DIAGNOSTIC END

            Class<?> apiClass = null;
            try {
                apiClass = Class.forName("org.zuxaw.plugin.api.RPGLevelingAPI");
                //AquaThirstHunger.logInfo("[rpg] Found RPGLevelingAPI at: org.zuxaw.plugin.api.RPGLevelingAPI");
            } catch (ClassNotFoundException e) {
                available = false;
                //AquaThirstHunger.logWarning("[rpg] RPGLevelingAPI class not found. Check manifest.json and IDs.");
                return;
            }

            //AquaThirstHunger.logInfo("[rpg] Verifying API Availability...");
            Method isAvailable = apiClass.getMethod("isAvailable");
            if (!(boolean) isAvailable.invoke(null)) {
                available = false;
                //AquaThirstHunger.logInfo("[rpg] RPGLevelingAPI reported as NOT available.");
                return;
            }

            //AquaThirstHunger.logInfo("[rpg] Fetching API Instance...");
            Method get = apiClass.getMethod("get");
            apiInstance = get.invoke(null);
            if (apiInstance == null) {
                //AquaThirstHunger.logWarning("[rpg] RPGLevelingAPI.get() returned null instance.");
                available = false;
                return;
            }

            Class<?> xpSourceClass = null;
            try {
                xpSourceClass = Class.forName("org.zuxaw.plugin.api.XPSource");
                //AquaThirstHunger.logInfo("[rpg] Found XPSource at: org.zuxaw.plugin.api.XPSource");
            } catch (ClassNotFoundException e) {
                available = false;
                //AquaThirstHunger.logWarning("[rpg] XPSource class not found.");
                return;
            }

            //AquaThirstHunger.logInfo("[rpg] Creating XPSource instance...");
            Method createSource = xpSourceClass.getMethod("create", String.class);
            xpSource = createSource.invoke(null, "AQUATHIRST_HYDRATION");

            //AquaThirstHunger.logInfo("[rpg] Mapping API methods...");
            addXpMethod = apiClass.getMethod("addXP", UUID.class, double.class, xpSourceClass);
            getLevelMethod = apiClass.getMethod("getPlayerLevel", UUID.class);

            // Registrar listeners vía Proxy (inspirado en AquaSanity)
            registerListeners(apiClass);

            available = true;
            //AquaThirstHunger.logInfo("[rpg] RPG Integration bridge successfully initialized.");
        } catch (Exception e) {
            available = false;
            //AquaThirstHunger.logWarning("[rpg] Error during RPG Bridge Initialization: " + e.toString());
            e.printStackTrace();
        }
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public void awardXP(Ref<EntityStore> ref, Store<EntityStore> store, String skillId, float amount, String reason) {
        if (!available || ref == null || store == null)
            return;
        try {
            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            if (playerRef != null) {
                int rpgLevel = getSkillLevel(ref, store, "GLOBAL");

                float hungerMod = calculateComponentModifier(ref, store, true, rpgLevel);
                float thirstMod = calculateComponentModifier(ref, store, false, rpgLevel);

                float finalMultiplier = 1.0f + hungerMod + thirstMod;
                double finalXp = amount * finalMultiplier;

                // Set flags in components for HUD feedback depending on the actual interaction
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

                addXpMethod.invoke(apiInstance, playerRef.getUuid(), finalXp, xpSource);

                // Paso 4: Retrasar la primera inyección 500ms
                com.hypixel.hytale.server.core.universe.world.World world = store.getExternalData().getWorld();
                if (world != null) {
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
                }

                if (hungerMod != 0.0f || thirstMod != 0.0f) {
                    double xpDiff = finalXp - amount;
                    //AquaThirstHunger.logInfo(String.format(
                    //        "[rpg] XP concedido a %s | Base: %.1f | Final: %.2f | Diff: %+.2f | Mods: [H: %+.1f%%, T: %+.1f%%] | Nivel: %d",
                    //        playerRef.getUsername(), (double) amount, finalXp, xpDiff, hungerMod * 100,
                    //        thirstMod * 100, rpgLevel));
                }
            }
        } catch (Exception e) {
            //AquaThirstHunger.logWarning("[rpg] Error al concedir XP: " + e.getMessage());
        }
    }

    private float calculateComponentModifier(Ref<EntityStore> ref, Store<EntityStore> store, boolean isHunger, int rpgLevel) {
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
            float playerFactor = Math.min(rpgLevel / 100f, 1.0f);
            return 0.01f + (playerFactor * 0.10f); // Bono por estar bien alimentado/hidratado
        } else if (percent <= lowThreshold) {
            float playerFactor = Math.min(rpgLevel / 100f, 1.0f);
            return -0.05f - (playerFactor * 0.15f); // Penalización por hambre/sed extremo
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
                Object result = getLevelMethod.invoke(apiInstance, playerRef.getUuid());
                return result instanceof Integer ? (Integer) result : 0;
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

    // ==========================================
    // Lógica de Proxy (vía AquaSanity)
    // ==========================================

    private void registerListeners(Class<?> apiClass) {
        try {
            Method registerMethod = findMethod(apiClass, "registerLevelUpListener");
            if (registerMethod == null) return;

            Class<?> listenerInterface = registerMethod.getParameterTypes()[0];

            Object proxy = Proxy.newProxyInstance(
                    listenerInterface.getClassLoader(),
                    new Class<?>[]{ listenerInterface },
                    (proxyObj, method, args) -> {
                        if (args != null && args.length > 0 && !method.getName().equals("toString")) {
                            handleLevelUpEvent(args[0]);
                        }
                        return null;
                    }
            );

            registerMethod.invoke(apiInstance, proxy);
            //AquaThirstHunger.logInfo("[rpg] LevelUpListener registrado (Proxy activado).");
        } catch (Exception e) {
            //AquaThirstHunger.logWarning("[rpg] No se pudo registrar LevelUpListener: " + e.getMessage());
        }
    }

    private void handleLevelUpEvent(Object event) {
        try {
            Method getPlayerMethod = event.getClass().getMethod("getPlayer");
            Object playerRefObj = getPlayerMethod.invoke(event);

            if (playerRefObj instanceof PlayerRef hytalePlayerRef) {
                // Sincronización HUD post-subida de nivel
                Ref<EntityStore> ref = hytalePlayerRef.getReference();
                if (ref == null) return;

                int newLevel = 0;
                try {
                    Method getNewLevel = event.getClass().getMethod("getNewLevel");
                    newLevel = (int) getNewLevel.invoke(event);
                } catch (Exception ignored) {}

                //AquaThirstHunger.logInfo(String.format("[rpg] Player %s subió de nivel RPG! (Nivel: %d).",
                //    hytalePlayerRef.getUsername(), newLevel));
                
                // Forzar feedback visual WOW pero retrasado para evadir el reinject
                mx.jume.aquahunger.components.HungerComponent hunger = ref.getStore().getComponent(ref, mx.jume.aquahunger.components.HungerComponent.getComponentType());
                mx.jume.aquahunger.components.ThirstComponent thirst = ref.getStore().getComponent(ref, mx.jume.aquahunger.components.ThirstComponent.getComponentType());
                if (hunger != null) hunger.setXpUp(true);
                
                com.hypixel.hytale.server.core.universe.world.World world = ref.getStore().getExternalData().getWorld();
                if (world != null) {
                    java.util.concurrent.CompletableFuture.delayedExecutor(500, java.util.concurrent.TimeUnit.MILLISECONDS)
                            .execute(() -> world.execute(() -> {
                                try {
                                    if (hytalePlayerRef != null && hytalePlayerRef.getReference() != null && hytalePlayerRef.getReference().getStore() != null) {
                                        float hLvl = hunger != null ? hunger.getHungerLevel() : 100.0f;
                                        float tLvl = thirst != null ? thirst.getThirstLevel() : 100.0f;
                                        mx.jume.aquahunger.ui.HHMHud.updatePlayerHungerLevel(hytalePlayerRef, hLvl);
                                        mx.jume.aquahunger.ui.HHMThirstHud.updatePlayerThirstLevel(hytalePlayerRef, tLvl);
                                    }
                                } catch (Exception ignored) {}
                            }));
                            
                    // Limpieza (TTL) a los 3 segundos
                    java.util.concurrent.CompletableFuture.delayedExecutor(3000, java.util.concurrent.TimeUnit.MILLISECONDS)
                            .execute(() -> world.execute(() -> {
                                try {
                                    if (hytalePlayerRef != null && hytalePlayerRef.getReference() != null && hytalePlayerRef.getReference().getStore() != null) {
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
                                        mx.jume.aquahunger.ui.HHMHud.updatePlayerHungerLevel(hytalePlayerRef, hLvl);
                                        mx.jume.aquahunger.ui.HHMThirstHud.updatePlayerThirstLevel(hytalePlayerRef, tLvl);
                                    }
                                } catch (Exception ignored) {}
                            }));
                }
            }
        } catch (Exception e) {
            //AquaThirstHunger.logWarning("[rpg] Error en LevelUpListener Proxy: " + e.getMessage());
        }
    }

    private Method findMethod(Class<?> clazz, String name) {
        for (Method m : clazz.getMethods()) {
            if (m.getName().equals(name)) return m;
        }
        return null;
    }
}
