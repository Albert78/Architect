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
package de.dh.utils.fx.io.formats.obj;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dh.cad.architect.utils.Namespace;
import de.dh.cad.architect.utils.vfs.IDirectoryLocator;
import de.dh.cad.architect.utils.vfs.IResourceLocator;
import de.dh.utils.fx.io.utils.FloatArrayList;
import de.dh.utils.fx.io.utils.IntegerArrayList;

/**
 * Obj file reader creating {@link ObjData} and {@link RawMaterialData} objects.
 */
public class ObjReader {
    protected static final String DEFAULT_MESH_NAME = "Mesh";

    /**
     * Builder class which collects mesh data until a new mesh data object can be created.
     */
    protected static class MeshDataBuilder {
        protected final Namespace<Void> mNamespace = new Namespace<>(); // Used to create unique names when object name is not defined
        protected final Namespace<Void> mIdNamespace = new Namespace<>(); // Used to create stable ids

        protected IntegerArrayList mFaces = new IntegerArrayList();
        protected IntegerArrayList mFaceNormals = new IntegerArrayList();
        protected IntegerArrayList mSmoothingGroups = new IntegerArrayList();

        protected int mCurrentSmoothGroup;
        protected String mName;
        protected Collection<String> mGroups;
        protected String mMaterialName;

        public MeshDataBuilder() {
            reset();
        }

        /**
         * Resets the mesh data builder to be used for a new mesh.
         */
        public void reset() {
            mFaces.clear();
            mFaceNormals.clear();
            mSmoothingGroups.clear();

            mCurrentSmoothGroup = 0;
            mName = null;
            mGroups = new ArrayList<>();
        }

        public String getName() {
            return mName;
        }

        /**
         * Sets the name of the current mesh object to build.
         */
        public void setName(String value) {
            mName = value;
        }

        public Collection<String> getGroups() {
            return mGroups;
        }

        public void setGroups(Collection<String> value) {
            mGroups = new ArrayList<>(value);
        }

        public String getMaterialName() {
            return mMaterialName;
        }

        public void setMaterialName(String value) {
            mMaterialName = value;
        }

        public void addFace(int v1, int uv1, int v2, int uv2, int v3, int uv3) {
            mFaces.add(v1);
            mFaces.add(uv1);
            mFaces.add(v2);
            mFaces.add(uv2);
            mFaces.add(v3);
            mFaces.add(uv3);
        }

        public void addFaceNormal(int n1, int n2, int n3) {
            mFaceNormals.add(n1);
            mFaceNormals.add(n2);
            mFaceNormals.add(n3);
        }

        public int getCurrentSmoothGroup() {
            return mCurrentSmoothGroup;
        }

        public void setCurrentSmoothGroup(int value) {
            mCurrentSmoothGroup = value;
        }

        /**
         * Adds the current smoothing group to the list of smoothing groups.
         */
        public void addSmoothingGroup() {
            mSmoothingGroups.add(mCurrentSmoothGroup);
        }

        public Optional<Pair<MeshData, String>> tryFinishMesh(FloatArrayList vertices, FloatArrayList uvs, FloatArrayList normals) {
            if (mFaces.isEmpty()) {
                return Optional.empty();
            }

            Map<Integer, Integer> vertexMap = new HashMap<>(vertices.size() / 2);
            Map<Integer, Integer> uvMap = new HashMap<>(uvs.size() / 2);
            Map<Integer, Integer> normalMap = new HashMap<>(normals.size() / 2);
            FloatArrayList newVertices = new FloatArrayList(vertices.size() / 2);
            FloatArrayList newUVs = new FloatArrayList(uvs.size() / 2);
            FloatArrayList newNormals = new FloatArrayList(normals.size() / 2);
            boolean useNormals = true;

            for (int i = 0; i < mFaces.size(); i += 2) {
                // The current vertex indices in the faces array point to vertex positions in the "big", common vertex list.
                // We extract vertices which are used by the faces of the current mesh to a new, smaller vertex list and
                // rewrite the indices in the faces list.
                int vi = mFaces.get(i);
                Integer nvi = vertexMap.get(vi);
                if (nvi == null) {
                    nvi = newVertices.size() / 3;
                    vertexMap.put(vi, nvi);
                    newVertices.add(vertices.get(vi * 3));
                    newVertices.add(vertices.get(vi * 3 + 1));
                    newVertices.add(vertices.get(vi * 3 + 2));
                }
                mFaces.set(i, nvi);

                // The same for UV indices
                int uvi = mFaces.get(i + 1);
                Integer nuvi = uvMap.get(uvi);
                if (nuvi == null) {
                    nuvi = newUVs.size() / 2;
                    uvMap.put(uvi, nuvi);
                    if (uvi >= 0) {
                        newUVs.add(uvs.get(uvi * 2));
                        newUVs.add(uvs.get(uvi * 2 + 1));
                    } else {
                        newUVs.add(0f);
                        newUVs.add(0f);
                    }
                }
                mFaces.set(i + 1, nuvi);

                // The same for face normals
                if (useNormals) {
                    int ni = mFaceNormals.get(i / 2);
                    Integer nni = normalMap.get(ni);
                    if (nni == null) {
                        nni = newNormals.size() / 3;
                        normalMap.put(ni, nni);
                        if (ni >= 0 && normals.size() >= (ni + 1) * 3) {
                            newNormals.add(normals.get(ni * 3));
                            newNormals.add(normals.get(ni * 3 + 1));
                            newNormals.add(normals.get(ni * 3 + 2));
                        } else {
                            useNormals = false;
                            newNormals.add(0f);
                            newNormals.add(0f);
                            newNormals.add(0f);
                        }
                    }
                    mFaceNormals.set(i / 2, nni);
                }
            }

            String id = mName;
            if (id == null && !mGroups.isEmpty()) {
                id = mGroups.iterator().next();
            }
            if (id == null) {
                id = DEFAULT_MESH_NAME;
            }
            if (mIdNamespace.contains(id)) {
                id = mIdNamespace.generateName(id);
            }
            mIdNamespace.add(id, null);

            // We separate name and id by design; name is not necessarily unique but id is.
            // The code is redundant by design - the id generation code must remain stable while the code to generate the name may change.

            String name = mName;
            if (name == null && !mGroups.isEmpty()) {
                name = mGroups.iterator().next();
            }
            if (name == null) {
                name = DEFAULT_MESH_NAME;
            }
            if (mNamespace.contains(name)) {
                name = mNamespace.generateName(name);
            }
            mNamespace.add(name, null);

            String materialName = mMaterialName;
            MeshData result = new MeshData(id, name, mGroups, newVertices, newUVs,
                new IntegerArrayList(mFaces), new IntegerArrayList(mSmoothingGroups),
                useNormals ? Optional.of(new MeshData.FaceNormalsData(newNormals, new IntegerArrayList(mFaceNormals))) : Optional.empty(),
                materialName);
            reset();

            // The reset() call above did reset all fields, so only use copies of fields here!
            return Optional.of(Pair.of(result, materialName));
        }
    }

    private static final Logger log = LoggerFactory.getLogger(ObjReader.class);

    protected static int vertexIndex(int vertexIndex, int numVertices) {
        if (vertexIndex < 0) {
            return vertexIndex + numVertices / 3;
        } else {
            return vertexIndex - 1;
        }
    }

    protected static int uvIndex(int uvIndex, int numUvs) {
        if (uvIndex < 0) {
            return uvIndex + numUvs / 2;
        } else {
            return uvIndex - 1;
        }
    }

    protected static int normalIndex(int normalIndex, int numNormals) {
        if (normalIndex < 0) {
            return normalIndex + numNormals / 3;
        } else {
            return normalIndex - 1;
        }
    }

    protected static boolean mFlatXZ = false;

    public static void setFlatXZ(boolean flatXZ) {
        ObjReader.mFlatXZ = flatXZ;
    }

    public static ObjData readObj(IResourceLocator resourceLocator) throws IOException {
        return readObj(resourceLocator, Collections.emptyMap());
    }

    /**
     * Reads an {@code .obj} file from the given resource.
     * Note that the generated {@link ObjData} will always yield the same {@link MeshData#getId() mesh ids} and {@link MeshData#getName() names}
     * among different calls for the same object file.
     */
    public static ObjData readObj(IResourceLocator objFileLocator, Map<String, RawMaterialData> defaultMaterials) throws IOException {
        IDirectoryLocator basePath = objFileLocator.getParentDirectory();
        Map<String, RawMaterialData> materialLibrary = new TreeMap<>(defaultMaterials);

        log.debug("Reading object file " + objFileLocator);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(objFileLocator.inputStream()))) {
            Collection<Pair<MeshData, String>> meshesWithMaterial = new ArrayList<>();

            // Those lists grow over the reading process and span all meshes;
            // the generated meshes are independent from each other and don't share
            // those values, so each mesh gets its own modified copy of those lists
            FloatArrayList vertices = new FloatArrayList();
            FloatArrayList uvs = new FloatArrayList();
            FloatArrayList normals = new FloatArrayList();

            // Those are mesh specific and are cleared for each new mesh
            MeshDataBuilder meshBuilder = new MeshDataBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                try {
                    if (line.isEmpty() || line.startsWith("#")) {
                        // Comments and empty lines are ignored
                    } else if (line.startsWith("o ") || line.equals("o")) {
                        // A new object name finishes any already started mesh - will be a noop if we have no new mesh data yet
                        meshBuilder.tryFinishMesh(vertices, uvs, normals)
                            .ifPresent(meshAndMaterial -> meshesWithMaterial.add(meshAndMaterial));
                        if (line.length() > 2) {
                            meshBuilder.setName(line.substring(2));
                        }
                    } else if (line.startsWith("g ") || line.equals("g")) {
                        meshBuilder.tryFinishMesh(vertices, uvs, normals)
                            .ifPresent(meshAndMaterial -> meshesWithMaterial.add(meshAndMaterial));
                        if (line.length() > 2) {
                            String groupsStr = line.substring(2);
                            meshBuilder.setGroups(Arrays.asList(groupsStr.split(" +")));
                        }
                    } else if (line.startsWith("usemtl ")) {
                        meshBuilder.tryFinishMesh(vertices, uvs, normals)
                            .ifPresent(meshAndMaterial -> meshesWithMaterial.add(meshAndMaterial));
                        // Setting new material for next mesh
                        meshBuilder.setMaterialName(line.substring(7).trim());
                    } else if (line.startsWith("mtllib ")) {
                        // setting materials lib
                        String[] split = line.substring(7).trim().split(" +");

                        for (String filename : split) {
                            materialLibrary.putAll(MtlLibraryIO.readMaterialSet(basePath.resolveResource(filename)));
                        }
                    } else if (line.startsWith("v ")) {
                        String[] split = line.substring(2).trim().split(" +");
                        float x = Float.parseFloat(split[0]);
                        float y = Float.parseFloat(split[1]);
                        float z = Float.parseFloat(split[2]);
                        vertices.add(x);
                        vertices.add(y);
                        vertices.add(z);

                        if (mFlatXZ) {
                            uvs.add(x);
                            uvs.add(z);
                        }
                    } else if (line.startsWith("vt ")) {
                        String[] split = line.substring(3).trim().split(" +");
                        float u = Float.parseFloat(split[0]);
                        float v = Float.parseFloat(split[1]);
                        uvs.add(u);
                        uvs.add(1 - v);
                    } else if (line.startsWith("f ")) {
                        String[] split = line.substring(2).trim().split(" +");
                        int[][] data = new int[split.length][];
                        boolean uvProvided = true;
                        boolean normalProvided = true;
                        for (int i = 0; i < split.length; i++) {
                            String[] split2 = split[i].split("/");
                            if (split2.length < 2) {
                                uvProvided = false;
                            }
                            if (split2.length < 3) {
                                normalProvided = false;
                            }
                            data[i] = new int[split2.length];
                            for (int j = 0; j < split2.length; j++) {
                                if (split2[j].length() == 0) {
                                    data[i][j] = 0;
                                    if (j == 1) {
                                        uvProvided = false;
                                    }
                                    if (j == 2) {
                                        normalProvided = false;
                                    }
                                } else {
                                    data[i][j] = Integer.parseInt(split2[j]);
                                }
                            }
                        }
                        int numVertices = vertices.size();
                        int numUvs = uvs.size();
                        int v1 = vertexIndex(data[0][0], numVertices);
                        int uv1 = -1;
                        int n1 = -1;
                        if (uvProvided && !mFlatXZ) {
                            uv1 = uvIndex(data[0][1], numUvs);
                            if (uv1 < 0) {
                                uvProvided = false;
                            }
                        }
                        int numNormals = normals.size();
                        if (normalProvided) {
                            n1 = normalIndex(data[0][2], numNormals);
                            if (n1 < 0) {
                                normalProvided = false;
                            }
                        }
                        for (int i = 1; i < data.length - 1; i++) {
                            int v2 = vertexIndex(data[i][0], numVertices);
                            int v3 = vertexIndex(data[i + 1][0], numVertices);
                            int uv2 = -1;
                            int uv3 = -1;
                            int n2 = -1;
                            int n3 = -1;
                            if (uvProvided && !mFlatXZ) {
                                uv2 = uvIndex(data[i][1], numUvs);
                                uv3 = uvIndex(data[i + 1][1], numUvs);
                            }
                            if (normalProvided) {
                                n2 = normalIndex(data[i][2], numNormals);
                                n3 = normalIndex(data[i + 1][2], numNormals);
                            }
                            meshBuilder.addFace(v1, uv1,
                                v2, uv2,
                                v3, uv3);
                            meshBuilder.addFaceNormal(n1, n2, n3);

                            meshBuilder.addSmoothingGroup();
                        }
                    } else if (line.startsWith("s ")) {
                        if (line.substring(2).equals("off")) {
                            meshBuilder.setCurrentSmoothGroup(0);
                        } else {
                            meshBuilder.setCurrentSmoothGroup(Integer.parseInt(line.substring(2)));
                        }
                    } else if (line.startsWith("vn ")) {
                        String[] split = line.substring(2).trim().split(" +");
                        float x = Float.parseFloat(split[0]);
                        float y = Float.parseFloat(split[1]);
                        float z = Float.parseFloat(split[2]);
                        normals.add(x);
                        normals.add(y);
                        normals.add(z);
                    } else {
                        log.warn("Line skipped: " + line);
                    }
                } catch (Exception ex) {
                    log.error("Failed to parse line: " + line, ex);
                }
            }
            meshBuilder.tryFinishMesh(vertices, uvs, normals)
                .ifPresent(meshAndMaterial -> meshesWithMaterial.add(meshAndMaterial));

            return ObjData.of(meshesWithMaterial, materialLibrary);
        }
    }

    public static Collection<IResourceLocator> getAllFiles(IResourceLocator objFileLocator) throws IOException {
        Collection<IResourceLocator> result = new ArrayList<>();
        result.add(objFileLocator);
        IDirectoryLocator baseDirectoryLocator = objFileLocator.getParentDirectory();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(objFileLocator.inputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("mtllib ")) {
                    String[] split = line.substring("mtllib ".length()).trim().split(" +");

                    for (String fileName : split) {
                        result.addAll(MtlLibraryIO.getAllFiles(baseDirectoryLocator.resolveResource(fileName)));
                    }
                }
            }
        }
        return result;
    }
}
