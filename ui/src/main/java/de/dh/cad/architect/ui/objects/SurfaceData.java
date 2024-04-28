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

import java.util.Objects;
import java.util.Optional;

import de.dh.cad.architect.model.objects.MaterialMappingConfiguration;
import de.dh.utils.Vector2D;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Shape3D;

/**
 * Represents an object's surface view together with the ability to show a temporary overlay material or
 * a temporary overlay color, used for showing the selection, object or surface spot and for painting object's surfaces.
 * @param <T> Type of the surface's shape object.
 */
public class SurfaceData<T extends Shape3D> {
    protected final AbstractSolid3DRepresentation mOwnerRepr;
    protected final String mSurfaceTypeId;

    protected final T mShape;
    protected Optional<Vector2D> mOSurfaceSize = Optional.empty();

    protected PhongMaterial mOriginalMaterial = null;
    protected Color mOriginalColor = null;
    protected PhongMaterial mOverlayMaterial = null;
    protected Color mOverlayColor = null;

    public SurfaceData(AbstractSolid3DRepresentation ownerRepr, String surfaceTypeId, T shape) {
        mOwnerRepr = ownerRepr;
        mSurfaceTypeId = surfaceTypeId;
        mShape = shape;
        PhongMaterial material = (PhongMaterial) mShape.getMaterial();
        if (material == null) {
            material = new PhongMaterial();
        }
        setMaterial(material);
    }

    public AbstractSolid3DRepresentation getOwnerRepr() {
        return mOwnerRepr;
    }

    public MaterialMappingConfiguration getMaterialMappingConfiguration() {
        return mOwnerRepr.getSurfaceMaterial(mSurfaceTypeId);
    }

    public void setMaterialMappingConfiguration(MaterialMappingConfiguration value) {
        mOwnerRepr.setSurfaceMaterial(mSurfaceTypeId, value);
    }

    public String getSurfaceTypeId() {
        return mSurfaceTypeId;
    }

    public T getShape() {
        return mShape;
    }

    public Optional<Vector2D> getSurfaceSize() {
        return mOSurfaceSize;
    }

    public void setSurfaceSize(Vector2D value) {
        mOSurfaceSize = Optional.ofNullable(value);
    }

    // **************** Material management *****************

    public PhongMaterial getMaterial() {
        return (PhongMaterial) mShape.getMaterial();
    }

    public void setMaterial(PhongMaterial material) {
        if (material == null) {
            material = new PhongMaterial();
        }
        mOriginalMaterial = material;
        mOriginalColor = mOriginalMaterial.getDiffuseColor();
        updateShape();
    }

    /**
     * Sets the given material to be used temporarily for our shape, e.g. if we're showing a material preview on
     * the surface. The original material can be reset via {@link #resetOverlayMaterial()}.
     * @param value Temporary material to be shown on this surface.
     */
    public void setOverlayMaterial(PhongMaterial value) {
        if (Objects.equals(mOverlayMaterial, value)) {
            return;
        }
        mOverlayMaterial = value;
        updateShape();
    }

    /**
     * Removes the temporary overlay material from this surface.
     */
    public void resetOverlayMaterial() {
        if (mOverlayMaterial == null) {
            return;
        }
        mOverlayMaterial = null;
        updateShape();
    }

    /**
     * Sets the given temporary object color for our shape, e.g. to show a current object's selection or mouse spot.
     * @param value Temporary object color.
     */
    public void setOverlayColor(Color value) {
        if (Objects.equals(mOverlayColor, value)) {
            return;
        }
        mOverlayColor = value;
        updateShape();
    }

    /**
     * Removes the temporary overlay color.
     */
    public void resetOverlayColor() {
        if (mOverlayColor == null) {
            return;
        }
        mOverlayColor = null;
        updateShape();
    }

    protected void updateShape() {
        if (mOverlayMaterial != null) {
            mShape.setMaterial(mOverlayMaterial);
            return;
        }
        if (mOverlayColor != null) {
            mOriginalMaterial.setDiffuseColor(mOverlayColor);
            mShape.setMaterial(mOriginalMaterial);
            return;
        }
        mOriginalMaterial.setDiffuseColor(mOriginalColor);
        mShape.setMaterial(mOriginalMaterial);
    }
}
