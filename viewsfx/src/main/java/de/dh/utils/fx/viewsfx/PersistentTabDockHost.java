package de.dh.utils.fx.viewsfx;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;

/**
 * Non-closable subclass of {@link TabDockHost} which displays a placeholder node when no tabs are present.
 */
public abstract class PersistentTabDockHost extends TabDockHost {
    protected final Node mPlaceholder;

    public PersistentTabDockHost(String tabDockHostId, String dockZoneId, IDockZoneParent dockZoneParent) {
        super(tabDockHostId, dockZoneId, dockZoneParent);

        mPlaceholder = createPlaceholder();
        BooleanBinding emptyBinding = Bindings.isEmpty(mTabPane.getTabs());
        mPlaceholder.visibleProperty().bind(emptyBinding);
        mPlaceholder.managedProperty().bind(emptyBinding);
        ObservableList<Node> children = getChildren();
        int tabPaneIndex = children.indexOf(mTabPane);
        children.add(tabPaneIndex + 1, mPlaceholder);

        alignmentProperty().bind(Bindings.when(emptyBinding).then(Pos.CENTER).otherwise(Pos.TOP_LEFT));
    }

    /**
     * Subclasses should return a node containing a placeholder to be shown when no tabs are present.
     * The placeholder might any JavaFX node which can be used as a child for a {@link Parent}.
     * The placeholder will be switched visible and invisible depending on the presence of tabs.
     */
    protected abstract Node createPlaceholder();

    @Override
    public boolean isClosable() {
        return false;
    }
}
