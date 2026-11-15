package mx.jume.aquahunger.compat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import mx.jume.aquahunger.AquaThirstHunger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LangManager {
    private static final Map<String, Map<String, String>> langCache = new ConcurrentHashMap<>();
    private static final Gson GSON = new Gson();
    
    private static final Map<String, String> SHORT_CODE_MAP = Map.of(
        "en", "en_US",
        "es", "es_ES",
        "fr", "fr_FR"
    );

    public static @javax.annotation.Nonnull String getForLanguage(String hytaleLang, String key) {
        if (hytaleLang == null) hytaleLang = "en-US";
        String langCode = hytaleLang.replace('-', '_');

        Map<String, String> messages = loadLanguage(langCode);
        if (messages == null || !messages.containsKey(key)) {
            // Fallback to family
            String family = langCode.length() >= 2 ? langCode.substring(0, 2).toLowerCase() : "en";
            String fallback = SHORT_CODE_MAP.getOrDefault(family, "en_US");
            messages = loadLanguage(fallback);
        }
        if (messages == null || !messages.containsKey(key)) {
            // A translation that lacks this key: English instead of showing the raw key
            messages = loadLanguage("en_US");
        }

        String result = (messages != null) ? messages.getOrDefault(key, key) : key;
        return result != null ? result : key;
    }

    private static Map<String, String> loadLanguage(String langCode) {
        return langCache.computeIfAbsent(langCode, code -> {
            try (InputStream is = LangManager.class.getResourceAsStream("/lang/" + code + ".json")) {
                if (is == null) {
                    //AquaThirstHunger.logWarning("[aquahunger-Lang] Language file not found: " + code);
                    return null;
                }
                Map<String, String> map = GSON.fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), 
                        new TypeToken<Map<String, String>>(){}.getType());
                if (map != null) {
                    //AquaThirstHunger.logInfo("[aquahunger-Lang] Idioma cargado: " + code);
                }
                return map;
            } catch (Exception e) {
                //AquaThirstHunger.logWarning("[aquahunger-Lang] Error loading language " + code + ": " + e.getMessage());
                return null;
            }
        });
    }
}
