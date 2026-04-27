package mx.jume.aquahunger.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import mx.jume.aquahunger.AquaThirstHunger;

import javax.annotation.Nullable;
import javax.annotation.Nonnull;

public class HungerComponent implements Component<EntityStore> {
    public static final BuilderCodec<HungerComponent> CODEC = BuilderCodec
            .builder(HungerComponent.class, HungerComponent::new)
            .append(new KeyedCodec<>("HungerLevel", Codec.FLOAT),
                    ((data, value) -> data.hungerLevel = value),
                    HungerComponent::getHungerLevel)
            .add()
            .append(new KeyedCodec<>("HealTimer", Codec.FLOAT),
                    ((data, value) -> data.healTimer = value),
                    HungerComponent::getHealTimer)
            .add()
            .append(new KeyedCodec<>("MaxHunger", Codec.FLOAT),
                    ((data, value) -> data.maxHunger = value),
                    HungerComponent::getMaxHunger)
            .add()
            .append(new KeyedCodec<>("TimesFilled", Codec.FLOAT),
                    ((data, value) -> data.timesFilledTotal = value),
                    HungerComponent::getTimesFilledTotal)
            .add()
            .append(new KeyedCodec<>("TimesEmptied", Codec.FLOAT),
                    ((data, value) -> data.timesEmptiedTotal = value),
                    HungerComponent::getTimesEmptiedTotal)
            .add()
            .append(new KeyedCodec<>("GluttonTriggered", Codec.BOOLEAN),
                    ((data, value) -> data.gluttonTriggered = value),
                    HungerComponent::isGluttonTriggered)
            .add()
            .build();

    public static final float DEFAULT_MAX_HUNGER = 200.0f;
    private float maxHunger;
    private float timesFilledTotal = 0.0f;
    private float timesEmptiedTotal = 0.0f;
    private boolean awardedResilienceXP = false; 
    private boolean xpUp = false;
    private boolean xpDown = false;
    // public static final float initialHungerLevel = 100.0f; // Removed in favor of
    // config
    private float elapsedTime = 0.0f;
    private float starvationElapsedTime = 0.0f;
    private int blockHits = 0;
    private float lowestStaminaSeen = 10.0f;
    private boolean gluttonTriggered = false; // Flag para el sistema MMO
    private float hungerLevel;
    private float healTimer = 0.0f; // Track time for lifePerHunger logic
    private float growlCooldownTimer = 3.0f; // Initialize at 3s to allow immediate first sound
    private float lastHungerLevel = 100.0f; // Track hunger level of previous tick

    public HungerComponent() {
        this.hungerLevel = AquaThirstHunger.get().getHungerConfig().getInitialHungerLevel();
        this.maxHunger = DEFAULT_MAX_HUNGER;
    }

    public HungerComponent(float hungerLevel) {
        this.hungerLevel = hungerLevel;
        this.maxHunger = DEFAULT_MAX_HUNGER;
    }

    public HungerComponent(HungerComponent other) {
        this.hungerLevel = other.hungerLevel;
        this.elapsedTime = other.elapsedTime;
        this.lowestStaminaSeen = other.lowestStaminaSeen;
        this.blockHits = other.blockHits;
        this.maxHunger = other.maxHunger;
        this.timesFilledTotal = other.timesFilledTotal;
        this.timesEmptiedTotal = other.timesEmptiedTotal;
        this.xpUp = other.xpUp;
        this.xpDown = other.xpDown;
        this.gluttonTriggered = other.gluttonTriggered;
    }

    @Nullable
    @Override
    public Component<EntityStore> clone() {
        return new HungerComponent(this);
    }

    public float getElapsedTime() {
        return this.elapsedTime;
    }

    public void addElapsedTime(float deltaTime) {
        this.elapsedTime += deltaTime;
    }

    public void resetElapsedTime() {
        this.elapsedTime = 0.0f;
    }

    public float getAndResetLowestStaminaSeen() {
        float lowestStaminaSeen = this.lowestStaminaSeen;
        this.lowestStaminaSeen = 10.0f;
        return lowestStaminaSeen;
    }

    public void setLowestStaminaSeen(float lowestStaminaSeen) { this.lowestStaminaSeen = lowestStaminaSeen; }

    public boolean isGluttonTriggered() { return gluttonTriggered; }
    public void setGluttonTriggered(boolean gluttonTriggered) { this.gluttonTriggered = gluttonTriggered; }

    public void setStaminaSeen(float stamina) {
        if (stamina > this.lowestStaminaSeen)
            return;
        this.lowestStaminaSeen = stamina;
    }

    public int getAndResetBlockHits() {
        int blockHits = this.blockHits;
        this.blockHits = 0;
        return blockHits;
    }

    public void incrementBlockHits() {
        this.blockHits += 1;
    }

    public float getHungerLevel() {
        return this.hungerLevel;
    }

    public void setHungerLevel(float hungerLevel) {
        this.hungerLevel = Math.max(0.0f, Math.min(hungerLevel, maxHunger));
    }

    public void feed(float amount) {
        this.hungerLevel = Math.min(this.hungerLevel + amount, maxHunger);
    }

    public void starve(float amount) {
        this.hungerLevel = Math.max(this.hungerLevel - amount, 0.0f);
    }

    public float getMaxHunger() {
        return this.maxHunger;
    }

    public void setMaxHunger(float maxHunger) {
        this.maxHunger = maxHunger;
    }

    public float getTimesFilledTotal() {
        return timesFilledTotal;
    }

    public void incrementTimesFilled() {
        this.timesFilledTotal += 1.0f;
    }

    public float getTimesEmptiedTotal() {
        return timesEmptiedTotal;
    }

    public void incrementTimesEmptied() {
        this.timesEmptiedTotal += 1.0f;
    }

    public boolean hasAwardedResilienceXP() {
        return awardedResilienceXP;
    }

    public void setAwardedResilienceXP(boolean awarded) {
        this.awardedResilienceXP = awarded;
    }

    public float getHealTimer() {
        return this.healTimer;
    }

    public void setHealTimer(float healTimer) {
        this.healTimer = healTimer;
    }

    public boolean isXpUp() { return xpUp; }
    public void setXpUp(boolean xpUp) { this.xpUp = xpUp; }
    public boolean isXpDown() { return xpDown; }
    public void setXpDown(boolean xpDown) { this.xpDown = xpDown; }

    public float getGrowlCooldownTimer() { return growlCooldownTimer; }
    public void setGrowlCooldownTimer(float timer) { this.growlCooldownTimer = timer; }
    public void addGrowlCooldown(float dt) { this.growlCooldownTimer += dt; }

    public float getLastHungerLevel() { return lastHungerLevel; }
    public void setLastHungerLevel(float level) { this.lastHungerLevel = level; }

    @Nonnull
    public static ComponentType<EntityStore, HungerComponent> getComponentType() {
        return AquaThirstHunger.get().getHungerComponentType();
    }
}
