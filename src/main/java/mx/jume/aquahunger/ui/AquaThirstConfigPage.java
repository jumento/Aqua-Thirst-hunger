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
import mx.jume.aquahunger.compat.LangManager;
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
        cmd.set("#configtype.Text", String.format(
                LangManager.getForLanguage(this.playerRef.getLanguage(), "ui.config.active_preset"),
                config.getActivePreset()));

        cmd.set("#InputMaxThirst.Value", ConfigForm.format(config.getMaxThirst()));
        cmd.set("#InputRespawnThirst.Value", ConfigForm.format(config.getRespawnThirstLevel()));
        cmd.set("#InputDepletionTickRate.Value", ConfigForm.format(config.getDepletionTickRate()));
        cmd.set("#InputBaseThirstDepletion.Value", ConfigForm.format(config.getDepletionPerTick()));
        cmd.set("#InputDepletionPerBlockHit.Value", ConfigForm.format(config.getDepletionPerBlockHit()));
        cmd.set("#InputSprintDepletionModifier.Value", ConfigForm.format(config.getSprintDepletionModifier()));
        cmd.set("#InputDehydrationDamage.Value", ConfigForm.format(config.getDehydrationDamage()));
        cmd.set("#InputDamageIntervalSeconds.Value", ConfigForm.format(config.getDamageIntervalSeconds()));

        cmd.set("#ChkResetDeath #CheckBox.Value", config.isResetThirstOnDeath());
        cmd.set("#ChkEnableThirst #CheckBox.Value", config.isEnableThirst());

        cmd.set("#InputFruitMultiplier.Value", ConfigForm.format(foodConfig.getFruitMultiplier()));
        cmd.set("#InputCanteenThirst.Value", ConfigForm.format(itemThirst(foodConfig, "AquaThirstHunger_Canteen")));
        cmd.set("#InputCanteenProThirst.Value",
                ConfigForm.format(itemThirst(foodConfig, "AquaThirstHunger_Canteenpro_Empty")));
        cmd.set("#InputBasicBottleThirst.Value",
                ConfigForm.format(itemThirst(foodConfig, "AquaThirstHunger_BasicBottle_Empty")));
        cmd.set("#InputStaminaBoostThreshold.Value", ConfigForm.format(config.getStaminaBoostThreshold()));

        evt.addEventBinding(CustomUIEventBindingType.Activating, "#BtnEasy", createSyncData("PRESET:EASY"));
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#BtnNormal", createSyncData("PRESET:NORMAL"));
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#BtnHard", createSyncData("PRESET:HARD"));
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#BtnCancel", createSyncData("close"));
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#BtnBack", createSyncData("back"));
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

        // Each field is applied on its own: one invalid number no longer discards the rest.
        ConfigForm form = new ConfigForm();
        form.number("MaxThirst", data.maxThirst, config.getMaxThirst(), config::setMaxThirst);
        form.number("RespawnThirstLevel", data.respawnThirst, config.getRespawnThirstLevel(), config::setRespawnThirstLevel);
        form.number("DepletionTickRate", data.depletionTickRate, config.getDepletionTickRate(), config::setDepletionTickRate);
        form.number("BaseThirstDepletion", data.baseDepletion, config.getDepletionPerTick(), config::setDepletionPerTick);
        form.number("DepletionPerBlockHit", data.depletionBlockHit, config.getDepletionPerBlockHit(),
                config::setDepletionPerBlockHit);
        form.number("SprintDepletionModifier", data.sprintMod, config.getSprintDepletionModifier(),
                config::setSprintDepletionModifier);
        form.number("DehydrationDamage", data.dehydrationDamage, config.getDehydrationDamage(),
                config::setDehydrationDamage);
        form.number("DamageIntervalSeconds", data.damageInterval, config.getDamageIntervalSeconds(),
                config::setDamageIntervalSeconds);
        form.number("StaminaBoostThreshold", data.staminaBoostThreshold, config.getStaminaBoostThreshold(),
                config::setStaminaBoostThreshold);
        form.bool(data.resetDeath, config.isResetThirstOnDeath(), config::setResetThirstOnDeath);
        form.bool(data.enableThirst, config.isEnableThirst(), config::setEnableThirst);
        form.number("FruitMultiplier", data.fruitMultiplier, foodConfig.getFruitMultiplier(), foodConfig::setFruitMultiplier);
        form.number("AquaThirstHunger_Canteen", data.canteenThirst, itemThirst(foodConfig, "AquaThirstHunger_Canteen"),
                v -> foodConfig.setItemThirstRestoration("AquaThirstHunger_Canteen", v));
        form.number("AquaThirstHunger_Canteenpro_Empty", data.canteenProThirst,
                itemThirst(foodConfig, "AquaThirstHunger_Canteenpro_Empty"),
                v -> foodConfig.setItemThirstRestoration("AquaThirstHunger_Canteenpro_Empty", v));
        form.number("AquaThirstHunger_BasicBottle_Empty", data.basicBottleThirst,
                itemThirst(foodConfig, "AquaThirstHunger_BasicBottle_Empty"),
                v -> foodConfig.setItemThirstRestoration("AquaThirstHunger_BasicBottle_Empty", v));
        config.sanitize();
        form.reportInvalid(this.playerRef);
        boolean modifiedManually = form.isModified();

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

        if (data.action != null && data.action.startsWith("PRESET:")) {
            String preset = data.action.substring(7);
            config.applyPreset(preset, foodConfig);
            configManager.save();
            AquaThirstHunger.get().syncHUDs();

            player.getPageManager().openCustomPage(ref, store,
                    new AquaThirstConfigPage(this.playerRef, Objects.requireNonNull(configManager)));
            this.playerRef.sendMessage(Message.empty()
                    .insert(String.format(LangManager.getForLanguage(this.playerRef.getLanguage(),
                            "ui.config.preset_applied"), Objects.requireNonNull(preset)))
                    .color("#00FFAA"));
            return;
        }

        if (modifiedManually)
            config.setActivePreset("CUSTOM");
        configManager.save();
    }

    private static float itemThirst(HHMThirstFoodValuesConfig foodConfig, String itemId) {
        Float value = foodConfig.getItemThirstRestoration(itemId);
        return value != null ? value : 0.0f;
    }

}
