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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import de.dh.utils.csg.CSGSurfaceAwareAddon.SurfacePart;
import eu.mihosoft.jcsg.CSG;
import eu.mihosoft.jcsg.Polygon;
import eu.mihosoft.jcsg.PropertyStorage;
import eu.mihosoft.jcsg.ext.org.poly2tri.PolygonUtil;
import eu.mihosoft.vvecmath.Vector3d;

/**
 * Utility class for the Creation of CSG objects of a common shape.
 * The objects are located on the X/Y plane and point upwards in Z direction.
 */
public class CSGs {
    //////////////////////////////////////////////////////// Boxes ///////////////////////////////////////////////////////////////
    public static final String DEFAULT_SURFACE_FRONT = "front";
    public static final String DEFAULT_SURFACE_RIGHT = "right";
    public static final String DEFAULT_SURFACE_BACK = "back";
    public static final String DEFAULT_SURFACE_LEFT = "left";
    public static final String DEFAULT_SURFACE_TOP = "top";
    public static final String DEFAULT_SURFACE_BOTTOM = "bottom";

    /**
     * Creates a box with the given dimensions and the same given surface type on each of the six sides.
     */
    public static CSG box(double width, double depth, double height, String surface) {
        return box(width, depth, height,
            surface, surface,
            surface, surface,
            surface, surface);
    }

    /**
     * Creates a box with the given dimensions and the default surface types (see constants in this class).
     */
    public static CSG box(double width, double depth, double height) {
        return box(width, depth, height,
            DEFAULT_SURFACE_FRONT, DEFAULT_SURFACE_BACK,
            DEFAULT_SURFACE_LEFT, DEFAULT_SURFACE_RIGHT,
            DEFAULT_SURFACE_BOTTOM, DEFAULT_SURFACE_TOP);
    }

    /**
     * Creates a box with the given dimensions and with the given surface types.
     */
    public static CSG box(double width, double depth, double height,
        String surfaceFront, String surfaceBack,
        String surfaceLeft, String surfaceRight,
        String surfaceBottom, String surfaceTop) {
        // Width = X direction
        // Depth = Y direction
        // Height = Z direction
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

        List<String> borderSurfacesCW = Arrays.asList(
            surfaceLeft,
            surfaceBack,
            surfaceRight,
            surfaceFront);

        Vector3d topPolygonTextureDirectionX = Vector3d.xyz(width, 0, 0);
        Vector3d bottomPolygonTextureDirectionX = Vector3d.xyz(width, 0, 0);

        return extrudeSurfaces(new ExtrusionSurfaceDataProvider<String>() {
            @Override
            public List<Vector3d> getBottomPolygonPointsCW() {
                return bottomPoints;
            }

            @Override
            public List<Vector3d> getTopPolygonPointsCW() {
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
            public String getSurfaceCW(int startPointIndex) {
                return borderSurfacesCW.get(startPointIndex);
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

    /////////////////////////////////////////////////////////////// Extrusions ////////////////////////////////////////////////////////////
    /**
     * Class which provides all data for a surface aware CSG extruded from a base polygon.
     */
    public interface ExtrusionSurfaceDataProvider<S> {
        /**
         * Returns a list of corner points of the bottom surface.
         * Since the extrusion is in Z direction, the bottom points must have the smaller Z coordinate values.
         * At the moment, we only support convex or concave polygons without holes or intersections,
         * given in counter-clockwise direction. The polygon plane must not be orthogonal to the X/Y plane
         * because of the limitations of the extrusion function.
         */
        List<Vector3d> getBottomPolygonPointsCW();

        /**
         * Returns a list of polygon points for the top surface which represent the same corners as the bottom
         * polygon points. The point ordering must be the same as for {@link #getBottomPolygonPointsCW() the bottom points},
         * i.e. each bottom point must have a corresponding point in the result of this method.
         * Since the extrusion is in Z direction, the top points must have the higher Z coordinate values.
         */
        List<Vector3d> getTopPolygonPointsCW();

        Vector3d getTopPolygonTextureDirectionX();
        Vector3d getBottomPolygonTextureDirectionX();

        // Base points are located in XY plane, so texture normal of top polygon is in positive Z direction,
        // normal for bottom polygon is in negative Z direction
        default TextureCoordinateSystem getTopPolygonTextureCoordinateSystem() {
            return TextureCoordinateSystem.create(
                Vector3d.Z_ONE,
                getTopPolygonTextureDirectionX());
        }

        default TextureCoordinateSystem getBottomPolygonTextureCoordinateSystem() {
            return TextureCoordinateSystem.create(
                Vector3d.Z_ONE.negated(),
                getBottomPolygonTextureDirectionX());
        }

        default TextureProjection getTopPolygonTextureProjection() {
            return TextureProjection.fromPointsBorder(getTopPolygonTextureCoordinateSystem(), getTopPolygonPointsCW());
        }

        default TextureProjection getBottomPolygonTextureProjection() {
            return TextureProjection.fromPointsBorder(getBottomPolygonTextureCoordinateSystem(), getBottomPolygonPointsCW());
        }

        /**
         * Gets the (side) surface in the connection between the point of the given start index and the next point in the list of polygon points.
         */
        S getSurfaceCW(int startPointIndex);

        S getTopSurface();
        S getBottomSurface();
    }

    /**
     * Extrudes a given (bottom) polygon specified by a border path up to another similar given (top) polygon with the same number
     * of edges, marking top, bottom and all extruded side faces internally with surface markers. Extrusion is in Z direction.
     *
     * Main idea for this method is to create a CSG for a wall, specified by a given base outline and extruded to
     * top points representing the upper border of that wall. From that CSG, we can afterwards remove the holes
     * for windows and doors.
     *
     * A surface is one face of the created object, that can be a simple plane (like for bottom or top surfaces) but a surface
     * can also extend over multiple continuous parts like for beveled walls, whose surface consists of the "main" part for that
     * wall and the small bevel part.
     * The extruded polygon will consist of N surfaces, one for the top, one for the bottom and
     * an arbitrary count of surfaces for the borders, each including a consecutive list of extruded base edges.
     * For each surface, we will create a separate mesh in method {@link #createMeshes(Optional)}, which means,
     * each surface can be covered with a separate texture at the end. With parameter {@code continueSurfaceTextures} set to {@code true},
     * adjacent surfaces of the same type will be combined to a bigger surface.
     *
     * During the extrusion process, data about the relative polygon location in a surface is collected and stored in the
     * polygon's properties for later generating the mesh objects in method {@link CSGSurfaceAwareAddon#createMeshes(CSG, CSGSurfaceAwareAddon.ISurfaceDataProvider)}.
     *
     * @param extrusionSurfaceDataProvider Provider for the data needed during the creation of the resulting CSG.
     * @param startPoint Index of a path point of the top- and bottom polygons provided by the surface data provider,
     * which is a starting point of one surface. This is to control the starting point of the first surface texture.
     * For single-surface objects, this is the starting point where the (single) surface texture begins.
     * @param continueSurfaceTextures Combines adjacent surfaces of the same surface type to a bigger logical surface,
     * extending over the combined surfaces, to make it yield continuous surface texture coordinates.
     *
     * @return A surface aware CSG object with the desired surface mappings.
     */
    public static <T, S> CSG extrudeSurfaces(ExtrusionSurfaceDataProvider<S> extrusionSurfaceDataProvider, int startPoint, boolean continueSurfaceTextures) {
        // Top
        S topSurface = extrusionSurfaceDataProvider.getTopSurface();
        SurfacePart<S> topPart = new SurfacePart<>(topSurface, extrusionSurfaceDataProvider.getTopPolygonTextureProjection());
        PropertyStorage topProperties = CSGSurfaceAwareAddon.createSurfacePartProperties(topPart);

        // Bottom
        S bottomSurface = extrusionSurfaceDataProvider.getBottomSurface();
        SurfacePart<S> bottomPart = new SurfacePart<>(bottomSurface, extrusionSurfaceDataProvider.getBottomPolygonTextureProjection());
        PropertyStorage bottomProperties = CSGSurfaceAwareAddon.createSurfacePartProperties(bottomPart);

        List<Vector3d> bottomPolygonPointsCW = extrusionSurfaceDataProvider.getBottomPolygonPointsCW();
        List<Vector3d> topPolygonPointsCW = extrusionSurfaceDataProvider.getTopPolygonPointsCW();
        List<Vector3d> bottomPolygonPointsCCW = new ArrayList<>(bottomPolygonPointsCW);
        Collections.reverse(bottomPolygonPointsCCW); // Turn points counter-clockwise to turn polygons to the bottom

        List<Polygon> surfacePolygons = new ArrayList<>();

        // Calculate top polygons
        List<Polygon> topConvexCW = PolygonUtil.concaveToConvex(Polygon.fromPoints(PolygonUtil.cleanupPolygonPoints(topPolygonPointsCW)));
        for (Polygon polygon : topConvexCW) {
            polygon.setStorage(topProperties);
        }
        surfacePolygons.addAll(topConvexCW);

        // Calculate bottom polygons
        List<Polygon> bottomConvexCCW = PolygonUtil.concaveToConvex(Polygon.fromPoints(PolygonUtil.cleanupPolygonPoints(bottomPolygonPointsCCW)));
        for (Polygon polygon : bottomConvexCCW) {
            polygon.setStorage(bottomProperties);
        }
        surfacePolygons.addAll(bottomConvexCCW);

        Map<S, List<PropertyStorage>> surfaceProperties = new HashMap<>();
        int numPoints = bottomPolygonPointsCW.size();
        for (int i = 0; i < numPoints; i++) {
            int elementIndex = (i + startPoint) % numPoints;
            int nextElementIndex = (i + startPoint + 1) % numPoints;

            Vector3d bottomV1 = bottomPolygonPointsCW.get(elementIndex);
            Vector3d topV1 = topPolygonPointsCW.get(elementIndex);
            Vector3d bottomV2 = bottomPolygonPointsCW.get(nextElementIndex);
            Vector3d topV2 = topPolygonPointsCW.get(nextElementIndex);

            S surface = extrusionSurfaceDataProvider.getSurfaceCW(elementIndex);

            // Attention: In the object-to-texture-mapping model, the texture coords are part of the
            // 3D object/mesh and NOT part of the material. This means, the alignment of textures to surfaces
            // is sort of "static" to the object.
            // The texture coordinates calculation here is meant to calculate this static alignment of the texture to the
            // extruded object's borders.
            // To align a texture to an object on the basis of the texture's content, for example to create a good-looking
            // picture wallpaper, the texture image needs to match the given object's corner coordinates. It could make sense
            // to generate that texture image especially for the given object.
            // See also the continueSurfacesTextures part below; a surface texture might span multiple parts.
            Vector3d textureDirectionX = bottomV1.minus(bottomV2);
            Vector3d textureDirectionY = bottomV1.minus(topV1);
            TextureCoordinateSystem textureCoordinateSystem = TextureCoordinateSystem.create(
                textureDirectionX.crossed(textureDirectionY),
                textureDirectionX);
            TextureProjection textureProjection = TextureProjection.fromPointsBorder(textureCoordinateSystem,
                Arrays.asList(bottomV1, bottomV2, topV2, topV1));

            // A surface part spans from the current corner point to the next and thus represents a flat
            // rectangle for the final texture to be projected. In a simple situation,
            // we will have a new surface part per edge. But it is also possible to connect
            // succeeding surface parts together to make the final texture be projected to multiple
            // surface parts, being "buckled".
            // That situation is present if our data provider provides us the same surface value as in the last iteration.
            SurfacePart<S> part = new SurfacePart<>(surface, textureProjection);
            PropertyStorage connectionProperties = CSGSurfaceAwareAddon.createSurfacePartProperties(part);

            // Remember connected surfaces - for each surface, we remember a list of succeeding surface parts.
            surfaceProperties.computeIfAbsent(surface, s -> new ArrayList<>())
                .add(connectionProperties);

            List<Vector3d> pPoints = Arrays.asList(bottomV1, topV1, topV2, bottomV2);

            surfacePolygons.add(Polygon.fromPoints(pPoints, connectionProperties));
        }

        if (continueSurfaceTextures) {
            // Modify texture projections of connected surfaces in case surfaces span multiple parts.
            // This is the case e.g. if a side of a wall spans multiple segments (wall side, bevel, ...).
            for (Entry<S, List<PropertyStorage>> entry : surfaceProperties.entrySet()) {
                S surface = entry.getKey();
                List<PropertyStorage> connectedSurfaceProperties = entry.getValue();
                if (connectedSurfaceProperties.size() < 2) {
                    // Texture projection is already ok, no need to extend over multiple parts
                    continue;
                }

                // Calculate cumulative size of the area where the final texture should be projected
                double totalRangeX = 0;
                for (PropertyStorage sp : connectedSurfaceProperties) {
                    SurfacePart<S> part = CSGSurfaceAwareAddon.getSurfacePart(sp);
                    totalRangeX += part.getTextureProjection().getRangeTxy().getX();
                }

                // Calculate the sections of the texture which should be projected to each surface part (which
                // are represented by the connectedSurfaceProperties)
                double previousWidth = 0;
                for (PropertyStorage sp : connectedSurfaceProperties) {
                    SurfacePart<S> part = CSGSurfaceAwareAddon.getSurfacePart(sp);
                    TextureProjection textureProjection = part.getTextureProjection();
                    double currentWidth = textureProjection.getRangeTxy().getX();
                    textureProjection = textureProjection.extend(previousWidth, 0, totalRangeX - currentWidth, 0);
                    SurfacePart<S> newSurfacePart = new SurfacePart<>(surface, textureProjection);
                    CSGSurfaceAwareAddon.markSurfacePart(sp, newSurfacePart);

                    previousWidth += currentWidth;
                }
            }
        }

        return CSG.fromPolygons(surfacePolygons);
    }
}
