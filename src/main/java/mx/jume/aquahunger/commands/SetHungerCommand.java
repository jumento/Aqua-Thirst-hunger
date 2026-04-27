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
import mx.jume.aquahunger.components.HungerComponent;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class SetHungerCommand extends AbstractPlayerCommand {
    public static final String requiredPermission = "hungry.hunger.set.self";

    private final RequiredArg<Float> hungerLevel = this.withRequiredArg("hungerLevel", "A value between 0 and 100", ArgTypes.FLOAT);

    public SetHungerCommand() {
        super("set", "Set own hunger level", false);
        this.requirePermission(requiredPermission);
        this.addUsageVariant(new SetHungerOtherCommand());
    }

    private static void setHungerLevel (
        @NonNullDecl CommandContext context,
        @NonNullDecl Store<EntityStore> store,
        @NonNullDecl Ref<EntityStore> ref,
        @NonNullDecl PlayerRef targetPlayerRef,
        float newHungerLevel
    ) {
        final HungerComponent hunger = store.getComponent(ref, HungerComponent.getComponentType());
        float max = hunger != null ? hunger.getMaxHunger() : HungerComponent.DEFAULT_MAX_HUNGER;
        
        String lang = targetPlayerRef.getLanguage();
        if (newHungerLevel < 0 || newHungerLevel > max) {
            String rangeMsg = String.format(LangManager.getForLanguage(lang, "command.hunger.set.range"), max);
            context.sendMessage(Message.raw(rangeMsg));
            return;
        }
        HHMUtils.setPlayerHungerLevel(ref, store, newHungerLevel);
        String successMsg = String.format(LangManager.getForLanguage(lang, "command.hunger.set.success"), newHungerLevel, targetPlayerRef.getUsername());
        context.sendMessage(Message.raw(successMsg));
    }

    @Override
    protected void execute(
            @NonNullDecl CommandContext context,
            @NonNullDecl Store<EntityStore> store,
            @NonNullDecl Ref<EntityStore> ref,
            @NonNullDecl PlayerRef playerRef,
            @NonNullDecl World world
    ) {
        float newHungerLevel = this.hungerLevel.get(context);
        setHungerLevel(context, store, ref, playerRef, newHungerLevel);
    }

    public static class SetHungerOtherCommand extends CommandBase {
        public static final String requiredPermission = "hungry.hunger.set.other";

        private final RequiredArg<PlayerRef> playerArg = this.withRequiredArg("player", "The target player", ArgTypes.PLAYER_REF);
        private final RequiredArg<Float> hungerLevel = this.withRequiredArg("hungerLevel", "A value between 0 and 100", ArgTypes.FLOAT);

        public SetHungerOtherCommand() {
            super("Set another player's hunger level");
            this.requirePermission(requiredPermission);
        }

        @Override
        protected void executeSync(@NonNullDecl CommandContext context) {
            PlayerRef targetPlayerRef = this.playerArg.get(context);
            float newHungerLevel = this.hungerLevel.get(context);
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
            world.execute(() -> setHungerLevel(context, store, ref, targetPlayerRef, newHungerLevel));
        }
    }
}

