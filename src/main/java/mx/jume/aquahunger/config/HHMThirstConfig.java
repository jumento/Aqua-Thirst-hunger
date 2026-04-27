package mx.jume.aquahunger.config;

import com.google.gson.annotations.SerializedName;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import java.util.Optional;

public class HHMThirstConfig {
    public static final BuilderCodec<HHMThirstConfig> CODEC = BuilderCodec
            .builder(HHMThirstConfig.class, HHMThirstConfig::new)
            .append(new KeyedCodec<>("MaxThirst", Codec.FLOAT),
                    ((config, value) -> config.maxThirst = value),
                    HHMThirstConfig::getMaxThirst)
            .add()
            .append(new KeyedCodec<>("BaseThirstDepletion", Codec.FLOAT),
                    ((config, value) -> config.depletionPerTick = value),
                    HHMThirstConfig::getDepletionPerTick)
            .add()
            .append(new KeyedCodec<>("DepletionTickRate", Codec.FLOAT),
                    ((config, value) -> config.depletionTickRate = value),
                    HHMThirstConfig::getDepletionTickRate)
            .add()
            .append(new KeyedCodec<>("DepletionPerBlockHit", Codec.FLOAT),
                    ((config, value) -> config.depletionPerBlockHit = value),
                    HHMThirstConfig::getDepletionPerBlockHit)
            .add()
            .append(new KeyedCodec<>("SprintDepletionModifier", Codec.FLOAT),
                    ((config, value) -> config.sprintDepletionModifier = value),
                    HHMThirstConfig::getSprintDepletionModifier)
            .add()
            .append(new KeyedCodec<>("DehydrationDamage", Codec.FLOAT),
                    ((config, value) -> config.dehydrationDamage = value),
                    HHMThirstConfig::getDehydrationDamage)
            .add()
            .append(new KeyedCodec<>("DamageIntervalSeconds", Codec.FLOAT),
                    ((config, value) -> config.damageIntervalSeconds = value),
                    HHMThirstConfig::getDamageIntervalSeconds)
            .add()
            .append(new KeyedCodec<>("HudPosition", Codec.STRING),
                    ((config, value) -> {
                        config.hudPosition = Optional.ofNullable(HudPosition.valueOf(value))
                                .orElse(HudPosition.pluginDefault());
                    }),
                    ((config) -> config.getHudPosition().name()))
            .add()
            .append(new KeyedCodec<>("ResetThirstOnDeath", Codec.BOOLEAN),
                    ((config, value) -> config.resetThirstOnDeath = value),
                    HHMThirstConfig::isResetThirstOnDeath)
            .add()
            .append(new KeyedCodec<>("RespawnThirstLevel", Codec.FLOAT),
                    ((config, value) -> config.respawnThirstLevel = value),
                    HHMThirstConfig::getRespawnThirstLevel)
            .add()
            .append(new KeyedCodec<>("EnableThirst", Codec.BOOLEAN),
                    ((config, value) -> config.enableThirst = value),
                    HHMThirstConfig::isEnableThirst)
            .add()
            .append(new KeyedCodec<>("ConfigVersion", Codec.STRING),
                    ((config, value) -> config.configVersion = value),
                    (c) -> c.configVersion)
            .add()
            .append(new KeyedCodec<>("ActivePreset", Codec.STRING),
                    ((config, value) -> config.activePreset = value),
                    (c) -> c.activePreset)
            .add()
            .append(new KeyedCodec<>("StaminaBoostThreshold", Codec.FLOAT),
                    ((config, value) -> config.staminaBoostThreshold = value),
                    HHMThirstConfig::getStaminaBoostThreshold)
            .add()
            .append(new KeyedCodec<>("SatiatedThreshold", Codec.FLOAT),
                    ((config, value) -> config.satiatedThreshold = value),
                    HHMThirstConfig::getSatiatedThreshold)
            .add()
            .append(new KeyedCodec<>("StarveResilienceThreshold", Codec.FLOAT),
                    ((config, value) -> config.starveResilienceThreshold = value),
                    HHMThirstConfig::getStarveResilienceThreshold)
            .add()
            .append(new KeyedCodec<>("EnableDebugLogs", Codec.BOOLEAN),
                    ((config, value) -> config.enableDebugLogs = value),
                    HHMThirstConfig::isEnableDebugLogs)
            .add()
            .build();

    @SerializedName("MaxThirst")
    private float maxThirst = 100.0f;
    @SerializedName("DepletionTickRate")
    private float depletionTickRate = 2.0f;
    @SerializedName("BaseThirstDepletion")
    private float depletionPerTick = 0.03f;
    @SerializedName("DepletionPerBlockHit")
    private float depletionPerBlockHit = 0.09f;
    @SerializedName("SprintDepletionModifier")
    private float sprintDepletionModifier = 0.25f;
    @SerializedName("DehydrationDamage")
    private float dehydrationDamage = 0.5f;
    @SerializedName("DamageIntervalSeconds")
    private float damageIntervalSeconds = 10.0f;
    @SerializedName("HudPosition")
    private HudPosition hudPosition = HudPosition.Preset.BelowHotbarRight;
    @SerializedName("ResetThirstOnDeath")
    private boolean resetThirstOnDeath = true;
    @SerializedName("RespawnThirstLevel")
    private float respawnThirstLevel = 50.0f;
    @SerializedName("EnableThirst")
    private boolean enableThirst = true;
    @SerializedName("ConfigVersion")
    private String configVersion = "1.7.5";
    @SerializedName("ActivePreset")
    private String activePreset = "NORMAL";
    @SerializedName("StaminaBoostThreshold")
    private float staminaBoostThreshold = 95.0f;
    @SerializedName("SatiatedThreshold")
    private float satiatedThreshold = 80.0f;
    @SerializedName("StarveResilienceThreshold")
    private float starveResilienceThreshold = 20.0f;
    @SerializedName("EnableDebugLogs")
    private boolean enableDebugLogs = false;

    public float getMaxThirst() {
        return maxThirst;
    }

    public void setMaxThirst(float val) {
        this.maxThirst = val;
    }

    public float getDepletionTickRate() {
        return depletionTickRate;
    }

    public void setDepletionTickRate(float val) {
        this.depletionTickRate = val;
    }

    public float getDepletionPerTick() {
        return depletionPerTick;
    }

    public void setDepletionPerTick(float val) {
        this.depletionPerTick = val;
    }

    public float getDepletionPerBlockHit() {
        return depletionPerBlockHit;
    }

    public void setDepletionPerBlockHit(float val) {
        this.depletionPerBlockHit = val;
    }

    public float getSprintDepletionModifier() {
        return sprintDepletionModifier;
    }

    public void setSprintDepletionModifier(float val) {
        this.sprintDepletionModifier = val;
    }

    public float getDehydrationDamage() {
        return dehydrationDamage;
    }

    public void setDehydrationDamage(float val) {
        this.dehydrationDamage = val;
    }

    public float getDamageIntervalSeconds() {
        return damageIntervalSeconds;
    }

    public void setDamageIntervalSeconds(float val) {
        this.damageIntervalSeconds = val;
    }

    public HudPosition getHudPosition() {
        return hudPosition;
    }

    public void setHudPosition(HudPosition val) {
        this.hudPosition = val;
    }

    public boolean isResetThirstOnDeath() {
        return resetThirstOnDeath;
    }

    public void setResetThirstOnDeath(boolean val) {
        this.resetThirstOnDeath = val;
    }

    public float getRespawnThirstLevel() {
        return respawnThirstLevel;
    }

    public void setRespawnThirstLevel(float val) {
        this.respawnThirstLevel = val;
    }

    public boolean isEnableThirst() {
        return enableThirst;
    }

    public void setEnableThirst(boolean val) {
        this.enableThirst = val;
    }

    public String getConfigVersion() {
        return configVersion;
    }

    public void setConfigVersion(String val) {
        this.configVersion = val;
    }

    public String getActivePreset() {
        return activePreset;
    }

    public void setActivePreset(String val) {
        this.activePreset = val;
    }

    public float getStaminaBoostThreshold() {
        return staminaBoostThreshold;
    }

    public void setStaminaBoostThreshold(float val) {
        this.staminaBoostThreshold = val;
    }

    public float getSatiatedThreshold() {
        return satiatedThreshold;
    }

    public void setSatiatedThreshold(float val) {
        this.satiatedThreshold = val;
    }

    public float getStarveResilienceThreshold() {
        return starveResilienceThreshold;
    }

    public void setStarveResilienceThreshold(float val) {
        this.starveResilienceThreshold = val;
    }

    public boolean isEnableDebugLogs() {
        return enableDebugLogs;
    }

    public void setEnableDebugLogs(boolean val) {
        this.enableDebugLogs = val;
    }

    public void ensureDefaults() {
        ConfigMigrationManager.migrate(this);
    }

    public void applyPreset(String preset, HHMThirstFoodValuesConfig foodConfig) {
        switch (preset.toUpperCase()) {
            case "EASY":
                this.setMaxThirst(100.0f);
                this.setEnableThirst(true);
                this.setResetThirstOnDeath(false);
                this.setRespawnThirstLevel(100.0f);
                this.setDepletionTickRate(10.0f);
                this.setDepletionPerTick(0.02f);
                this.setDepletionPerBlockHit(0.01f);
                this.setSprintDepletionModifier(0.05f);
                this.setDehydrationDamage(0.3f);
                this.setDamageIntervalSeconds(10.0f);
                this.setStaminaBoostThreshold(70.0f);

                if (foodConfig != null) {
                    foodConfig.setFruitMultiplier(7.0f);
                    foodConfig.setItemThirstRestoration("AquaThirstHunger_Canteen", 15.0f);
                    foodConfig.setItemThirstRestoration("AquaThirstHunger_Canteenpro_Empty", 15.0f);
                    foodConfig.setItemThirstRestoration("AquaThirstHunger_BasicBottle_Empty", 15.0f);
                }
                this.setActivePreset("EASY");
                break;
            case "NORMAL":
                this.setMaxThirst(100.0f);
                this.setEnableThirst(true);
                this.setResetThirstOnDeath(true);
                this.setRespawnThirstLevel(50.0f);
                this.setDepletionTickRate(2.0f);
                this.setDepletionPerTick(0.03f);
                this.setDepletionPerBlockHit(0.10f);
                this.setSprintDepletionModifier(0.25f);
                this.setDehydrationDamage(1.5f);
                this.setDamageIntervalSeconds(2f);
                this.setStaminaBoostThreshold(95.0f);

                if (foodConfig != null) {
                    foodConfig.setFruitMultiplier(4.5f);
                    foodConfig.setItemThirstRestoration("AquaThirstHunger_Canteen", 14.0f);
                    foodConfig.setItemThirstRestoration("AquaThirstHunger_Canteenpro_Empty", 14.0f);
                    foodConfig.setItemThirstRestoration("AquaThirstHunger_BasicBottle_Empty", 14.0f);
                }
                this.setActivePreset("NORMAL");
                break;
            case "HARD":
                this.setMaxThirst(100.0f);
                this.setEnableThirst(true);
                this.setResetThirstOnDeath(true);
                this.setRespawnThirstLevel(30.0f);
                this.setDepletionTickRate(1f);
                this.setDepletionPerTick(0.05f);
                this.setDepletionPerBlockHit(0.15f);
                this.setSprintDepletionModifier(0.25f);
                this.setDehydrationDamage(1f);
                this.setDamageIntervalSeconds(1f);
                this.setStaminaBoostThreshold(100.0f);

                if (foodConfig != null) {
                    foodConfig.setFruitMultiplier(3.5f);
                    foodConfig.setItemThirstRestoration("AquaThirstHunger_Canteen", 8.0f);
                    foodConfig.setItemThirstRestoration("AquaThirstHunger_Canteenpro_Empty", 8.0f);
                    foodConfig.setItemThirstRestoration("AquaThirstHunger_BasicBottle_Empty", 8.0f);
                }
                this.setActivePreset("HARD");
                break;
        }
    }
}
