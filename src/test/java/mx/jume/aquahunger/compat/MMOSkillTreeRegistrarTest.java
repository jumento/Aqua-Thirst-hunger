package mx.jume.aquahunger.compat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MMOSkillTreeRegistrarTest {

    @TempDir
    Path mmoDir;

    @Test
    void freshDirectoryGetsTheThreeFiles() throws IOException {
        assertTrue(MMOSkillTreeRegistrar.register(mmoDir));

        JsonObject custom = read("custom-skills.json");
        assertEquals(Set.of("NUTRITION", "THIRST"), skillIds(custom));

        JsonObject overrides = read("skill-tree.json").getAsJsonObject("overrides");
        assertTrue(overrides.get("NUTRITION").isJsonArray());
        assertTrue(overrides.get("THIRST").isJsonArray());

        JsonObject xpMaps = read("xp-maps.json").getAsJsonObject("xpMaps");
        assertTrue(xpMaps.has("NUTRITION"));
        assertTrue(xpMaps.has("THIRST"));
    }

    @Test
    void secondRunLeavesTheFilesAlone() throws IOException {
        MMOSkillTreeRegistrar.register(mmoDir);
        String before = Files.readString(mmoDir.resolve("skill-tree.json"));

        assertFalse(MMOSkillTreeRegistrar.register(mmoDir));
        assertEquals(before, Files.readString(mmoDir.resolve("skill-tree.json")));
    }

    @Test
    void keepsTheOwnerConfigAndReplacesOurOldEntries() throws IOException {
        write("custom-skills.json", """
                {"schemaVersion": 1, "defaultMaxLevel": 150,
                 "skills": [{"id": "HERBALISM", "displayName": "Herbalism"},
                            {"id": "nutrition", "displayName": "Old name"}],
                 "disabledBuiltinSkills": ["TAMING"]}
                """);
        // MMO 1.7 writes overrides as a map of tiers (schema 5).
        write("skill-tree.json", """
                {"schemaVersion": 5,
                 "overrides": {"MINING": {"0": {"tier": 0, "levelRequired": 5}},
                               "NUTRITION": {"0": {"tier": 0, "levelRequired": 1}}}}
                """);
        write("xp-maps.json", """
                {"schemaVersion": 1, "xpMaps": {"MINING": {"Rock_": 3}}}
                """);

        assertTrue(MMOSkillTreeRegistrar.register(mmoDir));

        JsonObject custom = read("custom-skills.json");
        assertEquals(Set.of("HERBALISM", "NUTRITION", "THIRST"), skillIds(custom));
        assertEquals(150, custom.get("defaultMaxLevel").getAsInt());
        assertEquals("TAMING", custom.getAsJsonArray("disabledBuiltinSkills").get(0).getAsString());

        JsonObject tree = read("skill-tree.json");
        assertEquals(5, tree.get("schemaVersion").getAsInt());
        JsonObject overrides = tree.getAsJsonObject("overrides");
        assertEquals(5, overrides.getAsJsonObject("MINING").getAsJsonObject("0").get("levelRequired").getAsInt());
        assertEquals(10, overrides.getAsJsonArray("NUTRITION").size());

        JsonObject xpMaps = read("xp-maps.json").getAsJsonObject("xpMaps");
        assertEquals(3, xpMaps.getAsJsonObject("MINING").get("Rock_").getAsInt());
        assertTrue(xpMaps.has("NUTRITION"));
    }

    @Test
    void invalidOwnerFileIsNotReplaced() throws IOException {
        String broken = "{\"schemaVersion\": 1, \"skills\": [";
        write("custom-skills.json", broken);

        assertThrows(IOException.class, () -> MMOSkillTreeRegistrar.register(mmoDir));
        assertEquals(broken, Files.readString(mmoDir.resolve("custom-skills.json")));
    }

    @Test
    void templateTiersAreTierListsMmoAccepts() throws IOException {
        JsonObject overrides = template("/mmo/skill-tree.json").getAsJsonObject("overrides");
        for (String skill : new String[] {"NUTRITION", "THIRST"}) {
            JsonArray tiers = overrides.getAsJsonArray(skill);
            Set<Integer> seen = new HashSet<>();
            for (JsonElement tier : tiers) {
                seen.add(tier.getAsJsonObject().get("tier").getAsInt());
            }
            assertEquals(Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), seen, skill);
        }
    }

    @Test
    void everyRewardTheBridgeReadsExistsInTheTree() throws IOException {
        Set<String> choices = new HashSet<>();
        JsonObject overrides = template("/mmo/skill-tree.json").getAsJsonObject("overrides");
        for (String skill : overrides.keySet()) {
            for (JsonElement tier : overrides.getAsJsonArray(skill)) {
                for (JsonElement choice : tier.getAsJsonObject().getAsJsonArray("choices")) {
                    choices.add(choice.getAsJsonObject().get("id").getAsString());
                }
            }
        }

        for (String id : new String[] {
                MMOSkillTreeBridge.NUTRITION_KING_FITNESS, MMOSkillTreeBridge.NUTRITION_MEGA_GLUTTON,
                MMOSkillTreeBridge.NUTRITION_HUNGRY_SUPREME, MMOSkillTreeBridge.THIRST_HYDRATION_MASTER,
                MMOSkillTreeBridge.THIRST_SUPER_HEALTHY}) {
            assertTrue(choices.contains(id), id);
        }
        for (String[] glutton : MMOSkillTreeBridge.NUTRITION_GLUTTON_ITEMS) {
            assertTrue(choices.contains(glutton[0]), glutton[0]);
        }
        for (int i = 1; i <= 9; i++) {
            assertTrue(choices.contains("thirst_rehydrated_" + i), "thirst_rehydrated_" + i);
        }
        for (String prefix : MMOSkillTreeBridge.REWARD_PREFIXES) {
            assertTrue(choices.stream().anyMatch(id -> id.startsWith(prefix)), prefix);
        }
    }

    private static Set<String> skillIds(JsonObject customSkills) {
        Set<String> ids = new HashSet<>();
        for (JsonElement skill : customSkills.getAsJsonArray("skills")) {
            ids.add(skill.getAsJsonObject().get("id").getAsString().toUpperCase());
        }
        assertEquals(customSkills.getAsJsonArray("skills").size(), ids.size(), "duplicated skill id");
        return ids;
    }

    private JsonObject read(String name) throws IOException {
        return JsonParser.parseString(Files.readString(mmoDir.resolve(name))).getAsJsonObject();
    }

    private void write(String name, String json) throws IOException {
        Files.writeString(mmoDir.resolve(name), json);
    }

    private static JsonObject template(String path) throws IOException {
        try (InputStream is = MMOSkillTreeRegistrarTest.class.getResourceAsStream(path)) {
            assertNotNull(is, path);
            return JsonParser.parseReader(new InputStreamReader(is, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}
