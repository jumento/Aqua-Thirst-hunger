package mx.jume.aquahunger.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import mx.jume.aquahunger.AquaThirstHunger;
import mx.jume.aquahunger.config.ConfigManager;
import mx.jume.aquahunger.config.HHMHungerConfig;

import javax.annotation.Nonnull;
import java.util.Objects;

public class AquaLifePerHungerPage extends InteractiveCustomUIPage<AquaLifePerHungerPage.ConfigEventData> {

    private final ConfigManager configManager;
    private final PlayerRef playerRef;

    public static class ConfigEventData {
        public String action;
        public boolean lifePerHunger;
        public String pulseInterval;
        public String healthPerPulse;
        public String hungerCost;
        public String thirstCost;

        public static final BuilderCodec<ConfigEventData> CODEC = BuilderCodec
                .builder(ConfigEventData.class, ConfigEventData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING), (o, v) -> o.action = v, o -> o.action).add()
                .append(new KeyedCodec<>("@LifePerHunger", Codec.BOOLEAN),
                        (o, v) -> o.lifePerHunger = (v != null ? v : false), o -> o.lifePerHunger)
                .add()
                .append(new KeyedCodec<>("@PulseInterval", Codec.STRING),
                        (o, v) -> o.pulseInterval = (v != null ? v : "0"), o -> o.pulseInterval)
                .add()
                .append(new KeyedCodec<>("@HealthPerPulse", Codec.STRING),
                        (o, v) -> o.healthPerPulse = (v != null ? v : "0"), o -> o.healthPerPulse)
                .add()
                .append(new KeyedCodec<>("@HungerCost", Codec.STRING),
                        (o, v) -> o.hungerCost = (v != null ? v : "0"), o -> o.hungerCost)
                .add()
                .append(new KeyedCodec<>("@ThirstCost", Codec.STRING),
                        (o, v) -> o.thirstCost = (v != null ? v : "0"), o -> o.thirstCost)
                .add()
                .build();
        @Nonnull
        public static final BuilderCodec<ConfigEventData> CODEC_NONNULL = Objects.requireNonNull(CODEC);
    }

    public AquaLifePerHungerPage(@Nonnull PlayerRef playerRef, @Nonnull ConfigManager configManager) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, ConfigEventData.CODEC_NONNULL);
        this.playerRef = playerRef;
        this.configManager = Objects.requireNonNull(configManager);
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt,
            @Nonnull Store<EntityStore> store) {
        HHMHungerConfig config = configManager.getHungerConfig();

        cmd.append("Pages/AquaLifePerHungerUI.ui");

        // Display Active Status
        cmd.set("#configtype.Text", "Config: " + config.getActivePreset());

        // CheckBoxes
        cmd.set("#ChkLifePerHunger #CheckBox.Value", config.isLifePerHunger());

        // TextFields
        cmd.set("#InputPulseInterval.Value", String.valueOf(config.getPulseInterval()));
        cmd.set("#InputHealthPerPulse.Value", String.valueOf(config.getHealthPerPulse()));
        cmd.set("#InputHungerCost.Value", String.valueOf(config.getHungerCost()));
        cmd.set("#InputThirstCost.Value", String.valueOf(config.getThirstCost()));

        // Bind events
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#BtnCancel", createSyncData("close"));
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#BtnBack", createSyncData("back"));
    }

    private EventData createSyncData(String action) {
        return new EventData()
                .append("Action", action)
                .append("@LifePerHunger", "#ChkLifePerHunger #CheckBox.Value")
                .append("@PulseInterval", "#InputPulseInterval.Value")
                .append("@HealthPerPulse", "#InputHealthPerPulse.Value")
                .append("@HungerCost", "#InputHungerCost.Value")
                .append("@ThirstCost", "#InputThirstCost.Value");
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
            @Nonnull ConfigEventData data) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null)
            return;

        HHMHungerConfig config = configManager.getHungerConfig();

        // Save
        boolean modifiedManually = false;
        if (data.lifePerHunger != config.isLifePerHunger()) {
            config.setLifePerHunger(data.lifePerHunger);
            modifiedManually = true;
        }

        try {
            float pulseInterval = Float.parseFloat(data.pulseInterval);
            if (pulseInterval != config.getPulseInterval()) {
                config.setPulseInterval(pulseInterval);
                modifiedManually = true;
            }

            float healthPerPulse = Float.parseFloat(data.healthPerPulse);
            if (healthPerPulse != config.getHealthPerPulse()) {
                config.setHealthPerPulse(healthPerPulse);
                modifiedManually = true;
            }

            float hungerCost = Float.parseFloat(data.hungerCost);
            if (hungerCost != config.getHungerCost()) {
                config.setHungerCost(hungerCost);
                modifiedManually = true;
            }

            float thirstCost = Float.parseFloat(data.thirstCost);
            if (thirstCost != config.getThirstCost()) {
                config.setThirstCost(thirstCost);
                modifiedManually = true;
            }
        } catch (NumberFormatException ignored) {
        }

        if ("close".equals(data.action)) {
            if (modifiedManually)
                config.setActivePreset("CUSTOM");
            configManager.save();
            AquaThirstHunger.get().syncHUDs();
            player.getPageManager().setPage(ref, store, Page.None);
            return;
        }

        if ("back".equals(data.action)) {
            if (modifiedManually)
                config.setActivePreset("CUSTOM");
            configManager.save();
            AquaThirstHunger.get().syncHUDs();
            player.getPageManager().openCustomPage(ref, store, new AquaCheffCustomPage(this.playerRef));
            return;
        }
    }
}
