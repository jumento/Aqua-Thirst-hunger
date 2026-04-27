package mx.jume.aquahunger.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import mx.jume.aquahunger.HHMUtils;
import mx.jume.aquahunger.compat.LangManager;
import mx.jume.aquahunger.components.ThirstComponent;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class SetThirstCommand extends AbstractPlayerCommand {
    public static final String requiredPermission = "thirsty.thirst.set.self";

    private final RequiredArg<Float> thirstLevel = this.withRequiredArg("thirstLevel", "A value between 0 and 100",
            ArgTypes.FLOAT);

    public SetThirstCommand() {
        super("set", "Set own thirst level", false);
        this.requirePermission(requiredPermission);
        this.addUsageVariant(new SetThirstOtherCommand());
    }

    private static void setThirstLevel(
            @NonNullDecl CommandContext context,
            @NonNullDecl Store<EntityStore> store,
            @NonNullDecl Ref<EntityStore> ref,
            @NonNullDecl PlayerRef targetPlayerRef,
            float newThirstLevel) {
        final ThirstComponent thirst = store.getComponent(ref, ThirstComponent.getComponentType());
        float max = thirst != null ? thirst.getMaxThirst() : ThirstComponent.DEFAULT_MAX_THIRST;

        String lang = targetPlayerRef.getLanguage();
        if (newThirstLevel < 0 || newThirstLevel > max) {
            String rangeMsg = String.format(LangManager.getForLanguage(lang, "command.thirst.set.range"), max);
            context.sendMessage(Message.raw(rangeMsg));
            return;
        }
        HHMUtils.setPlayerThirstLevel(ref, store, newThirstLevel);
        String successMsg = String.format(LangManager.getForLanguage(lang, "command.thirst.set.success"), newThirstLevel, targetPlayerRef.getUsername());
        context.sendMessage(Message.raw(successMsg));
    }

    @Override
    protected void execute(
            @NonNullDecl CommandContext context,
            @NonNullDecl Store<EntityStore> store,
            @NonNullDecl Ref<EntityStore> ref,
            @NonNullDecl PlayerRef playerRef,
            @NonNullDecl World world) {
        float newThirstLevel = this.thirstLevel.get(context);
        setThirstLevel(context, store, ref, playerRef, newThirstLevel);
    }

    public static class SetThirstOtherCommand extends CommandBase {
        public static final String requiredPermission = "thirsty.thirst.set.other";

        private final RequiredArg<PlayerRef> playerArg = this.withRequiredArg("player", "The target player",
                ArgTypes.PLAYER_REF);
        private final RequiredArg<Float> thirstLevel = this.withRequiredArg("thirstLevel", "A value between 0 and 100",
                ArgTypes.FLOAT);

        public SetThirstOtherCommand() {
            super("Set another player's thirst level");
            this.requirePermission(requiredPermission);
        }

        @Override
        protected void executeSync(@NonNullDecl CommandContext context) {
            PlayerRef targetPlayerRef = this.playerArg.get(context);
            float newThirstLevel = this.thirstLevel.get(context);
            Ref<EntityStore> ref = targetPlayerRef.getReference();
            if (ref == null || !ref.isValid()) {
                String lang = "en_US";
                if (context.sender() instanceof PlayerRef) {
                    lang = ((PlayerRef)context.sender()).getLanguage();
                }
                context.sendMessage(Message.raw(LangManager.getForLanguage(lang, "command.player.not_found")));
                return;
            }
            Store<EntityStore> store = ref.getStore();
            World world = store.getExternalData().getWorld();

            world.execute(() -> {
                final ThirstComponent thirst = store.getComponent(ref, ThirstComponent.getComponentType());
                float max = thirst != null ? thirst.getMaxThirst() : ThirstComponent.DEFAULT_MAX_THIRST;

                String lang = targetPlayerRef.getLanguage();
                if (newThirstLevel < 0 || newThirstLevel > max) {
                    String rangeMsg = String.format(LangManager.getForLanguage(lang, "command.thirst.set.range"), max);
                    context.sendMessage(Message.raw(rangeMsg));
                    return;
                }
                HHMUtils.setPlayerThirstLevel(ref, store, newThirstLevel);
                String successMsg = String.format(LangManager.getForLanguage(lang, "command.thirst.set.success"), newThirstLevel, targetPlayerRef.getUsername());
                context.sendMessage(Message.raw(successMsg));
            });
        }
    }
}
