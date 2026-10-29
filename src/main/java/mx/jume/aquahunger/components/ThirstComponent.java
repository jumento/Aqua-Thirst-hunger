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

public class ThirstComponent implements Component<EntityStore> {
    public static final BuilderCodec<ThirstComponent> CODEC = BuilderCodec
            .builder(ThirstComponent.class, ThirstComponent::new)
            .append(new KeyedCodec<>("ThirstLevel", Codec.FLOAT),
                    ((data, value) -> data.thirstLevel = value),
                    ThirstComponent::getThirstLevel)
            .add()
            .append(new KeyedCodec<>("MaxThirst", Codec.FLOAT),
                    ((data, value) -> data.maxThirst = value),
                    ThirstComponent::getMaxThirst)
            .add()
            .append(new KeyedCodec<>("TimesFilled", Codec.FLOAT),
                    ((data, value) -> data.timesFilledTotal = value),
                    ThirstComponent::getTimesFilledTotal)
            .add()
            .append(new KeyedCodec<>("TimesEmptied", Codec.FLOAT),
                    ((data, value) -> data.timesEmptiedTotal = value),
                    ThirstComponent::getTimesEmptiedTotal)
            .add()
            .append(new KeyedCodec<>("StaminaBoostApplied", Codec.BOOLEAN),
                    ((data, value) -> data.staminaBoostApplied = value),
                    ThirstComponent::isStaminaBoostApplied)
            .add()
            .build();

    public static final float DEFAULT_MAX_THIRST = 100.0f;
    private float maxThirst;
    private float timesFilledTotal = 0.0f;
    private float timesEmptiedTotal = 0.0f;
    private boolean awardedResilienceXP = false;
    private boolean xpUp = false;
    private boolean xpDown = false;
    private float thirstLevel;
    private float elapsedTime = 0.0f;
    private float lowestStaminaSeen = 10.0f;
    private int blockHits = 0;
    private float coughCooldownTimer = 3.0f;
    private float lastThirstLevel = 100.0f;
    private float idleTime = 0.0f; // Seconds without moving or hitting blocks (AFK detection)
    // Starts "ready" so the first dehydration hit lands as soon as thirst reaches 0.
    private float dehydrationDamageTimer = Float.MAX_VALUE;
    private boolean staminaBoostApplied = false;

    public ThirstComponent() {
        this.maxThirst = AquaThirstHunger.get().getThirstConfig().getMaxThirst();
        this.thirstLevel = maxThirst;
    }

    public ThirstComponent(float thirstLevel) {
        this.maxThirst = DEFAULT_MAX_THIRST;
        this.thirstLevel = Math.max(0.0f, Math.min(thirstLevel, maxThirst));
    }

    public ThirstComponent(ThirstComponent other) {
        this.thirstLevel = other.thirstLevel;
        this.elapsedTime = other.elapsedTime;
        this.lowestStaminaSeen = other.lowestStaminaSeen;
        this.blockHits = other.blockHits;
        this.maxThirst = other.maxThirst;
        this.timesFilledTotal = other.timesFilledTotal;
        this.timesEmptiedTotal = other.timesEmptiedTotal;
        this.awardedResilienceXP = other.awardedResilienceXP;
        this.xpUp = other.xpUp;
        this.xpDown = other.xpDown;
        this.coughCooldownTimer = other.coughCooldownTimer;
        this.lastThirstLevel = other.lastThirstLevel;
        this.idleTime = other.idleTime;
        this.dehydrationDamageTimer = other.dehydrationDamageTimer;
        this.staminaBoostApplied = other.staminaBoostApplied;
    }

    @Nullable
    @Override
    public Component<EntityStore> clone() {
        return new ThirstComponent(this);
    }

    public float getAndResetLowestStaminaSeen() {
        float lowestStaminaSeen = this.lowestStaminaSeen;
        this.lowestStaminaSeen = 10.0f;
        return lowestStaminaSeen;
    }

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
        this.idleTime = 0.0f;
    }

    public float getIdleTime() { return idleTime; }

    /** Accumulates idle time while the player stands still; any movement resets it. */
    public void trackIdle(boolean standingStill, float dt) {
        this.idleTime = standingStill ? this.idleTime + dt : 0.0f;
    }

    /** @return true when a dehydration hit is due (every {@code interval} seconds at 0 thirst). */
    public boolean tickDehydrationDamage(float dt, float interval) {
        if (this.dehydrationDamageTimer != Float.MAX_VALUE) {
            this.dehydrationDamageTimer += dt;
        }
        if (this.dehydrationDamageTimer >= interval) {
            this.dehydrationDamageTimer = 0.0f;
            return true;
        }
        return false;
    }

    public void resetDehydrationDamage() {
        this.dehydrationDamageTimer = Float.MAX_VALUE;
    }

    public boolean isStaminaBoostApplied() { return staminaBoostApplied; }
    public void setStaminaBoostApplied(boolean applied) { this.staminaBoostApplied = applied; }

    public float getThirstLevel() {
        return this.thirstLevel;
    }

    public void setThirstLevel(float thirstLevel) {
        this.thirstLevel = Math.max(0.0f, Math.min(thirstLevel, maxThirst));
    }

    public void drink(float amount) {
        this.thirstLevel = Math.min(this.thirstLevel + amount, maxThirst);
    }

    public void dehydrate(float amount) {
        this.thirstLevel = Math.max(this.thirstLevel - amount, 0.0f);
    }

    public float getMaxThirst() {
        return maxThirst;
    }

    public void setMaxThirst(float maxThirst) {
        this.maxThirst = maxThirst;
        if (this.thirstLevel > maxThirst) {
            this.thirstLevel = maxThirst;
        }
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

    public float getElapsedTime() {
        return elapsedTime;
    }

    public void addElapsedTime(float delta) {
        this.elapsedTime += delta;
    }

    public void resetElapsedTime() {
        this.elapsedTime = 0.0f;
    }

    public boolean isXpUp() { return xpUp; }
    public void setXpUp(boolean xpUp) { this.xpUp = xpUp; }
    public boolean isXpDown() { return xpDown; }
    public void setXpDown(boolean xpDown) { this.xpDown = xpDown; }

    public float getCoughCooldownTimer() { return coughCooldownTimer; }
    public void setCoughCooldownTimer(float timer) { this.coughCooldownTimer = timer; }
    public void addCoughCooldown(float dt) { this.coughCooldownTimer += dt; }

    public float getLastThirstLevel() { return lastThirstLevel; }
    public void setLastThirstLevel(float level) { this.lastThirstLevel = level; }

    @Nonnull
    public static ComponentType<EntityStore, ThirstComponent> getComponentType() {
        return AquaThirstHunger.get().getThirstComponentType();
    }
}
