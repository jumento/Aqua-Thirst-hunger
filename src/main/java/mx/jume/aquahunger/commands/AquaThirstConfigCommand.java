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
import mx.jume.aquahunger.ui.AquaThirstConfigPage;

import javax.annotation.Nonnull;

/**
 * Command to open the Aqua Thirst config UI.
 */
public class AquaThirstConfigCommand extends AbstractPlayerCommand {
    public static final String requiredPermission = "aquathirst.command.config";

    public AquaThirstConfigCommand() {
        super("aquathirstconfig", "Opens the Aqua Thirst configuration UI", false);
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

        AquaThirstConfigPage page = new AquaThirstConfigPage(playerRef, AquaThirstHunger.get().getConfigManager());
        player.getPageManager().openCustomPage(ref, store, page);
    }
}
