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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import de.dh.utils.Vector2D;
import de.dh.utils.io.MeshData;
import eu.mihosoft.jcsg.CSG;
import eu.mihosoft.jcsg.Polygon;
import eu.mihosoft.jcsg.PropertyStorage;
import eu.mihosoft.jcsg.ext.org.poly2tri.PolygonUtil;
import eu.mihosoft.vvecmath.Vector3d;

/**
 * CSG object which knows about it's different sides/surfaces. This information is maintained during all
 * CSG operations and is used at the end to be able to create separate mesh objects for each surface.
 *
 * @param <S> Type of the surface identificator used in this class. For simple objects, this can be a
 * string (like {@code "top"}, {@code "bottom"} etc.) but you could also use an enum for a more sophisticated
 * handling in the client code like
 * <pre><code>
 * enum WallSurface {
 *   A,
 *   B,
 *   One,
 *   Two,
 *   Top,
 *   Bottom,
 *   Embrasure;
 * }
 * </code></pre>
 */
public class SurfaceAwareCSG<S> extends CSG {
    protected static final String KEY_SURFACE_PART = "surface-part";

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

    /**
     * Descriptor for a single, plain part of a surface.
     * Contains a unique id for the surface part, the surface id and the information how to apply/map the final texture to this plain part.
     * At the beginning, a surface part typically is one simple, plane side of the object. In that case, the polygons of that face for a continuous area.
     * But during the CSG operations, a surface part might get interrupted by cutouts. In that case, the remaining polygons will still hold the same
     * part of the texture as before the cut out operation. This behavior is achieved by using and storing the same {@link TextureProjection} in each
     * of the polygons of a surface part.
     */
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
     * Extrudes a given (bottom) polygon specified by a border path up to another similar given (top) polygon with the same number
     * of edges, marking top, bottom and all extruded side faces internally with surface markers.
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
     * each surface can be covered with a separate texture at the end.
     *
     * During the extrusion process, data about the relative polygon location in a surface is collected and stored in the
     * polygon's properties for later generating the mesh objects in method {@link #createJavaFXTrinagleMesh(Function)}.
     *
     * @param extrusionSurfaceDataProvider Provider for the data needed during the creation of the resulting CSG.
     * @param startPoint Index of a path point of the top- and bottom polygons provided by the surface data provider,
     * which is a starting point of one surface. This is to control the starting point of the first surface texture.
     * For single-surface objects, this is the starting point where the (single) surface texture begins.
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
        List<Vector3d> bottomPolygonPointsReversed = new ArrayList<>(bottomPolygonPointsCCW);
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

        Map<S, List<PropertyStorage>> surfaceProperties = new HashMap<>();
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
            // picture wallpaper, the texture image needs to matche the given object's corner coordinates. It could make sense
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
            PropertyStorage connectionProperties = createSurfacePartProperties(part);

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
                    SurfacePart<S> part = getSurfacePart(sp);
                    totalRangeX += part.getTextureProjection().getRangeTxy().getX();
                }

                // Calculate the sections of the texture which should be projected to each surface part (which
                // are represented by the connectedSurfaceProperties)
                double previousWidth = 0;
                for (PropertyStorage sp : connectedSurfaceProperties) {
                    SurfacePart<S> part = getSurfacePart(sp);
                    TextureProjection textureProjection = part.getTextureProjection();
                    double currentWidth = textureProjection.getRangeTxy().getX();
                    textureProjection = textureProjection.extend(previousWidth, 0, totalRangeX - currentWidth, 0);
                    SurfacePart<S> newSurfacePart = new SurfacePart<>(surface, textureProjection);
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

    /**
     * Provides metadata for a given surface which is used in the mesh data generated for that surface.
     */
    public interface ISurfaceDataProvider<S> {
        String getSurfaceId(S surface);

        String getSurfaceName(S surface);

        default Collection<String> getGroups(S surface) {
            return Collections.emptyList();
        }
    }

    public Map<S, MeshData> createMeshes(ISurfaceDataProvider<S> surfaceDataProvider) {
        return createMeshes(Optional.of(surfaceDataProvider));
    }

    /**
     * Exports this CSG object to format-independent {@link MeshData} objects, one mesh per surface.
     */
    public Map<S, MeshData> createMeshes(Optional<ISurfaceDataProvider<S>> oSurfaceDataProvider) {
        Map<S, MeshData> result = new HashMap<>();
        for (Polygon p : mPolygons) {
            SurfacePart<S> surfacePart = getSurfacePart(p.getStorage());

            S surface = surfacePart.getSurface();

            MeshData current = result.computeIfAbsent(surface, s -> new MeshData(oSurfaceDataProvider.map(sdp -> sdp.getSurfaceId(surface)).orElse(null),
                oSurfaceDataProvider.map(sdp -> sdp.getSurfaceName(surface)).orElse(null),
                oSurfaceDataProvider.map(sdp -> sdp.getGroups(surface)).orElse(null),
                new ArrayList<>(), // Vertices
                new ArrayList<>(), // Uvs
                new ArrayList<>(), // Faces
                new ArrayList<>(), // SmoothingGroups
                Optional.empty(), // FaceNormalsData
                null)); // Material name

            List<Float> points = current.getVertices();
            List<Float> texCoords = current.getTexCoords();
            List<Integer> faces = current.getFaces();

            if (p.vertices.size() < 3) {
                // Ignore polygon
            }
            TextureProjection textureProjection = surfacePart.getTextureProjection();

            Vector3d pos1 = p.vertices.get(0).pos;
            Vector2D v1UV = textureProjection.getTextureCoordinates(pos1);

            for (int i = 0; i < p.vertices.size() - 2; i++) {
                points.addAll(Arrays.asList(
                    (float) pos1.x(),
                    (float) pos1.y(),
                    (float) pos1.z()));

                texCoords.addAll(Arrays.asList(
                    (float) v1UV.getX(),
                    (float) v1UV.getY()));
                int t0 = texCoords.size() / 2 - 1;

                Vector3d pos2 = p.vertices.get(i + 1).pos;
                Vector2D v2UV = textureProjection.getTextureCoordinates(pos2);

                points.addAll(Arrays.asList(
                    (float) pos2.x(),
                    (float) pos2.y(),
                    (float) pos2.z()));

                texCoords.addAll(Arrays.asList(
                    (float) v2UV.getX(),
                    (float) v2UV.getY()));
                int t1 = texCoords.size() / 2 - 1;

                Vector3d pos3 = p.vertices.get(i + 2).pos;
                Vector2D v3UV = textureProjection.getTextureCoordinates(pos3);

                points.addAll(Arrays.asList(
                    (float) pos3.x(),
                    (float) pos3.y(),
                    (float) pos3.z()));

                texCoords.addAll(Arrays.asList(
                    (float) v3UV.getX(),
                    (float) v3UV.getY()));
                int t2 = texCoords.size() / 2 - 1;

                int vertexCount = faces.size() / 2;
                faces.addAll(Arrays.asList(
                    vertexCount, // first vertex
                    t0,
                    vertexCount + 1, // second vertex
                    t1,
                    vertexCount + 2, // third vertex
                    t2
                ));
            } // end for vertex
        } // end for polygon

        return result;
    }

    public static <S> void markSurfacePart(PropertyStorage properties, SurfacePart<S> surfacePart) {
        properties.set(KEY_SURFACE_PART, surfacePart);
    }

    @SuppressWarnings("unchecked")
    public static <S> SurfacePart<S> getSurfacePart(PropertyStorage properties) {
        return (SurfacePart<S>) properties.getValue(KEY_SURFACE_PART).orElse(null);
    }

    public static <S> PropertyStorage createSurfacePartProperties(SurfacePart<S> surfacePart) {
        PropertyStorage result = new PropertyStorage();
        markSurfacePart(result, surfacePart);
        return result;
    }
}
