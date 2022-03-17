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
package de.dh.utils.fx;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import de.dh.cad.architect.fx.nodes.objviewer.ThreeDObjectViewConfiguration.CameraType;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Bounds;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.ParallelCamera;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SnapshotParameters;
import javafx.scene.SubScene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;

public class ImageUtils {
    public static ImageView loadSquareIcon(Class<?> origin, String resourceName, int size) {
        return loadIcon(origin, resourceName, size, size);
    }

    public static ImageView loadIcon(Class<?> origin, String resourceName, int sizeX, int sizeY) {
        try (InputStream is = origin.getResourceAsStream(resourceName)) {
            ImageView result = new ImageView(new Image(is));
            result.setFitWidth(sizeX);
            result.setFitHeight(sizeY);
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Error loading icon '" + resourceName + "' for class " + origin.getName(), e);
        }
    }

    public static Image takeSnapshot(Node obj, int imageWidth, int imageHeight, Color backgroundColor, CameraType cameraType,
        LightConfiguration pointLightConfig, LightConfiguration ambientLightConfig) {
        ///////////////////////////////
        // Snapshot scene calculation
        ///////////////////////////////

        Group sceneRoot = new Group();
        Scene scene = new Scene(sceneRoot, -1, -1, true);
        scene.setFill(backgroundColor);

        Group subsceneRoot = new Group();
        SubScene subScene = new SubScene(subsceneRoot, 0, 0, true, SceneAntialiasing.BALANCED);
        ObservableList<Node> sceneRootChildren = sceneRoot.getChildren();
        sceneRootChildren.add(subScene);
        subsceneRoot.getChildren().add(obj);

        ///////////////////////////////
        // Calculation of correct scale
        ///////////////////////////////

        // TODO: Use transform of SnapshotParams:
        // SnapshotParams also supports a transform function but that transform works slightly different then
        // applying a transformation directly on our object.
        ObservableList<Transform> transforms = obj.getTransforms();

        // Scale object to make it's size match the desired image size - the result image size
        // will be derived from the object's size by the snapshot procedure
        Bounds boundsInParent = obj.getBoundsInParent();
        double scaleX = imageWidth / boundsInParent.getWidth();
        double scaleY = imageHeight / boundsInParent.getHeight();
        double scale = Math.min(scaleX, scaleY); // Makes the biggest direction of the image fit the desired image size; the other direction can be smaller

        // Hack: ParallelCamera has a stupid hard coded calculation for the near and far clipping plane.
        // Let mvs be the max X/Y image size, then the far clipping pane is located at Z=mvs/2 and the near clipping pane is located at Z=-mvs/2.
        // See ParallelCamera#computeProjectionTransform(GeneralTransform3D).
        // This Z scale makes our object match into those clipping planes for ParallelCamera. Also seems to work for PerspectiveCamera.
        double scaleZ = Math.max(imageWidth, imageHeight) / 2 / boundsInParent.getHeight();
        if (scaleZ > 1) {
            scaleZ = 1;
        }
        Scale objectScale = new Scale(scale, scale, scaleZ);
        transforms.add(0, objectScale);

        boundsInParent = obj.getBoundsInParent();

        double translateZ;
        if (cameraType == CameraType.Parallel) {
            // For parallel camera, we center the object between the hard coded clipping panes of ParallelCamera
            translateZ = -boundsInParent.getCenterZ();
        } else if (cameraType == CameraType.Perspective) {
            // We try to compmpensate the perspective camera's field of view.
            // Actually, we'd need to find a X/Y/Z translation to make all points of the camera's projection fit into our
            // target image, which depends on the object's shape (e.g. an object which is wider at its front needs a bigger
            // Z translation than an object which is wider at its back, additionally if the object is unequally wide or high
            // at right/left/top/bottom would leave an empty space in the target image at that side, which could be compensated
            // with a X/Y translation.
            // Using a constant value is a good approximation because we work with a normalized object size.
            // The value is a compromise between too much space at the border for special object shapes and/or rotations
            // and a clipped object projection.
            translateZ = 60;
        } else {
            throw new RuntimeException("Handling for camera type " + cameraType + " is not implemented");
        }
        // Move object to start at 0/0 because the camera's view port starts at 0/0.
        Translate objectTranslate = new Translate(-boundsInParent.getMinX(), -boundsInParent.getMinY(), translateZ);
        transforms.add(0, objectTranslate);

        ///////////////////////////////
        // Lights
        ///////////////////////////////

        AmbientLight ambientLight = new AmbientLight();
        PointLight pointLight = new PointLight();
        Rotate lightXRotate = new Rotate(pointLightConfig.getLightAngleX(), Rotate.X_AXIS);
        Rotate lightZRotate = new Rotate(180 + pointLightConfig.getLightAngleZ(), Rotate.Z_AXIS); // I don't know why the Z rotation is different for snapshot...
        Translate lightTranslate = new Translate(0, pointLightConfig.getLightDistance());

        pointLight.getTransforms().addAll(objectTranslate, objectScale, lightXRotate, lightZRotate, lightTranslate);
        pointLight.setLightOn(pointLightConfig.isLightOn());
        pointLight.setColor(pointLightConfig.getLightColor());
        ambientLight.setLightOn(ambientLightConfig.isLightOn());
        ambientLight.setColor(ambientLightConfig.getLightColor());

        sceneRootChildren.addAll(pointLight, ambientLight);

        ///////////////////////////////
        // Snapshot parameters
        ///////////////////////////////

        SnapshotParameters params = new SnapshotParameters();
        params.setDepthBuffer(true);
        params.setFill(backgroundColor);

        // Problem with ParallelCamera: Near and far clipping parameters are ignored, so we must scale our object
        // Problem with PerspectiveCamera: Because of the perspective, it's too complicated to compute a correct translation
        // to make the object match our desired image size
        switch (cameraType) {
        case Parallel:
            params.setCamera(new ParallelCamera());
            break;
        case Perspective:
            params.setCamera(new PerspectiveCamera(false));
            break;
        default:
            throw new RuntimeException("Handling for camera type " + cameraType + " is not implemented");
        }

        WritableImage result = subsceneRoot.snapshot(params, null);
        // The system produces a spare pixel border at X=0 and Y=0, which seems a problem of the mapping of
        // float/double coordinates to int image coordinates. The resulting image is then one pixel too big in
        // both directions. As workaround, we cleanup the image borders.
        result = cropTransparentImageBorders(result);
        return result;
    }

    // Old implementation, only for ParallelCamera
    public static Image takeSnapshot(Node objView, LightType lightType, int imageSize) {
        Group sceneRoot = new Group();
        Scene scene = new Scene(sceneRoot, -1, -1, true);
        scene.setFill(Color.TRANSPARENT);

        Group subsceneRoot = new Group();
        SubScene subScene = new SubScene(subsceneRoot, 0, 0, true, SceneAntialiasing.DISABLED);
        ObservableList<Node> sceneRootChildren = sceneRoot.getChildren();
        sceneRootChildren.add(subScene);
        ObservableList<Transform> transforms = objView.getTransforms();

        Bounds boundsInParent = objView.getBoundsInParent();

        // Move object to start at 0/0 because ParallelCamera's view port starts at 0/0.
        // In Z direction, we move the object to the center to make our scale hack work (see below).
        transforms.add(0, new Translate(-boundsInParent.getMinX(), -boundsInParent.getMinY(), -boundsInParent.getCenterZ()));

        // Scale object to make it's size match the desired image size - the result image size
        // will be derived from the object's size by the snapshot procedure
        double objExtentsInPlane = Math.max(boundsInParent.getWidth(), boundsInParent.getHeight());
        double scale = imageSize / objExtentsInPlane;

        // Hack: ParallelCamera has a stupid hard coded calculation for the near and far clipping plane.
        // Let mvs be the max X/Y image size, then the far clipping pane is located at Z=mvs and the near clipping pane is located at Z=-mvs.
        // See ParallelCamera#computeProjectionTransform(GeneralTransform3D).
        // We choose the Z scale to make our object match into those clipping planes.
        double scaleZ = imageSize / 2 / boundsInParent.getDepth();
        transforms.add(0, new Scale(scale, scale, scaleZ));
        boundsInParent = objView.getBoundsInParent();

        // Reflective objects look better with an AmbientLight while matt objects look better with PointLight
        switch (lightType) {
        case Ambient: {
            sceneRootChildren.add(new AmbientLight());
            break;
        }
        case Point: {
            PointLight light = new PointLight();

            light.setTranslateZ(-1000 - boundsInParent.getDepth() / 2);
            light.setTranslateX(boundsInParent.getWidth() / 3); // Division by 3 to move light out of the center
            light.setTranslateY(boundsInParent.getHeight() / 3);
            sceneRootChildren.add(light);
            break;
        }
        default:
            throw new IllegalArgumentException("Unsupported light type " + lightType);
        }

        subsceneRoot.getChildren().add(objView);

        SnapshotParameters params = new SnapshotParameters();
        params.setDepthBuffer(true);
        params.setFill(Color.TRANSPARENT);

        // Problem with ParallelCamera: Near and far clipping parameters are ignored, so we must scale our object
        // Problem with PerspectiveCamera: Because of the perspective, it's not simply possible to calculate
        // a scale factor to make the object match our desired image size
        ParallelCamera camera = new ParallelCamera();
        params.setCamera(camera);

        WritableImage result = subsceneRoot.snapshot(params, null);
        // The system produces a spare pixel border at X=0 and Y=0, which seems a problem of the mapping of
        // float/double coordinates to int image coordinates. The resulting image is then one pixel too big in
        // both directions. As workaround, we cleanup the image borders.
        result = cropTransparentImageBorders(result);
        return result;
    }

    public static WritableImage cropTransparentImageBorders(WritableImage image) {
        int height = (int) image.getHeight();
        int width = (int) image.getWidth();
        int startX = 0;
        int startY = 0;
        boolean changed = false;

        PixelReader pixelReader = image.getPixelReader();
        // Upper border
        boolean allTransparent = true;
        for (int x = startX; x < width; x++) {
            Color color = pixelReader.getColor(x, startY);
            if (color.getOpacity() > 0) {
                allTransparent = false;
                break;
            }
        }
        if (allTransparent) {
            startY++;
            height--;
            changed = true;
        }

        // Lower border
        allTransparent = true;
        for (int x = startX; x < width; x++) {
            Color color = pixelReader.getColor(x, startY + height - 1);
            if (color.getOpacity() > 0) {
                allTransparent = false;
                break;
            }
        }
        if (allTransparent) {
            height--;
            changed = true;
        }

        // Left border
        allTransparent = true;
        for (int y = startY; y < height; y++) {
            Color color = pixelReader.getColor(startX, y);
            if (color.getOpacity() > 0) {
                allTransparent = false;
                break;
            }
        }
        if (allTransparent) {
            startX++;
            width--;
            changed = true;
        }

        // Right border
        allTransparent = true;
        for (int y = startY; y < height; y++) {
            Color color = pixelReader.getColor(startX + width - 1, y);
            if (color.getOpacity() > 0) {
                allTransparent = false;
                break;
            }
        }
        if (allTransparent) {
            width--;
            changed = true;
        }

        if (!changed) {
            return image;
        }

        WritableImage result = new WritableImage(width, height);
        PixelWriter pixelWriter = result.getPixelWriter();

        for (int y = 0; y < height; y++){
            for (int x = 0; x < width; x++){
                Color color = pixelReader.getColor(x + startX, y + startY);
                pixelWriter.setColor(x, y, color);
            }
        }

        return result;
    }

    public static void saveImage(Image image, String extension, Path imagePath) throws IOException {
        try (OutputStream os = Files.newOutputStream(imagePath)) {
            saveImage(image, extension, os);
        }
    }

    public static void saveImage(Image image, String extension, OutputStream outputStream) throws IOException {
        BufferedImage bi = SwingFXUtils.fromFXImage(image, null);
        ImageIO.write(bi, extension, outputStream);
    }
}
