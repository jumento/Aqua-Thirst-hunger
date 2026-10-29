package mx.jume.aquahunger.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HudBarMathTest {
    private static final float EPS = 1.0e-6f;

    @Test
    void hungerBarFillsUpToOneHundred() {
        assertEquals(0.5f, HHMHud.hungerBarValue(50f), EPS);
        assertEquals(1.0f, HHMHud.hungerBarValue(150f), EPS);
        assertEquals(0.0f, HHMHud.hungerBarValue(-5f), EPS);
    }

    @Test
    void saturationIsRelativeToTheConfiguredMaximum() {
        assertEquals(0.5f, HHMHud.saturationBarValue(150f, 200f), EPS); // default max: same as before
        assertEquals(1.0f, HHMHud.saturationBarValue(150f, 150f), EPS); // Hard preset max
        assertEquals(0.0f, HHMHud.saturationBarValue(100f, 100f), EPS); // no saturation range
        assertEquals(1.0f, HHMHud.saturationBarValue(250f, 200f), EPS);
        assertEquals(0.0f, HHMHud.saturationBarValue(80f, 200f), EPS);
    }

    @Test
    void thirstBarUsesTheRealMaximum() {
        assertEquals(0.5f, HHMThirstHud.thirstBarValue(50f, 100f), EPS);
        assertEquals(0.8f, HHMThirstHud.thirstBarValue(100f, 125f), EPS); // level bonus raised the max
        assertEquals(1.0f, HHMThirstHud.thirstBarValue(130f, 125f), EPS);
        assertEquals(0.0f, HHMThirstHud.thirstBarValue(10f, 0f), EPS);
    }
}
