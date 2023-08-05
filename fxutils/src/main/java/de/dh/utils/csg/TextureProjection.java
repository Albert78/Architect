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
package de.dh.utils.csg;

import java.util.Collection;

import de.dh.utils.Vector2D;
import eu.mihosoft.vvecmath.Vector3d;

/**
 * Describes the projection of a (cutout part of a) texture (given in texture coords) to an object in real objects coordinate system.
 */
public class TextureProjection {
    protected final TextureCoordinateSystem mTextureCoordinateSystem;

    protected final Vector2D mMinTxy; // In texture coordinates
    protected final Vector2D mRangeTxy; // In texture coordinates

    private TextureProjection(TextureCoordinateSystem textureCoordinateSystem,
        Vector2D minTxy, Vector2D maxTxy) {
        mTextureCoordinateSystem = textureCoordinateSystem;

        mMinTxy = minTxy;
        mRangeTxy = maxTxy.minus(minTxy);
    }

    @SuppressWarnings("null")
    public static TextureProjection fromPointsBorder(
            TextureCoordinateSystem textureCoordinateSystem, Collection<Vector3d> points) {

        // Find min/max coordinates in texture coordinate system to find area the texture must cover
        Double minTx = null;
        Double minTy = null;
        Double maxTx = null;
        Double maxTy = null;

        for (Vector3d point : points) {
            Vector2D pT = textureCoordinateSystem.mapToTextureCoordinateSystem(point);

            double tX = pT.getX();
            double tY = pT.getY();

            if (minTx == null || tX < minTx) {
                minTx = tX;
            }
            if (maxTx == null || tX > maxTx) {
                maxTx = tX;
            }
            if (minTy == null || tY < minTy) {
                minTy = tY;
            }
            if (maxTy == null || tY > maxTy) {
                maxTy = tY;
            }
        }

        return new TextureProjection(textureCoordinateSystem, new Vector2D(minTx, minTy), new Vector2D(maxTx, maxTy));
    }

    /**
     * Gets the lower left border of this texture anchor rectangle in the texture coordinate system.
     */
    public Vector2D getMinTxy() {
        return mMinTxy;
    }

    /**
     * Gets the size of this texture anchor rectangle, in the texture coordinate system.
     * The texture projection is a rotation so the returned range is also the range of real coordinates
     * which are mapped to the texture anchor rectangle, see {@link #getSpannedSize()}.
     */
    public Vector2D getRangeTxy() {
        return mRangeTxy;
    }

    public TextureCoordinateSystem getTextureCoordinateSystem() {
        return mTextureCoordinateSystem;
    }

    /**
     * Given a point Pr in real coordinates, this method returns the mapping of Pr to this texture anchor rectangle, given in texture coordinate space,
     * normalized to a vector between (0; 0) and (1; 1) according to the position of mapped point in the texture anchor rectangle.
     */
    public Vector2D getTextureCoordinates(Vector3d point) {
        Vector2D pT = mTextureCoordinateSystem.mapToTextureCoordinateSystem(point);
        Vector2D diff = pT.minus(mMinTxy);
        return new Vector2D(
            diff.getX() / mRangeTxy.getX(),
            diff.getY() / mRangeTxy.getY());
    }

    /**
     * Returns the size of the range in real coordinates which is covered by this projection.
     * The returned X/Y length corresponds to the size the final texture will span in real coordinates.
     */
    public Vector2D getSpannedSize() {
        // We know that the texture coordinate system only uses rotations and never scales. Thus, the range
        // in texture coordinates is the same than then the range in real coordinates.
        return mRangeTxy;
    }

    public TextureProjection extend(double minX, double minY, double rangeX, int rangeY) {
        Vector2D newMin = new Vector2D(mMinTxy.getX() - minX, mMinTxy.getY() - minY);
        Vector2D newMax = new Vector2D(newMin.getX() + mRangeTxy.getX() + rangeX, newMin.getY() + mRangeTxy.getY() + rangeY);
        return new TextureProjection(mTextureCoordinateSystem, newMin, newMax);
    }
}