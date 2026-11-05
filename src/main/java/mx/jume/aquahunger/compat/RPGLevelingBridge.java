package mx.jume.aquahunger.compat;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import mx.jume.aquahunger.AquaThirstHunger;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;

/**
 * Bridge for RPG Leveling And Stats/Skills ({@code Zuxaw:RPGLeveling}), API package
 * {@code org.zuxaw.plugin.api}. Checked against 0.4.2.
 *
 * <p>Uses reflection so the mod loads without RPG Leveling. {@code addXP} is safe to call from a
 * system tick: RPG Leveling hands the award to the player's world thread itself.
 */
public class RPGLevelingBridge implements HungerThirstIntegrationBridge {
    private static final String API_CLASS = "org.zuxaw.plugin.api.RPGLevelingAPI";
    private static final String XP_SOURCE_CLASS = "org.zuxaw.plugin.api.XPSource";

    private boolean available = false;
    private Object apiInstance;
    private Method addXpMethod;
    private Method getLevelMethod;
    private Object xpSource;
    private Object levelUpListener;
    private Method unregisterListenerMethod;

    public RPGLevelingBridge() {
        // Lightweight: initialize() runs once RPG Leveling has started (from our start()).
    }

    public synchronized void initialize() {
        if (available) {
            return;
        }
        try {
            Class<?> apiClass = OptionalPlugins.loadClass(OptionalPlugins.RPG_LEVELING, API_CLASS);

            Method isAvailable = apiClass.getMethod("isAvailable");
            if (!(boolean) isAvailable.invoke(null)) {
                AquaThirstHunger.logWarning("[rpg] RPG Leveling is installed but its API is not available.");
                return;
            }

            apiInstance = apiClass.getMethod("get").invoke(null);
            if (apiInstance == null) {
                AquaThirstHunger.logWarning("[rpg] RPGLevelingAPI.get() returned null.");
                return;
            }

            Class<?> xpSourceClass = OptionalPlugins.loadClass(OptionalPlugins.RPG_LEVELING, XP_SOURCE_CLASS);
            xpSource = xpSourceClass.getMethod("create", String.class).invoke(null, "AQUATHIRST_HYDRATION");

            addXpMethod = apiClass.getMethod("addXP", UUID.class, double.class, xpSourceClass);
            getLevelMethod = apiClass.getMethod("getPlayerLevel", UUID.class);

            registerListeners(apiClass);

            available = true;
        } catch (ClassNotFoundException e) {
            // RPG Leveling is not installed.
        } catch (Exception e) {
            AquaThirstHunger.logWarning("[rpg] Could not connect to RPG Leveling: " + e);
        }
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    /** True when RPG Leveling's level-up events reach the HUD. */
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

                // +XP / -XP en el HUD durante unos segundos
                XpFeedback.show(playerRef);

                AquaThirstHunger.logDebug(String.format(
                        "[rpg] XP to %s | base %.1f | final %.2f | mods [H %+.1f%%, T %+.1f%%] | level %d",
                        playerRef.getUsername(), (double) amount, finalXp, hungerMod * 100, thirstMod * 100, rpgLevel));
            }
        } catch (Exception e) {
            AquaThirstHunger.logDebug("[rpg] Could not award XP: " + e);
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

    @Override
    public synchronized void shutdown() {
        if (levelUpListener == null || unregisterListenerMethod == null) {
            return;
        }
        try {
            unregisterListenerMethod.invoke(apiInstance, levelUpListener);
        } catch (Exception ignored) {
            // RPG Leveling may already be shutting down.
        }
        levelUpListener = null;
    }

    // ==========================================
    // Level-up listener (Proxy, via AquaSanity)
    // ==========================================

    private void registerListeners(Class<?> apiClass) {
        try {
            Method registerMethod = findMethod(apiClass, "registerLevelUpListener");
            if (registerMethod == null) return;

            Class<?> listenerInterface = registerMethod.getParameterTypes()[0];

            // RPG Leveling may keep listeners in a collection that calls equals/hashCode.
            Object proxy = Proxy.newProxyInstance(
                    listenerInterface.getClassLoader(),
                    new Class<?>[]{ listenerInterface },
                    (proxyObj, method, args) -> switch (method.getName()) {
                        case "equals" -> args != null && args.length == 1 && proxyObj == args[0];
                        case "hashCode" -> System.identityHashCode(proxyObj);
                        case "toString" -> "AquaThirstHunger level-up listener";
                        default -> {
                            if (args != null && args.length == 1) {
                                handleLevelUpEvent(args[0]);
                            }
                            yield null;
                        }
                    }
            );

            registerMethod.invoke(apiInstance, proxy);
            levelUpListener = proxy;
            unregisterListenerMethod = findMethod(apiClass, "unregisterLevelUpListener");
        } catch (Exception e) {
            AquaThirstHunger.logWarning("[rpg] Could not register the level-up listener: " + e);
        }
    }

    private void handleLevelUpEvent(Object event) {
        try {
            Method getPlayerMethod = event.getClass().getMethod("getPlayer");
            Object playerRefObj = getPlayerMethod.invoke(event);

            if (playerRefObj instanceof PlayerRef hytalePlayerRef) {
                // El listener puede llegar desde otro hilo: el feedback se aplica en el mundo del jugador.
                mx.jume.aquahunger.ui.HudService.runOnPlayerWorld(hytalePlayerRef,
                        () -> XpFeedback.showLevelUp(hytalePlayerRef));
            }
        } catch (Exception e) {
            AquaThirstHunger.logDebug("[rpg] Level-up event not handled: " + e);
        }
    }

    private Method findMethod(Class<?> clazz, String name) {
        for (Method m : clazz.getMethods()) {
            if (m.getName().equals(name)) return m;
        }
        return null;
    }
}
