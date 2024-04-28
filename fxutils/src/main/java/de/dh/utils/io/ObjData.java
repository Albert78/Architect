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
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import de.dh.utils.io.fx.MaterialData;
import de.dh.utils.io.obj.RawMaterialData;
import javafx.scene.paint.Material;
import javafx.scene.shape.Shape3D;

/**
 * Data of a 3D object, contains a collection of meshes which are mapped to materials, as it was defined in an {@code .obj} file.
 * The {@link ObjData} class provides an intermediate, in-memory format for the object definition which allows us
 * to separate the parsing/loading of object files from the creation of the final 3D shape object in our 3D library.
 * This allows us to override the mesh materials, which are defined in the object file, via user configuration. So we can for example
 * redefine the materials of the meshes of a piece of furniture to change the color or the surface of the object.
 * This would not be possible so easily if we would translate the object and material data directly to objects in the final 3D library format
 * (e.g. {@link Shape3D} with {@link Material} instances), like it is done in several libraries available in the net.
 *
 * We use a separate map {@link #getMeshNamesToMaterials()} to indirectly assign mesh objects to materials via the artificial mesh's name
 * (instead of using the material name from the {@link MeshData} as mapping key). This allows us to define a material for each individual
 * mesh instead of just exchanging a given material (which potentially could be mapped to multiple meshes).
 *
 * Depending on the input data, each concept has its pros and cons; while well-designed objects would have assigned the same material for
 * similar surfaces (where the material is defined by name), we use the more flexible concept in this class (mapping of materials via mesh names),
 * which allows to overide the material for each individual mesh, independent of the object file definition.
 * Note that this model requires the {@link MeshData#getName() mesh names} to be stable among different readings of the same object
 * if we want to use the described overriding mechanism. So the reader has to ensure that the names are unique, attaching a unique prefix string,
 * if necessary.
 *
 * Typically, the user defined material assignment (mesh names -> material) is persisted between different sessions by the application.
 * For this usage scenario, we use the mesh names as stable connection/key between the object file mesh objects and the assigned materials.
 * Thus, because the object data is loaded repeatedly from the same object file, the object file reader will/should generate stable
 * names among different file loadings.
 */
public class ObjData {
    protected final Collection<MeshData> mMeshes; // Mesh names must be unique
    protected final Map<String, MaterialData> mMeshNamesToMaterials; // Mesh names to material - not all mesh names need to have a mapping

    public ObjData(Collection<MeshData> meshes, Map<String, MaterialData> materials) {
        mMeshes = meshes;
        mMeshNamesToMaterials = materials;
    }

    public Collection<MeshData> getMeshes() {
        return mMeshes;
    }

    public Map<String, MaterialData> getMeshNamesToMaterials() {
        return mMeshNamesToMaterials;
    }
}
