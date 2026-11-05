package mx.jume.aquahunger.commands;

import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import mx.jume.aquahunger.compat.EndlessLevelingBridge;
import mx.jume.aquahunger.compat.IntegrationManager;
import mx.jume.aquahunger.compat.LangManager;
import mx.jume.aquahunger.compat.MMOSkillTreeBridge;
import mx.jume.aquahunger.compat.OptionalPlugins;
import mx.jume.aquahunger.compat.RPGLevelingBridge;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

/** /aquahunger integrations: which RPG and skill mods are connected, and the player's levels in them. */
public class HungryIntegrationsCommand extends AbstractPlayerCommand {
    public static final String requiredPermission = "hungry.command.integrations";

    private static final String COLOR_ACTIVE = "#55FF55";
    private static final String COLOR_INACTIVE = "#FF5555";
    private static final String COLOR_MISSING = "#AAAAAA";
    private static final String COLOR_DETAIL = "#FFFF55";

    public HungryIntegrationsCommand() {
        super("integrations", "Show which RPG and skill mods are connected", false);
        this.requirePermission(requiredPermission);
    }

    @Override
    protected void execute(
            @NonNullDecl CommandContext context,
            @NonNullDecl Store<EntityStore> store,
            @NonNullDecl Ref<EntityStore> ref,
            @NonNullDecl PlayerRef playerRef,
            @NonNullDecl World world
    ) {
        String lang = playerRef.getLanguage();
        playerRef.sendMessage(Message.raw(LangManager.getForLanguage(lang, "command.integrations.header")));

        RPGLevelingBridge rpg = IntegrationManager.getRpgBridge();
        if (status(playerRef, lang, "RPG Leveling", OptionalPlugins.RPG_LEVELING, rpg != null)) {
            detail(playerRef, String.format(LangManager.getForLanguage(lang, "command.integrations.level"),
                    rpg.getLevelingLevel(ref, store)));
            detail(playerRef, LangManager.getForLanguage(lang, rpg.hasLevelUpListener()
                    ? "command.integrations.levelup_on" : "command.integrations.levelup_off"));
        }

        EndlessLevelingBridge endless = IntegrationManager.getEndlessBridge();
        if (status(playerRef, lang, "Endless Leveling", OptionalPlugins.ENDLESS_LEVELING, endless != null)) {
            detail(playerRef, String.format(LangManager.getForLanguage(lang, "command.integrations.level"),
                    endless.getLevelingLevel(ref, store)));
            detail(playerRef, LangManager.getForLanguage(lang, endless.hasLevelUpListener()
                    ? "command.integrations.levelup_on" : "command.integrations.levelup_off"));
        }

        MMOSkillTreeBridge mmo = IntegrationManager.getMmoBridge();
        if (status(playerRef, lang, "MMO Skill Tree", OptionalPlugins.MMO_SKILL_TREE, mmo != null)) {
            detail(playerRef, String.format(LangManager.getForLanguage(lang, "command.integrations.mmo_levels"),
                    mmo.getSkillLevel(ref, store, "NUTRITION"), mmo.getSkillLevel(ref, store, "THIRST")));
            detail(playerRef, LangManager.getForLanguage(lang, mmo.isRewardReadingAvailable()
                    ? "command.integrations.mmo_rewards_on" : "command.integrations.mmo_rewards_off"));
        }
    }

    /** Sends the status line of one mod. @return true when the bridge is connected. */
    private static boolean status(PlayerRef playerRef, String lang, String name, PluginIdentifier id, boolean connected) {
        String version = OptionalPlugins.version(id);
        String label = version != null ? name + " " + version : name;
        String key;
        String color;
        if (connected) {
            key = "command.integrations.active";
            color = COLOR_ACTIVE;
        } else if (version != null) {
            key = "command.integrations.inactive";
            color = COLOR_INACTIVE;
        } else {
            key = "command.integrations.not_installed";
            color = COLOR_MISSING;
        }
        playerRef.sendMessage(Message.empty().insert(String.format(LangManager.getForLanguage(lang, key), label)).color(color));
        return connected;
    }

    private static void detail(PlayerRef playerRef, String text) {
        playerRef.sendMessage(Message.empty().insert(text).color(COLOR_DETAIL));
    }
}
