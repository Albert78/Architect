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
package de.dh.cad.architect.libraryimporter;

import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dh.cad.architect.utils.vfs.IResourceLocator;
import de.dh.utils.io.ObjData;
import de.dh.utils.io.fx.FxMeshBuilder;
import de.dh.utils.io.obj.ObjReader;
import de.dh.utils.io.obj.RawMaterialData;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Transform;

public class ObjectLoader {
    private static final Logger log = LoggerFactory.getLogger(ObjectLoader.class);

    /**
     * Loads a 3D resource from an {@code .obj} file to a JavaFX (mesh) object.
     */
    public static Node load3DResource(IResourceLocator resource, float[][] modelRotation, Map<String, RawMaterialData> defaultMaterials) {
        if (resource == null) {
            return null;
        }
        try {
            Group result = new Group();
            ObjData objData = ObjReader.readObj(resource, defaultMaterials);
            result.getChildren().addAll(FxMeshBuilder.buildMeshViews(objData.getMeshes(), objData.getMeshNamesToMaterials(), false));
            Transform trans = createTransform(modelRotation);
            if (trans != null) {
                result.getTransforms().add(trans);
            }

            return result;
        } catch (IOException e) {
            log.warn("Error loading 3D resource '" + resource + "'", e);
            return null;
        }
    }

    public static Transform createTransform(float[][] rotationMatrix) {
        if (rotationMatrix == null) {
            return null;
        }
        try {
            return Affine.affine(
                rotationMatrix[0][0], rotationMatrix[0][1], rotationMatrix[0][2], 0,
                rotationMatrix[1][0], rotationMatrix[1][1], rotationMatrix[1][2], 0,
                rotationMatrix[2][0], rotationMatrix[2][1], rotationMatrix[2][2], 0);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
