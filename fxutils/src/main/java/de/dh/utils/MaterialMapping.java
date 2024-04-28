/*******************************************************************************
 * Architect - A free 2D/3D home and interior designer
 * Copyright (c) 2024 Daniel Höh
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>
 ******************************************************************************/

package de.dh.utils;

import java.util.Optional;

/**
 * Configuration options for the application of a raw material texture on a target {@link javafx.scene.paint.Material}.
 * A material texture can be either {@link LayoutMode#Stretch stretched} or {@link LayoutMode#Tile tiled} to fill a target
 * object.
 */
public class MaterialMapping {
    public static final double DEFAULT_TEXTURE_RESOLUTION_PER_LENGTH_UNIT = 1;

    public enum LayoutMode {
        Stretch, Tile
    }

    protected Optional<Vector2D> mMaterialOffset;
    protected Optional<Vector2D> mMaterialTileSize;
    protected Optional<Double> mMaterialRotationDeg;
    protected LayoutMode mLayoutMode;
    protected Optional<Vector2D> mTargetSurfaceSize;
    protected double mTextureResolutionPerLengthUnit = DEFAULT_TEXTURE_RESOLUTION_PER_LENGTH_UNIT;

    public MaterialMapping(Optional<Vector2D> materialOffset, Optional<Vector2D> materialTileSize,
            Optional<Double> materialRotationDeg, LayoutMode layoutMode, Optional<Vector2D> targetSurfaceSize) {
        mMaterialOffset = materialOffset;
        mMaterialTileSize = materialTileSize;
        mMaterialRotationDeg = materialRotationDeg;
        mLayoutMode = layoutMode;
        mTargetSurfaceSize = targetSurfaceSize;
    }

    public static MaterialMapping stretch() {
        return new MaterialMapping(Optional.empty(), Optional.empty(), Optional.empty(),
                LayoutMode.Stretch, Optional.empty());
    }

    public static MaterialMapping tile(Vector2D targetSurfaceSize) {
        return new MaterialMapping(Optional.empty(), Optional.empty(), Optional.empty(),
                LayoutMode.Tile, Optional.of(targetSurfaceSize));
    }

    public static MaterialMapping tile(Vector2D materialTileSize, Vector2D targetSurfaceSize) {
        return new MaterialMapping(Optional.empty(), Optional.of(materialTileSize), Optional.empty(),
                LayoutMode.Tile, Optional.of(targetSurfaceSize));
    }

    public static MaterialMapping tile(Vector2D materialTileSize, double materialRotationDeg, Vector2D targetSurfaceSize) {
        return new MaterialMapping(Optional.empty(), Optional.of(materialTileSize), Optional.of(materialRotationDeg),
                LayoutMode.Tile, Optional.of(targetSurfaceSize));
    }

    /**
     * Returns the information whether the material tile should be tiled or stretched on the target surface.
     */
    public LayoutMode getLayoutMode() {
        return mLayoutMode;
    }

    public void setLayoutMode(LayoutMode value) {
        mLayoutMode = value;
    }

    /**
     * Gets the offset of the material tile alignment on the target texture in pixels.
     * The offset will be applied to the material tile first, after that the (moved) tile gets rotated.
     * If defined, this value overrides the offset value given in the raw material declaration.
     */
    public Optional<Vector2D> getMaterialOffset() {
        return mMaterialOffset;
    }

    public void setMaterialOffset(Optional<Vector2D> value) {
        mMaterialOffset = value;
    }

    /**
     * Gets the size in pixels one material tile should cover on the target surface texture.
     * This value is only taken into account if the {@link #getLayoutMode() layout mode} is set to
     * {@link LayoutMode#Tile}.
     * If defined, this value overrides the scale value given in the raw material declaration.
     */
    public Optional<Vector2D> getMaterialTileSize() {
        return mMaterialTileSize;
    }

    public void setMaterialTileSize(Optional<Vector2D> value) {
        mMaterialTileSize = value;
    }

    /**
     * Gets the angle in degrees the material should be rotated on the surface in clockwise direction.
     */
    public Optional<Double> getMaterialRotationDeg() {
        return mMaterialRotationDeg;
    }

    public void setMaterialRotationDeg(Optional<Double> value) {
        mMaterialRotationDeg = value;
    }

    /**
     * Gets the size of the target surface where the material will be applied.
     * The resolution of the created material texture will be derived from this value multiplied by
     * {@link #getTextureResolutionPerLengthUnit()}.
     * This will also affect the tile count of the material tile on the target surface.
     */
    public Optional<Vector2D> getTargetSurfaceSize() {
        return mTargetSurfaceSize;
    }

    public void setTargetSurfaceSize(Optional<Vector2D> value) {
        mTargetSurfaceSize = value;
    }

    /**
     * Gets the multiplier for the calculation of the target material's texture size.
     * When a target texture is generated, we typically want to have it in a higher resolution than
     * the pixel size of the surface. This will facilitate a better render result when objects are transformed/zoomed etc.
     * The default value for this property is {@link #DEFAULT_TEXTURE_RESOLUTION_PER_LENGTH_UNIT}.
     */
    public double getTextureResolutionPerLengthUnit() {
        return mTextureResolutionPerLengthUnit;
    }

    public void setTextureResolutionPerLengthUnit(double value) {
        mTextureResolutionPerLengthUnit = value;
    }
}
