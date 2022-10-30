package de.dh.utils.fx.viewsfx.utils;

import javafx.scene.Node;
import javafx.scene.control.SplitPane;

public class SplitPaneUtils {
    public static void setItemMaintainDividerPositions(SplitPane splitPane, int index, Node newNode) {
        // It's necessary to restore the divider positions after changing the items because the implementation
        // of the list change event in SplitPane corrupts them.
        double[] dividerPositions = splitPane.getDividerPositions();
        splitPane.getItems().set(index, newNode);
        splitPane.setDividerPositions(dividerPositions);
    }
}
