package es.xcm.hunger.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import es.xcm.hunger.ui.HHMHud;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class HungryHideCommand extends AbstractPlayerCommand {
    public HungryHideCommand() {
        super("hide", "Hide Hungry Bar", false);
    }

    @Override
    protected void execute(
            @NonNullDecl CommandContext context,
            @NonNullDecl Store<EntityStore> store,
            @NonNullDecl Ref<EntityStore> ref,
            @NonNullDecl PlayerRef playerRef,
            @NonNullDecl World world
    ) {
        HHMHud.updatePlayerHudVisibility(playerRef, false);
    }
}
