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
package de.dh.cad.architect.ui.view.threed.behaviors;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;

import de.dh.cad.architect.fx.nodes.CombinedTransformGroup;
import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.dialogs.CameraPositionsManagerDialog;
import de.dh.cad.architect.ui.objects.Abstract3DAncillaryObject;
import de.dh.cad.architect.ui.objects.Abstract3DRepresentation;
import de.dh.cad.architect.ui.objects.AbstractSolid3DRepresentation;
import de.dh.cad.architect.ui.persistence.CameraPosition;
import de.dh.cad.architect.ui.view.AbstractUiMode;
import de.dh.cad.architect.ui.view.AbstractViewBehavior;
import de.dh.cad.architect.ui.view.IContextAction;
import de.dh.cad.architect.ui.view.threed.DragController3D;
import de.dh.cad.architect.ui.view.threed.DragController3D.DragMode;
import de.dh.cad.architect.ui.view.threed.ThreeDView;
import de.dh.cad.architect.utils.Namespace;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;
import javafx.scene.robot.Robot;

public abstract class Abstract3DViewBehavior extends AbstractViewBehavior<Abstract3DRepresentation, Abstract3DAncillaryObject> {
    protected EventHandler<ScrollEvent> mZoomEventHandler = null;
    protected int mMouseOverSurfaceBlocked = 0; // 0 means not blocked, bigger than 0 means blocked

    protected Abstract3DViewBehavior(AbstractUiMode<Abstract3DRepresentation, Abstract3DAncillaryObject> parentMode) {
        super(parentMode);
    }

    public void setMouseOverSurfaceBlocked(boolean value) {
        if (value) {
            mMouseOverSurfaceBlocked++;
        } else {
            mMouseOverSurfaceBlocked--;
        }
    }

    public boolean isMouseOverSurfaceBlocked() {
        return mMouseOverSurfaceBlocked > 0;
    }

    protected void enablePlanMouseGestures() {
        var dragController = new DragController3D() {
            boolean mouseOverSurfaceBlockSet = false;

            void blockMouseOverSurface() {
                mouseOverSurfaceBlockSet = true;
                setMouseOverSurfaceBlocked(true);
            }

            void releaseMouseOverSurfaceBlock() {
                if (mouseOverSurfaceBlockSet) {
                    setMouseOverSurfaceBlocked(false);
                    mouseOverSurfaceBlockSet = false;
                }
            }
        };

        ThreeDView view = getView();
        Group rootGroup = view.getRootGroup();
        CombinedTransformGroup transformedRoot = view.getTransformedRoot();

        mView.setOnMousePressed(event -> {
            dragController.mousePressed(event, transformedRoot);
            if (dragController.getCurrentDragMode() == DragMode.None) {
                mView.setCursor(Cursor.DEFAULT);
            } else {
                mView.setCursor(Cursor.MOVE);
                dragController.blockMouseOverSurface();
            }
        });

        mView.setOnMouseReleased(event -> {
            dragController.mouseReleased(event);
            mView.setCursor(Cursor.DEFAULT);
            dragController.releaseMouseOverSurfaceBlock();
        });

        mView.setOnMouseDragged(event -> {
            try {
                dragController.mouseDragged(event, rootGroup, transformedRoot);
                view.setDirty();
            } catch (Exception e) {
                throw new RuntimeException("Unable to translate plan coordinates", e);
            }
        });

        if (mZoomEventHandler == null) {
            mZoomEventHandler = event -> {
                // DeltaX is produced by scrolling Y (scroll wheel) and pressing shift
                view.moveNearClip(-event.getDeltaX());

                // Normal scroll wheel
                view.zoom(event.getDeltaY());
            };
            mView.addEventHandler(ScrollEvent.SCROLL, mZoomEventHandler);
        }
    }

    protected void disablePlanMouseGestures() {
        mView.setOnMousePressed(null);
        mView.setOnMouseReleased(null);
        mView.setOnMouseDragged(null);
        if (mZoomEventHandler != null) {
            mView.removeEventHandler(ScrollEvent.SCROLL, mZoomEventHandler);
            mZoomEventHandler = null;
        }
    }

    @Override
    protected void installDefaultViewHandlers() {
        super.installDefaultViewHandlers();
        // Extracted as extra method to enable sub classes to prevent default handlers
        enablePlanMouseGestures();
        getView().setOnMouseClicked(mouseEvent -> {
            // Check if primary button...
            MouseButton button = mouseEvent.getButton();
            if (button != MouseButton.PRIMARY) {
                return;
            }
            // ... and not moving
            if (!mouseEvent.isStillSincePress()) {
                return;
            }
            mouseEvent.consume();
            getUiController().selectedObjectIds().clear();
        });
        installDefaultSpaceToggleObjectVisibilityKeyHandler();
    }

    @Override
    protected void uninstallDefaultViewHandlers() {
        super.uninstallDefaultViewHandlers();
        uninstallDefaultSpaceToggleObjectVisibilityKeyHandler();
        getView().setOnMouseClicked(null);
        disablePlanMouseGestures();
    }

    @Override
    public ThreeDView getView() {
        return (ThreeDView) mView;
    }

    public void objectsAdded(Collection<Abstract3DRepresentation> reprs) {
        // Override, if needed
    }

    public void objectsRemoved(Collection<Abstract3DRepresentation> reprs) {
        // Override, if needed
    }

    /**
     * Returns the mouse spot mode of this behavior, e.g. whether the surface or the whole object under the
     * mouse is highlighted on mouse over spot.
     */
    protected AbstractSolid3DRepresentation.MouseSpotMode getMouseSpotMode() {
        return AbstractSolid3DRepresentation.MouseSpotMode.Object;
    }

    @Override
    protected void configureDefaultObjectHandlers(Abstract3DRepresentation repr) {
        if (repr instanceof AbstractSolid3DRepresentation sRepr) {
            sRepr.setMouseSpotMode(getMouseSpotMode());
        }
        repr.objectSpottedProperty().removeListener(OBJECT_SPOTTED_LISTENER);
        repr.objectSpottedProperty().addListener(OBJECT_SPOTTED_LISTENER);
        repr.setOnMouseClicked(MOUSE_CLICK_HANDLER_SELECT_OBJECT);
    }

    @Override
    protected void unconfigureDefaultObjectHandlers(Abstract3DRepresentation repr) {
        if (repr instanceof AbstractSolid3DRepresentation sRepr) {
            sRepr.setDefaultMouseSpotMode();
        }
        repr.objectSpottedProperty().removeListener(OBJECT_SPOTTED_LISTENER);
        repr.setOnMouseClicked(null);
    }

    @Override
    public abstract String getTitle();

    protected IContextAction createCameraPositionsMenuAction() {
        return new IContextAction() {
            @Override
            public String getTitle() {
                return Strings.THREE_D_CAMERA_POSITIONS_ACTION_TITLE;
            }

            @Override
            public void execute() {
                ContextMenu contextMenu = createCameraPositionsContextMenu();
                Robot r = new Robot();
                contextMenu.show(getView().getScene().getWindow(), r.getMouseX(), r.getMouseY());
            }
        };
    }

    public ContextMenu createCameraPositionsContextMenu() {
        ContextMenu result = new ContextMenu();
        result.setOnShowing(showingEvent -> {
            ObservableList<MenuItem> items = result.getItems();
            items.clear();
            MenuItem saveItem = new MenuItem(Strings.THREE_D_MENU_CAMERA_POSITIONS_SAVE_CURRENT);
            saveItem.setOnAction(event -> {
                querySaveCurrentCameraPosition();
            });
            items.add(saveItem);
            MenuItem resetCameraPositionItem = new MenuItem(Strings.THREE_D_MENU_CAMERA_POSITIONS_RESET);
            resetCameraPositionItem.setOnAction(event -> {
                getView().resetViewState();
            });
            items.add(resetCameraPositionItem);
            MenuItem manageItem = new MenuItem(Strings.THREE_D_MENU_CAMERA_POSITIONS_MANAGE_POSITIONS);
            manageItem.setOnAction(event -> {
                manageCameraPositions();
            });
            items.add(manageItem);
            TreeSet<String> sortedNames = new TreeSet<>(getView().getNamedCameraPositions().keySet());
            if (!sortedNames.isEmpty()) {
                items.add(new SeparatorMenuItem());
                for (String name : sortedNames) {
                    MenuItem item = new MenuItem(name);
                    item.setOnAction(event -> {
                        getView().loadCameraPosition(name);
                    });
                    items.add(item);
                }
            }
        });
        return result;
    }

    protected void querySaveCurrentCameraPosition() {
        Namespace<Object> ns = new Namespace<>();
        Map<String, Object> nsMappings = ns.getMappings();
        for (String name : getView().getNamedCameraPositions().keySet()) {
            nsMappings.put(name, null);
        }
        String name = Strings.THREE_D_SAVE_CAMERA_NAME_PATTERN;
        while (true) {
            TextInputDialog dialog = new TextInputDialog(ns.generateName(name, 1));
            dialog.setTitle(Strings.THREE_D_SAVE_CAMERA_DIALOG_TITLE);
            dialog.setHeaderText(Strings.THREE_D_SAVE_CAMERA_DIALOG_HEADER);
            dialog.setContentText(Strings.THREE_D_SAVE_CAMERA_DIALOG_CAMERA_NAME_LABEL);

            // Traditional way to get the response value.
            Optional<String> result = dialog.showAndWait();
            if (!result.isPresent()) {
                return;
            }
            name = result.get();
            if (ns.contains(name)) {
                CameraPositionsManagerDialog.showCameraNameAlreadyExistsDialog(name);
            } else {
                getView().saveCameraPosition(name);
                return;
            }
        }
    }

    protected void manageCameraPositions() {
        CameraPositionsManagerDialog dialog = CameraPositionsManagerDialog.create(getView().getNamedCameraPositions());
        Optional<Map<String, CameraPosition>> oModifiedPositions = dialog.showAndWait();
        oModifiedPositions.ifPresent(modifiedPositions -> {
            getView().setNamedCameraPositions(modifiedPositions);
        });
    }
}
