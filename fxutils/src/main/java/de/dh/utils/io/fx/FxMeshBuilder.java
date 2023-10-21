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
import de.dh.utils.Vector2D;
import de.dh.utils.io.MeshData;
import de.dh.utils.io.MeshData.FaceNormalsData;
import de.dh.utils.io.ObjData;
import de.dh.utils.io.SmoothingGroups;
import de.dh.utils.io.obj.ParserUtils;
import de.dh.utils.io.obj.ParserUtils.TokenIterator;
import de.dh.utils.io.obj.RawMaterialData;
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
 * it is hard to implement an overriding semantics of materials in inherited objects, like exchanging the
 * color of the surface of objects.
 * Therefore, we use a format-independent in-memory-model for mesh data and material with an explicit mapping
 * of meshes to materials.
 * </li>
 * <li>
 * The typical object reader/writer code you can find in internet is JavaFX specific and could not be reused
 * for other 3D frontend libraries.
 * </li>
 * <li>
 * There are properties in the material library specification (.mtl file) which would need to be translated
 * into properties of the final {@link MeshView} object in JavaFX, e.g. the {@code d} command in the material file,
 * which needs to be translated to the {@link MeshView#getOpacity() opacity} of the mesh view.
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
            log.trace("Loaded image from " + fileLocator);
            return result;
        } catch (IOException e) {
            throw new IOException("Error loading image from " + fileLocator, e);
        }
    }

    public static void configureMaterial_Strict(Shape3D shape, RawMaterialData materialData, Optional<Vector2D> oSurfaceSize) throws IOException {
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
                                // In case we know the final surface size, we'll generate a surface image texture of the final size to fit best without the
                                // need of scaling.
                                Vector2D surfaceSize = oSurfaceSize.get();
                                surfaceImageSize = new Dimension2D(surfaceSize.getX(), surfaceSize.getY());
                            } else if (sU != 1 || sV != 1 || oU != 0 || oV != 0) {
                                // In case we don't know the surface size, we just generate a surface image with the original texture
                                // size - scaled and moved according to -s and -o settings
                                surfaceImageSize = new Dimension2D(imageWidth, imageHeight);
                            }

                            if (surfaceImageSize != null) {
                                Rectangle rectangle = new Rectangle(surfaceImageSize.getWidth(), surfaceImageSize.getHeight()); // Size of the surface in real coordinates (for example: 6x3 m)
                                // That surface image texture will contain the actual texture image tiled over its surface.
                                ImagePattern pattern = new ImagePattern(textureImage, oU, oV, imageWidth * sU, imageHeight * sV, false); // Size of the texture on the surface in real coordinates (for example: 1x1 m)
                                rectangle.setFill(pattern);
                                rectangle.setStrokeWidth(0);
                                material.setDiffuseMap(rectangle.snapshot(new SnapshotParameters(), null));
                            } else {
                                material.setDiffuseMap(textureImage);
                            }
                        } catch (IOException e) {
                            throw new IOException("Unable to load image for diffuse map", e);
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
                        log.trace("Material line ignored for material '" + materialData.getName() + "': '" + line + "'");
                    }
                } catch (Exception e) {
                    throw new IOException("Failed in line " + lineNo + ": " + line, e);
                }
            }
        }

        shape.setMaterial(material);
    }

    public static void configureMaterial_Lax(Shape3D shape, RawMaterialData materialData, Optional<Vector2D> oSurfaceSize) {
        try {
            configureMaterial_Strict(shape, materialData, oSurfaceSize);
        } catch (IOException e) {
            log.error("Error whil configuring material", e);
        }
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

    public static Collection<MeshView> buildMeshViews(Collection<MeshData> meshes, Map<String, RawMaterialData> meshNamesToMaterials, boolean failOnError) throws IOException {
        Collection<MeshView> result = new ArrayList<>();
        for (MeshData meshData : meshes) {
            MeshView meshView = FxMeshBuilder.buildMeshView(meshData);
            RawMaterialData materialData = meshNamesToMaterials.get(meshData.getName());
            if (failOnError) {
                configureMaterial_Strict(meshView, materialData, Optional.empty());
            } else {
                configureMaterial_Lax(meshView, materialData, Optional.empty());
            }
            result.add(meshView);
        }
        return result;
    }
}
