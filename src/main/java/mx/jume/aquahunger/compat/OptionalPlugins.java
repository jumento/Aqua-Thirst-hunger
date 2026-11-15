package mx.jume.aquahunger.compat;

import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.server.core.plugin.PluginBase;
import com.hypixel.hytale.server.core.plugin.PluginManager;
import com.hypixel.hytale.server.core.plugin.PluginState;

import javax.annotation.Nullable;

/**
 * Lookup of the optional mods we integrate with, by their real plugin IDs.
 *
 * <p>Since Hytale 0.7 a plugin's class loader only sees the plugins its manifest declares. The
 * three leveling mods are declared as OptionalDependencies, so Hytale loads them before us and
 * their classes resolve through our own class loader with a plain {@code Class.forName}.
 * Whether a mod is installed is asked to the plugin manager, which needs no class loading.
 */
public final class OptionalPlugins {
    public static final PluginIdentifier RPG_LEVELING = new PluginIdentifier("Zuxaw", "RPGLeveling");
    public static final PluginIdentifier ENDLESS_LEVELING = new PluginIdentifier("Airijko", "EndlessLevelingCore");
    public static final PluginIdentifier MMO_SKILL_TREE = new PluginIdentifier("Ziggfreed", "MMOSkillTree");
    public static final PluginIdentifier AQUA_SANITY = new PluginIdentifier("mx.jume.aquasanity", "AquaSanity");

    private OptionalPlugins() {
    }

    /** @return the plugin, or null when it is not installed or failed to load. */
    @Nullable
    public static PluginBase find(PluginIdentifier id) {
        PluginManager manager = PluginManager.get();
        if (manager == null) {
            return null;
        }
        PluginBase plugin = manager.getPlugin(id);
        if (plugin == null) {
            return null;
        }
        PluginState state = plugin.getState();
        if (state == PluginState.FAILED || state == PluginState.DISABLED || state == PluginState.SHUTDOWN) {
            return null;
        }
        return plugin;
    }

    public static boolean isPresent(PluginIdentifier id) {
        return find(id) != null;
    }

    /** @return the version from the plugin's manifest, or null when it is not installed. */
    @Nullable
    public static String version(PluginIdentifier id) {
        PluginBase plugin = find(id);
        if (plugin == null || plugin.getManifest() == null || plugin.getManifest().getVersion() == null) {
            return null;
        }
        return plugin.getManifest().getVersion().toString();
    }

    /**
     * Loads a class of an optional mod. Call it from start() or later: the mod has started by then
     * (optional dependencies start before us), so initializing its class is safe.
     */
    public static Class<?> loadClass(PluginIdentifier id, String className) throws ClassNotFoundException {
        if (find(id) == null) {
            throw new ClassNotFoundException(className + " (" + id + " is not loaded)");
        }
        return Class.forName(className);
    }

    /** Like {@link #loadClass} but tries each name in order, for classes a mod moved between versions. */
    public static Class<?> loadFirstClass(PluginIdentifier id, String... classNames) throws ClassNotFoundException {
        for (String className : classNames) {
            try {
                return loadClass(id, className);
            } catch (ClassNotFoundException ignored) {
                // try the next name
            }
        }
        throw new ClassNotFoundException(String.join(", ", classNames));
    }
}
