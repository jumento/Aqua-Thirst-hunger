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
import mx.jume.aquahunger.compat.NotifiedPlayersManager;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class AquaHungerResetCommand extends AbstractPlayerCommand {
    public AquaHungerResetCommand() {
        super("aquahungerreset", "Resetea el estado de notificacion para poder ver el aviso de nuevo", false);
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    @Override
    protected void execute(
            @NonNullDecl CommandContext context,
            @NonNullDecl Store<EntityStore> store,
            @NonNullDecl Ref<EntityStore> ref,
            @NonNullDecl PlayerRef playerRef,
            @NonNullDecl World world) {
        
        NotifiedPlayersManager.unmarkNotified(playerRef.getUuid().toString());
        
        String lang = playerRef.getLanguage();
        Message msg = Message.empty()
            .insert(LangManager.getForLanguage(lang, "command.reset.success"))
            .color("#55FF55");
        playerRef.sendMessage(msg);
    }
}
