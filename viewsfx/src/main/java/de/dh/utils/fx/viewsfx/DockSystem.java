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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import de.dh.utils.fx.viewsfx.DockViewLocationDescriptor.DockZoneSplitting;
import de.dh.utils.fx.viewsfx.ViewsRegistry.ViewLifecycleManager;
import de.dh.utils.fx.viewsfx.layout.AbstractDockLayoutDescriptor;
import de.dh.utils.fx.viewsfx.layout.FloatingViewLayoutDescriptor;
import de.dh.utils.fx.viewsfx.layout.PerspectiveDescriptor;
import de.dh.utils.fx.viewsfx.layout.SashLayoutDescriptor;
import de.dh.utils.fx.viewsfx.layout.SashLayoutDescriptor.Orientation;
import de.dh.utils.fx.viewsfx.layout.TabHostLayoutDescriptor;
import de.dh.utils.fx.viewsfx.state.AbstractDockZoneState;
import de.dh.utils.fx.viewsfx.state.FloatingViewState;
import de.dh.utils.fx.viewsfx.state.SashDockZoneState;
import de.dh.utils.fx.viewsfx.state.SashDockZoneState.MaximizedZone;
import de.dh.utils.fx.viewsfx.state.TabHostDockZoneState;
import de.dh.utils.fx.viewsfx.state.ViewDockState;
import de.dh.utils.fx.viewsfx.state.ViewsLayoutState;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.robot.Robot;
import javafx.stage.Stage;

/**
 * Static context for transferring dragged content.
 */
public class DockSystem {
    protected static final DataFormat DND_DATAFORMAT = new DataFormat("DockSystem");
    protected static final String DND_PLACEHOLDER = "DockSystem DND Operation";

    protected static Optional<DockDragOperation> mCurrentDragOperation = Optional.empty();
    protected static Stage mMainWindow = null;
    protected static ViewsRegistry mViewsRegistry = new ViewsRegistry();
    protected static Map<String, DockAreaControl> mDockAreaControls = new HashMap<>();
    protected static PerspectiveDescriptor mPerspectiveLayout = new PerspectiveDescriptor(Collections.emptyMap(), Collections.emptyList(), Collections.emptyList());

    protected static ObservableList<DockableFloatingStage> mFloatingStages = FXCollections.observableArrayList();

    static void notifyNewStage(DockableFloatingStage stage) {
        mFloatingStages.add(stage);
    }

    static void notifyStageClosed(DockableFloatingStage stage) {
        mFloatingStages.remove(stage);
    }

    static Optional<DockDragOperation> getCurrentDragOperation() {
        return mCurrentDragOperation;
    }

    static void installDragOperation(Dockable<?> dockable) {
        if (mCurrentDragOperation.isPresent()) {
            throw new IllegalStateException("A drag operation is already in progress");
        }

        DockDragOperation operation = new DockDragOperation(dockable);
        mCurrentDragOperation = Optional.of(operation);
    }

    public static void finishDragOperation() {
        if (mCurrentDragOperation.isEmpty()) {
            return;
        }
        DockDragOperation operation = mCurrentDragOperation.get();
        operation.dispose();
        mCurrentDragOperation = Optional.empty();
    }

    /**
     * This method needs to be called on each possible object which acts as a drag source for a dock operation,
     * i.e. all tabs of our tab dock hosts.
     * @param dockable The dockable object to be dragged/docked when the drag source is dragged.
     * @param dragSource UI object wich is a possible starting point for a mouse press-drag gesture for a dock operation.
     */
    static void installDragSourceEventHandlers(Dockable<?> dockable, Node dragSource) {
        dragSource.setOnDragDetected(event -> {
            // Neither of the different dragging modes implemented in JavaFX seem to work very well for us.
            // - In simple mode, drag events are only delivered to the source node.
            // - In full mode, drag events are delivered only in the same stage --> floatable stages don't work.
            // - System DND mode works but needs a workaround to store the object to be dragged, since the drag content
            // which is normally transferred via the Dragboard needs to be serializable.
            // It doesn't make sense to serialize our draggable object, so we must workaround it and store a static
            // drag operation object.
            Dragboard db = dragSource.startDragAndDrop(TransferMode.MOVE);

            ClipboardContent content = new ClipboardContent();
            content.put(DND_DATAFORMAT, DND_PLACEHOLDER);

            db.setContent(content);

            // Drag start -> initialize drag operation
            installDragOperation(dockable);

            event.consume();
        });
        dragSource.setOnDragDone(event -> {
            DockSystem.getCurrentDragOperation().ifPresent(operation -> {
                // Drag did not find a drop target nor was cancelled, i.e. the user dropped the object on a different
                // place in screen.
                // In this case, we will create a floating stage as drop target.

                // JavaFX 17:
                // - Bug: We are also routed to this position if the user had pressed the escape key. In that situation,
                //   the drop operation actually should be cancelled. But unfortunately, we cannot distinguish between
                //   "normal" mouse release and a press of the escape key here, can we?
                // - The given DragEvent doesn't contain sensible mouse coordinates, that's why we use Robot.
                //   Is there a better solution?
                Robot r = new Robot();
                DockableFloatingStage dfs = dockable.toFloatingState(mMainWindow, r.getMouseX(), r.getMouseY());
                dfs.requestFocus();
                DockSystem.finishDragOperation();
            });
        });
    }

    /**
     * Cleanup of the drag sources which have been prepared via {@link #installDragSourceEventHandlers(Dockable, Node)}
     */
    static void removeDragSourceEventHandlers(Node dragSource) {
        dragSource.setOnDragDetected(null);
        dragSource.setOnMouseReleased(null);
    }

    /**
     * This method needs to be called on each UI object which can act as drag target object, i.e. our tab dock hosts.
     */
    static void installDragTargetEventHandlers(Node dragTarget, IDockDragTargetHandlers handlers) {
        dragTarget.setOnDragEntered(event -> {
            DockSystem.getCurrentDragOperation().ifPresent(operation -> {
                if (handlers.handleDragEntered(operation, event.getX(), event.getY())) {
                    event.consume();
                }
            });
        });
        dragTarget.setOnDragOver(event -> {
            DockSystem.getCurrentDragOperation().ifPresent(operation -> {
                if (handlers.handleDragOver(operation, event.getX(), event.getY())) {
                    event.acceptTransferModes(TransferMode.ANY);
                    event.consume();
                }
            });
        });
        dragTarget.setOnDragExited(event -> {
            DockSystem.getCurrentDragOperation().ifPresent(operation -> {
                if (handlers.handleDragExited(operation)) {
                    event.consume();
                }
            });
        });
        dragTarget.setOnDragDropped(event -> {
            DockSystem.getCurrentDragOperation().ifPresent(operation -> {
                if (handlers.handleDragDropped(operation, event.getX(), event.getY())) {
                    event.setDropCompleted(true);
                    finishDragOperation();
                    event.consume();
                }
            });
        });
    }

    /**
     * Cleanup of event handlers which had been installed via {@link #installDragTargetEventHandlers(Node, IDockDragTargetHandlers)}.
     */
    static void removeDragTargetEventHandlers(Node dragTarget) {
        dragTarget.setOnDragEntered(null);
        dragTarget.setOnDragOver(null);
        dragTarget.setOnDragExited(null);
        dragTarget.setOnDragDropped(null);
    }

    public static ViewsRegistry getViewsRegistry() {
        return mViewsRegistry;
    }

    public static Map<String, DockAreaControl> getDockAreaControlsRegistry() {
        return mDockAreaControls;
    }

    public static ObservableList<DockableFloatingStage> getFloatingStages() {
        return mFloatingStages;
    }

    public static Optional<TabDockHost> tryGetOrCreateTabDockHost(DockViewLocationDescriptor location) {
        for (DockAreaControl dockAreaControl : mDockAreaControls.values()) {
            Optional<TabDockHost> res = dockAreaControl.getOrTryCreateDockHost(location);
            if (res.isPresent()) {
                return res;
            }
        }
        return Optional.empty();
    }

    protected static Optional<DockViewLocationDescriptor> findViewLocationDescriptorForTabDockHost(String dockAreaId, String tabDockHostId, AbstractDockLayoutDescriptor dockZoneLayout) {
        if (dockZoneLayout instanceof TabHostLayoutDescriptor thld) {
            if (thld.getTabDockHostId().equals(tabDockHostId)) {
                // Found target
                return Optional.of(new DockViewLocationDescriptor(dockAreaId, tabDockHostId, thld.getDockZoneId(), new ArrayList<>()));
            }
        } else if (dockZoneLayout instanceof SashLayoutDescriptor sld) {
            Optional<DockViewLocationDescriptor> oDvld = findViewLocationDescriptorForTabDockHost(dockAreaId, tabDockHostId, sld.getZoneA());
            if (oDvld.isPresent()) {
                DockViewLocationDescriptor dvld = oDvld.get();
                dvld.getDockLocationBuildSteps().add(0, new DockZoneSplitting(sld.getDockZoneId(), sld.getOrientation() == Orientation.Horizontal ? DockSide.West : DockSide.North, sld.getDividerPosition()));
                return Optional.of(dvld);
            }
            oDvld = findViewLocationDescriptorForTabDockHost(dockAreaId,tabDockHostId, sld.getZoneB());
            if (oDvld.isPresent()) {
                DockViewLocationDescriptor dvld = oDvld.get();
                dvld.getDockLocationBuildSteps().add(0, new DockZoneSplitting(sld.getDockZoneId(), sld.getOrientation() == Orientation.Horizontal ? DockSide.East : DockSide.South, sld.getDividerPosition()));
                return Optional.of(dvld);
            }
        } else {
            throw new RuntimeException("Unknown subtype of " + dockZoneLayout.getClass());
        }
        return Optional.empty();
    }

    public static Optional<DockViewLocationDescriptor> getViewLocationDescriptorForTabDockHost(String tabDockHostId) {
        for (Entry<String, AbstractDockLayoutDescriptor> entry : mPerspectiveLayout.getDockAreaLayouts().entrySet()) {
            String dockAreaId = entry.getKey();
            AbstractDockLayoutDescriptor rootLayout = entry.getValue();
            Optional<DockViewLocationDescriptor> res = findViewLocationDescriptorForTabDockHost(dockAreaId, tabDockHostId, rootLayout);
            if (res.isPresent()) {
                return res;
            }
        }
        return Optional.empty();
    }

    public static Stage getMainWindow() {
        return mMainWindow;
    }

    public static void setMainWindow(Stage primaryStage) {
        mMainWindow = primaryStage;
    }

    /**
     * Gets the currently active perspective layout.
     * The layout (in form of the {@link PerspectiveDescriptor}) contains the blueprint for creating dock zones.
     * The currently active state of the UI is saved and restored by methods {@link #saveLayoutState()} and
     * {@link #restoreLayoutState(ViewsLayoutState)}.
     */
    public static PerspectiveDescriptor getPerspectiveLayout() {
        return mPerspectiveLayout;
    }

    /**
     * Sets the given perspective layout for future layout lookups.
     * This method doesn't adjust the current layout state to the new layout, this has to be
     * done manually by calling {@link #resetPerspective()}.
     */
    public static void setPerspectiveLayout(PerspectiveDescriptor value) {
        mPerspectiveLayout = value;
    }

    /**
     * Resets all dock areas and floating views to the default state declared in the current perspective.
     */
    public static void resetPerspective() {
        ViewsLayoutState layoutState = calculateTargetStateForPerspective(mPerspectiveLayout);
        restoreLayoutState(layoutState);
    }

    /**
     * Gets the current views layout state which can be stored to a persistent place and restored by calling
     * {@link #restoreLayoutState(ViewsLayoutState)}.
     */
    public static ViewsLayoutState saveLayoutState() {
        ViewsLayoutState result = new ViewsLayoutState();

        for (Entry<String, DockAreaControl> entry : mDockAreaControls.entrySet()) {
            String dockAreaId = entry.getKey();
            DockAreaControl dockAreaControl = entry.getValue();
            result.getDockLayouts().put(dockAreaId, dockAreaControl.saveLayout());
        }

        List<FloatingViewState> floatingViewStates = DockSystem.getViewsRegistry().storeFloatingViewStates();
        result.getFloatingWindowStates().addAll(floatingViewStates);
        return result;
    }

    /**
     * Step one of materializiation of a perspective's default views to a given layout:
     * Translate the layout to state instances 1:1.
     */
    protected static AbstractDockZoneState translateRecursive(AbstractDockLayoutDescriptor dockZoneLayout) {
        if (dockZoneLayout instanceof SashLayoutDescriptor sld) {
            SashDockZoneState result = new SashDockZoneState();
            result.setDockZoneId(sld.getDockZoneId());
            result.setZoneStateA(translateRecursive(sld.getZoneA()));
            result.setZoneStateB(translateRecursive(sld.getZoneB()));
            result.setOrientation(sld.getOrientation() == Orientation.Horizontal
                    ? de.dh.utils.fx.viewsfx.state.SashDockZoneState.Orientation.Horizontal
                    : de.dh.utils.fx.viewsfx.state.SashDockZoneState.Orientation.Vertical);
            result.setDividerPosition(sld.getDividerPosition());
            result.setMaximizedZone(switch (sld.getMaximizedZone()) {
                case None -> MaximizedZone.None;
                case ZoneA -> MaximizedZone.ZoneA;
                case ZoneB -> MaximizedZone.ZoneB;
                });
            return result;
        } else if (dockZoneLayout instanceof TabHostLayoutDescriptor thld) {
            TabHostDockZoneState result = new TabHostDockZoneState();
            result.setTabDockHostId(thld.getTabDockHostId());
            result.setDockZoneId(thld.getDockZoneId());
            return result;
        } else {
            throw new RuntimeException("Unknown subtype of " + dockZoneLayout.getClass());
        }
    }

    /**
     * Step two of materialization of a perspective's default views to a given layout:
     * Fill the dock zone state instances with the views to be shown and thin out the structure where necessary to avoid
     * obsolete sashs.
     */
    protected static AbstractDockZoneState materializeRecursive(AbstractDockZoneState dockZoneState, Map<String, Collection<String>> tabDockHostsToViewIds) {
        if (dockZoneState instanceof SashDockZoneState sdzs) {
            AbstractDockZoneState materializedZoneA = materializeRecursive(sdzs.getZoneStateA(), tabDockHostsToViewIds);
            AbstractDockZoneState materializedZoneB = materializeRecursive(sdzs.getZoneStateB(), tabDockHostsToViewIds);
            if (materializedZoneA != null && materializedZoneB != null) {
                // Fully occupied sash
                sdzs.setZoneStateA(materializedZoneA);
                sdzs.setZoneStateB(materializedZoneB);
                return sdzs;
            } else if (materializedZoneA != null) {
                // Only Zone A is used, sash of the current level is not needed so compress the hierarchy and skip the sash
                return materializedZoneA;
            } else if (materializedZoneB != null) {
                // Only Zone B is used, sash of the current level is not needed so compress the hierarchy and skip the sash
                return materializedZoneB;
            } else {
                // Completely empty
                return null;
            }
        } else if (dockZoneState instanceof TabHostDockZoneState thdzs) {
            Collection<String> viewIds = tabDockHostsToViewIds.get(thdzs.getTabDockHostId());
            if (viewIds == null) {
                // Empty tab dock host
                return null;
            } else {
                thdzs.getViewDockStates().addAll(viewIds
                    .stream()
                    .map(viewId -> {
                        ViewDockState result = new ViewDockState();
                        result.setViewId(viewId);
                        return result;
                    })
                    .collect(Collectors.toList()));
                return thdzs;
            }
        } else {
            throw new RuntimeException("Unknown subtype of " + dockZoneState.getClass());
        }
    }

    /**
     * Materializes the views layout state according to the default views declared in the perspective and their
     * default layout.
     */
    public static ViewsLayoutState calculateTargetStateForPerspective(PerspectiveDescriptor perspective) {
        Map<String, AbstractDockZoneState> dockLayouts = new HashMap<>();
        Collection<FloatingViewState> floatingViewStates = new ArrayList<>();

        // For the views which are visible by default, prepare map of tab dock hosts to their views
        Map<String, Collection<String>> tabDockHostsToViewIds = new HashMap<>();
        Collection<String> defaultViewIds = perspective.getDefaultViewIds();
        for (String viewId : defaultViewIds) {
            ViewLifecycleManager<?> viewLifecycleManager = mViewsRegistry.getViewLifecycleManager(viewId);
            if (viewLifecycleManager != null) {
                viewLifecycleManager.getPreferredViewLocation().ifPresent(vld -> {
                    if (vld instanceof DockViewLocationDescriptor dvld) {
                        tabDockHostsToViewIds
                            .computeIfAbsent(dvld.getTabDockHostId(), id -> new ArrayList<>())
                            .add(viewId);
                    }
                });
            }
        }

        // Calculate dock layout state for each dock area
        for (Entry<String, AbstractDockLayoutDescriptor> entry : perspective.getDockAreaLayouts().entrySet()) {
            String dockAreaId = entry.getKey();
            AbstractDockLayoutDescriptor dockAreaLayout = entry.getValue();

            AbstractDockZoneState rootDockZoneState = translateRecursive(dockAreaLayout);
            rootDockZoneState = materializeRecursive(rootDockZoneState, tabDockHostsToViewIds);
            dockLayouts.put(dockAreaId, rootDockZoneState);
        }

        // Translates all
        for (FloatingViewLayoutDescriptor layoutDescriptor : perspective.getFloatingViewLayouts()) {
            FloatingViewState state = new FloatingViewState(
                layoutDescriptor.getViewId(),
                layoutDescriptor.getFloatingPositionX(),
                layoutDescriptor.getFloatingPositionY(),
                layoutDescriptor.getFloatingWidth(),
                layoutDescriptor.getFloatingHeight());
            floatingViewStates.add(state);
        }

        return new ViewsLayoutState(dockLayouts, floatingViewStates);
    }

    /**
     * Restores all dock areas and floating views to the given layout state.
     */
    public static void restoreLayoutState(ViewsLayoutState layoutState) {
        Map<String, AbstractDockZoneState> dockZoneStates = layoutState.getDockLayouts();

        for (Entry<String, DockAreaControl> entry : mDockAreaControls.entrySet()) {
            String dockAreaId = entry.getKey();

            AbstractDockZoneState dockZoneState = dockZoneStates.get(dockAreaId);
            if (dockZoneState != null) {
                DockAreaControl dockAreaControl = entry.getValue();
                dockAreaControl.restoreLayout(dockZoneState);
            }
        }

        mViewsRegistry.restoreFloatingViews(layoutState.getFloatingWindowStates(), mMainWindow);
    }
}
