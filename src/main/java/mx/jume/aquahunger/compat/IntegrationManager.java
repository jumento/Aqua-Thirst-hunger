package mx.jume.aquahunger.compat;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import mx.jume.aquahunger.AquaThirstHunger;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class IntegrationManager {
    private static final List<HungerThirstIntegrationBridge> bridges = new ArrayList<>();

    private static RPGLevelingBridge rpg;
    private static EndlessLevelingBridge endless;
    private static MMOSkillTreeBridge mmo;

    /** Runs from our start(): RPG Leveling and Endless Leveling (optional dependencies) have started by then. */
    public static void init() {
        shutdown();

        mmo = new MMOSkillTreeBridge();
        if (mmo.isAvailable()) {
            bridges.add(mmo);
            AquaThirstHunger.logInfo("[mmo] Connected to MMO Skill Tree " + versionOf(OptionalPlugins.MMO_SKILL_TREE)
                    + (mmo.isRewardReadingAvailable() ? "." : " (without skill tree rewards)."));
        }

        rpg = new RPGLevelingBridge();
        rpg.initialize();
        if (rpg.isAvailable()) {
            bridges.add(rpg);
            AquaThirstHunger.logInfo("[rpg] Connected to RPG Leveling " + versionOf(OptionalPlugins.RPG_LEVELING) + ".");
        }

        endless = new EndlessLevelingBridge();
        endless.initialize();
        if (endless.isAvailable()) {
            bridges.add(endless);
            AquaThirstHunger.logInfo("[endless] Connected to Endless Leveling " + versionOf(OptionalPlugins.ENDLESS_LEVELING) + ".");
        }

        if (bridges.isEmpty()) {
            AquaThirstHunger.logInfo("[rpg|mmo|endless] Running without active XP bridges.");
        }
    }

    /** Unregisters the listeners the bridges left in the other mods. */
    public static void shutdown() {
        for (HungerThirstIntegrationBridge b : bridges) b.shutdown();
        bridges.clear();
    }

    private static String versionOf(com.hypixel.hytale.common.plugin.PluginIdentifier id) {
        String version = OptionalPlugins.version(id);
        return version != null ? version : "";
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
        for (HungerThirstIntegrationBridge b : bridges) b.awardXP(ref, store, skillId, amount, reason);
    }

    public static void onEatingAction(Ref<EntityStore> ref, Store<EntityStore> store,
            CommandBuffer<EntityStore> commandBuffer, PlayerRef playerRef) {
        for (HungerThirstIntegrationBridge b : bridges) {
            b.onEatingAction(ref, store, commandBuffer, playerRef);
        }
    }

    /**
     * The player's level in RPG Leveling or Endless Leveling (the higher one), which raises the
     * maximum thirst. MMO Skill Tree has no overall level and does not count here.
     */
    public static int getLevelingLevel(Ref<EntityStore> ref, Store<EntityStore> store) {
        int max = 0;
        for (HungerThirstIntegrationBridge b : bridges) {
            max = Math.max(max, b.getLevelingLevel(ref, store));
        }
        return max;
    }

    /** @return the RPG Leveling bridge when it is connected, else null. */
    @Nullable
    public static RPGLevelingBridge getRpgBridge() {
        return rpg != null && rpg.isAvailable() ? rpg : null;
    }

    /** @return the Endless Leveling bridge when it is connected, else null. */
    @Nullable
    public static EndlessLevelingBridge getEndlessBridge() {
        return endless != null && endless.isAvailable() ? endless : null;
    }

    /** @return the MMO Skill Tree bridge when it is connected, else null. */
    @Nullable
    public static MMOSkillTreeBridge getMmoBridge() {
        return mmo != null && mmo.isAvailable() ? mmo : null;
    }
}
