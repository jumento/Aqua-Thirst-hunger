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
import mx.jume.aquahunger.ui.AquaHungerConfigPage;

import javax.annotation.Nonnull;

/**
 * Command to open the Aqua Hunger config UI.
 */
public class AquaHungerConfigCommand extends AbstractPlayerCommand {
    public static final String requiredPermission = "aquahunger.command.config";

    public AquaHungerConfigCommand() {
        super("aquahungerconfig", "Opens the Aqua Hunger configuration UI", false);
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
        if (player == null) return;

        AquaHungerConfigPage page = new AquaHungerConfigPage(playerRef, AquaThirstHunger.get().getConfigManager());
        player.getPageManager().openCustomPage(ref, store, page);
    }
}
