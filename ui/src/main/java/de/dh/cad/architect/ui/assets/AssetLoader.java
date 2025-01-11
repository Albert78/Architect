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

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dh.cad.architect.model.assets.AbstractAssetDescriptor;
import de.dh.cad.architect.model.assets.AbstractModelResource;
import de.dh.cad.architect.model.assets.AssetRefPath;
import de.dh.cad.architect.model.assets.MaterialSetDescriptor;
import de.dh.cad.architect.model.assets.MaterialsModel;
import de.dh.cad.architect.model.assets.MeshConfiguration;
import de.dh.cad.architect.model.assets.ObjModelResource;
import de.dh.cad.architect.model.assets.RawMaterialModel;
import de.dh.cad.architect.model.assets.SupportObjectDescriptor;
import de.dh.cad.architect.model.assets.ThreeDModelResource;
import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.model.objects.MaterialMappingConfiguration;
import de.dh.cad.architect.ui.assets.AssetManager.AssetLocation;
import de.dh.cad.architect.ui.utils.CoordinateUtils;
import de.dh.cad.architect.ui.view.libraries.ImageLoadOptions;
import de.dh.cad.architect.utils.vfs.IDirectoryLocator;
import de.dh.cad.architect.utils.vfs.IPathLocator;
import de.dh.cad.architect.utils.vfs.IResourceLocator;
import de.dh.utils.MaterialMapping;
import de.dh.utils.Vector2D;
import de.dh.utils.fx.BoxMesh;
import de.dh.utils.io.fx.FxMeshBuilder;
import de.dh.utils.io.fx.MaterialData;
import de.dh.utils.io.obj.MtlLibraryIO;
import de.dh.utils.io.obj.ObjReader;
import de.dh.utils.io.obj.ObjReader.ObjDataRaw;
import de.dh.utils.io.obj.RawMaterialData;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
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

        public static ImportResource fromResource(IResourceLocator resource) {
            return new ImportResource(resource, resource.getFileName());
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
    public static final String DEFAULT_MATERIALS_BASE = "defaultmaterials"; // Potential base directory for resources of the default materials

    public static final String TEMPLATE_MATERIAL_SET_ICON_IMAGE = MATERIAL_SET_PLACEHOLDER_ICON_IMAGE;

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

    /**
     * Loads the 3D object of the given support object descriptor without loading of materials.
     * @param soDescriptor Descriptor of the support object to load.
     * @return Loaded 3D object.
     */
    public IncompleteThreeDObject loadSupportObject3DResourcePure(SupportObjectDescriptor soDescriptor) throws IOException {
        AssetRefPath assetRefPath = soDescriptor.getSelfRef();
        AssetLocation assetLocation = mAssetManager.resolveAssetLocation(assetRefPath);
        AbstractModelResource model = soDescriptor.getModel();
        if (model == null) {
            throw new NullPointerException("3D model is not assigned in descriptor <" + assetRefPath + ">");
        }
        try {
            if (model instanceof ObjModelResource omr) {

                ObjDataRaw objData = loadObjModelData(assetLocation, omr);

                Collection<MeshView> meshes = objData.getMeshes()
                        .stream()
                        .map(md -> {
                            try {
                                return FxMeshBuilder.buildMeshView(md);
                            } catch (IOException e) {
                                throw new RuntimeException("Unable to build mesh view from " + md);
                            }
                        }).toList();
                Optional<Transform> oTrans = AssetLoaderUtils.createTransform(omr.getModelRotationMatrix());
                return new IncompleteThreeDObject(meshes, objData.getMeshNamesToMaterialNames(), oTrans, soDescriptor.getWidth(), soDescriptor.getHeight(), soDescriptor.getDepth());
            } else {
                throw new NotImplementedException("Unable to load object 3D model of class <" + model.getClass() + "> in descriptor <" + assetRefPath + ">");
            }
        } catch (IOException e) {
            String msg = "Unable to load 3D model for support object descriptor <" + soDescriptor + ">";
            throw new IOException(msg, e);
        }
    }

    public ThreeDObject loadSupportObject3DResource(SupportObjectDescriptor soDescriptor) throws IOException {
        IncompleteThreeDObject incompleteThreeDObject = loadSupportObject3DResourcePure(soDescriptor);
        Map<String, String> meshNamesToOrigMaterialNames = incompleteThreeDObject.getMeshNamesToMaterialNames();

/*      // The following code produces a NullPointerException if MeshConfiguration::getMaterialAssignment returns null.
        // See https://stackoverflow.com/questions/24630963/nullpointerexception-in-collectors-tomap-with-null-entry-values
        Map<String, AssetRefPath> meshNamesToSODMaterialRefs = soDescriptor.getMeshNamesToMeshConfigurations().values()
                .stream()
                .collect(Collectors.toMap(MeshConfiguration::getMeshName, MeshConfiguration::getMaterialAssignment));
        // --> We replace it by this:
*/
        Map<String, AssetRefPath> meshNamesToSODMaterialRefs = soDescriptor.getMeshNamesToMeshConfigurations().values()
                .stream()
                .collect(HashMap::new, (m, v) -> m.put(v.getMeshName(), v.getMaterialAssignment()), HashMap::putAll);

        ThreeDObject result = incompleteThreeDObject.getThreeDObjectWithoutMaterials();

        for (MeshView meshView : result.getSurfaceMeshViews()) {
            String meshName = meshView.getId();
            AssetRefPath materialRef = meshNamesToSODMaterialRefs.get(meshName);
            MaterialData material;
            if (materialRef == null) {
                String materialName = meshNamesToOrigMaterialNames.get(meshName);
                material = materialName == null ? null : mAssetManager.getDefaultMaterials().get(materialName);
            } else {
                material = loadMaterialData(materialRef);
            }
            meshView.setMaterial(buildMaterial_Lax(material, MaterialMapping.stretch()));
        }

        return result;
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

    public void updateSupportObject3DViewObjRotation(SupportObjectDescriptor descriptor, Transform rotation) {
        AssetRefPath assetRefPath = descriptor.getSelfRef();

        AbstractModelResource model = descriptor.getModel();
        if (!(model instanceof ThreeDModelResource tdmr)) {
            throw new NullPointerException("3D model is not assigned in descriptor <" + assetRefPath + ">");
        }
        float[][] rotationMatrix = AssetLoaderUtils.createRotationMatrix(rotation);
        tdmr.setModelRotationMatrix(rotationMatrix);
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

    protected void clearResourcesFolder(AssetRefPath assetRefPath) throws IOException {
        IDirectoryLocator directory = getResourcesDirectory(assetRefPath);
        directory.clean();
    }

    /**
     * Gets the resources directory of the given asset.
     * @param assetRefPath Asset ref path to a material set or support object.
     * @return {@link AssetManager#RESOURCES_DIRECTORY_NAME Resources directory name} of the given asset.
     */
    public IDirectoryLocator getResourcesDirectory(AssetRefPath assetRefPath) throws IOException {
        AssetLocation assetLocation = mAssetManager.resolveAssetLocation(assetRefPath);
        return assetLocation.resolveResourcesDirectory().getDirectoryLocator();
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

    public void importAssetResources(Collection<ImportResource> resources, AssetRefPath materialSetRefPath, boolean cleanDirectory) throws IOException {
        AssetLocation resourceLocation = mAssetManager.resolveAssetLocation(materialSetRefPath).resolveResourcesDirectory();
        IDirectoryLocator resourceDirectory = resourceLocation.getDirectoryLocator();
        if (cleanDirectory && resourceDirectory.exists()) {
            resourceDirectory.clean();
        }
        for (ImportResource resource : resources) {
            resourceLocation.importResource(resource.getResourceLocator(), resource.getTargetFileName());
        }
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
        clearResourcesFolder(assetRefPath);
        ThreeDModelResource modelResource = importAssetResourceObj(assetRefPath, sourceObjResourceLocator, importMode, oOverriddenName);
        if (oModelRotation.isPresent()) {
            modelResource.setModelRotationMatrix(oModelRotation.get());
        } else {
            modelResource.setModelRotationMatrix(null);
        }
        descriptor.setModel(modelResource);

        if (importMode == ThreeDResourceImportMode.Directory) {
            AbstractModelResource soModel = descriptor.getModel();
            if (soModel instanceof ObjModelResource omr) {
                AssetLocation soAssetLocation = mAssetManager.resolveAssetLocation(assetRefPath);
                // We have imported a whole directory consisting of license files, .obj files, .mtl files and additional resources.
                // Now we'll disassemble the materials and compile the data to local support object materials in asset library format.
                convertSOLocalMtlsToMaterialSets(descriptor, soAssetLocation, omr);
            } else {
                System.out.println("Skipping non-object model of descriptor " + assetRefPath);
            }
        }

        mAssetManager.saveSupportObjectDescriptor(descriptor);
    }

    /**
     * Given a support object with already imported .obj file part, this method converts the referenced materials
     * from .mtl files which were already copied to the support object's {@link AssetManager#RESOURCES_DIRECTORY_NAME Resources} directory
     * to local material sets of the support object in asset library format.
     *
     * @param soDescriptor Descriptor of the half-imported support object.
     * @param soAssetLocation Asset location of the support object. This parameter is redundant to and could be calculated from {@code soDescriptor}
     * but is already present in caller and thus given here for performance reasons.
     * @param omr Object model of the support object. This parameter is also redundant but passed for performance reasons.
     */
    public void convertSOLocalMtlsToMaterialSets(SupportObjectDescriptor soDescriptor, AssetLocation soAssetLocation, ObjModelResource omr) throws IOException {
        try {
            ObjDataRaw objDataRaw = loadObjModelData(soAssetLocation, omr);

            Map<String, String> meshNamesToMaterialNames = objDataRaw.getMeshNamesToMaterialNames();

            Map<String, AssetRefPath> materialNamesToMaterialSetRefPaths = new HashMap<>();

            for (IPathLocator mtlFileLocator : soAssetLocation.resolveResourcesDirectory().getDirectoryLocator().list(pl -> { return pl.getFileName().endsWith(".mtl"); })) {
                materialNamesToMaterialSetRefPaths.putAll(convertSOLocalMtlToMaterialSet(soDescriptor, soAssetLocation, (IResourceLocator) mtlFileLocator));
            }

            Map<String, MeshConfiguration> meshNamesToMeshConfigurations = soDescriptor.getMeshNamesToMeshConfigurations();
            for (Entry<String, String> entry : meshNamesToMaterialNames.entrySet()) {
                String meshName = entry.getKey();
                String materialName = entry.getValue();
                MeshConfiguration mc = new MeshConfiguration(meshName);
                AssetRefPath materialSetRefPath = materialNamesToMaterialSetRefPaths.get(materialName);
                if (materialSetRefPath != null) {
                    mc.setMaterialAssignment(materialSetRefPath.withMaterialName(materialName));
                } // Else, we'll leave the material assignment empty -> Fallback to default material
                meshNamesToMeshConfigurations.put(meshName, mc);
            }

            mAssetManager.saveAssetDescriptor(soDescriptor);
        } catch (Exception e) {
            throw new RuntimeException("Unable to import local material sets", e);
        }
    }

    protected Map<String, AssetRefPath> convertSOLocalMtlToMaterialSet(SupportObjectDescriptor soDescriptor,
            AssetLocation soAssetLocation, IResourceLocator mtlFileLocator) throws IOException {

        Collection<IResourceLocator> mtlResourcesToDelete = new ArrayList<>(); // Those files are moved ore replaced by another file during migration

        Collection<IResourceLocator> allFilesOfMtlResource = MtlLibraryIO.getAllFiles(mtlFileLocator);
        mtlResourcesToDelete.addAll(allFilesOfMtlResource);

        /////// Create new material set
        MaterialSetDescriptor msDescriptor = mAssetManager.createMaterialSet(soAssetLocation.resolveLocalMaterialSetsDirectory());
        AssetRefPath msRefPath = msDescriptor.getSelfRef();

        // Metadata
        msDescriptor.setName(soDescriptor.getName() + " (Materials)");
        msDescriptor.setAuthor(soDescriptor.getAuthor());
        msDescriptor.setOrigin(soDescriptor.getOrigin());
        msDescriptor.setLastModified(LocalDateTime.now());

        // MaterialSet directories
        AssetLocation msAssetLocation = mAssetManager.resolveAssetLocation(msRefPath);
        IDirectoryLocator msDirectory = msAssetLocation.getDirectoryLocator();
        IDirectoryLocator msResourceDirectory = msDirectory.resolveDirectory(AssetManager.RESOURCES_DIRECTORY_NAME);
        msResourceDirectory.mkDirs();

        Map<String, RawMaterialData> materialSet = MtlLibraryIO.readMaterialSet(mtlFileLocator);

        // Move all relevant files for material set
        for (IResourceLocator someMtlFile : allFilesOfMtlResource) {
            try {
                if (someMtlFile.getFileName().endsWith(".mtl")) {
                    continue;
                }
                someMtlFile.copyTo(msResourceDirectory);
            } catch (NoSuchFileException e) {
                System.err.println("Unable to copy missing file of MTL library: " + someMtlFile.getAbsolutePath());
            }
        }

        Collection<RawMaterialModel> rawMaterialModels = new ArrayList<>();
        // Register new material models in descriptor
        for (RawMaterialData rawMaterialData : materialSet.values()) {
            rawMaterialModels.add(new RawMaterialModel(rawMaterialData.getName(), Optional.empty(), rawMaterialData.getLines()));
        }
        msDescriptor.setModel(new MaterialsModel(rawMaterialModels));
        Path soIconImagePath = Paths.get(soAssetLocation.resolveResource(soDescriptor.getIconImageResourceName()).getAbsolutePath());

        // As icon image, we'll use the SupportObject's icon image with an overlay "M" at the lower right part
        Path targetPath = Paths.get(msDirectory.getAbsolutePath()).resolve(AssetManager.ICON_IMAGE_DEFAULT_BASE_NAME + ".png");
        AssetLoaderUtils.createIconWithOverlay(soIconImagePath, targetPath, "M");

        mAssetManager.saveAssetDescriptor(msDescriptor);

        // Cleanup of old files
        for (IResourceLocator mtlResourceLocator : mtlResourcesToDelete) {
            String filePath = mtlResourceLocator.getAbsolutePath();
            System.out.println("Trying to delete obsolete material/resource file " + filePath);
            try {
                Files.delete(Paths.get(filePath));
            } catch (NoSuchFileException e) {
                System.err.println("Cannot delete associated file of MTL library, file is missing: " + filePath);
            }
        }

        return materialSet
                        .keySet()
                        .stream()
                        .collect(Collectors.toMap(Function.identity(), msRefPath::withMaterialName));
    }

    public void importRawMaterialSet(MaterialSetDescriptor descriptor, Collection<RawMaterialModel> materials, Collection<ImportResource> resources) throws IOException {
        AssetRefPath materialSetRefPath = descriptor.getSelfRef();
        importAssetResources(resources, materialSetRefPath, true);
        descriptor.setModel(new MaterialsModel(materials));
        mAssetManager.saveMaterialSetDescriptor(descriptor);
    }

    public ObjDataRaw loadObjModelData(AssetLocation assetLocation, ObjModelResource model) throws IOException {
        IResourceLocator resourceLocator = AssetManager.resolveResourcesModel(assetLocation, model);
        return loadObjModelData(resourceLocator);
    }

    public ObjDataRaw loadObjModelData(IResourceLocator resourceLocator) throws IOException {
        return ObjReader.readObjRaw(resourceLocator);
    }

    /**
     * Converts a {@link RawMaterialModel} to a {@link MaterialData}.
     */
    public static MaterialData mapMaterial(RawMaterialModel material, IDirectoryLocator baseDirectory) {
        return new MaterialData(material.getName(),material.getCommands(), material.getTileSize().map(CoordinateUtils::modelVector2DToUiVector2D), baseDirectory);
    }

    /**
     * Converts a collection of {@link RawMaterialModel} materials which all share the same resource base directory to a map of {@link MaterialData}.
     */
    public static Map<String, MaterialData> mapSiblingMaterials(Collection<RawMaterialModel> materials, IDirectoryLocator baseDirectory) {
        return materials
                .stream()
                .collect(Collectors.toMap(RawMaterialModel::getName, material -> mapMaterial(material, baseDirectory)));
    }

    public MaterialData loadMaterialData(MaterialSetDescriptor materialSetDescriptor, String materialName) throws IOException {
        AssetRefPath materialSetRefPath = materialSetDescriptor.getSelfRef();
        AbstractModelResource model = materialSetDescriptor.getModel();
        if (model instanceof MaterialsModel mm) {
            RawMaterialModel rawMaterialModel = mm.getMaterials().get(materialName);
            if (rawMaterialModel == null) {
                throw new IllegalArgumentException("Material " + materialName + " is not present in material set <" + materialSetRefPath + ">");
            }
            IDirectoryLocator resourcesDirectory = getResourcesDirectory(materialSetRefPath);
            return mapMaterial(rawMaterialModel, resourcesDirectory);
        } else {
            throw new NotImplementedException("Unable to load material from ref path <" + materialSetRefPath + ">: Unknown materials model type");
        }
    }

    public MaterialData loadMaterialData(AssetRefPath materialRefPath) throws IOException {
        String materialName = materialRefPath.getOMaterialName().orElseThrow(
                () -> new IllegalArgumentException("Material descriptor expected but asset ref path '" + materialRefPath + "' doesn't contain a material name"));
        AssetRefPath materialSetRefPath = materialRefPath.withoutMaterialName();
        MaterialSetDescriptor materialSetDescriptor = mAssetManager.loadMaterialSetDescriptor(materialSetRefPath);
        return loadMaterialData(materialSetDescriptor, materialName);
    }

    public Map<String, MaterialData> loadMaterials(MaterialSetDescriptor materialSetDescriptor) throws IOException {
        AssetRefPath materialSetRefPath = materialSetDescriptor.getSelfRef();
        AbstractModelResource model = materialSetDescriptor.getModel();
        if (model == null) {
            return Collections.emptyMap();
        } else if (model instanceof MaterialsModel mm) {
            IDirectoryLocator resourcesDirectory = getResourcesDirectory(materialSetRefPath);
            return mapSiblingMaterials(mm.getMaterials().values(), resourcesDirectory);
        } else {
            throw new NotImplementedException("Unable to load material from ref path <" + materialSetRefPath + ">: Unknown materials model type <" + model + ">");
        }
    }

    public Map<String, MaterialData> loadMaterials(AssetRefPath materialSetRefPath) throws IOException {
        if (materialSetRefPath.getOMaterialName().isPresent()) {
            throw new IllegalArgumentException("Material set descriptor expected but asset ref path '" + materialSetRefPath + "' contains a material name");
        }
        MaterialSetDescriptor materialSetDescriptor = mAssetManager.loadMaterialSetDescriptor(materialSetRefPath);
        return loadMaterials(materialSetDescriptor);
    }

    public Map<String, MaterialData> loadMaterialsData(Map<String, AssetRefPath> materialRefs) throws IOException {
        Map<String, MaterialData> result = new HashMap<>();
        for (Entry<String, AssetRefPath> entry : materialRefs.entrySet()) {
            String key = entry.getKey();
            AssetRefPath materialRefPath = entry.getValue();
            MaterialData material = materialRefPath == null ? null : loadMaterialData(materialRefPath);
            result.put(key, material);
        }
        return result;
    }

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

    public ThreeDObject loadSupportObject3DObject(SupportObjectDescriptor descriptor, boolean fallbackToPlaceholder) {
        try {
            return loadSupportObject3DResource(descriptor);
        } catch (IOException e) {
            if (fallbackToPlaceholder) {
                logMissingSupportObjectObjectView(descriptor, descriptor.getModel(), e);
                return loadBroken3DResource();
            } else {
                return null;
            }
        }
    }

    public ThreeDObject loadSupportObject3DObject(AssetRefPath supportObjectDescriptorRef, boolean fallbackToPlaceholder) {
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
        return loadSupportObject3DObject(descriptor, fallbackToPlaceholder);
    }

    public PhongMaterial buildMaterial(AssetRefPath materialRefPath, MaterialMapping mappingConfig) {
        if (materialRefPath == null) {
            return new PhongMaterial(Color.WHITE);
        }
        try {
            MaterialData material = loadMaterialData(materialRefPath);
            return buildMaterial_Lax(material, mappingConfig);
        } catch (IOException e) {
            log.warn("Error building material <" + materialRefPath + ">", e);
            Image placeholder = loadMaterialPlaceholderTextureImage(Optional.empty());
            PhongMaterial material = new PhongMaterial(Color.WHITE);
            material.setDiffuseMap(placeholder);
            return material;
        }
    }

    public PhongMaterial buildMaterial(MaterialMappingConfiguration mappingConfig, Optional<Vector2D> surfaceSize) {
        return buildMaterial(
                mappingConfig == null ? null : mappingConfig.getMaterialRef(),
                createMaterialMapping(mappingConfig, surfaceSize));
    }

    public PhongMaterial buildMaterial_Strict(MaterialData material, MaterialMapping mappingConfig) throws IOException {
        return FxMeshBuilder.buildMaterial_Strict(material, mappingConfig);
    }

    public PhongMaterial buildMaterial_Lax(MaterialData material, MaterialMapping mappingConfig) {
        return FxMeshBuilder.buildMaterial_Lax(material, mappingConfig);
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

    public static MaterialMapping.LayoutMode translateLayoutMode(MaterialMappingConfiguration.LayoutMode layoutMode) {
        return switch (layoutMode) {
            case Stretch -> MaterialMapping.LayoutMode.Stretch;
            case Tile -> MaterialMapping.LayoutMode.Tile;
        };
    }

    public static MaterialMapping createMaterialMapping(MaterialMappingConfiguration mmc, Optional<Vector2D> surfaceSize) {
        if (mmc == null) {
            return MaterialMapping.stretch();
        }
        return new MaterialMapping(
                Optional.ofNullable(mmc.getOffset()).map(CoordinateUtils::modelVector2DToUiVector2D),
                Optional.ofNullable(mmc.getTileSize()).map(CoordinateUtils::dimensions2DToUiVector2D),
                Optional.ofNullable(mmc.getMaterialRotationDeg()),
                AssetLoader.translateLayoutMode(mmc.getLayoutMode()),
                surfaceSize);
    }
}
