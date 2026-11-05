package mx.jume.aquahunger.ui;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.asset.type.gamemode.GameModeType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.HudManager;
import com.hypixel.hytale.server.core.modules.entity.gamemode.GameModeTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import mx.jume.aquahunger.components.HungerComponent;
import mx.jume.aquahunger.components.ThirstComponent;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lifecycle of the mod's HUDs on top of Hytale's native keyed custom HUDs.
 *
 * Every custom HUD now has its own key and element tree on the client, so the bars coexist
 * with other mods' HUDs without a shared container or packet interception. The engine drops
 * all custom HUDs when a player changes world, so they are (re)attached on every
 * PlayerReadyEvent.
 */
public final class HudService {
    private record PlayerHuds(PlayerRef playerRef, HHMHud hunger, HHMThirstHud thirst) {
    }

    private static final Map<UUID, PlayerHuds> HUDS = new ConcurrentHashMap<>();

    private HudService() {
    }

    public static HHMHud getHungerHud(@NonNullDecl PlayerRef playerRef) {
        PlayerHuds huds = HUDS.get(playerRef.getUuid());
        return huds != null ? huds.hunger() : null;
    }

    public static HHMThirstHud getThirstHud(@NonNullDecl PlayerRef playerRef) {
        PlayerHuds huds = HUDS.get(playerRef.getUuid());
        return huds != null ? huds.thirst() : null;
    }

    /** Login and every world change. Must run on the player's world thread. */
    public static void attach(
            @NonNullDecl Store<EntityStore> store,
            @NonNullDecl Ref<EntityStore> ref,
            @NonNullDecl PlayerRef playerRef,
            @NonNullDecl Player player) {
        HungerComponent hunger = store.ensureAndGetComponent(ref, HungerComponent.getComponentType());
        ThirstComponent thirst = store.ensureAndGetComponent(ref, ThirstComponent.getComponentType());
        GameMode gameMode = player.getGameMode();

        PlayerHuds huds = HUDS.get(playerRef.getUuid());
        if (huds == null || huds.playerRef() != playerRef) {
            huds = new PlayerHuds(playerRef,
                    new HHMHud(playerRef, gameMode, hunger.getHungerLevel()),
                    new HHMThirstHud(playerRef, gameMode, thirst.getThirstLevel()));
            HUDS.put(playerRef.getUuid(), huds);
        }

        boolean spectating = isSpectating(ref, store);
        huds.hunger().prepare(gameMode, hunger.getHungerLevel(), spectating);
        huds.thirst().prepare(gameMode, thirst.getThirstLevel(), spectating);
        show(player, playerRef, huds.hunger());
        show(player, playerRef, huds.thirst());
    }

    public static void detach(@NonNullDecl PlayerRef playerRef) {
        HUDS.remove(playerRef.getUuid());
    }

    /** Re-reads the config (position, enabled flags) and rebuilds every HUD on its own world thread. */
    public static void refreshAll() {
        for (PlayerHuds huds : HUDS.values()) {
            runOnPlayerWorld(huds.playerRef(), () -> {
                huds.hunger().reloadConfig();
                huds.thirst().reloadConfig();
                huds.hunger().rebuild();
                huds.thirst().rebuild();
            });
        }
    }

    /** Pushes the current component values, e.g. after the +XP/-XP flags changed. World thread only. */
    public static void refreshValues(@NonNullDecl PlayerRef playerRef) {
        PlayerHuds huds = HUDS.get(playerRef.getUuid());
        if (huds == null) {
            return;
        }
        HungerComponent hunger = playerRef.getComponent(HungerComponent.getComponentType());
        if (hunger != null) {
            huds.hunger().setHungerLevel(hunger.getHungerLevel());
        }
        ThirstComponent thirst = playerRef.getComponent(ThirstComponent.getComponentType());
        if (thirst != null) {
            huds.thirst().setThirstLevel(thirst.getThirstLevel());
        }
    }

    public static void setGameMode(@NonNullDecl PlayerRef playerRef, GameMode gameMode) {
        PlayerHuds huds = HUDS.get(playerRef.getUuid());
        if (huds == null) {
            return;
        }
        huds.hunger().setGameMode(gameMode);
        huds.thirst().setGameMode(gameMode);
    }

    public static boolean setHiddenByPlayer(@NonNullDecl PlayerRef playerRef, boolean hidden) {
        PlayerHuds huds = HUDS.get(playerRef.getUuid());
        if (huds == null) {
            return false;
        }
        huds.hunger().setHiddenByPlayer(hidden);
        huds.thirst().setHiddenByPlayer(hidden);
        return true;
    }

    /** Hides the bars while the player is in a spectator game mode type. World thread only. */
    public static void refreshSpectating(@NonNullDecl PlayerRef playerRef) {
        PlayerHuds huds = HUDS.get(playerRef.getUuid());
        Ref<EntityStore> ref = playerRef.getReference();
        if (huds == null || ref == null || !ref.isValid()) {
            return;
        }
        boolean spectating = isSpectating(ref, ref.getStore());
        huds.hunger().setSpectating(spectating);
        huds.thirst().setSpectating(spectating);
    }

    public static void showNotice(@NonNullDecl Player player, @NonNullDecl PlayerRef playerRef,
            @NonNullDecl WelcomeNoticeHud notice) {
        show(player, playerRef, notice);
    }

    /** @return true if a notice was on screen and has been removed. */
    public static boolean dismissNotice(@NonNullDecl Player player, @NonNullDecl PlayerRef playerRef) {
        HudManager hudManager = player.getHudManager();
        if (hudManager.getCustomHud(WelcomeNoticeHud.HUD_KEY) == null) {
            return false;
        }
        hudManager.removeCustomHud(playerRef, WelcomeNoticeHud.HUD_KEY);
        return true;
    }

    /** Runs the task on the world the player is currently in (skipped if the player left). */
    public static void runOnPlayerWorld(@NonNullDecl PlayerRef playerRef, @NonNullDecl Runnable task) {
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            return;
        }
        World world = ref.getStore().getExternalData().getWorld();
        if (world != null) {
            world.execute(task);
        }
    }

    private static void show(Player player, PlayerRef playerRef, AquaCustomHud hud) {
        HudManager hudManager = player.getHudManager();
        hud.markAttached();
        if (hudManager.getCustomHud(hud.getKey()) == hud) {
            // Still registered (no world change in between): resend the full state.
            hud.show();
        } else {
            hudManager.addCustomHud(playerRef, hud);
        }
    }

    static boolean isSpectating(Ref<EntityStore> ref, ComponentAccessor<EntityStore> accessor) {
        try {
            GameModeType type = GameModeTypes.getCurrentType(ref, accessor);
            return type != null && type.isSpectator();
        } catch (RuntimeException e) {
            return false;
        }
    }
}
