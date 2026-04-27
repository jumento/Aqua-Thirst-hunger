package mx.jume.aquahunger.compat;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public interface HungerThirstIntegrationBridge {
    default boolean isAvailable() { return true; }
    
    // Multipliers (1.0 = no change, 0.9 = 10% reduction in LOSS)
    default float getStaminaLossMultiplier(Ref<EntityStore> ref, Store<EntityStore> store) { return 1.0f; }
    default float getWorkLossMultiplier(Ref<EntityStore> ref, Store<EntityStore> store) { return 1.0f; }
    default float getMetabolismLossMultiplier(Ref<EntityStore> ref, Store<EntityStore> store) { return 1.0f; }
    default float getHealCostMultiplier(Ref<EntityStore> ref, Store<EntityStore> store) { return 1.0f; }

    default float getThirstStaminaLossMultiplier(Ref<EntityStore> ref, Store<EntityStore> store) { return 1.0f; }
    default float getThirstWorkLossMultiplier(Ref<EntityStore> ref, Store<EntityStore> store) { return 1.0f; }
    default float getThirstMetabolismLossMultiplier(Ref<EntityStore> ref, Store<EntityStore> store) { return 1.0f; }
    default float getRehydrationBonus(Ref<EntityStore> ref, Store<EntityStore> store) { return 0.0f; }
    
    // Event hooks
    default void onEatingAction(Ref<EntityStore> ref, Store<EntityStore> store,
            CommandBuffer<EntityStore> commandBuffer, PlayerRef playerRef) {}

    default void awardXP(Ref<EntityStore> ref, Store<EntityStore> store, String skillId, float amount, String reason) {}
    
    default int getSkillLevel(Ref<EntityStore> ref, Store<EntityStore> store, String skillId) { return 0; }
}
