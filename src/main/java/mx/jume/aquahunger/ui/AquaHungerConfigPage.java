package mx.jume.aquahunger.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.Message;
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

/**
 * AquaHungerConfigPage - Managed configuration UI for the AquaHunger mod.
 * Features: Localized tooltips, Presets (Easy, Normal, Hard), and Active status
 * tracking.
 */
public class AquaHungerConfigPage extends InteractiveCustomUIPage<AquaHungerConfigPage.ConfigEventData> {

    private final ConfigManager configManager;

    public static class ConfigEventData {
        public String action;
        public String initialHunger;
        public String maxHunger;
        public String respawnHunger;
        public boolean resetDeath;
        public String starveTick;
        public String starvePerTick;
        public String starveBlockHit;
        public String staminaMod;
        public String hungryThreshold;
        public String starveDamage;
        public boolean singlePlayer;
        public boolean enableHunger;
        public boolean lifePerHunger;

        public static final BuilderCodec<ConfigEventData> CODEC = BuilderCodec
                .builder(ConfigEventData.class, ConfigEventData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING), (o, v) -> o.action = v, o -> o.action).add()
                .append(new KeyedCodec<>("@InitialHunger", Codec.STRING),
                        (o, v) -> o.initialHunger = (v != null ? v : "0"), o -> o.initialHunger)
                .add()
                .append(new KeyedCodec<>("@MaxHunger", Codec.STRING), (o, v) -> o.maxHunger = (v != null ? v : "0"),
                        o -> o.maxHunger)
                .add()
                .append(new KeyedCodec<>("@RespawnHunger", Codec.STRING),
                        (o, v) -> o.respawnHunger = (v != null ? v : "0"), o -> o.respawnHunger)
                .add()
                .append(new KeyedCodec<>("@ResetDeath", Codec.BOOLEAN),
                        (o, v) -> o.resetDeath = (v != null ? v : false), o -> o.resetDeath)
                .add()
                .append(new KeyedCodec<>("@StarveTick", Codec.STRING), (o, v) -> o.starveTick = (v != null ? v : "0"),
                        o -> o.starveTick)
                .add()
                .append(new KeyedCodec<>("@StarvePerTick", Codec.STRING),
                        (o, v) -> o.starvePerTick = (v != null ? v : "0"), o -> o.starvePerTick)
                .add()
                .append(new KeyedCodec<>("@StarveBlockHit", Codec.STRING),
                        (o, v) -> o.starveBlockHit = (v != null ? v : "0"), o -> o.starveBlockHit)
                .add()
                .append(new KeyedCodec<>("@StaminaMod", Codec.STRING), (o, v) -> o.staminaMod = (v != null ? v : "0"),
                        o -> o.staminaMod)
                .add()
                .append(new KeyedCodec<>("@HungryThreshold", Codec.STRING),
                        (o, v) -> o.hungryThreshold = (v != null ? v : "0"), o -> o.hungryThreshold)
                .add()
                .append(new KeyedCodec<>("@StarveDamage", Codec.STRING),
                        (o, v) -> o.starveDamage = (v != null ? v : "0"), o -> o.starveDamage)
                .add()
                .append(new KeyedCodec<>("@SinglePlayer", Codec.BOOLEAN),
                        (o, v) -> o.singlePlayer = (v != null ? v : false), o -> o.singlePlayer)
                .add()
                .append(new KeyedCodec<>("@EnableHunger", Codec.BOOLEAN),
                        (o, v) -> o.enableHunger = (v != null ? v : false), o -> o.enableHunger)
                .add()
                .append(new KeyedCodec<>("@LifePerHunger", Codec.BOOLEAN),
                        (o, v) -> o.lifePerHunger = (v != null ? v : false), o -> o.lifePerHunger)
                .add()
                .build();
        @Nonnull
        public static final BuilderCodec<ConfigEventData> CODEC_NONNULL = Objects.requireNonNull(CODEC);
    }

    public AquaHungerConfigPage(@Nonnull PlayerRef playerRef, @Nonnull ConfigManager configManager) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, ConfigEventData.CODEC_NONNULL);
        this.configManager = Objects.requireNonNull(configManager);
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt,
            @Nonnull Store<EntityStore> store) {
        HHMHungerConfig config = configManager.getHungerConfig();

        cmd.append("Pages/AquaHungerConfigUI.ui");

        // Display Active Status
        cmd.set("#configtype.Text", "Config: " + config.getActivePreset());

        // Set form values
        cmd.set("#InputInitialHunger.Value", String.valueOf((int) config.getInitialHungerLevel()));
        cmd.set("#InputMaxHunger.Value", String.valueOf((int) config.getMaxHungerLevel()));
        cmd.set("#InputRespawnHunger.Value", String.valueOf((int) config.getRespawnHungerLevel()));
        cmd.set("#InputStarveTick.Value", String.valueOf((int) config.getStarvationTickRate()));
        cmd.set("#InputStarvePerTick.Value", String.valueOf(config.getStarvationPerTick()));
        cmd.set("#InputStarveBlockHit.Value", String.valueOf(config.getStarvationPerBlockHit()));
        cmd.set("#InputStaminaMod.Value", String.valueOf(config.getStarvationStaminaModifier()));
        cmd.set("#InputHungryThreshold.Value", String.valueOf(config.getHungryThreshold()));
        cmd.set("#InputStarvationDamage.Value", String.valueOf(config.getStarvationDamage()));

        // CheckBoxes (Hytale Macro Protocol: Deep Selectors)
        cmd.set("#ChkResetDeath #CheckBox.Value", config.isResetHungerOnDeath());
        cmd.set("#ChkSinglePlayer #CheckBox.Value", config.isSinglePlayer());
        cmd.set("#ChkEnableHunger #CheckBox.Value", config.isEnableHunger());
        cmd.set("#ChkLifePerHunger #CheckBox.Value", config.isLifePerHunger());

        // Bind events to Buttons ONLY
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#BtnEasy", createSyncData("PRESET:EASY"));
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#BtnNormal", createSyncData("PRESET:NORMAL"));
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#BtnHard", createSyncData("PRESET:HARD"));
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#BtnCancel", createSyncData("close"));
    }

    private EventData createSyncData(String action) {
        return new EventData()
                .append("Action", action)
                .append("@InitialHunger", "#InputInitialHunger.Value")
                .append("@MaxHunger", "#InputMaxHunger.Value")
                .append("@RespawnHunger", "#InputRespawnHunger.Value")
                .append("@ResetDeath", "#ChkResetDeath #CheckBox.Value")
                .append("@StarveTick", "#InputStarveTick.Value")
                .append("@StarvePerTick", "#InputStarvePerTick.Value")
                .append("@StarveBlockHit", "#InputStarveBlockHit.Value")
                .append("@StaminaMod", "#InputStaminaMod.Value")
                .append("@HungryThreshold", "#InputHungryThreshold.Value")
                .append("@StarveDamage", "#InputStarvationDamage.Value")
                .append("@SinglePlayer", "#ChkSinglePlayer #CheckBox.Value")
                .append("@EnableHunger", "#ChkEnableHunger #CheckBox.Value")
                .append("@LifePerHunger", "#ChkLifePerHunger #CheckBox.Value");
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
            @Nonnull ConfigEventData data) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null)
            return;

        HHMHungerConfig config = configManager.getHungerConfig();

        // 1. Capture and Compare Form Values
        boolean modifiedManually = false;
        try {
            float iH = Float.parseFloat(data.initialHunger.trim());
            if (iH != config.getInitialHungerLevel()) {
                config.setInitialHungerLevel(iH);
                modifiedManually = true;
            }

            float mH = Float.parseFloat(data.maxHunger.trim());
            if (mH != config.getMaxHungerLevel()) {
                config.setMaxHungerLevel(mH);
                modifiedManually = true;
            }

            float rH = Float.parseFloat(data.respawnHunger.trim());
            if (rH != config.getRespawnHungerLevel()) {
                config.setRespawnHungerLevel(rH);
                modifiedManually = true;
            }

            float sT = Float.parseFloat(data.starveTick.trim());
            if (sT != config.getStarvationTickRate()) {
                config.setStarvationTickRate(sT);
                modifiedManually = true;
            }

            float sPT = Float.parseFloat(data.starvePerTick.trim());
            if (sPT != config.getStarvationPerTick()) {
                config.setStarvationPerTick(sPT);
                modifiedManually = true;
            }

            float sBH = Float.parseFloat(data.starveBlockHit.trim());
            if (sBH != config.getStarvationPerBlockHit()) {
                config.setStarvationPerBlockHit(sBH);
                modifiedManually = true;
            }

            float sM = Float.parseFloat(data.staminaMod.trim());
            if (sM != config.getStarvationStaminaModifier()) {
                config.setStarvationStaminaModifier(sM);
                modifiedManually = true;
            }

            float hT = Float.parseFloat(data.hungryThreshold.trim());
            if (hT != config.getHungryThreshold()) {
                config.setHungryThreshold(hT);
                modifiedManually = true;
            }

            float sD = Float.parseFloat(data.starveDamage.trim());
            if (sD != config.getStarvationDamage()) {
                config.setStarvationDamage(sD);
                modifiedManually = true;
            }

            if (data.resetDeath != config.isResetHungerOnDeath()) {
                config.setResetHungerOnDeath(data.resetDeath);
                modifiedManually = true;
            }
            if (data.singlePlayer != config.isSinglePlayer()) {
                config.setSinglePlayer(data.singlePlayer);
                modifiedManually = true;
            }
            if (data.enableHunger != config.isEnableHunger()) {
                config.setEnableHunger(data.enableHunger);
                modifiedManually = true;
            }
            if (data.lifePerHunger != config.isLifePerHunger()) {
                config.setLifePerHunger(data.lifePerHunger);
                modifiedManually = true;
            }

        } catch (Exception ignored) {
        }

        // 2. Handle Actions
        if ("close".equals(data.action)) {
            if (modifiedManually)
                config.setActivePreset("CUSTOM");
            configManager.save();
            AquaThirstHunger.get().syncHUDs(); // Sync changes on close
            player.getPageManager().setPage(ref, store, Page.None);
            return;
        }

        if (data.action != null && data.action.startsWith("PRESET:")) {
            String preset = data.action.substring(7);
            applyPreset(preset);
            config.setActivePreset(preset);
            configManager.save();
            AquaThirstHunger.get().syncHUDs(); // Sync changes on preset

            // Refresh the page
            player.getPageManager().openCustomPage(ref, store,
                    new AquaHungerConfigPage(this.playerRef, Objects.requireNonNull(configManager)));
            this.playerRef.sendMessage(
                    Message.empty().insert("Preset applied: ").insert(Objects.requireNonNull(preset)).color("#00BFFF"));
            return;
        }

        if (modifiedManually)
            config.setActivePreset("CUSTOM");
        configManager.save();
    }

    private void applyPreset(String preset) {
        HHMHungerConfig config = configManager.getHungerConfig();
        switch (preset) {
            case "EASY":
                config.setInitialHungerLevel(200.0f);
                config.setMaxHungerLevel(100.0f);
                config.setRespawnHungerLevel(80.0f);
                config.setResetHungerOnDeath(false);
                config.setStarvationTickRate(2.0f);
                config.setStarvationPerTick(0.05f);
                config.setStarvationPerBlockHit(0.01f);
                config.setStarvationStaminaModifier(0.1f);
                config.setHungryThreshold(10.0f);
                config.setStarvationDamage(2.0f);
                config.setSinglePlayer(true);
                config.setEnableHunger(true);
                config.setLifePerHunger(true);
                break;
            case "NORMAL":
                config.setInitialHungerLevel(100.0f);
                config.setMaxHungerLevel(100.0f);
                config.setRespawnHungerLevel(50.0f);
                config.setResetHungerOnDeath(true);
                config.setStarvationTickRate(2.0f);
                config.setStarvationPerTick(0.125f);
                config.setStarvationPerBlockHit(0.02f);
                config.setStarvationStaminaModifier(0.175f);
                config.setHungryThreshold(20.0f);
                config.setStarvationDamage(5.0f);
                config.setSinglePlayer(true);
                config.setEnableHunger(true);
                config.setLifePerHunger(true);
                break;
            case "HARD":
                config.setInitialHungerLevel(50.0f);
                config.setMaxHungerLevel(100.0f);
                config.setRespawnHungerLevel(20.0f);
                config.setResetHungerOnDeath(true);
                config.setStarvationTickRate(1.0f);
                config.setStarvationPerTick(0.25f);
                config.setStarvationPerBlockHit(0.05f);
                config.setStarvationStaminaModifier(0.3f);
                config.setHungryThreshold(30.0f);
                config.setStarvationDamage(10.0f);
                config.setSinglePlayer(true);
                config.setEnableHunger(true);
                config.setLifePerHunger(false);
                break;
        }
    }
}
