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
package de.dh.cad.architect.ui.objects;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.dh.cad.architect.model.changes.IModelChange;
import de.dh.cad.architect.model.objects.BaseSolidObject;
import de.dh.cad.architect.model.objects.MaterialMappingConfiguration;
import de.dh.cad.architect.model.objects.SurfaceConfiguration;
import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.objects.AbstractObjectUIRepresentation.Cardinality;
import de.dh.cad.architect.ui.view.threed.Abstract3DView;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape3D;

/**
 * UI representation of a model object of subtype {@link BaseSolidObject}. This class adds object surface handling.
 */
public abstract class AbstractSolid3DRepresentation extends Abstract3DRepresentation {
    public enum MouseSpotMode {
        None,
        Object,
        Surface
    }

    protected final SimpleObjectProperty<SurfaceData<? extends Shape3D>> mMouseOverSurfaceProperty = new SimpleObjectProperty<>(null);
    protected final SimpleObjectProperty<SurfaceData<? extends Shape3D>> mSpottedSurfaceProperty = new SimpleObjectProperty<>(this, "spottedSurface", null);

    protected final Map<String, SurfaceData<? extends Shape3D>> mSurfacesByTypeId = new HashMap<>();

    protected MouseSpotMode mMouseSpotMode = MouseSpotMode.None;

    protected AbstractSolid3DRepresentation(BaseSolidObject modelObject, Abstract3DView parentView) {
        super(modelObject, parentView);

        ChangeListener<Boolean> overlayColorUpdateListener = (observable, oldVal, newVal) -> updateOverlayColor();
        mSelectedProperty.addListener(overlayColorUpdateListener);
        mObjectSpottedProperty.addListener(overlayColorUpdateListener);
        mSpottedSurfaceProperty.addListener((observable, oldVal, newVal) -> surfaceSpotChanged(oldVal, newVal));
    }

    /**
     * Updates the overlay colors of all surfaces based on the selection and spot state.
     */
    protected void updateOverlayColor() {
        for (SurfaceData<?> surface : mSurfacesByTypeId.values()) {
            Color color = calculateSurfaceColor(getSpottedSurface() == surface || isObjectSpotted());
            if (color == null) {
                surface.resetOverlayColor();
            } else {
                surface.setOverlayColor(color);
            }
        }
    }

    protected void surfaceSpotChanged(SurfaceData<? extends Shape3D> oldSpottedSurface, SurfaceData<? extends Shape3D> newSpottedSurface) {
        if (oldSpottedSurface != null) {
            oldSpottedSurface.getOwnerRepr().updateOverlayColor();
        }
        if (newSpottedSurface != null) {
            Color color = calculateSurfaceColor(true);
            newSpottedSurface.setOverlayColor(color);
        }
    }

    public ObjectProperty<SurfaceData<? extends Shape3D>> mouseOverSurfaceProperty() {
        return mMouseOverSurfaceProperty;
    }

    public SurfaceData<? extends Shape3D> getMouseOverSurface() {
        return mMouseOverSurfaceProperty.get();
    }

    /**
     * Property representing the currently spotted surface. This property holds the currently spotted surface if
     * the {@link #getMouseSpotMode() mouse spot mode} is set to {@link AbstractSolid3DRepresentation.MouseSpotMode#Surface}, else it remains
     * {@code null}.
     */
    public SimpleObjectProperty<SurfaceData<? extends Shape3D>> spottedSurfaceProperty() {
        return mSpottedSurfaceProperty;
    }

    public SurfaceData<? extends Shape3D> getSpottedSurface() {
        return mSpottedSurfaceProperty.get();
    }

    /**
     * Gets the mouse spot mode of this object. This will define which parts of this object are shown in a highlight color
     * if the mouse cursor is over it.
     * If set to {@link MouseSpotMode#None}, the object won't show a highlight. If set to {@link MouseSpotMode#Object},
     * the whole object will be highlighted. If set to {@link MouseSpotMode#Surface}, only the object surface under the
     * mouse cursor will be highlighted.
     * Depending on the spot mode, properties {@link #objectSpottedProperty()} or {@link #spottedSurfaceProperty()}
     * will be bound to the corresponding mouse-over properties, which means,
     * the properties {@link #mouseOverProperty()} and {@link #mouseOverSurfaceProperty()} always represent the mouse-over
     * state while the values of properties {@link #objectSpottedProperty()} and {@link #spottedSurfaceProperty()} remain
     * {@code null} if the corresponding mouse spot mode is not switched on.
     */
    public MouseSpotMode getMouseSpotMode() {
        return mMouseSpotMode;
    }

    public void setMouseSpotMode(MouseSpotMode value) {
        if (mMouseSpotMode == value) {
            return;
        }
        mMouseSpotMode = value;
        mObjectSpottedProperty.unbind();
        mObjectSpottedProperty.set(false);
        mSpottedSurfaceProperty.unbind();
        mSpottedSurfaceProperty.set(null);

        switch (value) {
            case None:
                break;
            case Object:
                mObjectSpottedProperty.bind(mMouseOverProperty);
                break;
            case Surface:
                mSpottedSurfaceProperty.bind(mMouseOverSurfaceProperty);
                break;
        }
    }

    public void setDefaultMouseSpotMode() {
        setMouseSpotMode(MouseSpotMode.Object);
    }

    /**
     * Assigns the given surface to the given 3D node object. This needs to be done when the object is constructed, it is
     * necessary for the {@link #mouseOverSurfaceProperty()} and the mouse spot engine to work.
     * Currently, a node can only be marked with a surface but that assignment cannot be reverted for technical reasons.
     * If such a node is {@link #remove(Node) removed} for some reason, that node must not be reused.
     */
    protected void registerSurface(SurfaceData<? extends Shape3D> surface) {
        Shape3D shape = surface.getShape();
        // Those event handlers represent the context of the node <-> surface mapping,
        // thus we cannot put them into fields. That's why we cannot remember the event handler's
        // instances without further overhead. That's why we currently don't support the removal of the event handlers
        // in some unregister() method.
        // If that capability will be needed in the future, we could store the event handler's instances
        // in the surface or something...
        shape.addEventHandler(MouseEvent.MOUSE_ENTERED, event -> {
            mMouseOverSurfaceProperty.set(surface);
        });
        shape.addEventHandler(MouseEvent.MOUSE_EXITED, event -> {
            mMouseOverSurfaceProperty.set(null);
        });
        mSurfacesByTypeId.put(surface.getSurfaceTypeId(), surface);
    }

    @Override
    public BaseSolidObject getModelObject() {
        return (BaseSolidObject) super.getModelObject();
    }

    public MaterialMappingConfiguration getSurfaceMaterial(String surfaceTypeId) {
        SurfaceConfiguration surfaceConfiguration = getModelObject().getSurfaceTypeIdsToSurfaceConfigurations().get(surfaceTypeId);
        return surfaceConfiguration == null ? null : surfaceConfiguration.getMaterialMappingConfiguration();
    }

    /**
     * Sets the material of the given surface in the underlaying business object.
     * @param surfaceTypeId Surface type id of the surface to assign the given material.
     * @param materialConfiguration Material configuration for the mapping of the material to the surface.
     */
    public void setSurfaceMaterial(String surfaceTypeId, MaterialMappingConfiguration materialConfiguration) {
        BaseSolidObject bo = getModelObject();
        List<IModelChange> changeTrace = new ArrayList<>();
        bo.setSurfaceMaterial(surfaceTypeId, materialConfiguration, changeTrace);
        mParentView.getUiController().notifyChange(changeTrace,
            MessageFormat.format(Strings.SET_OBJECT_MATERIAL_CHANGE,
                ObjectTypesRegistry.getUIRepresentation(bo.getClass()).getTypeName(Cardinality.Singular)));
    }
}
