package mx.jume.aquahunger.ui;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import mx.jume.aquahunger.compat.LangManager;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Applies the fields submitted by a config page one by one, so a single invalid number no longer
 * discards the rest of the form, and reports the fields that were ignored.
 */
final class ConfigForm {
    private final List<String> invalidFields = new ArrayList<>();
    private boolean modified = false;

    /** Applies a numeric field when it parses and differs from the current value. */
    void number(String key, String raw, float current, Consumer<Float> setter) {
        Float value = parse(raw);
        if (value == null) {
            if (raw != null) {
                invalidFields.add(key);
            }
            return;
        }
        if (value != current) {
            setter.accept(value);
            modified = true;
        }
    }

    void bool(boolean value, boolean current, Consumer<Boolean> setter) {
        if (value != current) {
            setter.accept(value);
            modified = true;
        }
    }

    boolean isModified() {
        return modified;
    }

    void reportInvalid(PlayerRef playerRef) {
        if (invalidFields.isEmpty()) {
            return;
        }
        String text = String.format(LangManager.getForLanguage(playerRef.getLanguage(), "ui.config.invalid_fields"),
                String.join(", ", invalidFields));
        playerRef.sendMessage(Message.empty().insert(text).color("#FF5555"));
    }

    /** Accepts "0.5" and "0,5"; null for anything that is not a finite number. */
    static Float parse(String raw) {
        if (raw == null) {
            return null;
        }
        String text = raw.trim().replace(',', '.');
        if (text.isEmpty()) {
            return null;
        }
        try {
            float value = Float.parseFloat(text);
            return Float.isFinite(value) ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Shows whole numbers without decimals and keeps fractions, so saving the page never truncates values. */
    static String format(float value) {
        if (value == Math.rint(value) && Math.abs(value) < 1.0e7f) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }
}
