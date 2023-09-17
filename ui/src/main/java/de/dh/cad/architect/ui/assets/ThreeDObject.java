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
package de.dh.cad.architect.ui.assets;

import java.util.Collection;
import java.util.Optional;

import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.ui.utils.CoordinateUtils;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.shape.MeshView;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;

/**
 * Contains a 3D object, consisting of surfaces and an optional global normalization transformation.
 * Each surface is meant to be textured with an arbitrary material. The global normalization transform is used
 * to compensate a potential object rotation of the base object.
 */
public class ThreeDObject {
    protected final Collection<MeshView> mSurfaces;
    protected final Optional<Transform> mORootTransformation;
    protected final Length mWidth;
    protected final Length mHeight;
    protected final Length mDepth;

    public ThreeDObject(Collection<MeshView> surfaces, Optional<Transform> oTrans, Length width, Length height, Length depth) {
        mSurfaces = surfaces;
        mORootTransformation = oTrans;

        mWidth = width;
        mHeight = height;
        mDepth = depth;
    }

    public Length getWidth() {
        return mWidth;
    }

    public Length getHeight() {
        return mHeight;
    }

    public Length getDepth() {
        return mDepth;
    }

    /**
     * Returns the surfaces of this 3D object. Each returned {@link MeshView} has the surface id set
     * in the {@link Node#getId() id property}.
     */
    public Collection<MeshView> getSurfaceMeshViews() {
        return mSurfaces;
    }

    /**
     * Returns the transformation which compensates a potentially wrong object orientation,
     * thus this transformation has to be applied to the object model when using it.
     */
    public Optional<Transform> getORootTransformation() {
        return mORootTransformation;
    }

    public Transform getTransform(Transform additionalTransform) {
        return mORootTransformation.map(rootTransformation -> {
            // Attention: Matrix multiplication, inverse sequence of transformations
            return additionalTransform.createConcatenation(rootTransformation);
        }).orElse(additionalTransform);
    }

    public Optional<Transform> getTransform(Optional<Transform> oAdditionalTransform) {
        return mORootTransformation.map(rootTransformation -> {
            if (oAdditionalTransform.isPresent()) {
                Transform additionalTransform = oAdditionalTransform.get();
                // Attention: Matrix multiplication, inverse sequence of transformations
                return Optional.of(additionalTransform.createConcatenation(rootTransformation));
            }
            return Optional.of(rootTransformation);
        }).orElse(oAdditionalTransform);
    }

    public Group getObject() {
        Group result = new Group();
        result.getChildren().addAll(mSurfaces);
        Optional<Transform> oTrans = getTransform(Optional.empty());
        Transform rotate = oTrans.orElse(new Affine());
        rotateAndCenter(result, rotate);

        double width = CoordinateUtils.lengthToCoords(mWidth, null);
        double height = CoordinateUtils.lengthToCoords(mHeight, null);
        double depth = CoordinateUtils.lengthToCoords(mDepth, null);

        Bounds bounds = result.getBoundsInParent();

        // Attention: Global coordinate system -> Y/Z exchanged
        Scale scale = new Scale(width / bounds.getWidth(), depth / bounds.getHeight(), height / bounds.getDepth());
        result.getTransforms().add(0, scale);
        return result;
    }

    /**
     * Rotates the given node according to the given rotation transform, centers it in X/Y and positions it on Z=0.
     */
    public static void rotateAndCenter(Group node, Transform rotate) {
        ObservableList<Transform> transforms = node.getTransforms();

        // Rotate needs to be applied first because we don't center in Z direction, thus movint the rotation after the
        // center translation below would move our object away from the desired centered/Z=0 position
        transforms.add(rotate);

        Bounds bounds = node.getBoundsInParent();

        // Attention: When playing with this transform, remember that it will put the object on level Z=0 in
        // the coordinate system of our local node. The visible node in the 3D view is centered again, thus
        // we probably won't see the bottom of our object at Z=0 in the 3D view!
        Translate centerTranslate = new Translate(
            -bounds.getWidth() / 2 - bounds.getMinX(),
            -bounds.getHeight() / 2 - bounds.getMinY(),
            -bounds.getMinZ()); // Center in X/Y, put to 0 in Z

        transforms.add(0, centerTranslate);
    }
}