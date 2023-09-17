package de.dh.cad.architect.model;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.dh.cad.architect.model.changes.IModelChange;
import de.dh.cad.architect.model.changes.MacroChange;
import de.dh.cad.architect.model.changes.ObjectRemovalChange;
import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.model.objects.ObjectsGroup;
import de.dh.cad.architect.model.objects.Wall;

/**
 * Test class for {@link MacroChange}.
 */
public class MarcoChangeTest {
    @Test
    @DisplayName("Macro Change Test")
    public void testCase1() {
        List<IModelChange> changeTrace = new ArrayList<>();

        Wall wall1 = new Wall("1", null, Length.ofMM(1), Length.ofM(2.5), Length.ofM(2.5));
        Wall wall2 = new Wall("2", null, Length.ofMM(1), Length.ofM(2.5), Length.ofM(2.5));
        ObjectsGroup group1 = new ObjectsGroup("Group", "Group");

        group1.addAllObjects(Arrays.asList(wall1, wall2), changeTrace);

        MacroChange macroChange = MacroChange.create(changeTrace, false);

        assertTrue(macroChange.getAdditions().size() == 0, "Number of additions incorrect");
        assertTrue(macroChange.getRemovals().size() == 0, "Number of removals incorrect");
        assertTrue(macroChange.getModifications().size() == 3, "Number of modifications incorrect");

        changeTrace.clear();

        group1.delete(changeTrace); // Can't call the container's remove here, so we'll fake the removal change:
        changeTrace.add(new ObjectRemovalChange(group1) {
            @Override
            public void undo(List<IModelChange> undoChangeTrace) {
                // Ignore
            }
        });

        macroChange = MacroChange.create(changeTrace, false);

        assertTrue(macroChange.getAdditions().size() == 0, "Number of additions incorrect");
        assertTrue(macroChange.getRemovals().size() == 1, "Number of removals incorrect");
        assertTrue(macroChange.getModifications().size() == 2, "Number of modifications incorrect");
    }
}
