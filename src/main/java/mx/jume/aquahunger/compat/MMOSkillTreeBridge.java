package mx.jume.aquahunger.compat;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import mx.jume.aquahunger.AquaThirstHunger;

import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Bridge to MMOSkillTree mod (ziggfreed).
 * Reads claimed skill tree rewards to calculate multipliers and trigger Glutton/Junk systems.
 */
public class MMOSkillTreeBridge implements HungerThirstIntegrationBridge {

    private boolean available = false;

    // --- Core API reflection ---
    private Method addXpMethod;
    private Method getLevelMethod;

    // --- Reward reading reflection ---
    private Method getSkillComponentMethod;
    private Method getClaimedRewardsMethod;
    private Method rewardGetIdMethod;

    // --- Glutton item pool: [choiceId, itemId] ---
    private static final String[][] NUTRITION_GLUTTON_ITEMS = {
        {"nutrition_glutton_1", "Food_Wildmeat_Raw"},
        {"nutrition_glutton_2", "Food_Fish_Raw"},
        {"nutrition_glutton_3", "Plant_Fruit_Berries_Red"},
        {"nutrition_glutton_4", "Food_Egg"},
        {"nutrition_glutton_5", "Plant_Fruit_Apple"},
        {"nutrition_glutton_6", "Food_Cheese"},
        {"nutrition_glutton_7", "Food_Salad_Berry"},
        {"nutrition_glutton_8", "Food_Popcorn"},
        {"nutrition_glutton_9", "Food_Pie_Meat"},
        {"nutrition_mega_glutton", "Food_Pie_Meat"}
    };

    // --- Junk system ---
    private static final String[] JUNK_ITEMS = {"Ingredient_Bone_Fragment", "Deco_Trash"};
    private static final float BASE_JUNK_CHANCE = 0.65f;
    private static final float MIN_JUNK_CHANCE = 0.25f;

    private final Random random = new Random();
    private boolean rewardReadingAvailable = false;

    // ========================================================================
    // Constructor
    // ========================================================================

    public MMOSkillTreeBridge() {
        try {
            Class<?> apiClass = Class.forName("com.ziggfreed.mmoskilltree.api.MMOSkillTreeAPI");
            
            // ALWAYS register/overwrite the skill tree file if MMO is present.
            MMOSkillTreeRegistrar.ensureRegistered();

            addXpMethod = apiClass.getMethod("addXp", Store.class, Ref.class, String.class, long.class);
            getLevelMethod = apiClass.getMethod("getLevel", Store.class, Ref.class, String.class);
            available = true;
            //AquaThirstHunger.logInfo("[mmo] API detected and initialized via static reflection.");

            // Reward reading (optional — if MMO version doesn't have it, multipliers fall back to level-based)
            try {
                getSkillComponentMethod = apiClass.getMethod("getSkillComponent", Store.class, Ref.class);

                Class<?> serviceClass = Class.forName("com.ziggfreed.mmoskilltree.service.SkillTreeService");
                Class<?> skillCompClass = Class.forName("com.ziggfreed.mmoskilltree.data.SkillComponent");
                getClaimedRewardsMethod = serviceClass.getMethod("getClaimedRewardsForSkillByName", skillCompClass, String.class);

                Class<?> rewardClass = Class.forName("com.ziggfreed.mmoskilltree.data.SkillReward");
                rewardGetIdMethod = rewardClass.getMethod("getId");

                rewardReadingAvailable = true;
                //AquaThirstHunger.logInfo("[mmo] Skill tree reward reading is available. Glutton/multiplier system active.");
            } catch (Exception e) {
                rewardReadingAvailable = false;
                //AquaThirstHunger.logInfo("[mmo] Reward reading not available (older MMO version?). Using level-based fallback.");
            }
            
            // ALWAYS register/overwrite the skill tree file if MMO is present.
            MMOSkillTreeRegistrar.ensureRegistered();
            
        } catch (Exception e) {
            available = false;

        }
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    // ========================================================================
    // Shared: Read claimed reward IDs for a skill
    // ========================================================================

    /**
     * Returns the list of choice IDs the player has claimed for a given skill.
     * E.g. ["nutrition_runner_1", "nutrition_glutton_3", "nutrition_hungry_1"]
     */
    private List<String> getClaimedRewardIds(Ref<EntityStore> ref, Store<EntityStore> store, String skillId) {
        List<String> ids = new ArrayList<>();
        if (!rewardReadingAvailable || ref == null || store == null) return ids;
        try {
            Object skillComp = getSkillComponentMethod.invoke(null, store, ref);
            if (skillComp == null) return ids;

            @SuppressWarnings("unchecked")
            List<?> rewards = (List<?>) getClaimedRewardsMethod.invoke(null, skillComp, skillId);
            if (rewards == null) return ids;

            for (Object reward : rewards) {
                String id = (String) rewardGetIdMethod.invoke(reward);
                if (id != null) ids.add(id);
            }
        } catch (Exception e) {
            //AquaThirstHunger.logWarning("[mmo] Error reading claimed rewards for " + skillId + ": " + e.getMessage());
        }
        return ids;
    }

    // ========================================================================
    // Multipliers — Based on claimed rewards
    // ========================================================================

    /**
     * Sprint loss multiplier. Reduced by Corredor picks (-5% each) and Rey del Fitness (-20%).
     */
    @Override
    public float getStaminaLossMultiplier(Ref<EntityStore> ref, Store<EntityStore> store) {
        if (!rewardReadingAvailable) return fallbackStaminaMultiplier(ref, store);

        List<String> claimed = getClaimedRewardIds(ref, store, "NUTRITION");
        float reduction = 0f;
        for (String id : claimed) {
            if (id.startsWith("nutrition_runner_")) reduction += 0.05f;
            if ("nutrition_king_fitness".equals(id)) reduction += 0.20f;
        }
        return Math.max(0.1f, 1.0f - reduction);
    }

    /**
     * Mining/block hit loss multiplier. Reduced by Minero picks (-5% each) and Rey del Fitness (-20%).
     */
    @Override
    public float getWorkLossMultiplier(Ref<EntityStore> ref, Store<EntityStore> store) {
        if (!rewardReadingAvailable) return fallbackWorkMultiplier(ref, store);

        List<String> claimed = getClaimedRewardIds(ref, store, "NUTRITION");
        float reduction = 0f;
        for (String id : claimed) {
            if (id.startsWith("nutrition_miner_")) reduction += 0.05f;
            if ("nutrition_king_fitness".equals(id)) reduction += 0.20f;
        }
        return Math.max(0.1f, 1.0f - reduction);
    }

    /**
     * Passive depletion multiplier. Reduced by Fitness picks (-5% each) and Rey del Fitness (-15%).
     */
    @Override
    public float getMetabolismLossMultiplier(Ref<EntityStore> ref, Store<EntityStore> store) {
        if (!rewardReadingAvailable) return fallbackMetabolismMultiplier(ref, store);

        List<String> claimed = getClaimedRewardIds(ref, store, "NUTRITION");
        float reduction = 0f;
        for (String id : claimed) {
            if (id.startsWith("nutrition_fitness_")) reduction += 0.05f;
            if ("nutrition_king_fitness".equals(id)) reduction += 0.15f;
        }
        return Math.max(0.1f, 1.0f - reduction);
    }

    @Override
    public float getHealCostMultiplier(Ref<EntityStore> ref, Store<EntityStore> store) {
        // Keep level-based until THIRST tree is implemented
        int nutrition = getSkillLevel(ref, store, "NUTRITION");
        float reduction = 0f;
        if (nutrition >= 10) reduction += 0.15f;
        if (nutrition >= 20) reduction += 0.15f;
        return Math.max(0.1f, 1.0f - reduction);
    }

    // --- Fallbacks for older MMO versions without reward reading ---

    private float fallbackStaminaMultiplier(Ref<EntityStore> ref, Store<EntityStore> store) {
        int nutrition = getSkillLevel(ref, store, "NUTRITION");
        int thirst = getSkillLevel(ref, store, "THIRST");
        int avgLevel = (nutrition + thirst) / 2;
        float reduction = 0;
        if (avgLevel >= 1) reduction += 0.05f;
        if (avgLevel >= 10) reduction += 0.10f;
        if (avgLevel >= 20) reduction += 0.10f;
        return Math.max(0.1f, 1.0f - reduction);
    }

    private float fallbackWorkMultiplier(Ref<EntityStore> ref, Store<EntityStore> store) {
        int nutrition = getSkillLevel(ref, store, "NUTRITION");
        float reduction = 0;
        if (nutrition >= 5) reduction += 0.10f;
        if (nutrition >= 15) reduction += 0.10f;
        if (nutrition >= 20) reduction += 0.10f;
        return Math.max(0.1f, 1.0f - reduction);
    }

    private float fallbackMetabolismMultiplier(Ref<EntityStore> ref, Store<EntityStore> store) {
        int nutrition = getSkillLevel(ref, store, "NUTRITION");
        int thirst = getSkillLevel(ref, store, "THIRST");
        int maxLvl = Math.max(nutrition, thirst);
        float reduction = 0;
        if (maxLvl >= 1) reduction += 0.05f;
        if (maxLvl >= 10) reduction += 0.05f;
        if (maxLvl >= 20) reduction += 0.10f;
        return Math.max(0.1f, 1.0f - reduction);
    }

    // ========================================================================
    // THIRST — Specific multipliers
    // ========================================================================

    /**
     * Stamina drain multiplier for THIRST.
     * Reduced by thirst_runner_* (-5% each) and thirst_king_fitness (-20%).
     */
    @Override
    public float getThirstStaminaLossMultiplier(Ref<EntityStore> ref, Store<EntityStore> store) {
        if (!rewardReadingAvailable) return 1.0f;
        List<String> claimed = getClaimedRewardIds(ref, store, "THIRST");
        float reduction = 0f;
        for (String id : claimed) {
            if (id.startsWith("thirst_runner_")) reduction += 0.05f;
            if ("thirst_king_fitness".equals(id)) reduction += 0.20f;
        }
        return Math.max(0.1f, 1.0f - reduction);
    }

    /**
     * Mining drain multiplier for THIRST.
     * Reduced by thirst_miner_* (-5% each) and thirst_king_fitness (-20%).
     */
    @Override
    public float getThirstWorkLossMultiplier(Ref<EntityStore> ref, Store<EntityStore> store) {
        if (!rewardReadingAvailable) return 1.0f;
        List<String> claimed = getClaimedRewardIds(ref, store, "THIRST");
        float reduction = 0f;
        for (String id : claimed) {
            if (id.startsWith("thirst_miner_")) reduction += 0.05f;
            if ("thirst_king_fitness".equals(id)) reduction += 0.20f;
        }
        return Math.max(0.1f, 1.0f - reduction);
    }

    /**
     * Passive depletion multiplier for THIRST.
     * Reduced by thirst_fitness_* (-5% each) and thirst_super_healthy (-15%).
     */
    @Override
    public float getThirstMetabolismLossMultiplier(Ref<EntityStore> ref, Store<EntityStore> store) {
        if (!rewardReadingAvailable) return 1.0f;
        List<String> claimed = getClaimedRewardIds(ref, store, "THIRST");
        float reduction = 0f;
        for (String id : claimed) {
            if (id.startsWith("thirst_fitness_")) reduction += 0.05f;
            if ("thirst_super_healthy".equals(id)) reduction += 0.15f;
        }
        return Math.max(0.1f, 1.0f - reduction);
    }

    /**
     * Rehydration bonus multiplier applied on top of base thirst restoration.
     * Example: base=10, bonus=0.60 → final = 10 * (1 + 0.60) = 16.
     *
     *   thirst_rehydrated_1/3/5/6/8 → +5% each
     *   thirst_rehydrated_2/4/7/9   → +10% each
     *   thirst_hydration_master     → +20%
     */
    @Override
    public float getRehydrationBonus(Ref<EntityStore> ref, Store<EntityStore> store) {
        if (!rewardReadingAvailable) return 0f;
        List<String> claimed = getClaimedRewardIds(ref, store, "THIRST");
        float bonus = 0f;
        for (String id : claimed) {
            switch (id) {
                case "thirst_rehydrated_1", "thirst_rehydrated_3",
                     "thirst_rehydrated_5", "thirst_rehydrated_6",
                     "thirst_rehydrated_8":
                    bonus += 0.05f;
                    break;
                case "thirst_rehydrated_2", "thirst_rehydrated_4",
                     "thirst_rehydrated_7", "thirst_rehydrated_9":
                    bonus += 0.10f;
                    break;
                case "thirst_hydration_master":
                    bonus += 0.20f;
                    break;
            }
        }
        return bonus;
    }

    // ========================================================================
    // Glutton System — Item delivery on eating
    // ========================================================================

    @Override
    public void onEatingAction(Ref<EntityStore> ref, Store<EntityStore> store,
            CommandBuffer<EntityStore> commandBuffer, PlayerRef playerRef) {
        if (!available || !rewardReadingAvailable || playerRef == null) return;

        List<String> claimed = getClaimedRewardIds(ref, store, "NUTRITION");

        // Collect all unlocked glutton items
        List<String[]> unlockedGluttons = new ArrayList<>();
        for (String id : claimed) {
            for (String[] entry : NUTRITION_GLUTTON_ITEMS) {
                if (entry[0].equals(id)) {
                    unlockedGluttons.add(entry);
                }
            }
        }

        if (unlockedGluttons.isEmpty()) return;

        // Select one random item from the unlocked pool
        String[] selected = unlockedGluttons.get(random.nextInt(unlockedGluttons.size()));
        String itemId = selected[1];

        // Calculate junk chance
        float junkChance = calculateJunkChance(claimed);

        // Roll for junk
        boolean isJunk = random.nextFloat() < junkChance;

        // Mega Glutton doubling logic: global x2 if unlocked, but never x2 for junk.
        int quantity = (claimed.contains("nutrition_mega_glutton") && !isJunk) ? 2 : 1;

        if (isJunk) {
            itemId = JUNK_ITEMS[random.nextInt(JUNK_ITEMS.length)];
            quantity = 1;
        }

        // Get player position using TransformComponent (no reflection)
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) return;
        Vector3d pos = transform.getPosition();

        // Deliver item
        giveOrDropItem(ref, store, commandBuffer, pos, itemId, quantity);

        // Log
        //AquaThirstHunger.logInfo(String.format("[mmo] Glutton: %s → %s x%d%s (junkChance=%.0f%%)",
        //        playerRef.getUsername(), itemId, quantity, isJunk ? " (JUNK)" : "", junkChance * 100));

        // Chat notification (Bilingual via LangManager)
        String lang = playerRef.getLanguage();
        String color = isJunk ? "#FF5555" : "#55FF55";
        String key = isJunk ? "mmo.glutton.failed.junk" : "mmo.glutton.success";
        
        String rawText = LangManager.getForLanguage(lang, key);
        String itemName = getCasualName(itemId, lang);
        String text = isJunk ? rawText : String.format(rawText, itemName + (quantity > 1 ? " x" + quantity : ""));
        
        playerRef.sendMessage(Message.empty().insert(text).color(color));
    }

    /**
     * Maps technical item IDs to player-friendly casual names for chat display.
     */
    private String getCasualName(String itemId, String lang) {
        boolean isEs = lang != null && lang.startsWith("es");
        switch (itemId) {
            case "Food_Wildmeat_Raw": return isEs ? "Carne de fauna silvestre cruda" : "Raw Wildmeat";
            case "Food_Fish_Raw": return isEs ? "Pescado crudo" : "Raw Fish";
            case "Plant_Fruit_Berries_Red": return isEs ? "Bayas rojas" : "Red Berries";
            case "Food_Egg": return isEs ? "Huevo" : "Egg";
            case "Plant_Fruit_Apple": return isEs ? "Manzana" : "Apple";
            case "Food_Cheese": return isEs ? "Queso" : "Cheese";
            case "Food_Salad_Berry": return isEs ? "Ensalada de bayas" : "Berry Salad";
            case "Food_Popcorn": return isEs ? "Palomitas" : "Popcorn";
            case "Food_Pie_Meat": return isEs ? "Pastel de carne" : "Meat Pie";
            case "Ingredient_Bone_Fragment": return isEs ? "Fragmento de hueso" : "Bone Fragment";
            case "Deco_Trash": return isEs ? "Basura" : "Trash";
            default: return itemId;
        }
    }

    /**
     * Calculates junk chance based on all claimed rewards.
     * Base: 65%. Reduced by Hambriento (-5% each), Hambriento Supremo (-15%),
     * and inherent reductions from Glutton IV (-1%), V (-2%), VI (-2%).
     * Minimum: 25%.
     */
    private float calculateJunkChance(List<String> claimed) {
        float reduction = 0f;

        for (String id : claimed) {
            // Hambriento I/II/III: -5% each
            if (id.startsWith("nutrition_hungry_") && !id.equals("nutrition_hungry_supreme")) {
                reduction += 0.05f;
            }
            // Hambriento Supremo: -15%
            if ("nutrition_hungry_supreme".equals(id)) {
                reduction += 0.15f;
            }
            // Inherent reductions from specific glutton items (applied when UNLOCKED, not per-roll)
            if ("nutrition_glutton_4".equals(id)) reduction += 0.01f;
            if ("nutrition_glutton_5".equals(id)) reduction += 0.02f;
            if ("nutrition_glutton_6".equals(id)) reduction += 0.02f;
        }

        return Math.max(MIN_JUNK_CHANCE, BASE_JUNK_CHANCE - reduction);
    }

    /**
     * Delivers an item to the player's inventory. If full, drops it on the ground.
     * Follows the pattern: inventory first → floor if full.
     */
    private void giveOrDropItem(Ref<EntityStore> ref, Store<EntityStore> store,
            CommandBuffer<EntityStore> commandBuffer, Vector3d pos, String itemId, int quantity) {
        try {
            // Create ItemStack
            ItemStack itemStack = new ItemStack(itemId, quantity);

            // Try inventory first
            InventoryComponent.Storage storage = store.getComponent(ref, InventoryComponent.Storage.getComponentType());

            if (storage != null) {
                ItemStackTransaction tx = storage.getInventory().addItemStack(itemStack);
                ItemStack remainder = tx.getRemainder();
                if (remainder == null || ItemStack.isEmpty(remainder)) {
                    return; // Item delivered to inventory successfully
                }
                // Update quantity for drop if some items were added but not all
                quantity = remainder.getQuantity();
            }

            // Inventory full or not available — drop on ground
            // Creating a fresh ItemStack for the drop to avoid side effects
            ItemStack dropStack = new ItemStack(itemId, quantity);

            Holder<EntityStore>[] holders = ItemComponent.generateItemDrops(
                    store,
                    Collections.singletonList(dropStack),
                    pos,
                    new Vector3f(
                            (float) (random.nextFloat() * 2 - 1), // X velocity
                            2.0f,                                 // Y velocity (upwards)
                            (float) (random.nextFloat() * 2 - 1)  // Z velocity
                    ));

            for (Holder<EntityStore> holder : holders) {
                commandBuffer.addEntity(holder, AddReason.SPAWN);
            }

            //AquaThirstHunger.logInfo(String.format("[mmo] Inventory full. Item %s x%d dropped at (%.1f, %.1f, %.1f)",
            //        itemId, quantity, pos.x, pos.y, pos.z));

        } catch (Exception e) {
            //AquaThirstHunger.logWarning("[mmo] Error delivering Glutton item: " + e.getMessage());
        }
    }

    // ========================================================================
    // XP and Level (existing)
    // ========================================================================

    @Override
    public void awardXP(Ref<EntityStore> ref, Store<EntityStore> store, String skillId, float amount, String reason) {
        if (!available || ref == null || store == null) return;
        try {
            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            if (playerRef == null) return;

            int level = getSkillLevel(ref, store, "GLOBAL");
            float totalMod = calculateXpModifiers(ref, store, level);
            double finalXp = amount * (1.0f + totalMod);

            addXpMethod.invoke(null, store, ref, skillId, (long) finalXp);

            if (totalMod != 0.0f) {
                //AquaThirstHunger.logInfo(String.format("[mmo] XP concedido a %s | Skill: %s | Base: %.1f | Mod: %+.1f%% | Final: %.2f",
                //        playerRef.getUsername(), skillId, (double) amount, totalMod * 100, finalXp));
            }
        } catch (Exception e) {
            //AquaThirstHunger.logWarning("[mmo] Error al conceder XP: " + e.getMessage());
        }
    }

    private float calculateXpModifiers(Ref<EntityStore> ref, Store<EntityStore> store, int level) {
        float hMod = getBarModifier(ref, store, true, level);
        float tMod = getBarModifier(ref, store, false, level);
        return hMod + tMod;
    }

    private float getBarModifier(Ref<EntityStore> ref, Store<EntityStore> store, boolean isHunger, int level) {
        float barValue, maxValue;
        if (isHunger) {
            mx.jume.aquahunger.components.HungerComponent h =
                    store.getComponent(ref, mx.jume.aquahunger.components.HungerComponent.getComponentType());
            if (h == null) return 0;
            barValue = h.getHungerLevel();
            maxValue = h.getMaxHunger();
        } else {
            mx.jume.aquahunger.components.ThirstComponent t =
                    store.getComponent(ref, mx.jume.aquahunger.components.ThirstComponent.getComponentType());
            if (t == null) return 0;
            barValue = t.getThirstLevel();
            maxValue = t.getMaxThirst();
        }
        float p = (barValue / maxValue) * 100f;
        if (p >= 90f) return 0.01f + (0.14f * Math.min(level / 100f, 1.0f));
        if (p <= 20f) return -0.15f;
        return 0;
    }

    @Override
    public int getSkillLevel(Ref<EntityStore> ref, Store<EntityStore> store, String skillId) {
        if (!available || ref == null || store == null) return 0;
        try {
            return (int) getLevelMethod.invoke(null, store, ref, skillId);
        } catch (Exception e) {
            return 0;
        }
    }
}
