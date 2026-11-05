package mx.jume.aquahunger.ui;

import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

/**
 * Base for the mod's HUDs on top of Hytale's native keyed custom HUDs.
 *
 * The engine drops every custom HUD when the player changes world (onRemove() is called),
 * so partial updates are only sent while the HUD is attached; otherwise the client would
 * receive commands for an element tree it no longer has.
 */
public abstract class AquaCustomHud extends CustomUIHud {
    private volatile boolean attached = false;

    protected AquaCustomHud(@NonNullDecl PlayerRef playerRef, @NonNullDecl String key) {
        super(playerRef, key);
    }

    public boolean isAttached() {
        return attached;
    }

    void markAttached() {
        this.attached = true;
    }

    @Override
    protected void onRemove() {
        this.attached = false;
    }

    /** Full rebuild of this HUD only (clear + build). No-op while detached. */
    public void rebuild() {
        if (attached) {
            show();
        }
    }

    protected void sendUpdate(@NonNullDecl UICommandBuilder ui) {
        if (attached) {
            update(false, ui);
        }
    }
}
