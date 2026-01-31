package mx.jume.aquahunger.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import mx.jume.aquahunger.AquaThirstHunger;
import mx.jume.aquahunger.ui.AquaCheffPage;

import javax.annotation.Nonnull;

/**
 * Command to open the Aqua Cheff configuration UI.
 * Usage: /aquacheff
 */
public class AquaCheffCommand extends AbstractPlayerCommand {

    public AquaCheffCommand() {
        super("aquacheff", "Opens the Aqua Cheff configuration UI", false);
        this.addSubCommand(new HungryReloadCommand());
    }

    @Override
    protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {

        // If a subcommand was matched, this base execute will NOT be called in Hytale's
        // system
        // providing the parent command is configured to allow arguments.

        Player player = store.getComponent(ref, Player.getComponentType());

        // Open the configuration UI
        AquaCheffPage page = new AquaCheffPage(
                playerRef,
                AquaThirstHunger.get().getConfigManager());

        player.getPageManager().openCustomPage(ref, store, page);
    }
}
