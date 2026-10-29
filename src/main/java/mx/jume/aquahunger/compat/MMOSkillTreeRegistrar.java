package mx.jume.aquahunger.compat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mx.jume.aquahunger.AquaThirstHunger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Writes our NUTRITION and THIRST skills into MMO Skill Tree's owner config files.
 *
 * <p>MMO reads {@code custom-skills.json}, {@code xp-maps.json} and {@code skill-tree.json} once,
 * in its own setup, and only keeps skill-tree overrides for skills that are already registered
 * at that moment. So the skills cannot come from a content pack (packs load later) and the files
 * must be written before MMO's setup: the manifest declares {@code LoadBefore: Ziggfreed:MMOSkillTree}
 * and this runs from our setup. MMO 1.7 still accepts a skill-tree override written as a list of tiers.
 */
public final class MMOSkillTreeRegistrar {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String[] SKILLS = {"NUTRITION", "THIRST"};

    /** Same relative path MMO Skill Tree uses for its owner config. */
    static final Path MMO_CONFIG_DIR = Paths.get("mods", "mmoskilltree");

    private static final String CUSTOM_SKILLS_FILE = "custom-skills.json";
    private static final String SKILL_TREE_FILE    = "skill-tree.json";
    private static final String XP_MAPS_FILE       = "xp-maps.json";

    private static boolean registered = false;

    private MMOSkillTreeRegistrar() {}

    public static void ensureRegistered() {
        if (registered) return;
        registered = true;

        try {
            if (register(MMO_CONFIG_DIR)) {
                AquaThirstHunger.logInfo("[mmo] NUTRITION and THIRST skills written to " + MMO_CONFIG_DIR + ".");
            } else {
                AquaThirstHunger.logDebug("[mmo] NUTRITION and THIRST skills already up to date in " + MMO_CONFIG_DIR + ".");
            }
        } catch (Exception e) {
            AquaThirstHunger.logWarning("[mmo] Could not write the NUTRITION and THIRST skills to " + MMO_CONFIG_DIR
                    + " (" + e + "). MMO Skill Tree will start without them.");
        }
    }

    /**
     * Puts our skills into the three files under {@code mmoDir}, replacing earlier versions of them
     * and keeping everything else. A file is only rewritten when its content changes.
     *
     * @return true if any file was written
     */
    static boolean register(Path mmoDir) throws IOException {
        boolean skillInjected = injectCustomSkills(mmoDir);
        boolean treeInjected  = injectSkillTree(mmoDir);
        boolean xpInjected    = injectXpMaps(mmoDir);
        return skillInjected || treeInjected || xpInjected;
    }

    private static boolean injectCustomSkills(Path mmoDir) throws IOException {
        Path file = mmoDir.resolve(CUSTOM_SKILLS_FILE);
        JsonObject root = readOrCreateObject(file);
        JsonObject before = root.deepCopy();

        if (!root.has("schemaVersion")) root.addProperty("schemaVersion", 1);
        if (!root.has("skills") || !root.get("skills").isJsonArray()) root.add("skills", new JsonArray());
        if (!root.has("disabledBuiltinSkills")) root.add("disabledBuiltinSkills", new JsonArray());

        JsonArray currentSkills = root.getAsJsonArray("skills");
        JsonObject templateRoot = loadClasspathJson("/mmo/custom-skills.json").getAsJsonObject();
        JsonArray templateSkills = templateRoot.getAsJsonArray("skills");

        for (int t = 0; t < templateSkills.size(); t++) {
            JsonObject templateSkill = templateSkills.get(t).getAsJsonObject();
            String id = templateSkill.get("id").getAsString();

            // Eliminar si existe para sobreescribir con valores nuevos (iconos, etc)
            for (int i = currentSkills.size() - 1; i >= 0; i--) {
                JsonElement elem = currentSkills.get(i);
                if (elem.isJsonObject()) {
                    JsonObject obj = elem.getAsJsonObject();
                    if (obj.has("id") && id.equalsIgnoreCase(obj.get("id").getAsString())) {
                        currentSkills.remove(i);
                    }
                }
            }
            currentSkills.add(templateSkill);
        }

        return writeIfChanged(file, before, root);
    }

    private static boolean injectSkillTree(Path mmoDir) throws IOException {
        Path file = mmoDir.resolve(SKILL_TREE_FILE);
        JsonObject root = readOrCreateObject(file);
        JsonObject before = root.deepCopy();

        if (!root.has("schemaVersion")) root.addProperty("schemaVersion", 3);
        if (!root.has("overrides") || !root.get("overrides").isJsonObject()) root.add("overrides", new JsonObject());

        JsonObject overrides = root.getAsJsonObject("overrides");

        // Limpiamos los viejos en la raiz si existen (por si venian de una versión muy vieja)
        if (root.has("NUTRITION")) root.remove("NUTRITION");
        if (root.has("THIRST")) root.remove("THIRST");

        JsonObject templateRoot = loadClasspathJson("/mmo/skill-tree.json").getAsJsonObject();
        JsonObject templateOverrides = templateRoot.getAsJsonObject("overrides");

        for (String skillId : SKILLS) {
            if (templateOverrides.has(skillId)) {
                overrides.add(skillId, templateOverrides.get(skillId));
            }
        }

        return writeIfChanged(file, before, root);
    }

    private static boolean injectXpMaps(Path mmoDir) throws IOException {
        Path file = mmoDir.resolve(XP_MAPS_FILE);
        JsonObject root = readOrCreateObject(file);
        JsonObject before = root.deepCopy();

        if (!root.has("schemaVersion")) root.addProperty("schemaVersion", 1);
        if (!root.has("xpMaps") || !root.get("xpMaps").isJsonObject()) root.add("xpMaps", new JsonObject());

        JsonObject xpMaps = root.getAsJsonObject("xpMaps");
        JsonObject templateRoot = loadClasspathJson("/mmo/xp-maps.json").getAsJsonObject();
        JsonObject templateXpMaps = templateRoot.has("xpMaps") ? templateRoot.getAsJsonObject("xpMaps") : templateRoot;

        for (String skillId : SKILLS) {
            if (templateXpMaps.has(skillId)) {
                xpMaps.add(skillId, templateXpMaps.get(skillId));
            }
        }

        return writeIfChanged(file, before, root);
    }

    /** An existing file that is not a JSON object throws, so an owner's broken file is never replaced. */
    private static JsonObject readOrCreateObject(Path file) throws IOException {
        if (Files.exists(file)) {
            try (Reader reader = new InputStreamReader(Files.newInputStream(file), StandardCharsets.UTF_8)) {
                JsonElement elem = JsonParser.parseReader(reader);
                if (elem != null && elem.isJsonObject()) return elem.getAsJsonObject();
                if (elem == null || elem.isJsonNull()) return new JsonObject();
                throw new IOException(file + " is not a JSON object");
            } catch (com.google.gson.JsonParseException e) {
                throw new IOException(file + " is not valid JSON: " + e.getMessage(), e);
            }
        }
        return new JsonObject();
    }

    private static JsonElement loadClasspathJson(String path) throws IOException {
        try (InputStream is = MMOSkillTreeRegistrar.class.getResourceAsStream(path)) {
            if (is == null) throw new IOException("Template not found: " + path);
            try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader);
            }
        }
    }

    private static boolean writeIfChanged(Path file, JsonObject before, JsonObject after) throws IOException {
        if (Files.exists(file) && before.equals(after)) {
            return false;
        }
        Files.createDirectories(file.getParent());
        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(file), StandardCharsets.UTF_8)) {
            GSON.toJson(after, writer);
        }
        return true;
    }
}
