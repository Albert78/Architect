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
package de.dh.cad.architect.libraryimporter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dh.cad.architect.libraryimporter.sh3d.textures.CatalogTexture;
import de.dh.cad.architect.libraryimporter.sh3d.textures.DefaultTexturesCatalog.SH3DTexturesLibrary;
import de.dh.cad.architect.model.assets.AssetRefPath;
import de.dh.cad.architect.model.assets.AssetRefPath.LibraryAssetPathAnchor;
import de.dh.cad.architect.model.assets.AssetType;
import de.dh.cad.architect.model.assets.MaterialSetDescriptor;
import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.ui.assets.AssetLoader;
import de.dh.cad.architect.ui.assets.AssetLoader.ImportResource;
import de.dh.cad.architect.ui.assets.AssetManager;
import de.dh.cad.architect.ui.assets.AssetManager.AssetLocation;
import de.dh.cad.architect.ui.assets.AssetManager.LibraryData;
import de.dh.cad.architect.utils.vfs.IResourceLocator;
import de.dh.utils.io.obj.RawMaterialData;
import javafx.scene.image.Image;

public class ExternalTextureDescriptor {
    private static final Logger log = LoggerFactory.getLogger(ExternalTextureDescriptor.class);

    protected final CatalogTexture mSourceTexture;
    protected final SH3DTexturesLibrary mSourceLibrary;
    protected final Path mSourceLibraryPath;

    public ExternalTextureDescriptor(CatalogTexture texture, SH3DTexturesLibrary sourceLibrary, Path sourceLibraryPath) {
        mSourceTexture = texture;
        mSourceLibrary = sourceLibrary;
        mSourceLibraryPath = sourceLibraryPath;
    }

    public CatalogTexture getSourceTexture() {
        return mSourceTexture;
    }

    public String getId() {
        return mSourceTexture.getId();
    }

    public Path getSourceLibrary() {
        return mSourceLibraryPath;
    }

    public static Image loadImage(IResourceLocator imageFileLocator) throws IOException {
        try (InputStream is = imageFileLocator.inputStream()) {
            return new Image(is);
        }
    }

    protected void createMaterialForImage(MaterialSetDescriptor importedDescriptor, IResourceLocator sourceImage, String name, Length width, Length height, AssetLoader assetLoader) throws IOException {
        Collection<ImportResource> additionalResources = new ArrayList<>();
        List<String> lines = new ArrayList<>();
        Image image = loadImage(sourceImage);
        double imageWidth = image.getWidth();
        double imageHeight = image.getHeight();
        String sourceImageFileName = sourceImage.getFileName();
        String sourceImageBaseFileName = FilenameUtils.removeExtension(sourceImageFileName);
        lines.add("map_Kd -s " + (width.inCM() / imageWidth) + " " + (height.inCM() / imageHeight) + " " + sourceImageFileName);
        additionalResources.add(new ImportResource(sourceImage, sourceImageFileName));
        RawMaterialData material = new RawMaterialData(sourceImageBaseFileName, lines, sourceImage.getParentDirectory());
        Collection<RawMaterialData> materials = Arrays.asList(material);
        assetLoader.importMaterialSetMtlResource(importedDescriptor, materials, sourceImageBaseFileName + ".mtl", additionalResources);
    }

    public MaterialSetDescriptor importTexture(LibraryData targetLibraryData, AssetLoader assetLoader) {
        AssetManager assetManager = assetLoader.getAssetManager();

        String id = mSourceTexture.getId();
        if (StringUtils.isEmpty(id)) {
            log.debug("Generating id for texture");
            id = UUID.randomUUID().toString();
        }
        log.info("Importing texture '" + id + "'");
        LibraryAssetPathAnchor libraryAnchor = new LibraryAssetPathAnchor(targetLibraryData.getLibrary().getId());
        Path relativeAssetPath = Paths.get(AssetManager.SUPPORT_OBJECTS_DIRECTORY).resolve(id);
        AssetRefPath arp = new AssetRefPath(AssetType.SupportObject, libraryAnchor, relativeAssetPath);

        MaterialSetDescriptor existingMS = null;
        try {
            existingMS = assetManager.loadMaterialSetDescriptor(arp);
        } catch (Exception e) {
            // Ignore, descriptor doesn't seem to exist
        }
        try {
            if (existingMS != null) {
                assetManager.deleteAsset(arp);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error deleting asset '" + arp + "' before re-importing it", e);
        }

        // SH3D textures and Architect MaterialSets have a different structure:
        // A SH3D texture is a simple form of an Architect material and will be imported as a single material.
        // An Architect MaterialSet contains multiple, similar/related materials.
        // We'll import each texture to its own MaterialSet containing a single material for that texture;
        // The user will have to restructure the material sets and more materials between material sets in the
        // library manager later.
        MaterialSetDescriptor importedDescriptor;
        try {
            importedDescriptor = assetManager.createMaterialSet_PredefinedId(new AssetLocation(targetLibraryData.getAssetCollection(), Path.of("")), id);
        } catch (IOException e) {
            throw new RuntimeException("Error creating asset '" + arp + "' for import", e);
        }
        importedDescriptor.setCategory(mSourceTexture.getCategory());
        String name = mSourceTexture.getName();
        importedDescriptor.setName(name);
        importedDescriptor.setAuthor(mSourceTexture.getCreator());
        importedDescriptor.setOrigin("SH3D Texture Library '" + mSourceLibrary.getLibrary().getName() + "'");
        importedDescriptor.setLastModified(LocalDateTime.now());

        arp = importedDescriptor.getSelfRef();

        IResourceLocator icon = mSourceTexture.getIcon();
        IResourceLocator image = mSourceTexture.getImage();
        try {
            if (icon != null) {
                assetLoader.importAssetIconImage(importedDescriptor, icon, Optional.empty());
            } else {
                assetLoader.importAssetIconImage(importedDescriptor, image, Optional.empty());
            }

            createMaterialForImage(importedDescriptor, image, name, Length.ofCM(mSourceTexture.getWidth()), Length.ofCM(mSourceTexture.getHeight()), assetLoader);

            log.debug("Successfully imported texture '" + id + "'");
            return importedDescriptor;
        } catch (Exception e) {
            log.info("Error while importing texture '" + id + "', will delete it", e);
            try {
                assetManager.deleteAsset(arp);
            } catch (IOException ex) {
                throw new RuntimeException("Error deleting asset '" + arp + "' after erroneous import", e);
            }
            return null;
        }
    }
}
