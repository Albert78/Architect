package de.dh.utils.fx.viewsfx;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import de.dh.utils.fx.viewsfx.DockDragOperation.DockDefinition;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

/**
 * Leaf of a dock host hierarchy, containing the actual dockable's UI control(s).
 * A tab dock host is either child of a splitter dock host or a direct child of the root dock host control.
 *
 * There will always be at least one tab dock host per {@link DockHostControl}. If no dockables are
 * currently docked, that tab dock host is the only child of the dock host control and it is empty.
 */
public final class TabDockHost extends StackPane implements IDockZone {
    protected enum TabHostDockPosition {
        North, South, East, West, Center
    }

    protected static class TabDockTarget implements IDockTarget {
        protected final TabDockHost mParent;
        protected final TabHostDockPosition mPosition;
        protected final Optional<Integer> mOBeforeTabIndex;

        // Dock to the border
        public TabDockTarget(TabDockHost parent, TabHostDockPosition position) {
            mParent = parent;
            mPosition = position;
            mOBeforeTabIndex = Optional.empty();
        }

        // Dock to the center at a tab position
        public TabDockTarget(TabDockHost parent, int beforeTabIndex) {
            mParent = parent;
            mPosition = TabHostDockPosition.Center;
            mOBeforeTabIndex = Optional.of(beforeTabIndex);
        }

        public TabHostDockPosition getPosition() {
            return mPosition;
        }

        /**
         * Gets the index of the tab, which be moved up when docking at {@link TabHostDockPosition#Center}, i.e. as a tab in the tab host.
         * If this value is {@code null}, it will be interpreted as the last possible tab position, i.e. {@code number of tabs - 1}.
         * A {@code null} value is used to distinguish between tab feedback for the last tab position and a dock feedback covering
         * the whole area, which is in fact the same but looks better for the user for different mouse drag positions. See calculation
         * of the dock feedbacks.
         */
        public Optional<Integer> getBeforeTabIndex() {
            return mOBeforeTabIndex;
        }

        @Override
        public String toString() {
            switch (mPosition) {
            case Center:
                return "Center before tab " + mOBeforeTabIndex;
            default:
                return mPosition.toString();
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof TabDockTarget)) {
                return false;
            }
            TabDockTarget other = (TabDockTarget) obj;
            return mParent == other.mParent && mPosition.equals(other.mPosition) && mOBeforeTabIndex.equals(other.mOBeforeTabIndex);
        }
    }

    public static class DockableTabControl extends Tab implements IDockableUIRepresentation {
        protected final ChangeListener<? super String> mTitleListener = (observable, oldValue, newValue) -> {
            setText(newValue);
        };

        protected final Dockable<?> mDockable;
        protected final TabDockHost mDockHost;
        protected final Label mTitleLabel;

        public DockableTabControl(Dockable<?> dockable, TabDockHost dockHost) {
            mDockable = dockable;
            mTitleLabel = new Label(dockable.getDockableTitle());
            setGraphic(mTitleLabel); // We use our own Label to enable adding our drag & drop event handlers
            mDockable.titleProperty().addListener(mTitleListener);
            // TODO: Graphic, ToolTip
            setClosable(dockable.isShowCloseButton());
            setOnCloseRequest(event -> {
                dockable.close();
                event.consume();
            });
            setContent(mDockable.getView());
            mDockHost = dockHost;
        }

        public Dockable<?> getDockable() {
            return mDockable;
        }

        public Label getTitleLabel() {
            return mTitleLabel;
        }

        public TabDockHost getDockHost() {
            return mDockHost;
        }

        @Override
        public Node dispose() {
            mDockable.titleProperty().removeListener(mTitleListener);
            setContent(null);
            mDockHost.removeDockableTab(this);
            return mDockable.getView();
        }

        public int getDockPosition() {
            return mDockHost.getTabs().indexOf(this);
        }
    }

    protected final String mDockZoneId;
    protected final TabPane mTabPane;
    protected final Pane mFeedbackPane;
    protected IDockHostParent mParentDockHost = null;

    public TabDockHost(String dockZoneId) {
        mDockZoneId = dockZoneId;
        mTabPane = new TabPane() {
            // Hack to let our dockable get the focus at once instead of focusing the tab pane
            @Override
            public void requestFocus() {
                Optional<Node> on = getSelectedDockable()
                        .map(d -> d.getFocusControl());
                if (on.isPresent()) {
                    Node n = on.get();
                    n.requestFocus();
                } else {
                    super.requestFocus();
                }
            }
        };
        mFeedbackPane = new Pane();
        mFeedbackPane.setMouseTransparent(true);
        getChildren().addAll(mTabPane, mFeedbackPane);
    }

    public static TabDockHost create(IDockHostParent parentDockHost) {
        return create(parentDockHost, UUID.randomUUID().toString());
    }

    public static TabDockHost create(IDockHostParent parentDockHost, String dockZoneId) {
        TabDockHost result = new TabDockHost(dockZoneId);
        result.setParentDockHost(parentDockHost);
        result.initialize();
        return result;
    }

    protected void initialize() {
        DockSystem.installDragTargetEventHandlers(mTabPane, new IDockDragTargetHandlers() {
            @Override
            public boolean handleDragEntered(DockDragOperation operation, double dragPositionX, double dragPositionY) {
                return TabDockHost.this.handleDragEntered(operation, dragPositionX, dragPositionY);
            }

            @Override
            public boolean handleDragOver(DockDragOperation operation, double dragPositionX, double dragPositionY) {
                return TabDockHost.this.handleDragOver(operation, dragPositionX, dragPositionY);
            }

            @Override
            public boolean handleDragExited(DockDragOperation operation) {
                return TabDockHost.this.handleDragExited(operation);
            }

            @Override
            public boolean handleDragDropped(DockDragOperation operation, double dragPositionX, double dragPositionY) {
                return TabDockHost.this.handleDragDropped(operation, dragPositionX, dragPositionY);
            }
        });
    }

    protected void dispose() {
        DockSystem.removeDragTargetEventHandlers(this);
        mParentDockHost = null;
    }

    public boolean isAlive() {
        return mParentDockHost != null;
    }

    public TabPane getTabPane() {
        return mTabPane;
    }

    public ObservableList<Tab> getTabs() {
        return mTabPane.getTabs();
    }

    public Optional<Dockable<?>> getSelectedDockable() {
        DockableTabControl tab = (DockableTabControl) mTabPane.getSelectionModel().getSelectedItem();
        if (tab != null) {
            return Optional.of(tab.getDockable());
        }
        return Optional.empty();
    }

    @Override
    public IDockZone findDockZoneById(String dockZoneId) {
        return dockZoneId.equals(mDockZoneId) ? this : null;
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

    public String getDockZoneId() {
        return mDockZoneId;
    }

    public boolean isSingleChild(IDockableUIRepresentation representation) {
        if (!(representation instanceof DockableTabControl)) {
            return false;
        }
        DockableTabControl dtc = (DockableTabControl) representation;
        ObservableList<Tab> tabs = getTabs();
        return tabs.size() == 1 && tabs.contains(dtc);
    }

    public int getDockPosition(IDockableUIRepresentation representation) {
        if (!(representation instanceof DockableTabControl)) {
            return -1;
        }
        DockableTabControl dtc = (DockableTabControl) representation;
        return getTabs().indexOf(dtc);
    }

    protected IDockFeedback createDockFeedback(TabDockTarget feedbackTarget) {
        TabHostDockPosition position = feedbackTarget.getPosition();
        Optional<Integer> oBeforeTabIndex = feedbackTarget.getBeforeTabIndex();
        if (position == TabHostDockPosition.Center && oBeforeTabIndex.isPresent()) {
            int beforeTabIndex = oBeforeTabIndex.get();
            // Find position of visual tab divider line
            List<Bounds> tabBoundsInLocal = getTabBoundsInLocal();
            double positionX;
            if (beforeTabIndex >= tabBoundsInLocal.size()) {
                if (tabBoundsInLocal.isEmpty()) {
                    positionX = 0;
                } else {
                    positionX = tabBoundsInLocal.get(tabBoundsInLocal.size() - 1).getMaxX() + 1;
                }
            } else {
                positionX = tabBoundsInLocal.get(beforeTabIndex).getMinX();
            }

            double height = getTabPaneHeaderBoundsInLocal().getHeight();
            return new TabDockFeedback(this, new BoundingBox(positionX, 0, 0, height));
        }
        double width = mFeedbackPane.getWidth();
        double height = mFeedbackPane.getHeight();

        Bounds bounds = switch (position) {
            case Center:
                yield new BoundingBox(0, 0, width, height);
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

    protected Bounds getTabPaneHeaderBoundsInLocal() {
        Pane headerArea = (Pane) lookup(".tab-header-area");
        return headerArea.getBoundsInLocal();
    }

    protected List<Bounds> getTabBoundsInLocal() {
        return lookupAll(".tab").stream()
                .map(tab -> sceneToLocal(tab.localToScene(tab.getBoundsInLocal())))
                .toList();
    }

    protected TabDockTarget findBestDockTarget(double dragPositionX, double dragPositionY) {
        Bounds bounds = mTabPane.getBoundsInLocal();

        // Check if we're in the tab pane header
        Bounds tabPaneHeaderArea = getTabPaneHeaderBoundsInLocal();
        if (tabPaneHeaderArea.contains(dragPositionX, dragPositionY)) {
            int index = 0;
            List<Bounds> tabBoundsInLocal = getTabBoundsInLocal();
            for (Bounds tabBounds : tabBoundsInLocal) {
                if (dragPositionX > tabBounds.getMaxX()) {
                    index++;
                } else {
                    break;
                }
            }
            return new TabDockTarget(this, index);
        }

        // Check if we're in the center area
        // Center area is here hard coded to a quarter of the size of this control
        double part = 1/4d;
        if (Math.abs(bounds.getWidth() / 2 - dragPositionX) < bounds.getWidth() * part / 2 &&
                Math.abs(bounds.getHeight() / 2 - dragPositionY) < bounds.getHeight() * part / 2) {
            return new TabDockTarget(this, TabHostDockPosition.Center);
        }
        // Find the correct border by checking the current position against the diagonals
        double m = bounds.getHeight() / bounds.getWidth(); // Diagonal: y = m*x
        if (dragPositionX * m > dragPositionY) {
            // North / east
            if ((bounds.getWidth() - dragPositionX) * m < dragPositionY) {
                return new TabDockTarget(this, TabHostDockPosition.East);
            } else {
                return new TabDockTarget(this, TabHostDockPosition.North);
            }
        } else {
            // South / west
            if ((bounds.getWidth() - dragPositionX) * m < dragPositionY) {
                return new TabDockTarget(this, TabHostDockPosition.South);
            } else {
                return new TabDockTarget(this, TabHostDockPosition.West);
            }
        }
    }
    protected void updateDockFeedback(DockDragOperation operation, double dragPositionX, double dragPositionY) {
        TabDockTarget target = findBestDockTarget(dragPositionX, dragPositionY);
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
        if (!(dockTarget instanceof TabDockTarget)) {
            return false;
        }
        TabDockTarget tabDockTarget = (TabDockTarget) dockTarget;
        TabHostDockPosition dockPosition = tabDockTarget.getPosition();
        Dockable<?> dockable = operation.getDockable();
        DockableTabControl targetTab;
        if (dockPosition == TabHostDockPosition.Center) {
            int beforePosition = tabDockTarget.getBeforeTabIndex().orElse(mTabPane.getTabs().size());
            targetTab = dockable.dockAt(this, beforePosition);
        } else {
            DockSide side = switch (dockPosition) {
            case North:
                yield DockSide.North;
            case South:
                yield DockSide.South;
            case East:
                yield DockSide.East;
            case West:
                yield DockSide.West;
            default:
                throw new RuntimeException("This will not happen as case Center is handled above");
            };
            targetTab = dockable.dockAt(this, side);
        }
        targetTab.getTabPane().requestFocus();

        // No need to call operation.removeDockFeedback() because we'll get another call to handleDragExited()
        return true;
    }

    protected DockableTabControl addDockable(Dockable<?> dockable, int beforePosition) {
        ObservableList<Tab> tabs = mTabPane.getTabs();
        DockableTabControl result = new DockableTabControl(dockable, this);
        Label titleLabel = result.getTitleLabel();
        DockSystem.installDragSourceEventHandlers(dockable, titleLabel);
        tabs.add(beforePosition, result);
        mTabPane.getSelectionModel().select(beforePosition);
        return result;
    }

    protected void removeDockableTab(DockableTabControl dockableTab) {
        ObservableList<Tab> tabs = mTabPane.getTabs();
        tabs.remove(dockableTab);

        // Not necessary to remove the event handlers but we installed it so we will also clean up...
        DockSystem.removeDragSourceEventHandlers(dockableTab.getTitleLabel());

        if (tabs.isEmpty()) {
            getParentDockHost().invalidateLeaf(this);
        }
    }
}
