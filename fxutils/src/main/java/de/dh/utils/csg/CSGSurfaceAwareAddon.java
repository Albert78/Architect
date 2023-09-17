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
import java.util.Optional;
import java.util.UUID;

import de.dh.utils.Vector2D;
import de.dh.utils.io.MeshData;
import eu.mihosoft.jcsg.CSG;
import eu.mihosoft.jcsg.Polygon;
import eu.mihosoft.jcsg.PropertyStorage;
import eu.mihosoft.vvecmath.Vector3d;

/**
 * CSG utility methods to make a CSG know about it's different sides/surfaces. This information is maintained during all
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
public class CSGSurfaceAwareAddon {
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

    protected static final String KEY_SURFACE_PART = "surface-part";

    /**
     * Provides metadata for a given surface which is used in the mesh data generated for that surface.
     */
    public interface ISurfaceDataProvider<S> {
        String getSurfaceName(S surface);

        default Collection<String> getGroups(S surface) {
            return Collections.emptyList();
        }
    }

    public static <S> Map<S, MeshData> createMeshes(CSG csg, ISurfaceDataProvider<S> surfaceDataProvider) {
        return createMeshes(csg, Optional.of(surfaceDataProvider));
    }

    /**
     * Exports this CSG object to format-independent {@link MeshData} objects, one mesh per surface.
     */
    public static <S> Map<S, MeshData> createMeshes(CSG csg, Optional<ISurfaceDataProvider<S>> oSurfaceDataProvider) {
        Map<S, MeshData> result = new HashMap<>();
        for (Polygon p : csg.getPolygons()) {
            SurfacePart<S> surfacePart = getSurfacePart(p.getStorage());

            S surface = surfacePart.getSurface();

            MeshData current = result.computeIfAbsent(surface, s -> new MeshData(oSurfaceDataProvider.map(sdp -> sdp.getSurfaceName(surface)).orElse(null),
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
