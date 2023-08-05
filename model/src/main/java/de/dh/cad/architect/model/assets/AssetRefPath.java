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
package de.dh.cad.architect.model.assets;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;

// Examples for asset ref paths:

// Support objects: (Descriptor)
// Normal in library (Case 1):
// /Libraries/c5704b87-f568-4711-b11a-0935e9735c80/SupportObjects/484ff4d7-fcc9-4939-8a0c-74b844c9b4ec
// In plan (Case 2):
// /Plan/SupportObjects/42264bae-8a92-4200-86e1-ab970bf0f5f4

// MaterialSets: (Descriptor)
// Normal in library (Case 3):
// /Libraries/c5704b87-f568-4711-b11a-0935e9735c80/MaterialSets/9a51e220-be8d-4e8d-96f1-e6052097814c
// In plan (Case 4):
// /Plan/MaterialSets/ec3ca4bf-1a04-4b82-920d-38e9b5e21464
// Local material in a Support Objekt in library (Case 5):
// /Libraries/c5704b87-f568-4711-b11a-0935e9735c80/SupportObjects/e264bad9-2ec9-47b9-b08c-867c885f9a3d/MaterialSets/9a51e220-be8d-4e8d-96f1-e6052097814c
// Local material in a Support Objekt in plan (Case 6):
// /Plan/SupportObjects/e264bad9-2ec9-47b9-b08c-867c885f9a3d/MaterialSets/9a51e220-be8d-4e8d-96f1-e6052097814c

// The name of the descriptor file is not contained in the FilePath section.

// Material: (Single named material)
// [...Material-Descriptor-Ref-Path...]/[Material-Name]

// Anchor / FilePath [/ Material-Name]
//
// Anchor:
// /Libraries/[Library-ID]
// /Plan
//
// FilePath:
// /SupportObjects/[ID]
// /MaterialSets/[ID]
// /SupportObjects/[ID]/MaterialSets/[ID]
//
// Material name:
// /brown-choclate
public class AssetRefPath {
    public static interface IAssetPathAnchor {
        // Intentionally empty
    }

    public static class LibraryAssetPathAnchor implements IAssetPathAnchor {
        protected final String mLibraryId;

        public LibraryAssetPathAnchor(String libraryId) {
            mLibraryId = libraryId;
        }

        public String getLibraryId() {
            return mLibraryId;
        }

        @Override
        public String toString() {
            return "Library '" + mLibraryId + "'";
        }
    }

    public static class SupportObjectAssetPathAnchor implements IAssetPathAnchor {
        protected final AssetRefPath mSupportObjectRef;

        public SupportObjectAssetPathAnchor(AssetRefPath supportObjectRef) {
            mSupportObjectRef = supportObjectRef;
        }

        public AssetRefPath getSupportObjectRef() {
            return mSupportObjectRef;
        }

        @Override
        public String toString() {
            return "Support object '" + mSupportObjectRef + "'";
        }
    }

    public static class PlanAssetPathAnchor implements IAssetPathAnchor {
        @Override
        public String toString() {
            return "Plan";
        }
    }

    protected final AssetType mAssetType;
    protected final IAssetPathAnchor mAnchor;
    protected final Path mAssetBasePath;
    protected final Optional<String> mOMaterialName;

    public AssetRefPath(AssetType assetType, IAssetPathAnchor anchor, Path assetBasePath, Optional<String> oMaterialName) {
        mAssetType = assetType;
        mAnchor = anchor;
        mAssetBasePath = assetBasePath;
        mOMaterialName = oMaterialName;
    }

    public AssetRefPath(AssetType assetType, IAssetPathAnchor anchor, Path assetBasePath) {
        this(assetType, anchor, assetBasePath, Optional.empty());
    }

    protected static final String FILE_NAME_PATTERN_STR = "[A-Za-z0-9 \\!\\@\\#\\$\\%\\^\\&\\(\\)\\'\\;\\{\\}\\[\\]\\=\\+\\-\\_\\~\\`\\.&&[^/]]+";
    protected static final String PLAN_ANCHOR_PATTERN_STR = "/Plan/";
    protected static final String LIBRARY_ANCHOR_PATTERN_STR = "/Libraries/" + "(" + FILE_NAME_PATTERN_STR + ")/";
    protected static final String SUPPORT_OBJECT_PATH_SEGMENT_STR = "SupportObjects/" + FILE_NAME_PATTERN_STR;
    protected static final String MATERIAL_SETS_PATH_SEGMENT_STR = "MaterialSets/" + FILE_NAME_PATTERN_STR;
    protected static final String OPTIONAL_MATERIAL_NAME_SUFFIX_PATTERN_STR = "(/\\S+)??";

    // Case 1
    protected static final Pattern LIBRARY_SUPPORT_OBJECT_PATH_PATTERN =
                    Pattern.compile("^" + LIBRARY_ANCHOR_PATTERN_STR + "(" + SUPPORT_OBJECT_PATH_SEGMENT_STR + ")" + "$");
    // Case 2
    protected static final Pattern PLAN_SUPPORT_OBJECT_PATH_PATTERN =
                    Pattern.compile("^" + PLAN_ANCHOR_PATTERN_STR + "(" + SUPPORT_OBJECT_PATH_SEGMENT_STR + ")" + "$");

    // Case 3
    protected static final Pattern LIBRARY_MATERIAL_S_PATH_PATTERN =
                    Pattern.compile("^" + LIBRARY_ANCHOR_PATTERN_STR + "(" + MATERIAL_SETS_PATH_SEGMENT_STR + ")" + OPTIONAL_MATERIAL_NAME_SUFFIX_PATTERN_STR + "$");
    // Case 4
    protected static final Pattern PLAN_MATERIAL_S_PATH_PATTERN =
                    Pattern.compile("^" + PLAN_ANCHOR_PATTERN_STR + "(" + MATERIAL_SETS_PATH_SEGMENT_STR + ")" + OPTIONAL_MATERIAL_NAME_SUFFIX_PATTERN_STR + "$");
    // Case 5
    protected static final Pattern LIBRARY_SUPPORT_OBJECT_MATERIAL_S_PATH_PATTERN =
                    Pattern.compile("^" + LIBRARY_ANCHOR_PATTERN_STR + "(" + SUPPORT_OBJECT_PATH_SEGMENT_STR + MATERIAL_SETS_PATH_SEGMENT_STR + ")" + OPTIONAL_MATERIAL_NAME_SUFFIX_PATTERN_STR + "$");
    // Case 6
    protected static final Pattern PLAN_SUPPORT_OBJECT_MATERIAL_S_PATH_PATTERN =
                    Pattern.compile("^" + PLAN_ANCHOR_PATTERN_STR + "(" + SUPPORT_OBJECT_PATH_SEGMENT_STR + "/" + MATERIAL_SETS_PATH_SEGMENT_STR + ")" + OPTIONAL_MATERIAL_NAME_SUFFIX_PATTERN_STR + "$");

    public static AssetRefPath parse(String path) {
        // We could use regex matches if already supported in switch statement...
        Matcher matcher;
        matcher = LIBRARY_SUPPORT_OBJECT_PATH_PATTERN.matcher(path);
        if (matcher.find()) {
            String libraryId = matcher.group(1);
            IAssetPathAnchor anchor = new LibraryAssetPathAnchor(libraryId);
            Path filePath = Paths.get(matcher.group(2));
            return new AssetRefPath(AssetType.SupportObject, anchor, filePath, Optional.empty());
        }
        matcher = PLAN_SUPPORT_OBJECT_PATH_PATTERN.matcher(path);
        if (matcher.find()) {
            IAssetPathAnchor anchor = new PlanAssetPathAnchor();
            Path filePath = Paths.get(matcher.group(1));
            return new AssetRefPath(AssetType.SupportObject, anchor, filePath, Optional.empty());
        }
        matcher = LIBRARY_MATERIAL_S_PATH_PATTERN.matcher(path);
        if (matcher.find()) {
            String libraryId = matcher.group(1);
            IAssetPathAnchor anchor = new LibraryAssetPathAnchor(libraryId);
            Path filePath = Paths.get(matcher.group(2));
            String materialNameSegment = matcher.group(3);
            return new AssetRefPath(AssetType.MaterialSet, anchor, filePath, extractOptionalMaterialName(materialNameSegment, path));
        }
        matcher = PLAN_MATERIAL_S_PATH_PATTERN.matcher(path);
        if (matcher.find()) {
            IAssetPathAnchor anchor = new PlanAssetPathAnchor();
            Path filePath = Paths.get(matcher.group(1));
            String materialNameSegment = matcher.group(2);
            return new AssetRefPath(AssetType.MaterialSet, anchor, filePath, extractOptionalMaterialName(materialNameSegment, path));
        }
        matcher = LIBRARY_SUPPORT_OBJECT_MATERIAL_S_PATH_PATTERN.matcher(path);
        if (matcher.find()) {
            String libraryId = matcher.group(1);
            IAssetPathAnchor anchor = new LibraryAssetPathAnchor(libraryId);
            Path filePath = Paths.get(matcher.group(2));
            String materialNameSegment = matcher.group(3);
            return new AssetRefPath(AssetType.MaterialSet, anchor, filePath, extractOptionalMaterialName(materialNameSegment, path));
        }
        matcher = PLAN_SUPPORT_OBJECT_MATERIAL_S_PATH_PATTERN.matcher(path);
        if (matcher.find()) {
            IAssetPathAnchor anchor = new PlanAssetPathAnchor();
            Path filePath = Paths.get(matcher.group(1));
            String materialNameSegment = matcher.group(2);
            return new AssetRefPath(AssetType.MaterialSet, anchor, filePath, extractOptionalMaterialName(materialNameSegment, path));
        }
        throw new RuntimeException("Error parsing asset ref path '" + path + "'");
    }

    protected static Optional<String> extractOptionalMaterialName(String materialNameSegment, String path) {
        if (StringUtils.isEmpty(materialNameSegment)) {
            return Optional.empty();
        }
        if (materialNameSegment.length() < 2) {
            throw new RuntimeException("Error parsing material name segment in asset ref path '" + path + "'");
        }
        return Optional.of(materialNameSegment.substring(1));
    }

    public AssetType getAssetType() {
        return mAssetType;
    }

    public IAssetPathAnchor getAnchor() {
        return mAnchor;
    }

    public Path getAssetBasePath() {
        return mAssetBasePath;
    }

    /**
     * Gets the id of the support object or the material set.
     * Note that if this is a material ref path, the {@link #getOMaterialName() material name} is also important.
     */
    public String getAssetId() {
        return mAssetBasePath.getFileName().toString();
    }

    public Optional<String> getOMaterialName() {
        return mOMaterialName;
    }

    /**
     * Returns this asset ref path in serialized form, parseable by {@link #parse(String)}.
     */
    public String toPathString() {
        StringBuilder result = new StringBuilder();
        if (mAnchor instanceof PlanAssetPathAnchor) {
            result.append("/Plan");
        } else if (mAnchor instanceof LibraryAssetPathAnchor lapa) {
            result.append("/Libraries/" + lapa.getLibraryId());
        } else {
            throw new NotImplementedException("Asset ref path anchor " + mAnchor);
        }
        for (Path path : mAssetBasePath) {
            result.append("/" + path.toString());
        }
        mOMaterialName.ifPresent(materialName -> result.append("/" + materialName));
        return result.toString();
    }

    public AssetRefPath withMaterialName(String materialName) {
        return new AssetRefPath(mAssetType, mAnchor, mAssetBasePath, Optional.of(materialName));
    }

    public AssetRefPath withoutMaterialName() {
        return new AssetRefPath(mAssetType, mAnchor, mAssetBasePath);
    }

    @Override
    public int hashCode() {
        return toPathString().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof AssetRefPath)) {
            return false;
        }
        AssetRefPath other = (AssetRefPath) obj;
        return toPathString().equals(other.toPathString());
    }

    @Override
    public String toString() {
        return toPathString();
    }
}
