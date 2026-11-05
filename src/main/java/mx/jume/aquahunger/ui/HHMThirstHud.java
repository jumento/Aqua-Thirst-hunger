package mx.jume.aquahunger.ui;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.ui.Anchor;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import mx.jume.aquahunger.AquaThirstHunger;
import mx.jume.aquahunger.components.ThirstComponent;
import mx.jume.aquahunger.config.HudPosition;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class HHMThirstHud extends AquaCustomHud {
    public static final String HUD_KEY = "AquaThirstHunger_Thirst";

    private HudPosition hudPosition;
    private GameMode gameMode;
    private float thirstLevel;
    private float maxThirst = ThirstComponent.DEFAULT_MAX_THIRST;
    private float previewThirstRestoration = 0.0f;
    private boolean hiddenByPlayer = false;
    private boolean spectating = false;

    // Last bar state sent to the client, so unchanged values are not re-sent every tick.
    private float sentThirstBar = Float.NaN;
    private boolean sentXpUp = false;
    private boolean sentXpDown = false;

    public HHMThirstHud(@NonNullDecl PlayerRef playerRef, GameMode gameMode, float thirstLevel) {
        super(playerRef, HUD_KEY);
        this.gameMode = gameMode;
        this.thirstLevel = thirstLevel;
        this.hudPosition = AquaThirstHunger.get().getThirstConfig().getHudPosition();
    }

    @Override
    protected void build(@NonNullDecl UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.append("Hungry/HUD/Thirst.ui");
        writeHudPosition(uiCommandBuilder);
        writeGameMode(uiCommandBuilder);
        writeThirstLevel(uiCommandBuilder);
        writeVisibility(uiCommandBuilder);
    }

    private void writeHudPosition(UICommandBuilder uiCommandBuilder) {
        HudPosition hudPosition = this.hudPosition;
        Anchor anchor = new Anchor();
        anchor.setHeight(Value.of(20));

        if (hudPosition.centered()) {
            anchor.setWidth(Value.of(351));
            anchor.setLeft(Value.of(176));
        }
        else if (hudPosition.right() >= 0) {
            anchor.setWidth(Value.of(332));
            anchor.setRight(Value.of(hudPosition.right()));
        }
        else if (hudPosition.left() != 0) {
            anchor.setWidth(Value.of(332));
            anchor.setLeft(Value.of(hudPosition.left()));
        } else {
            anchor.setWidth(Value.of(702));
        }

        if (hudPosition.bottom() != 0) {
            anchor.setBottom(Value.of(hudPosition.bottom()));
        }

        uiCommandBuilder.setObject("#HHMThirstContainer.Anchor", anchor);
    }

    private void writeThirstLevel(UICommandBuilder uiCommandBuilder) {
        ThirstComponent thirst = readThirstComponent();
        if (thirst != null) {
            this.maxThirst = thirst.getMaxThirst();
        }
        float thirstBarValue = thirstBarValue(this.thirstLevel, this.maxThirst);
        boolean xpUp = thirst != null && thirst.isXpUp();
        boolean xpDown = thirst != null && thirst.isXpDown();

        uiCommandBuilder.set("#HHMThirstBar.Value", thirstBarValue);
        uiCommandBuilder.set("#HHMThirstBarEffect.Value", thirstBarValue);
        uiCommandBuilder.set("#HHMCreativeThirstBar.Value", thirstBarValue);
        uiCommandBuilder.set("#HHMThirstXpUp.Visible", xpUp);
        uiCommandBuilder.set("#HHMThirstXpDown.Visible", xpDown);

        this.sentThirstBar = thirstBarValue;
        this.sentXpUp = xpUp;
        this.sentXpDown = xpDown;

        if (this.previewThirstRestoration != 0.0f) {
            writeRestorationPreview(uiCommandBuilder);
        }
    }

    private void writeRestorationPreview(UICommandBuilder uiCommandBuilder) {
        if (this.previewThirstRestoration == 0.0f) {
            uiCommandBuilder.set("#HHMThirstRestorePreviewBar.Value", 0.0f);
            return;
        }

        float expectedThirstLevel = Math.min(this.thirstLevel + this.previewThirstRestoration, this.maxThirst);
        uiCommandBuilder.set("#HHMThirstRestorePreviewBar.Value", thirstBarValue(expectedThirstLevel, this.maxThirst));
    }

    private void writeGameMode(UICommandBuilder uiCommandBuilder) {
        String iconBackground = gameMode == GameMode.Adventure
                ? "Hungry/HUD/ThirstIcon.png"
                : "Hungry/HUD/CreativeThirstIcon.png";

        uiCommandBuilder.set("#HHMThirstIcon.Background", iconBackground);
        uiCommandBuilder.set("#HHMThirstBar.Visible", gameMode == GameMode.Adventure);
        uiCommandBuilder.set("#HHMCreativeThirstBar.Visible", gameMode == GameMode.Creative);
    }

    private void writeVisibility(UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.set("#HHMThirstContainer.Visible", isShown());
    }

    private boolean isShown() {
        return AquaThirstHunger.get().getThirstConfig().isEnableThirst() && !hiddenByPlayer && !spectating;
    }

    /** The bar is drawn against the real maximum, which can grow with progression. */
    static float thirstBarValue(float thirstLevel, float maxThirst) {
        if (maxThirst <= 0.0f) {
            return 0.0f;
        }
        return Math.max(0.0f, Math.min(thirstLevel / maxThirst, 1.0f));
    }

    private ThirstComponent readThirstComponent() {
        try {
            return this.getPlayerRef().getComponent(ThirstComponent.getComponentType());
        } catch (RuntimeException e) {
            return null;
        }
    }

    // ---------- state changes (each sends only what changed) ----------

    public void setThirstLevel(float thirstLevel) {
        this.thirstLevel = thirstLevel;
        if (!isAttached()) {
            return;
        }
        ThirstComponent thirst = readThirstComponent();
        float max = thirst != null ? thirst.getMaxThirst() : this.maxThirst;
        boolean unchanged = Math.abs(thirstBarValue(thirstLevel, max) - sentThirstBar) < 0.0005f
                && (thirst != null && thirst.isXpUp()) == sentXpUp
                && (thirst != null && thirst.isXpDown()) == sentXpDown;
        if (unchanged) {
            return;
        }
        UICommandBuilder uiCommandBuilder = new UICommandBuilder();
        writeThirstLevel(uiCommandBuilder);
        sendUpdate(uiCommandBuilder);
    }

    public void setRestorationPreview(float thirstRestoration) {
        if (thirstRestoration == this.previewThirstRestoration) {
            return;
        }
        this.previewThirstRestoration = thirstRestoration;
        UICommandBuilder uiCommandBuilder = new UICommandBuilder();
        writeRestorationPreview(uiCommandBuilder);
        sendUpdate(uiCommandBuilder);
    }

    public void setGameMode(GameMode gameMode) {
        this.gameMode = gameMode;
        UICommandBuilder uiCommandBuilder = new UICommandBuilder();
        writeGameMode(uiCommandBuilder);
        writeThirstLevel(uiCommandBuilder);
        sendUpdate(uiCommandBuilder);
    }

    public void setHiddenByPlayer(boolean hiddenByPlayer) {
        this.hiddenByPlayer = hiddenByPlayer;
        UICommandBuilder uiCommandBuilder = new UICommandBuilder();
        writeVisibility(uiCommandBuilder);
        sendUpdate(uiCommandBuilder);
    }

    public void setSpectating(boolean spectating) {
        if (this.spectating == spectating) {
            return;
        }
        this.spectating = spectating;
        UICommandBuilder uiCommandBuilder = new UICommandBuilder();
        writeVisibility(uiCommandBuilder);
        sendUpdate(uiCommandBuilder);
    }

    /** Picks up the current config (position, enabled flag) on the next build. */
    void reloadConfig() {
        this.hudPosition = AquaThirstHunger.get().getThirstConfig().getHudPosition();
    }

    /** State refresh before (re)attaching; the build that follows sends all of it. */
    void prepare(GameMode gameMode, float thirstLevel, boolean spectating) {
        this.gameMode = gameMode;
        this.thirstLevel = thirstLevel;
        this.spectating = spectating;
        this.previewThirstRestoration = 0.0f;
        reloadConfig();
    }

    // ---------- static helpers used by systems, interactions and commands ----------

    static public HHMThirstHud getHud(@NonNullDecl PlayerRef playerRef) {
        return HudService.getThirstHud(playerRef);
    }

    static public void updatePlayerThirstLevel(@NonNullDecl PlayerRef playerRef, float thirstLevel) {
        HHMThirstHud hud = getHud(playerRef);
        if (hud != null) {
            hud.setThirstLevel(thirstLevel);
        }
    }

    static public void updatePlayerThirstRestorationPreview(@NonNullDecl PlayerRef playerRef,
            float thirstRestoration) {
        HHMThirstHud hud = getHud(playerRef);
        if (hud != null) {
            hud.setRestorationPreview(thirstRestoration);
        }
    }
}
