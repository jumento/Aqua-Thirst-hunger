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
import mx.jume.aquahunger.compat.LangManager;
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
        String lang = playerRef.getLanguage();
        try {
            AquaThirstHunger.get().reloadConfig();

            playerRef.sendMessage(Message.empty().insert(LangManager.getForLanguage(lang, "command.reload.success")));

        } catch (Exception e) {
            String failMsg = String.format(LangManager.getForLanguage(lang, "command.reload.failure"), e.getMessage());
            playerRef.sendMessage(Message.empty().insert(failMsg));
            // AquaThirstHunger.LOGGER.at(java.util.logging.Level.SEVERE).withCause(e).log("Error reloading config");
        }
    }
}
