/*******************************************************************************
 * Architect - A free 2D/3D home and interior designer
 * Copyright (c) 2024 Daniel Höh
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>
 ******************************************************************************/

package de.dh.cad.architect.ui.assets;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import de.dh.cad.architect.model.coords.Length;
import javafx.scene.shape.MeshView;
import javafx.scene.transform.Transform;

public class IncompleteThreeDObject {
    protected final ThreeDObject mThreeDObjectWithoutMaterials;
    protected final Map<String, String> mMeshNamesToMaterialNames;

    public IncompleteThreeDObject(Collection<MeshView> surfaces, Map<String, String> meshNamesToMaterialNames,
            Optional<Transform> oTrans, Length width, Length height, Length depth) {
        mThreeDObjectWithoutMaterials = new ThreeDObject(surfaces, oTrans, width, height, depth);
        mMeshNamesToMaterialNames = meshNamesToMaterialNames;
    }

    public ThreeDObject getThreeDObjectWithoutMaterials() {
        return mThreeDObjectWithoutMaterials;
    }

    public Map<String, String> getMeshNamesToMaterialNames() {
        return mMeshNamesToMaterialNames;
    }
}
