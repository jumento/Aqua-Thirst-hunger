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
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#BtnCustomFoods",
                new com.hypixel.hytale.server.core.ui.builder.EventData().append("Action", "custom_foods"));
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#BtnBarsPosition",
                new com.hypixel.hytale.server.core.ui.builder.EventData().append("Action", "bars_position"));
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#BtnHungerConfig",
                new com.hypixel.hytale.server.core.ui.builder.EventData().append("Action", "hunger_config"));
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#BtnThirstConfig",
                new com.hypixel.hytale.server.core.ui.builder.EventData().append("Action", "thirst_config"));
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

        switch (data.action) {
            case "custom_foods":
                player.getPageManager().openCustomPage(ref, store,
                        new AquaCheffPage(this.playerRef, AquaThirstHunger.get().getConfigManager()));
                break;
            case "bars_position":
                player.getPageManager().openCustomPage(ref, store, new AquaBarsPage(this.playerRef));
                break;
            case "hunger_config":
                player.getPageManager().openCustomPage(ref, store,
                        new AquaHungerConfigPage(this.playerRef, AquaThirstHunger.get().getConfigManager()));
                break;
            case "thirst_config":
                player.getPageManager().openCustomPage(ref, store,
                        new AquaThirstConfigPage(this.playerRef, AquaThirstHunger.get().getConfigManager()));
                break;
            case "close":
                player.getPageManager().setPage(ref, store, Page.None);
                break;
        }
    }
}
