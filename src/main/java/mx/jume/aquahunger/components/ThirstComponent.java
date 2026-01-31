package mx.jume.aquahunger.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import mx.jume.aquahunger.AquaThirstHunger;
import javax.annotation.Nullable;

public class ThirstComponent implements Component<EntityStore> {
    public static final BuilderCodec<ThirstComponent> CODEC = BuilderCodec
            .builder(ThirstComponent.class, ThirstComponent::new)
            .append(new KeyedCodec<>("ThirstLevel", Codec.FLOAT),
                    ((data, value) -> data.thirstLevel = value),
                    ThirstComponent::getThirstLevel)
            .add()
            .build();

    public static final float maxThirstLevel = 100.0f;
    private float thirstLevel;
    private float elapsedTime = 0.0f;

    public ThirstComponent() {
        // Default to max (hydrated)
        this.thirstLevel = maxThirstLevel;
    }

    public ThirstComponent(float thirstLevel) {
        this.thirstLevel = Math.max(0.0f, Math.min(thirstLevel, maxThirstLevel));
    }

    public ThirstComponent(ThirstComponent other) {
        this.thirstLevel = other.thirstLevel;
        this.elapsedTime = other.elapsedTime;
    }

    @Nullable
    @Override
    public Component<EntityStore> clone() {
        return new ThirstComponent(this);
    }

    public float getThirstLevel() {
        return this.thirstLevel;
    }

    public void setThirstLevel(float thirstLevel) {
        this.thirstLevel = Math.max(0.0f, Math.min(thirstLevel, maxThirstLevel));
    }

    public void drink(float amount) {
        this.thirstLevel = Math.min(this.thirstLevel + amount, maxThirstLevel);
    }

    public void dehydrate(float amount) {
        this.thirstLevel = Math.max(this.thirstLevel - amount, 0.0f);
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

    public static ComponentType<EntityStore, ThirstComponent> getComponentType() {
        return AquaThirstHunger.get().getThirstComponentType();
    }
}
