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
package de.dh.cad.architect.ui.assets;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeSet;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dh.cad.architect.model.assets.AbstractAssetDescriptor;
import de.dh.cad.architect.model.assets.AbstractModelResource;
import de.dh.cad.architect.model.assets.AssetRefPath;
import de.dh.cad.architect.model.assets.MaterialSetDescriptor;
import de.dh.cad.architect.model.assets.MtlModelResource;
import de.dh.cad.architect.model.assets.ObjModelResource;
import de.dh.cad.architect.model.assets.SupportObjectDescriptor;
import de.dh.cad.architect.ui.assets.AssetManager.AssetCollection;
import de.dh.cad.architect.ui.assets.AssetManager.AssetLocation;
import de.dh.cad.architect.utils.vfs.IDirectoryLocator;
import de.dh.cad.architect.utils.vfs.IResourceLocator;
import de.dh.utils.fx.BoxMesh;
import de.dh.utils.fx.Vector2D;
import de.dh.utils.fx.io.formats.obj.FxMeshBuilder;
import de.dh.utils.fx.io.formats.obj.MtlLibraryIO;
import de.dh.utils.fx.io.formats.obj.ObjData;
import de.dh.utils.fx.io.formats.obj.ObjReader;
import de.dh.utils.fx.io.formats.obj.RawMaterialData;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Shape3D;
import javafx.scene.transform.Affine;
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

    protected static Image loadImageFromResource(String localResourceName) {
        try (InputStream is = getResource(localResourceName).openStream()) {
            return new Image(is);
        } catch (IOException e) {
            throw new RuntimeException("Image resource '" + localResourceName + "' could not be loaded", e);
        }
    }

    public static Image loadBrokenImageSmall() {
        return loadImageFromResource(BROKEN_IMAGE_SMALL);
    }

    public static Image loadBrokenImageBig() {
        return loadImageFromResource(BROKEN_IMAGE_BIG);
    }

    public static Image loadSupportObjectPlaceholderIconImage() {
        return loadImageFromResource(SUPPORT_OBJECT_PLACEHOLDER_ICON_IMAGE);
    }

    public static Image loadMaterialSetPlaceholderIconImage() {
        return loadImageFromResource(MATERIAL_SET_PLACEHOLDER_ICON_IMAGE);
    }

    public static Image loadMaterialPlaceholderTextureImage() {
        return loadImageFromResource(MATERIAL_PLACEHOLDER_TEXTURE);
    }

    public static Image loadSupportObjectPlaceholderPlanViewImage() {
        return loadImageFromResource(SUPPORT_OBJECT_PLACEHOLDER_PLAN_VIEW_IMAGE);
    }

    public static ThreeDObject loadBroken3DResource() {
        MeshView result = new MeshView(BoxMesh.createMesh(100, 100, 100));
        PhongMaterial material = new PhongMaterial(Color.RED, loadBrokenImageBig(), null, null, null);
        result.setMaterial(material);
        return new ThreeDObject(Collections.singleton(result), Optional.empty());
    }

    public static ThreeDObject loadSupportObjectPlaceholder3DResource() {
        MeshView result = new MeshView(BoxMesh.createMesh(100, 100, 100));
        PhongMaterial material = new PhongMaterial(Color.GREEN);
        result.setMaterial(material);
        return new ThreeDObject(Collections.singleton(result), Optional.empty());
    }

    protected Image loadAssetResourceImage(AssetRefPath assetRefPath, String resourceName) throws IOException {
        AssetLocation assetLocation = mAssetManager.getAssetLocation(assetRefPath);
        return assetLocation.getAssetCollection().loadImage(assetLocation.getRelativePathInAssetCollection().resolve(resourceName));
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

    public ThreeDObject loadSupportObject3DResource(SupportObjectDescriptor soDescriptor, Optional<Map<String, RawMaterialData>> oOverriddenMaterials) throws IOException {
        AssetRefPath assetRefPath = soDescriptor.getSelfRef();
        AssetLocation assetLocation = mAssetManager.getAssetLocation(assetRefPath);
        AbstractModelResource model = soDescriptor.getModel();
        Collection<MeshView> meshes;
        if (model instanceof ObjModelResource) {
            try {
                meshes = loadObjModelMeshes(assetLocation, (ObjModelResource) model, oOverriddenMaterials, true);
            } catch (IOException e) {
                String msg = "Unable to load 3D model for support object descriptor <" + soDescriptor + ">";
                throw new IOException(msg, e);
            }
        } else {
            throw new NotImplementedException("Unable to resolve object 3D model <" + model + "> in descriptor <" + assetRefPath + ">");
        }
        Optional<Transform> oTrans = createTransform(soDescriptor.getModelRotationMatrix());
        return new ThreeDObject(meshes, oTrans);
    }

    protected void importAssetResourceImage(AssetRefPath assetRefPath, Image image, String imageName) throws IOException {
        if (!imageName.endsWith("." + AssetManager.STORE_IMAGE_EXTENSION)) {
            imageName = imageName + "." + AssetManager.STORE_IMAGE_EXTENSION;
        }
        AssetLocation assetLocation = mAssetManager.getAssetLocation(assetRefPath);
        assetLocation.getAssetCollection().saveImage(assetLocation.getRelativePathInAssetCollection().resolve(imageName), image);
    }

    public String importAssetResourceImage(AssetRefPath assetRefPath, IResourceLocator sourceImageResource, Optional<String> oOverriddenName) throws IOException {
        AssetLocation assetLocation = mAssetManager.getAssetLocation(assetRefPath);
        String imageName = oOverriddenName.orElse(sourceImageResource.getFileName());
        assetLocation.getAssetCollection().importResource(assetLocation.getRelativePathInAssetCollection().resolve(imageName), sourceImageResource);
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
    protected AbstractModelResource importAssetResourceObj(AssetRefPath assetRefPath, IResourceLocator sourceObjResourceLocator,
        ThreeDResourceImportMode importMode, Optional<String> oOverriddenName) throws IOException {
        AssetLocation assetLocation = mAssetManager.getAssetLocation(assetRefPath);
        Path resourceDirectoryPath = assetLocation.getRelativePathInAssetCollection().resolve(AssetManager.RESOURCES_DIRECTORY_NAME);
        String fileName = oOverriddenName.orElse(sourceObjResourceLocator.getFileName());
        switch (importMode) {
        case Directory:
            assetLocation.getAssetCollection().importResourceDirectory(resourceDirectoryPath, sourceObjResourceLocator.getParentDirectory());
            break;
        case ObjFile:
            assetLocation.getAssetCollection().importResource(resourceDirectoryPath.resolve(fileName), sourceObjResourceLocator);
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

    protected void deleteAssetResource(AssetRefPath assetRefPath, String resourceName) throws IOException {
        if (StringUtils.isEmpty(resourceName)) {
            return;
        }
        AssetLocation assetLocation = mAssetManager.getAssetLocation(assetRefPath);
        IResourceLocator resourceLocator = assetLocation.getAssetCollection().getAssetResourceLocator(assetLocation.getRelativePathInAssetCollection().resolve(resourceName));
        resourceLocator.delete();
    }

    public void importAssetIconImage(AbstractAssetDescriptor descriptor, Image image, String imageName) throws IOException {
        AssetRefPath assetRefPath = descriptor.getSelfRef();
        deleteAssetResource(assetRefPath, descriptor.getIconImageResourceName());
        importAssetResourceImage(assetRefPath, image, imageName);
        descriptor.setIconImageResourceName(imageName);
        mAssetManager.saveAssetDescriptor(descriptor);
    }

    public void importSupportObjectPlanViewImage(SupportObjectDescriptor descriptor, Image image, String imageName) throws IOException {
        AssetRefPath assetRefPath = descriptor.getSelfRef();
        deleteAssetResource(assetRefPath, descriptor.getPlanViewImageResourceName());
        importAssetResourceImage(assetRefPath, image, imageName);
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

    public void importSupportObject3DViewObjResource(SupportObjectDescriptor descriptor, IResourceLocator objResourceLocator,
        Optional<float[][]> oModelRotation, ThreeDResourceImportMode importMode, Optional<String> oOverriddenName) throws IOException {
        AssetRefPath assetRefPath = descriptor.getSelfRef();
        clearModelFolder(assetRefPath);
        AbstractModelResource modelResource = importAssetResourceObj(assetRefPath, objResourceLocator, importMode, oOverriddenName);
        descriptor.setModel(modelResource);
        if (oModelRotation.isPresent()) {
            descriptor.setModelRotationMatrix(oModelRotation.get());
        } else {
            descriptor.setModelRotationMatrix(null);
        }
        mAssetManager.saveSupportObjectDescriptor(descriptor);
    }

    public MtlModelResource importAssetResourceMtl(AssetRefPath materialSetRefPath, IResourceLocator mtlResourceLocator, Optional<String> oOverriddenName) throws IOException {
        clearModelFolder(materialSetRefPath);
        AssetLocation assetLocation = mAssetManager.getAssetLocation(materialSetRefPath);
        Path resourceDirectoryPath = assetLocation.getRelativePathInAssetCollection().resolve(AssetManager.RESOURCES_DIRECTORY_NAME);
        String fileName = oOverriddenName.orElse(mtlResourceLocator.getFileName());
        assetLocation.getAssetCollection().importResource(resourceDirectoryPath.resolve(fileName), mtlResourceLocator);
        Path modelPath = Paths.get(fileName);
        return new MtlModelResource(modelPath);
    }

    public MtlModelResource importAssetResourceMtl(AssetRefPath materialSetRefPath, Collection<RawMaterialData> materials, String mtlFileName, Collection<ImportResource> additionalResources) throws IOException {
        clearModelFolder(materialSetRefPath);
        AssetLocation assetLocation = mAssetManager.getAssetLocation(materialSetRefPath);
        Path resourceDirectoryPath = assetLocation.getRelativePathInAssetCollection().resolve(AssetManager.RESOURCES_DIRECTORY_NAME);
        AssetCollection assetCollection = assetLocation.getAssetCollection();
        assetCollection.saveMaterialsToMtl(resourceDirectoryPath.resolve(mtlFileName), materials);
        for (ImportResource resource : additionalResources) {
            assetCollection.importResource(resourceDirectoryPath.resolve(resource.getTargetFileName()), resource.getResourceLocator());
        }
        Path modelPath = Paths.get(mtlFileName);
        return new MtlModelResource(modelPath);
    }

    public IDirectoryLocator getModelDirectory(AssetRefPath assetRefPath) throws IOException {
        AssetLocation assetLocation = mAssetManager.getAssetLocation(assetRefPath);
        return assetLocation.getAssetCollection().getAssetDirectoryLocator(assetLocation.getRelativePathInAssetCollection().resolve(AssetManager.RESOURCES_DIRECTORY_NAME));
    }

    public IResourceLocator getMtlResource(AssetRefPath materialSetRefPath) throws IOException {
        MaterialSetDescriptor materialSetDescriptor = mAssetManager.loadMaterialSetDescriptor(materialSetRefPath);
        AssetLocation assetLocation = mAssetManager.getAssetLocation(materialSetRefPath);
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

    protected Collection<MeshView> loadObjModelMeshes(AssetLocation assetLocation, ObjModelResource model, Optional<Map<String, RawMaterialData>> oOverriddenMaterials,
        boolean failOnError) throws IOException {
        IResourceLocator resourceLocator = AssetManager.resolveResourcesModel(assetLocation, model);
        return loadObjModelMeshes(resourceLocator, oOverriddenMaterials, failOnError);
    }

    public Collection<MeshView> loadObjModelMeshes(IResourceLocator resourceLocator, Optional<Map<String, RawMaterialData>> oOverriddenMaterials,
        boolean failOnError) throws IOException {
        ObjData objData = ObjReader.readObj(resourceLocator, mAssetManager.getDefaultMaterials());
        Map<String, RawMaterialData> defaultMeshIdsToMaterials = objData.getMeshIdsToMaterials();
        Map<String, RawMaterialData> meshIdsToMaterials = oOverriddenMaterials
                        .map(om -> mergeMaterials(defaultMeshIdsToMaterials, om))
                        .orElse(defaultMeshIdsToMaterials);
        return FxMeshBuilder.buildMeshViews(objData.getMeshes(), meshIdsToMaterials, failOnError);
    }

    public Map<String, RawMaterialData> loadMaterialData(Map<String, AssetRefPath> materialRefs) throws IOException {
        Map<String, RawMaterialData> result = new HashMap<>();
        for (Entry<String, AssetRefPath> entry : materialRefs.entrySet()) {
            String key = entry.getKey();
            AssetRefPath materialRefPath = entry.getValue();
            RawMaterialData materialData = loadMaterialData(materialRefPath);
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
        AssetLocation assetLocation = mAssetManager.getAssetLocation(materialSetRefPath);

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
                    throw new FileNotFoundException(msg);
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

    public static Map<String, RawMaterialData> mergeMaterials(Map<String, RawMaterialData> defaultMeshIdsToMaterials, Map<String, RawMaterialData> overriddenMeshIdsToMaterials) {
        Map<String, RawMaterialData> result = new HashMap<>();
        for (Entry<String, RawMaterialData> mapping : defaultMeshIdsToMaterials.entrySet()) {
            String meshId = mapping.getKey();
            RawMaterialData overriddenMaterialData = overriddenMeshIdsToMaterials.get(meshId);
            result.put(meshId, overriddenMaterialData == null ? mapping.getValue() : overriddenMaterialData);
        }
        return result;
    }

    public static Optional<Transform> createTransform(float[][] rotationMatrix) {
        if (rotationMatrix == null) {
            return Optional.empty();
        }
        return Optional.of(Affine.affine(
            rotationMatrix[0][0], rotationMatrix[0][1], rotationMatrix[0][2], 0,
            rotationMatrix[1][0], rotationMatrix[1][1], rotationMatrix[1][2], 0,
            rotationMatrix[2][0], rotationMatrix[2][1], rotationMatrix[2][2], 0));
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public Image loadSupportObjectIconImage(SupportObjectDescriptor descriptor, boolean fallbackToPlaceholder) {
        try {
            return loadAssetIconImage(descriptor);
        } catch (Exception e) {
            if (fallbackToPlaceholder) {
                logMissingIconImage(descriptor.getSelfRef(), e);
                // Should we return an error resource or fallback to placeholder?
                return loadSupportObjectPlaceholderIconImage();
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
                return loadMaterialSetPlaceholderIconImage();
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
                return loadSupportObjectPlaceholderPlanViewImage();
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
                return loadSupportObjectPlaceholderPlanViewImage();
            } else {
                return null;
            }
        }
    }

    public ThreeDObject loadSupportObject3DObject(AssetRefPath supportObjectDescriptorRef, Optional<Map<String, RawMaterialData>> oOverriddenMaterials, boolean fallbackToPlaceholder) {
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
        return loadSupportObject3DObject(descriptor, oOverriddenMaterials, fallbackToPlaceholder);
    }

    public ThreeDObject loadSupportObject3DObject(SupportObjectDescriptor descriptor, Optional<Map<String, RawMaterialData>> oOverriddenMaterials, boolean fallbackToPlaceholder) {
        try {
            return loadSupportObject3DResource(descriptor, oOverriddenMaterials);
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
        try {
            configureMaterialEx(shape, materialRefPath, oSurfaceSize, false);
        } catch (IOException e) {
            Image placeholder = loadMaterialPlaceholderTextureImage();
            PhongMaterial material = new PhongMaterial(Color.WHITE);
            material.setDiffuseMap(placeholder);
            shape.setMaterial(material);
        }
    }

    public void configureMaterialEx(Shape3D shape, AssetRefPath materialRefPath, Optional<Vector2D> oSurfaceSize, boolean failOnError) throws IOException {
        if (materialRefPath == null) {
            shape.setMaterial(new PhongMaterial(Color.WHITE));
            return;
        }
        RawMaterialData materialData = loadMaterialData(materialRefPath);
        configureMaterial(shape, materialData, oSurfaceSize, failOnError);
    }

    public void configureMaterial(Shape3D shape, RawMaterialData materialData, Optional<Vector2D> oSurfaceSize, boolean failOnError) throws IOException {
        FxMeshBuilder.configureMaterial(shape, materialData, oSurfaceSize, failOnError);
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
}
