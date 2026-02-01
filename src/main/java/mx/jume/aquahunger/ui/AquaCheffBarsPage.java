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
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import mx.jume.aquahunger.AquaThirstHunger;
import mx.jume.aquahunger.config.HudPosition;

import javax.annotation.Nonnull;

public class AquaCheffBarsPage extends InteractiveCustomUIPage<AquaCheffBarsPage.BarsEventData> {

    public static class BarsEventData {
        public String action;
        public String value;

        public static final BuilderCodec<BarsEventData> CODEC = BuilderCodec
                .builder(BarsEventData.class, BarsEventData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING), (o, v) -> o.action = v, o -> o.action).add()
                .append(new KeyedCodec<>("Value", Codec.STRING), (o, v) -> o.value = v, o -> o.value).add()
                .build();
    }

    public AquaCheffBarsPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, BarsEventData.CODEC);
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt,
            @Nonnull Store<EntityStore> store) {
        cmd.append("Pages/AquaCheffBarsUI.ui");

        HudPosition hungerPos = AquaThirstHunger.get().getHungerConfig().getHudPosition();
        HudPosition thirstPos = AquaThirstHunger.get().getThirstConfig().getHudPosition();

        String hungerActive = (hungerPos instanceof HudPosition.Preset p) ? p.name() : null;
        String thirstActive = (thirstPos instanceof HudPosition.Preset p) ? p.name() : null;

        for (HudPosition.Preset preset : HudPosition.Preset.values()) {
            String name = preset.name();

            // Hunger Buttons
            String hId = "#H_" + name;
            evt.addEventBinding(CustomUIEventBindingType.Activating, hId,
                    new EventData().append("Action", "set_hunger").append("Value", name));

            if (name.equals(hungerActive)) {
                cmd.setObject(hId + ".Style", "@ActiveButtonStyle");
            }

            // Thirst Buttons
            String tId = "#T_" + name;
            evt.addEventBinding(CustomUIEventBindingType.Activating, tId,
                    new EventData().append("Action", "set_thirst").append("Value", name));

            if (name.equals(thirstActive)) {
                cmd.setObject(tId + ".Style", "@ActiveButtonStyle");
            }
        }

        evt.addEventBinding(CustomUIEventBindingType.Activating, "#CancelButton",
                new EventData().append("Action", "cancel"));
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
            @Nonnull BarsEventData data) {
        Player player = store.getComponent(ref, Player.getComponentType());

        if ("cancel".equals(data.action)) {
            player.getPageManager().setPage(ref, store, Page.None);
            return;
        }

        if (data.action != null && data.action.startsWith("set_")) {
            boolean isHunger = data.action.equals("set_hunger");
            HudPosition newPos = HudPosition.valueOf(data.value);

            if (newPos != null) {
                AquaThirstHunger mod = AquaThirstHunger.get();
                if (isHunger) {
                    mod.getHungerConfig().setHudPosition(newPos);
                } else {
                    mod.getThirstConfig().setHudPosition(newPos);
                }
                mod.saveHungerConfig();

                for (World world : Universe.get().getWorlds().values()) {
                    world.execute(() -> {
                        world.getPlayerRefs().forEach((pRef) -> {
                            if (isHunger) {
                                HHMHud.updatePlayerHudPosition(pRef, newPos);
                            } else {
                                HHMThirstHud.updatePlayerHudPosition(pRef, newPos);
                            }
                        });
                    });
                }

                // Refresh UI
                player.getPageManager().openCustomPage(ref, store, this);
            }
        }
    }
}
