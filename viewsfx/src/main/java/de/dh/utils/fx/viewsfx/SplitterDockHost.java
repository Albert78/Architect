package de.dh.utils.fx.viewsfx;

import java.util.Optional;

import de.dh.utils.fx.viewsfx.DockDragOperation.DockDefinition;
import de.dh.utils.fx.viewsfx.TabDockHost.DockableTabControl;
import de.dh.utils.fx.viewsfx.utils.SplitPaneUtils;
import javafx.collections.ObservableList;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

/**
 * Part of a dock host hierarchy. Splitter dock hosts are nested, each level has a different orientation.
 * Each leaf of a dock hierarchy is a {@link TabDockHost} instance, there is at least one tab dock host child.
 *
 * Splitters are inserted into the dock hierarchy as needed, there might be zero or more splitter dock host in the dock hierarchy.
 * A splitter dock host always contains at least 2 children, else it will be removed from the dock hierarchy.
 */
public final class SplitterDockHost extends StackPane implements IDockHostParent, IDockZone {
    protected static class SplitterDockTarget implements IDockTarget {
        protected final SplitterDockHost mParent;
        protected final DockSide mSide;

        public SplitterDockTarget(SplitterDockHost parent, DockSide side) {
            mParent = parent;
            mSide = side;
        }

        public DockSide getDockSide() {
            return mSide;
        }

        @Override
        public String toString() {
            return mSide.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof SplitterDockTarget)) {
                return false;
            }
            SplitterDockTarget other = (SplitterDockTarget) obj;
            return mParent == other.mParent && mSide.equals(other.mSide);
        }
    }

    protected final SplitPane mSplitPane;
    protected final Pane mFeedbackPane;
    protected IDockHostParent mParentDockHost = null;
    protected IDockZone mReplacementZoneOrSelf = this;

    public SplitterDockHost() {
        mSplitPane = new SplitPane();
        mFeedbackPane = new Pane();
        mFeedbackPane.setMouseTransparent(true);
        getChildren().addAll(mSplitPane, mFeedbackPane);
    }

    public static SplitterDockHost create(IDockHostParent parentDockHost) {
        SplitterDockHost result = new SplitterDockHost();
        result.setParentDockHost(parentDockHost);
        result.initialize();
        return result;
    }

    protected void initialize() {
        DockSystem.installDragTargetEventHandlers(this, new IDockDragTargetHandlers() {
            @Override
            public boolean handleDragEntered(DockDragOperation operation, double dragPositionX, double dragPositionY) {
                return SplitterDockHost.this.handleDragEntered(operation, dragPositionX, dragPositionY);
            }

            @Override
            public boolean handleDragOver(DockDragOperation operation, double dragPositionX, double dragPositionY) {
                return SplitterDockHost.this.handleDragOver(operation, dragPositionX, dragPositionY);
            }

            @Override
            public boolean handleDragExited(DockDragOperation operation) {
                return SplitterDockHost.this.handleDragExited(operation);
            }

            @Override
            public boolean handleDragDropped(DockDragOperation operation, double dragPositionX, double dragPositionY) {
                return SplitterDockHost.this.handleDragDropped(operation, dragPositionX, dragPositionY);
            }
        });
    }

    public void dispose() {
        DockSystem.removeDragTargetEventHandlers(this);
    }

    // This is a little hack to prevent overcomplication for the situation this splitter is chosen as
    // docking target but needs to be removed during the rearrangement of the zones. See
    // docs in Dockable#dockAt(IDockZone, DockSide).
    public void disposeAndReplace(IDockZone replacementZone) {
        dispose();
        mReplacementZoneOrSelf = replacementZone;
    }

    public IDockZone getReplacementOrSelf() {
        return mReplacementZoneOrSelf;
    }

    public ObservableList<Node> getItems() {
        return mSplitPane.getItems();
    }

    public Orientation getOrientation() {
        return mSplitPane.getOrientation();
    }

    public void setOrientation(Orientation value) {
        mSplitPane.setOrientation(value);
    }

    public SplitPane getSplitPane() {
        return mSplitPane;
    }

    @Override
    public IDockZone findDockZoneById(String dockZoneId) {
        IDockZone res = getZoneA().findDockZoneById(dockZoneId);
        if (res != null) {
            return res;
        }
        return getZoneB().findDockZoneById(dockZoneId);
    }

    @Override
    public Pane getFeedbackPane() {
        return mFeedbackPane;
    }

    @Override
    public IDockHostParent getParentDockHost() {
        return mParentDockHost;
    }

    @Override
    public void setParentDockHost(IDockHostParent value) {
        mParentDockHost = value;
    }

    public IDockZone getZoneA() {
        return (IDockZone) getItems().get(0);
    }

    public IDockZone getZoneB() {
        return (IDockZone) getItems().get(1);
    }

    public void setZones(IDockZone zoneA, IDockZone zoneB, double dividerPosition) {
        getItems().addAll((Parent) zoneA, (Parent) zoneB);
        setDividerPosition(dividerPosition);
    }

    public double getDividerPosition() {
        return mSplitPane.getDividers().get(0).getPosition();
    }

    public void setDividerPosition(double value) {
        mSplitPane.setDividerPositions(value);
    }

    @Override
    public void invalidateLeaf(TabDockHost child) {
        ObservableList<Node> items = mSplitPane.getItems();
        child.dispose();
        items.remove(child);
        if (items.size() == 1) {
            // Only one child left, need to cleanup dock hierarchy, i.e. parent will remove this instance and
            // put the child in place of it
            mParentDockHost.compressDockHierarchy(this, (IDockZone) items.get(0));
        } else {
            // We still have more than 2 items and thus we'll have at least 2
            // items after the removal -> don't need to cleanup dock hierarchy
            items.remove(child);
        }
    }

    @Override
    public void compressDockHierarchy(SplitterDockHost obsoleteSplitter, IDockZone moveUpChild) {
        ObservableList<Node> items = getItems();
        int index = items.indexOf(obsoleteSplitter);
        obsoleteSplitter.disposeAndReplace(moveUpChild);
        SplitPaneUtils.setItemMaintainDividerPositions(mSplitPane, index, (Parent) moveUpChild);
        moveUpChild.setParentDockHost(this);
    }

    @Override
    public SplitterDockHost replaceWithSplitter(IDockZone replaceChild) {
        ObservableList<Node> items = getItems();
        Parent innerNode = (Parent) replaceChild;
        int index = items.indexOf(innerNode);
        if (index == -1) {
            throw new IllegalStateException("Inner node is no child of this splitter dock host");
        }
        SplitterDockHost result = SplitterDockHost.create(this);
        result.getItems().add((Parent) replaceChild);
        SplitPaneUtils.setItemMaintainDividerPositions(mSplitPane, index, result);
        return result;
    }

    protected IDockFeedback createDockFeedback(SplitterDockTarget feedbackTarget) {
        DockSide side = feedbackTarget.getDockSide();
        double width = mSplitPane.getWidth();
        double height = mSplitPane.getHeight();

        Bounds bounds = switch (side) {
            case East:
                yield new BoundingBox(width / 2, 0, width / 2, height);
            case West:
                yield new BoundingBox(0, 0, width / 2, height);
            case North:
                yield new BoundingBox(0, 0, width, height / 2);
            case South:
                yield new BoundingBox(0, height / 2, width, height / 2);
            default:
                throw new IllegalStateException();
        };
        return new DirectionDockFeedback(this, bounds);
    }

    protected SplitterDockTarget findBestDockTarget(double dragPositionX, double dragPositionY) {
        Bounds bounds = getBoundsInLocal();

        // Find the correct border by checking the current position against the diagonals
        double m = bounds.getHeight() / bounds.getWidth(); // Diagonal: y = m*x
        if (dragPositionX * m > dragPositionY) {
            // North / east
            if ((bounds.getWidth() - dragPositionX) * m < dragPositionY) {
                return new SplitterDockTarget(this, DockSide.East);
            } else {
                return new SplitterDockTarget(this, DockSide.North);
            }
        } else {
            // South / west
            if ((bounds.getWidth() - dragPositionX) * m < dragPositionY) {
                return new SplitterDockTarget(this, DockSide.South);
            } else {
                return new SplitterDockTarget(this, DockSide.West);
            }
        }
    }
    protected void updateDockFeedback(DockDragOperation operation, double dragPositionX, double dragPositionY) {
        SplitterDockTarget target = findBestDockTarget(dragPositionX, dragPositionY);
        IDockFeedback feedback = createDockFeedback(target);
        operation.setCurrentDockTarget(target, Optional.of(feedback));
    }

    /**
     * Installs the drag feedback on this dock host as drag target node.
     */
    protected boolean handleDragEntered(DockDragOperation operation, double dragPositionX, double dragPositionY) {
        updateDockFeedback(operation, dragPositionX, dragPositionY);
        return true;
    }

    /**
     * Updates the drag feedback on this dock host as drag target node.
     */
    protected boolean handleDragOver(DockDragOperation operation, double dragPositionX, double dragPositionY) {
        updateDockFeedback(operation, dragPositionX, dragPositionY);
        return true;
    }

    /**
     * Removes the drag feedback on this dock host as drag target node.
     */
    protected boolean handleDragExited(DockDragOperation operation) {
        operation.removeDockFeedback();
        return true;
    }

    /**
     * Commits a drag & drop operation on this dock host as drag target = drop node.
     */
    protected boolean handleDragDropped(DockDragOperation operation, double dragPositionX, double dragPositionY) {
        if (operation.getCurrentDockDefinition().isEmpty()) {
            return false;
        }
        DockDefinition currentDockDefinition = operation.getCurrentDockDefinition().get();
        IDockTarget dockTarget = currentDockDefinition.getDockTarget();
        if (!(dockTarget instanceof SplitterDockTarget)) {
            return false;
        }
        SplitterDockTarget splitterDockTarget = (SplitterDockTarget) dockTarget;
        DockSide side = splitterDockTarget.getDockSide();
        Dockable<?> dockable = operation.getDockable();
        DockableTabControl targetTab = dockable.dockAt(this, side);
        targetTab.getTabPane().requestFocus();

        // No need to call operation.removeDockFeedback() because we'll get another call to handleDragExited()
        return true;
    }
}
