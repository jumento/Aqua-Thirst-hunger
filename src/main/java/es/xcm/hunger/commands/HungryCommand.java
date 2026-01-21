package es.xcm.hunger.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class HungryCommand extends AbstractPlayerCommand {
    public HungryCommand() {
        super("hunger", "Hungry Command", false);
        this.addSubCommand(new SetHungerCommand());
        this.addSubCommand(new HungryHideCommand());
        this.addSubCommand(new HungryShowCommand());
    }

    static String helpMessage = """
        Hungry commands
                    
        /hunger - Show this help message.
        /hunger hide - Hide the Hunger bar.
        /hunger show - Show this Hunger bar.
        /hunger set <level> - Set your hunger level (0-100).
        /hunger set <player> <level> - Set other player hunger level (0-100).
        """;

    @Override
    protected void execute(
            @NonNullDecl CommandContext context,
            @NonNullDecl Store<EntityStore> store,
            @NonNullDecl Ref<EntityStore> ref,
            @NonNullDecl PlayerRef playerRef,
            @NonNullDecl World world
    ) {
        playerRef.sendMessage(Message.raw(helpMessage));
    }
}
