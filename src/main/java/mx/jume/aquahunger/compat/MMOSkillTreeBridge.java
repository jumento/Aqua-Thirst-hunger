package mx.jume.aquahunger.compat;

import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import mx.jume.aquahunger.AquaThirstHunger;

import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.math.vector.Rotation3f;
import org.joml.Vector3d;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Bridge to MMO Skill Tree ({@code Ziggfreed:MMOSkillTree}). Checked against 1.7.4.
 * Reads claimed skill tree rewards to calculate multipliers and trigger Glutton/Junk systems.
 *
 * <p>Our NUTRITION and THIRST skills are written to MMO's config files by
 * {@link MMOSkillTreeRegistrar} during our setup, before MMO reads them.
 */
public class MMOSkillTreeBridge implements HungerThirstIntegrationBridge {

    private static final String API_CLASS = "com.ziggfreed.mmoskilltree.api.MMOSkillTreeAPI";

    private boolean available = false;

    // --- Core API reflection ---
    private Method addXpMethod;
    private Method getLevelMethod;

    // --- Reward reading reflection ---
    private Method getSkillComponentMethod;
    private Method getClaimedRewardsMethod;
    private Method rewardGetIdMethod;

    // --- Reward IDs of our skill tree (resources/mmo/skill-tree.json) ---
    static final String NUTRITION_KING_FITNESS = "nutrition_king_fitness";
    static final String NUTRITION_MEGA_GLUTTON = "nutrition_mega_glutton";
    static final String NUTRITION_HUNGRY_SUPREME = "nutrition_hungry_supreme";
    static final String THIRST_HYDRATION_MASTER = "thirst_hydration_master";
    /** "Super Healthy" in the tree: -20% running, -20% mining and -15% passive thirst drain. */
    static final String THIRST_SUPER_HEALTHY = "thirst_healthy_lifestyle";
    /** Rewards repeated across tiers (runner_1, runner_2, ...). */
    static final String[] REWARD_PREFIXES = {
        "nutrition_runner_", "nutrition_miner_", "nutrition_fitness_", "nutrition_hungry_",
        "thirst_runner_", "thirst_miner_", "thirst_fitness_", "thirst_rehydrated_"
    };

    // --- Glutton item pool: [choiceId, itemId] ---
    static final String[][] NUTRITION_GLUTTON_ITEMS = {
        {"nutrition_glutton_1", "Food_Wildmeat_Raw"},
        {"nutrition_glutton_2", "Food_Fish_Raw"},
        {"nutrition_glutton_3", "Plant_Fruit_Berries_Red"},
        {"nutrition_glutton_4", "Food_Egg"},
        {"nutrition_glutton_5", "Plant_Fruit_Apple"},
        {"nutrition_glutton_6", "Food_Cheese"},
        {"nutrition_glutton_7", "Food_Salad_Berry"},
        {"nutrition_glutton_8", "Food_Popcorn"},
        {"nutrition_glutton_9", "Food_Pie_Meat"},
        {NUTRITION_MEGA_GLUTTON, "Food_Pie_Meat"}
    };

    // --- Junk system ---
    static final String[] JUNK_ITEMS = {"Ingredient_Bone_Fragment", "Deco_Trash"};
    private static final float BASE_JUNK_CHANCE = 0.65f;
    private static final float MIN_JUNK_CHANCE = 0.25f;

    private final Random random = new Random();
    private boolean rewardReadingAvailable = false;

    // ========================================================================
    // Constructor
    // ========================================================================

    public MMOSkillTreeBridge() {
        PluginIdentifier mmo = OptionalPlugins.MMO_SKILL_TREE;
        try {
            Class<?> apiClass = OptionalPlugins.loadClass(mmo, API_CLASS);

            // addXp only updates the player's existing skill component, so it is safe from a system tick.
            addXpMethod = apiClass.getMethod("addXp", Store.class, Ref.class, String.class, long.class);
            getLevelMethod = apiClass.getMethod("getLevel", Store.class, Ref.class, String.class);
            available = true;

            // Reward reading (optional — if MMO version doesn't have it, multipliers fall back to level-based)
            try {
                getSkillComponentMethod = apiClass.getMethod("getSkillComponent", Store.class, Ref.class);

                // MMO 1.7 moved the skill tree classes from .service / .data to .skilltree.
                Class<?> serviceClass = OptionalPlugins.loadFirstClass(mmo,
                        "com.ziggfreed.mmoskilltree.skilltree.SkillTreeService",
                        "com.ziggfreed.mmoskilltree.service.SkillTreeService");
                Class<?> skillCompClass = OptionalPlugins.loadClass(mmo, "com.ziggfreed.mmoskilltree.data.SkillComponent");
                getClaimedRewardsMethod = serviceClass.getMethod("getClaimedRewardsForSkillByName", skillCompClass, String.class);

                Class<?> rewardClass = OptionalPlugins.loadFirstClass(mmo,
                        "com.ziggfreed.mmoskilltree.skilltree.SkillReward",
                        "com.ziggfreed.mmoskilltree.data.SkillReward");
                rewardGetIdMethod = rewardClass.getMethod("getId");

                rewardReadingAvailable = true;
            } catch (Exception e) {
                rewardReadingAvailable = false;
                AquaThirstHunger.logWarning("[mmo] Skill tree rewards cannot be read from this MMO Skill Tree version: "
                        + "level-based bonuses are used and Glutton items are off. " + e);
            }
        } catch (ClassNotFoundException e) {
            available = false; // MMO Skill Tree is not installed.
        } catch (Exception e) {
            available = false;
            AquaThirstHunger.logWarning("[mmo] Could not connect to MMO Skill Tree: " + e);
        }
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    /** True when claimed skill tree rewards drive the bonuses and the Glutton items. */
    public boolean isRewardReadingAvailable() {
        return rewardReadingAvailable;
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
            if (NUTRITION_KING_FITNESS.equals(id)) reduction += 0.20f;
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
            if (NUTRITION_KING_FITNESS.equals(id)) reduction += 0.20f;
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
            if (NUTRITION_KING_FITNESS.equals(id)) reduction += 0.15f;
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
     * Reduced by thirst_runner_* (-5% each) and Super Healthy (-20%).
     */
    @Override
    public float getThirstStaminaLossMultiplier(Ref<EntityStore> ref, Store<EntityStore> store) {
        if (!rewardReadingAvailable) return 1.0f;
        List<String> claimed = getClaimedRewardIds(ref, store, "THIRST");
        float reduction = 0f;
        for (String id : claimed) {
            if (id.startsWith("thirst_runner_")) reduction += 0.05f;
            if (THIRST_SUPER_HEALTHY.equals(id)) reduction += 0.20f;
        }
        return Math.max(0.1f, 1.0f - reduction);
    }

    /**
     * Mining drain multiplier for THIRST.
     * Reduced by thirst_miner_* (-5% each) and Super Healthy (-20%).
     */
    @Override
    public float getThirstWorkLossMultiplier(Ref<EntityStore> ref, Store<EntityStore> store) {
        if (!rewardReadingAvailable) return 1.0f;
        List<String> claimed = getClaimedRewardIds(ref, store, "THIRST");
        float reduction = 0f;
        for (String id : claimed) {
            if (id.startsWith("thirst_miner_")) reduction += 0.05f;
            if (THIRST_SUPER_HEALTHY.equals(id)) reduction += 0.20f;
        }
        return Math.max(0.1f, 1.0f - reduction);
    }

    /**
     * Passive depletion multiplier for THIRST.
     * Reduced by thirst_fitness_* (-5% each) and Super Healthy (-15%).
     */
    @Override
    public float getThirstMetabolismLossMultiplier(Ref<EntityStore> ref, Store<EntityStore> store) {
        if (!rewardReadingAvailable) return 1.0f;
        List<String> claimed = getClaimedRewardIds(ref, store, "THIRST");
        float reduction = 0f;
        for (String id : claimed) {
            if (id.startsWith("thirst_fitness_")) reduction += 0.05f;
            if (THIRST_SUPER_HEALTHY.equals(id)) reduction += 0.15f;
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
                case THIRST_HYDRATION_MASTER:
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
        int quantity = (claimed.contains(NUTRITION_MEGA_GLUTTON) && !isJunk) ? 2 : 1;

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
     * Player-friendly item name for chat display, from the {@code mmo.item.<ItemId>} lang keys.
     */
    private String getCasualName(String itemId, String lang) {
        String key = "mmo.item." + itemId;
        String name = LangManager.getForLanguage(lang, key);
        return name.equals(key) ? itemId : name;
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
            if (id.startsWith("nutrition_hungry_") && !id.equals(NUTRITION_HUNGRY_SUPREME)) {
                reduction += 0.05f;
            }
            // Hambriento Supremo: -15%
            if (NUTRITION_HUNGRY_SUPREME.equals(id)) {
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

            // Try inventory first (uses the container the player's pickup settings choose)
            ItemStackTransaction tx = Player.giveItem(itemStack, ref, commandBuffer);
            ItemStack remainder = tx != null ? tx.getRemainder() : itemStack;
            if (remainder == null || ItemStack.isEmpty(remainder)) {
                return; // Item delivered to inventory successfully
            }

            // Inventory full or not available — drop the remainder on the ground
            Holder<EntityStore>[] holders = ItemComponent.generateItemDrops(
                    store,
                    Collections.singletonList(remainder),
                    new Vector3d(pos),
                    Rotation3f.ZERO);

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
