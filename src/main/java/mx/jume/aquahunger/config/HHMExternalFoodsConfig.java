package mx.jume.aquahunger.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * HHMExternalFoodsConfig - Manages external food items with two-tier system:
 * - ExternalFoodsDefaultConfig.json: Mod defaults (always overwritten on
 * update)
 * - ExternalFoodsConfig.json: User overrides (never touched, highest priority)
 */
public class HHMExternalFoodsConfig {
    private static final Logger LOGGER = Logger.getLogger("AquaThirstHunger");
    private final File defaultConfigFile;
    private final File userConfigFile;
    private final Gson gson;

    private String configVersion = "1.5.0";
    private Map<String, ExternalFoodEntry> defaultEntries = new HashMap<>();
    private Map<String, ExternalFoodEntry> userEntries = new HashMap<>();
    private Map<String, ExternalFoodEntry> mergedEntries = new HashMap<>();

    public static class ExternalFoodEntry {
        public float hungerRestoration;
        public float maxHungerSaturation;
        public float thirstRestoration;

        public ExternalFoodEntry() {
        }

        public ExternalFoodEntry(float hungerRestoration, float maxHungerSaturation, float thirstRestoration) {
            this.hungerRestoration = hungerRestoration;
            this.maxHungerSaturation = maxHungerSaturation;
            this.thirstRestoration = thirstRestoration;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            ExternalFoodEntry that = (ExternalFoodEntry) o;
            return Float.compare(that.hungerRestoration, hungerRestoration) == 0 &&
                    Float.compare(that.maxHungerSaturation, maxHungerSaturation) == 0 &&
                    Float.compare(that.thirstRestoration, thirstRestoration) == 0;
        }
    }

    public HHMExternalFoodsConfig(File configDir) {
        this.defaultConfigFile = new File(configDir, "ExternalFoodsDefaultConfig.json");
        this.userConfigFile = new File(configDir, "ExternalFoodsConfig.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        load();
    }

    private void load() {
        // Always overwrite defaults file from bundled resource (only affects
        // ExternalFoodsDefaultConfig.json)
        overwriteDefaultsFromResource();

        defaultEntries.clear();
        userEntries.clear();

        // Load defaults first
        loadFile(defaultConfigFile, defaultEntries, "default");
        // Load user overrides second
        loadFile(userConfigFile, userEntries, "user");
        // Merge: defaults + user (user wins on conflicts)
        rebuildMerged();
    }

    private void loadFile(File file, Map<String, ExternalFoodEntry> targetMap, String type) {
        if (!file.exists()) {
            LOGGER.info("ExternalFoods " + type + " config not found: " + file.getName());
            return;
        }

        try (FileReader reader = new FileReader(file)) {
            Type mapType = new TypeToken<Map<String, Object>>() {
            }.getType();
            Map<String, Object> raw = gson.fromJson(reader, mapType);
            if (raw != null) {
                int count = 0;
                for (Map.Entry<String, Object> entry : raw.entrySet()) {
                    String key = entry.getKey();
                    if (key.equals("__ConfigVersion") || key.equals("ConfigVersion")) {
                        if (type.equals("default")) {
                            this.configVersion = String.valueOf(entry.getValue());
                        }
                        continue;
                    }
                    if (key.startsWith("__"))
                        continue;

                    try {
                        String json = gson.toJson(entry.getValue());
                        ExternalFoodEntry food = gson.fromJson(json, ExternalFoodEntry.class);
                        if (food != null) {
                            targetMap.put(key, food);
                            count++;
                        }
                    } catch (Exception e) {
                        LOGGER.warning("Skipping invalid food entry in " + type + " config: " + key);
                    }
                }
                LOGGER.info("Loaded " + count + " items from " + file.getName()
                        + (type.equals("default") ? " (Version: " + configVersion + ")" : " (User overrides)"));
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load " + file.getName(), e);
        }
    }

    private void rebuildMerged() {
        mergedEntries.clear();
        // Start with defaults
        mergedEntries.putAll(defaultEntries);
        // User entries override defaults
        mergedEntries.putAll(userEntries);
        LOGGER.info("Merged configuration: " + mergedEntries.size() + " total items ("
                + defaultEntries.size() + " defaults + " + userEntries.size() + " user overrides)");
    }

    public void saveDefaults() {
        // Defaults are now always copied from resources and should not be saved from
        // memory
        // to avoid losing ordering or comments if we ever add them.
        // saveFile(defaultConfigFile, defaultEntries, true);
    }

    public void saveUser() {
        saveFile(userConfigFile, userEntries, false);
    }

    private void saveFile(File file, Map<String, ExternalFoodEntry> entries, boolean includeVersion) {
        try {
            if (file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }

            Map<String, Object> out = new HashMap<>();
            if (includeVersion) {
                out.put("__ConfigVersion", configVersion);
            }
            out.putAll(entries);

            try (FileWriter writer = new FileWriter(file)) {
                gson.toJson(out, writer);
            }
            LOGGER.info("Saved " + entries.size() + " items to " + file.getName());
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to save " + file.getName(), e);
        }
    }

    public void ensureDefaults() {
        // Passing outside migration system as requested
        // ConfigMigrationManager.migrate(this);
    }

    public ExternalFoodEntry resolve(String itemId) {
        if (itemId == null)
            return null;

        // Check exact match in merged entries
        if (mergedEntries.containsKey(itemId))
            return mergedEntries.get(itemId);

        // Check wildcard matches
        for (Map.Entry<String, ExternalFoodEntry> entry : mergedEntries.entrySet()) {
            String key = entry.getKey();
            if (key.endsWith("*")) {
                String prefix = key.substring(0, key.length() - 1);
                if (itemId.startsWith(prefix)) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    public void updateItem(String id, float hunger, float saturation, float thirst) {
        // User updates always go to user config
        userEntries.put(id, new ExternalFoodEntry(hunger, saturation, thirst));
        rebuildMerged();
    }

    public Map<String, ExternalFoodEntry> getEntries() {
        return mergedEntries;
    }

    public Map<String, ExternalFoodEntry> getDefaultEntries() {
        return defaultEntries;
    }

    public Map<String, ExternalFoodEntry> getUserEntries() {
        return userEntries;
    }

    public void setDefaultEntries(Map<String, ExternalFoodEntry> entries) {
        this.defaultEntries = entries;
        rebuildMerged();
    }

    public String getConfigVersion() {
        return configVersion;
    }

    public void setConfigVersion(String configVersion) {
        this.configVersion = configVersion;
    }

    private void overwriteDefaultsFromResource() {
        // The source is now bundled at the root of the JAR (pulled from mods folder
        // during build)
        String resourcePath = "/ExternalFoodsDefaultConfig.json";
        try {
            if (defaultConfigFile.getParentFile() != null) {
                defaultConfigFile.getParentFile().mkdirs();
            }

            // Get resource from JAR root
            try (java.io.InputStream in = HHMExternalFoodsConfig.class.getResourceAsStream(resourcePath)) {
                if (in == null) {
                    // Try without leading slash
                    try (java.io.InputStream in2 = HHMExternalFoodsConfig.class.getClassLoader()
                            .getResourceAsStream("ExternalFoodsDefaultConfig.json")) {
                        if (in2 == null) {
                            LOGGER.severe("CRITICAL: Default config not found inside the mod JAR!");
                            return;
                        }
                        copyAndLog(in2);
                    }
                } else {
                    copyAndLog(in);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to mandatory overwrite " + defaultConfigFile.getName(), e);
        }
    }

    private void copyAndLog(java.io.InputStream in) throws IOException {
        // Absolute overwrite: Delete first
        if (defaultConfigFile.exists()) {
            defaultConfigFile.delete();
        }
        java.nio.file.Files.copy(in, defaultConfigFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        LOGGER.info("Successfully refreshed " + defaultConfigFile.getName() + " from internal mod data.");
    }

}
