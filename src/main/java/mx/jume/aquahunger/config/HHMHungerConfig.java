package mx.jume.aquahunger.config;

import com.google.gson.annotations.SerializedName;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import java.util.Optional;

public class HHMHungerConfig {
    public static final BuilderCodec<HHMHungerConfig> CODEC = BuilderCodec
            .builder(HHMHungerConfig.class, HHMHungerConfig::new)
            .append(new KeyedCodec<>("InitialHungerLevel", Codec.FLOAT),
                    ((config, value) -> config.initialHungerLevel = value),
                    HHMHungerConfig::getInitialHungerLevel)
            .add()
            .append(new KeyedCodec<>("MaxHungerLevel", Codec.FLOAT),
                    ((config, value) -> config.maxHungerLevel = value),
                    HHMHungerConfig::getMaxHungerLevel)
            .add()
            .append(new KeyedCodec<>("RespawnHungerLevel", Codec.FLOAT),
                    ((config, value) -> config.respawnHungerLevel = value),
                    HHMHungerConfig::getRespawnHungerLevel)
            .add()
            .append(new KeyedCodec<>("ResetHungerOnDeath", Codec.BOOLEAN),
                    ((config, value) -> config.resetHungerOnDeath = value),
                    HHMHungerConfig::isResetHungerOnDeath)
            .add()
            .append(new KeyedCodec<>("StarvationTickRate", Codec.FLOAT),
                    ((config, value) -> config.starvationTickRate = value),
                    HHMHungerConfig::getStarvationTickRate)
            .add()
            .append(new KeyedCodec<>("StarvationPerTick", Codec.FLOAT),
                    ((config, value) -> config.starvationPerTick = value),
                    HHMHungerConfig::getStarvationPerTick)
            .add()
            .append(new KeyedCodec<>("StarvationPerBlockHit", Codec.FLOAT),
                    ((config, value) -> config.starvationPerBlockHit = value),
                    HHMHungerConfig::getStarvationPerBlockHit)
            .add()
            .append(new KeyedCodec<>("StarvationStaminaModifier", Codec.FLOAT),
                    ((config, value) -> config.starvationStaminaModifier = value),
                    HHMHungerConfig::getStarvationStaminaModifier)
            .add()
            .append(new KeyedCodec<>("HungryThreshold", Codec.FLOAT),
                    ((config, value) -> config.hungryThreshold = value),
                    HHMHungerConfig::getHungryThreshold)
            .add()
            .append(new KeyedCodec<>("StarvationDamage", Codec.FLOAT),
                    ((config, value) -> config.starvationDamage = value),
                    HHMHungerConfig::getStarvationDamage)
            .add()
            .append(new KeyedCodec<>("HudPosition", Codec.STRING),
                    ((config, value) -> {
                        config.hudPosition = Optional.ofNullable(HudPosition.valueOf(value))
                                .orElse(HudPosition.pluginDefault());
                    }),
                    ((config) -> config.getHudPosition().name()))
            .add()
            .append(new KeyedCodec<>("SinglePlayer", Codec.BOOLEAN),
                    ((config, value) -> config.singlePlayer = value),
                    (config) -> config.singlePlayer)
            .add()
            .append(new KeyedCodec<>("EnableHunger", Codec.BOOLEAN),
                    ((config, value) -> config.enableHunger = value),
                    HHMHungerConfig::isEnableHunger)
            .add()
            .append(new KeyedCodec<>("LifePerHunger", Codec.BOOLEAN),
                    ((config, value) -> config.lifePerHunger = value),
                    HHMHungerConfig::isLifePerHunger)
            .add()
            .append(new KeyedCodec<>("PulseInterval", Codec.FLOAT),
                    ((config, value) -> config.pulseInterval = value),
                    HHMHungerConfig::getPulseInterval)
            .add()
            .append(new KeyedCodec<>("HealthPerPulse", Codec.FLOAT),
                    ((config, value) -> config.healthPerPulse = value),
                    HHMHungerConfig::getHealthPerPulse)
            .add()
            .append(new KeyedCodec<>("HungerCost", Codec.FLOAT),
                    ((config, value) -> config.hungerCost = value),
                    HHMHungerConfig::getHungerCost)
            .add()
            .append(new KeyedCodec<>("ThirstCost", Codec.FLOAT),
                    ((config, value) -> config.thirstCost = value),
                    HHMHungerConfig::getThirstCost)
            .add()
            .append(new KeyedCodec<>("ConfigVersion", Codec.STRING),
                    ((config, value) -> config.configVersion = value),
                    (c) -> c.configVersion)
            .add()
            .append(new KeyedCodec<>("ActivePreset", Codec.STRING),
                    ((config, value) -> config.activePreset = value),
                    (c) -> c.activePreset)
            .add()
            .append(new KeyedCodec<>("SatiatedThreshold", Codec.FLOAT),
                    ((config, value) -> config.satiatedThreshold = value),
                    HHMHungerConfig::getSatiatedThreshold)
            .add()
            .append(new KeyedCodec<>("StarveResilienceThreshold", Codec.FLOAT),
                    ((config, value) -> config.starveResilienceThreshold = value),
                    HHMHungerConfig::getStarveResilienceThreshold)
            .add()
            .build();

    @SerializedName("InitialHungerLevel")
    private float initialHungerLevel = 100.0f;
    @SerializedName("MaxHungerLevel")
    private float maxHungerLevel = 200.0f;
    @SerializedName("RespawnHungerLevel")
    private float respawnHungerLevel = 50.0f;
    @SerializedName("ResetHungerOnDeath")
    private boolean resetHungerOnDeath = true;
    @SerializedName("StarvationTickRate")
    private float starvationTickRate = 2.0f;
    @SerializedName("StarvationPerTick")
    private float starvationPerTick = 0.01f;
    @SerializedName("StarvationPerBlockHit")
    private float starvationPerBlockHit = 0.02f;
    @SerializedName("StarvationStaminaModifier")
    private float starvationStaminaModifier = 0.2f;
    @SerializedName("HungryThreshold")
    private float hungryThreshold = 20.0f;
    @SerializedName("StarvationDamage")
    private float starvationDamage = 0.5f;
    @SerializedName("HudPosition")
    private HudPosition hudPosition = HudPosition.Preset.BelowHotbarLeft;
    @SerializedName("SinglePlayer")
    private boolean singlePlayer = true;
    @SerializedName("EnableHunger")
    private boolean enableHunger = true;
    @SerializedName("LifePerHunger")
    private boolean lifePerHunger = true;
    @SerializedName("PulseInterval")
    private float pulseInterval = 1.5f;
    @SerializedName("HealthPerPulse")
    private float healthPerPulse = 2.0f;
    @SerializedName("HungerCost")
    private float hungerCost = 0.25f;
    @SerializedName("ThirstCost")
    private float thirstCost = 0.35f;
    @SerializedName("ConfigVersion")
    private String configVersion = "1.7.5";
    @SerializedName("ActivePreset")
    private String activePreset = "NORMAL";
    @SerializedName("SatiatedThreshold")
    private float satiatedThreshold = 85.0f;
    @SerializedName("StarveResilienceThreshold")
    private float starveResilienceThreshold = 15.0f;

    public float getInitialHungerLevel() {
        return initialHungerLevel;
    }

    public void setInitialHungerLevel(float val) {
        this.initialHungerLevel = val;
    }

    public float getMaxHungerLevel() {
        return maxHungerLevel;
    }

    public void setMaxHungerLevel(float val) {
        this.maxHungerLevel = val;
    }

    public float getRespawnHungerLevel() {
        return respawnHungerLevel;
    }

    public void setRespawnHungerLevel(float val) {
        this.respawnHungerLevel = val;
    }

    public boolean isResetHungerOnDeath() {
        return resetHungerOnDeath;
    }

    public void setResetHungerOnDeath(boolean val) {
        this.resetHungerOnDeath = val;
    }

    public float getStarvationTickRate() {
        return starvationTickRate;
    }

    public void setStarvationTickRate(float val) {
        this.starvationTickRate = val;
    }

    public float getStarvationPerTick() {
        return starvationPerTick;
    }

    public void setStarvationPerTick(float val) {
        this.starvationPerTick = val;
    }

    public float getStarvationPerBlockHit() {
        return starvationPerBlockHit;
    }

    public void setStarvationPerBlockHit(float val) {
        this.starvationPerBlockHit = val;
    }

    public float getStarvationStaminaModifier() {
        return starvationStaminaModifier;
    }

    public void setStarvationStaminaModifier(float val) {
        this.starvationStaminaModifier = val;
    }

    public float getHungryThreshold() {
        return hungryThreshold;
    }

    public void setHungryThreshold(float val) {
        this.hungryThreshold = val;
    }

    public float getStarvationDamage() {
        return starvationDamage;
    }

    public void setStarvationDamage(float val) {
        this.starvationDamage = val;
    }

    public HudPosition getHudPosition() {
        return hudPosition;
    }

    public void setHudPosition(HudPosition val) {
        this.hudPosition = val;
    }

    public boolean isSinglePlayer() {
        return singlePlayer;
    }

    public void setSinglePlayer(boolean val) {
        this.singlePlayer = val;
    }

    public boolean isEnableHunger() {
        return enableHunger;
    }

    public void setEnableHunger(boolean val) {
        this.enableHunger = val;
    }

    public boolean isLifePerHunger() {
        return lifePerHunger;
    }

    public void setLifePerHunger(boolean val) {
        this.lifePerHunger = val;
    }

    public float getPulseInterval() {
        return pulseInterval;
    }

    public void setPulseInterval(float val) {
        this.pulseInterval = val;
    }

    public float getHealthPerPulse() {
        return healthPerPulse;
    }

    public void setHealthPerPulse(float val) {
        this.healthPerPulse = val;
    }

    public float getHungerCost() {
        return hungerCost;
    }

    public void setHungerCost(float val) {
        this.hungerCost = val;
    }

    public float getThirstCost() {
        return thirstCost;
    }

    public void setThirstCost(float val) {
        this.thirstCost = val;
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

    public void ensureDefaults() {
        ConfigMigrationManager.migrate(this);
    }

    public void applyPreset(String preset) {
        switch (preset.toUpperCase()) {
            case "EASY":
                this.setInitialHungerLevel(200.0f);
                this.setMaxHungerLevel(200.0f);
                this.setRespawnHungerLevel(80.0f);
                this.setResetHungerOnDeath(false);
                this.setStarvationTickRate(30.0f);
                this.setStarvationPerTick(0.01f);
                this.setStarvationPerBlockHit(0.01f);
                this.setStarvationStaminaModifier(0.1f);
                this.setHungryThreshold(10.0f);
                this.setStarvationDamage(5.0f);
                this.setPulseInterval(1.5f);
                this.setHealthPerPulse(2.0f);
                this.setHungerCost(0.25f);
                this.setThirstCost(0.45f);
                this.setSinglePlayer(true);
                this.setEnableHunger(true);
                this.setActivePreset("EASY");
                break;
            case "NORMAL":
                this.setInitialHungerLevel(100.0f);
                this.setMaxHungerLevel(200.0f);
                this.setRespawnHungerLevel(50.0f);
                this.setResetHungerOnDeath(true);
                this.setStarvationTickRate(2f);
                this.setStarvationPerTick(0.01f);
                this.setStarvationPerBlockHit(0.02f);
                this.setStarvationStaminaModifier(0.2f);
                this.setHungryThreshold(20.0f);
                this.setStarvationDamage(1.5f);
                this.setPulseInterval(1.5f);
                this.setHealthPerPulse(2.0f);
                this.setHungerCost(0.25f);
                this.setThirstCost(0.35f);
                this.setLifePerHunger(true);
                this.setSinglePlayer(true);
                this.setEnableHunger(true);
                this.setActivePreset("NORMAL");
                break;
            case "HARD":
                this.setInitialHungerLevel(70.0f);
                this.setMaxHungerLevel(150.0f);
                this.setRespawnHungerLevel(30.0f);
                this.setResetHungerOnDeath(true);
                this.setStarvationTickRate(1.0f);
                this.setStarvationPerTick(0.05f);
                this.setStarvationPerBlockHit(0.1f);
                this.setStarvationStaminaModifier(0.3f);
                this.setHungryThreshold(30.0f);
                this.setStarvationDamage(1.5f);
                this.setPulseInterval(1.5f);
                this.setHealthPerPulse(2.0f);
                this.setHungerCost(0.25f);
                this.setThirstCost(0.45f);
                this.setSinglePlayer(true);
                this.setEnableHunger(true);
                this.setActivePreset("HARD");
                break;
        }
    }
}
