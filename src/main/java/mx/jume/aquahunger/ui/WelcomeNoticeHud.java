package mx.jume.aquahunger.ui;

import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

/**
 * Non-blocking integration notice, shown as its own HUD so it never interferes with the bars
 * and disappears cleanly with removeCustomHud when dismissed (/aquahungerok).
 */
public class WelcomeNoticeHud extends AquaCustomHud {
    public static final String HUD_KEY = "AquaThirstHunger_Notice";

    private final String[] lines;
    private final String dismissText;

    public WelcomeNoticeHud(@NonNullDecl PlayerRef playerRef, @NonNullDecl String[] lines,
            @NonNullDecl String dismissText) {
        super(playerRef, HUD_KEY);
        this.lines = lines;
        this.dismissText = dismissText;
    }

    @Override
    protected void build(@NonNullDecl UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.append("Hungry/HUD/WelcomeNotice.ui");
        for (int i = 0; i < lines.length; i++) {
            uiCommandBuilder.set("#WelcomeNoticeLine" + (i + 1) + ".Text", lines[i]);
        }
        uiCommandBuilder.set("#WelcomeNoticeDismiss.Text", dismissText);
        uiCommandBuilder.set("#WelcomeNoticeRoot.Visible", true);
    }
}
