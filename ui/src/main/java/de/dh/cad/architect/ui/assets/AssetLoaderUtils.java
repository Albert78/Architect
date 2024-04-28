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

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import javax.imageio.ImageIO;

import de.dh.utils.fx.ImageUtils;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Transform;

public class AssetLoaderUtils {
    public static void createIconWithOverlay(Path sourceImage, Path targetImage, String overlayText) throws IOException {
        BufferedImage image;
        try {
            image = ImageIO.read(sourceImage.toFile());
        } catch (Exception e) {
            throw new IOException("Error creating icon with overlay from source image '" + sourceImage + "'", e);
        }
        try {
            ImageUtils.addImageOverlay(image, overlayText);
        } catch (Exception e) {
            throw new RuntimeException("Error creating image overlay icon for source image '" + sourceImage + "'", e);
        }
        try {
            ImageIO.write(image, "png", targetImage.toFile());
        } catch (Exception e) {
            throw new IOException("Error writing target image '" + targetImage + "'", e);
        }
    }

    /**
     * Converts a raw model rotation matrix, given in a support object descriptor, to a JavaFX {@link Transform}.
     */
    public static Optional<Transform> createTransform(float[][] rotationMatrix) {
        if (rotationMatrix == null) {
            return Optional.empty();
        }
        return Optional.of(Affine.affine(
                rotationMatrix[0][0], rotationMatrix[0][1], rotationMatrix[0][2], 0,
                rotationMatrix[1][0], rotationMatrix[1][1], rotationMatrix[1][2], 0,
                rotationMatrix[2][0], rotationMatrix[2][1], rotationMatrix[2][2], 0));
    }

    /**
     * Converts a JavaFX {@link Transform} to a raw model rotation matrix to be stored in a support object descriptor.
     */
    public static float[][] createRotationMatrix(Transform transform) {
        float[][] result = new float[3][];
        float[] xRow = new float[3];
        result[0] = xRow;

        xRow[0] = (float) transform.getMxx();
        xRow[1] = (float) transform.getMxy();
        xRow[2] = (float) transform.getMxz();

        float[] yRow = new float[3];
        result[1] = yRow;

        yRow[0] = (float) transform.getMyx();
        yRow[1] = (float) transform.getMyy();
        yRow[2] = (float) transform.getMyz();

        float[] zRow = new float[3];
        result[2] = zRow;

        zRow[0] = (float) transform.getMzx();
        zRow[1] = (float) transform.getMzy();
        zRow[2] = (float) transform.getMzz();

        return result;
    }
}
