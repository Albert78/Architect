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
package de.dh.utils.io;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Format-independent data container for a single 3D mesh object (e.g. from an {@code .obj} file).
 * We use a RHS, like in .obj files:
 * X grows to the right,
 * Y grows to the top,
 * Z grows in direction to the observer.
 */
public class MeshData {
    public static class FaceNormalsData {
        protected final List<Float> mNormals;
        protected final List<Integer> mFaceNormals;

        public FaceNormalsData(List<Float> normals, List<Integer> faceNormals) {
            mNormals = normals;
            mFaceNormals = faceNormals;
        }

        public List<Float> getNormals() {
            return mNormals;
        }

        public List<Integer> getFaceNormals() {
            return mFaceNormals;
        }
    }

    protected final String mName;
    protected final Collection<String> mGroups;
    protected final List<Float> mVertices;
    protected final List<Float> mTexCoods;
    protected final List<Integer> mFaces;
    protected final List<Integer> mSmoothingGroups;
    protected final Optional<FaceNormalsData> mOFaceNormalsData;
    protected final String mMaterialName;

    /**
     * Creates a new mesh data object.
     * ATTENTION: Id and name should be stable among different readings of the same file.
     */
    public MeshData(String name, Collection<String> groups, List<Float> vertices, List<Float> texCoords,
        List<Integer> faces, List<Integer> smoothingGroups, Optional<FaceNormalsData> oFaceNormalsData,
        String materialName) {
        mName = name;
        mGroups = groups;
        mVertices = vertices;
        mTexCoods = texCoords;
        mFaces = faces;
        mSmoothingGroups = smoothingGroups;
        mOFaceNormalsData = oFaceNormalsData;
        mMaterialName = materialName;
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

    public List<Float> getVertices() {
        return mVertices;
    }

    public List<Float> getTexCoords() {
        return mTexCoods;
    }

    public List<Integer> getFaces() {
        return mFaces;
    }

    public List<Integer> getSmoothingGroups() {
        return mSmoothingGroups;
    }

    public Optional<FaceNormalsData> getOFaceNormalsData() {
        return mOFaceNormalsData;
    }

    public String getMaterialName() {
        return mMaterialName;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mName);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MeshData other = (MeshData) obj;
        return Objects.equals(mName, other.mName);
    }

    @Override
    public String toString() {
        return "MeshData [name=" + mName + ", #faces=" + mFaces.size() + "]";
    }
}