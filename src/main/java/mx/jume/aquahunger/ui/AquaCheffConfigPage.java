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
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import mx.jume.aquahunger.AquaThirstHunger;
import mx.jume.aquahunger.config.ConfigManager;
import mx.jume.aquahunger.config.HHMHungerConfig;
import mx.jume.aquahunger.config.HHMThirstConfig;
import mx.jume.aquahunger.config.HHMThirstFoodValuesConfig;
import com.hypixel.hytale.server.core.Message;

import javax.annotation.Nonnull;
import java.util.Objects;

public class AquaCheffConfigPage extends InteractiveCustomUIPage<AquaCheffConfigPage.PageData> {

    private final PlayerRef playerRef;

    public static class PageData {
        public String action;

        public static final BuilderCodec<PageData> CODEC = BuilderCodec
                .builder(PageData.class, PageData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING), (o, v) -> o.action = v, o -> o.action)
                .add()
                .build();

        @Nonnull
        public static final BuilderCodec<PageData> CODEC_NONNULL = Objects.requireNonNull(CODEC);
    }

    public AquaCheffConfigPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, PageData.CODEC_NONNULL);
        this.playerRef = playerRef;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt,
            @Nonnull Store<EntityStore> store) {
        cmd.append("Pages/AquaCheffConfigUI.ui");

        // Bind events to Buttons
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#BtnEasy",
                new com.hypixel.hytale.server.core.ui.builder.EventData().append("Action", "EASY"));
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#BtnNormal",
                new com.hypixel.hytale.server.core.ui.builder.EventData().append("Action", "NORMAL"));
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#BtnHard",
                new com.hypixel.hytale.server.core.ui.builder.EventData().append("Action", "HARD"));
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#BtnCustom",
                new com.hypixel.hytale.server.core.ui.builder.EventData().append("Action", "custom"));
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#BtnClose",
                new com.hypixel.hytale.server.core.ui.builder.EventData().append("Action", "close"));
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
            @Nonnull PageData data) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null)
            return;

        if (data.action == null)
            return;

        ConfigManager configManager = AquaThirstHunger.get().getConfigManager();

        switch (data.action) {
            case "EASY":
            case "NORMAL":
            case "HARD":
                applyFullPreset(data.action, configManager);
                try {
                    AquaThirstHunger.get().reloadConfig();
                    this.playerRef.sendMessage(Message.empty().insert("System Reloaded Successfully."));
                } catch (Exception e) {
                    this.playerRef.sendMessage(Message.empty().insert("Reload Failed: " + e.getMessage()));
                }
                player.getPageManager().setPage(ref, store, Page.None);
                break;
            case "custom":
                player.getPageManager().openCustomPage(ref, store, new AquaCheffCustomPage(this.playerRef));
                break;
            case "close":
                player.getPageManager().setPage(ref, store, Page.None);
                break;
        }
    }

    private void applyFullPreset(String preset, ConfigManager configManager) {
        HHMHungerConfig hunger = configManager.getHungerConfig();
        HHMThirstConfig thirst = configManager.getThirstConfig();
        HHMThirstFoodValuesConfig food = configManager.getThirstFoodValuesConfig();

        hunger.applyPreset(preset);
        thirst.applyPreset(preset, food);

        configManager.save();
    }
}
