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
package de.dh.utils.io.fx;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dh.cad.architect.utils.vfs.IDirectoryLocator;
import de.dh.cad.architect.utils.vfs.IResourceLocator;
import de.dh.utils.ArrayUtils;
import de.dh.utils.MaterialMapping;
import de.dh.utils.Vector2D;
import de.dh.utils.Vector3D;
import de.dh.utils.io.MeshData;
import de.dh.utils.io.MeshData.FaceNormalsData;
import de.dh.utils.io.ObjData;
import de.dh.utils.io.SmoothingGroups;
import de.dh.utils.io.obj.ParserUtils;
import de.dh.utils.io.obj.ParserUtils.TokenIterator;
import de.dh.utils.io.obj.RawMaterialData;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.Material;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.Mesh;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Rotate;

/**
 * Class for building JavaFX {@link MeshView} objects from {@code .obj} and {@code .mtl} file data which
 * are given in the form of {@link MeshData} and {@link RawMaterialData} instances.
 *
 * Common JavaFX {@code .obj} file importers in the internet build JavaFX {@link Material} instances directly from the
 * entries in the material library ({@code .mtl}) file and {@link Mesh} objects directly from the object
 * entries in the object ({@code .obj}) file.
 * But this typical approach has some drawbacks:
 * <ul>
 * <li>
 * When transporting the object properties directly via JavaFX {@link Material} / {@link MeshView} instances,
 * it is hard to implement an overriding semantics of materials in inherited objects, like exchanging the
 * color of the surface of objects.
 * Therefore, we use a format-independent in-memory-model for mesh data and material with an explicit mapping
 * of meshes to materials.
 * </li>
 * <li>
 * The typical object reader/writer code you can find in internet is JavaFX specific and could not be reused
 * for other 3D frontend libraries.
 * </li>
 * </ul>
 *
 * Therefore, we use a late-binding approach where we store all object and material data in a combination of
 * {@link ObjData} / {@link MeshData} / {@link RawMaterialData} instances, which are completely independent from
 * JavaFX and then use this {@link FxMeshBuilder} class to translate/materialize the objects to JavaFX
 * {@link MeshView} instances.
 */
public class FxMeshBuilder {
    private static final Logger log = LoggerFactory.getLogger(FxMeshBuilder.class);

    protected static float parseBaseColor(String baseColorStr) {
        return Math.min(1, Float.parseFloat(baseColorStr));
    }

    /**
     * Translates an RGB color string from a material file to a JavaFX {@link Color}.
     */
    protected static Color readMtlColor(String colorStr) {
        String[] split = colorStr.trim().split(" +");
        float red = parseBaseColor(split[0]);
        float green = parseBaseColor(split[1]);
        float blue = parseBaseColor(split[2]);
        return Color.color(red, green, blue);
    }

    protected static Image loadImage(String fileName, IDirectoryLocator baseDirectory) throws IOException {
        IResourceLocator fileLocator = baseDirectory.resolveResource(fileName);
        try (InputStream is = fileLocator.inputStream()) {
            Image result = new Image(is);
            log.trace("Loaded image from " + fileLocator);
            return result;
        } catch (IOException e) {
            throw new IOException("Error loading image from " + fileLocator, e);
        }
    }

    protected static class TextureMapData {
        protected final Image mTextureImage;

        protected final Optional<Vector3D> mOffset;
        protected final Optional<Vector3D> mScale;

        protected TextureMapData(Image textureImage, Optional<Vector3D> offset, Optional<Vector3D> scale) {
            mTextureImage = textureImage;
            mOffset = offset;
            mScale = scale;
        }

        public Image getTextureImage() {
            return mTextureImage;
        }

        /**
         * Gets the position offset of the texture image on the target surface.
         */
        public Optional<Vector3D> getOffset() {
            return mOffset;
        }

        /**
         * Gets the optional scaling factor to be applied to the texture. The texture tile size
         * is the size of the texture image times this scale factor.
         */
        public Optional<Vector3D> getScale() {
            return mScale;
        }
    }

    /**
     * Checks if the given line is the given map command and tries to parse that material texture map command.
     * @param line A line of a .mtl material file, like {@code map_Kd -o 0.200 0.000 0.000 texture.png}.
     * @param mapCommand The command to check, one of the map commands ({@code map_Kd, map_Ks, map_Ka}).
     * @param baseDirectory Directory to search the referenced image or texture files.
     * @return The extracted data or {@code null} if the given line does not specify the given command.
     */
    protected static TextureMapData tryParseTextureMapCommand(String line, String mapCommand,
            IDirectoryLocator baseDirectory) throws IOException {
        if (!line.startsWith(mapCommand + " ")) {
            return null;
        }

        String rest = line.substring(7).trim();
        String fileName = ParserUtils.getLastPart(rest);

        // Texture offsets
        float oU = 0;
        float oV = 0;
        @SuppressWarnings("unused")
        float oW = 0;
        // Texture scale
        float sU = 1;
        float sV = 1;
        @SuppressWarnings("unused")
        float sW = 1;

        if (rest.length() > fileName.length()) {
            String optionsStr = rest.substring(0, rest.length() - fileName.length());
            if (!StringUtils.isEmpty(optionsStr)) {
                TokenIterator ti = TokenIterator.tokenize(optionsStr);
optionsLoop:
                while (ti.moveNext()) {
                    String t = ti.getCurrentToken();
                    if ("-s".equals(t)) {
                        if (!ti.moveNext()) {
                            log.warn("Invalid arguments for option -s in options string '" + optionsStr + "' for command " + mapCommand);
                            break;
                        }
                        sU = Float.parseFloat(ti.getCurrentToken());
                        if (ti.moveNext()) {
                            t = ti.getCurrentToken();
                            if (t.startsWith("-")) {
                                ti.moveBack();
                            } else {
                                String vStr = ti.getCurrentToken();
                                sV = Float.parseFloat(vStr);

                                t = ti.getCurrentToken();
                                if (t.startsWith("-")) {
                                    ti.moveBack();
                                } else {
                                    String wStr = ti.getCurrentToken();
                                    sW = Float.parseFloat(wStr);
                                }
                            }
                        }
                        continue;
                    }
                    if ("-o".equals(t)) {
                        if (!ti.moveNext()) {
                            log.warn("Invalid arguments for option -o in options string '" + optionsStr + "' for command " + mapCommand);
                            break;
                        }
                        oU = Float.parseFloat(ti.getCurrentToken());
                        if (ti.moveNext()) {
                            t = ti.getCurrentToken();
                            if (t.startsWith("-")) {
                                ti.moveBack();
                            } else {
                                String vStr = ti.getCurrentToken();
                                oV = Float.parseFloat(vStr);

                                t = ti.getCurrentToken();
                                if (t.startsWith("-")) {
                                    ti.moveBack();
                                } else {
                                    String wStr = ti.getCurrentToken();
                                    oW = Float.parseFloat(wStr);
                                }
                            }
                        }
                        continue;
                    }
                    // Skip until next option
                    StringBuilder ignored = new StringBuilder(t);
                    while (ti.moveNext()) {
                        t = ti.getCurrentToken();
                        if (t.startsWith("-")) {
                            ti.moveBack();
                            continue optionsLoop;
                        }
                        ignored.append(" ").append(t);
                    }
                    log.warn("Ignoring option '" + ignored + "' for command " + mapCommand);
                }
            }
        }

        try {
            Image textureImage = loadImage(fileName, baseDirectory);
            return new TextureMapData(textureImage,
                    oU != 0 || oV != 0 || oW != 0 ? Optional.of(new Vector3D(oU, oV, oW)) : Optional.empty(),
                    sU != 1 || sV != 1 || sW != 1 ? Optional.of(new Vector3D(sU, sV, sW)) : Optional.empty());
        } catch (IOException e) {
            throw new IOException("Unable to load image for command " + mapCommand, e);
        }
    }

    protected static Image tryCreateTextureMap(String line, String mapCommand, Optional<Vector2D> oNativeMaterialTileSize, IDirectoryLocator baseDirectory,
            MaterialMapping mappingConfig) throws IOException {
        TextureMapData tmd = tryParseTextureMapCommand(line, mapCommand, baseDirectory);
        if (tmd == null) {
            return null;
        }
        Optional<Vector3D> offsetFromCommand = tmd.getOffset();
        Optional<Vector3D> scaleFromCommand = tmd.getScale();

        Image textureImage = tmd.getTextureImage();
        double imageWidth = textureImage.getWidth();
        double imageHeight = textureImage.getHeight();
        Vector2D imageSize = new Vector2D(imageWidth, imageHeight);

        Optional<Vector2D> oOffset = mappingConfig
                .getMaterialOffset()
                .or(() -> offsetFromCommand
                        .map(Vector3D::projectXY)); // We don't support the "w" parts yet (neither in offset nor scale)

        // Attention: The mapping config stores the tile size while the command only stores the scale, which must be multiplied with the image size
        Optional<Vector2D> oTileSize = mappingConfig
                .getMaterialTileSize()
                .or(() -> oNativeMaterialTileSize)
                .or(() -> scaleFromCommand
                        .map(s -> new Vector2D(s.getX() + imageWidth, s.getY() * imageHeight)));

        Optional<Double> oMaterialRotationDeg = mappingConfig.getMaterialRotationDeg();

        MaterialMapping.LayoutMode layoutMode = mappingConfig.getLayoutMode();

        if (layoutMode == MaterialMapping.LayoutMode.Stretch && oOffset.isEmpty() && oMaterialRotationDeg.isEmpty()) {
            // In this case, we don't need to go the complex path; the given texture can be used as it is
            return textureImage;
        }

        Vector2D targetSurfaceSize = mappingConfig
                .getTargetSurfaceSize()
                .orElse(imageSize);

        Vector2D offset = oOffset
                .orElse(Vector2D.EMPTY);
        Vector2D tileSize = layoutMode == MaterialMapping.LayoutMode.Stretch
                ? targetSurfaceSize
                : oTileSize
                .orElse(imageSize);

        double textureResolutionPerLengthUnit = mappingConfig.getTextureResolutionPerLengthUnit();

        ImagePattern pattern = new ImagePattern(textureImage,
                offset.getX() * textureResolutionPerLengthUnit, offset.getY() * textureResolutionPerLengthUnit, // Offset of the material tile on the surface
                tileSize.getX() * textureResolutionPerLengthUnit, tileSize.getY() * textureResolutionPerLengthUnit, // Size of the material tile on the surface
                false);

        // That surface image texture will enclose / contain the actual texture image tiled over its surface.
        double textureWidth = targetSurfaceSize.getX() * textureResolutionPerLengthUnit;
        double textureHeight = targetSurfaceSize.getY() * textureResolutionPerLengthUnit;
        Canvas canvas = new Canvas(textureWidth, textureHeight);
        final GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(pattern);

        if (oMaterialRotationDeg.isPresent()) {
            double materialRotationDeg = oMaterialRotationDeg.get();
            double materialRotationRad = materialRotationDeg * Math.PI / 180;
            double sin = Math.abs(Math.sin(materialRotationRad));
            double cos = Math.abs(Math.cos(materialRotationRad));
            double textureWidthRot = textureWidth * cos + textureHeight * sin;
            double textureHeightRot = textureWidth * sin + textureHeight * cos;

            double dx = -(textureWidthRot - textureWidth) / 2;
            double dy = -(textureHeightRot - textureHeight) / 2;
            gc.setTransform(new Affine(new Rotate(materialRotationDeg, textureWidth / 2, textureHeight / 2)));
            gc.fillRect(dx, dy, textureWidthRot, textureHeightRot);
        } else {
            gc.fillRect(0, 0, textureWidth, textureHeight);
        }
        return canvas.snapshot(new SnapshotParameters(), null);
    }

    public static PhongMaterial buildMaterial_Strict(MaterialData materialData, MaterialMapping mappingConfig) throws IOException {
        PhongMaterial result = new PhongMaterial(Color.WHITE);
        if (materialData != null) {
            int lineNo = 0;
            for (String line : materialData.getLines()) {
                lineNo++;
                line = line.trim();
                try {
                    Image textureImage;

                    // TODO: There are many unsupported commands and options, e.g.
                    //  the source texture modifications options like -mm or -texres
                    if (line.isEmpty() || line.startsWith("#")) {
                        // Ignore comments and empty lines
                    } else if (line.startsWith("newmtl ")) {
                        // Ignore name
                    } else if (line.startsWith("Kd ")) {
                        result.setDiffuseColor(readMtlColor(line.substring(3).trim()));
                    } else if (line.startsWith("Ks ")) {
                        result.setSpecularColor(readMtlColor(line.substring(3).trim()));
                    } else if (line.startsWith("Ns ")) {
                        result.setSpecularPower(Double.parseDouble(line.substring(3).trim()));
                    } else if ((textureImage = tryCreateTextureMap(line, "map_Kd", materialData.getTileSize(), materialData.getBaseDirectory(), mappingConfig)) != null) {
                        result.setDiffuseMap(textureImage);
                    } else if ((textureImage = tryCreateTextureMap(line, "map_Ks", materialData.getTileSize(), materialData.getBaseDirectory(), mappingConfig)) != null) {
                        result.setSpecularMap(textureImage);
                    } else if ((textureImage = tryCreateTextureMap(line, "bump", materialData.getTileSize(), materialData.getBaseDirectory(), mappingConfig)) != null) {
                        result.setBumpMap(textureImage);
                    } else if (line.startsWith("d ")) {
                        // d factor
                        // d -halo factor
                        String[] split = line.substring(2).trim().split(" +");
                        float factor;
                        if (split.length == 1) {
                            factor = Float.parseFloat(split[0]);
                        } else {
                            factor = Float.parseFloat(split[1]);
                        }
                        Color color = result.getDiffuseColor();
                        if (color != null) {
                            result.setDiffuseColor(color.deriveColor(0, 1, 1, factor));
                        }
                        // Specular color doesn't seem to look different if alpha value is added, as of JavaFX 16
                    } else {
                        log.trace("Material line ignored for material '" + materialData.getName() + "': '" + line + "'");
                    }
                } catch (Exception e) {
                    throw new IOException("Failed in line " + lineNo + ": " + line, e);
                }
            }
        }

        return result;
    }

    public static PhongMaterial buildMaterial_Lax(MaterialData materialData, MaterialMapping mappingConfig) {
        try {
            return buildMaterial_Strict(materialData, mappingConfig);
        } catch (IOException e) {
            log.error("Error while configuring material", e);
        }
        return new PhongMaterial();
    }


    public static Mesh buildMesh(MeshData meshData) {
        TriangleMesh result = new TriangleMesh();
        List<Float> vertices = meshData.getVertices();
        float[] verticesArray = ArrayUtils.toPrimitiveFloatArray(vertices);

        List<Float> uvs = meshData.getTexCoords();
        float[] uvsArray = ArrayUtils.toPrimitiveFloatArray(uvs);

        List<Integer> faces = meshData.getFaces();
        int[] facesArray = ArrayUtils.toPrimitiveIntArray(faces);

        result.getPoints().setAll(verticesArray);
        result.getTexCoords().setAll(uvsArray);
        result.getFaces().setAll(facesArray);

        // Use normals if they are provided
        Optional<FaceNormalsData> oFaceNormalsData = meshData.getOFaceNormalsData();
        if (oFaceNormalsData.isPresent()) {
            FaceNormalsData faceNormalsData = oFaceNormalsData.get();
            List<Float> normals = faceNormalsData.getNormals();
            List<Integer> faceNormals = faceNormalsData.getFaceNormals();
            int[] faceNormalsArray = ArrayUtils.toPrimitiveIntArray(faceNormals);
            float[] normalsArray = ArrayUtils.toPrimitiveFloatArray(normals);
            int[] smGroups = SmoothingGroups.calcSmoothGroups(result, facesArray, faceNormalsArray, normalsArray);
            result.getFaceSmoothingGroups().setAll(smGroups);
        } else {
            List<Integer> smoothingGroups = meshData.getSmoothingGroups();
            int[] smoothingGroupsArray = ArrayUtils.toPrimitiveIntArray(smoothingGroups);
            result.getFaceSmoothingGroups().setAll(smoothingGroupsArray);
        }

        log.trace(
            "Created mesh '" + meshData.getName() + "' of " + result.getPoints().size() / result.getPointElementSize() + " vertices, "
            + result.getTexCoords().size() / result.getTexCoordElementSize() + " uvs, "
            + result.getFaces().size() / result.getFaceElementSize() + " faces, "
            + result.getFaceSmoothingGroups().size() + " smoothing groups");

        return result;
    }

    public static MeshView buildMeshView(MeshData meshData) throws IOException {
        MeshView meshView = new MeshView();
        meshView.setId(meshData.getName());
        Mesh mesh = buildMesh(meshData);
        meshView.setMesh(mesh);
        meshView.setCullFace(CullFace.BACK);
        return meshView;
    }

    public static Collection<MeshView> buildMeshViews(Collection<MeshData> meshes,
            Map<String, MaterialData> meshNamesToMaterials, MaterialMapping mappingConfig,
            boolean failOnError) throws IOException {
        Collection<MeshView> result = new ArrayList<>();
        for (MeshData meshData : meshes) {
            MeshView meshView = FxMeshBuilder.buildMeshView(meshData);
            MaterialData materialData = meshNamesToMaterials.get(meshData.getName());
            if (failOnError) {
                meshView.setMaterial(buildMaterial_Strict(materialData, mappingConfig));
            } else {
                meshView.setMaterial(buildMaterial_Lax(materialData, mappingConfig));
            }
            result.add(meshView);
        }
        return result;
    }
}
