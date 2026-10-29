package mx.jume.aquahunger.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigMigrationTest {

    @Test
    void thirstConfigFromPreviousVersionsKeepsItsDehydrationDamageCadence() {
        HHMThirstConfig config = new HHMThirstConfig();
        config.setConfigVersion("1.7.5");
        config.setDepletionTickRate(2.0f);
        config.setDamageIntervalSeconds(10.0f); // was ignored before 1.8.0

        ConfigMigrationManager.migrate(config);

        assertEquals(2.0f, config.getDamageIntervalSeconds());
        assertEquals(ConfigMigrationManager.LATEST_VERSION, config.getConfigVersion());
    }

    @Test
    void currentThirstConfigIsLeftAlone() {
        HHMThirstConfig config = new HHMThirstConfig();
        config.setDepletionTickRate(2.0f);
        config.setDamageIntervalSeconds(10.0f);

        ConfigMigrationManager.migrate(config);

        assertEquals(10.0f, config.getDamageIntervalSeconds());
    }

    @Test
    void thirstFoodValuesWithoutVersionDoNotCrash() {
        HHMThirstFoodValuesConfig config = new HHMThirstFoodValuesConfig();
        config.setConfigVersion(null);

        assertDoesNotThrow(() -> ConfigMigrationManager.migrate(config));
        assertEquals(ConfigMigrationManager.LATEST_VERSION, config.getConfigVersion());
    }

    @Test
    void freshDefaultsKeepTheOldDamageCadence() {
        HHMThirstConfig config = new HHMThirstConfig();
        assertEquals(config.getDepletionTickRate(), config.getDamageIntervalSeconds());
    }

    @Test
    void sanitizeClampsValuesThatWouldBreakTheSystems() {
        HHMHungerConfig hunger = new HHMHungerConfig();
        hunger.setStarvationTickRate(0f);
        hunger.setMaxHungerLevel(50f);
        hunger.setInitialHungerLevel(300f);
        hunger.setPulseInterval(-1f);
        hunger.sanitize();

        assertEquals(0.1f, hunger.getStarvationTickRate());
        assertEquals(100f, hunger.getMaxHungerLevel());
        assertEquals(100f, hunger.getInitialHungerLevel());
        assertEquals(0.1f, hunger.getPulseInterval());

        HHMThirstConfig thirst = new HHMThirstConfig();
        thirst.setDamageIntervalSeconds(0f);
        thirst.setStaminaBoostThreshold(150f); // above 100 switches the boost off and must survive
        thirst.sanitize();

        assertEquals(0.1f, thirst.getDamageIntervalSeconds());
        assertEquals(150f, thirst.getStaminaBoostThreshold());
    }
}
