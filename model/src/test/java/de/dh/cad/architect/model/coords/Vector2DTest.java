package de.dh.cad.architect.model.coords;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.dh.cad.architect.model.changes.MacroChange;

/**
 * Test class for {@link MacroChange}.
 */
public class Vector2DTest {
    protected static final Length EPSILON = Length.ofMM(10);

    @Test
    @DisplayName("Rotation direction test")
    public void testVector2DRotation() {
        // Angel bisector
        Vector2D v = new Vector2D(Length.ofM(1), Length.ofM(1));

        Vector2D rotated = v.rotate(-45).toUnitVector(LengthUnit.M);

        Vector2D Y1 = Vector2D.X1M;

        assertTrue(rotated.distance(Y1).lt(EPSILON), "Rotation direction of vector is incorrect");
    }
}
