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

import java.util.Arrays;
import java.util.List;

import de.dh.utils.csg.SurfaceAwareCSG.ExtrusionSurfaceDataProvider;
import eu.mihosoft.vvecmath.Vector3d;

public class CSGs {
    public static final String DEFAULT_SURFACE_FRONT = "front";
    public static final String DEFAULT_SURFACE_RIGHT = "right";
    public static final String DEFAULT_SURFACE_BACK = "back";
    public static final String DEFAULT_SURFACE_LEFT = "left";
    public static final String DEFAULT_SURFACE_TOP = "top";
    public static final String DEFAULT_SURFACE_BOTTOM = "bottom";

    /**
     * Creates a box with the given dimensions and the same given surface type on each of the six sides.
     */
    public static SurfaceAwareCSG<String> box(double width, double height, double depth, String surface) {
        return box(width, height, depth,
            surface, surface,
            surface, surface,
            surface, surface);
    }

    /**
     * Creates a box with the given dimensions and the default surface types (see constants in this class).
     */
    public static SurfaceAwareCSG<String> box(double width, double height, double depth) {
        return box(width, height, depth,
            DEFAULT_SURFACE_FRONT, DEFAULT_SURFACE_BACK,
            DEFAULT_SURFACE_LEFT, DEFAULT_SURFACE_RIGHT,
            DEFAULT_SURFACE_BOTTOM, DEFAULT_SURFACE_TOP);
    }

    /**
     * Creates a box with the given dimensions andwith the given surface types.
     */
    public static SurfaceAwareCSG<String> box(double width, double height, double depth,
        String surfaceFront, String surfaceBack,
        String surfaceLeft, String surfaceRight,
        String surfaceBottom, String surfaceTop) {
        List<Vector3d> topPoints = Arrays.asList(
            Vector3d.xyz(0, 0, height),
            Vector3d.xyz(0, depth, height),
            Vector3d.xyz(width, depth, height),
            Vector3d.xyz(width, 0, height));
        List<Vector3d> bottomPoints = Arrays.asList(
            Vector3d.xyz(0, 0, 0),
            Vector3d.xyz(0, depth, 0),
            Vector3d.xyz(width, depth, 0),
            Vector3d.xyz(width, 0, 0));

        List<String> borderSurfacesCCW = Arrays.asList(
            surfaceFront,
            surfaceRight,
            surfaceBack,
            surfaceLeft);

        Vector3d topPolygonTextureDirectionX = Vector3d.xyz(width, 0, 0);
        Vector3d bottomPolygonTextureDirectionX = Vector3d.xyz(width, 0, 0);

        return SurfaceAwareCSG.extrudeSurfaces(new ExtrusionSurfaceDataProvider<String>() {
            @Override
            public List<Vector3d> getBottomPolygonPointsCCW() {
                return bottomPoints;
            }

            @Override
            public List<Vector3d> getConnectedTopPolygonPointsCCW() {
                return topPoints;
            }

            @Override
            public Vector3d getTopPolygonTextureDirectionX() {
                return topPolygonTextureDirectionX;
            }

            @Override
            public Vector3d getBottomPolygonTextureDirectionX() {
                return bottomPolygonTextureDirectionX;
            }

            @Override
            public String getSurfaceCCW(int startPointIndex) {
                return borderSurfacesCCW.get(startPointIndex);
            }

            @Override
            public String getTopSurface() {
                return surfaceTop;
            }

            @Override
            public String getBottomSurface() {
                return surfaceBottom;
            }
        }, 0, false);
    }
}
