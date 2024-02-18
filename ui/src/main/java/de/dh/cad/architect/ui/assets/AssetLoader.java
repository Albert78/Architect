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

import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dh.cad.architect.model.assets.AbstractAssetDescriptor;
import de.dh.cad.architect.model.assets.AbstractModelResource;
import de.dh.cad.architect.model.assets.AssetRefPath;
import de.dh.cad.architect.model.assets.MaterialSetDescriptor;
import de.dh.cad.architect.model.assets.MeshConfiguration;
import de.dh.cad.architect.model.assets.MtlModelResource;
import de.dh.cad.architect.model.assets.ObjModelResource;
import de.dh.cad.architect.model.assets.SupportObjectDescriptor;
import de.dh.cad.architect.model.assets.ThreeDModelResource;
import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.ui.assets.AssetManager.AssetLocation;
import de.dh.cad.architect.ui.view.libraries.ImageLoadOptions;
import de.dh.cad.architect.utils.vfs.IDirectoryLocator;
import de.dh.cad.architect.utils.vfs.IPathLocator;
import de.dh.cad.architect.utils.vfs.IResourceLocator;
import de.dh.utils.Vector2D;
import de.dh.utils.fx.BoxMesh;
import de.dh.utils.fx.ImageUtils;
import de.dh.utils.io.fx.FxMeshBuilder;
import de.dh.utils.io.obj.MtlLibraryIO;
import de.dh.utils.io.obj.ObjReader;
import de.dh.utils.io.obj.ObjReader.ObjDataRaw;
import de.dh.utils.io.obj.RawMaterialData;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Shape3D;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;

public class AssetLoader {
    public static class ImportResource {
        protected final IResourceLocator mResourceLocator;
        protected final String mTargetFileName;

        public ImportResource(IResourceLocator resourceLocator, String targetFileName) {
            mResourceLocator = resourceLocator;
            mTargetFileName = targetFileName;
        }

        public IResourceLocator getResourceLocator() {
            return mResourceLocator;
        }

        public String getTargetFileName() {
            return mTargetFileName;
        }
    }

    private static Logger log = LoggerFactory.getLogger(AssetLoader.class);

    public static final String BROKEN_IMAGE_SMALL = "broken-image-small.png";
    public static final String BROKEN_IMAGE_BIG = "broken-image-big.png";
    public static final String SUPPORT_OBJECT_PLACEHOLDER_PLAN_VIEW_IMAGE = "support-object-placeholder-plan-view.png";
    public static final String SUPPORT_OBJECT_PLACEHOLDER_ICON_IMAGE = "support-object-placeholder-icon.png";
    public static final String MATERIAL_SET_PLACEHOLDER_ICON_IMAGE = "materialset-placeholder-icon.png";
    public static final String MATERIAL_PLACEHOLDER_TEXTURE = "material-placeholder-texture.png";

    public static final String TEMPLATE_MATERIAL_SET_ICON_IMAGE = MATERIAL_SET_PLACEHOLDER_ICON_IMAGE;
    public static final String TEMPLATE_MATERIAL_LIBRARY = "template-material-library.mtl";

    public static final String TEMPLATE_SUPPORT_OBJECT_ICON_IMAGE = SUPPORT_OBJECT_PLACEHOLDER_ICON_IMAGE;
    public static final String TEMPLATE_SUPPORT_OBJECT_PLAN_VIEW_IMAGE = SUPPORT_OBJECT_PLACEHOLDER_PLAN_VIEW_IMAGE;
    public static final String TEMPLATE_SUPPORT_OBJECT_MODEL = "template-support-object-model.obj";

    protected final AssetManager mAssetManager;
    protected final Collection<String> mLoggedMessages = new TreeSet<>(); // To avoid logging the same message multiple times

    public AssetLoader(AssetManager assetManager) {
        mAssetManager = assetManager;
    }

    public static AssetLoader build(AssetManager assetManager) {
        return new AssetLoader(assetManager);
    }

    public AssetManager getAssetManager() {
        return mAssetManager;
    }

    //////////////////////////////////////////////////////// Resource access ////////////////////////////////////////////////////////////////////

    protected static URL getResource(String localResourceName) {
        return AssetLoader.class.getResource(localResourceName);
    }

    protected static Image loadImageFromResource(String localResourceName, Optional<ImageLoadOptions> oLoadOptions) {
        try (InputStream is = getResource(localResourceName).openStream()) {
            return oLoadOptions
                    .map(loadOptions -> new Image(is, loadOptions.getWidth(), loadOptions.getHeight(), loadOptions.isPreserveRatio(), loadOptions.isSmooth()))
                    .orElse(new Image(is));
        } catch (IOException e) {
            throw new RuntimeException("Image resource '" + localResourceName + "' could not be loaded", e);
        }
    }

    public static Image loadBrokenImageSmall(Optional<ImageLoadOptions> oLoadOptions) {
        return loadImageFromResource(BROKEN_IMAGE_SMALL, oLoadOptions);
    }

    public static Image loadBrokenImageBig(Optional<ImageLoadOptions> oLoadOptions) {
        return loadImageFromResource(BROKEN_IMAGE_BIG, oLoadOptions);
    }

    public static Image loadSupportObjectPlaceholderIconImage(Optional<ImageLoadOptions> oLoadOptions) {
        return loadImageFromResource(SUPPORT_OBJECT_PLACEHOLDER_ICON_IMAGE, oLoadOptions);
    }

    public static Image loadMaterialSetPlaceholderIconImage(Optional<ImageLoadOptions> oLoadOptions) {
        return loadImageFromResource(MATERIAL_SET_PLACEHOLDER_ICON_IMAGE, oLoadOptions);
    }

    public static Image loadMaterialPlaceholderTextureImage(Optional<ImageLoadOptions> oLoadOptions) {
        return loadImageFromResource(MATERIAL_PLACEHOLDER_TEXTURE, oLoadOptions);
    }

    public static Image loadSupportObjectPlaceholderPlanViewImage(Optional<ImageLoadOptions> oLoadOptions) {
        return loadImageFromResource(SUPPORT_OBJECT_PLACEHOLDER_PLAN_VIEW_IMAGE, oLoadOptions);
    }

    public static ThreeDObject loadBroken3DResource() {
        int width = 100;
        int height = 100;
        int depth = 100;
        MeshView result = new MeshView(BoxMesh.createMesh(width, height, depth));
        PhongMaterial material = new PhongMaterial(Color.RED, loadBrokenImageBig(Optional.of(new ImageLoadOptions(width, height))), null, null, null);
        result.setMaterial(material);
        Length def = Length.ofCM(10);
        return new ThreeDObject(Collections.singleton(result), Optional.empty(), def, def, def);
    }

    public static ThreeDObject loadSupportObjectPlaceholder3DResource() {
        int width = 100;
        int height = 100;
        int depth = 100;
        MeshView result = new MeshView(BoxMesh.createMesh(width, height, depth));
        PhongMaterial material = new PhongMaterial(Color.GREEN);
        result.setMaterial(material);
        Length def = Length.ofCM(10);
        return new ThreeDObject(Collections.singleton(result), Optional.empty(), def, def, def);
    }

    protected Image loadAssetResourceImage(AssetRefPath assetRefPath, String resourceName) throws IOException {
        AssetLocation assetLocation = mAssetManager.resolveAssetLocation(assetRefPath);
        return assetLocation.loadImage(resourceName);
    }

    public Image loadAssetIconImage(AbstractAssetDescriptor descriptor) throws IOException {
        String resourceName = descriptor.getIconImageResourceName();
        AssetRefPath assetRefPath = descriptor.getSelfRef();
        if (StringUtils.isEmpty(resourceName)) {
            throw new FileNotFoundException("No icon resource defined in asset descriptor '" + assetRefPath + "'");
        }
        return loadAssetResourceImage(assetRefPath, resourceName);
    }

    public Image loadSupportObjectPlanViewImage(SupportObjectDescriptor descriptor) throws IOException {
        String resourceName = descriptor.getPlanViewImageResourceName();
        AssetRefPath assetRefPath = descriptor.getSelfRef();
        if (StringUtils.isEmpty(resourceName)) {
            throw new FileNotFoundException("No plan view resource defined in asset descriptor '" + assetRefPath + "'");
        }
        return loadAssetResourceImage(assetRefPath, resourceName);
    }

    public ThreeDObject loadSupportObject3DResource(SupportObjectDescriptor soDescriptor, Optional<Map<String, AssetRefPath>> oOverriddenSurfaceMaterialRefs) throws IOException {
        AssetRefPath assetRefPath = soDescriptor.getSelfRef();
        AssetLocation assetLocation = mAssetManager.resolveAssetLocation(assetRefPath);
        AbstractModelResource model = soDescriptor.getModel();
        if (model == null) {
            throw new NullPointerException("3D model is not assigned in descriptor <" + assetRefPath + ">");
        }
        try {
            if (model instanceof ObjModelResource omr) {
                Map<String, MeshConfiguration> meshNamesToMeshConfigurations = soDescriptor.getMeshNamesToMeshConfigurations();

                ObjDataRaw objData = loadObjModelData(assetLocation, omr);
                Map<String, String> defaultMeshNamesToMaterialNames = objData.getMeshNamesToMaterialNames();

                // Lookup material ref paths from SO descriptor by the mesh names given in obj file
                Map<String, AssetRefPath> defaultMeshNamesToMaterialRefs = defaultMeshNamesToMaterialNames
                    .entrySet()
                    .stream()
                    // Because of a bug in JDK, the following code fails because of a potential null value;
                    // Collectors.toMap needs both the map's key and value to be non-null:: https://bugs.openjdk.org/browse/JDK-8148463
//                    .collect(Collectors.toMap(entry -> entry.getKey(), entry -> {
//                        String meshName = entry.getKey();
//                        MeshConfiguration mc = meshNamesToMeshConfigurations.get(meshName);
//                        if (mc == null) {
//                            return null;
//                        }
//                        return mc.getMaterialAssignment();
//                    }));
                    // Work around
                    .collect(HashMap::new, (m, entry) -> {
                        String meshName = entry.getKey();
                        MeshConfiguration mc = meshNamesToMeshConfigurations.get(meshName); // From SO descriptor
                        AssetRefPath materialAssignment = mc == null
                                        ? null
                                        : mc.getMaterialAssignment();
                        m.put(meshName, materialAssignment);
                    }, HashMap::putAll);

                Map<String, AssetRefPath> meshNamesToMaterialRefs = oOverriddenSurfaceMaterialRefs
                                .map(om -> mergeMaterials(defaultMeshNamesToMaterialRefs, om))
                                .orElse(defaultMeshNamesToMaterialRefs);

                Map<String, RawMaterialData> meshNamesToMaterials;
                try {
                    meshNamesToMaterials = loadMaterialData(meshNamesToMaterialRefs);
                } catch (IOException e) {
                    log.error("Error loading overridden materials", e);
                    meshNamesToMaterials = Collections.emptyMap();
                }
                // If we neither have a material assignment in the SO descriptor, nor an overridden material, we fall back to the default
                // materials defined in asset manager.
                // Default materials don't have an asset ref path, so we have to use defaultMeshNamesToMaterialNames for the lookup.
                for (Entry<String, RawMaterialData> entry : new ArrayList<>(meshNamesToMaterials.entrySet())) {
                    RawMaterialData material = entry.getValue();
                    if (material == null) {
                        String meshName = entry.getKey();
                        String materialName = defaultMeshNamesToMaterialNames.get(meshName);
                        meshNamesToMaterials.put(meshName, mAssetManager.getDefaultMaterials().get(materialName));
                    }
                }

                Collection<MeshView> meshes = FxMeshBuilder.buildMeshViews(objData.getMeshes(), meshNamesToMaterials, false);
                Optional<Transform> oTrans = createTransform(omr.getModelRotationMatrix());
                return new ThreeDObject(meshes, oTrans, soDescriptor.getWidth(), soDescriptor.getHeight(), soDescriptor.getDepth());
            } else {
                throw new NotImplementedException("Unable to load object 3D model of class <" + model.getClass() + "> in descriptor <" + assetRefPath + ">");
            }
        } catch (IOException e) {
            String msg = "Unable to load 3D model for support object descriptor <" + soDescriptor + ">";
            throw new IOException(msg, e);
        }
    }

    protected String importAssetResourceImage(AssetRefPath assetRefPath, Image image, String imageName) throws IOException {
        if (!imageName.endsWith("." + AssetManager.STORE_IMAGE_EXTENSION)) {
            imageName = imageName + "." + AssetManager.STORE_IMAGE_EXTENSION;
        }
        AssetLocation assetLocation = mAssetManager.resolveAssetLocation(assetRefPath);
        assetLocation.saveImage(image, imageName);
        return imageName;
    }

    public String importAssetResourceImage(AssetRefPath assetRefPath, IResourceLocator sourceImageResource, Optional<String> oOverriddenName) throws IOException {
        AssetLocation assetLocation = mAssetManager.resolveAssetLocation(assetRefPath);
        String imageName = oOverriddenName.orElse(sourceImageResource.getFileName());
        assetLocation.importResource(sourceImageResource, imageName);
        return imageName;
    }

    public static enum ThreeDResourceImportMode {
        /**
         * Only the given {@code .obj} file will be imported, nothing else.
         * This mode makes sense if the 3D model only consists of a single file.
         */
        ObjFile,

        /**
         * The whole directory of the given {@code .obj} file will be imported.
         * This mode makes sense if the 3D model includes more files like {@code .mtl} files,
         * images, a license file etc.
         * In this case, all those files must be located in a single directory and that directory should only
         * contain the files for that 3D model.
         */
        Directory
    }

    // We assume that the whole structure is located in the same directory
    protected ThreeDModelResource importAssetResourceObj(AssetRefPath assetRefPath, IResourceLocator sourceObjResourceLocator,
        ThreeDResourceImportMode importMode, Optional<String> oOverriddenName) throws IOException {
        AssetLocation resourcesDirectory = mAssetManager.resolveAssetLocation(assetRefPath).resolveResourcesDirectory();
        String fileName = oOverriddenName.orElse(sourceObjResourceLocator.getFileName());
        switch (importMode) {
        case Directory:
            resourcesDirectory.importResourceDirectory(sourceObjResourceLocator.getParentDirectory());
            break;
        case ObjFile:
            resourcesDirectory.importResource(sourceObjResourceLocator, fileName);
            break;
        default:
            throw new NotImplementedException("Import mode '" + importMode + "' is not implemented");
        }
        Path modelPath = Paths.get(fileName);
        return new ObjModelResource(modelPath);
    }

    protected void clearModelFolder(AssetRefPath assetRefPath) throws IOException {
        IDirectoryLocator directory = getModelDirectory(assetRefPath);
        directory.clean();
    }

    /**
     * Deletes a file of an asset.
     * @param assetRefPath Path of the asset.
     * @param fileNameOrPath Relative path or filename resolved against the asset's base directory.
     */
    protected void deleteAssetResource(AssetRefPath assetRefPath, String fileNameOrPath) throws IOException {
        if (StringUtils.isEmpty(fileNameOrPath)) {
            return;
        }
        AssetLocation assetLocation = mAssetManager.resolveAssetLocation(assetRefPath);
        IResourceLocator resourceLocator = assetLocation.resolveResource(fileNameOrPath);
        resourceLocator.delete();
    }

    public void importAssetIconImage(AbstractAssetDescriptor descriptor, Image image, String imageName) throws IOException {
        AssetRefPath assetRefPath = descriptor.getSelfRef();
        deleteAssetResource(assetRefPath, descriptor.getIconImageResourceName());
        imageName = importAssetResourceImage(assetRefPath, image, imageName);
        descriptor.setIconImageResourceName(imageName);
        mAssetManager.saveAssetDescriptor(descriptor);
    }

    public void importSupportObjectPlanViewImage(SupportObjectDescriptor descriptor, Image image, String imageName) throws IOException {
        AssetRefPath assetRefPath = descriptor.getSelfRef();
        deleteAssetResource(assetRefPath, descriptor.getPlanViewImageResourceName());
        imageName = importAssetResourceImage(assetRefPath, image, imageName);
        descriptor.setPlanViewImageResourceName(imageName);
        mAssetManager.saveSupportObjectDescriptor(descriptor);
    }

    public void importAssetIconImage(AbstractAssetDescriptor descriptor, IResourceLocator imageResource, Optional<String> oOverriddenName) throws IOException {
        AssetRefPath assetRefPath = descriptor.getSelfRef();
        deleteAssetResource(assetRefPath, descriptor.getIconImageResourceName());
        String relativeName = importAssetResourceImage(assetRefPath, imageResource, oOverriddenName);
        descriptor.setIconImageResourceName(relativeName);
        mAssetManager.saveAssetDescriptor(descriptor);
    }

    public void importSupportObjectPlanViewImage(SupportObjectDescriptor descriptor, IResourceLocator imageResource, Optional<String> oOverriddenName) throws IOException {
        AssetRefPath assetRefPath = descriptor.getSelfRef();
        deleteAssetResource(assetRefPath, descriptor.getPlanViewImageResourceName());
        String relativeName = importAssetResourceImage(assetRefPath, imageResource, oOverriddenName);
        descriptor.setPlanViewImageResourceName(relativeName);
        mAssetManager.saveSupportObjectDescriptor(descriptor);
    }

    public void importSupportObject3DViewObjResource(SupportObjectDescriptor descriptor, IResourceLocator sourceObjResourceLocator,
        Optional<float[][]> oModelRotation, ThreeDResourceImportMode importMode, Optional<String> oOverriddenName) throws IOException {
        AssetRefPath assetRefPath = descriptor.getSelfRef();
        clearModelFolder(assetRefPath);
        ThreeDModelResource modelResource = importAssetResourceObj(assetRefPath, sourceObjResourceLocator, importMode, oOverriddenName);
        if (oModelRotation.isPresent()) {
            modelResource.setModelRotationMatrix(oModelRotation.get());
        } else {
            modelResource.setModelRotationMatrix(null);
        }
        descriptor.setModel(modelResource);

        if (importMode == ThreeDResourceImportMode.Directory) {
            // Convert material library file(s) to their own local material set
            importLocalMaterialSets(descriptor);
        }

        mAssetManager.saveSupportObjectDescriptor(descriptor);
    }

    public void updateSupportObject3DViewObjRotation(SupportObjectDescriptor descriptor, Transform rotation) {
        AssetRefPath assetRefPath = descriptor.getSelfRef();

        AbstractModelResource model = descriptor.getModel();
        if (!(model instanceof ThreeDModelResource)) {
            throw new NullPointerException("3D model is not assigned in descriptor <" + assetRefPath + ">");
        }
        ThreeDModelResource tdmr = (ThreeDModelResource) model;
        float[][] rotationMatrix = AssetLoader.createRotationMatrix(rotation);
        tdmr.setModelRotationMatrix(rotationMatrix);
    }

    public boolean importLocalMaterialSets(SupportObjectDescriptor soDescriptor) throws IOException {
        AssetRefPath soRefPath = soDescriptor.getSelfRef();
        AssetLocation soAssetLocation = mAssetManager.resolveAssetLocation(soRefPath);
        AbstractModelResource soModel = soDescriptor.getModel();
        if (!(soModel instanceof ObjModelResource)) {
            System.out.println("Skipping non-object model of descriptor " + soRefPath);
            return false;
        }

        try {
            ObjDataRaw objDataRaw = loadObjModelData(soAssetLocation, (ObjModelResource) soModel);

            Map<String, String> meshNamesToMaterialNames = objDataRaw.getMeshNamesToMaterialNames();

            Map<String, AssetRefPath> materialNamesToMaterialSetRefPaths = new HashMap<>();

            for (IPathLocator mtlFileLocator : soAssetLocation.resolveResourcesDirectory().getDirectoryLocator().list(pl -> { return pl.getFileName().endsWith(".mtl"); })) {
                materialNamesToMaterialSetRefPaths.putAll(importLocalMaterialSetFromMtlResource((IResourceLocator) mtlFileLocator, soDescriptor));
            }

            Map<String, MeshConfiguration> meshNamesToMeshConfigurations = soDescriptor.getMeshNamesToMeshConfigurations();
            for (Entry<String, String> entry : meshNamesToMaterialNames.entrySet()) {
                String meshName = entry.getKey();
                String materialName = entry.getValue();
                MeshConfiguration mc = new MeshConfiguration(meshName);
                AssetRefPath materialSetRefPath = materialNamesToMaterialSetRefPaths.get(materialName);
                if (materialSetRefPath != null) {
                    mc.setMaterialAssignment(materialSetRefPath);
                } // Else, we'll leave the material assignment empty -> Fallback to default material
                meshNamesToMeshConfigurations.put(meshName, mc);
            }

            mAssetManager.saveAssetDescriptor(soDescriptor);
            return true;
        } catch (Exception e) {
            throw new RuntimeException("Unable to import local material sets", e);
        }
    }

    protected Map<String, AssetRefPath> importLocalMaterialSetFromMtlResource(IResourceLocator mtlFileLocator, SupportObjectDescriptor soDescriptor) throws IOException {
        AssetLocation targetSOAssetLocation = mAssetManager.resolveAssetLocation(soDescriptor.getSelfRef());
        IDirectoryLocator targetSOAssetDirectoryLocator = targetSOAssetLocation.getDirectoryLocator();

        Collection<IResourceLocator> mtlResourcesToDelete = new ArrayList<>(); // Those files are moved ore replaced by another file during migration

        Collection<IResourceLocator> allFilesOfMtlResource = MtlLibraryIO.getAllFiles(mtlFileLocator);
        mtlResourcesToDelete.addAll(allFilesOfMtlResource);

        /////// Create new material set
        MaterialSetDescriptor msDescriptor = mAssetManager.createMaterialSet(targetSOAssetLocation.resolveLocalMaterialSetsDirectory());
        AssetRefPath msRefPath = msDescriptor.getSelfRef();

        // Metadata
        msDescriptor.setName(soDescriptor.getName() + " (Materials)");
        msDescriptor.setAuthor(soDescriptor.getAuthor());
        msDescriptor.setOrigin(soDescriptor.getOrigin());
        msDescriptor.setLastModified(LocalDateTime.now());

        // MateriaSet directories
        AssetLocation msAssetLocation = mAssetManager.resolveAssetLocation(msRefPath);
        IDirectoryLocator msDirectory = msAssetLocation.getDirectoryLocator();
        IDirectoryLocator msResourceDirectory = msDirectory.resolveDirectory(AssetManager.RESOURCES_DIRECTORY_NAME);
        msResourceDirectory.mkDirs();

        Map<String, RawMaterialData> materialSet = MtlLibraryIO.readMaterialSet(mtlFileLocator);

        // Move all relevant files for material set
        for (IResourceLocator someMtlFile : allFilesOfMtlResource) {
            try {
                someMtlFile.copyTo(msResourceDirectory);
            } catch (NoSuchFileException e) {
                System.out.println("Unable to copy missing file of MTL library: " + someMtlFile.getAbsolutePath());
            }
        }
        // Register new MTL model in descriptor, cleanup
        MtlModelResource modelResource = new MtlModelResource(Paths.get(mtlFileLocator.getFileName()));
        String oldResourceName = ((MtlModelResource) msDescriptor.getModel()).getRelativePath().getFileName().toString();
        mtlResourcesToDelete.add(msResourceDirectory.resolveResource(oldResourceName));
        msDescriptor.setModel(modelResource);

        Path soIconImagePath = Paths.get(targetSOAssetDirectoryLocator.getAbsolutePath()).resolve(soDescriptor.getIconImageResourceName());

        // As icon image, we'll use the SupportObject's icon image with an overlay "M" at the lower right part
        createIconWithOverlay(soIconImagePath, Paths.get(msDirectory.getAbsolutePath()), "M");

        mAssetManager.saveAssetDescriptor(msDescriptor);
        for (IResourceLocator mtlResourceLocator : mtlResourcesToDelete) {
            String filePath = mtlResourceLocator.getAbsolutePath();
            System.out.println("Trying to delete obsolete file " + filePath);
            try {
                Files.delete(Paths.get(filePath));
            } catch (NoSuchFileException e) {
                System.out.println("Cannot delete associated file of MTL library, file is missing: " + filePath);
            }
        }
        return materialSet
                        .keySet()
                        .stream()
                        .collect(Collectors.toMap(Function.identity(), materialName -> msRefPath.withMaterialName(materialName)));
    }

    protected static void createIconWithOverlay(Path sourceImage, Path targetImage, String overlayText) throws IOException {
        BufferedImage image = ImageIO.read(sourceImage.toFile());
        ImageUtils.addImageOverlay(image, overlayText);
        ImageIO.write(image, "png", targetImage.resolve(AssetManager.ICON_IMAGE_DEFAULT_BASE_NAME + ".png").toFile());
    }

    public MtlModelResource importAssetResourceMtl(AssetRefPath materialSetRefPath, IResourceLocator mtlResourceLocator, Optional<String> oOverriddenName) throws IOException {
        clearModelFolder(materialSetRefPath);
        AssetLocation resourceDirectory = mAssetManager.resolveAssetLocation(materialSetRefPath).resolveResourcesDirectory();
        String fileName = oOverriddenName.orElse(mtlResourceLocator.getFileName());
        resourceDirectory.importResource(mtlResourceLocator, fileName);
        Path modelPath = Paths.get(fileName);
        return new MtlModelResource(modelPath);
    }

    public MtlModelResource importAssetResourceMtl(AssetRefPath materialSetRefPath, Collection<RawMaterialData> materials, String mtlFileName, Collection<ImportResource> additionalResources) throws IOException {
        clearModelFolder(materialSetRefPath);
        AssetLocation resourceDirectory = mAssetManager.resolveAssetLocation(materialSetRefPath).resolveResourcesDirectory();
        resourceDirectory.saveMaterialsToMtl(materials, mtlFileName);
        for (ImportResource resource : additionalResources) {
            resourceDirectory.importResource(resource.getResourceLocator(), resource.getTargetFileName());
        }
        Path modelPath = Paths.get(mtlFileName);
        return new MtlModelResource(modelPath);
    }

    public IDirectoryLocator getModelDirectory(AssetRefPath assetRefPath) throws IOException {
        AssetLocation assetLocation = mAssetManager.resolveAssetLocation(assetRefPath);
        return assetLocation.resolveResourcesDirectory().getDirectoryLocator();
    }

    public IResourceLocator getMtlResource(AssetRefPath materialSetRefPath) throws IOException {
        MaterialSetDescriptor materialSetDescriptor = mAssetManager.loadMaterialSetDescriptor(materialSetRefPath);
        AssetLocation assetLocation = mAssetManager.resolveAssetLocation(materialSetRefPath);
        AbstractModelResource model = materialSetDescriptor.getModel();
        if (model instanceof MtlModelResource mmr) {
            return AssetManager.resolveResourcesModel(assetLocation, mmr);
        } else {
            throw new IllegalArgumentException("Unable to resolve mtl file path for material set ref <" + materialSetRefPath + ">");
        }
    }

    public void importMaterialSetMtlResource(MaterialSetDescriptor descriptor, IResourceLocator mtlResourceLocator, Optional<String> oOverriddenName) throws IOException {
        AssetRefPath materialSetRefPath = descriptor.getSelfRef();
        MtlModelResource modelResource = importAssetResourceMtl(materialSetRefPath, mtlResourceLocator, oOverriddenName);
        descriptor.setModel(modelResource);
        mAssetManager.saveMaterialSetDescriptor(descriptor);
    }

    public void importMaterialSetMtlResource(MaterialSetDescriptor descriptor, Collection<RawMaterialData> materials, String mtlFileName, Collection<ImportResource> additionalResources) throws IOException {
        AssetRefPath materialSetRefPath = descriptor.getSelfRef();
        MtlModelResource modelResource = importAssetResourceMtl(materialSetRefPath, materials, mtlFileName, additionalResources);
        descriptor.setModel(modelResource);
        mAssetManager.saveMaterialSetDescriptor(descriptor);
    }

    public ObjDataRaw loadObjModelData(AssetLocation assetLocation, ObjModelResource model) throws IOException {
        IResourceLocator resourceLocator = AssetManager.resolveResourcesModel(assetLocation, model);
        return loadObjModelData(resourceLocator);
    }

    public ObjDataRaw loadObjModelData(IResourceLocator resourceLocator) throws IOException {
        return ObjReader.readObjRaw(resourceLocator);
    }

    public Map<String, RawMaterialData> loadMaterialData(Map<String, AssetRefPath> materialRefs) throws IOException {
        Map<String, RawMaterialData> result = new HashMap<>();
        for (Entry<String, AssetRefPath> entry : materialRefs.entrySet()) {
            String key = entry.getKey();
            AssetRefPath materialRefPath = entry.getValue();
            RawMaterialData materialData = materialRefPath == null ? null : loadMaterialData(materialRefPath);
            result.put(key, materialData);
        }
        return result;
    }

    public Map<String, RawMaterialData> loadMaterials(AssetRefPath materialSetRefPath) throws IOException {
        Optional<String> oMaterialName = materialSetRefPath.getOMaterialName();
        if (oMaterialName.isPresent()) {
            throw new IllegalArgumentException("Asset ref path '" + materialSetRefPath + "' contains a material name; material set descriptor expected");
        }
        MaterialSetDescriptor materialSetDescriptor = mAssetManager.loadMaterialSetDescriptor(materialSetRefPath);
        AssetLocation assetLocation = mAssetManager.resolveAssetLocation(materialSetRefPath);

        AbstractModelResource model = materialSetDescriptor.getModel();
        if (model == null) {
            return Collections.emptyMap();
        } else if (model instanceof MtlModelResource mmr) {
            try {
                IResourceLocator mtlResource = AssetManager.resolveResourcesModel(assetLocation, mmr);
                return MtlLibraryIO.readMaterialSet(mtlResource);
            } catch (IOException e) {
                String msg = "Unable to load mtl file for material ref path <" + materialSetRefPath + ">";
                if (e instanceof FileNotFoundException) {
                    throw new FileNotFoundException(msg + ": " + e.getMessage());
                }
                throw new IOException(msg, e);
            }
        } else {
            throw new NotImplementedException("Unable to resolve material data <" + model + "> for ref path <" + materialSetRefPath + ">");
        }
    }

    public RawMaterialData loadMaterialData(AssetRefPath materialRefPath) throws IOException {
        Optional<String> oMaterialName = materialRefPath.getOMaterialName();
        String materialName = oMaterialName.orElseThrow(() -> new IllegalArgumentException("Asset ref path '" + materialRefPath + "' doesn't contain a material name"));
        AssetRefPath materialSetRefPath = materialRefPath.withoutMaterialName();
        Map<String, RawMaterialData> materials = loadMaterials(materialSetRefPath);
        RawMaterialData result = materials.get(materialName);
        if (result == null) {
            throw new IOException("Material with name '" + materialName + "' could not be found in material set '" + materialRefPath + "'");
        }
        return result;
    }

    public static Map<String, AssetRefPath> mergeMaterials(Map<String, AssetRefPath> defaultMeshIdsToMaterialNamess, Map<String, AssetRefPath> overriddenMeshIdsToMaterialNames) {
        Map<String, AssetRefPath> result = new HashMap<>();
        for (Entry<String, AssetRefPath> mapping : defaultMeshIdsToMaterialNamess.entrySet()) {
            String meshName = mapping.getKey();
            AssetRefPath overriddenMaterialName = overriddenMeshIdsToMaterialNames.get(meshName);
            result.put(meshName, overriddenMaterialName == null ? mapping.getValue() : overriddenMaterialName);
        }
        return result;
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

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public Image loadSupportObjectIconImage(SupportObjectDescriptor descriptor, boolean fallbackToPlaceholder) {
        try {
            return loadAssetIconImage(descriptor);
        } catch (Exception e) {
            if (fallbackToPlaceholder) {
                logMissingIconImage(descriptor.getSelfRef(), e);
                // Should we return an error resource or fallback to placeholder?
                return loadSupportObjectPlaceholderIconImage(Optional.empty());
            }
            return null;
        }
    }

    public Image loadMaterialSetIconImage(MaterialSetDescriptor descriptor, boolean fallbackToPlaceholder) {
        try {
            return loadAssetIconImage(descriptor);
        } catch (Exception e) {
            if (fallbackToPlaceholder) {
                logMissingIconImage(descriptor.getSelfRef(), e);
                // Should we return an error resource or fallback to placeholder?
                return loadMaterialSetPlaceholderIconImage(Optional.empty());
            }
            return null;
        }
    }

    public <T> Image loadAssetIconImage(T descriptor, boolean fallbackToPlaceholder) {
        if (descriptor instanceof SupportObjectDescriptor soDescriptor) {
            return loadSupportObjectIconImage(soDescriptor, fallbackToPlaceholder);
        } else if (descriptor instanceof MaterialSetDescriptor msDescriptor) {
            return loadMaterialSetIconImage(msDescriptor, fallbackToPlaceholder);
        } else {
            throw new NotImplementedException("Unable to load asset icon image for descriptor <" + descriptor + ">");
        }
    }

    public Image loadSupportObjectPlanViewImage(AssetRefPath supportObjectDescriptorRef, boolean fallbackToPlaceholder) {
        SupportObjectDescriptor descriptor;
        try {
            descriptor = mAssetManager.loadSupportObjectDescriptor(supportObjectDescriptorRef);
        } catch (IOException e) {
            if (fallbackToPlaceholder) {
                logMissingDescriptor(supportObjectDescriptorRef, e);
                return loadSupportObjectPlaceholderPlanViewImage(Optional.empty());
            } else {
                return null;
            }
        }
        return loadSupportObjectPlanViewImage(descriptor, fallbackToPlaceholder);
    }

    public Image loadSupportObjectPlanViewImage(SupportObjectDescriptor descriptor, boolean fallbackToPlaceholder) {
        try {
            return loadSupportObjectPlanViewImage(descriptor);
        } catch (Exception e) {
            if (fallbackToPlaceholder) {
                logMissingPlanViewImage(descriptor.getSelfRef(), e);
                return loadSupportObjectPlaceholderPlanViewImage(Optional.empty());
            } else {
                return null;
            }
        }
    }

    public ThreeDObject loadSupportObject3DObject(AssetRefPath supportObjectDescriptorRef, Optional<Map<String, AssetRefPath>> overriddenSurfaceMaterialRefs, boolean fallbackToPlaceholder) {
        SupportObjectDescriptor descriptor;
        try {
            descriptor = mAssetManager.loadSupportObjectDescriptor(supportObjectDescriptorRef);
        } catch (IOException e) {
            if (fallbackToPlaceholder) {
                logMissingDescriptor(supportObjectDescriptorRef, e);
                return loadSupportObjectPlaceholder3DResource();
            } else {
                return null;
            }
        }
        return loadSupportObject3DObject(descriptor, overriddenSurfaceMaterialRefs, fallbackToPlaceholder);
    }

    public ThreeDObject loadSupportObject3DObject(SupportObjectDescriptor descriptor, Optional<Map<String, AssetRefPath>> overriddenSurfaceMaterialRefs, boolean fallbackToPlaceholder) {
        try {
            return loadSupportObject3DResource(descriptor, overriddenSurfaceMaterialRefs);
        } catch (IOException e) {
            if (fallbackToPlaceholder) {
                logMissingSupportObjectObjectView(descriptor, descriptor.getModel(), e);
                return loadBroken3DResource();
            } else {
                return null;
            }
        }
    }

    public void configureMaterial(Shape3D shape, AssetRefPath materialRefPath, Optional<Vector2D> oSurfaceSize) {
        if (materialRefPath == null) {
            shape.setMaterial(new PhongMaterial(Color.WHITE));
            return;
        }
        try {
            RawMaterialData materialData = loadMaterialData(materialRefPath);
            configureMaterial_Lax(shape, materialData, oSurfaceSize);
        } catch (IOException e) {
            Image placeholder = loadMaterialPlaceholderTextureImage(Optional.empty());
            PhongMaterial material = new PhongMaterial(Color.WHITE);
            material.setDiffuseMap(placeholder);
            shape.setMaterial(material);
        }
    }

    public void configureMaterial_Strict(Shape3D shape, RawMaterialData materialData, Optional<Vector2D> oSurfaceSize) throws IOException {
        FxMeshBuilder.configureMaterial_Strict(shape, materialData, oSurfaceSize);
    }

    public void configureMaterial_Lax(Shape3D shape, RawMaterialData materialData, Optional<Vector2D> oSurfaceSize) {
        FxMeshBuilder.configureMaterial_Lax(shape, materialData, oSurfaceSize);
    }

    protected void logMissingDescriptor(AssetRefPath descriptorRef, Throwable e) {
        String ds = descriptorRef.toString();
        logWarnOnce("Missing: " + ds, "Asset descriptor '" + ds + "' is not available", e);
    }

    protected void logMissingIconImage(AssetRefPath descriptorRef, Throwable e) {
        String ds = descriptorRef.toString();
        logWarnOnce("Missing: " + ds + "-Icon", "Icon image of descriptor '" + ds + "' is not available", e);
    }

    protected void logMissingPlanViewImage(AssetRefPath supportObjectDescriptorRef, Throwable e) {
        String sor = supportObjectDescriptorRef.toString();
        logWarnOnce("Missing: " + sor + "-PlanView", "Plan view image of descriptor '" + sor + "' is not available", e);
    }

    protected void logMissingSupportObjectObjectView(SupportObjectDescriptor descriptor, AbstractModelResource modelResource, Throwable e) {
        String mr = modelResource.toString();
        logWarnOnce("Missing: " + descriptor.getSelfRef() + "-" + mr, "Support object 3D model '" + mr + "' is not available", e);
    }

    protected void logWarnOnce(String uniqueKey, String msg) {
        if (mLoggedMessages.contains(uniqueKey)) {
            return;
        }
        mLoggedMessages.add(uniqueKey);
        log.warn(msg);
    }

    protected void logWarnOnce(String uniqueKey, String msg, Throwable t) {
        if (mLoggedMessages.contains(uniqueKey)) {
            return;
        }
        mLoggedMessages.add(uniqueKey);
        if (t == null) {
            log.warn(msg);
        } else {
            log.warn(msg, t);
        }
    }

    public static Transform createTransformObjToArchitect() {
        return new Rotate(90, Rotate.X_AXIS);
    }

    // Transformation Architect -> JavaFx is in CoordinateUtils
}
