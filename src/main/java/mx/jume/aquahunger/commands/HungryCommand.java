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

import java.util.function.Predicate;

public class HungryCommand extends AbstractPlayerCommand {
    public static final String requiredPermission = "hungry.commmand.base";

    public HungryCommand() {
        super("aquahunger", "Aquahunger Command", false);
        this.addSubCommand(new SetHungerCommand());
        this.addSubCommand(new HungryStatsCommand());
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
        
        message.insert(LangManager.getForLanguage(lang, "command.help.base"));
        
        if (context.sender().hasPermission(SetHungerCommand.requiredPermission)) {
            message.insert("\n").insert(LangManager.getForLanguage(lang, "command.help.set.self"));
        }
        if (context.sender().hasPermission(SetHungerCommand.SetHungerOtherCommand.requiredPermission)) {
            message.insert("\n").insert(LangManager.getForLanguage(lang, "command.help.set.other"));
        }
        message.insert("\n").insert(LangManager.getForLanguage(lang, "command.help.stats"));
        
        playerRef.sendMessage(message);
    }
}
