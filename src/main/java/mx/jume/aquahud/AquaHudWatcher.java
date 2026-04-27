package mx.jume.aquahud;

import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.interface_.CustomHud;
import com.hypixel.hytale.protocol.packets.interface_.CustomUICommand;
import com.hypixel.hytale.protocol.packets.interface_.CustomUICommandType;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketWatcher;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.logger.HytaleLogger;

import java.util.logging.Level;

/**
 * PlayerPacketWatcher que protege contra paquetes clear=true de terceros.
 * Solo activo en modo COORDINATED.
 */
public class AquaHudWatcher implements PlayerPacketWatcher {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static volatile boolean registered = false;

    public static void ensureRegistered() {
        if (registered) return;
        registered = true;
        PacketAdapters.registerOutbound(new AquaHudWatcher());
        // LOGGER.at(Level.INFO).log("[AquaHudWatcher] Registered");
    }

    @Override
    public void accept(PlayerRef playerRef, Packet packet) {
        if (AquaHudBridge.getMode() != AquaHudBridge.Mode.COORDINATED) return;

        if (!(packet instanceof CustomHud customHud)) return;

        if (!customHud.clear) return;

        // Verificar si el paquete es nuestro (contiene MultipleHUD.ui)
        CustomUICommand[] cmds = customHud.commands;
        if (cmds != null) {
            for (CustomUICommand cmd : cmds) {
                if (cmd == null || cmd.type != CustomUICommandType.Append || cmd.text == null)
                    continue;
                if (cmd.text.contains("MultipleHUD.ui")) {
                    return; // Paquete nuestro (show()) — no intervenir
                }
            }
        }

        // Es clear=true de tercero
        AquaMultiHUD container = AquaMultiHUD.getContainer(playerRef);
        if (container == null || !container.isActivated() || container.player == null) {
            // No hay container activo — solo convertir clear=false y reinject si posible
            customHud.clear = false;
            if (container != null) {
                UICommandBuilder reinjectBuilder = new UICommandBuilder();
                container.reinject(reinjectBuilder);
                container.update(false, reinjectBuilder);
            }
            return;
        }

        // Container activo — detectar si el tercero tomó el slot
        CustomUIHud currentHud = container.player.getHudManager().getCustomHud();

        if (currentHud != null && currentHud != container) {
            // El tercero tomó el slot (show() del engine llama setCustomHud internamente).
            String adoptId = "Adopted_" + currentHud.getClass().getSimpleName();
            boolean alreadyAdopted = container.customHuds.containsKey(adoptId);

            // 1. Neutralizar el paquete del tercero
            customHud.clear = false;
            customHud.commands = new CustomUICommand[0];

            if (!alreadyAdopted) {
                // PRIMER TAKEOVER: adoptar + full reclaim (un solo clear=true, necesario)
                container.adoptHud(adoptId, currentHud);
                container.player.getHudManager().setCustomHud(container.getPlayerRef(), container);
                // LOGGER.at(Level.INFO).log("[AquaHudWatcher] FIRST TAKEOVER — adopted "
                        // + currentHud.getClass().getSimpleName() + " and reclaimed slot for: "
                        // + playerRef.getUsername());
            } else {
                // TAKEOVER REPETIDO: reclaim silencioso + data-only update
                // NO llamar setCustomHud — genera clear=true y causa flicker.
                container.reclaimSlotSilently();
                UICommandBuilder builder = new UICommandBuilder();
                for (String identifier : container.customHuds.keySet()) {
                    if (identifier.startsWith("Adopted_")) {
                        String normalizedId = container.normalizedIds.get(identifier);
                        CustomUIHud hud = container.customHuds.get(identifier);
                        if (normalizedId != null && hud != null) {
                            AquaMultiHUD.updateAdoptedHudData(builder, normalizedId, hud);
                        }
                    }
                }
                container.update(false, builder);
                // LOGGER.at(Level.INFO).log("[AquaHudWatcher] Repeated TAKEOVER — silent reclaim + data update for: "
                        // + playerRef.getUsername());
            }
        } else {
            // SIN TAKEOVER: clear=true de tercero sin cambio de slot (caso raro).
            customHud.clear = false;
            customHud.commands = new CustomUICommand[0];
            UICommandBuilder builder = new UICommandBuilder();
            for (String identifier : container.customHuds.keySet()) {
                if (identifier.startsWith("Adopted_")) {
                    String normalizedId = container.normalizedIds.get(identifier);
                    CustomUIHud hud = container.customHuds.get(identifier);
                    if (normalizedId != null && hud != null) {
                        AquaMultiHUD.updateAdoptedHudData(builder, normalizedId, hud);
                    }
                }
            }
            container.update(false, builder);
        }

        // LOGGER.at(Level.INFO).log("[AquaHudWatcher] Converted clear=true -> clear=false for: "
                // + playerRef.getUsername());
    }
}
