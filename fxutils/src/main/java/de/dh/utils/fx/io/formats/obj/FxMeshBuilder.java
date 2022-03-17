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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dh.cad.architect.utils.vfs.IDirectoryLocator;
import de.dh.cad.architect.utils.vfs.IResourceLocator;
import de.dh.utils.fx.Vector2D;
import de.dh.utils.fx.io.formats.obj.MeshData.FaceNormalsData;
import de.dh.utils.fx.io.formats.obj.ParserUtils.TokenIterator;
import de.dh.utils.fx.io.utils.IntegerArrayList;
import de.dh.utils.fx.io.utils.SmoothingGroups;
import javafx.geometry.Dimension2D;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.Material;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.Mesh;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape3D;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Scale;

/**
 * Class for building JavaFX {@link MeshView} objects from {@code .obj} and {@code .mtl} file data which
 * are given in the form of {@link MeshData} and {@link RawMaterialData} instances.
 *
 * Common JavaFX {@code .obj} file importers in the internet build JavaFX {@link Material} instances directly from the
 * entries in the material library ({@code .mtl}) file and {@link Mesh} objects directly from the object
 * entries in the object ({@code .obj}) file.
 * But this typical approach has three main drawbacks:
 * <ul>
 * <li>
 * When transporting the object properties directly via JavaFX {@link Material} / {@link MeshView} instances,
 * overriding of materials in inherited objects, like exchanging the color of the surface of objects, cannot
 * be implemented in a straight-forward way. It is better to have a format-independent in-memory-model for
 * mesh data and material with an explicit mapping of meshes to materials.
 * </li>
 * <li>
 * The typical object reader/writer code is JavaFX specific and could not be reused for other 3D frontend libraries.
 * </li>
 * <li>
 * There are properties in the material specification which actually would need to be translated into properties of
 * the final {@link MeshView} object, e.g. the {@code d} command in the material file, which needs to be translated
 * to the {@link MeshView#getOpacity() opacity} of the mesh view.
 * Due to a current limitation of JavaFX (as of version 16), the {@link MeshView#getOpacity() opacity property} is not
 * supported for 3D objects, instead, we are forced to "hack" the opacity using the alpha value of the diffuse color and
 * specular color. So this third argument doesn't actually apply in the current implementation but could maybe become
 * relevant in the future for other properties which need to be set on the mesh instead on the material itself.
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
            log.debug("Loaded image from " + fileLocator);
            return result;
        } catch (IOException e) {
            throw new IOException("Error loading image from " + fileLocator, e);
        }
    }

    public static void configureMaterial(Shape3D shape, RawMaterialData materialData, Optional<Vector2D> oSurfaceSize, boolean failOnError) throws IOException {
        PhongMaterial material = new PhongMaterial(Color.WHITE);
        if (materialData != null) {
            int lineNo = 0;
            for (String line : materialData.getLines()) {
                lineNo++;
                try {
                    if (line.isEmpty() || line.startsWith("#")) {
                        // Ignore comments and empty lines
                    } else if (line.startsWith("newmtl ")) {
                        // Ignore name
                    } else if (line.startsWith("Kd ")) {
                        material.setDiffuseColor(readMtlColor(line.substring(3).trim()));
                    } else if (line.startsWith("Ks ")) {
                        material.setSpecularColor(readMtlColor(line.substring(3).trim()));
                    } else if (line.startsWith("Ns ")) {
                        material.setSpecularPower(Double.parseDouble(line.substring(3).trim()));
                    } else if (line.startsWith("map_Kd ")) {
                        material.setDiffuseColor(Color.WHITE);
                        try {
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
                                    while (ti.moveNext()) {
                                        String t = ti.getCurrentToken();
                                        if ("-s".equals(t)) {
                                            if (!ti.moveNext()) {
                                                log.warn("Invalid arguments for option -s in options string '" + optionsStr + "' for command map_Kd");
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
                                                log.warn("Invalid arguments for option -o in options string '" + optionsStr + "' for command map_Kd");
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
                                        String ignored = t;
                                        while (ti.moveNext()) {
                                            t = ti.getCurrentToken();
                                            if (t.startsWith("-")) {
                                                ti.moveBack();
                                                continue;
                                            }
                                            ignored += " " + t;
                                        }
                                        log.warn("Ignoring option '" + ignored + "' for command map_Kd");
                                    }
                                }
                            }

                            Image textureImage = loadImage(fileName, materialData.getBaseDirectory());
                            double imageWidth = textureImage.getWidth();
                            double imageHeight = textureImage.getHeight();
                            Dimension2D surfaceImageSize = null;

                            if (oSurfaceSize.isPresent()) {
                                // In case we know the final surface size, we'll generate a surface image texture of the final size for our texture.
                                // That surface image texture will contain the actual texture image tiled over its surface.
                                Vector2D surfaceSize = oSurfaceSize.get();
                                surfaceImageSize = new Dimension2D(surfaceSize.getX(), surfaceSize.getY());
                            } else if (sU != 1 || sV != 1 || oU != 0 || oV != 0) {
                                // In case we don't know the surface size, we just generate a surface image with the original texture
                                // size - scaled and moved according to -s and -o settings
                                surfaceImageSize = new Dimension2D(imageWidth, imageHeight);
                            }

                            if (surfaceImageSize != null) {
                                Rectangle rectangle = new Rectangle(surfaceImageSize.getWidth(), surfaceImageSize.getHeight()); // Size of the surface in real coordinates (for example: 6x3 m)
                                ImagePattern pattern = new ImagePattern(textureImage, oU, oV, imageWidth * sU, imageHeight * sV, false); // Size of the texture on the surface in real coordinates (for example: 1x1 m)
                                rectangle.setFill(pattern);
                                rectangle.setStrokeWidth(0);
                                material.setDiffuseMap(rectangle.snapshot(new SnapshotParameters(), null));
                            } else {
                                material.setDiffuseMap(textureImage);
                            }
                        } catch (IOException e) {
                            String msg = "Unable to load image for diffuse map";
                            if (failOnError) {
                                throw new IOException(msg, e);
                            } else {
                                log.error(msg, e);
                            }
                        }
                        // TODO: Other map_ commands
//                        material.setSelfIlluminationMap(loadImage(line.substring("map_Kd ".length())));
//                        material.setSpecularColor(Color.WHITE);
//                    } else if (line.startsWith("illum ")) {
//                        int illumNo = Integer.parseInt(line.substring("illum ".length()));
/*
                        0    Color on and Ambient off
                        1    Color on and Ambient on
                        2    Highlight on
                        3    Reflection on and Ray trace on
                        4    Transparency: Glass on
                             Reflection: Ray trace on
                        5    Reflection: Fresnel on and Ray trace on
                        6    Transparency: Refraction on
                             Reflection: Fresnel off and Ray trace on
                        7    Transparency: Refraction on
                             Reflection: Fresnel on and Ray trace on
                        8    Reflection on and Ray trace off
                        9    Transparency: Glass on
                             Reflection: Ray trace off
                        10   Casts shadows onto invisible surfaces
*/
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
                        Color color = material.getDiffuseColor();
                        if (color != null) {
                            material.setDiffuseColor(color.deriveColor(0, 1, 1, factor));
                        }
                        // Specular color doesn't seem to look different if alpha value is added, as of JavaFX 16
                    } else {
                        log.debug("Material line ignored for material '" + materialData.getName() + "': '" + line + "'");
                    }
                } catch (Exception e) {
                    String msg = "Failed in line " + lineNo + ": " + line;
                    if (failOnError) {
                        throw new IOException(msg, e);
                    } else {
                        log.error(msg, e);
                    }
                }
            }
        }

        shape.setMaterial(material);
    }

    public static Mesh buildMesh(MeshData meshData) {
        TriangleMesh result = new TriangleMesh();
        float[] vertices = meshData.getVertices().toFloatArray();
        float[] uvs = meshData.getUvs().toFloatArray();
        int[] faces = meshData.getFaces().toIntArray();
        result.getPoints().setAll(vertices);
        result.getTexCoords().setAll(uvs);
        result.getFaces().setAll(faces);

        // Use normals if they are provided
        Optional<FaceNormalsData> oFaceNormalsData = meshData.getOFaceNormalsData();
        if (oFaceNormalsData.isPresent()) {
            FaceNormalsData faceNormalsData = oFaceNormalsData.get();
            float[] normals = faceNormalsData.getNormals().toFloatArray();
            IntegerArrayList faceNormals = faceNormalsData.getFaceNormals();
            int[] newFaces = faces;
            int[] newFaceNormals = faceNormals.toIntArray();
            int[] smGroups = SmoothingGroups.calcSmoothGroups(result, newFaces, newFaceNormals, normals);
            result.getFaceSmoothingGroups().setAll(smGroups);
        } else {
            result.getFaceSmoothingGroups().setAll(meshData.getSmoothingGroups().toIntArray());
        }

        log.debug(
            "Created mesh '" + meshData.getName() + "' of " + result.getPoints().size() / result.getPointElementSize() + " vertices, "
            + result.getTexCoords().size() / result.getTexCoordElementSize() + " uvs, "
            + result.getFaces().size() / result.getFaceElementSize() + " faces, "
            + result.getFaceSmoothingGroups().size() + " smoothing groups");

        return result;
    }

    public static MeshView buildMeshView(MeshData meshData) throws IOException {
        MeshView meshView = new MeshView();
        meshView.setId(meshData.getId());
        Mesh mesh = buildMesh(meshData);
        meshView.setMesh(mesh);
        meshView.setCullFace(CullFace.NONE);

        // JavaFX uses LHS:
        // Z coordinate grows in direction away from the observer
        // Y coordinate grows to the bottom
        // OBJ format uses RHS:
        // Z grows in direction to the observer
        // Y grows to the top
        // Therefore, we mirror by Z and Y to correct the object coordinates
        Scale objToJavaFX = new Scale(1, -1, -1, 0, 0, 0);
        meshView.getTransforms().add(objToJavaFX);
        return meshView;
    }

    public static Collection<MeshView> buildMeshViews(Collection<MeshData> meshes, Map<String, RawMaterialData> meshIdsToMaterials, boolean failOnError) throws IOException {
        Collection<MeshView> result = new ArrayList<>();
        for (MeshData meshData : meshes) {
            MeshView meshView = FxMeshBuilder.buildMeshView(meshData);
            RawMaterialData materialData = meshIdsToMaterials.get(meshData.getId());
            configureMaterial(meshView, materialData, Optional.empty(), failOnError);
            result.add(meshView);
        }
        return result;
    }
}
