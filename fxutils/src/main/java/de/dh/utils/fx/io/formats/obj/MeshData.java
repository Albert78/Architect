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

import java.util.Collection;
import java.util.Optional;

import de.dh.utils.fx.io.utils.FloatArrayList;
import de.dh.utils.fx.io.utils.IntegerArrayList;

/**
 * Data of a single 3D mesh object (from an {@code .obj} file).
 */
public class MeshData {
    public static class FaceNormalsData {
        protected final FloatArrayList mNormals;
        protected final IntegerArrayList mFaceNormals;

        public FaceNormalsData(FloatArrayList normals, IntegerArrayList faceNormals) {
            mNormals = normals;
            mFaceNormals = faceNormals;
        }

        public FloatArrayList getNormals() {
            return mNormals;
        }

        public IntegerArrayList getFaceNormals() {
            return mFaceNormals;
        }
    }

    protected final String mId;
    protected final String mName;
    protected final Collection<String> mGroups;
    protected final FloatArrayList mVertices;
    protected final FloatArrayList mUvs;
    protected final IntegerArrayList mFaces;
    protected final IntegerArrayList mSmoothingGroups;
    protected final Optional<FaceNormalsData> mOFaceNormalsData;
    protected final String mMaterialName;

    /**
     * Creates a new mesh data object.
     * ATTENTION: Id and name should be stable among different reads of the same file.
     */
    public MeshData(String id, String name, Collection<String> groups, FloatArrayList vertices, FloatArrayList uvs,
        IntegerArrayList faces, IntegerArrayList smoothingGroups, Optional<FaceNormalsData> oFaceNormalsData,
        String materialName) {
        mId = id;
        mName = name;
        mGroups = groups;
        mVertices = vertices;
        mUvs = uvs;
        mFaces = faces;
        mSmoothingGroups = smoothingGroups;
        mOFaceNormalsData = oFaceNormalsData;
        mMaterialName = materialName;
    }

    /**
     * Gets the id of this mesh data, which is unique in the set of meshes in the object file.
     * The id remains stable among different reads of the same object file.
     */
    public String getId() {
        return mId;
    }

    /**
     * Gets the name which is defined in the object file for this mesh or a generated name if no name is defined.
     * The name is unique in the set of meshes in the object file; if the name defined in the object file is not unique,
     * it is suffixed with a number to be made unique.
     * The name remains stable among different reads of the same object file.
     */
    public String getName() {
        return mName;
    }

    public Collection<String> getGroups() {
        return mGroups;
    }

    public FloatArrayList getVertices() {
        return mVertices;
    }

    public FloatArrayList getUvs() {
        return mUvs;
    }

    public IntegerArrayList getFaces() {
        return mFaces;
    }

    public IntegerArrayList getSmoothingGroups() {
        return mSmoothingGroups;
    }

    public Optional<FaceNormalsData> getOFaceNormalsData() {
        return mOFaceNormalsData;
    }

    public String getMaterialName() {
        return mMaterialName;
    }
}