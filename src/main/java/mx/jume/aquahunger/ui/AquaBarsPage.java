package mx.jume.aquahunger.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
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
import mx.jume.aquahunger.config.HudPosition;
import javax.annotation.Nonnull;

public class AquaBarsPage extends InteractiveCustomUIPage<AquaBarsPage.BindingData> {

    public AquaBarsPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, BindingData.CODEC);
    }

    public static class BindingData {
        public String action;
        public static final BuilderCodec<BindingData> CODEC = BuilderCodec.builder(BindingData.class, BindingData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING), (o, v) -> o.action = v, o -> o.action).add()
                .build();
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder uiCommandBuilder,
            @Nonnull UIEventBuilder uiEventBuilder, @Nonnull Store<EntityStore> store) {
        uiCommandBuilder.append("Pages/AquaBarsUI.ui");

        // Load current positions from config
        HudPosition currentHungerPos = AquaThirstHunger.get().getHungerConfig().getHudPosition();
        HudPosition currentThirstPos = AquaThirstHunger.get().getThirstConfig().getHudPosition();

        // Set initial text values showing current positions
        String lang = this.playerRef.getLanguage();
        uiCommandBuilder.set("#TextHunger.Text",
                String.format(LangManager.getForLanguage(lang, "ui.bars.current"), currentHungerPos.name()));
        uiCommandBuilder.set("#TextThirst.Text",
                String.format(LangManager.getForLanguage(lang, "ui.bars.current"), currentThirstPos.name()));

        // Loop binding matching the user's manual ID format
        for (HudPosition.Preset preset : HudPosition.Preset.values()) {
            String hungerBtnId = "#Hunger" + preset.name();
            uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, hungerBtnId,
                    new EventData().append("Action", "SET_HUNGER:" + preset.name()));

            String thirstBtnId = "#Thirst" + preset.name();
            uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, thirstBtnId,
                    new EventData().append("Action", "SET_THIRST:" + preset.name()));
        }

        // Close Button
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CancelButton",
                new EventData().append("Action", "CLOSE"));
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BackButton",
                new EventData().append("Action", "BACK"));
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
            @Nonnull BindingData data) {
        if (data.action == null)
            return;

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null)
            return;

        if ("CLOSE".equals(data.action)) {
            player.getPageManager().setPage(ref, store, com.hypixel.hytale.protocol.packets.interface_.Page.None);
            return;
        }

        if ("BACK".equals(data.action)) {
            player.getPageManager().openCustomPage(ref, store, new AquaCheffCustomPage(this.playerRef));
            return;
        }

        boolean changed = false;
        String[] parts = data.action.split(":");

        if (parts.length == 2) {
            String type = parts[0];
            String presetName = parts[1];
            HudPosition newPos = HudPosition.Preset.valueOf(presetName);

            if ("SET_HUNGER".equals(type)) {
                AquaThirstHunger.get().getHungerConfig().setHudPosition(newPos);
                changed = true;
            } else if ("SET_THIRST".equals(type)) {
                AquaThirstHunger.get().getThirstConfig().setHudPosition(newPos);
                changed = true;
            }
        }

        if (changed) {
            String lang = this.playerRef.getLanguage();
            // Immediate save and reload on every click; the reload rebuilds every HUD in place.
            AquaThirstHunger.get().saveHungerConfig();
            try {
                AquaThirstHunger.get().reloadConfig();
                this.playerRef.sendMessage(Message.empty()
                        .insert(LangManager.getForLanguage(lang, "ui.bars.applied"))
                        .color("#55FF55"));
            } catch (Exception e) {
                this.playerRef.sendMessage(Message.empty()
                        .insert(String.format(LangManager.getForLanguage(lang, "ui.bars.error"), e.getMessage())));
            }

            // Close the page automatically
            player.getPageManager().setPage(ref, store, com.hypixel.hytale.protocol.packets.interface_.Page.None);
        }
    }
}
