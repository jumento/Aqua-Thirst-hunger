package mx.jume.aquahunger.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import mx.jume.aquahunger.AquaThirstHunger;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class HungryReloadCommand extends AbstractPlayerCommand {
    public static final String requiredPermission = "aquahunger.command.reload";

    public HungryReloadCommand() {
        super("aquahungerreload", "Reload configurations", true);
        this.requirePermission(requiredPermission);
    }

    @Override
    protected void execute(
            @NonNullDecl CommandContext context,
            @NonNullDecl Store<EntityStore> store,
            @NonNullDecl Ref<EntityStore> ref,
            @NonNullDecl PlayerRef playerRef,
            @NonNullDecl World world) {
        try {
            AquaThirstHunger.get().reloadConfig();

            playerRef.sendMessage(Message.empty().insert("Configuration reloaded successfully."));

        } catch (Exception e) {
            playerRef.sendMessage(Message.empty().insert("Failed to reload configuration: " + e.getMessage()));
            AquaThirstHunger.LOGGER.at(java.util.logging.Level.SEVERE).withCause(e).log("Error reloading config");
        }
    }
}
