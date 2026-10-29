package mx.jume.aquahunger.components;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThirstComponentTest {

    @Test
    void dehydrationDamageHitsImmediatelyThenEveryInterval() {
        ThirstComponent thirst = new ThirstComponent(0f);

        assertTrue(thirst.tickDehydrationDamage(0.05f, 2.0f), "first hit as soon as thirst is 0");
        assertFalse(thirst.tickDehydrationDamage(1.0f, 2.0f));
        assertTrue(thirst.tickDehydrationDamage(1.0f, 2.0f));
        assertFalse(thirst.tickDehydrationDamage(0.5f, 2.0f));

        thirst.resetDehydrationDamage();
        assertTrue(thirst.tickDehydrationDamage(0.05f, 2.0f), "ready again after drinking");
    }

    @Test
    void loweringTheMaximumClampsTheLevel() {
        ThirstComponent thirst = new ThirstComponent(90f);
        thirst.setMaxThirst(125f);
        thirst.setThirstLevel(120f);
        thirst.setMaxThirst(100f);
        assertEquals(100f, thirst.getThirstLevel());
    }

    @Test
    void idleTimeAccumulatesAndResetsOnMovementOrBlockHits() {
        ThirstComponent thirst = new ThirstComponent(50f);
        thirst.trackIdle(true, 2f);
        thirst.trackIdle(true, 4f);
        assertEquals(6f, thirst.getIdleTime());

        thirst.incrementBlockHits();
        assertEquals(0f, thirst.getIdleTime());

        thirst.trackIdle(true, 3f);
        thirst.trackIdle(false, 0.05f);
        assertEquals(0f, thirst.getIdleTime());
    }

    @Test
    void copyKeepsTransientState() {
        ThirstComponent thirst = new ThirstComponent(0f);
        thirst.setStaminaBoostApplied(true);
        thirst.setAwardedResilienceXP(true);
        thirst.tickDehydrationDamage(0.05f, 2.0f);

        ThirstComponent copy = (ThirstComponent) thirst.clone();
        assertTrue(copy.isStaminaBoostApplied());
        assertTrue(copy.hasAwardedResilienceXP());
        assertFalse(copy.tickDehydrationDamage(0.5f, 2.0f), "timer carried over, not ready again");
    }
}
