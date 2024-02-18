/*******************************************************************************
 *     Architect - A free 2D/3D home and interior designer
 *     Copyright (C) 2021 - 2023  Daniel Höh
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>
 *******************************************************************************/
package de.dh.utils.fx.viewsfx;

import java.util.Optional;

import de.dh.utils.fx.sash.SashEx;
import de.dh.utils.fx.viewsfx.DockDragOperation.DockDefinition;
import de.dh.utils.fx.viewsfx.TabDockHost.DockableTabControl;
import de.dh.utils.fx.viewsfx.utils.SashUtils;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

/**
 * Part of a dock zones hierarchy. Sash dock hosts are nested, each level contains exactly two child zones.
 * Each leaf of a dock hierarchy is a {@link TabDockHost} instance, there is at least one tab dock host child.
 *
 * Sashs are inserted into the dock hierarchy as needed, there might be zero or more sash dock host in the dock hierarchy.
 * A sash dock host always contains exactly 2 children. If more than two children are docked side-by-side (horizontaly or
 * vertically), multiple sash dock hosts are nested. If a child is removed, the sash is removed from the hierarchy.
 */
public final class SashDockHost extends StackPane implements IDockZoneParent, IDockZone {
    protected static class SashDockTarget implements IDockTarget {
        protected final SashDockHost mParent;
        protected final DockSide mSide;

        public SashDockTarget(SashDockHost parent, DockSide side) {
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
            if (!(obj instanceof SashDockTarget)) {
                return false;
            }
            SashDockTarget other = (SashDockTarget) obj;
            return mParent == other.mParent && mSide.equals(other.mSide);
        }
    }

    protected final SashEx mSash;
    protected final Pane mFeedbackPane;
    protected String mDockZoneId;
    protected IDockZoneParent mDockZoneParent = null;
    protected IDockZone mReplacementZoneOrSelf = this;

    public SashDockHost(String dockZoneId, IDockZoneParent dockZoneParent) {
        mSash = new SashEx();
        mFeedbackPane = new Pane();
        mFeedbackPane.setMouseTransparent(true);
        getChildren().addAll(mSash, mFeedbackPane);
        occupyDockZone(dockZoneId, dockZoneParent);
    }

    public static SashDockHost create(String dockZoneId, IDockZoneParent dockZoneParent) {
        SashDockHost result = new SashDockHost(dockZoneId, dockZoneParent);
        result.initialize();
        return result;
    }

    protected void initialize() {
        DockSystem.installDragTargetEventHandlers(this, new IDockDragTargetHandlers() {
            @Override
            public boolean handleDragEntered(DockDragOperation operation, double dragPositionX, double dragPositionY) {
                return SashDockHost.this.handleDragEntered(operation, dragPositionX, dragPositionY);
            }

            @Override
            public boolean handleDragOver(DockDragOperation operation, double dragPositionX, double dragPositionY) {
                return SashDockHost.this.handleDragOver(operation, dragPositionX, dragPositionY);
            }

            @Override
            public boolean handleDragExited(DockDragOperation operation) {
                return SashDockHost.this.handleDragExited(operation);
            }

            @Override
            public boolean handleDragDropped(DockDragOperation operation, double dragPositionX, double dragPositionY) {
                return SashDockHost.this.handleDragDropped(operation, dragPositionX, dragPositionY);
            }
        });
    }

    public void dispose() {
        DockSystem.removeDragTargetEventHandlers(this);
        mDockZoneParent = null;
    }

    // This is a little hack to prevent overcomplicating the situation when this sash is chosen as
    // docking target but needs to be removed during the rearrangement of the zones. See
    // docs in Dockable#dockAt(IDockZone, DockSide).
    public void disposeAndReplace(IDockZone replacementZone) {
        dispose();
        mReplacementZoneOrSelf = replacementZone;
    }

    public IDockZone getReplacementOrSelf() {
        return mReplacementZoneOrSelf;
    }

    public Orientation getOrientation() {
        return mSash.getOrientation();
    }

    public void setOrientation(Orientation value) {
        mSash.setOrientation(value);
    }

    public SashEx getSash() {
        return mSash;
    }

    @Override
    public void clearViews() {
        IDockZone zoneA = getZoneA();
        IDockZone zoneB = getZoneB();
        zoneA.clearViews();
        zoneB.clearViews();
    }

    @Override
    public Optional<IDockZone> findDockZoneById(String dockZoneId) {
        if (mDockZoneId.equals(dockZoneId)) {
            return Optional.of(this);
        }
        Optional<IDockZone> res = getZoneA().findDockZoneById(dockZoneId);
        if (res.isPresent()) {
            return res;
        }
        return getZoneB().findDockZoneById(dockZoneId);
    }

    @Override
    public Optional<TabDockHost> findTabDockHostById(String tabDockHostId) {
        Optional<TabDockHost> res = getZoneA().findTabDockHostById(tabDockHostId);
        if (res.isPresent()) {
            return res;
        }
        return getZoneB().findTabDockHostById(tabDockHostId);
    }

    @Override
    public Pane getFeedbackPane() {
        return mFeedbackPane;
    }

    @Override
    public boolean isAlive() {
        return mDockZoneParent != null;
    }

    @Override
    public String getDockZoneId() {
        return mDockZoneId;
    }

    @Override
    public void occupyDockZone(String dockZoneId, IDockZoneParent dockZoneParent) {
        mDockZoneId = dockZoneId;
        mDockZoneParent = dockZoneParent;
    }

    @Override
    public String getDockAreaId() {
        return getDockZoneParent().getDockAreaId();
    }

    @Override
    public IDockZoneParent getDockZoneParent() {
        return mDockZoneParent;
    }

    public IDockZone getZoneA() {
        return (IDockZone) mSash.getFirstChild();
    }

    public IDockZone getZoneB() {
        return (IDockZone) mSash.getLastChild();
    }

    public void setZones(IDockZone zoneA, IDockZone zoneB, double dividerPosition) {
        mSash.setFirstChild((Node) zoneA);
        mSash.setLastChild((Node) zoneB);
        setDividerPosition(dividerPosition);
    }

    public double getDividerPosition() {
        return mSash.getDividerPosition();
    }

    public void setDividerPosition(double value) {
        mSash.setDividerPosition(value);
    }

    @Override
    public void invalidateLeaf(TabDockHost child) {
        child.dispose();
        DockSide invalidatedSide = SashUtils.getDockSideOfItem(mSash, child)
                .orElseThrow(() -> new IllegalStateException("Leaf item is no child of this dock host"));
        DockSide remainingSide = invalidatedSide.opposite();
        Node remainingItem = SashUtils.getSashItem(mSash, remainingSide);

        SashUtils.setSashItem(mSash, invalidatedSide, null);
        // Only one child left, need to clean up dock hierarchy, i.e. parent will remove this instance and
        // put the child in place of it
        mDockZoneParent.compressDockHierarchy(this, (IDockZone) remainingItem);
    }

    @Override
    public void compressDockHierarchy(SashDockHost obsoleteSash, IDockZone moveUpChild) {
        DockSide dockedSide = SashUtils.getDockSideOfItem(mSash, obsoleteSash)
                .orElseThrow(() -> new IllegalStateException("Disposed dock zone is no child of this dock host"));

        obsoleteSash.disposeAndReplace(moveUpChild);
        SashUtils.setSashItem(mSash, dockedSide, (Node) moveUpChild);
        moveUpChild.occupyDockZone(obsoleteSash.getDockZoneId(), this);
    }

    @Override
    public SashDockHost replaceWithSash(IDockZone replaceChild, String newChildDockZoneId, DockSide emptySide, IDockHostCreator dockHostCreator) {
        Node innerNode = (Node) replaceChild;
        DockSide dockedSide = SashUtils.getDockSideOfItem(mSash, (Node) replaceChild)
                .orElseThrow(() -> new IllegalStateException("Zone to be replaced is no child of this dock host"));
        SashDockHost result = dockHostCreator.createSashDockHost(replaceChild.getDockZoneId(), this);
        SashUtils.setSashItem(result.getSash(), emptySide.opposite(), innerNode);
        replaceChild.occupyDockZone(newChildDockZoneId, result);
        SashUtils.setSashItem(mSash, dockedSide, result);
        return result;
    }

    protected IDockFeedback createDockFeedback(SashDockTarget feedbackTarget) {
        DockSide side = feedbackTarget.getDockSide();
        double width = mSash.getWidth();
        double height = mSash.getHeight();

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

    protected SashDockTarget findBestDockTarget(double dragPositionX, double dragPositionY) {
        Bounds bounds = getBoundsInLocal();

        // Find the correct border by checking the current position against the diagonals
        double m = bounds.getHeight() / bounds.getWidth(); // Diagonal: y = m*x
        if (dragPositionX * m > dragPositionY) {
            // North / east
            if ((bounds.getWidth() - dragPositionX) * m < dragPositionY) {
                return new SashDockTarget(this, DockSide.East);
            } else {
                return new SashDockTarget(this, DockSide.North);
            }
        } else {
            // South / west
            if ((bounds.getWidth() - dragPositionX) * m < dragPositionY) {
                return new SashDockTarget(this, DockSide.South);
            } else {
                return new SashDockTarget(this, DockSide.West);
            }
        }
    }
    protected void updateDockFeedback(DockDragOperation operation, double dragPositionX, double dragPositionY) {
        SashDockTarget target = findBestDockTarget(dragPositionX, dragPositionY);
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
        if (!(dockTarget instanceof SashDockTarget)) {
            return false;
        }
        SashDockTarget sashDockTarget = (SashDockTarget) dockTarget;
        DockSide side = sashDockTarget.getDockSide();
        Dockable<?> dockable = operation.getDockable();
        DockableTabControl targetTab = dockable.dockAt(this, side);
        targetTab.getTabPane().requestFocus();

        // No need to call operation.removeDockFeedback() because we'll get another call to handleDragExited()
        return true;
    }

    @Override
    public String toString() {
        return "SashDockHost, DockZoneId='" + mDockZoneId + "'";
    }
}
