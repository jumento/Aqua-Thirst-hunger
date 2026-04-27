package mx.jume.aquahunger.test;

import java.lang.reflect.Method;
import mx.jume.aquahunger.AquaThirstHunger;

public class APIDiscovery {
    public static void discover() {
        try {
            Class<?> apiClass = Class.forName("com.zuxaw.rpgleveling.api.RPGLevelingAPI");
            //AquaThirstHunger.logInfo("Found RPGLevelingAPI. Methods:");
            for (Method m : apiClass.getMethods()) {
                //AquaThirstHunger.logInfo(" - " + m.getName() + "(" + java.util.Arrays.toString(m.getParameterTypes()) + ")");
            }
        } catch (Exception e) {
            //AquaThirstHunger.logInfo("RPGLevelingAPI not found: " + e.getMessage());
        }
    }
}
