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
import mx.jume.aquahunger.config.ConfigManager;
import mx.jume.aquahunger.config.HHMExternalFoodsConfig;

import javax.annotation.Nonnull;

/**
 * AquaCheffPage - Interactive UI for editing external food configuration.
 */
public class AquaCheffPage extends InteractiveCustomUIPage<AquaCheffPage.ConfigEventData> {

    private final ConfigManager configManager;

    /**
     * Event data structure for handling UI interactions.
     * Contains the action performed and values from all input fields.
     */
    public static class ConfigEventData {
        public String action; // Button action: "save" or "cancel"
        public String itemId; // Selected item ID
        public String hungerValue; // Hunger value as string
        public String saturationValue; // Saturation value as string
        public String thirstValue; // Thirst value as string

        public static final BuilderCodec<ConfigEventData> CODEC = BuilderCodec
                .builder(ConfigEventData.class, ConfigEventData::new)
                .append(
                        new KeyedCodec<>("Action", Codec.STRING),
                        (ConfigEventData o, String v) -> o.action = v,
                        (ConfigEventData o) -> o.action)
                .add()
                .append(
                        new KeyedCodec<>("@ItemId", Codec.STRING),
                        (ConfigEventData o, String v) -> o.itemId = v,
                        (ConfigEventData o) -> o.itemId)
                .add()
                .append(
                        new KeyedCodec<>("@HungerValue", Codec.STRING),
                        (ConfigEventData o, String v) -> o.hungerValue = v,
                        (ConfigEventData o) -> o.hungerValue)
                .add()
                .append(
                        new KeyedCodec<>("@SaturationValue", Codec.STRING),
                        (ConfigEventData o, String v) -> o.saturationValue = v,
                        (ConfigEventData o) -> o.saturationValue)
                .add()
                .append(
                        new KeyedCodec<>("@ThirstValue", Codec.STRING),
                        (ConfigEventData o, String v) -> o.thirstValue = v,
                        (ConfigEventData o) -> o.thirstValue)
                .add()
                .build();
    }

    public AquaCheffPage(@Nonnull PlayerRef playerRef, @Nonnull ConfigManager configManager) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, ConfigEventData.CODEC);
        this.configManager = configManager;
    }

    @Override
    public void build(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull UICommandBuilder cmd,
            @Nonnull UIEventBuilder evt,
            @Nonnull Store<EntityStore> store) {
        // Load the UI definition file
        cmd.append("Pages/AquaCheffUI.ui");

        // Attempt to pre-populate Item ID using reflection to avoid hard dependency on
        // unimported classes
        // Pre-populate Item ID using reflection based on debug findings
        try {
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player != null) {
                java.lang.reflect.Method getInventory = player.getClass().getMethod("getInventory");
                Object inventory = getInventory.invoke(player);

                if (inventory != null) {
                    // Use 'getItemInHand' as identified
                    Object itemStack = inventory.getClass().getMethod("getItemInHand").invoke(inventory);

                    if (itemStack != null) {
                        // Check if not empty
                        boolean isEmpty = false;
                        try {
                            isEmpty = (boolean) itemStack.getClass().getMethod("isEmpty").invoke(itemStack);
                        } catch (Exception ignored) {
                        }

                        if (!isEmpty) {
                            // Get Item
                            Object item = itemStack.getClass().getMethod("getItem").invoke(itemStack);
                            if (item != null) {
                                // Get ID
                                String id = (String) item.getClass().getMethod("getId").invoke(item);
                                if (id != null) {
                                    cmd.set("#ItemIdInput.Value", id);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            mx.jume.aquahunger.AquaThirstHunger
                    .logInfo("Info: Could not auto-fill held item ID (Reflection error: " + e.getMessage() + ")");
        }

        // Bind the Save button event
        evt.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#SaveButton",
                new EventData()
                        .append("Action", "save")
                        .append("@ItemId", "#ItemIdInput.Value")
                        .append("@HungerValue", "#HungerInput.Value")
                        .append("@SaturationValue", "#SaturationInput.Value")
                        .append("@ThirstValue", "#ThirstInput.Value"));

        // Bind the Cancel button event
        evt.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#CancelButton",
                new EventData()
                        .append("Action", "cancel"));
    }

    @Override
    public void handleDataEvent(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull ConfigEventData data) {
        Player player = store.getComponent(ref, Player.getComponentType());

        switch (data.action) {
            case "save":
                handleSave(ref, store, player, data);
                break;

            case "cancel":
                handleCancel(ref, store, player);
                break;
        }
    }

    private void handleSave(Ref<EntityStore> ref, Store<EntityStore> store, Player player, ConfigEventData data) {
        try {
            // Handle blank ID by saving a template instead of erroring out (To prevent UI
            // hang and provide a guide)
            String finalId = (data.itemId == null || data.itemId.trim().isEmpty())
                    ? "Andiechef_New_Food_Template"
                    : data.itemId.trim();

            if (data.itemId == null || data.itemId.trim().isEmpty()) {
                playerRef.sendMessage(Message.empty().insert("Item ID was blank. Using template: ").insert(finalId));
            }

            // Fallback to 0.0 for empty fields to avoid "hanging" the UI/Config
            float hungerVal = 0.0f;
            float saturationVal = 0.0f;
            float thirstVal = 0.0f;

            // Parse values with silent fallback
            if (data.hungerValue != null && !data.hungerValue.trim().isEmpty()) {
                try {
                    hungerVal = Float.parseFloat(data.hungerValue.trim());
                } catch (NumberFormatException ignored) {
                }
            }

            if (data.saturationValue != null && !data.saturationValue.trim().isEmpty()) {
                try {
                    saturationVal = Float.parseFloat(data.saturationValue.trim());
                } catch (NumberFormatException ignored) {
                }
            }

            if (data.thirstValue != null && !data.thirstValue.trim().isEmpty()) {
                try {
                    thirstVal = Float.parseFloat(data.thirstValue.trim());
                } catch (NumberFormatException ignored) {
                }
            }

            // Save the configuration
            configManager.getExternalFoodsConfig().updateItem(finalId, hungerVal, saturationVal, thirstVal);
            configManager.save();

            // Success feedback
            playerRef.sendMessage(Message.empty().insert("Configuration saved for: ").insert(finalId));
            playerRef.sendMessage(
                    Message.empty().insert("Values: [" + hungerVal + ", " + saturationVal + ", " + thirstVal + "]"));

            // Guaranteed Close
            player.getPageManager().setPage(ref, store, Page.None);

        } catch (Exception e) {
            playerRef.sendMessage(Message.empty().insert("Error saving configuration: " + e.getMessage()));
            player.getPageManager().setPage(ref, store, Page.None);
        }
    }

    private void handleCancel(Ref<EntityStore> ref, Store<EntityStore> store, Player player) {
        playerRef.sendMessage(Message.empty().insert("Configuration editor closed."));
        player.getPageManager().setPage(ref, store, Page.None);
    }
}
