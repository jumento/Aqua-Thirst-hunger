package mx.jume.aquahunger.compat;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;

public class SuperlativeManager {
    private static String mostGluttonousName = "---";
    private static float mostGluttonousValue = 0;

    private static String mostHungryName = "---";
    private static float mostHungryValue = 0;

    private static String mostThirstyName = "---";
    private static float mostThirstyValue = 0;

    private static String mostDehydratedName = "---";
    private static float mostDehydratedValue = 0;

    public static void checkGluttony(PlayerRef player, float value) {
        if (player == null) return;
        if (value > mostGluttonousValue) {
            mostGluttonousValue = value;
            mostGluttonousName = player.getUsername();
        }
    }

    public static void checkHungry(PlayerRef player, float value) {
        if (player == null) return;
        if (value > mostHungryValue) {
            mostHungryValue = value;
            mostHungryName = player.getUsername();
        }
    }

    public static void checkThirsty(PlayerRef player, float value) {
        if (player == null) return;
        if (value > mostThirstyValue) {
            mostThirstyValue = value;
            mostThirstyName = player.getUsername();
        }
    }

    public static void checkDehydrated(PlayerRef player, float value) {
        if (player == null) return;
        if (value > mostDehydratedValue) {
            mostDehydratedValue = value;
            mostDehydratedName = player.getUsername();
        }
    }

    public static void showSuperlatives(PlayerRef viewer) {
        if (viewer == null) return;
        String lang = viewer.getLanguage();
        Message msg = Message.empty()
            .insert(LangManager.getForLanguage(lang, "superlative.header")).color("#F1C40F")
            .insert(LangManager.getForLanguage(lang, "superlative.most_gluttonous")).color("#E67E22")
            .insert(mostGluttonousName + " (" + (int)mostGluttonousValue + ")\n").color("#FFFFFF")
            .insert(LangManager.getForLanguage(lang, "superlative.most_hungry")).color("#C0392B")
            .insert(mostHungryName + " (" + (int)mostHungryValue + ")\n").color("#FFFFFF")
            .insert(LangManager.getForLanguage(lang, "superlative.most_thirsty")).color("#3498DB")
            .insert(mostThirstyName + " (" + (int)mostThirstyValue + ")\n").color("#FFFFFF")
            .insert(LangManager.getForLanguage(lang, "superlative.most_dehydrated")).color("#2980B9")
            .insert(mostDehydratedName + " (" + (int)mostDehydratedValue + ")").color("#FFFFFF");
        viewer.sendMessage(msg);
    }
}
