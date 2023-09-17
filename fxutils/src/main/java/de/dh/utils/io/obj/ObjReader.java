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
package de.dh.utils.io.obj;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dh.cad.architect.utils.Namespace;
import de.dh.cad.architect.utils.vfs.IDirectoryLocator;
import de.dh.cad.architect.utils.vfs.IResourceLocator;
import de.dh.utils.io.MeshData;
import de.dh.utils.io.ObjData;

/**
 * Obj file reader creating {@link ObjData} and {@link RawMaterialData} objects.
 */
public class ObjReader {
    public static class ObjDataRaw {
        protected final Collection<MeshData> mMeshes; // Mesh names must be unique
        protected final Map<String, String> mMeshNamesToMaterialNames; // Mesh names to material - not all mesh names need to have a mapping, in that case, value is null
        protected final Collection<String> mUsedMaterialLibraries;

        public ObjDataRaw(Collection<MeshData> meshes, Map<String, String> meshNamesToMaterialNames, Collection<String> usedMaterialLibraries) {
            mMeshes = meshes;
            mMeshNamesToMaterialNames = meshNamesToMaterialNames;
            mUsedMaterialLibraries = usedMaterialLibraries;
        }

        public static ObjDataRaw of(Collection<Pair<MeshData, String>> meshesWithMaterial, Collection<String> usedMaterialLibraries) {
            Collection<MeshData> meshes = new ArrayList<>();
            Map<String, String> meshNamesToMaterialNames = new TreeMap<>();
            for (Pair<MeshData, String> pair : meshesWithMaterial) {
                MeshData mesh = pair.getLeft();
                String materialName = pair.getRight();
                String meshName = mesh.getName();
                meshes.add(mesh);
                meshNamesToMaterialNames.put(meshName, materialName);
            }
            return new ObjDataRaw(meshes, meshNamesToMaterialNames, usedMaterialLibraries);
        }

        public Collection<MeshData> getMeshes() {
            return mMeshes;
        }

        public Map<String, String> getMeshNamesToMaterialNames() {
            return mMeshNamesToMaterialNames;
        }

        public Collection<String> getUsedMaterialLibraries() {
            return mUsedMaterialLibraries;
        }
    }

    protected static final String DEFAULT_MESH_NAME = "Mesh";

    /**
     * Builder class which collects mesh data until a new mesh data object can be created.
     */
    protected static class MeshDataBuilder {
        protected final Namespace<Void> mNamespace = new Namespace<>(); // Used to create unique names when object name is not defined. This also ensures that the names are stable among different reads of the same obj file.

        protected List<Integer> mFaces = new ArrayList<>();
        protected List<Integer> mFaceNormals = new ArrayList<>();
        protected List<Integer> mSmoothingGroups = new ArrayList<>();

        protected String mMaterialName = "white"; // Default material for .mtl files
        protected int mCurrentSmoothGroup;
        protected String mName;
        protected Collection<String> mGroups;

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

        public Optional<Pair<MeshData, String>> tryFinishMesh(List<Float> sharedVertices, List<Float> sharedTexCoords, List<Float> sharedNormals) {
            if (mFaces.isEmpty()) {
                return Optional.empty();
            }

            Map<Integer, Integer> vertexMap = new HashMap<>(sharedVertices.size() / 2);
            Map<Integer, Integer> texCoodsMap = new HashMap<>(sharedTexCoords.size() / 2);
            Map<Integer, Integer> normalMap = new HashMap<>(sharedNormals.size() / 2);
            List<Float> newVertices = new ArrayList<>(sharedVertices.size() / 2);
            List<Float> newUVs = new ArrayList<>(sharedTexCoords.size() / 2);
            List<Float> newNormals = new ArrayList<>(sharedNormals.size() / 2);
            boolean useNormals = true;

            for (int i = 0; i < mFaces.size(); i += 2) {
                // The current vertex indices in the faces array point to vertex positions in the "big", common vertex list.
                // We extract vertices which are used by the faces of the current mesh to a new, smaller vertex list and
                // rewrite the indices in the faces list to match the new smaller, local list.
                int vi = mFaces.get(i);
                Integer nvi = vertexMap.get(vi);
                if (nvi == null) {
                    nvi = newVertices.size() / 3;
                    vertexMap.put(vi, nvi);
                    newVertices.add(sharedVertices.get(vi * 3));
                    newVertices.add(sharedVertices.get(vi * 3 + 1));
                    newVertices.add(sharedVertices.get(vi * 3 + 2));
                }
                mFaces.set(i, nvi);

                // The same for UV (texture coordinates) indices
                int uvi = mFaces.get(i + 1);
                Integer nuvi = texCoodsMap.get(uvi);
                if (nuvi == null) {
                    nuvi = newUVs.size() / 2;
                    texCoodsMap.put(uvi, nuvi);
                    if (uvi >= 0) {
                        newUVs.add(sharedTexCoords.get(uvi * 2));
                        newUVs.add(sharedTexCoords.get(uvi * 2 + 1));
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
                        if (ni >= 0 && sharedNormals.size() >= (ni + 1) * 3) {
                            newNormals.add(sharedNormals.get(ni * 3));
                            newNormals.add(sharedNormals.get(ni * 3 + 1));
                            newNormals.add(sharedNormals.get(ni * 3 + 2));
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
            MeshData result = new MeshData(name, mGroups, newVertices, newUVs,
                new ArrayList<>(mFaces), new ArrayList<>(mSmoothingGroups),
                useNormals ? Optional.of(new MeshData.FaceNormalsData(newNormals, new ArrayList<>(mFaceNormals))) : Optional.empty(),
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

    public static ObjData readObj(IResourceLocator resourceLocator) throws IOException {
        return readObj(resourceLocator, Collections.emptyMap());
    }

    /**
     * Reads an {@code .obj} file from the given resource.
     * Note that the generated {@link ObjData} will always yield the same {@link MeshData#getName() names}
     * among different calls for the same object file.
     */
    public static ObjData readObj(IResourceLocator objFileLocator, Map<String, RawMaterialData> defaultMaterials) throws IOException {
        Map<String, RawMaterialData> materialLibrary = new TreeMap<>(defaultMaterials);

        IDirectoryLocator basePath = objFileLocator.getParentDirectory();

        ObjDataRaw objDataRaw = readObjRaw(objFileLocator);

        for (String filename : objDataRaw.getUsedMaterialLibraries()) {
            try {
                Map<String, RawMaterialData> materialSet = MtlLibraryIO.readMaterialSet(basePath.resolveResource(filename));
                materialLibrary.putAll(materialSet);
            } catch (IOException e) {
                log.error("Failed to read material library '" + filename + "'", e);
            }
        }

        Map<String, RawMaterialData> meshNamesToMaterials = new TreeMap<>();
        for (Entry<String, String> entry : objDataRaw.getMeshNamesToMaterialNames().entrySet()) {
            String meshName = entry.getKey();
            String materialName = entry.getValue();
            RawMaterialData materialData = StringUtils.isEmpty(materialName) ? null : materialLibrary.get(materialName);
            meshNamesToMaterials.put(meshName, materialData);
        }

        return new ObjData(objDataRaw.getMeshes(), meshNamesToMaterials);
    }

    public static ObjDataRaw readObjRaw(IResourceLocator objFileLocator) throws IOException {
        log.debug("Reading object file " + objFileLocator);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(objFileLocator.inputStream()))) {
            Collection<Pair<MeshData, String>> meshesWithMaterial = new ArrayList<>();
            Collection<String> usedMaterialLibraries = new ArrayList<>();

            // Those lists grow over the reading process and span all meshes;
            // the generated meshes are independent from each other and don't share
            // those values, so each mesh gets its own modified copy of those lists
            List<Float> sharedVertices = new ArrayList<>();
            List<Float> sharedTexCoords = new ArrayList<>();
            List<Float> sharedNormals = new ArrayList<>();

            // Those are mesh specific and are cleared for each new mesh
            MeshDataBuilder meshBuilder = new MeshDataBuilder();
            Pattern SPACE = Pattern.compile(" +");
            String line;
            while ((line = br.readLine()) != null) {
                try {
                    if (line.isEmpty() || line.startsWith("#")) {
                        // Comments and empty lines are ignored
                    } else if (line.startsWith("o ") || line.equals("o")) {
                        // A new object name finishes any already started mesh - will be a noop if we have no new mesh data yet
                        meshBuilder.tryFinishMesh(sharedVertices, sharedTexCoords, sharedNormals)
                            .ifPresent(meshAndMaterial -> meshesWithMaterial.add(meshAndMaterial));
                        if (line.length() > 2) {
                            meshBuilder.setName(line.substring(2));
                        }
                    } else if (line.startsWith("g ") || line.equals("g")) {
                        meshBuilder.tryFinishMesh(sharedVertices, sharedTexCoords, sharedNormals)
                            .ifPresent(meshAndMaterial -> meshesWithMaterial.add(meshAndMaterial));
                        if (line.length() > 2) {
                            String groupsStr = line.substring(2);
                            meshBuilder.setGroups(Arrays.asList(SPACE.split(groupsStr)));
                        }
                    } else if (line.startsWith("usemtl ")) {
                        meshBuilder.tryFinishMesh(sharedVertices, sharedTexCoords, sharedNormals)
                            .ifPresent(meshAndMaterial -> meshesWithMaterial.add(meshAndMaterial));
                        // Setting new material for next mesh
                        meshBuilder.setMaterialName(line.substring(7).trim());
                    } else if (line.startsWith("mtllib ")) {
                        // setting materials lib
                        String[] split = SPACE.split(line.substring(7).trim());

                        usedMaterialLibraries.addAll(Arrays.asList(split));
                    } else if (line.startsWith("v ")) {
                        String[] split = SPACE.split(line.substring(2).trim());
                        float x = Float.parseFloat(split[0]);
                        float y = Float.parseFloat(split[1]);
                        float z = Float.parseFloat(split[2]);
                        sharedVertices.add(x);
                        sharedVertices.add(y);
                        sharedVertices.add(z);
                    } else if (line.startsWith("vt ")) {
                        String[] split = SPACE.split(line.substring(3).trim());
                        float u = Float.parseFloat(split[0]);
                        float v = Float.parseFloat(split[1]);
                        sharedTexCoords.add(u);
                        sharedTexCoords.add(1 - v);
                    } else if (line.startsWith("f ")) {
                        String[] split = SPACE.split(line.substring(2).trim());
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
                        int numVertices = sharedVertices.size();
                        int numUvs = sharedTexCoords.size();
                        int v1 = vertexIndex(data[0][0], numVertices);
                        int uv1 = -1;
                        int n1 = -1;
                        if (uvProvided) {
                            uv1 = uvIndex(data[0][1], numUvs);
                            if (uv1 < 0) {
                                uvProvided = false;
                            }
                        }
                        int numNormals = sharedNormals.size();
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
                            if (uvProvided) {
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
                        String[] split = SPACE.split(line.substring(2).trim());
                        float x = Float.parseFloat(split[0]);
                        float y = Float.parseFloat(split[1]);
                        float z = Float.parseFloat(split[2]);
                        sharedNormals.add(x);
                        sharedNormals.add(y);
                        sharedNormals.add(z);
                    } else {
                        log.warn("Line skipped: " + line);
                    }
                } catch (Exception ex) {
                    log.error("Failed to parse line: " + line, ex);
                }
            }
            meshBuilder.tryFinishMesh(sharedVertices, sharedTexCoords, sharedNormals)
                .ifPresent(meshAndMaterial -> meshesWithMaterial.add(meshAndMaterial));

            return ObjDataRaw.of(meshesWithMaterial, usedMaterialLibraries);
        }
    }

    public static Collection<IResourceLocator> getAllFiles(IResourceLocator objFileLocator) throws IOException {
        Collection<IResourceLocator> result = new ArrayList<>();
        result.add(objFileLocator);
        IDirectoryLocator baseDirectoryLocator = objFileLocator.getParentDirectory();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(objFileLocator.inputStream()))) {
            Pattern SPACE = Pattern.compile(" +");
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("mtllib ")) {
                    String[] split = SPACE.split(line.substring("mtllib ".length()).trim());

                    for (String fileName : split) {
                        result.addAll(MtlLibraryIO.getAllFiles(baseDirectoryLocator.resolveResource(fileName)));
                    }
                }
            }
        }
        return result;
    }
}
