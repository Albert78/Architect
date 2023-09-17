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

import de.dh.utils.Vector2D;
import de.dh.utils.fx.MathUtils;
import eu.mihosoft.vvecmath.Transform;
import eu.mihosoft.vvecmath.Vector3d;

/**
 * Given the situation that a texture should be applied to a plane surface in the real objects world,
 * this class describes the mapping between real world coordinates to texture coordinates.
 */
public class TextureCoordinateSystem {
    protected final Vector3d mTextureDirectionX; // X vector of texture; this is the texture U direction

    protected final Vector3d mTexturePlaneNormal; // Normal vector of texture plane. The plane goes through the point of origin.
    protected final Transform mMappingTransform; // Transformation mapping a vector in real coordinate system to 2D texture coordinate system

    private TextureCoordinateSystem(Vector3d textureNormal, Vector3d textureDirectionX) {
        mTexturePlaneNormal = textureNormal.normalized();
        mTextureDirectionX = textureDirectionX.normalized();

        Transform t = MathUtils.rot(Transform.unity(), mTexturePlaneNormal, Vector3d.Z_ONE);
        Vector3d tDX_XY = mTextureDirectionX.transformed(t);
        double angle = Vector2D.angleBetween(new Vector2D(tDX_XY.getX(), tDX_XY.getY()), Vector2D.X_ONE);
        t = Transform.unity().rot(Vector3d.ZERO, Vector3d.Z_ONE, angle);
        t = MathUtils.rot(t, mTexturePlaneNormal, Vector3d.Z_ONE);

        double[] m = t.to();
        // Projection to XY plane
        for (int i = 8; i < 12; i++) {
            m[i] = 0;
        }
        mMappingTransform = Transform.from(m);
    }

    /**
     * Creates a new coordinate system for texture mappings.
     * @param textureNormal Vector normal to the texture plane. The direction of the vector must be that way,
     * that texture normal, texture direction X and texture direction Y together build a LHS
     * (thumb = texture normal, forefinger = texture direction X, middle finger = texture direction Y.
     * @param textureDirectionX Vector in the texture plane pointing in positive X / U direction.
     */
    public static TextureCoordinateSystem create(Vector3d textureNormal, Vector3d textureDirectionX) {
        return new TextureCoordinateSystem(textureNormal, textureDirectionX);
    }

    public Vector3d getTextureDirectionX() {
        return mTextureDirectionX;
    }

    public Vector3d getTexturePlaneNormal() {
        return mTexturePlaneNormal;
    }

    public Vector2D mapToTextureCoordinateSystem(Vector3d realPoint) {
        Vector3d p = mMappingTransform.transform(realPoint);
        return new Vector2D(p.getX(), p.getY());
    }

    public static void main(String[] args) throws Exception {
        Vector3d x1;

        // Test:
        // Normal, texture X coordinate und (implicit) texture Y coordinate form a LHS
        // All tests should return roughly (1, 2)

        System.out.println("Normal Z, TexX X:");
        TextureCoordinateSystem tcsZX = TextureCoordinateSystem.create(Vector3d.Z_ONE, Vector3d.X_ONE);
        x1 = Vector3d.xyz(1, 2, 10);
        System.out.println("X " + x1 + " -> " + tcsZX.mapToTextureCoordinateSystem(x1));

        System.out.println("Normal Z, TexX -X:");
        TextureCoordinateSystem tcsZmX = TextureCoordinateSystem.create(Vector3d.Z_ONE, Vector3d.X_ONE.negated());
        x1 = Vector3d.xyz(-1, -2, 10);
        System.out.println("X " + x1 + " -> " + tcsZmX.mapToTextureCoordinateSystem(x1));

        System.out.println("Normal -Z, TexX X:");
        TextureCoordinateSystem tcsMZ = TextureCoordinateSystem.create(Vector3d.Z_ONE.negated(), Vector3d.X_ONE);
        x1 = Vector3d.xyz(1, -2, 10);
        System.out.println("X " + x1 + " -> " + tcsMZ.mapToTextureCoordinateSystem(x1));

        System.out.println("Normal X, TexX Y:");
        TextureCoordinateSystem tcsXY = TextureCoordinateSystem.create(Vector3d.X_ONE, Vector3d.Y_ONE);
        x1 = Vector3d.xyz(10, 1, 2);
        System.out.println("X " + x1 + " -> " + tcsXY.mapToTextureCoordinateSystem(x1));

        System.out.println("Normal Y, TexX X:");
        TextureCoordinateSystem tcsYX = TextureCoordinateSystem.create(Vector3d.Y_ONE, Vector3d.X_ONE);
        x1 = Vector3d.xyz(1, 10, -2);
        System.out.println("X " + x1 + " -> " + tcsYX.mapToTextureCoordinateSystem(x1));

        System.out.println("Normal Y, TexX Z:");
        TextureCoordinateSystem tcsYZ = TextureCoordinateSystem.create(Vector3d.Y_ONE, Vector3d.Z_ONE);
        x1 = Vector3d.xyz(2, 10, 1);
        System.out.println("X " + x1 + " -> " + tcsYZ.mapToTextureCoordinateSystem(x1));
    }
}