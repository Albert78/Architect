/*******************************************************************************
 *     Architect - A free 2D/3D home and interior designer
 *     Copyright (C) 2021, 2022  Daniel Höh
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

import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.dialogs.CameraPositionsManagerDialog;
import de.dh.cad.architect.ui.objects.Abstract3DAncillaryObject;
import de.dh.cad.architect.ui.objects.Abstract3DRepresentation;
import de.dh.cad.architect.ui.persistence.CameraPosition;
import de.dh.cad.architect.ui.view.AbstractUiMode;
import de.dh.cad.architect.ui.view.AbstractViewBehavior;
import de.dh.cad.architect.ui.view.threed.DragController3D;
import de.dh.cad.architect.ui.view.threed.DragController3D.DragMode;
import de.dh.cad.architect.ui.view.threed.ThreeDView;
import de.dh.cad.architect.utils.Namespace;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;

public abstract class Abstract3DViewBehavior extends AbstractViewBehavior<Abstract3DRepresentation, Abstract3DAncillaryObject> {
    protected DragMode mCurrentDragMode = DragMode.None;
    protected EventHandler<ScrollEvent> mZoomEventHandler = null;

    protected Abstract3DViewBehavior(AbstractUiMode<Abstract3DRepresentation, Abstract3DAncillaryObject> parentMode) {
        super(parentMode);
    }

    protected void enablePlanMouseGestures() {
        DragController3D dragController = new DragController3D();
        Scene scene = mView.getScene();

        mView.setOnMousePressed(event -> {
            dragController.mousePressed(event, getView().getTransformedRoot());
            if (dragController.getCurrentDragMode() == DragMode.None) {
                scene.setCursor(Cursor.DEFAULT);
            } else {
                scene.setCursor(Cursor.MOVE);
            }
        });

        mView.setOnMouseReleased(event -> {
            dragController.mouseReleased(event);
            scene.setCursor(Cursor.DEFAULT);
        });

        mView.setOnMouseDragged(event -> {
            try {
                ThreeDView view = getView();
                dragController.mouseDragged(event, view.getRootGroup(), view.getTransformedRoot());
            } catch (Exception e) {
                throw new RuntimeException("Unable to translate plan coordinates", e);
            }
        });

        if (mZoomEventHandler == null) {
            mZoomEventHandler = new EventHandler<>() {
                @Override
                public void handle(ScrollEvent event) {
                    // DeltaX is produced by scrolling Y and pressing shift
                    getView().moveNearClip(-event.getDeltaX());

                    // Normal scroll wheel
                    getView().zoom(event.getDeltaY());
                }
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

    @Override
    protected void configureDefaultObjectHandlers(Abstract3DRepresentation repr) {
        repr.enableMouseOverSpot();
        repr.objectSpottedProperty().removeListener(OBJECT_SPOTTED_LISTENER_UPDATE_USER_HINT);
        repr.objectSpottedProperty().addListener(OBJECT_SPOTTED_LISTENER_UPDATE_USER_HINT);
        repr.setOnMouseClicked(MOUSE_CLICK_HANDLER_SELECT_OBJECT);
    }

    @Override
    protected void unconfigureDefaultObjectHandlers(Abstract3DRepresentation repr) {
        repr.disableMouseOverSpot();
        repr.objectSpottedProperty().removeListener(OBJECT_SPOTTED_LISTENER_UPDATE_USER_HINT);
        repr.setOnMouseClicked(null);
    }

    @Override
    public abstract String getTitle();

    @Override
    public Menu getBehaviorMenu() {
        Menu result = new Menu(Strings.THREE_D_MENU);
        Menu cameraPositionsSubMenu = new Menu(Strings.THREE_D_MENU_CAMERA_POSITIONS);
        result.setOnShowing(showingEvent -> {
            ObservableList<MenuItem> items = cameraPositionsSubMenu.getItems();
            items.clear();
            MenuItem saveItem = new MenuItem(Strings.THREE_D_MENU_CAMERA_POSITIONS_SAVE_CURRENT);
            saveItem.setOnAction(event -> {
                querySaveCurrentCameraPosition();
            });
            items.add(saveItem);
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
        result.getItems().add(cameraPositionsSubMenu);
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