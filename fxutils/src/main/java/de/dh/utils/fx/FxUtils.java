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
package de.dh.utils.fx;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

public class FxUtils {
    public static void addClippingToBounds(Region node) {
        Rectangle clipRect = new Rectangle(node.getWidth(), node.getHeight());
        clipRect.heightProperty().bind(node.heightProperty());
        clipRect.widthProperty().bind(node.widthProperty());
        node.setClip(clipRect);
    }

    public static Transform concatenate(List<Transform> transforms) {
        Transform result = null;
        for (Transform t : transforms) {
            if (result == null) {
                result = t;
            } else {
                result = result.createConcatenation(t);
            }
        }
        return result == null ? new Affine() : result;
    }

    public static void trySetInitialDirectory(FileChooser fc, Path path) {
        while (path != null && !Files.isDirectory(path)) {
            path = path.getParent();
        }
        if (path != null) {
            fc.setInitialDirectory(path.toFile());
        }
    }

    public static void trySetInitialDirectory(DirectoryChooser dc, Path path) {
        while (path != null && !Files.isDirectory(path)) {
            path = path.getParent();
        }
        if (path != null) {
            dc.setInitialDirectory(path.toFile());
        }
    }

    /**
     * Makes an editable {@link ComboBox} reflect it's editor's value changes directly
     * to its {@link ComboBox#getValue() model}. By default, the JavaFX ComboBox does
     * not reflect changes of its text edit to its model until the user presses Enter or
     * the ComboBox looses focus (? not sure).
     */
    public static void setImmediateCommitText(ComboBox<String> comboBox) {
        comboBox.getEditor().textProperty().addListener((obs, oldText, newText) -> {
            comboBox.setValue(newText);
        });
    }

    public static void normalizeAndCenter(Node node, int targetSize, boolean resetTransforms) {
        ObservableList<Transform> transforms = node.getTransforms();
        if (resetTransforms) {
            transforms.clear();
        }
        Bounds objBoundsInParent = node.getBoundsInParent();

        Translate translate = new Translate();
        // Translate the object to the center
        double objWidth = objBoundsInParent.getWidth();
        double middleTranslateX = -objWidth / 2;
        double currentTranslateX = objBoundsInParent.getMinX();
        translate.setX(middleTranslateX - currentTranslateX);

        double objHeight = objBoundsInParent.getHeight();
        double middleTranslateY = -objHeight / 2;
        double currentTranslateY = objBoundsInParent.getMinY();
        translate.setY(middleTranslateY - currentTranslateY);

        double objDepth = objBoundsInParent.getDepth();
        double middleTranslateZ = -objDepth / 2;
        double currentTranslateZ = objBoundsInParent.getMinZ();
        translate.setZ(middleTranslateZ - currentTranslateZ);

        // Normalize the object's size
        double maxSize = Math.max(objBoundsInParent.getWidth(), Math.max(objBoundsInParent.getHeight(), objBoundsInParent.getDepth()));
        double scale = targetSize / maxSize;
        Scale objectSizeCompensation = new Scale();
        objectSizeCompensation.setX(scale);
        objectSizeCompensation.setY(scale);
        objectSizeCompensation.setZ(scale);

        transforms.addAll(0, Arrays.asList(objectSizeCompensation, translate));
    }

    public static Font deriveFont(Font font, FontWeight newWeight, FontPosture newPosture) {
        return Font.font(font.getFamily(), newWeight, newPosture, font.getSize());
    }
}
