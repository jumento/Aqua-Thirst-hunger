package mx.jume.aquahunger.compat;

import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.server.core.plugin.PluginBase;
import com.hypixel.hytale.server.core.plugin.PluginManager;
import com.hypixel.hytale.server.core.plugin.PluginState;

import javax.annotation.Nullable;

/**
 * Lookup of the optional mods we integrate with, by their real plugin IDs.
 *
 * <p>Since Hytale 0.7 a plugin's class loader only sees the plugins its manifest declares, and
 * MMO Skill Tree is declared as {@code LoadBefore} instead of an optional dependency (its skill
 * files have to be written before it reads them in its own setup). So classes of these mods are
 * loaded through the mod's own class loader, which works however the mod is declared.
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
     * Loads a class of an optional plugin through that plugin's own class loader. The class is not
     * initialized here: MMO Skill Tree starts after us, so its static state is left for the first real call.
     */
    public static Class<?> loadClass(PluginIdentifier id, String className) throws ClassNotFoundException {
        PluginBase plugin = find(id);
        if (plugin == null) {
            throw new ClassNotFoundException(className + " (" + id + " is not loaded)");
        }
        return Class.forName(className, false, plugin.getClass().getClassLoader());
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
