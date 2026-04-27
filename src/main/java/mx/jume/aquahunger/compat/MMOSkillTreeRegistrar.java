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

public final class MMOSkillTreeRegistrar {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String[] SKILLS = {"NUTRITION", "THIRST"};

    private static final String CUSTOM_SKILLS_FILE = "custom-skills.json";
    private static final String SKILL_TREE_FILE    = "skill-tree.json";
    private static final String XP_MAPS_FILE       = "xp-maps.json";

    private static boolean registered = false;

    private MMOSkillTreeRegistrar() {}

    public static void ensureRegistered() {
        if (registered) return;
        registered = true;

        Path mmoDir = resolveMMODir();
        if (mmoDir == null) {
            //AquaThirstHunger.logInfo("[mmo] Directorio mmoskilltree no encontrado, omitiendo registro.");
            return;
        }

        try {
            boolean skillInjected = injectCustomSkills(mmoDir);
            boolean treeInjected  = injectSkillTree(mmoDir);
            boolean xpInjected    = injectXpMaps(mmoDir);

            if (skillInjected || treeInjected || xpInjected) {
                //AquaThirstHunger.logInfo("[mmo] Skill trees (NUTRITION, THIRST) registrados/actualizados correctamente.");
            } else {
                //AquaThirstHunger.logInfo("[mmo] Skills ya estaban registrados correctamente.");
            }
        } catch (Exception e) {
            //System.err.println("[AquaHunger] Error al registrar skill tree: " + e.getMessage());
        }
    }

    private static boolean injectCustomSkills(Path mmoDir) throws IOException {
        Path file = mmoDir.resolve(CUSTOM_SKILLS_FILE);
        JsonObject root = readOrCreateObject(file);

        if (!root.has("schemaVersion")) root.addProperty("schemaVersion", 1);
        if (!root.has("skills")) root.add("skills", new JsonArray());
        if (!root.has("disabledBuiltinSkills")) root.add("disabledBuiltinSkills", new JsonArray());

        JsonArray currentSkills = root.getAsJsonArray("skills");
        JsonObject templateRoot = loadClasspathJson("/mmo/custom-skills.json").getAsJsonObject();
        JsonArray templateSkills = templateRoot.getAsJsonArray("skills");

        boolean changed = false;

        for (int t = 0; t < templateSkills.size(); t++) {
            JsonObject templateSkill = templateSkills.get(t).getAsJsonObject();
            String id = templateSkill.get("id").getAsString();

            // Eliminar si existe para sobreescribir con valores nuevos (iconos, etc)
            for (int i = currentSkills.size() - 1; i >= 0; i--) {
                JsonElement elem = currentSkills.get(i);
                if (elem.isJsonObject()) {
                    JsonObject obj = elem.getAsJsonObject();
                    if (obj.has("id") && id.equals(obj.get("id").getAsString())) {
                        currentSkills.remove(i);
                    }
                }
            }
            currentSkills.add(templateSkill);
            changed = true;
        }

        if (changed) writeJson(file, root);
        return changed;
    }

    private static boolean injectSkillTree(Path mmoDir) throws IOException {
        Path file = mmoDir.resolve(SKILL_TREE_FILE);
        JsonObject root = readOrCreateObject(file);

        if (!root.has("schemaVersion")) root.addProperty("schemaVersion", 3);
        if (!root.has("overrides")) root.add("overrides", new JsonObject());

        JsonObject overrides = root.getAsJsonObject("overrides");
        
        // Limpiamos los viejos en la raiz si existen (por si venian de una versión muy vieja)
        if (root.has("NUTRITION")) root.remove("NUTRITION");
        if (root.has("THIRST")) root.remove("THIRST");

        JsonObject templateRoot = loadClasspathJson("/mmo/skill-tree.json").getAsJsonObject();
        JsonObject templateOverrides = templateRoot.getAsJsonObject("overrides");

        boolean changed = false;
        for (String skillId : SKILLS) {
            if (templateOverrides.has(skillId)) {
                if (overrides.has(skillId)) {
                    overrides.remove(skillId); // Remover explícitamente y reemplazar
                }
                overrides.add(skillId, templateOverrides.get(skillId));
                changed = true;
            }
        }

        if (changed) writeJson(file, root);
        return changed;
    }

    private static boolean injectXpMaps(Path mmoDir) throws IOException {
        Path file = mmoDir.resolve(XP_MAPS_FILE);
        JsonObject root = readOrCreateObject(file);

        if (!root.has("schemaVersion")) root.addProperty("schemaVersion", 1);
        if (!root.has("xpMaps")) root.add("xpMaps", new JsonObject());

        JsonObject xpMaps = root.getAsJsonObject("xpMaps");
        JsonObject templateRoot = loadClasspathJson("/mmo/xp-maps.json").getAsJsonObject();
        JsonObject templateXpMaps = templateRoot.has("xpMaps") ? templateRoot.getAsJsonObject("xpMaps") : templateRoot;

        boolean changed = false;
        for (String skillId : SKILLS) {
            if (templateXpMaps.has(skillId)) {
                if (xpMaps.has(skillId)) {
                    xpMaps.remove(skillId);
                }
                xpMaps.add(skillId, templateXpMaps.get(skillId));
                changed = true;
            }
        }

        if (changed) writeJson(file, root);
        return changed;
    }

    private static Path resolveMMODir() {
        try {
            Path dataDir = AquaThirstHunger.get().getConfigManager().getDataDirectory();
            Path modsRoot = dataDir.getParent();
            if (modsRoot == null) return null;

            String[] folders = {"mmoskilltree", "mmoskills", "MMOSkillTree", "MMOSkills"};
            for (String folder : folders) {
                Path p = modsRoot.resolve(folder);
                if (Files.isDirectory(p)) {
                    //AquaThirstHunger.logInfo("[mmo] Directorio MMO detectado en: " + p.toAbsolutePath());
                    return p;
                }
            }
            // Fallback a mmoskilltree por defecto si no existe ninguno
            Path defaultPath = modsRoot.resolve("mmoskilltree");
            //AquaThirstHunger.logInfo("[mmo] No se detectó carpeta MMO existente, usando: " + defaultPath.toAbsolutePath());
            return defaultPath;
        } catch (Exception e) {
            //AquaThirstHunger.logWarning("[mmo] Error resolviendo directorio MMOSkillTree: " + e.getMessage());
            return null;
        }
    }

    private static JsonObject readOrCreateObject(Path file) throws IOException {
        if (Files.exists(file)) {
            try (Reader reader = new InputStreamReader(Files.newInputStream(file), StandardCharsets.UTF_8)) {
                JsonElement elem = JsonParser.parseReader(reader);
                if (elem != null && elem.isJsonObject()) return elem.getAsJsonObject();
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

    private static void writeJson(Path file, JsonElement json) throws IOException {
        Files.createDirectories(file.getParent());
        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(file), StandardCharsets.UTF_8)) {
            GSON.toJson(json, writer);
        }
    }
}
