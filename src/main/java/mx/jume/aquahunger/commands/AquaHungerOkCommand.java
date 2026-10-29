package mx.jume.aquahunger.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import mx.jume.aquahunger.compat.LangManager;
import mx.jume.aquahunger.compat.WelcomeNoticeManager;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class AquaHungerOkCommand extends AbstractPlayerCommand {
    public AquaHungerOkCommand() {
        super("aquahungerok", "Cierra el aviso de bienvenida de AquaThirst", false);
        this.requireNoPermission(); // Cualquier jugador puede usar /aquahungerok
    }

    @Override
    protected void execute(
            @NonNullDecl CommandContext context,
            @NonNullDecl Store<EntityStore> store,
            @NonNullDecl Ref<EntityStore> ref,
            @NonNullDecl PlayerRef playerRef,
            @NonNullDecl World world) {
        String lang = playerRef.getLanguage();
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player != null && WelcomeNoticeManager.dismissNotice(player, playerRef)) {
            Message msg = Message.empty()
                .insert(LangManager.getForLanguage(lang, "integration.welcome.ok_response"))
                .color("#55FF55");
            playerRef.sendMessage(msg);
        } else {
            Message msg = Message.empty()
                .insert(LangManager.getForLanguage(lang, "integration.welcome.no_notice"))
                .color("#FF5555");
            playerRef.sendMessage(msg);
        }
    }
}
