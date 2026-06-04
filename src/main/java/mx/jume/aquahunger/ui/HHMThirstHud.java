package mx.jume.aquahunger.ui;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.Anchor;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import mx.jume.aquahunger.AquaThirstHunger;
import mx.jume.aquahunger.components.ThirstComponent;
import mx.jume.aquahunger.config.HHMThirstConfig;
import mx.jume.aquahunger.config.HudPosition;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public class HHMThirstHud extends CustomUIHud {
    static private final Map<PlayerRef, HHMThirstHud> hudMap = Collections.synchronizedMap(new WeakHashMap<>());
    static public final String hudIdentifier = "mx.jume.aquahunger.hud.thirst";

    private static final long MIN_UPDATE_INTERVAL_NANOS = 200_000_000L;
    private static final Map<PlayerRef, Long> lastUpdateNanos = Collections.synchronizedMap(new WeakHashMap<>());

    private HudPosition hudPosition;
    private GameMode gameMode;
    private float thirstLevel;
    private float previewThirstRestoration = 0.0f;
    private boolean visible = true;

    public HHMThirstHud(@NonNullDecl PlayerRef playerRef, GameMode gameMode, float thirstLevel) {
        super(playerRef, hudIdentifier);
        this.gameMode = gameMode;
        this.thirstLevel = thirstLevel;
        HHMThirstConfig config = AquaThirstHunger.get().getThirstConfig();
        this.hudPosition = config.getHudPosition();
        hudMap.put(playerRef, this);
    }

    @Override
    protected void build(@NonNullDecl UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.append("Hungry/HUD/Thirst.ui");
        updateHudPosition(uiCommandBuilder, this.hudPosition);
        updateGameMode(uiCommandBuilder, this.gameMode);
        updateThirstLevel(uiCommandBuilder, this.thirstLevel);
        updateVisibility(uiCommandBuilder, this.visible);
    }

    protected void updateHudPosition(UICommandBuilder uiCommandBuilder, HudPosition hudPosition) {
        this.hudPosition = hudPosition;
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

    protected void updateThirstLevel(UICommandBuilder uiCommandBuilder, float thirstLevel) {
        this.thirstLevel = thirstLevel;
        float thirstBarValue = Math.min(thirstLevel, 100.0f) / 100.0f;

        uiCommandBuilder.set("#HHMThirstBar.Value", thirstBarValue);
        uiCommandBuilder.set("#HHMThirstBarEffect.Value", thirstBarValue);
        uiCommandBuilder.set("#HHMCreativeThirstBar.Value", thirstBarValue);

        ThirstComponent thirst = this.getPlayerRef().getComponent(ThirstComponent.getComponentType());
        if (thirst != null) {
            uiCommandBuilder.set("#HHMThirstXpUp.Visible", thirst.isXpUp());
            uiCommandBuilder.set("#HHMThirstXpDown.Visible", thirst.isXpDown());
        }

        if (this.previewThirstRestoration != 0.0f) {
            updateThirstRestorationPreview(uiCommandBuilder, this.previewThirstRestoration);
        }
    }

    protected void updateThirstRestorationPreview(UICommandBuilder uiCommandBuilder, float thirstRestoration) {
        this.previewThirstRestoration = thirstRestoration;
        if (thirstRestoration == 0.0f) {
            uiCommandBuilder.set("#HHMThirstRestorePreviewBar.Value", 0.0f);
            return;
        }

        float expectedThirstLevel = Math.min(this.thirstLevel + thirstRestoration, 100.0f);
        float thirstBarValue = Math.min(expectedThirstLevel, 100.0f) / 100.0f;

        uiCommandBuilder.set("#HHMThirstRestorePreviewBar.Value", thirstBarValue);
    }

    public void setGameMode(GameMode gameMode) {
        this.gameMode = gameMode;
    }

    protected void updateGameMode(UICommandBuilder uiCommandBuilder, GameMode gameMode) {
        this.gameMode = gameMode;
        String iconBackground = gameMode == GameMode.Adventure
                ? "Hungry/HUD/ThirstIcon.png"
                : "Hungry/HUD/CreativeThirstIcon.png";

        uiCommandBuilder.set("#HHMThirstIcon.Background", iconBackground);
        uiCommandBuilder.set("#HHMThirstBar.Visible", gameMode == GameMode.Adventure);
        uiCommandBuilder.set("#HHMCreativeThirstBar.Visible", gameMode == GameMode.Creative);
    }

    protected void updateVisibility(UICommandBuilder uiCommandBuilder, boolean visible) {
        boolean configEnabled = AquaThirstHunger.get().getThirstConfig().isEnableThirst();
        this.visible = visible && configEnabled;
        uiCommandBuilder.set("#HHMThirstContainer.Visible", this.visible);
    }

    static public boolean hasHud(@NonNullDecl PlayerRef playerRef) {
        return hudMap.containsKey(playerRef);
    }

    static public HHMThirstHud getHud(@NonNullDecl PlayerRef playerRef) {
        return hudMap.get(playerRef);
    }

    static public void removeHud(@NonNullDecl PlayerRef playerRef) {
        hudMap.remove(playerRef);
    }

    public void appendAllCommands(UICommandBuilder uiCommandBuilder) {
        this.build(uiCommandBuilder);
    }

    static public void updatePlayerThirstLevel(@NonNullDecl PlayerRef playerRef, float thirstLevel) {
        HHMThirstHud hud = hudMap.get(playerRef);
        if (hud == null)
            return;

        long now = System.nanoTime();
        Long last = lastUpdateNanos.get(playerRef);
        if (last != null && now - last < MIN_UPDATE_INTERVAL_NANOS)
            return;
        lastUpdateNanos.put(playerRef, now);

        UICommandBuilder uiCommandBuilder = new UICommandBuilder();
        hud.updateThirstLevel(uiCommandBuilder, thirstLevel);
        hud.update(false, uiCommandBuilder);
    }

    static public void updatePlayerThirstRestorationPreview(@NonNullDecl PlayerRef playerRef,
            float thirstRestoration) {
        HHMThirstHud hud = hudMap.get(playerRef);
        if (hud == null)
            return;
        UICommandBuilder uiCommandBuilder = new UICommandBuilder();
        hud.updateThirstRestorationPreview(uiCommandBuilder, thirstRestoration);
        hud.update(false, uiCommandBuilder);
    }

    static public void updatePlayerGameMode(@NonNullDecl PlayerRef playerRef, GameMode gameMode) {
        HHMThirstHud hud = hudMap.get(playerRef);
        if (hud == null)
            return;
        UICommandBuilder uiCommandBuilder = new UICommandBuilder();
        hud.updateGameMode(uiCommandBuilder, gameMode);
        hud.update(false, uiCommandBuilder);
    }

    static public void updatePlayerHudPosition(@NonNullDecl PlayerRef playerRef, HudPosition hudPosition) {
        HHMThirstHud hud = hudMap.get(playerRef);
        if (hud == null)
            return;
        UICommandBuilder uiCommandBuilder = new UICommandBuilder();
        hud.updateHudPosition(uiCommandBuilder, hudPosition);
        hud.update(false, uiCommandBuilder);
    }

    static public void updatePlayerHudVisibility(@NonNullDecl PlayerRef playerRef, boolean visible) {
        HHMThirstHud hud = hudMap.get(playerRef);
        if (hud == null)
            return;
        UICommandBuilder uiCommandBuilder = new UICommandBuilder();
        hud.updateVisibility(uiCommandBuilder, visible);
        hud.update(false, uiCommandBuilder);
    }

    static public void refreshAllHuds(HHMThirstConfig config) {
        for (PlayerRef playerRef : hudMap.keySet()) {
            updatePlayerHudPosition(playerRef, config.getHudPosition());
            updatePlayerHudVisibility(playerRef, true);
        }
    }

    static public void createPlayerHud(
            @NonNullDecl Store<EntityStore> store,
            @NonNullDecl Ref<EntityStore> ref,
            @NonNullDecl PlayerRef playerRef,
            @NonNullDecl Player player) {
        ThirstComponent thirst = store.ensureAndGetComponent(ref, ThirstComponent.getComponentType());

        HHMThirstHud hud = new HHMThirstHud(playerRef, player.getGameMode(), thirst.getThirstLevel());
        player.getHudManager().addCustomHud(playerRef, hud);
    }
}
