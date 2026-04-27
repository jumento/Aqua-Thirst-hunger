package mx.jume.aquahunger.compat;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.ArrayList;
import java.util.List;

public class IntegrationManager {
    private static final List<HungerThirstIntegrationBridge> bridges = new ArrayList<>();

    public static void init() {
        bridges.clear();
        MMOSkillTreeBridge mmo = new MMOSkillTreeBridge();
        if (mmo.isAvailable()) {
            bridges.add(mmo);
            //mx.jume.aquahunger.AquaThirstHunger.logInfo("[mmo] Success: MMOSkillTree bridge initialized and registered.");
        } else {
            //mx.jume.aquahunger.AquaThirstHunger.logInfo("[mmo] MMOSkillTree bridge is not available.");
        }

        RPGLevelingBridge rpg = new RPGLevelingBridge();
        if (rpg.isAvailable()) {
            bridges.add(rpg);
            //mx.jume.aquahunger.AquaThirstHunger.logInfo("[rpg] Success: RPGLeveling bridge initialized and registered.");
        } else {
            //mx.jume.aquahunger.AquaThirstHunger.logInfo("[rpg] RPGLeveling bridge is not available.");
        }

        EndlessLevelingBridge endless = new EndlessLevelingBridge();
        if (endless.isAvailable()) {
            bridges.add(endless);
            //mx.jume.aquahunger.AquaThirstHunger.logInfo("[endless] Success: EndlessLeveling bridge initialized and registered.");
        } else {
            //mx.jume.aquahunger.AquaThirstHunger.logInfo("[endless] EndlessLeveling bridge is not available.");
        }

        if (bridges.isEmpty()) {
            //mx.jume.aquahunger.AquaThirstHunger.logInfo("[rpg|mmo|endless] Running without active XP bridges.");
        }
    }

    public static float getStaminaLossMultiplier(Ref<EntityStore> ref, Store<EntityStore> store) {
        float mult = 1.0f;
        for (HungerThirstIntegrationBridge b : bridges) mult *= b.getStaminaLossMultiplier(ref, store);
        return mult;
    }

    public static float getWorkLossMultiplier(Ref<EntityStore> ref, Store<EntityStore> store) {
        float mult = 1.0f;
        for (HungerThirstIntegrationBridge b : bridges) mult *= b.getWorkLossMultiplier(ref, store);
        return mult;
    }

    public static float getMetabolismLossMultiplier(Ref<EntityStore> ref, Store<EntityStore> store) {
        float mult = 1.0f;
        for (HungerThirstIntegrationBridge b : bridges) mult *= b.getMetabolismLossMultiplier(ref, store);
        return mult;
    }

    public static float getHealCostMultiplier(Ref<EntityStore> ref, Store<EntityStore> store) {
        float mult = 1.0f;
        for (HungerThirstIntegrationBridge b : bridges) mult *= b.getHealCostMultiplier(ref, store);
        return mult;
    }

    public static float getThirstStaminaLossMultiplier(Ref<EntityStore> ref, Store<EntityStore> store) {
        float mult = 1.0f;
        for (HungerThirstIntegrationBridge b : bridges) mult *= b.getThirstStaminaLossMultiplier(ref, store);
        return mult;
    }

    public static float getThirstWorkLossMultiplier(Ref<EntityStore> ref, Store<EntityStore> store) {
        float mult = 1.0f;
        for (HungerThirstIntegrationBridge b : bridges) mult *= b.getThirstWorkLossMultiplier(ref, store);
        return mult;
    }

    public static float getThirstMetabolismLossMultiplier(Ref<EntityStore> ref, Store<EntityStore> store) {
        float mult = 1.0f;
        for (HungerThirstIntegrationBridge b : bridges) mult *= b.getThirstMetabolismLossMultiplier(ref, store);
        return mult;
    }

    public static float getRehydrationBonus(Ref<EntityStore> ref, Store<EntityStore> store) {
        float sum = 0.0f;
        for (HungerThirstIntegrationBridge b : bridges) sum += b.getRehydrationBonus(ref, store);
        return sum;
    }

    public static void awardXP(Ref<EntityStore> ref, Store<EntityStore> store, String skillId, float amount, String reason) {
        //mx.jume.aquahunger.AquaThirstHunger.logInfo("[aquahunger] Awarding " + amount + " XP for skill " + skillId + " reason: " + reason);
        for (HungerThirstIntegrationBridge b : bridges) b.awardXP(ref, store, skillId, amount, reason);
    }

    public static void onEatingAction(Ref<EntityStore> ref, Store<EntityStore> store,
            CommandBuffer<EntityStore> commandBuffer, PlayerRef playerRef) {
        for (HungerThirstIntegrationBridge b : bridges) {
            b.onEatingAction(ref, store, commandBuffer, playerRef);
        }
    }

    public static int getSkillLevel(Ref<EntityStore> ref, Store<EntityStore> store, String skillId) {
        int max = 0;
        for (HungerThirstIntegrationBridge b : bridges) {
            max = Math.max(max, b.getSkillLevel(ref, store, skillId));
        }
        return max;
    }

    public static boolean isRpgLevelingAvailable() {
        for (HungerThirstIntegrationBridge b : bridges) {
            if (b instanceof RPGLevelingBridge) return b.isAvailable();
        }
        return false;
    }

    public static boolean isEndlessLevelingAvailable() {
        for (HungerThirstIntegrationBridge b : bridges) {
            if (b instanceof EndlessLevelingBridge) return b.isAvailable();
        }
        return false;
    }

    /** @return true si algún bridge de leveling (RPG o Endless) está activo. */
    public static boolean isAnyLevelingAvailable() {
        return isRpgLevelingAvailable() || isEndlessLevelingAvailable();
    }

    public static ClassLoader getPluginClassLoader(String pluginId) {
        try {
            java.lang.reflect.Field initField = com.hypixel.hytale.server.core.plugin.JavaPlugin.class.getDeclaredField("init");
            initField.setAccessible(true);
            
            mx.jume.aquahunger.AquaThirstHunger main = mx.jume.aquahunger.AquaThirstHunger.get();
            if (main == null) return null;
            
            Object init = initField.get(main);
            if (init == null) return null;
            
            java.lang.reflect.Method getPm = init.getClass().getMethod("getPluginManager");
            Object pm = getPm.invoke(init);
            if (pm == null) return null;
            
            java.lang.reflect.Method getPlugins = pm.getClass().getMethod("getPlugins");
            java.util.Collection<?> plugins = (java.util.Collection<?>) getPlugins.invoke(pm);
            
            for (Object p : plugins) {
                java.lang.reflect.Method getId = p.getClass().getMethod("getId");
                String id = (String) getId.invoke(p);
                if (id != null && id.equalsIgnoreCase(pluginId)) {
                    return p.getClass().getClassLoader();
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
}
