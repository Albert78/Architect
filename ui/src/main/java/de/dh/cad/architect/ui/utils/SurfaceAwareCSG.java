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
package de.dh.cad.architect.ui.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;

import de.dh.utils.fx.Vector2D;
import eu.mihosoft.jcsg.CSG;
import eu.mihosoft.jcsg.Polygon;
import eu.mihosoft.jcsg.PropertyStorage;
import eu.mihosoft.jcsg.ext.org.poly2tri.PolygonUtil;
import eu.mihosoft.vvecmath.Vector3d;
import javafx.collections.ObservableFloatArray;
import javafx.scene.shape.Mesh;
import javafx.scene.shape.ObservableFaceArray;
import javafx.scene.shape.TriangleMesh;

public class SurfaceAwareCSG<S> extends CSG {
    public static void main(String[] args) throws Exception {
        Vector3d x1;

        // Test:
        // Normale, Textur-X-Koordinate und (implizite) Textur-Y-Koordinate bilden ein LHS
        // Alle Tests müssen ungefähr (1, 2) liefern

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

    /**
     * Class which provides all data for a surface aware CSG extruded from a base polygon.
     */
    public interface ExtrusionSurfaceDataProvider<S> {
        /**
         * Returns a list of corner points of the bottom surface.
         * At the moment, we only support convex or concave polygons without holes or intersections,
         * given in counter-clockwise direction. The polygon plane must not be orthogonal to the X/Y plane
         * because of the limitations of the extrusion function.
         */
        List<Vector3d> getBottomPolygonPointsCCW();

        /**
         * Returns a list of polygon points for the top surface which represent the same corners as the bottom
         * polygon points. The point ordering must be the same as for {@link #getBottomPolygonPointsCCW() the bottom points},
         * i.e. each bottom point must have a corresponding point in the result of this method.
         */
        List<Vector3d> getConnectedTopPolygonPointsCCW();

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
            return TextureProjection.fromPointsBorder(getTopPolygonTextureCoordinateSystem(), getConnectedTopPolygonPointsCCW());
        }

        default TextureProjection getBottomPolygonTextureProjection() {
            return TextureProjection.fromPointsBorder(getBottomPolygonTextureCoordinateSystem(), getBottomPolygonPointsCCW());
        }

        /**
         * Gets the surface of the connection between the point of the given start index and the next point in the list of polygon points.
         */
        S getSurfaceCCW(int startPointIndex);

        S getTopSurface();
        S getBottomSurface();
    }

    public static class SurfacePart<S> {
        protected final String mId;
        protected final S mSurface;
        protected final TextureProjection mTextureProjection;

        public SurfacePart(S surface, TextureProjection textureProjection) {
            mId = UUID.randomUUID().toString();
            mSurface = surface;
            mTextureProjection = textureProjection;
        }

        public String getId() {
            return mId;
        }

        public S getSurface() {
            return mSurface;
        }

        public TextureProjection getTextureProjection() {
            return mTextureProjection;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((mId == null) ? 0 : mId.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            SurfacePart<?> other = (SurfacePart<?>) obj;
            if (!mId.equals(other.mId))
                return false;
            return true;
        }
    }

    public SurfaceAwareCSG(List<Polygon> polygons) {
        mPolygons = polygons;
    }

    /**
     * Extrudes a specified path between two similar polygons, marking all polygons internally with surface markers.
     * The extruded polygon will consist of N surfaces, one for the top, one for the bottom and
     * an arbitrary count of surfaces for the borders, each including a consecutive list of extruded base edges.
     * Each surface can be covered with a separate texture at the end.
     * During the extrusion process, data about the relative polygon location in the overall structure is collected
     * and preserved for generating the mesh objects using method {@link #createJavaFXTrinagleMesh(Function)}.
     *
     * @param extrusionSurfaceDataProvider Provider for the data needed during the creation of the resulting CSG.
     * @param startPoint Index of a polygon point of the top- and bottom polygons which is a starting point
     * of a surface. This is to control the starting point of the first surface texture. For single-surface objects,
     * this is the starting point where the (single) surface texture begins.
     *
     * @return A surface aware CSG object with the desired surface mappings.
     */
    public static <T, S> SurfaceAwareCSG<S> extrudeSurfaces(ExtrusionSurfaceDataProvider<S> extrusionSurfaceDataProvider, int startPoint, boolean continueSurfaceTextures) {
        // Top
        S topSurface = extrusionSurfaceDataProvider.getTopSurface();
        SurfacePart<S> topPart = new SurfacePart<>(topSurface, extrusionSurfaceDataProvider.getTopPolygonTextureProjection());
        PropertyStorage topProperties = createSurfacePartProperties(topPart);

        // Bottom
        S bottomSurface = extrusionSurfaceDataProvider.getBottomSurface();
        SurfacePart<S> bottomPart = new SurfacePart<>(bottomSurface, extrusionSurfaceDataProvider.getBottomPolygonTextureProjection());
        PropertyStorage bottomProperties = createSurfacePartProperties(bottomPart);

        List<Vector3d> bottomPolygonPointsCCW = extrusionSurfaceDataProvider.getBottomPolygonPointsCCW();
        List<Vector3d> connectedTopPolygonPoints = extrusionSurfaceDataProvider.getConnectedTopPolygonPointsCCW();
        List<Vector3d> bottomPolygonPointsReversed = new ArrayList<>(extrusionSurfaceDataProvider.getBottomPolygonPointsCCW());
        Collections.reverse(bottomPolygonPointsReversed); // Turn points clockwise to make upper surface point to the outside

        List<Polygon> surfacePolygons = new ArrayList<>();

        // Calculate top polygons
        List<Polygon> topConvexCCW = PolygonUtil.concaveToConvex(Polygon.fromPoints(PolygonUtil.cleanupPolygonPoints(connectedTopPolygonPoints)));
        for (Polygon polygon : topConvexCCW) {
            polygon.setStorage(topProperties);
        }
        surfacePolygons.addAll(topConvexCCW);

        // Calculate bottom polygons
        List<Polygon> bottomConvexCW = PolygonUtil.concaveToConvex(Polygon.fromPoints(PolygonUtil.cleanupPolygonPoints(bottomPolygonPointsReversed)));
        for (Polygon polygon : bottomConvexCW) {
            polygon.setStorage(bottomProperties);
        }
        surfacePolygons.addAll(bottomConvexCW);

        Map<S, List<PropertyStorage>> surfaceProperties = new TreeMap<>();
        int numPoints = bottomPolygonPointsCCW.size();
        for (int i = 0; i < numPoints; i++) {
            int elementIndex = (i + startPoint) % numPoints;
            int nextElementIndex = (i + startPoint + 1) % numPoints;

            Vector3d bottomV1 = bottomPolygonPointsCCW.get(elementIndex);
            Vector3d topV1 = connectedTopPolygonPoints.get(elementIndex);
            Vector3d bottomV2 = bottomPolygonPointsCCW.get(nextElementIndex);
            Vector3d topV2 = connectedTopPolygonPoints.get(nextElementIndex);

            S surface = extrusionSurfaceDataProvider.getSurfaceCCW(elementIndex);

            // Attention: In the object-to-texture-mapping model, the texture coords are part of the
            // 3D object/mesh and NOT part of the material. This means, the alignment of textures to surfaces
            // is sort of "static" to the object.
            // The texture coordinates calculation here is meant to do this static alignment of the texture to the
            // extruded object's borders.
            // To align a texture to an object on the basis of the texture's content, for example to create a good-looking
            // picture wallpaper, the texture image needs to be generated to match the given object's texture coordinates.
            // See also the continueSurfacesTextures part below; a surface texture might span multiple parts.
            Vector3d textureDirectionX = bottomV1.minus(bottomV2);
            Vector3d textureDirectionY = bottomV1.minus(topV1);
            TextureCoordinateSystem textureCoordinateSystem = TextureCoordinateSystem.create(
                textureDirectionX.crossed(textureDirectionY),
                textureDirectionX);
            TextureProjection textureProjection = TextureProjection.fromPointsBorder(textureCoordinateSystem,
                Arrays.asList(bottomV1, bottomV2, topV2, topV1));

            SurfacePart<S> part = new SurfacePart<>(surface, textureProjection);
            PropertyStorage connectionProperties = createSurfacePartProperties(part);

            // Remember connected surfaces
            surfaceProperties.computeIfAbsent(surface, s -> new ArrayList<>())
                .add(connectionProperties);

            List<Vector3d> pPoints = Arrays.asList(bottomV1, topV1, topV2, bottomV2);

            surfacePolygons.add(Polygon.fromPoints(pPoints, connectionProperties));
        }

        if (continueSurfaceTextures) {
            // Modify texture projections of connected surfaces in case surfaces span multiple parts.
            // This is the case e.g. if a side of a wall spans multiple segments (wall side, bevel, ...).
            for (List<PropertyStorage> connectedSurfaceProperties : surfaceProperties.values()) {
                if (connectedSurfaceProperties.size() < 2) {
                    // Texture projection is already ok, no need to extend over multiple parts
                    continue;
                }

                double commonRangeX = 0;
                for (PropertyStorage sp : connectedSurfaceProperties) {
                    SurfacePart<S> part = getSurfacePart(sp);
                    commonRangeX += part.getTextureProjection().getRangeTxy().getX();
                }

                double previousWidth = 0;
                for (PropertyStorage sp : connectedSurfaceProperties) {
                    SurfacePart<S> part = getSurfacePart(sp);
                    TextureProjection textureProjection = part.getTextureProjection();
                    double currentWidth = textureProjection.getRangeTxy().getX();
                    textureProjection = textureProjection.extend(previousWidth, 0, commonRangeX - currentWidth, 0);
                    SurfacePart<S> newSurfacePart = new SurfacePart<>(part.getSurface(), textureProjection);
                    markSurfacePart(sp, newSurfacePart);

                    previousWidth += currentWidth;
                }
            }
        }

        return new SurfaceAwareCSG<>(surfacePolygons);
    }

    public SurfaceAwareCSG<S> difference(SurfaceAwareCSG<S> csg) {
        CSG diff = super.difference(csg);
        return new SurfaceAwareCSG<>(diff.getPolygons());
    }

    public SurfaceAwareCSG<S> union(SurfaceAwareCSG<S> csg) {
        CSG union = super.union(csg);
        return new SurfaceAwareCSG<>(union.getPolygons());
    }

    public static class ShapeSurfaceData<S> {
        protected final Mesh mMesh;
        protected final Collection<SurfacePart<S>> mSurfaceParts;

        public ShapeSurfaceData(Mesh mesh, Collection<SurfacePart<S>> surfacePart) {
            mMesh = mesh;
            mSurfaceParts = surfacePart;
        }

        public Mesh getMesh() {
            return mMesh;
        }

        /**
         * Gets the parts of this surface, the order of the parts is not defined.
         */
        public Collection<SurfacePart<S>> getSurfaceParts() {
            return mSurfaceParts;
        }
    }

    public ShapeSurfaceData<S> createJavaFXTrinagleMesh(S surface) {
        Collection<SurfacePart<S>> parts = new ArrayList<>();
        TriangleMesh mesh = new TriangleMesh();
        ObservableFloatArray points = mesh.getPoints();
        ObservableFloatArray texCoords = mesh.getTexCoords();
        ObservableFaceArray faces = mesh.getFaces();
        int vertexCount = 0;
        for (Polygon p : mPolygons) {
            SurfacePart<S> surfacePart = getSurfacePart(p.getStorage());
            if (!surface.equals(surfacePart.getSurface())) {
                continue;
            }
            parts.add(surfacePart);
            if (p.vertices.size() < 3) {
                // Ignore polygon
            }
            TextureProjection textureProjection = surfacePart.getTextureProjection();

            Vector3d pos1 = p.vertices.get(0).pos;
            Vector2D v1UV = textureProjection.getTextureCoordinates(pos1);

            for (int i = 0; i < p.vertices.size() - 2; i++) {
                points.addAll(
                    (float) pos1.x(),
                    (float) pos1.y(),
                    (float) pos1.z());

                texCoords.addAll(
                    (float) v1UV.getX(),
                    (float) v1UV.getY());
                int t0 = texCoords.size() / 2 - 1;

                Vector3d pos2 = p.vertices.get(i + 1).pos;
                Vector2D v2UV = textureProjection.getTextureCoordinates(pos2);

                points.addAll(
                    (float) pos2.x(),
                    (float) pos2.y(),
                    (float) pos2.z());

                texCoords.addAll(
                    (float) v2UV.getX(),
                    (float) v2UV.getY());
                int t1 = texCoords.size() / 2 - 1;

                Vector3d pos3 = p.vertices.get(i + 2).pos;
                Vector2D v3UV = textureProjection.getTextureCoordinates(pos3);

                points.addAll(
                    (float) pos3.x(),
                    (float) pos3.y(),
                    (float) pos3.z());

                texCoords.addAll(
                    (float) v3UV.getX(),
                    (float) v3UV.getY());
                int t2 = texCoords.size() / 2 - 1;

                faces.addAll(
                    vertexCount, // first vertex
                    t0,
                    vertexCount + 1, // second vertex
                    t1,
                    vertexCount + 2, // third vertex
                    t2
                );
                vertexCount += 3;
            } // end for vertex
        } // end for polygon

        return new ShapeSurfaceData<>(mesh, parts);
    }

    public static <S> void markSurfacePart(PropertyStorage properties, SurfacePart<S> surfacePart) {
        properties.set("surface-part", surfacePart);
    }

    @SuppressWarnings("unchecked")
    public static <S> SurfacePart<S> getSurfacePart(PropertyStorage properties) {
        return (SurfacePart<S>) properties.getValue("surface-part").orElse(null);
    }

    public static <S> PropertyStorage createSurfacePartProperties(SurfacePart<S> surfacePart) {
        PropertyStorage result = new PropertyStorage();
        markSurfacePart(result, surfacePart);
        return result;
    }
}
