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
package de.dh.cad.architect.libraryimporter;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dh.cad.architect.libraryimporter.sh3d.furniture.CatalogPieceOfFurniture;
import de.dh.cad.architect.libraryimporter.sh3d.furniture.DefaultFurnitureCatalog.SH3DFurnitureLibrary;
import de.dh.cad.architect.model.assets.AssetRefPath;
import de.dh.cad.architect.model.assets.AssetRefPath.LibraryAssetPathAnchor;
import de.dh.cad.architect.model.assets.AssetType;
import de.dh.cad.architect.model.assets.SupportObjectDescriptor;
import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.ui.assets.AssetLoader;
import de.dh.cad.architect.ui.assets.AssetLoader.ThreeDResourceImportMode;
import de.dh.cad.architect.ui.assets.AssetManager;
import de.dh.cad.architect.ui.assets.AssetManager.LibraryData;
import de.dh.cad.architect.utils.vfs.IResourceLocator;
import de.dh.utils.fx.ImageUtils;
import de.dh.utils.fx.LightType;
import javafx.application.Platform;
import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.transform.Rotate;

public class ExternalSupportObjectDescriptor {
    private static final Logger log = LoggerFactory.getLogger(ExternalSupportObjectDescriptor.class);

    protected static final int ICON_SNAPSHOT_ANGLE_X = 30;
    protected static final int ICON_SNAPSHOT_ANGLE_Y = 300;

    public static final int DEFAULT_ICON_SIZE = 100;
    public static final int DEFAULT_PLAN_VIEW_IMAGE_SIZE = 300;

    private static final LightType ICON_SNAPSHOT_LIGHT_TYPE = LightType.Point;
    private static final LightType PLAN_VIEW_IMAGE_LIGHT_TYPE = LightType.Point;

    protected final CatalogPieceOfFurniture mSourcePieceOfFurniture;
    protected final SH3DFurnitureLibrary mSourceLibrary;
    protected final Path mSourceLibraryPath;

    public ExternalSupportObjectDescriptor(CatalogPieceOfFurniture pieceOfFurniture, SH3DFurnitureLibrary sourceLibrary, Path sourceLibraryPath) {
        mSourcePieceOfFurniture = pieceOfFurniture;
        mSourceLibrary = sourceLibrary;
        mSourceLibraryPath = sourceLibraryPath;
    }

    public CatalogPieceOfFurniture getSourcePieceOfFurniture() {
        return mSourcePieceOfFurniture;
    }

    public String getId() {
        return mSourcePieceOfFurniture.getId();
    }

    public Path getSourceLibrary() {
        return mSourceLibraryPath;
    }

    public SupportObjectDescriptor importSupportObject(LibraryData targetLibraryData, AssetManager assetManager) {
        String id = mSourcePieceOfFurniture.getId();
        if (StringUtils.isEmpty(id)) {
            log.debug("Generating id for support object");
            id = UUID.randomUUID().toString();
        }
        log.info("Importing piece of furniture '" + id + "'");
        LibraryAssetPathAnchor libraryAnchor = new LibraryAssetPathAnchor(targetLibraryData.getLibrary().getId());
        Path relativeAssetPath = Paths.get(AssetManager.SUPPORT_OBJECTS_DIRECTORY).resolve(id);
        AssetRefPath arp = new AssetRefPath(AssetType.SupportObject, libraryAnchor, relativeAssetPath);

        SupportObjectDescriptor existingSO = null;
        try {
            existingSO = assetManager.loadSupportObjectDescriptor(arp);
        } catch (Exception e) {
            // Ignore, descriptor doesn't seem to exist
        }
        try {
            if (existingSO != null) {
                assetManager.deleteAsset(arp);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error deleting asset '" + arp + "' before re-importing it", e);
        }
        SupportObjectDescriptor importedDescriptor;
        try {
            importedDescriptor = assetManager.createSupportObject_PredefinedId(libraryAnchor, id);
        } catch (IOException e) {
            throw new RuntimeException("Error creating asset '" + arp + "' for import", e);
        }
        importedDescriptor.setCategory(mSourcePieceOfFurniture.getCategory());
        importedDescriptor.setName(mSourcePieceOfFurniture.getName());
        importedDescriptor.getTags().addAll(Arrays.asList(mSourcePieceOfFurniture.getTags()));
        importedDescriptor.setDescription(mSourcePieceOfFurniture.getDescription());
//        importedDescriptor.setInformation(mSourcePieceOfFurniture.getInformation());
        importedDescriptor.setOrigin("SH3D Furniture Library '" + mSourceLibrary.getLibrary().getName() + "'");
        importedDescriptor.setWidth(Length.ofCM(mSourcePieceOfFurniture.getWidth()));
        importedDescriptor.setHeight(Length.ofCM(mSourcePieceOfFurniture.getHeight()));
        importedDescriptor.setDepth(Length.ofCM(mSourcePieceOfFurniture.getDepth()));
        importedDescriptor.setElevation(Length.ofCM(mSourcePieceOfFurniture.getElevation()));
        // importedDescriptor.setModelRotationMatrix(); --> Done in the model import step
        importedDescriptor.setLastModified(LocalDateTime.now());
        // TODO: Transfer more attributes

        arp = importedDescriptor.getSelfRef();

        AssetLoader assetLoader = assetManager.buildAssetLoader();

        IResourceLocator icon = mSourcePieceOfFurniture.getIcon();
        IResourceLocator planIcon = mSourcePieceOfFurniture.getPlanIcon();
        try {
            IResourceLocator modelResource = mSourcePieceOfFurniture.getModel();
            if (modelResource == null) {
                log.warn("Skipping support object '" + id + "', no model data present");
                assetManager.deleteAsset(arp);
                return null;
            }
            import3DModel(importedDescriptor, modelResource, Optional.ofNullable(mSourcePieceOfFurniture.getModelRotationJavaFX()), assetLoader);

            if (icon != null) {
                assetLoader.importAssetIconImage(importedDescriptor, icon, Optional.empty());
            } else {
                if (planIcon != null) {
                    assetLoader.importAssetIconImage(importedDescriptor, planIcon, Optional.empty());
                } else {
                    createSnapshotForIcon(importedDescriptor, modelResource, assetLoader);
                }
            }

            if (planIcon != null) {
                assetLoader.importSupportObjectPlanViewImage(importedDescriptor, planIcon, Optional.empty());
            } else {
                createSnapshotForPlanViewImage(importedDescriptor, modelResource, assetLoader);
            }

            log.debug("Successfully imported support object '" + id + "'");
            return importedDescriptor;
        } catch (Exception e) {
            log.info("Error while importing support object '" + id + "', will delete it", e);
            try {
                assetManager.deleteAsset(arp);
            } catch (IOException ex) {
                throw new RuntimeException("Error deleting asset '" + arp + "' after erroneous import", e);
            }
            return null;
        }
    }

    public void createSnapshotForIcon(SupportObjectDescriptor soDescriptor, IResourceLocator modelResource, AssetLoader assetLoader) {
        Node objView = ObjectLoader.load3DResource(modelResource, soDescriptor.getModelRotationMatrix(), assetLoader.getAssetManager().getDefaultMaterials());

        Rotate xRotate = new Rotate(ICON_SNAPSHOT_ANGLE_X, Rotate.X_AXIS);
        Rotate yRotate = new Rotate(ICON_SNAPSHOT_ANGLE_Y, Rotate.Y_AXIS);
        objView.getTransforms().addAll(0, Arrays.asList(xRotate, yRotate));

        Platform.runLater(() -> {
            try {
                Image snapshot = ImageUtils.takeSnapshot(objView, ICON_SNAPSHOT_LIGHT_TYPE, DEFAULT_ICON_SIZE);
                assetLoader.importAssetIconImage(soDescriptor, snapshot, modelResource.getFileName());
            } catch (IOException e) {
                throw new RuntimeException("Error importing snapshot image for plan view", e);
            }
        });
    }

    public void createSnapshotForPlanViewImage(SupportObjectDescriptor soDescriptor, IResourceLocator modelResource, AssetLoader assetLoader) {
        Node objView = ObjectLoader.load3DResource(modelResource, soDescriptor.getModelRotationMatrix(), assetLoader.getAssetManager().getDefaultMaterials());

        objView.getTransforms().add(0, new Rotate(90, new Point3D(1, 0, 0)));

        Platform.runLater(() -> {
            try {
                Image snapshot = ImageUtils.takeSnapshot(objView, PLAN_VIEW_IMAGE_LIGHT_TYPE, DEFAULT_PLAN_VIEW_IMAGE_SIZE);
                assetLoader.importSupportObjectPlanViewImage(soDescriptor, snapshot, modelResource.getFileName());
            } catch (IOException e) {
                throw new RuntimeException("Error importing snapshot image for plan view", e);
            }
        });
    }

    // Tries to retain 3D object resource directories with additional files like license files etc., if possible
    protected void import3DModel(SupportObjectDescriptor descriptor, IResourceLocator model, Optional<float[][]> oModelRotation, AssetLoader assetLoader) throws IOException {
        String location = mSourceLibrary.getLibrary().getLocation();
        String modelPath = model.getAbsolutePath();

        // location = "D:\Library\Path"
        // modelPath = "D:\Library\Path\contributions\3DrawerCabinet\3DrawerCabinet.obj" in case we have a 3D resource directory
        // - or -
        // modelPath = "D:\Library\Path\contributions\grandStaircaseDark.obj" in case the 3D resource is stored together with others

        if (!modelPath.startsWith(location)) {
            assetLoader.importSupportObject3DViewObjResource(descriptor, model, oModelRotation, ThreeDResourceImportMode.ObjFile, Optional.empty());
            return;
        }
        modelPath = modelPath.substring(location.length());
        modelPath = modelPath.replaceAll("\\\\", "/");
        if (modelPath.startsWith("/")) {
            modelPath = modelPath.substring(1);
        }
        // modelPath = "contributions/3DrawerCabinet/3DrawerCabinet.obj"
        // modelPath = "contributions/grandStaircaseDark.obj"
        String[] pathSegments = modelPath.split("/");

        if (pathSegments.length == 3) {
            // We assume we have an extra directory for the 3D resource files, license file etc., so import the whole directory
            assetLoader.importSupportObject3DViewObjResource(descriptor, model, oModelRotation, ThreeDResourceImportMode.Directory, Optional.empty());
            return;
        }
        // We assume the 3D object only consists of the obj file
        assetLoader.importSupportObject3DViewObjResource(descriptor, model, oModelRotation, ThreeDResourceImportMode.ObjFile, Optional.empty());
    }
}
