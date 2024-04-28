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

import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.objects.Abstract3DAncillaryObject;
import de.dh.cad.architect.ui.objects.Abstract3DRepresentation;
import de.dh.cad.architect.ui.objects.AbstractSolid3DRepresentation;
import de.dh.cad.architect.ui.objects.SurfaceData;
import de.dh.cad.architect.ui.view.AbstractUiMode;
import de.dh.cad.architect.ui.view.IContextAction;
import de.dh.cad.architect.ui.view.threed.ThreeDUIElementFilter;
import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Shape3D;

public abstract class AbstractPainterModeBehavior extends Abstract3DViewBehavior {
    protected ChangeListener<SurfaceData<? extends Shape3D>> MOUSE_OVER_SURFACE_CHANGE_LISTENER = (observable, oldValue, newValue) -> onMouseOverSurface(oldValue, newValue);

    protected EventHandler<MouseEvent> MOUSE_CLICK_HANDLER = event -> {
        if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 1) {
            onMouseClick();
            event.consume();
        }
    };

    public AbstractPainterModeBehavior(AbstractUiMode<Abstract3DRepresentation, Abstract3DAncillaryObject> parentMode) {
        super(parentMode);
        setUIElementFilter(new ThreeDUIElementFilter());
    }

    protected abstract void onMouseOverSurface(SurfaceData<? extends Shape3D> oldSurface, SurfaceData<? extends Shape3D> newSurface);

    protected abstract void onMouseClick();

    @Override
    protected void configureObject(Abstract3DRepresentation repr) {
        super.configureObject(repr);
        if (repr instanceof AbstractSolid3DRepresentation sRepr) {
            sRepr.mouseOverSurfaceProperty().removeListener(MOUSE_OVER_SURFACE_CHANGE_LISTENER);
            sRepr.mouseOverSurfaceProperty().addListener(MOUSE_OVER_SURFACE_CHANGE_LISTENER);
        }
        repr.setOnMouseClicked(MOUSE_CLICK_HANDLER);
    }

    @Override
    protected void unconfigureObject(Abstract3DRepresentation repr, boolean objectRemoved) {
        if (repr instanceof AbstractSolid3DRepresentation sRepr) {
            sRepr.mouseOverSurfaceProperty().removeListener(MOUSE_OVER_SURFACE_CHANGE_LISTENER);
        }
        repr.setOnMouseClicked(null);
        super.unconfigureObject(repr, objectRemoved);
    }

    // ************************************************ Default actions ************************************************
    protected IContextAction createSwitchToPainterBehaviorMenuAction() {
        return new IContextAction() {
            @Override
            public String getTitle() {
                return Strings.THREE_D_PAINTER_BEHAVIOR_ACTION_TITLE;
            }

            @Override
            public void execute() {
                AbstractUiMode<Abstract3DRepresentation, Abstract3DAncillaryObject> parentMode = getParentMode();
                parentMode.setBehavior(new PainterBehavior(parentMode));
            }
        };
    }
}
