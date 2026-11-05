package mx.jume.aquahunger.ui;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigFormTest {

    @Test
    void parsesDotAndCommaDecimals() {
        assertEquals(0.5f, ConfigForm.parse("0,5"));
        assertEquals(2.0f, ConfigForm.parse(" 2 "));
        assertEquals(0.03f, ConfigForm.parse("0.03"));
    }

    @Test
    void rejectsAnythingThatIsNotAFiniteNumber() {
        assertNull(ConfigForm.parse(null));
        assertNull(ConfigForm.parse(""));
        assertNull(ConfigForm.parse("abc"));
        assertNull(ConfigForm.parse("NaN"));
        assertNull(ConfigForm.parse("Infinity"));
    }

    @Test
    void formatNeverTruncatesFractions() {
        assertEquals("2", ConfigForm.format(2.0f));
        assertEquals("1.5", ConfigForm.format(1.5f));
        assertEquals("0.03", ConfigForm.format(0.03f));
        assertEquals("200", ConfigForm.format(200f));
        // What the page shows must parse back to the same value, or closing the page would change it.
        for (float value : new float[] { 1.5f, 0.01f, 0.25f, 30f, 2.5f }) {
            assertEquals(value, ConfigForm.parse(ConfigForm.format(value)));
        }
    }

    @Test
    void anInvalidFieldDoesNotDiscardTheOthers() {
        ConfigForm form = new ConfigForm();
        AtomicReference<Float> first = new AtomicReference<>(1f);
        AtomicReference<Float> second = new AtomicReference<>(2f);

        form.number("First", "oops", first.get(), first::set);
        form.number("Second", "3", second.get(), second::set);

        assertEquals(1f, first.get());
        assertEquals(3f, second.get());
        assertTrue(form.isModified());
    }

    @Test
    void unchangedValuesDoNotMarkTheFormModified() {
        ConfigForm form = new ConfigForm();
        AtomicReference<Float> value = new AtomicReference<>(2f);
        form.number("Value", "2", value.get(), value::set);
        form.bool(true, true, b -> { });
        assertFalse(form.isModified());
    }
}
