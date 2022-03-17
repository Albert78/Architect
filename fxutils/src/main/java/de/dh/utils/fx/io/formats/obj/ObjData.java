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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import javafx.scene.paint.Material;
import javafx.scene.shape.Shape3D;

/**
 * Data of a 3D object, contains a collection of meshes which are mapped to materials, as it was defined in an {@code .obj} file.
 * The {@link ObjData} class provides an intermediate, in-memory format for the object definition which allows us
 * to override the specified mesh materials via user configuration, for example redefine the materials of the meshes of a piece of furniture.
 * This would not be possible so easily if we would translate the object data directly to objects in the final 3D library format
 * (e.g. {@link Shape3D} with {@link Material} instances).
 * We use a separate map {@link #getMeshIdsToMaterials()} to indirectly assign mesh objects to materials via the artificial mesh id
 * (instead of using the material name from the {@link MeshData} for mapping). This allows us to exchange the material of each individual
 * mesh instead of just exchanging a material which is potentially mapped to multiple meshes. Depending on the input data, each concept
 * has its pros and cons; we use the more flexible concept (mapping of materials via mesh ids).
 * Note that this model requires the {@link MeshData#getId() mesh id} to be stable among different readings of the same object
 * if we want to use the described overriding mechanism:
 * Typically, the material assignment (mesh ids -> material) is persisted between different sessions while the object data is loaded repeatedly
 * from the same file. In that usage scenario, the mesh id is used as stable connection between the object file mesh objects and the
 * assigned materials.
 */
public class ObjData {
    protected final Collection<MeshData> mMeshes; // Mesh names don't need to be unique
    protected final Map<String, RawMaterialData> mMaterials; // Mesh ids to material - not all mesh ids need to have a mapping

    public ObjData(Collection<MeshData> meshes, Map<String, RawMaterialData> materials) {
        mMeshes = meshes;
        mMaterials = materials;
    }

    public static ObjData of(Collection<Pair<MeshData, String>> meshesWithMaterial, Map<String, RawMaterialData> materialLibrary) {
        Collection<MeshData> meshes = new ArrayList<>();
        Map<String, RawMaterialData> meshIdsToMaterials = new TreeMap<>();
        for (Pair<MeshData, String> pair : meshesWithMaterial) {
            MeshData mesh = pair.getLeft();
            String materialName = pair.getRight();
            String meshId = mesh.getId();
            meshes.add(mesh);
            RawMaterialData materialData = StringUtils.isEmpty(materialName) ? null : materialLibrary.get(materialName);
            meshIdsToMaterials.put(meshId, materialData);
        }
        return new ObjData(meshes, meshIdsToMaterials);
    }

    public Collection<MeshData> getMeshes() {
        return mMeshes;
    }

    public Map<String, RawMaterialData> getMeshIdsToMaterials() {
        return mMaterials;
    }
}