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
package de.dh.cad.architect.ui.view.libraries;

import java.io.IOException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dh.cad.architect.model.assets.AssetRefPath;
import de.dh.cad.architect.ui.assets.AssetLoader;
import de.dh.utils.MaterialMapping;
import de.dh.utils.io.fx.MaterialData;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.Material;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;

public class ThreeDPreview {
    private static final Logger log = LoggerFactory.getLogger(ThreeDPreview.class);

    public static Node createMaterialPreviewBox(Material material, double edgeLength) {
        Box box = new Box(edgeLength, edgeLength, edgeLength);
        box.setMaterial(material);
        Group group = new Group();
        group.getChildren().add(box);
        Bounds boundsInParent = group.getBoundsInParent();
        ObservableList<Transform> transforms = group.getTransforms();
        transforms.add(new Translate(-boundsInParent.getCenterX(), -boundsInParent.getCenterY(), -boundsInParent.getCenterZ()));
        return group;
    }

    public static Node createMaterialPreviewBox(AssetRefPath materialRef, AssetLoader assetLoader, Optional<Double> oEdgeLength, boolean fallbackToPlaceholder) {
        double edgeLength = oEdgeLength.orElse(100.0);
        PhongMaterial material;
        if (materialRef == null) {
            material = new PhongMaterial();
            material.setDiffuseMap(AssetLoader.loadMaterialPlaceholderTextureImage(Optional.of(new ImageLoadOptions(edgeLength, edgeLength))));
        } else {
            try {
                MaterialData materialData = assetLoader.loadMaterialData(materialRef);
                if (fallbackToPlaceholder) {
                    material = assetLoader.buildMaterial_Lax(materialData, MaterialMapping.stretch());
                } else {
                    material = assetLoader.buildMaterial_Strict(materialData, MaterialMapping.stretch());
                }
            } catch (IOException e) {
                String msg = "Error loading material <" + materialRef + ">";
                if (fallbackToPlaceholder) {
                    log.warn(msg, e);
                    material = new PhongMaterial(Color.WHITE);
                } else {
                    throw new RuntimeException(msg, e);
                }
            }
        }
        return createMaterialPreviewBox(material, edgeLength);
    }
}
