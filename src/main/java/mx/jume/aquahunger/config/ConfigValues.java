package mx.jume.aquahunger.config;

import java.util.logging.Logger;

/**
 * Range checks shared by the config classes. Out-of-range values (hand-edited files, typos in
 * the config pages) are clamped and logged instead of breaking the systems, e.g. a tick rate of
 * 0 would deplete hunger on every frame.
 */
final class ConfigValues {
    private static final Logger LOGGER = Logger.getLogger("AquaThirstHunger");

    private ConfigValues() {
    }

    static float atLeast(String file, String key, float value, float min) {
        if (Float.isNaN(value) || value < min) {
            LOGGER.warning("[Config] " + file + " " + key + "=" + value + " is out of range, using " + min);
            return min;
        }
        return value;
    }

    static float within(String file, String key, float value, float min, float max) {
        if (Float.isNaN(value) || value < min || value > max) {
            float clamped = Float.isNaN(value) ? min : Math.max(min, Math.min(value, max));
            LOGGER.warning("[Config] " + file + " " + key + "=" + value + " is out of range, using " + clamped);
            return clamped;
        }
        return value;
    }
}
