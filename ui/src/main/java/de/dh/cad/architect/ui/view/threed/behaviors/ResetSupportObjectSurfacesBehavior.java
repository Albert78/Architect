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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.dh.cad.architect.model.objects.BaseObject;
import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.objects.Abstract3DAncillaryObject;
import de.dh.cad.architect.ui.objects.Abstract3DRepresentation;
import de.dh.cad.architect.ui.objects.SupportObject3DRepresentation;
import de.dh.cad.architect.ui.utils.Cursors;
import de.dh.cad.architect.ui.view.AbstractPlanView;
import de.dh.cad.architect.ui.view.AbstractUiMode;
import de.dh.cad.architect.ui.view.IContextAction;
import de.dh.cad.architect.ui.view.threed.ThreeDView;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

public class ResetSupportObjectSurfacesBehavior extends Abstract3DViewBehavior {
    protected static Cursor RESET_CURSOR = Cursors.createCursorReset();
    protected static Cursor FORBIDDEN_CURSOR = Cursors.createCursorForbidden();

    protected ChangeListener<Boolean> MOUSE_OVER_SUPPORT_OBJECT_LISTENER = new ChangeListener<>() {
        @Override
        public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
            ThreeDView view = getView();
            if (newValue) {
                view.setCursor(RESET_CURSOR);
            } else {
                view.setCursor(Cursor.DEFAULT);
            }
        }
    };

    protected ChangeListener<Boolean> MOUSE_OVER_OTHER_OBJECT_LISTENER = new ChangeListener<>() {
        @Override
        public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
            ThreeDView view = getView();
            if (newValue) {
                view.setCursor(FORBIDDEN_CURSOR);
            } else {
                view.setCursor(Cursor.DEFAULT);
            }
        }
    };

    public ResetSupportObjectSurfacesBehavior(AbstractUiMode<Abstract3DRepresentation, Abstract3DAncillaryObject> parentMode) {
        super(parentMode);
    }

    @Override
    protected boolean canHandleSelectionChange() {
        return true;
    }

    @Override
    protected void setDefaultUserHint() {
        setUserHint(Strings.THREE_D_RESET_SUPPORT_OBJECT_SURFACES_BEHAVIOR_USER_HINT);
    }

    @Override
    public String getTitle() {
        return Strings.THREE_D_RESET_SUPPORT_OBJECT_SURFACES_BEHAVIOR_TITLE;
    }

    @Override
    protected void configureObject(Abstract3DRepresentation repr) {
        if (repr instanceof SupportObject3DRepresentation soRepr) {
            repr.enableMouseOverSpot();
            repr.mouseOverProperty().removeListener(MOUSE_OVER_SUPPORT_OBJECT_LISTENER);
            repr.mouseOverProperty().addListener(MOUSE_OVER_SUPPORT_OBJECT_LISTENER);

            repr.setOnMouseClicked(new EventHandler<>() {
                @Override
                public void handle(MouseEvent event) {
                    if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 1) {
                        soRepr.resetSupportObjectSurfaces(soRepr.getSupportObject(), getUiController());
                    }
                }
            });
        } else {
            // Do nothing, prevent other objects from being clickable to prevent leaving this behavior accidentally
            repr.mouseOverProperty().removeListener(MOUSE_OVER_OTHER_OBJECT_LISTENER);
            repr.mouseOverProperty().addListener(MOUSE_OVER_OTHER_OBJECT_LISTENER);
        }
    }

    @Override
    protected void unconfigureObject(Abstract3DRepresentation repr, boolean objectRemoved) {
        if (repr instanceof SupportObject3DRepresentation) {
            repr.disableMouseOverSpot();
            repr.mouseOverProperty().removeListener(MOUSE_OVER_SUPPORT_OBJECT_LISTENER);
            repr.setOnMouseClicked(null);
        } else {
            repr.mouseOverProperty().removeListener(MOUSE_OVER_OTHER_OBJECT_LISTENER);
        }
    }

    @Override
    protected void updateActionsList(List<BaseObject> selectedObjects, List<BaseObject> selectedRootObjects) {
        Collection<IContextAction> actions = new ArrayList<>();

        actions.add(createCameraPositionsMenuAction());
        actions.add(createCancelBehaviorAction(Strings.CANCEL_RESET_SUPPORT_OBJECT_SURFACES_BEHAVIOR));

        mActionsList.setAll(actions);
    }

    @Override
    public void install(AbstractPlanView<Abstract3DRepresentation, Abstract3DAncillaryObject> view) {
        super.install(view);
        installDefaultEscapeBehaviorKeyHandler();
    }

    @Override
    public void uninstall() {
        uninstallDefaultEscapeBehaviorKeyHandler();
        super.uninstall();
    }
}