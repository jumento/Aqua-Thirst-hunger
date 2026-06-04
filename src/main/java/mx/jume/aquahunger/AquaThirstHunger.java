package mx.jume.aquahunger;

import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import mx.jume.aquahunger.assets.FoodValue;
import mx.jume.aquahunger.assets.HungryAssetRegistryLoader;
import mx.jume.aquahunger.commands.*;
import mx.jume.aquahunger.components.HungerComponent;
import mx.jume.aquahunger.config.HHMHungerConfig;
import mx.jume.aquahunger.config.HHMFoodValuesConfig;
import mx.jume.aquahunger.events.GameModePacketWatcher;
import mx.jume.aquahunger.events.HHMPlayerReady;
import mx.jume.aquahunger.interactions.FailedFeedingInteraction;
import mx.jume.aquahunger.interactions.FeedInteraction;
import mx.jume.aquahunger.interactions.StartFeedingInteraction;
import mx.jume.aquahunger.systems.OnBlockHitSystem;
import mx.jume.aquahunger.systems.OnDeathSystem;
import mx.jume.aquahunger.systems.StarveSystem;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import javax.annotation.Nonnull;

import java.util.Set;
import java.util.logging.Level;

public class AquaThirstHunger extends JavaPlugin {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static AquaThirstHunger instance;

    private mx.jume.aquahunger.config.ConfigManager configManager;
    private ComponentType<EntityStore, HungerComponent> hungerComponentType;
    private ComponentType<EntityStore, mx.jume.aquahunger.components.ThirstComponent> thirstComponentType;

    public AquaThirstHunger(@NonNullDecl JavaPluginInit init) {
        super(init);
        instance = this;
    }

    @Override
    protected void setup() {
        super.setup();

        this.configManager = new mx.jume.aquahunger.config.ConfigManager();
        this.configManager.load();
        //logDebug("Configuration loaded.");

        mx.jume.aquahunger.compat.NotifiedPlayersManager.load(java.nio.file.Paths.get("mods/Aqua-Thirst-hunger"));

        final var entityStoreRegistry = this.getEntityStoreRegistry();
        this.hungerComponentType = entityStoreRegistry.registerComponent(HungerComponent.class, "HungerComponent", HungerComponent.CODEC);
        this.thirstComponentType = entityStoreRegistry.registerComponent(mx.jume.aquahunger.components.ThirstComponent.class, "ThirstComponent", mx.jume.aquahunger.components.ThirstComponent.CODEC);

        entityStoreRegistry.registerSystem(StarveSystem.create());
        entityStoreRegistry.registerSystem(mx.jume.aquahunger.systems.ThirstSystem.create());
        entityStoreRegistry.registerSystem(new OnBlockHitSystem());
        entityStoreRegistry.registerSystem(new OnDeathSystem());
        entityStoreRegistry.registerSystem(new mx.jume.aquahunger.systems.HungerLifeSystem());

        final var interactionRegistry = this.getCodecRegistry(Interaction.CODEC);
        interactionRegistry.register("Hungry_Feed", FeedInteraction.class, FeedInteraction.CODEC);
        interactionRegistry.register("Hungry_Start_Feeding", StartFeedingInteraction.class, StartFeedingInteraction.CODEC);
        interactionRegistry.register("Hungry_Failed_Feeding", FailedFeedingInteraction.class, FailedFeedingInteraction.CODEC);
        interactionRegistry.register("AquaThirstHunger_DrinkWater", FeedInteraction.class, FeedInteraction.CODEC);

        this.getEventRegistry().registerGlobal(PlayerReadyEvent.class, HHMPlayerReady::handle);
        this.getEventRegistry().registerGlobal(
                com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent.class,
                mx.jume.aquahunger.events.HHMPlayerDisconnect::handle);
        this.getEventRegistry().register(LoadedAssetsEvent.class, FoodValue.class, FoodValue::onItemAssetLoad);

        //logDebug("Registering events...");

        PacketAdapters.registerOutbound(new GameModePacketWatcher());

        this.getCommandRegistry().registerCommand(new HungryCommand());
        this.getCommandRegistry().registerCommand(new ThirstyCommand());
        this.getCommandRegistry().registerCommand(new AquaCheffCommand());
        this.getCommandRegistry().registerCommand(new AquaCheffBarsCommand());
        this.getCommandRegistry().registerCommand(new AquaHungerConfigCommand());
        this.getCommandRegistry().registerCommand(new AquaThirstConfigCommand());
        this.getCommandRegistry().registerCommand(new AquaCheffConfigCommand());
        this.getCommandRegistry().registerCommand(new HungryReloadCommand());
        this.getCommandRegistry().registerCommand(new mx.jume.aquahunger.commands.AquaHungerOkCommand());
        this.getCommandRegistry().registerCommand(new mx.jume.aquahunger.commands.AquaHungerResetCommand());
    }

    @Override
    protected void start() {
        super.start();

        mx.jume.aquahunger.compat.IntegrationManager.init();

        if (this.getHungerConfig().isSinglePlayer()) {
            final Set<String> permissions = Set.of(
                    HungryCommand.requiredPermission,
                    ThirstyCommand.requiredPermission,
                    HungryReloadCommand.requiredPermission);
            PermissionsModule.get().addGroupPermission("Adventure", permissions);
            PermissionsModule.get().addGroupPermission("Creative", permissions);
            //logDebug("Singleplayer module detected, added permissions to Adventure and Creative groups.");
        }
    }

    public ComponentType<EntityStore, HungerComponent> getHungerComponentType() { return this.hungerComponentType; }
    public ComponentType<EntityStore, mx.jume.aquahunger.components.ThirstComponent> getThirstComponentType() { return this.thirstComponentType; }
    public HHMHungerConfig getHungerConfig() { return this.configManager.getHungerConfig(); }
    public mx.jume.aquahunger.config.HHMThirstConfig getThirstConfig() { return this.configManager.getThirstConfig(); }
    public void saveHungerConfig() { this.configManager.save(); }
    public HHMFoodValuesConfig getFoodValuesConfig() { return this.configManager.getFoodValuesConfig(); }
    public mx.jume.aquahunger.config.HHMExternalFoodsConfig getExternalFoodsConfig() { return this.configManager.getExternalFoodsConfig(); }
    public mx.jume.aquahunger.config.HHMThirstFoodValuesConfig getThirstFoodValuesConfig() { return this.configManager.getThirstFoodValuesConfig(); }
    public mx.jume.aquahunger.config.ConfigManager getConfigManager() { return this.configManager; }

    public void reloadConfig() throws Exception {
        this.configManager.reload();
        syncHUDs();
    }

    public void syncHUDs() {
        mx.jume.aquahunger.ui.HHMHud.refreshAllHuds(getHungerConfig());
        mx.jume.aquahunger.ui.HHMThirstHud.refreshAllHuds(getThirstConfig());
    }

    public static AquaThirstHunger get() { return instance; }

    public boolean isRpgLevelingPresent() {
        if (checkClass("org.zuxaw.plugin.api.RPGLevelingAPI")) return true;
        if (checkClass("com.zuxaw.rpgleveling.api.RPGLevelingAPI")) return true;
        return false;
    }

    public boolean isEndlessLevelingPresent() {
        return checkClass("com.airijko.endlessleveling.api.EndlessLevelingAPI");
    }

    public boolean isHytaleMMOPresent() {
        return checkClass("com.ziggfreed.mmoskilltree.api.MMOSkillTreeAPI");
    }

    private boolean checkClass(String className) {
        try {
            Class.forName(className);
            //logDebug("Found class: " + className);
            return true;
        } catch (ClassNotFoundException e) {
            try {
                Thread.currentThread().getContextClassLoader().loadClass(className);
                //logDebug("Found class via CtxCL: " + className);
                return true;
            } catch (ClassNotFoundException e2) {
                return false;
            }
        }
    }

    public boolean isAquaSanityPresent() {
        return checkClass("mx.jume.aquasanity.AquaSanity");
    }

    public static void logInfo(String message) {
        // LOGGER.at(Level.INFO).log(message);
    }

    public static void logWarning(String message) {
        // LOGGER.at(Level.WARNING).log(message);
    }

    public static void logSevere(String message) {
        // LOGGER.at(Level.SEVERE).log(message);
    }

    public static void logDebug(String message) {
        if (instance != null && instance.getThirstConfig().isEnableDebugLogs()) {
            String tag = message.startsWith("[") ? "" : "[aquathirst] ";
            // LOGGER.at(Level.INFO).log(tag + "[debug] " + message);
        }
    }

    static {
        HungryAssetRegistryLoader.registerAssets();
    }
}
