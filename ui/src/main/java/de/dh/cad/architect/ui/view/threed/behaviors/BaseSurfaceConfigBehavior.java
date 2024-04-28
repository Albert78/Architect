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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.dh.cad.architect.model.objects.BaseObject;
import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.controller.UiController;
import de.dh.cad.architect.ui.objects.Abstract3DAncillaryObject;
import de.dh.cad.architect.ui.objects.Abstract3DRepresentation;
import de.dh.cad.architect.ui.objects.AbstractSolid3DRepresentation;
import de.dh.cad.architect.ui.objects.BaseObjectUIRepresentation;
import de.dh.cad.architect.ui.objects.SurfaceData;
import de.dh.cad.architect.ui.view.AbstractUiMode;
import de.dh.cad.architect.ui.view.IContextAction;
import javafx.scene.Cursor;
import javafx.scene.shape.Shape3D;

public class BaseSurfaceConfigBehavior extends AbstractPainterModeBehavior {
    protected class SurfaceHighlight {
        protected final SurfaceData<? extends Shape3D> mSurface;

        public SurfaceHighlight(SurfaceData<? extends Shape3D> surface) {
            mSurface = surface;
            updateMouseCursor();
        }

        public SurfaceData<? extends Shape3D> getSurface() {
            return mSurface;
        }

        protected void dispose() {
            resetMouseCursor();
        }

        public void reset() {
            dispose();
        }

        protected void updateMouseCursor() {
            getView().setCursor(getMouseOverSurfaceCursor());
        }

        protected void resetMouseCursor() {
            getView().setCursor(Cursor.DEFAULT);
        }

        public void commit() {
            dispose();
            switchToConfigSurfaceBehavior(mSurface);
        }
    }

    protected SurfaceHighlight mHighlightedSurface = null;

    public BaseSurfaceConfigBehavior(AbstractUiMode<Abstract3DRepresentation, Abstract3DAncillaryObject> parentMode) {
        super(parentMode);
    }

    @Override
    protected AbstractSolid3DRepresentation.MouseSpotMode getMouseSpotMode() {
        return AbstractSolid3DRepresentation.MouseSpotMode.Surface;
    }

    @Override
    protected void onMouseOverSurface(SurfaceData<? extends Shape3D> oldSurface, SurfaceData<? extends Shape3D> newSurface) {
        setHighlightedSurface(newSurface);
    }

    @Override
    protected void onMouseClick() {
        SurfaceData<? extends Shape3D> surface = mHighlightedSurface == null ? null : mHighlightedSurface.getSurface();
        if (surface != null) {
            UiController uiController = getUiController();
            uiController.setSelectedObjectId(surface.getOwnerRepr().getModelId());
        }
        commitHighlightedSurface();
    }

    protected void setHighlightedSurface(SurfaceData<? extends Shape3D> surface) {
        String surfaceTypeId = surface == null ? null : surface.getSurfaceTypeId();
        if (surfaceTypeId == null) {
            setDefaultUserHint();
        } else {
            @SuppressWarnings("null")
            Abstract3DRepresentation ownerRepr = surface.getOwnerRepr();
            setUserSurfaceHint(ownerRepr, surfaceTypeId);
        }
        if (mHighlightedSurface != null) {
            mHighlightedSurface.reset();
        }
        mHighlightedSurface = null;
        if (surface != null) {
            mHighlightedSurface = new SurfaceHighlight(surface);
        }
    }

    protected void commitHighlightedSurface() {
        if (mHighlightedSurface != null) {
            mHighlightedSurface.commit();
        }
        mHighlightedSurface = null;
    }

    protected Cursor getMouseOverSurfaceCursor() {
        return Cursor.HAND;
    }

    protected void switchToConfigSurfaceBehavior(SurfaceData<? extends Shape3D> surface) {
        AbstractUiMode<Abstract3DRepresentation, Abstract3DAncillaryObject> parentMode = getParentMode();
        parentMode.setBehavior(new SurfaceConfigBehavior(parentMode, surface));
    }

    @Override
    protected boolean canHandleSelectionChange() {
        return true;
    }

    @Override
    public void setDefaultUserHint() {
        setUserHint(Strings.THREE_D_PAINTER_CONFIG_SURFACE_BEHAVIOR_DEFAULT_USER_HINT);
    }

    protected void setUserSurfaceHint(Abstract3DRepresentation ownerRepr, String surfaceTypeId) {
        setUserHint(MessageFormat.format(Strings.THREE_D_PAINTER_CONFIG_SURFACE_BEHAVIOR_SURFACE_USER_HINT,
                BaseObjectUIRepresentation.getShortName(ownerRepr.getModelObject()), surfaceTypeId));
    }

    @Override
    public String getTitle() {
        return Strings.THREE_D_PAINTER_CONFIG_SURFACE_BEHAVIOR_TITLE;
    }

    @Override
    protected void updateActionsList(List<BaseObject> selectedObjects, List<BaseObject> selectedRootObjects) {
        Collection<IContextAction> actions = new ArrayList<>();

        actions.add(createSwitchToPainterBehaviorMenuAction());

        mActionsList.setAll(actions);
    }
}
