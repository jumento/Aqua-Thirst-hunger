package mx.jume.aquahunger.config;

import com.google.gson.annotations.SerializedName;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.EnumMapCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class HHMThirstFoodValuesConfig {
    public static final BuilderCodec<HHMThirstFoodValuesConfig> CODEC = BuilderCodec
            .builder(HHMThirstFoodValuesConfig.class, HHMThirstFoodValuesConfig::new)
            .append(new KeyedCodec<>("ResultResourceTypeId", Codec.STRING),
                    ((config, value) -> config.fruitResourceTypeId = value),
                    HHMThirstFoodValuesConfig::getFruitResourceTypeId)
            .add()
            .append(new KeyedCodec<>("FruitMultiplier", Codec.FLOAT),
                    ((config, value) -> config.fruitMultiplier = value),
                    HHMThirstFoodValuesConfig::getFruitMultiplier)
            .add()
            .append(new KeyedCodec<>("TierThirstRestoration",
                    new EnumMapCodec<>(ItemTier.class, Codec.FLOAT)),
                    ((config, value) -> config.tierThirstRestoration.putAll(value)),
                    (c) -> c.tierThirstRestoration)
            .add()
            .append(new KeyedCodec<>("ItemThirstRestoration", new MapCodec<>(Codec.FLOAT, HashMap::new)),
                    ((config, value) -> config.itemThirstRestoration = value),
                    (c) -> c.itemThirstRestoration)
            .add()
            .append(new KeyedCodec<>("ConfigVersion", Codec.STRING),
                    ((config, value) -> config.configVersion = value),
                    (c) -> c.configVersion)
            .add()
            .build();

    @SerializedName("ResultResourceTypeId")
    private String fruitResourceTypeId = "Fruits";
    @SerializedName("FruitMultiplier")
    private float fruitMultiplier = 3.5f;
    @SerializedName("ConfigVersion")
    private String configVersion = "1.5.0";
    @SerializedName("TierThirstRestoration")
    private final Map<ItemTier, Float> tierThirstRestoration = new EnumMap<>(ItemTier.class);
    @SerializedName("ItemThirstRestoration")
    private Map<String, Float> itemThirstRestoration = new HashMap<>();

    public HHMThirstFoodValuesConfig() {
        tierThirstRestoration.put(ItemTier.Common, 2.5f);
        tierThirstRestoration.put(ItemTier.Uncommon, 3.0f);
        tierThirstRestoration.put(ItemTier.Rare, 3.5f);
        tierThirstRestoration.put(ItemTier.Epic, 4.0f);
        tierThirstRestoration.put(ItemTier.Legendary, 4.5f);
        tierThirstRestoration.put(ItemTier.Mythic, 5.0f);
        tierThirstRestoration.put(ItemTier.Unique, 5.5f);
        itemThirstRestoration.put("AquaThirstHunger_Canteen", 14.0f);
        itemThirstRestoration.put("AquaThirstHunger_Canteenpro_Empty", 16.0f);
        itemThirstRestoration.put("AquaThirstHunger_BasicBottle_Empty", 16.0f);
    }

    public String getFruitResourceTypeId() {
        return fruitResourceTypeId;
    }

    public float getFruitMultiplier() {
        return fruitMultiplier;
    }

    public Float getItemThirstRestoration(@NonNullDecl String itemId) {
        if (itemId == null)
            return null;

        // Super Clean: Handle *, namespaces, and various state separators
        String cleanId = itemId;
        if (cleanId.startsWith("*"))
            cleanId = cleanId.substring(1);
        if (cleanId.contains(":"))
            cleanId = cleanId.substring(cleanId.indexOf(":") + 1);

        // Cut off all known state/variant separators
        int stateIdx = cleanId.indexOf("_State_");
        if (stateIdx != -1)
            cleanId = cleanId.substring(0, stateIdx);
        int variantIdx = cleanId.indexOf("_Variant_");
        if (variantIdx != -1)
            cleanId = cleanId.substring(0, variantIdx);
        int bracketIdx = cleanId.indexOf("[");
        if (bracketIdx != -1)
            cleanId = cleanId.substring(0, bracketIdx);

        // 1. Exact matches for both forms
        if (itemThirstRestoration.containsKey(itemId))
            return itemThirstRestoration.get(itemId);
        if (itemThirstRestoration.containsKey(cleanId))
            return itemThirstRestoration.get(cleanId);

        // 2. Best prefix match (prioritize longest key)
        String bestKey = null;
        for (String key : itemThirstRestoration.keySet()) {
            if (itemId.startsWith(key) || cleanId.startsWith(key)) {
                if (bestKey == null || key.length() > bestKey.length()) {
                    bestKey = key;
                }
            }
        }

        return (bestKey != null) ? itemThirstRestoration.get(bestKey) : null;
    }

    public float getTierThirstRestoration(@NonNullDecl ItemTier tier) {
        return tierThirstRestoration.getOrDefault(tier, 0.0f);
    }

    public String getConfigVersion() {
        return configVersion;
    }

    public void setConfigVersion(String val) {
        this.configVersion = val;
    }

    public void setFruitMultiplier(float val) {
        this.fruitMultiplier = val;
    }

    public Map<ItemTier, Float> getTierThirstRestoration() {
        return tierThirstRestoration;
    }

    public Map<String, Float> getItemThirstRestorationMap() {
        return itemThirstRestoration;
    }

    public void setItemThirstRestoration(String itemId, float value) {
        this.itemThirstRestoration.put(itemId, value);
    }

    public void ensureDefaults() {
        ConfigMigrationManager.migrate(this);
    }
}
