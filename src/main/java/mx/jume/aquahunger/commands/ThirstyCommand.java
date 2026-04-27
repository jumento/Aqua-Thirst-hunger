package mx.jume.aquahunger.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import mx.jume.aquahunger.compat.LangManager;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class ThirstyCommand extends AbstractPlayerCommand {
    public static final String requiredPermission = "thirsty.commmand.base";

    public ThirstyCommand() {
        super("aquathirst", "Thirst Command", false);
        this.addSubCommand(new SetThirstCommand());
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
        Message message = Message.empty();

        message.insert(LangManager.getForLanguage(lang, "command.thirst.help.base"));

        if (context.sender().hasPermission(SetThirstCommand.requiredPermission)) {
            message.insert("\n").insert(LangManager.getForLanguage(lang, "command.thirst.help.set.self"));
        }
        if (context.sender().hasPermission(SetThirstCommand.SetThirstOtherCommand.requiredPermission)) {
            message.insert("\n").insert(LangManager.getForLanguage(lang, "command.thirst.help.set.other"));
        }
        playerRef.sendMessage(message);
    }
}
