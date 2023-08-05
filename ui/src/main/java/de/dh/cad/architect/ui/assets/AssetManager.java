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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.BiFunction;
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
import de.dh.cad.architect.model.assets.AssetRefPath.SupportObjectAssetPathAnchor;
import de.dh.cad.architect.model.assets.AssetType;
import de.dh.cad.architect.model.assets.FileModelResource;
import de.dh.cad.architect.model.assets.MaterialSetDescriptor;
import de.dh.cad.architect.model.assets.SupportObjectDescriptor;
import de.dh.cad.architect.ui.assets.AssetLoader.ThreeDResourceImportMode;
import de.dh.cad.architect.ui.persistence.AssetDescriptorsIO;
import de.dh.cad.architect.ui.persistence.LibraryIO;
import de.dh.cad.architect.utils.IdGenerator;
import de.dh.cad.architect.utils.vfs.ClassLoaderFileSystemResourceLocator;
import de.dh.cad.architect.utils.vfs.IDirectoryLocator;
import de.dh.cad.architect.utils.vfs.IResourceLocator;
import de.dh.cad.architect.utils.vfs.PlainFileSystemDirectoryLocator;
import de.dh.utils.fx.ImageUtils;
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

    // TODO: Consolidate/unify method signatures (e.g. unify relative/absolute path arguments)
    public class AssetCollection {
        protected final IDirectoryLocator mBaseDirectory;
        protected Map<String, AssetCollectionCacheEntry<MaterialSetDescriptor>> mRootMaterialSetsCache = null; // Ids to cache entries
        protected Map<String, AssetCollectionCacheEntry<SupportObjectDescriptor>> mSupportObjectsCache = null; // Ids to cache entries
        protected Map<String, Image> mImagesCache = null; // Relative asset collection paths to images

        public AssetCollection(IDirectoryLocator baseDirectory) {
            mBaseDirectory = baseDirectory;
        }

        public IDirectoryLocator getBaseDirectory() {
            return mBaseDirectory;
        }

        public IDirectoryLocator getAssetBaseDirectory(Path relativeAssetBasePath) {
            return mBaseDirectory.resolveDirectory(relativeAssetBasePath);
        }

        // TODO: Cache
        protected SupportObjectDescriptor loadSupportObjectDescriptor(Path supportObjectBaseDirectoryPath, AssetRefPath supportObjectDescriptorRef) throws IOException {
            IResourceLocator resourceLocator = getAssetBaseDirectory(supportObjectBaseDirectoryPath).resolveResource(SUPPORT_OBJECT_DESCRIPTOR_NAME);
            try (Reader reader = new BufferedReader(new InputStreamReader(resourceLocator.inputStream()))) {
                return AssetDescriptorsIO.deserializeSupportObjectDescriptor(reader, supportObjectDescriptorRef);
            }
        }

        // TODO: Cache
        protected MaterialSetDescriptor loadMaterialSetDescriptor(Path materialSetBaseDirectoryPath, AssetRefPath materialDescriptorRef) throws IOException {
            IResourceLocator resourceLocator = getAssetBaseDirectory(materialSetBaseDirectoryPath).resolveResource(MATERIAL_SET_DESCRIPTOR_NAME);
            try (Reader reader = new BufferedReader(new InputStreamReader(resourceLocator.inputStream()))) {
                return AssetDescriptorsIO.deserializeMaterialSetDescriptor(reader, materialDescriptorRef);
            }
        }

        // TODO: Cache
        public void saveSupportObjectDescriptor(Path supportObjectBaseDirectoryPath, SupportObjectDescriptor descriptor) throws IOException {
            IDirectoryLocator baseDirectory = getAssetBaseDirectory(supportObjectBaseDirectoryPath);
            baseDirectory.mkDirs();
            IResourceLocator resourceLocator = baseDirectory.resolveResource(SUPPORT_OBJECT_DESCRIPTOR_NAME);
            try (Writer writer = new BufferedWriter(new OutputStreamWriter(resourceLocator.outputStream()))) {
                AssetDescriptorsIO.serializeSupportObjectDescriptor(descriptor, writer);
            }
        }

        // TODO: Cache
        public void saveMaterialSetDescriptor(Path materialSetBaseDirectoryPath, MaterialSetDescriptor descriptor) throws IOException {
            IDirectoryLocator baseDirectory = getAssetBaseDirectory(materialSetBaseDirectoryPath);
            baseDirectory.mkDirs();
            IResourceLocator resourceLocator = baseDirectory.resolveResource(MATERIAL_SET_DESCRIPTOR_NAME);
            try (Writer writer = new BufferedWriter(new OutputStreamWriter(resourceLocator.outputStream()))) {
                AssetDescriptorsIO.serializeMaterialSetDescriptor(descriptor, writer);
            }
        }

        // TODO: Cache
        public Image loadImage(Path imageFileRelativePathInAssetCollection) throws IOException {
            IResourceLocator resourceLocator = getAssetResourceLocator(imageFileRelativePathInAssetCollection);
            return AssetManager.loadImage(resourceLocator);
        }

        // TODO: Cache
        public void saveImage(Path imageFileRelativePathInAssetCollection, Image image) throws IOException {
            IResourceLocator resourceLocator = getAssetResourceLocator(imageFileRelativePathInAssetCollection);
            AssetManager.saveImage(resourceLocator, image);
        }

        public void saveMaterialsToMtl(Path mtlFileRelativePathInAssetCollection, Collection<RawMaterialData> materials) throws IOException {
            IResourceLocator resourceLocator = getAssetResourceLocator(mtlFileRelativePathInAssetCollection);
            AssetManager.saveMaterials(resourceLocator, materials);
        }

        // Should we cache this too?
        public void importResource(Path resourceFileRelativePathInAssetCollection, IResourceLocator sourceResource) throws IOException {
            IResourceLocator targetResourceLocator = getAssetResourceLocator(resourceFileRelativePathInAssetCollection);
            try (InputStream inputStream = sourceResource.inputStream()) {
                targetResourceLocator.copyFrom(inputStream);
            }
        }

        public void importResourceDirectory(Path resourceDirectoryRelativePathInAssetCollection, IDirectoryLocator sourceDirectory) throws IOException {
            IDirectoryLocator targetDirectoryLocator = getAssetDirectoryLocator(resourceDirectoryRelativePathInAssetCollection);
            sourceDirectory.copyContentsTo(targetDirectoryLocator);
        }

        // Raw access to an asset resource - not cached
        public IResourceLocator getAssetResourceLocator(Path assetFileRelativePathInAssetCollection) {
            return mBaseDirectory.resolveResource(assetFileRelativePathInAssetCollection);
        }

        // Raw access to an asset directory - not cached
        public IDirectoryLocator getAssetDirectoryLocator(Path assetDirectoryRelativePathInAssetCollection) {
            return mBaseDirectory.resolveDirectory(assetDirectoryRelativePathInAssetCollection);
        }

        public void clearCache() {
            mRootMaterialSetsCache = null;
            mSupportObjectsCache = null;
            mImagesCache = null;
        }

        // TODO: Update cache entries
        protected <T extends AbstractAssetDescriptor> Collection<T> loadAssetDescriptors(Path relativeBasePath,
            AssetType assetType, String assetTypeSubDirectoryName, IAssetPathAnchor anchor, BiFunction<Path, AssetRefPath, T> descriptorLoader) throws IOException {
            IDirectoryLocator rootDirectory = mBaseDirectory.resolveDirectory(relativeBasePath);
            IDirectoryLocator assetTypeDirectory = rootDirectory.resolveDirectory(assetTypeSubDirectoryName);
            Path assetTypeBasePath = relativeBasePath.resolve(assetTypeSubDirectoryName);
            try {
                return assetTypeDirectory.exists()
                                ? assetTypeDirectory
                                    .list(pl -> pl instanceof IDirectoryLocator)
                                    .stream()
                                    .map(pl -> {
                                        String assetId = pl.getFileName();
                                        Path filePath = assetTypeBasePath.resolve(assetId);
                                        AssetRefPath ref = new AssetRefPath(assetType, anchor, filePath);
                                        return descriptorLoader.apply(filePath, ref);
                                    })
                                    .filter(ad -> ad != null)
                                    .collect(Collectors.toList())
                                : Collections.emptyList();
            } catch (IOException e) {
                throw new IOException("Error listing support objects from root path '" + anchor + "', path '" + assetTypeBasePath + "'", e);
            }
        }

        // In fact, parameter relativeBasePath is not necessary for this method because there are no ramified locations for support objects like for material sets.
        // But to make the API easier for the caller below, we provide that parameter.
        public Collection<SupportObjectDescriptor> loadSupportObjectDescriptors(Path relativeBasePath, IAssetPathAnchor anchor) throws IOException {
            return loadAssetDescriptors(relativeBasePath, AssetType.SupportObject, SUPPORT_OBJECTS_DIRECTORY, anchor, (assetBaseDirectory, assetRefPath) -> {
                try {
                    return loadSupportObjectDescriptor(assetBaseDirectory, assetRefPath);
                } catch (IOException e) {
                    log.warn("Error loading support object descriptor '" + assetRefPath + "' from path '" + assetBaseDirectory + "'");
                    return null; // null value will be filtered out from caller loadAssetDescriptors(...)
                }
            });
        }

        public Collection<MaterialSetDescriptor> loadMaterialSetDescriptors(Path relativeBasePath, IAssetPathAnchor anchor) throws IOException {
            return loadAssetDescriptors(relativeBasePath, AssetType.MaterialSet, MATERIAL_SETS_DIRECTORY, anchor, (assetBaseDirectory, assetRefPath) -> {
                try {
                    return loadMaterialSetDescriptor(assetBaseDirectory, assetRefPath);
                } catch (IOException e) {
                    log.warn("Error loading material set descriptor '" + assetRefPath + "' from path '" + assetBaseDirectory + "'");
                    return null; // null value will be filtered out from caller loadAssetDescriptors(...)
                }
            });
        }

        public void deleteAsset(Path assetBaseDirectoryPath) throws IOException {
            IDirectoryLocator assetDirectory = getAssetBaseDirectory(assetBaseDirectoryPath);
            assetDirectory.deleteRecursively();
        }
    }

    public class LibraryData implements Comparable<LibraryData> {
        protected final AssetLibrary mLibrary;
        protected final IDirectoryLocator mRootDirectory;
        protected final AssetCollection mAssetCollection;

        public LibraryData(AssetLibrary library, IDirectoryLocator libraryRootDirectory) {
            mLibrary = library;
            mRootDirectory = libraryRootDirectory;
            mAssetCollection = new AssetCollection(mRootDirectory);
        }

        public AssetLibrary getLibrary() {
            return mLibrary;
        }

        public IDirectoryLocator getRootDirectory() {
            return mRootDirectory;
        }

        // Not accessible to the outside
        protected AssetCollection getAssetCollection() {
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

    protected class PlanContext {
        protected final IDirectoryLocator mPlanFileDirectory;
        protected final AssetCollection mAssetCollection;

        public PlanContext(IDirectoryLocator planFileDirectory, AssetCollection assetCollection) {
            mPlanFileDirectory = planFileDirectory;
            mAssetCollection = assetCollection;
        }

        public IDirectoryLocator getPlanFileDirectory() {
            return mPlanFileDirectory;
        }

        public AssetCollection getAssetCollection() {
            return mAssetCollection;
        }
    }

    public class AssetLocation {
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
    }

    private static final Logger log = LoggerFactory.getLogger(AssetManager.class);

    public static final String LOCAL_RESOURCE_BASE = '/' + AssetLoader.class.getPackageName().replace('.', '/');

    public static final String MATERIAL_SETS_DIRECTORY = "MaterialSets";
    public static final String SUPPORT_OBJECTS_DIRECTORY = "SupportObjects";
    public static final String RESOURCES_DIRECTORY_NAME = "Resources";
    public static final String SUPPORT_OBJECT_DESCRIPTOR_NAME = "SupportObjectDescriptor.xml";
    public static final String MATERIAL_SET_DESCRIPTOR_NAME = "MaterialSetDescriptor.xml";

    public static final String ICON_IMAGE_DEFAULT_BASE_NAME = "OverviewIcon";
    public static final String PLAN_VIEW_IMAGE_DEFAULT_BASE_NAME = "PlanViewImage";

    public static final String MTL_FILE_DEFAULT_NAME = "Materials.mtl";
    public static final String OBJ_FILE_DEFAULT_NAME = "Model.obj";

    public static final String STORE_IMAGE_EXTENSION = "png";

    protected final AssetManagerConfiguration mConfiguration;

    protected final Map<String, RawMaterialData> mDefaultMaterials = new TreeMap<>(); // Material names to materials
    protected final Map<String, LibraryData> mAssetLibraries = new TreeMap<>(); // Ids to asset libraries

    protected Optional<PlanContext> mOPlanContext = Optional.empty(); // Set if there is a plan in context, value changes if another plan is opened

    public AssetManager(AssetManagerConfiguration config) {
        mConfiguration = config;
    }

    public static AssetManager create() {
        AssetManagerConfiguration config = AssetManagerConfiguration.from(Preferences.userNodeForPackage(AssetManager.class));
        AssetManager result = new AssetManager(config);
        Map<String, RawMaterialData> defaultMaterialData = DefaultMaterials.createDefaultMaterials();
        result.getDefaultMaterials().putAll(defaultMaterialData);
        return result;
    }

    /**
     * Gets the base directory of the current plan file, if present.
     */
    public Optional<IDirectoryLocator> getPlanBaseDirectory() {
        return mOPlanContext.map(pc -> pc.getPlanFileDirectory());
    }

    /**
     * Sets the path to the current plan file. This is necessary to resolve plan-local assets.
     */
    public void setPlanBaseDirectory(IDirectoryLocator value) {
        if (value == null) {
            mOPlanContext = Optional.empty();
        }
        mOPlanContext = Optional.of(new PlanContext(value, new AssetCollection(value)));
    }

    /**
     * Gets the opened asset libraries.
     * @return Map of asset library ids to corresponding asset library entries.
     */
    public Map<String, LibraryData> getAssetLibraries() {
        return mAssetLibraries;
    }

    public Map<String, RawMaterialData> getDefaultMaterials() {
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

    // TODO: Document, see usages
    protected AssetLocation getAssetLocation(IAssetPathAnchor anchor, Path assetBasePath) throws IOException {
        if (anchor instanceof PlanAssetPathAnchor) {
            AssetCollection assetCollection = mOPlanContext.map(pc -> pc.getAssetCollection()).orElseThrow(() -> planAnchorPathNotAvailableException());
            return new AssetLocation(assetCollection, assetBasePath);
        } else if (anchor instanceof LibraryAssetPathAnchor libraryPathAnchor) {
            String libraryId = libraryPathAnchor.getLibraryId();
            LibraryData libraryData = mAssetLibraries.get(libraryId);
            if (libraryData == null) {
                throw libraryIdUnknownException(libraryId);
            }
            return new AssetLocation(libraryData.getAssetCollection(), assetBasePath);
        } else if (anchor instanceof SupportObjectAssetPathAnchor supportObjectPathAnchor) {
            AssetLocation baseAssetLocation = getAssetLocation(supportObjectPathAnchor.getSupportObjectRef());
            return new AssetLocation(baseAssetLocation.getAssetCollection(), baseAssetLocation.getRelativePathInAssetCollection().resolve(assetBasePath));
        } else {
            throw new NotImplementedException("Resolving of asset path anchor <" + anchor + "> is not implemented");
        }
    }

    /**
     * Gets the base location of the asset for the given asset reference path.
     */
    public AssetLocation getAssetLocation(AssetRefPath ref) throws IOException {
        return getAssetLocation(ref.getAnchor(), ref.getAssetBasePath());
    }

    public static IResourceLocator resolveResourcesModel(AssetLocation assetLocation, FileModelResource model) throws FileNotFoundException {
        Path modelPath = Paths.get(RESOURCES_DIRECTORY_NAME).resolve(model.getRelativePath());
        IResourceLocator result = assetLocation.getAssetCollection().getAssetResourceLocator(assetLocation.getRelativePathInAssetCollection().resolve(modelPath));
        if (!result.exists()) {
            throw new FileNotFoundException("Resource '" + modelPath + "' is not present");
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
        AssetLocation assetLocation = getAssetLocation(supportObjectDescriptorRef);
        return assetLocation.getAssetCollection().loadSupportObjectDescriptor(assetLocation.getRelativePathInAssetCollection(), supportObjectDescriptorRef);
    }

    public MaterialSetDescriptor loadMaterialSetDescriptor(AssetRefPath materialSetDescriptorRef) throws IOException {
        AssetLocation assetLocation = getAssetLocation(materialSetDescriptorRef);
        return assetLocation.getAssetCollection().loadMaterialSetDescriptor(assetLocation.getRelativePathInAssetCollection(), materialSetDescriptorRef);
    }

    public void saveMaterialSetDescriptor(MaterialSetDescriptor descriptor) throws IOException {
        AssetRefPath refPath = descriptor.getSelfRef();
        AssetLocation assetLocation = getAssetLocation(refPath);
        assetLocation.getAssetCollection().saveMaterialSetDescriptor(assetLocation.getRelativePathInAssetCollection(), descriptor);
    }

    public void saveSupportObjectDescriptor(SupportObjectDescriptor descriptor) throws IOException {
        AssetRefPath refPath = descriptor.getSelfRef();
        AssetLocation assetLocation = getAssetLocation(refPath);
        assetLocation.getAssetCollection().saveSupportObjectDescriptor(assetLocation.getRelativePathInAssetCollection(), descriptor);
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
        AssetLocation assetLocation = getAssetLocation(anchor, Paths.get(""));
        return assetLocation.getAssetCollection().loadSupportObjectDescriptors(assetLocation.getRelativePathInAssetCollection(), anchor);
    }

    public Collection<MaterialSetDescriptor> loadMaterialSetDescriptors(IAssetPathAnchor anchor) throws IOException {
        AssetLocation assetLocation = getAssetLocation(anchor, Paths.get(""));
        return assetLocation.getAssetCollection().loadMaterialSetDescriptors(assetLocation.getRelativePathInAssetCollection(), anchor);
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
            result.addAll(loadMaterialSetDescriptors(new LibraryAssetPathAnchor(libraryId)));
        }
        return result;
    }

    public MaterialSetDescriptor createMaterialSet(IAssetPathAnchor anchor) throws IOException {
        String materialSetDescriptorId = IdGenerator.generateUniqueId(MaterialSetDescriptor.class);
        return createMaterialSet_PredefinedId(anchor, materialSetDescriptorId);
    }

    public MaterialSetDescriptor createMaterialSet_PredefinedId(IAssetPathAnchor anchor, String materialSetDescriptorId) throws IOException {
        Path filePath = Paths.get(MATERIAL_SETS_DIRECTORY + "/" + materialSetDescriptorId);
        AssetRefPath arp = new AssetRefPath(AssetType.MaterialSet, anchor, filePath);
        MaterialSetDescriptor result = new MaterialSetDescriptor(materialSetDescriptorId, arp);
        saveMaterialSetDescriptor(result); // Creates the directory structure

        AssetLoader assetLoader = buildAssetLoader();
        assetLoader.importAssetIconImage(result, getClassLoaderResourceLocator(AssetLoader.MATERIAL_SET_PLACEHOLDER_ICON_IMAGE),
            Optional.of(ICON_IMAGE_DEFAULT_BASE_NAME + "." + STORE_IMAGE_EXTENSION));
        assetLoader.importMaterialSetMtlResource(result, getClassLoaderResourceLocator(AssetLoader.TEMPLATE_MATERIAL_LIBRARY),
            Optional.of(MTL_FILE_DEFAULT_NAME));

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

        saveSupportObjectDescriptor(result);
        return result;
    }

    public void deleteAsset(AssetRefPath assetRefPath) throws IOException {
        if (assetRefPath.getOMaterialName().isPresent()) {
            throw new IllegalArgumentException("Single material in material set cannot be deleted separately (material set ref path: '" + assetRefPath + "')");
        }
        AssetLocation assetLocation = getAssetLocation(assetRefPath);
        Path assetBaseDirectoryPath = assetLocation.getRelativePathInAssetCollection();
        try {
            assetLocation.getAssetCollection().deleteAsset(assetBaseDirectoryPath);
        } catch (IOException e) {
            throw new IOException("Error deleting asset '" + assetRefPath + "' from path '" + assetBaseDirectoryPath + "'", e);
        }
    }

    /////////////////////////////////////////////////////// Resource access ////////////////////////////////////////////////////////////////

    protected static IResourceLocator getClassLoaderResourceLocator(String localResourceName) {
        return new ClassLoaderFileSystemResourceLocator(Path.of(
            LOCAL_RESOURCE_BASE + "/" + localResourceName), AssetLoader.class.getModule());
    }

    public static Image loadImage(IResourceLocator imageFileLocator) throws IOException {
        try (InputStream is = imageFileLocator.inputStream()) {
            return new Image(is);
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
