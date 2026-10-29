package mx.jume.aquahunger.ui;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.ui.Anchor;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import mx.jume.aquahunger.AquaThirstHunger;
import mx.jume.aquahunger.components.HungerComponent;
import mx.jume.aquahunger.config.HudPosition;
import mx.jume.aquahunger.interactions.FeedInteraction;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

// https://www.youtube.com/watch?v=cha7YFULwxY&t=1330s

public class HHMHud extends AquaCustomHud {
    public static final String HUD_KEY = "AquaThirstHunger_Hunger";

    private HudPosition hudPosition;
    private GameMode gameMode;
    private float hungerLevel;
    private float maxHunger = HungerComponent.DEFAULT_MAX_HUNGER;
    private float previewHungerRestoration = 0.0f;
    private float previewMaxHungerSaturation = 0.0f;
    private boolean hiddenByPlayer = false;
    private boolean spectating = false;

    // Last bar state sent to the client, so unchanged values are not re-sent every tick.
    private float sentHungerBar = Float.NaN;
    private float sentSaturationBar = Float.NaN;
    private boolean sentXpUp = false;
    private boolean sentXpDown = false;

    public HHMHud(@NonNullDecl PlayerRef playerRef, GameMode gameMode, float hungerLevel) {
        super(playerRef, HUD_KEY);
        this.gameMode = gameMode;
        this.hungerLevel = hungerLevel;
        this.hudPosition = AquaThirstHunger.get().getHungerConfig().getHudPosition();
    }

    @Override
    protected void build(@NonNullDecl UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.append("Hungry/HUD/Hunger.ui");
        writeHudPosition(uiCommandBuilder);
        writeGameMode(uiCommandBuilder);
        writeHungerLevel(uiCommandBuilder);
        writeVisibility(uiCommandBuilder);
    }

    private void writeHudPosition(UICommandBuilder uiCommandBuilder) {
        HudPosition hudPosition = this.hudPosition;
        Anchor anchor = new Anchor();
        anchor.setHeight(Value.of(20));

        // Special handling for centered presets (AboveHotbarCentered,
        // BelowHotbarCentered)
        if (hudPosition.centered()) {
            anchor.setWidth(Value.of(351)); // Actual HUD width from UI
            // Calculate centering offset: (702 - 351) / 2 = 176
            anchor.setLeft(Value.of(176));
        }
        // Special handling for right-anchored preset (BottomRight)
        else if (hudPosition.right() >= 0) {
            anchor.setWidth(Value.of(332));
            anchor.setRight(Value.of(hudPosition.right()));
        }
        // Original logic for all other presets
        else if (hudPosition.left() != 0) {
            anchor.setWidth(Value.of(332));
            anchor.setLeft(Value.of(hudPosition.left()));
        } else {
            // Original fallback for left == 0 (non-centered)
            anchor.setWidth(Value.of(702));
        }

        if (hudPosition.bottom() != 0) {
            anchor.setBottom(Value.of(hudPosition.bottom()));
        }

        uiCommandBuilder.setObject("#HHMContainer.Anchor", anchor);
    }

    private void writeHungerLevel(UICommandBuilder uiCommandBuilder) {
        HungerComponent hunger = readHungerComponent();
        if (hunger != null) {
            this.maxHunger = hunger.getMaxHunger();
        }
        float hungerBarValue = hungerBarValue(this.hungerLevel);
        float saturationBarValue = saturationBarValue(this.hungerLevel, this.maxHunger);
        boolean xpUp = hunger != null && hunger.isXpUp();
        boolean xpDown = hunger != null && hunger.isXpDown();

        uiCommandBuilder.set("#HHMHungerBar.Value", hungerBarValue);
        uiCommandBuilder.set("#HHMHungerBarEffect.Value", 0.0f);
        uiCommandBuilder.set("#HHMCreativeHungerBar.Value", hungerBarValue);
        uiCommandBuilder.set("#HHMHungerSaturationBar.Value", saturationBarValue);
        uiCommandBuilder.set("#HHMXpUp.Visible", xpUp);
        uiCommandBuilder.set("#HHMXpDown.Visible", xpDown);

        this.sentHungerBar = hungerBarValue;
        this.sentSaturationBar = saturationBarValue;
        this.sentXpUp = xpUp;
        this.sentXpDown = xpDown;

        if (this.previewHungerRestoration != 0.0f || this.previewMaxHungerSaturation != 0.0f) {
            writeRestorationPreview(uiCommandBuilder);
        }
    }

    private void writeRestorationPreview(UICommandBuilder uiCommandBuilder) {
        if (this.previewHungerRestoration == 0.0f && this.previewMaxHungerSaturation == 0.0f) {
            uiCommandBuilder.set("#HHMHungerRestorePreviewBar.Value", 0.0f);
            uiCommandBuilder.set("#HHMHungerSaturationRestorePreviewBar.Value", 0.0f);
            return;
        }

        float expectedHungerLevel = Math.min(FeedInteraction.getExpectedHungerLevel(this.hungerLevel,
                this.previewHungerRestoration, this.previewMaxHungerSaturation), this.maxHunger);
        uiCommandBuilder.set("#HHMHungerRestorePreviewBar.Value", hungerBarValue(expectedHungerLevel));
        uiCommandBuilder.set("#HHMHungerSaturationRestorePreviewBar.Value",
                saturationBarValue(expectedHungerLevel, this.maxHunger));
    }

    private void writeGameMode(UICommandBuilder uiCommandBuilder) {
        String iconBackground = gameMode == GameMode.Adventure
                ? "Hungry/HUD/HungerIcon.png"
                : "Hungry/HUD/CreativeHungerIcon.png";
        uiCommandBuilder.set("#HHMIcon.Background", iconBackground);
        uiCommandBuilder.set("#HHMHungerBar.Visible", gameMode == GameMode.Adventure);
        uiCommandBuilder.set("#HHMHungerSaturationBar.Visible", gameMode == GameMode.Adventure);
        uiCommandBuilder.set("#HHMCreativeHungerBar.Visible", gameMode == GameMode.Creative);
    }

    private void writeVisibility(UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.set("#HHMContainer.Visible", isShown());
    }

    private boolean isShown() {
        return AquaThirstHunger.get().getHungerConfig().isEnableHunger() && !hiddenByPlayer && !spectating;
    }

    /** Hunger 0-100 fills the main bar. */
    static float hungerBarValue(float hungerLevel) {
        return Math.max(0.0f, Math.min(hungerLevel, 100.0f)) / 100.0f;
    }

    /** Everything above 100 is saturation, drawn relative to the real maximum. */
    static float saturationBarValue(float hungerLevel, float maxHunger) {
        float saturationRange = maxHunger - 100.0f;
        if (saturationRange <= 0.0f) {
            return 0.0f;
        }
        return Math.min(Math.max(hungerLevel - 100.0f, 0.0f) / saturationRange, 1.0f);
    }

    private HungerComponent readHungerComponent() {
        try {
            return this.getPlayerRef().getComponent(HungerComponent.getComponentType());
        } catch (RuntimeException e) {
            return null;
        }
    }

    // ---------- state changes (each sends only what changed) ----------

    public void setHungerLevel(float hungerLevel) {
        this.hungerLevel = hungerLevel;
        if (!isAttached()) {
            return;
        }
        HungerComponent hunger = readHungerComponent();
        float max = hunger != null ? hunger.getMaxHunger() : this.maxHunger;
        boolean unchanged = Math.abs(hungerBarValue(hungerLevel) - sentHungerBar) < 0.0005f
                && Math.abs(saturationBarValue(hungerLevel, max) - sentSaturationBar) < 0.0005f
                && (hunger != null && hunger.isXpUp()) == sentXpUp
                && (hunger != null && hunger.isXpDown()) == sentXpDown;
        if (unchanged) {
            return;
        }
        UICommandBuilder uiCommandBuilder = new UICommandBuilder();
        writeHungerLevel(uiCommandBuilder);
        sendUpdate(uiCommandBuilder);
    }

    public void setRestorationPreview(float hungerRestoration, float maxHungerSaturation) {
        if (hungerRestoration == this.previewHungerRestoration
                && maxHungerSaturation == this.previewMaxHungerSaturation) {
            return;
        }
        this.previewHungerRestoration = hungerRestoration;
        this.previewMaxHungerSaturation = maxHungerSaturation;
        UICommandBuilder uiCommandBuilder = new UICommandBuilder();
        writeRestorationPreview(uiCommandBuilder);
        sendUpdate(uiCommandBuilder);
    }

    public void setGameMode(GameMode gameMode) {
        this.gameMode = gameMode;
        UICommandBuilder uiCommandBuilder = new UICommandBuilder();
        writeGameMode(uiCommandBuilder);
        writeHungerLevel(uiCommandBuilder);
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
        this.hudPosition = AquaThirstHunger.get().getHungerConfig().getHudPosition();
    }

    /** State refresh before (re)attaching; the build that follows sends all of it. */
    void prepare(GameMode gameMode, float hungerLevel, boolean spectating) {
        this.gameMode = gameMode;
        this.hungerLevel = hungerLevel;
        this.spectating = spectating;
        this.previewHungerRestoration = 0.0f;
        this.previewMaxHungerSaturation = 0.0f;
        reloadConfig();
    }

    // ---------- static helpers used by systems, interactions and commands ----------

    static public HHMHud getHud(@NonNullDecl PlayerRef playerRef) {
        return HudService.getHungerHud(playerRef);
    }

    static public void updatePlayerHungerLevel(@NonNullDecl PlayerRef playerRef, float hungerLevel) {
        HHMHud hud = getHud(playerRef);
        if (hud != null) {
            hud.setHungerLevel(hungerLevel);
        }
    }

    static public void updatePlayerHungerRestorationPreview(@NonNullDecl PlayerRef playerRef, float hungerRestoration,
            float maxHungerSaturation) {
        HHMHud hud = getHud(playerRef);
        if (hud != null) {
            hud.setRestorationPreview(hungerRestoration, maxHungerSaturation);
        }
    }
}
