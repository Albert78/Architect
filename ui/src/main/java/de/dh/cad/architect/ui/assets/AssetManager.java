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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dh.cad.architect.model.assets.AbstractAssetDescriptor;
import de.dh.cad.architect.model.assets.AssetLibrary;
import de.dh.cad.architect.model.assets.AssetRefPath;
import de.dh.cad.architect.model.assets.AssetRefPath.IAssetPathAnchor;
import de.dh.cad.architect.model.assets.AssetRefPath.LibraryAssetPathAnchor;
import de.dh.cad.architect.model.assets.AssetRefPath.PlanAssetPathAnchor;
import de.dh.cad.architect.model.assets.AssetType;
import de.dh.cad.architect.model.assets.FileModelResource;
import de.dh.cad.architect.model.assets.MaterialSetDescriptor;
import de.dh.cad.architect.model.assets.SupportObjectDescriptor;
import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.ui.persistence.AssetDescriptorsIO;
import de.dh.cad.architect.ui.persistence.LibraryIO;
import de.dh.cad.architect.ui.view.libraries.ImageLoadOptions;
import de.dh.cad.architect.utils.IdGenerator;
import de.dh.cad.architect.utils.vfs.ClassLoaderFileSystemDirectoryLocator;
import de.dh.cad.architect.utils.vfs.ClassLoaderFileSystemResourceLocator;
import de.dh.cad.architect.utils.vfs.IDirectoryLocator;
import de.dh.cad.architect.utils.vfs.IResourceLocator;
import de.dh.cad.architect.utils.vfs.PlainFileSystemDirectoryLocator;
import de.dh.utils.fx.ImageUtils;
import de.dh.utils.io.fx.MaterialData;
import de.dh.utils.io.obj.DefaultMaterials;
import de.dh.utils.io.obj.MtlLibraryIO;
import de.dh.utils.io.obj.RawMaterialData;
import javafx.scene.image.Image;

/**
 * Management API for assets in asset libraries and local assets in the plan. For loading and consuming assets,
 * use the {@link AssetLoader} API which can be obtained via {@link #buildAssetLoader()}.
 */
public class AssetManager {
    protected static enum CacheState {
        /**
         * The cache entry was not investigated yet.
         */
        Unknown,

        /**
         * The cache entry was already loaded and seems to be ok.
         */
        Healthy,

        /**
         * The cache entry seems to be erroneous.
         */
        Erroneous
    }

    protected static class AssetCollectionCacheEntry<T extends AbstractAssetDescriptor> {
        protected CacheState mState;
        protected T mAssetDescriptor;

        public CacheState getState() {
            return mState;
        }

        public void setState(CacheState value) {
            mState = value;
        }

        public T getAssetDescriptor() {
            return mAssetDescriptor;
        }

        public void setAssetDescriptor(T value) {
            mAssetDescriptor = value;
        }

        @Override
        public int hashCode() {
            return mAssetDescriptor.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof AssetCollectionCacheEntry)) {
                return false;
            }

            AssetCollectionCacheEntry<?> other = (AssetCollectionCacheEntry<?>) obj;
            return mAssetDescriptor.equals(other.mAssetDescriptor);
        }
    }

    /**
     * Cached filesystem which represents the root of an asset tree (can be an asset library or a plan).
     * An asset collection is always the root of an asset tree and thus is defined by an asset path anchor.
     */
    // TODO: Implement cache
    public static class AssetCollection {
        protected final IAssetPathAnchor mAnchor;
        protected final IDirectoryLocator mBaseDirectory;
        protected Map<String, AssetCollectionCacheEntry<MaterialSetDescriptor>> mRootMaterialSetsCache = null; // Ids to cache entries
        protected Map<String, AssetCollectionCacheEntry<SupportObjectDescriptor>> mSupportObjectsCache = null; // Ids to cache entries
        protected Map<String, Image> mImagesCache = null; // Relative asset collection paths to images

        public AssetCollection(IAssetPathAnchor anchor, IDirectoryLocator baseDirectory) {
            mAnchor = anchor;
            mBaseDirectory = baseDirectory;
        }

        public IAssetPathAnchor getAnchor() {
            return mAnchor;
        }

        public IDirectoryLocator getBaseDirectory() {
            return mBaseDirectory;
        }
        // Raw access to an asset resource - not cached
        public IResourceLocator resolveResourceLocator(Path relativePathInAssetCollection) {
            return mBaseDirectory.resolveResource(relativePathInAssetCollection);
        }

        // Raw access to an asset directory - not cached
        public IDirectoryLocator resolveDirectoryLocator(Path relativePathInAssetCollection) {
            return mBaseDirectory.resolveDirectory(relativePathInAssetCollection);
        }

        /**
         * Gets the base directory of the support objects of this asset collection.
         */
        public AssetLocation resolveSOBaseDirectory() {
            return new AssetLocation(this, Path.of(SUPPORT_OBJECTS_DIRECTORY));
        }

        /**
         * Gets the base directory of the root material sets of this asset collection.
         */
        public AssetLocation resolveMSBaseDirectory() {
            return new AssetLocation(this, Path.of(MATERIAL_SETS_DIRECTORY));
        }

        public void clearCache() {
            mRootMaterialSetsCache = null;
            mSupportObjectsCache = null;
            mImagesCache = null;
        }
    }

    public class LibraryData implements Comparable<LibraryData> {
        protected final AssetLibrary mLibrary;
        protected final IDirectoryLocator mRootDirectory;
        protected final AssetCollection mAssetCollection;

        public LibraryData(AssetLibrary library, IDirectoryLocator libraryRootDirectory) {
            mLibrary = library;
            mRootDirectory = libraryRootDirectory;
            mAssetCollection = new AssetCollection(new LibraryAssetPathAnchor(library.getId()), mRootDirectory);
        }

        public AssetLibrary getLibrary() {
            return mLibrary;
        }

        public IAssetPathAnchor getLibraryAnchor() {
            return mAssetCollection.getAnchor();
        }

        public IDirectoryLocator getRootDirectory() {
            return mRootDirectory;
        }

        public AssetCollection getAssetCollection() {
            return mAssetCollection;
        }

        @Override
        public int compareTo(LibraryData o) {
            return mLibrary.getId().compareTo(o.mLibrary.getId());
        }

        @Override
        public int hashCode() {
            return mLibrary.getId().hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            LibraryData other = (LibraryData) obj;
            if (!mLibrary.getId().equals(other.mLibrary.getId()))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return mLibrary.getName();
        }
    }

    protected static class PlanContext {
        protected final String mPlanId;
        protected final IDirectoryLocator mPlanFileDirectory;
        protected final AssetCollection mAssetCollection;

        public PlanContext(String planId, IDirectoryLocator planFileDirectory, AssetCollection assetCollection) {
            mPlanId = planId;
            mPlanFileDirectory = planFileDirectory;
            mAssetCollection = assetCollection;
        }

        public String getPlanId() {
            return mPlanId;
        }

        /**
         * Gets the directory where the plan file is located; this is intentionally stored separately from the
         * plan's local asset collection to make the location of the plan's local asset collection independent from
         * the directory where the plan file exists.
         */
        public IDirectoryLocator getPlanFileDirectory() {
            return mPlanFileDirectory;
        }

        public AssetCollection getAssetCollection() {
            return mAssetCollection;
        }
    }

    /**
     * Represents a directory in an asset collection and provides methods to access asset descriptors and asset resource files.
     */
    public static class AssetLocation {
        protected final AssetCollection mAssetCollection;
        protected final Path mRelativePathInAssetCollection;

        public AssetLocation(AssetCollection assetCollection, Path relativePathInAssetCollection) {
            mAssetCollection = assetCollection;
            mRelativePathInAssetCollection = relativePathInAssetCollection;
        }

        public AssetCollection getAssetCollection() {
            return mAssetCollection;
        }

        public Path getRelativePathInAssetCollection() {
            return mRelativePathInAssetCollection;
        }

        public IDirectoryLocator getDirectoryLocator() {
            return mAssetCollection.resolveDirectoryLocator(mRelativePathInAssetCollection);
        }

        public IResourceLocator resolveResource(String fileNameOrPath) {
            return mAssetCollection.resolveResourceLocator(mRelativePathInAssetCollection.resolve(fileNameOrPath));
        }

        public IResourceLocator resolveResource(Path filePath) {
            return mAssetCollection.resolveResourceLocator(mRelativePathInAssetCollection.resolve(filePath));
        }

        public IAssetPathAnchor getAnchor() {
            return mAssetCollection.getAnchor();
        }

        /**
         * Resolves the given path at this asset location.
         */
        public AssetLocation resolvePath(String relativePath) {
            return new AssetLocation(mAssetCollection, mRelativePathInAssetCollection.resolve(relativePath));
        }

        /**
         * Resolves the given path at this asset location.
         */
        public AssetLocation resolvePath(Path relativePath) {
            return new AssetLocation(mAssetCollection, mRelativePathInAssetCollection.resolve(relativePath));
        }

        /**
         * To be called on an asset's base directory.
         * @return Resources folder of the asset.
         */
        public AssetLocation resolveResourcesDirectory() {
            return resolvePath(RESOURCES_DIRECTORY_NAME);
        }

        /**
         * To be called on a support object's base directory.
         * @return Local material sets directory.
         */
        public AssetLocation resolveLocalMaterialSetsDirectory() {
            return resolvePath(MATERIAL_SETS_DIRECTORY);
        }

        /**
         * To be called on a support object's base directory.
         * @return Local material set directory of the given material set.
         */
        public AssetLocation resolveLocalMaterialSetDirectory(String materialSetId) {
            return resolvePath(MATERIAL_SETS_DIRECTORY + "/" + materialSetId);
        }

        // TODO: Cache in AssetCollection
        protected SupportObjectDescriptor loadSupportObjectDescriptor() throws IOException {
            IResourceLocator resourceLocator = resolveResource(SUPPORT_OBJECT_DESCRIPTOR_NAME);
            try (Reader reader = new BufferedReader(new InputStreamReader(resourceLocator.inputStream(), StandardCharsets.UTF_8))) {
                AssetRefPath supportObjectDescriptorRef = new AssetRefPath(AssetType.SupportObject, getAnchor(), mRelativePathInAssetCollection);
                return AssetDescriptorsIO.deserializeSupportObjectDescriptor(reader, supportObjectDescriptorRef);
            }
        }

        // TODO: Cache in AssetCollection
        protected MaterialSetDescriptor loadMaterialSetDescriptor() throws IOException {
            IResourceLocator resourceLocator = resolveResource(MATERIAL_SET_DESCRIPTOR_NAME);
            try (Reader reader = new BufferedReader(new InputStreamReader(resourceLocator.inputStream(), StandardCharsets.UTF_8))) {
                AssetRefPath materialDescriptorRef = new AssetRefPath(AssetType.MaterialSet, getAnchor(), mRelativePathInAssetCollection);
                return AssetDescriptorsIO.deserializeMaterialSetDescriptor(reader, materialDescriptorRef);
            } catch (IOException e) {
                throw new IOException("Error loading material descriptor from path '" + resourceLocator.getAbsolutePath() + "'", e);
            }
        }

        // TODO: Cache in AssetCollection
        public void saveSupportObjectDescriptor(SupportObjectDescriptor descriptor) throws IOException {
            IDirectoryLocator baseDirectory = getDirectoryLocator();
            baseDirectory.mkDirs();
            IResourceLocator resourceLocator = baseDirectory.resolveResource(SUPPORT_OBJECT_DESCRIPTOR_NAME);
            try (Writer writer = new BufferedWriter(new OutputStreamWriter(resourceLocator.outputStream()))) {
                AssetDescriptorsIO.serializeSupportObjectDescriptor(descriptor, writer);
            } catch (IOException e) {
                throw new IOException("Error writing support object descriptor to path '" + resourceLocator.getAbsolutePath() + "'", e);
            }
        }

        // TODO: Cache in AssetCollection
        public void saveMaterialSetDescriptor(MaterialSetDescriptor descriptor) throws IOException {
            IDirectoryLocator baseDirectory = getDirectoryLocator();
            baseDirectory.mkDirs();
            IResourceLocator resourceLocator = baseDirectory.resolveResource(MATERIAL_SET_DESCRIPTOR_NAME);
            try (Writer writer = new BufferedWriter(new OutputStreamWriter(resourceLocator.outputStream()))) {
                AssetDescriptorsIO.serializeMaterialSetDescriptor(descriptor, writer);
            } catch (IOException e) {
                throw new IOException("Error writing material descriptor to path '" + resourceLocator.getAbsolutePath() + "'", e);
            }
        }

        // TODO: Cache in AssetCollection
        public Image loadImage(String imageFileName) throws IOException {
            IResourceLocator resourceLocator = resolveResource(imageFileName);
            try {
                return AssetManager.loadImage(resourceLocator, Optional.empty());
            } catch (IOException e) {
                throw new IOException("Error loading image from path '" + resourceLocator.getAbsolutePath() + "'", e);
            }
        }

        // TODO: Cache in AssetCollection
        public void saveImage(Image image, String imageFileName) throws IOException {
            IResourceLocator resourceLocator = resolveResource(imageFileName);
            try {
                AssetManager.saveImage(resourceLocator, image);
            } catch (IOException e) {
                throw new IOException("Error writing image to path '" + resourceLocator.getAbsolutePath() + "'", e);
            }
        }

        // Should we cache this too?
        public void importResource(IResourceLocator sourceResource, String resourceFileName) throws IOException {
            IResourceLocator targetResourceLocator = resolveResource(resourceFileName);
            try (InputStream inputStream = sourceResource.inputStream()) {
                targetResourceLocator.copyFrom(inputStream);
            }
        }

        public void importResourceDirectory(IDirectoryLocator sourceDirectory) throws IOException {
            IDirectoryLocator targetDirectoryLocator = getDirectoryLocator();
            try {
                sourceDirectory.copyContentsTo(targetDirectoryLocator);
            } catch (IOException e) {
                throw new IOException("Error importing resource directory '" + sourceDirectory.getAbsolutePath() + "' to '" + targetDirectoryLocator.getAbsolutePath() + "'", e);
            }
        }

        // TODO: Update cache entries
        protected <T extends AbstractAssetDescriptor> Collection<T> loadAssetDescriptors(
            AssetType assetType, Function<AssetLocation, T> descriptorLoader) throws IOException {
            IDirectoryLocator assetTypeDirectory = getDirectoryLocator(); // Corresponds to mRelativePathInAssetCollection
            try {
                return assetTypeDirectory.exists()
                                ? assetTypeDirectory
                                    .list(IDirectoryLocator.class::isInstance)
                                    .stream()
                                    .map(pl -> {
                                        String assetId = pl.getFileName();
                                        Path relativeAssetPath = mRelativePathInAssetCollection.resolve(assetId);
                                        return descriptorLoader.apply(new AssetLocation(mAssetCollection, relativeAssetPath));
                                    })
                                    .filter(Objects::nonNull)
                                    .collect(Collectors.toList())
                                : Collections.emptyList();
            } catch (IOException e) {
                throw new IOException("Error listing asset descriptors from '" + assetTypeDirectory + "'", e);
            }
        }

        /**
         * To be called on the support object's folder of an asset collection.
         */
        public Collection<SupportObjectDescriptor> loadSupportObjectDescriptors() throws IOException {
            return loadAssetDescriptors(AssetType.SupportObject, assetLocation -> {
                try {
                    return assetLocation.loadSupportObjectDescriptor();
                } catch (IOException e) {
                    log.warn("Error loading support object descriptor from '" + assetLocation.getDirectoryLocator() + "'");
                    return null; // null value will be filtered out from caller loadAssetDescriptors(...)
                }
            });
        }

        /**
         * To be called on the material set's folder of an asset collection or on a local material set's folder of a support object..
         */
        public Collection<MaterialSetDescriptor> loadMaterialSetDescriptors() throws IOException {
            return loadAssetDescriptors(AssetType.MaterialSet, assetLocation -> {
                try {
                    return assetLocation.loadMaterialSetDescriptor();
                } catch (IOException e) {
                    log.warn("Error loading material set descriptor from '" + assetLocation.getDirectoryLocator() + "'");
                    return null; // null value will be filtered out from caller loadAssetDescriptors(...)
                }
            });
        }

        /**
         * To be called on an asset's base directory.
         */
        public void deleteAssetDirectory() throws IOException {
            IDirectoryLocator assetDirectory = getDirectoryLocator();
            try {
                assetDirectory.deleteRecursively();
            } catch (IOException e) {
                throw new IOException("Error deleting asset directory '" + assetDirectory.getAbsolutePath() + "'", e);
            }
        }
    }

    private static final Logger log = LoggerFactory.getLogger(AssetManager.class);

    /**
     * Package name with {@code '.'} replaced by {@code '/'} with a trailing {@code '/'}, to be used as prefix
     * when loading resources via the classloader.
     */
    public static final String LOCAL_RESOURCE_BASE = '/' + AssetLoader.class.getPackageName().replace('.', '/');

    /************************************* Directory and file names of the asset library ****************************/
    public static final String MATERIAL_SETS_DIRECTORY = "MaterialSets";
    public static final String SUPPORT_OBJECTS_DIRECTORY = "SupportObjects";
    public static final String RESOURCES_DIRECTORY_NAME = "Resources";
    public static final String SUPPORT_OBJECT_DESCRIPTOR_NAME = "SupportObjectDescriptor.xml";
    public static final String MATERIAL_SET_DESCRIPTOR_NAME = "MaterialSetDescriptor.xml";

    public static final String ICON_IMAGE_DEFAULT_BASE_NAME = "OverviewIcon";
    public static final String PLAN_VIEW_IMAGE_DEFAULT_BASE_NAME = "PlanViewImage";

    public static final String OBJ_FILE_DEFAULT_NAME = "Model.obj";

    public static final String STORE_IMAGE_EXTENSION = "png";
    /****************************************************************************************************************/

    protected final AssetManagerConfiguration mConfiguration;

    protected final Map<String, MaterialData> mDefaultMaterials = new TreeMap<>(); // Material names to materials
    protected final Map<String, LibraryData> mAssetLibraries = new TreeMap<>(); // Ids to asset libraries

    protected Optional<PlanContext> mOPlanContext = Optional.empty(); // Set if there is a plan in context, value changes if another plan is opened

    public AssetManager(AssetManagerConfiguration config) {
        mConfiguration = config;
    }

    public static AssetManager create() {
        AssetManagerConfiguration config = AssetManagerConfiguration.from(Preferences.userNodeForPackage(AssetManager.class));
        AssetManager result = new AssetManager(config);
        Map<String, MaterialData> defaultMaterials = DefaultMaterials
                .createDefaultMaterials()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> convertDefaultMaterial(e.getValue())));
        result.getDefaultMaterials().putAll(defaultMaterials);
        return result;
    }

    protected static MaterialData convertDefaultMaterial(RawMaterialData materialData) {
        return new MaterialData(materialData.getName(), materialData.getLines(), Optional.empty(),
                getClassLoaderDirectoryLocator(AssetLoader.DEFAULT_MATERIALS_BASE));
    }

    /**
     * Gets the base directory of the current plan file, if present.
     */
    public Optional<IDirectoryLocator> getPlanBaseDirectory() {
        return mOPlanContext.map(PlanContext::getPlanFileDirectory);
    }

    public Optional<String> getPlanId() {
        return mOPlanContext.map(pc -> pc.getPlanId());
    }

    /**
     * Sets the path to the current plan file. This is necessary to resolve plan-local assets.
     */
    public void setCurrentPlan(String planId, IDirectoryLocator planBaseDirectory) {
        if (planBaseDirectory == null) {
            mOPlanContext = Optional.empty();
        }
        mOPlanContext = Optional.of(new PlanContext(planId, planBaseDirectory, new AssetCollection(new PlanAssetPathAnchor(planId), planBaseDirectory)));
    }

    /**
     * Gets the opened asset libraries.
     * @return Map of asset library ids to corresponding asset library entries.
     */
    public Map<String, LibraryData> getAssetLibraries() {
        return mAssetLibraries;
    }

    public Map<String, MaterialData> getDefaultMaterials() {
        return mDefaultMaterials;
    }

    public AssetManagerConfiguration getConfiguration() {
        return mConfiguration;
    }

    /////////////////////////////////////////////////////// Lifecycle methods ////////////////////////////////////////////////////////

    public void start() {
        Collection<Path> openLibraries = mConfiguration.getOpenAssetLibraries();
        for (Path libraryPath : openLibraries) {
            try {
                openAssetLibrary(new PlainFileSystemDirectoryLocator(libraryPath));
            } catch (Exception e) {
                log.warn("Unable to load asset library from path '" + libraryPath + "'", e);
            }
        }
    }

    public void shutdown() {
        // Nothing to do ATM
    }

    public AssetLoader buildAssetLoader() {
        return AssetLoader.build(this);
    }

    ////////////////////////////////////////////////////// Cache management /////////////////////////////////////////////////////////

    public void clearCache() {
        for (LibraryData ld : mAssetLibraries.values()) {
            ld.getAssetCollection().clearCache();
        }
        mOPlanContext.ifPresent(pc -> pc.getAssetCollection().clearCache());
    }

    //////////////////////////////////////////////////////// Directory computation /////////////////////////////////////////////////////

    public AssetCollection resolveAssetCollection(IAssetPathAnchor anchor) throws IOException {
        if (anchor instanceof PlanAssetPathAnchor) {
            return mOPlanContext.map(pc -> pc.getAssetCollection()).orElseThrow(() -> planAnchorPathNotAvailableException());
        } else if (anchor instanceof LibraryAssetPathAnchor libraryPathAnchor) {
            String libraryId = libraryPathAnchor.getLibraryId();
            LibraryData libraryData = mAssetLibraries.get(libraryId);
            if (libraryData == null) {
                throw libraryIdUnknownException(libraryId);
            }
            return libraryData.getAssetCollection();
        } else {
            throw new NotImplementedException("Resolving of asset path anchor <" + anchor + "> is not implemented");
        }
    }

    protected AssetLocation resolveAssetLocation(IAssetPathAnchor anchor, Path assetLocationPath) throws IOException {
        return new AssetLocation(resolveAssetCollection(anchor), assetLocationPath);
    }

    /**
     * Gets the base location of the asset for the given asset reference path.
     * @throws IOException
     */
    public AssetLocation resolveAssetLocation(AssetRefPath ref) throws IOException {
        return resolveAssetLocation(ref.getAnchor(), ref.getAssetBasePath());
    }

    /**
     * Gets the resource locator for the given asset's model.
     * @param assetLocation Base folder of the asset whose model should be resolved.
     * @param model Model descriptor whose resource should be resolved.
     * @throws FileNotFoundException If the given model's resource does not exist.
     */
    public static IResourceLocator resolveResourcesModel(AssetLocation assetLocation, FileModelResource model) throws FileNotFoundException {
        IResourceLocator result = assetLocation.resolveResourcesDirectory().resolveResource(model.getRelativePath());
        if (!result.exists()) {
            throw new FileNotFoundException("Resource '" + result + "' is not present");
        }
        return result;
    }

    ///////////////////////////////////////////////////////// Exceptions ////////////////////////////////////////////////////////////

    protected IOException libraryIdUnknownException(String libraryId) {
        return new IOException("Library id '" + libraryId + "' is unknown");
    }

    protected RuntimeException planAnchorPathNotAvailableException() {
        return new RuntimeException("No plan is available in context, plan path cannot be resolved");
    }

    /////////////////////////////////////////////////// Asset libraries //////////////////////////////////////////////////////////////

    public LibraryData createNewAssetLibrary(IDirectoryLocator libraryRootDirectory, String libraryName) {
        AssetLibrary library = new AssetLibrary(IdGenerator.generateUniqueId(AssetLibrary.class));
        try {
            libraryRootDirectory.mkDirs();
        } catch (Exception e) {
            throw new RuntimeException("Error creating directories for new asset library '" + library.getId() + "'", e);
        }
        try (Writer writer = new BufferedWriter(
            new OutputStreamWriter(libraryRootDirectory.resolveResource(LibraryIO.DEFAULT_ASSET_LIBRARY_FILE_NAME).outputStream()))) {
            library.setName(libraryName);
            LibraryIO.serializeAssetLibrary(library, writer);
            LibraryData result = new LibraryData(library, libraryRootDirectory);
            mAssetLibraries.put(library.getId(), result);
            saveOpenAssetLibraries();
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Error creating new asset library '" + library.getId() + "'", e);
        }
    }

    protected void saveOpenAssetLibraries() {
        Collection<Path> openAssetLibraries = new ArrayList<>();
        for (LibraryData libraryData : mAssetLibraries.values()) {
            IDirectoryLocator rootDirectory = libraryData.getRootDirectory();
            if (!(rootDirectory instanceof PlainFileSystemDirectoryLocator)) {
                // Actually, this restriction is not necessary, the only reason is because the configuration API doesn't support the VFS API yet
                log.warn("Currently, we can only save asset libraries which are accessed via the plain file system VFS API");
                continue;
            }
            PlainFileSystemDirectoryLocator dl = (PlainFileSystemDirectoryLocator) rootDirectory;
            openAssetLibraries.add(dl.getPath());
        }
        mConfiguration.setOpenAssetLibraries(openAssetLibraries);
    }

    public static boolean isAssetLibraryDirectory(IDirectoryLocator libraryRootDirectory) {
        return libraryRootDirectory.resolveResource(LibraryIO.DEFAULT_ASSET_LIBRARY_FILE_NAME).exists();
    }

    public static AssetLibrary loadAssetLibrary(IDirectoryLocator libraryRootDirectory) {
        try (Reader reader = new BufferedReader(
            new InputStreamReader(
                libraryRootDirectory.resolveResource(LibraryIO.DEFAULT_ASSET_LIBRARY_FILE_NAME).inputStream()))) {
            return LibraryIO.deserializeAssetLibrary(reader);
        } catch (Exception e) {
            throw new RuntimeException("Error loading asset library from path '" + libraryRootDirectory.toString() + "'", e);
        }
    }

    public LibraryData openAssetLibrary(IDirectoryLocator libraryRootDirectory) {
        AssetLibrary library = loadAssetLibrary(libraryRootDirectory);
        LibraryData result = new LibraryData(library, libraryRootDirectory);
        mAssetLibraries.put(library.getId(), result);
        saveOpenAssetLibraries();
        return result;
    }

    public void closeAssetLibrary(String libraryId) {
        mAssetLibraries.remove(libraryId);
        saveOpenAssetLibraries();
    }

    public void saveAssetLibrary(LibraryData libraryData) {
        AssetLibrary library = libraryData.getLibrary();
        try (Writer writer = new BufferedWriter(
            new OutputStreamWriter(
                libraryData.getRootDirectory().resolveResource(LibraryIO.DEFAULT_ASSET_LIBRARY_FILE_NAME).outputStream()))) {
            LibraryIO.serializeAssetLibrary(library, writer);
        } catch (Exception e) {
            throw new RuntimeException("Error saving asset library '" + library.getId() + "'", e);
        }
    }

    public void deleteAssetLibrary(LibraryData library) throws IOException {
        IDirectoryLocator rootDirectory = library.getRootDirectory();
        try {
            rootDirectory.deleteRecursively();
        } catch (IOException e) {
            throw new IOException("Error deleting asset library '" + library.getLibrary().getId() + "' from path '" + rootDirectory + "'", e);
        }
    }

    protected Collection<String> filterAssetCategories(Collection<? extends AbstractAssetDescriptor> descriptors) {
        return descriptors
            .stream()
            .map(msd -> msd.getCategory())
            .filter(e -> !StringUtils.isEmpty(e))
            .sorted()
            .distinct()
            .collect(Collectors.toList());
    }

    public Collection<String> filterAssetTypes(Collection<? extends AbstractAssetDescriptor> descriptors) {
        return descriptors
            .stream()
            .map(msd -> msd.getType())
            .filter(e -> !StringUtils.isEmpty(e))
            .sorted()
            .distinct()
            .collect(Collectors.toList());
    }

    public Collection<String> getMaterialSetCategories() {
        try {
            return filterAssetCategories(loadAllLibraryRootMaterialSetDescriptors());
        } catch (IOException e) {
            log.warn("Error while loading asset categories", e);
            return Collections.emptyList();
        }
    }

    public Collection<String> getMaterialSetTypes() {
        try {
            return filterAssetTypes(loadAllLibraryRootMaterialSetDescriptors());
        } catch (IOException e) {
            log.warn("Error while loading asset types", e);
            return Collections.emptyList();
        }
    }

    public Collection<String> getSupportObjectCategories() {
        try {
            return filterAssetCategories(loadAllLibrarySupportObjectDescriptors());
        } catch (IOException e) {
            log.warn("Error while loading asset categories", e);
            return Collections.emptyList();
        }
    }

    public Collection<String> getSupportObjectTypes() {
        try {
            return filterAssetTypes(loadAllLibrarySupportObjectDescriptors());
        } catch (IOException e) {
            log.warn("Error while loading asset types", e);
            return Collections.emptyList();
        }
    }

    ///////////////////////////////////////////////////// Descriptor methods ////////////////////////////////////////////////////////////

    public SupportObjectDescriptor loadSupportObjectDescriptor(AssetRefPath supportObjectDescriptorRef) throws IOException {
        AssetLocation assetLocation = resolveAssetLocation(supportObjectDescriptorRef);
        return assetLocation.loadSupportObjectDescriptor();
    }

    public MaterialSetDescriptor loadMaterialSetDescriptor(AssetRefPath materialSetDescriptorRef) throws IOException {
        AssetLocation assetLocation = resolveAssetLocation(materialSetDescriptorRef);
        return assetLocation.loadMaterialSetDescriptor();
    }

    public void saveMaterialSetDescriptor(MaterialSetDescriptor descriptor) throws IOException {
        AssetRefPath refPath = descriptor.getSelfRef();
        AssetLocation assetLocation = resolveAssetLocation(refPath);
        assetLocation.saveMaterialSetDescriptor(descriptor);
    }

    public void saveSupportObjectDescriptor(SupportObjectDescriptor descriptor) throws IOException {
        AssetRefPath refPath = descriptor.getSelfRef();
        AssetLocation assetLocation = resolveAssetLocation(refPath);
        assetLocation.saveSupportObjectDescriptor(descriptor);
    }

    public void saveAssetDescriptor(AbstractAssetDescriptor descriptor) throws IOException {
        if (descriptor instanceof MaterialSetDescriptor msd) {
            saveMaterialSetDescriptor(msd);
        } else if (descriptor instanceof SupportObjectDescriptor sod) {
            saveSupportObjectDescriptor(sod);
        } else {
            throw new NotImplementedException("Saving of descriptor type " + descriptor.getClass() + " is not implemented");
        }
    }

    public Collection<SupportObjectDescriptor> loadSupportObjectDescriptors(IAssetPathAnchor anchor) throws IOException {
        AssetLocation assetLocation = resolveAssetCollection(anchor).resolveSOBaseDirectory();
        return assetLocation.loadSupportObjectDescriptors();
    }

    public Collection<MaterialSetDescriptor> loadSupportObjectMaterialSetDescriptors(SupportObjectDescriptor supportObjectDescriptor) throws IOException {
        return resolveAssetLocation(supportObjectDescriptor.getSelfRef()).resolveLocalMaterialSetsDirectory().loadMaterialSetDescriptors();
    }

    public Collection<MaterialSetDescriptor> loadMaterialSetDescriptors(IAssetPathAnchor anchor, boolean includeLocalMaterials) throws IOException {
        AssetCollection assetCollection = resolveAssetCollection(anchor);
        Collection<MaterialSetDescriptor> result = new ArrayList<>(assetCollection.resolveMSBaseDirectory().loadMaterialSetDescriptors());
        if (!includeLocalMaterials) {
            return result;
        }
        Collection<SupportObjectDescriptor> supportObjectDescriptors = assetCollection.resolveSOBaseDirectory().loadSupportObjectDescriptors();
        for (SupportObjectDescriptor supportObjectDescriptor : supportObjectDescriptors) {
            result.addAll(loadSupportObjectMaterialSetDescriptors(supportObjectDescriptor));
        }
        return result;
    }

    public Collection<SupportObjectDescriptor> loadAllLibrarySupportObjectDescriptors() throws IOException {
        Collection<SupportObjectDescriptor> result = new ArrayList<>();
        for (String libraryId : mAssetLibraries.keySet()) {
            result.addAll(loadSupportObjectDescriptors(new LibraryAssetPathAnchor(libraryId)));
        }
        return result;
    }

    public Collection<MaterialSetDescriptor> loadAllLibraryRootMaterialSetDescriptors() throws IOException {
        Collection<MaterialSetDescriptor> result = new ArrayList<>();
        for (String libraryId : mAssetLibraries.keySet()) {
            result.addAll(loadMaterialSetDescriptors(new LibraryAssetPathAnchor(libraryId), false));
        }
        return result;
    }

    public MaterialSetDescriptor createRootMaterialSet(String libraryId) throws IOException {
        AssetLocation assetLocation = resolveAssetLocation(new LibraryAssetPathAnchor(libraryId), Path.of(""))
                        .resolveLocalMaterialSetsDirectory();
        return createMaterialSet(assetLocation);
    }

    public MaterialSetDescriptor createSupportObjectMaterialSet(AssetRefPath supportObjectRefPath) throws IOException {
        AssetLocation assetLocation = resolveAssetLocation(supportObjectRefPath)
                        .resolveLocalMaterialSetsDirectory();
        return createMaterialSet(assetLocation);
    }

    /**
     * Creates a new material set in the given material sets folder.
     * @param materialSetsAssetLocation Asset location pointing to a root material sets location or
     * to a support object local material sets location.
     */
    public MaterialSetDescriptor createMaterialSet(AssetLocation materialSetsAssetLocation) throws IOException {
        String materialSetDescriptorId = IdGenerator.generateUniqueId(MaterialSetDescriptor.class);
        return createMaterialSet_PredefinedId(materialSetsAssetLocation, materialSetDescriptorId);
    }

    public MaterialSetDescriptor createMaterialSet_PredefinedId(AssetLocation materialSetsAssetLocation, String materialSetDescriptorId) throws IOException {
        AssetRefPath arp = new AssetRefPath(AssetType.MaterialSet, materialSetsAssetLocation.getAssetCollection().getAnchor(), materialSetsAssetLocation.getRelativePathInAssetCollection().resolve(materialSetDescriptorId));
        MaterialSetDescriptor result = new MaterialSetDescriptor(materialSetDescriptorId, arp);
        saveMaterialSetDescriptor(result); // Creates the directory structure

        AssetLoader assetLoader = buildAssetLoader();
        assetLoader.importAssetIconImage(result, getClassLoaderResourceLocator(AssetLoader.MATERIAL_SET_PLACEHOLDER_ICON_IMAGE),
            Optional.of(ICON_IMAGE_DEFAULT_BASE_NAME + "." + STORE_IMAGE_EXTENSION));

        assetLoader.importRawMaterialSet(result, Collections.emptyList(), Collections.emptyList());

        saveMaterialSetDescriptor(result);
        return result;
    }

    public SupportObjectDescriptor createSupportObject(IAssetPathAnchor anchor) throws IOException {
        String supportObjectDescriptorId = IdGenerator.generateUniqueId(SupportObjectDescriptor.class);
        return createSupportObject_PredefinedId(anchor, supportObjectDescriptorId);
    }

    public SupportObjectDescriptor createSupportObject_PredefinedId(IAssetPathAnchor anchor, String supportObjectDescriptorId) throws IOException {
        Path filePath = Paths.get(SUPPORT_OBJECTS_DIRECTORY + "/" + supportObjectDescriptorId);
        AssetRefPath arp = new AssetRefPath(AssetType.SupportObject, anchor, filePath);
        SupportObjectDescriptor result = new SupportObjectDescriptor(supportObjectDescriptorId, arp);
        saveSupportObjectDescriptor(result); // Creates the directory structure

        AssetLoader assetLoader = buildAssetLoader();
        assetLoader.importAssetIconImage(result, getClassLoaderResourceLocator(AssetLoader.SUPPORT_OBJECT_PLACEHOLDER_ICON_IMAGE),
            Optional.of(ICON_IMAGE_DEFAULT_BASE_NAME + "." + STORE_IMAGE_EXTENSION));
        assetLoader.importSupportObjectPlanViewImage(result, getClassLoaderResourceLocator(AssetLoader.SUPPORT_OBJECT_PLACEHOLDER_PLAN_VIEW_IMAGE),
            Optional.of(PLAN_VIEW_IMAGE_DEFAULT_BASE_NAME + "." + STORE_IMAGE_EXTENSION));
        assetLoader.importSupportObject3DViewObjResource(result, getClassLoaderResourceLocator(AssetLoader.TEMPLATE_SUPPORT_OBJECT_MODEL), Optional.empty(),
            ThreeDResourceImportMode.ObjFile, Optional.of(OBJ_FILE_DEFAULT_NAME));

        result.setWidth(Length.ofM(1));
        result.setHeight(Length.ofM(1));
        result.setDepth(Length.ofM(1));

        saveSupportObjectDescriptor(result);
        return result;
    }

    public void deleteAsset(AssetRefPath assetRefPath) throws IOException {
        if (assetRefPath.getOMaterialName().isPresent()) {
            throw new IllegalArgumentException("Single material in material set cannot be deleted separately (material set ref path: '" + assetRefPath + "')");
        }
        AssetLocation assetLocation = resolveAssetLocation(assetRefPath);
        Path assetBaseDirectoryPath = assetLocation.getRelativePathInAssetCollection();
        try {
            assetLocation.deleteAssetDirectory();
        } catch (IOException e) {
            throw new IOException("Error deleting asset '" + assetRefPath + "' from path '" + assetBaseDirectoryPath + "'", e);
        }
    }

    /////////////////////////////////////////////////////// Resource access ////////////////////////////////////////////////////////////////

    protected static IResourceLocator getClassLoaderResourceLocator(String localResourceName) {
        return new ClassLoaderFileSystemResourceLocator(Path.of(
                LOCAL_RESOURCE_BASE + "/" + localResourceName), AssetLoader.class);
    }

    protected static IDirectoryLocator getClassLoaderDirectoryLocator(String localResourceName) {
        return new ClassLoaderFileSystemDirectoryLocator(Path.of(
                LOCAL_RESOURCE_BASE + "/" + localResourceName), AssetLoader.class);
    }

    public static Image loadImage(IResourceLocator imageFileLocator, Optional<ImageLoadOptions> oLoadOptions) throws IOException {
        try (InputStream is = imageFileLocator.inputStream()) {
            return oLoadOptions.map(loadOptions -> new Image(is, loadOptions.getWidth(), loadOptions.getHeight(), loadOptions.isPreserveRatio(), loadOptions.isSmooth())).orElse(new Image(is));
        }
    }

    public static void saveImage(IResourceLocator imageFileLocator, Image image) throws IOException {
        if (imageFileLocator.exists()) {
            imageFileLocator.delete();
        }
        IDirectoryLocator directory = imageFileLocator.getParentDirectory();
        directory.mkDirs();
        String extension = STORE_IMAGE_EXTENSION;
        try (OutputStream os = imageFileLocator.outputStream()) {
            ImageUtils.saveImage(image, extension, os);
        } catch (IOException e) {
            throw new IOException("Error saving image to asset library, target file path '" + imageFileLocator + "'", e);
        }
    }

    public static Map<String, RawMaterialData> loadMaterials(IResourceLocator materialLibFileLocator) throws IOException {
        return MtlLibraryIO.readMaterialSet(materialLibFileLocator);
    }

    public static void saveMaterials(IResourceLocator materialLibFileLocator, Collection<RawMaterialData> materials) throws IOException {
        if (materialLibFileLocator.exists()) {
            materialLibFileLocator.delete();
        }
        IDirectoryLocator directory = materialLibFileLocator.getParentDirectory();
        directory.mkDirs();
        MtlLibraryIO.writeMaterialSet(materialLibFileLocator, materials);
    }
}
