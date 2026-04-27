package mx.jume.aquahunger.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

public class ConfigMigrationManager {
        private static final Logger LOGGER = Logger.getLogger("AquaThirstHunger");
        public static final String LATEST_VERSION = "1.7.5";

        public static void migrate(HHMHungerConfig config) {
                String currentVersion = config.getConfigVersion();
                if (LATEST_VERSION.equals(currentVersion))
                        return;

                LOGGER.info(
                                "[Migration] Detected HungerConfig version: "
                                                + (currentVersion == null ? "legacy" : currentVersion));
                backup("HungerConfig.json", currentVersion);

                if (isLegacy(currentVersion)) {
                        Map<String, Object> oldDefaults = getHungerDefaults("1.0.0");
                        Map<String, Object> newDefaults = getHungerDefaults(LATEST_VERSION);

                        config.setInitialHungerLevel(
                                        merge(config.getInitialHungerLevel(), oldDefaults.get("InitialHungerLevel"),
                                                        newDefaults.get("InitialHungerLevel"), "InitialHungerLevel"));
                        config.setMaxHungerLevel(merge(config.getMaxHungerLevel(), oldDefaults.get("MaxHungerLevel"),
                                        newDefaults.get("MaxHungerLevel"), "MaxHungerLevel"));
                        config.setRespawnHungerLevel(
                                        merge(config.getRespawnHungerLevel(), oldDefaults.get("RespawnHungerLevel"),
                                                        newDefaults.get("RespawnHungerLevel"), "RespawnHungerLevel"));
                        config.setResetHungerOnDeath(
                                        merge(config.isResetHungerOnDeath(), oldDefaults.get("ResetHungerOnDeath"),
                                                        newDefaults.get("ResetHungerOnDeath"), "ResetHungerOnDeath"));
                        config.setStarvationTickRate(
                                        merge(config.getStarvationTickRate(), oldDefaults.get("StarvationTickRate"),
                                                        newDefaults.get("StarvationTickRate"), "StarvationTickRate"));
                        config.setStarvationPerTick(
                                        merge(config.getStarvationPerTick(), oldDefaults.get("StarvationPerTick"),
                                                        newDefaults.get("StarvationPerTick"), "StarvationPerTick"));
                        config.setStarvationPerBlockHit(
                                        merge(config.getStarvationPerBlockHit(),
                                                        oldDefaults.get("StarvationPerBlockHit"),
                                                        newDefaults.get("StarvationPerBlockHit"),
                                                        "StarvationPerBlockHit"));
                        config.setStarvationStaminaModifier(
                                        merge(config.getStarvationStaminaModifier(),
                                                        oldDefaults.get("StarvationStaminaModifier"),
                                                        newDefaults.get("StarvationStaminaModifier"),
                                                        "StarvationStaminaModifier"));
                        config.setHungryThreshold(merge(config.getHungryThreshold(), oldDefaults.get("HungryThreshold"),
                                        newDefaults.get("HungryThreshold"), "HungryThreshold"));
                        config.setStarvationDamage(
                                        merge(config.getStarvationDamage(), oldDefaults.get("StarvationDamage"),
                                                        newDefaults.get("StarvationDamage"), "StarvationDamage"));
                        config.setSinglePlayer(merge(config.isSinglePlayer(), oldDefaults.get("SinglePlayer"),
                                        newDefaults.get("SinglePlayer"), "SinglePlayer"));
                        config.setEnableHunger(merge(config.isEnableHunger(), oldDefaults.get("EnableHunger"),
                                        newDefaults.get("EnableHunger"), "EnableHunger"));
                        config.setLifePerHunger(merge(config.isLifePerHunger(), oldDefaults.get("LifePerHunger"),
                                        newDefaults.get("LifePerHunger"), "LifePerHunger"));
                        config.setPulseInterval(merge(config.getPulseInterval(), oldDefaults.get("PulseInterval"),
                                        newDefaults.get("PulseInterval"), "PulseInterval"));
                        config.setHealthPerPulse(merge(config.getHealthPerPulse(), oldDefaults.get("HealthPerPulse"),
                                        newDefaults.get("HealthPerPulse"), "HealthPerPulse"));
                        config.setHungerCost(merge(config.getHungerCost(), oldDefaults.get("HungerCost"),
                                        newDefaults.get("HungerCost"), "HungerCost"));
                        config.setThirstCost(merge(config.getThirstCost(), oldDefaults.get("ThirstCost"),
                                        newDefaults.get("ThirstCost"), "ThirstCost"));
                }

                config.setConfigVersion(LATEST_VERSION);
                LOGGER.info("[Migration] HungerConfig updated to " + LATEST_VERSION);
        }

        public static void migrate(HHMThirstConfig config) {
                String currentVersion = config.getConfigVersion();
                if (LATEST_VERSION.equals(currentVersion))
                        return;

                LOGGER.info(
                                "[Migration] Detected ThirstConfig version: "
                                                + (currentVersion == null ? "legacy" : currentVersion));
                backup("ThirstConfig.json", currentVersion);

                if (isLegacy(currentVersion)) {
                        Map<String, Object> oldDefaults = getThirstDefaults("1.0.0");
                        Map<String, Object> newDefaults = getThirstDefaults(LATEST_VERSION);

                        config.setMaxThirst(merge(config.getMaxThirst(), oldDefaults.get("MaxThirst"),
                                        newDefaults.get("MaxThirst"),
                                        "MaxThirst"));
                        config.setRespawnThirstLevel(
                                        merge(config.getRespawnThirstLevel(), oldDefaults.get("RespawnThirstLevel"),
                                                        newDefaults.get("RespawnThirstLevel"), "RespawnThirstLevel"));
                        config.setResetThirstOnDeath(
                                        merge(config.isResetThirstOnDeath(), oldDefaults.get("ResetThirstOnDeath"),
                                                        newDefaults.get("ResetThirstOnDeath"), "ResetThirstOnDeath"));
                        config.setDepletionTickRate(
                                        merge(config.getDepletionTickRate(), oldDefaults.get("DepletionTickRate"),
                                                        newDefaults.get("DepletionTickRate"), "DepletionTickRate"));
                        config.setDepletionPerTick(
                                        merge(config.getDepletionPerTick(), oldDefaults.get("BaseThirstDepletion"),
                                                        newDefaults.get("BaseThirstDepletion"), "BaseThirstDepletion"));
                        config.setDepletionPerBlockHit(
                                        merge(config.getDepletionPerBlockHit(), oldDefaults.get("DepletionPerBlockHit"),
                                                        newDefaults.get("DepletionPerBlockHit"),
                                                        "DepletionPerBlockHit"));
                        config.setSprintDepletionModifier(
                                        merge(config.getSprintDepletionModifier(),
                                                        oldDefaults.get("SprintDepletionModifier"),
                                                        newDefaults.get("SprintDepletionModifier"),
                                                        "SprintDepletionModifier"));
                        config.setDehydrationDamage(
                                        merge(config.getDehydrationDamage(), oldDefaults.get("DehydrationDamage"),
                                                        newDefaults.get("DehydrationDamage"), "DehydrationDamage"));
                        config.setDamageIntervalSeconds(
                                        merge(config.getDamageIntervalSeconds(),
                                                        oldDefaults.get("DamageIntervalSeconds"),
                                                        newDefaults.get("DamageIntervalSeconds"),
                                                        "DamageIntervalSeconds"));
                        config.setEnableThirst(merge(config.isEnableThirst(), oldDefaults.get("EnableThirst"),
                                        newDefaults.get("EnableThirst"), "EnableThirst"));
                        config.setStaminaBoostThreshold(
                                        merge(config.getStaminaBoostThreshold(),
                                                        oldDefaults.get("StaminaBoostThreshold"),
                                                        newDefaults.get("StaminaBoostThreshold"),
                                                        "StaminaBoostThreshold"));
                        config.setEnableDebugLogs(
                                        merge(config.isEnableDebugLogs(),
                                                        oldDefaults.get("EnableDebugLogs"),
                                                        newDefaults.get("EnableDebugLogs"), "EnableDebugLogs"));
                }

                config.setConfigVersion(LATEST_VERSION);
                LOGGER.info("[Migration] ThirstConfig updated to " + LATEST_VERSION);
        }

        public static void migrate(HHMFoodValuesConfig config) {
                String currentVersion = config.getConfigVersion();
                if (LATEST_VERSION.equals(currentVersion))
                        return;

                LOGGER.info("[Migration] Detected FoodValuesConfig version: "
                                + (currentVersion == null ? "legacy" : currentVersion));
                backup("FoodValuesConfig.json", currentVersion);
                config.setConfigVersion(LATEST_VERSION);
        }

        public static void migrate(HHMThirstFoodValuesConfig config) {
                String currentVersion = config.getConfigVersion();
                if (LATEST_VERSION.equals(currentVersion))
                        return;

                backup("ThirstFoodValuesConfig.json", currentVersion);
                if (isLegacy(currentVersion) || currentVersion.equals("1.3.0") || currentVersion.equals("1.5.0")) {
                        Map<String, Object> oldDefaults = getThirstFoodValuesDefaults(
                                        currentVersion.equals("1.3.0") ? "1.3.0" : "1.5.0");
                        Map<String, Object> newDefaults = getThirstFoodValuesDefaults(LATEST_VERSION);
                        config.setFruitMultiplier(merge(config.getFruitMultiplier(), oldDefaults.get("FruitMultiplier"),
                                        newDefaults.get("FruitMultiplier"), "FruitMultiplier"));

                        // Sync Tiers as well if they were at 1.3.0 defaults
                        Map<ItemTier, Float> oldTiers = (Map<ItemTier, Float>) oldDefaults.get("TierThirstRestoration");
                        Map<ItemTier, Float> newTiers = (Map<ItemTier, Float>) newDefaults.get("TierThirstRestoration");
                        for (ItemTier tier : ItemTier.values()) {
                                float current = config.getTierThirstRestoration().getOrDefault(tier, 0f);
                                config.getTierThirstRestoration().put(tier,
                                                merge(current, oldTiers.get(tier), newTiers.get(tier),
                                                                "TierThirstRestoration." + tier));
                        }
                }

                config.setConfigVersion(LATEST_VERSION);
        }

        // Removed migrate(HHMExternalFoodsConfig) as it now passes outside the
        // migration system.

        // Removed getExternalFoodsDefaults as it's no longer used.

        private static boolean isLegacy(String version) {
                return version == null || version.isEmpty() || version.equals("1.0.0") || version.equals("1.2")
                                || version.equals("1.0") || version.equals("1.5.0");
        }

        @SuppressWarnings("unchecked")
        private static <T> T merge(T currentVal, Object oldDefault, Object newDefault, String keyName) {
                if (oldDefault == null)
                        return (T) newDefault;
                if (areEqual(currentVal, oldDefault)) {
                        return (T) newDefault;
                }
                LOGGER.info("[Migration] Preserved user override for key: " + keyName + " (Current: " + currentVal
                                + ")");
                return currentVal;
        }

        private static boolean areEqual(Object a, Object b) {
                if (a instanceof Number && b instanceof Number) {
                        return Math.abs(((Number) a).doubleValue() - ((Number) b).doubleValue()) < 0.0001;
                }
                return Objects.equals(a, b);
        }

        private static void backup(String fileName, String version) {
                File dir = new File("mods/Aqua-Thirst-hunger");
                if (!dir.exists())
                        dir.mkdirs();
                File file = new File(dir, fileName);
                if (!file.exists())
                        return;
                String safeVer = (version == null || version.isEmpty()) ? "legacy" : version;
                File backup = new File(file.getParent(), fileName + ".v" + safeVer + ".bak");
                try {
                        Files.copy(file.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        LOGGER.info("[Migration] Created backup: " + backup.getName());
                } catch (IOException e) {
                        LOGGER.warning("[Migration] Backup failed for " + fileName + ": " + e.getMessage());
                }
        }

        private static Map<String, Object> getHungerDefaults(String version) {
                Map<String, Object> d = new HashMap<>();
                d.put("InitialHungerLevel", 100.0f);
                d.put("MaxHungerLevel", 200.0f);
                d.put("RespawnHungerLevel", 50.0f);
                d.put("ResetHungerOnDeath", true);
                d.put("StarvationTickRate", 1.5f);
                d.put("StarvationPerTick", 0.01f);
                d.put("StarvationPerBlockHit", 0.03f);
                d.put("StarvationStaminaModifier", 0.8f);
                d.put("HungryThreshold", 20.0f);
                d.put("StarvationDamage", 0.5f);
                d.put("SinglePlayer", true);
                d.put("EnableHunger", true);
                d.put("LifePerHunger", true);
                d.put("PulseInterval", 1.5f);
                d.put("HealthPerPulse", 2.0f);
                d.put("HungerCost", 0.25f);
                d.put("ThirstCost", 0.35f);
                return d;
        }

        private static Map<String, Object> getThirstDefaults(String version) {
                Map<String, Object> d = new HashMap<>();
                d.put("MaxThirst", 100.0f);
                d.put("RespawnThirstLevel", 50.0f);
                d.put("ResetThirstOnDeath", true);
                d.put("DepletionTickRate", 2.0f);
                d.put("BaseThirstDepletion", 0.03f);
                d.put("DepletionPerBlockHit", 0.10f);
                d.put("SprintDepletionModifier", 0.25f);
                d.put("DehydrationDamage", 1.5f);
                d.put("DamageIntervalSeconds", 2.0f);
                d.put("EnableThirst", true);
                d.put("StaminaBoostThreshold", 95.0f);
                d.put("EnableDebugLogs", false);
                return d;
        }

        private static Map<String, Object> getThirstFoodValuesDefaults(String version) {
                Map<String, Object> d = new HashMap<>();
                if (version.equals("1.3.0")) {
                        d.put("FruitMultiplier", 2.0f);
                        Map<ItemTier, Float> tiers = new HashMap<>();
                        tiers.put(ItemTier.Common, 0.5f);
                        tiers.put(ItemTier.Uncommon, 1.0f);
                        tiers.put(ItemTier.Rare, 1.5f);
                        tiers.put(ItemTier.Epic, 2.0f);
                        tiers.put(ItemTier.Legendary, 2.5f);
                        tiers.put(ItemTier.Mythic, 3.0f);
                        tiers.put(ItemTier.Unique, 4.0f);
                        d.put("TierThirstRestoration", tiers);
                } else {
                        // LATEST (1.6.1)
                        d.put("FruitMultiplier", 4.5f);
                        Map<ItemTier, Float> tiers = new HashMap<>();
                        tiers.put(ItemTier.Common, 2.5f);
                        tiers.put(ItemTier.Uncommon, 3.0f);
                        tiers.put(ItemTier.Rare, 3.5f);
                        tiers.put(ItemTier.Epic, 4.0f);
                        tiers.put(ItemTier.Legendary, 4.5f);
                        tiers.put(ItemTier.Mythic, 5.0f);
                        tiers.put(ItemTier.Unique, 5.5f);
                        d.put("TierThirstRestoration", tiers);
                }
                return d;
        }
}
