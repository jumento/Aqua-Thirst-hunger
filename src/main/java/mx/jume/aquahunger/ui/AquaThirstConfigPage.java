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
import mx.jume.aquahunger.config.HHMThirstConfig;
import mx.jume.aquahunger.config.HHMThirstFoodValuesConfig;

import javax.annotation.Nonnull;
import java.util.Objects;

public class AquaThirstConfigPage extends InteractiveCustomUIPage<AquaThirstConfigPage.ConfigEventData> {

    private final ConfigManager configManager;

    public static class ConfigEventData {
        public String action;
        public String maxThirst;
        public String respawnThirst;
        public boolean resetDeath;
        public String depletionTickRate;
        public String baseDepletion;
        public String depletionBlockHit;
        public String sprintMod;
        public String dehydrationDamage;
        public String damageInterval;
        public boolean enableThirst;
        public String fruitMultiplier;
        public String canteenThirst;
        public String canteenProThirst;
        public String basicBottleThirst;
        public String staminaBoostThreshold;

        public static final BuilderCodec<ConfigEventData> CODEC = BuilderCodec
                .builder(ConfigEventData.class, ConfigEventData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING), (o, v) -> o.action = v, o -> o.action).add()
                .append(new KeyedCodec<>("@MaxThirst", Codec.STRING), (o, v) -> o.maxThirst = (v != null ? v : "0"),
                        o -> o.maxThirst)
                .add()
                .append(new KeyedCodec<>("@RespawnThirst", Codec.STRING),
                        (o, v) -> o.respawnThirst = (v != null ? v : "0"), o -> o.respawnThirst)
                .add()
                .append(new KeyedCodec<>("@ResetDeath", Codec.BOOLEAN),
                        (o, v) -> o.resetDeath = (v != null ? v : false), o -> o.resetDeath)
                .add()
                .append(new KeyedCodec<>("@DepletionTickRate", Codec.STRING),
                        (o, v) -> o.depletionTickRate = (v != null ? v : "0"), o -> o.depletionTickRate)
                .add()
                .append(new KeyedCodec<>("@BaseDepletion", Codec.STRING),
                        (o, v) -> o.baseDepletion = (v != null ? v : "0"), o -> o.baseDepletion)
                .add()
                .append(new KeyedCodec<>("@DepletionBlockHit", Codec.STRING),
                        (o, v) -> o.depletionBlockHit = (v != null ? v : "0"), o -> o.depletionBlockHit)
                .add()
                .append(new KeyedCodec<>("@SprintMod", Codec.STRING), (o, v) -> o.sprintMod = (v != null ? v : "0"),
                        o -> o.sprintMod)
                .add()
                .append(new KeyedCodec<>("@DehydrationDamage", Codec.STRING),
                        (o, v) -> o.dehydrationDamage = (v != null ? v : "0"), o -> o.dehydrationDamage)
                .add()
                .append(new KeyedCodec<>("@DamageInterval", Codec.STRING),
                        (o, v) -> o.damageInterval = (v != null ? v : "0"), o -> o.damageInterval)
                .add()
                .append(new KeyedCodec<>("@EnableThirst", Codec.BOOLEAN),
                        (o, v) -> o.enableThirst = (v != null ? v : false), o -> o.enableThirst)
                .add()
                .append(new KeyedCodec<>("@FruitMultiplier", Codec.STRING),
                        (o, v) -> o.fruitMultiplier = (v != null ? v : "0"), o -> o.fruitMultiplier)
                .add()
                .append(new KeyedCodec<>("@CanteenThirst", Codec.STRING),
                        (o, v) -> o.canteenThirst = (v != null ? v : "0"), o -> o.canteenThirst)
                .add()
                .append(new KeyedCodec<>("@CanteenProThirst", Codec.STRING),
                        (o, v) -> o.canteenProThirst = (v != null ? v : "0"),
                        o -> Objects.requireNonNull(o.canteenProThirst))
                .add()
                .append(new KeyedCodec<>("@BasicBottleThirst", Codec.STRING),
                        (o, v) -> o.basicBottleThirst = (v != null ? v : "0"),
                        o -> Objects.requireNonNull(o.basicBottleThirst))
                .add()
                .append(new KeyedCodec<>("@StaminaBoostThreshold", Codec.STRING),
                        (o, v) -> o.staminaBoostThreshold = (v != null ? v : "0"),
                        o -> Objects.requireNonNull(o.staminaBoostThreshold))
                .add()
                .build();

        @Nonnull
        public static final BuilderCodec<ConfigEventData> CODEC_NONNULL = Objects.requireNonNull(CODEC);
    }

    public AquaThirstConfigPage(@Nonnull PlayerRef playerRef, @Nonnull ConfigManager configManager) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, ConfigEventData.CODEC_NONNULL);
        this.configManager = Objects.requireNonNull(configManager);
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt,
            @Nonnull Store<EntityStore> store) {
        HHMThirstConfig config = configManager.getThirstConfig();
        HHMThirstFoodValuesConfig foodConfig = configManager.getThirstFoodValuesConfig();

        cmd.append("Pages/AquaThirstConfigUI.ui");
        cmd.set("#configtype.Text", "Config: " + config.getActivePreset());

        cmd.set("#InputMaxThirst.Value", String.valueOf((int) config.getMaxThirst()));
        cmd.set("#InputRespawnThirst.Value", String.valueOf((int) config.getRespawnThirstLevel()));
        cmd.set("#InputDepletionTickRate.Value", String.valueOf(config.getDepletionTickRate()));
        cmd.set("#InputBaseThirstDepletion.Value", String.valueOf(config.getDepletionPerTick()));
        cmd.set("#InputDepletionPerBlockHit.Value", String.valueOf(config.getDepletionPerBlockHit()));
        cmd.set("#InputSprintDepletionModifier.Value", String.valueOf(config.getSprintDepletionModifier()));
        cmd.set("#InputDehydrationDamage.Value", String.valueOf(config.getDehydrationDamage()));
        cmd.set("#InputDamageIntervalSeconds.Value", String.valueOf(config.getDamageIntervalSeconds()));

        cmd.set("#ChkResetDeath #CheckBox.Value", config.isResetThirstOnDeath());
        cmd.set("#ChkEnableThirst #CheckBox.Value", config.isEnableThirst());

        // Food values
        cmd.set("#InputFruitMultiplier.Value", String.valueOf(foodConfig.getFruitMultiplier()));
        cmd.set("#InputCanteenThirst.Value",
                String.valueOf((int) foodConfig.getItemThirstRestoration("AquaThirstHunger_Canteen").floatValue()));
        cmd.set("#InputCanteenProThirst.Value", String
                .valueOf((int) foodConfig.getItemThirstRestoration("AquaThirstHunger_Canteenpro_Empty").floatValue()));
        cmd.set("#InputBasicBottleThirst.Value", String
                .valueOf((int) foodConfig.getItemThirstRestoration("AquaThirstHunger_BasicBottle_Empty").floatValue()));
        cmd.set("#InputStaminaBoostThreshold.Value", String.valueOf((int) config.getStaminaBoostThreshold()));

        evt.addEventBinding(CustomUIEventBindingType.Activating, "#BtnEasy", createSyncData("PRESET:EASY"));
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#BtnNormal", createSyncData("PRESET:NORMAL"));
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#BtnHard", createSyncData("PRESET:HARD"));
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#BtnCancel", createSyncData("close"));
    }

    private EventData createSyncData(String action) {
        return new EventData()
                .append("Action", action)
                .append("@MaxThirst", "#InputMaxThirst.Value")
                .append("@RespawnThirst", "#InputRespawnThirst.Value")
                .append("@ResetDeath", "#ChkResetDeath #CheckBox.Value")
                .append("@DepletionTickRate", "#InputDepletionTickRate.Value")
                .append("@BaseDepletion", "#InputBaseThirstDepletion.Value")
                .append("@DepletionBlockHit", "#InputDepletionPerBlockHit.Value")
                .append("@SprintMod", "#InputSprintDepletionModifier.Value")
                .append("@DehydrationDamage", "#InputDehydrationDamage.Value")
                .append("@DamageInterval", "#InputDamageIntervalSeconds.Value")
                .append("@EnableThirst", "#ChkEnableThirst #CheckBox.Value")
                .append("@FruitMultiplier", "#InputFruitMultiplier.Value")
                .append("@CanteenThirst", "#InputCanteenThirst.Value")
                .append("@CanteenProThirst", "#InputCanteenProThirst.Value")
                .append("@BasicBottleThirst", "#InputBasicBottleThirst.Value")
                .append("@StaminaBoostThreshold", "#InputStaminaBoostThreshold.Value");
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
            @Nonnull ConfigEventData data) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null)
            return;

        HHMThirstConfig config = configManager.getThirstConfig();
        HHMThirstFoodValuesConfig foodConfig = configManager.getThirstFoodValuesConfig();

        boolean modifiedManually = false;
        try {
            float mT = Float.parseFloat(data.maxThirst.trim());
            if (mT != config.getMaxThirst()) {
                config.setMaxThirst(mT);
                modifiedManually = true;
            }

            float rT = Float.parseFloat(data.respawnThirst.trim());
            if (rT != config.getRespawnThirstLevel()) {
                config.setRespawnThirstLevel(rT);
                modifiedManually = true;
            }

            float dTR = Float.parseFloat(data.depletionTickRate.trim());
            if (dTR != config.getDepletionTickRate()) {
                config.setDepletionTickRate(dTR);
                modifiedManually = true;
            }

            float bD = Float.parseFloat(data.baseDepletion.trim());
            if (bD != config.getDepletionPerTick()) {
                config.setDepletionPerTick(bD);
                modifiedManually = true;
            }

            float dBH = Float.parseFloat(data.depletionBlockHit.trim());
            if (dBH != config.getDepletionPerBlockHit()) {
                config.setDepletionPerBlockHit(dBH);
                modifiedManually = true;
            }

            float sM = Float.parseFloat(data.sprintMod.trim());
            if (sM != config.getSprintDepletionModifier()) {
                config.setSprintDepletionModifier(sM);
                modifiedManually = true;
            }

            float dD = Float.parseFloat(data.dehydrationDamage.trim());
            if (dD != config.getDehydrationDamage()) {
                config.setDehydrationDamage(dD);
                modifiedManually = true;
            }

            float dI = Float.parseFloat(data.damageInterval.trim());
            if (dI != config.getDamageIntervalSeconds()) {
                config.setDamageIntervalSeconds(dI);
                modifiedManually = true;
            }

            float sBT = Float.parseFloat(data.staminaBoostThreshold.trim());
            if (sBT != config.getStaminaBoostThreshold()) {
                config.setStaminaBoostThreshold(sBT);
                modifiedManually = true;
            }

            if (data.resetDeath != config.isResetThirstOnDeath()) {
                config.setResetThirstOnDeath(data.resetDeath);
                modifiedManually = true;
            }
            if (data.enableThirst != config.isEnableThirst()) {
                config.setEnableThirst(data.enableThirst);
                modifiedManually = true;
            }

            // Food values sync
            float fM = Float.parseFloat(data.fruitMultiplier.trim());
            if (fM != foodConfig.getFruitMultiplier()) {
                foodConfig.setFruitMultiplier(fM);
                modifiedManually = true;
            }

            float cT = Float.parseFloat(data.canteenThirst.trim());
            Float currentCT = foodConfig.getItemThirstRestoration("AquaThirstHunger_Canteen");
            if (currentCT == null || cT != currentCT) {
                foodConfig.setItemThirstRestoration("AquaThirstHunger_Canteen", cT);
                modifiedManually = true;
            }

            float cpT = Float.parseFloat(data.canteenProThirst.trim());
            Float currentCPT = foodConfig.getItemThirstRestoration("AquaThirstHunger_Canteenpro_Empty");
            if (currentCPT == null || cpT != currentCPT) {
                foodConfig.setItemThirstRestoration("AquaThirstHunger_Canteenpro_Empty", cpT);
                modifiedManually = true;
            }

            float bbT = Float.parseFloat(data.basicBottleThirst.trim());
            Float currentBBT = foodConfig.getItemThirstRestoration("AquaThirstHunger_BasicBottle_Empty");
            if (currentBBT == null || bbT != currentBBT) {
                foodConfig.setItemThirstRestoration("AquaThirstHunger_BasicBottle_Empty", bbT);
                modifiedManually = true;
            }

        } catch (Exception ignored) {
        }

        if ("close".equals(data.action)) {
            if (modifiedManually)
                config.setActivePreset("CUSTOM");
            configManager.save();
            AquaThirstHunger.get().syncHUDs();
            player.getPageManager().setPage(ref, store, Page.None);
            return;
        }

        if (data.action != null && data.action.startsWith("PRESET:")) {
            String preset = data.action.substring(7);
            applyPreset(preset);
            config.setActivePreset(preset);
            configManager.save();
            AquaThirstHunger.get().syncHUDs();

            player.getPageManager().openCustomPage(ref, store,
                    new AquaThirstConfigPage(this.playerRef, Objects.requireNonNull(configManager)));
            this.playerRef.sendMessage(Message.empty().insert("Thirst Preset applied: ")
                    .insert(Objects.requireNonNull(preset)).color("#00FFAA"));
            return;
        }

        if (modifiedManually)
            config.setActivePreset("CUSTOM");
        configManager.save();
    }

    private void applyPreset(String preset) {
        HHMThirstConfig config = configManager.getThirstConfig();
        HHMThirstFoodValuesConfig foodConfig = configManager.getThirstFoodValuesConfig();

        switch (preset) {
            case "EASY":
                config.setMaxThirst(100.0f);
                config.setEnableThirst(true);
                config.setResetThirstOnDeath(false);
                config.setRespawnThirstLevel(80.0f);
                config.setDepletionTickRate(2.0f);
                config.setDepletionPerTick(0.01f);
                config.setDepletionPerBlockHit(0.01f);
                config.setSprintDepletionModifier(0.05f);
                config.setDehydrationDamage(1.0f);
                config.setDamageIntervalSeconds(5.0f);
                config.setStaminaBoostThreshold(70.0f);

                foodConfig.setFruitMultiplier(10.0f);
                foodConfig.setItemThirstRestoration("AquaThirstHunger_Canteen", 15.0f);
                foodConfig.setItemThirstRestoration("AquaThirstHunger_Canteenpro_Empty", 20.0f);
                foodConfig.setItemThirstRestoration("AquaThirstHunger_BasicBottle_Empty", 20.0f);
                break;
            case "NORMAL":
                config.setMaxThirst(100.0f);
                config.setEnableThirst(true);
                config.setResetThirstOnDeath(true);
                config.setRespawnThirstLevel(50.0f);
                config.setDepletionTickRate(2.0f);
                config.setDepletionPerTick(0.05f);
                config.setDepletionPerBlockHit(0.05f);
                config.setSprintDepletionModifier(0.1f);
                config.setDehydrationDamage(2.0f);
                config.setDamageIntervalSeconds(4.0f);
                config.setStaminaBoostThreshold(90.0f);

                foodConfig.setFruitMultiplier(5.0f);
                foodConfig.setItemThirstRestoration("AquaThirstHunger_Canteen", 14.0f);
                foodConfig.setItemThirstRestoration("AquaThirstHunger_Canteenpro_Empty", 16.0f);
                foodConfig.setItemThirstRestoration("AquaThirstHunger_BasicBottle_Empty", 16.0f);
                break;
            case "HARD":
                config.setMaxThirst(100.0f);
                config.setEnableThirst(true);
                config.setResetThirstOnDeath(true);
                config.setRespawnThirstLevel(20.0f);
                config.setDepletionTickRate(1.0f);
                config.setDepletionPerTick(0.125f);
                config.setDepletionPerBlockHit(0.1f);
                config.setSprintDepletionModifier(0.2f);
                config.setDehydrationDamage(5.0f);
                config.setDamageIntervalSeconds(2.0f);
                config.setStaminaBoostThreshold(100.0f);

                foodConfig.setFruitMultiplier(3.5f);
                foodConfig.setItemThirstRestoration("AquaThirstHunger_Canteen", 8.0f);
                foodConfig.setItemThirstRestoration("AquaThirstHunger_Canteenpro_Empty", 10.0f);
                foodConfig.setItemThirstRestoration("AquaThirstHunger_BasicBottle_Empty", 10.0f);
                break;
        }
    }
}
