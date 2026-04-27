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
import javax.annotation.Nonnull;

public class RpgStatusCommand extends AbstractPlayerCommand {
    public RpgStatusCommand() {
        super("rpgstatus", "Checks the status of RPG integration", false);
    }

    @Override
    protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {
        
        boolean active = mx.jume.aquahunger.compat.IntegrationManager.isAnyLevelingAvailable();
        int playerLevel = mx.jume.aquahunger.compat.IntegrationManager.getSkillLevel(ref, store, "GLOBAL");
        
        String lang = playerRef.getLanguage();
        String statusStr = active ? LangManager.getForLanguage(lang, "command.rpg_status.active") : LangManager.getForLanguage(lang, "command.rpg_status.inactive");
        
        playerRef.sendMessage(Message.empty().insert(String.format(LangManager.getForLanguage(lang, "command.rpg_status.header"), statusStr)).color(active ? "#55FF55" : "#FF5555"));
        playerRef.sendMessage(Message.empty().insert(String.format(LangManager.getForLanguage(lang, "command.rpg_status.level"), playerLevel)).color("#FFFF55"));
        
        if (active) {
            playerRef.sendMessage(Message.empty().insert(LangManager.getForLanguage(lang, "command.rpg_status.test_hint")).color("#AAAAAA"));
        } else {
            playerRef.sendMessage(Message.empty().insert(LangManager.getForLanguage(lang, "command.rpg_status.log_hint")).color("#FF5555"));
        }
    }
}
