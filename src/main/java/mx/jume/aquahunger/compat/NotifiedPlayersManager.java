package mx.jume.aquahunger.compat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import mx.jume.aquahunger.AquaThirstHunger;

import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class NotifiedPlayersManager {
    private static Set<String> notifiedUuids = ConcurrentHashMap.newKeySet();
    private static Path filePath;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void load(Path dataDir) {
        try {
            if (!Files.exists(dataDir)) {
                Files.createDirectories(dataDir);
            }
            filePath = dataDir.resolve("notified_players.json");
            
            if (Files.exists(filePath)) {
                try (FileReader reader = new FileReader(filePath.toFile())) {
                    Set<String> set = GSON.fromJson(reader, new TypeToken<HashSet<String>>(){}.getType());
                    if (set != null) notifiedUuids.addAll(set);
                }
            }
        } catch (Exception e) {
            //AquaThirstHunger.logInfo("[Notice] Error loading notified players: " + e.getMessage());
        }
    }

    public static boolean isNotified(String uuid) {
        return notifiedUuids.contains(uuid);
    }

    public static void markNotified(String uuid) {
        if (uuid == null) return;
        notifiedUuids.add(uuid);
        save();
        writeToAquaSanityNotified(uuid);
    }

    public static void unmarkNotified(String uuid) {
        if (uuid == null) return;
        notifiedUuids.remove(uuid);
        save();
    }

    private static synchronized void save() {
        if (filePath == null) return;
        try (FileWriter writer = new FileWriter(filePath.toFile())) {
            GSON.toJson(notifiedUuids, writer);
        } catch (Exception e) {
            //AquaThirstHunger.logInfo("[Notice] Error saving notified players: " + e.getMessage());
        }
    }

    private static void writeToAquaSanityNotified(String uuid) {
        if (filePath == null) return;
        try {
            // Path: plugins/AquaThirstHunger/notified_players.json -> plugins/AquaSanity/notified_players.json
            Path aquaSanityPath = filePath.getParent().getParent().resolve("AquaSanity").resolve("notified_players.json");
            if (Files.isDirectory(aquaSanityPath.getParent())) {
                Set<String> asNotified = new HashSet<>();
                if (Files.exists(aquaSanityPath)) {
                    try (FileReader reader = new FileReader(aquaSanityPath.toFile())) {
                        Set<String> set = GSON.fromJson(reader, new TypeToken<HashSet<String>>(){}.getType());
                        if (set != null) asNotified.addAll(set);
                    }
                }
                
                if (!asNotified.contains(uuid)) {
                    asNotified.add(uuid);
                    try (FileWriter writer = new FileWriter(aquaSanityPath.toFile())) {
                        GSON.toJson(asNotified, writer);
                    }
                }
            }
        } catch (Exception e) {
            //AquaThirstHunger.logWarning("[aquahunger] Error syncing with AquaSanity notified players: " + e.getMessage());
        }
    }
}
