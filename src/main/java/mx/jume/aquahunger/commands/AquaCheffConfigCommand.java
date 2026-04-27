package mx.jume.aquahunger.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import mx.jume.aquahunger.ui.AquaCheffConfigPage;

import javax.annotation.Nonnull;

/**
 * Command to open the Aqua Cheff configuration menu.
 * Only operators should use this command.
 */
public class AquaCheffConfigCommand extends AbstractPlayerCommand {
    public static final String requiredPermission = "aquahunger.command.cheffconfig";

    public AquaCheffConfigCommand() {
        super("aquacheffconfig", "Opens the Aqua Cheff configuration menu", false);
        this.requirePermission(requiredPermission);
    }

    @Override
    protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null)
            return;

        // Additional safety check for OP status if desired, but permission should
        // suffice.
        // Given user request "only operator", we rely on the permission being granted
        // to OPs only.

        AquaCheffConfigPage page = new AquaCheffConfigPage(playerRef);
        player.getPageManager().openCustomPage(ref, store, page);
    }
}
