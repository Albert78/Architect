package de.dh.cad.architect.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.dh.cad.architect.model.assets.AssetRefPath;
import de.dh.cad.architect.model.assets.AssetRefPath.IAssetPathAnchor;
import de.dh.cad.architect.model.assets.AssetRefPath.LibraryAssetPathAnchor;
import de.dh.cad.architect.model.assets.AssetRefPath.PlanAssetPathAnchor;
import de.dh.cad.architect.model.assets.AssetType;

/**
 * Test class for {@link AssetRefPath}, see description there.
 */
public class AssetRefPathTest {
    protected static final String libraryId = "c5704b87-f568-4711-b11a-0935e9735c80";
    protected static final String planId = "e37040f1-4d10-4ebd-8bda-3640024f4673";
    protected static final String supportObjectId = "484ff4d7-fcc9-4939-8a0c-74b844c9b4ec";
    protected static final String materialSetId = "9a51e220-be8d-4e8d-96f1-e6052097814c";
    protected static final String materialName = "brown-choclate";

    @Test
    @DisplayName("Support Object in Library (Case 1)")
    public void testCase1() {
        String path = "/Libraries/" + libraryId + "/SupportObjects/" + supportObjectId;

        AssetRefPath refPath = AssetRefPath.parse(path);
        AssetType assetType = refPath.getAssetType();
        assertEquals(assetType, AssetType.SupportObject, "Asset type should be SupportObject");

        IAssetPathAnchor anchor = refPath.getAnchor();
        assertEquals(anchor, new LibraryAssetPathAnchor(libraryId), "Anchor does not match");

        String assetId = refPath.getAssetId();
        assertEquals(assetId, supportObjectId, "Id of SupportObject does not match");
    }

    @Test
    @DisplayName("Support Object in Plan (Case 2)")
    public void testCase2() {
        String path = "/Plan/" + planId + "/SupportObjects/" + supportObjectId;

        AssetRefPath refPath = AssetRefPath.parse(path);
        AssetType assetType = refPath.getAssetType();
        assertEquals(assetType, AssetType.SupportObject, "Asset type should be SupportObject");

        IAssetPathAnchor anchor = refPath.getAnchor();
        assertEquals(anchor, new PlanAssetPathAnchor(planId), "Anchor does not match");

        String assetId = refPath.getAssetId();
        assertEquals(assetId, supportObjectId, "Id of SupportObject does not match");
    }

    @Test
    @DisplayName("Material Set in Library (Case 3)")
    public void testCase3() {
        String path = "/Libraries/" + libraryId + "/MaterialSets/" + materialSetId;

        AssetRefPath refPath = AssetRefPath.parse(path);

        AssetType assetType = refPath.getAssetType();
        assertEquals(assetType, AssetType.MaterialSet, "Asset type should be MaterialSet");

        IAssetPathAnchor anchor = refPath.getAnchor();
        assertEquals(anchor, new LibraryAssetPathAnchor(libraryId), "Anchor does not match");

        String assetId = refPath.getAssetId();
        assertEquals(assetId, materialSetId, "Id of Material Set does not match");

        AssetRefPath materialRefPath = AssetRefPath.parse(path + "/" + materialName);

        assertEquals(materialRefPath.getOMaterialName().orElse(null), materialName, "Material name does not match");
    }

    @Test
    @DisplayName("Material Set in Plan (Case 4)")
    public void testCase4() {
        String path = "/Plan/" + planId + "/MaterialSets/" + materialSetId;

        AssetRefPath refPath = AssetRefPath.parse(path);
        AssetType assetType = refPath.getAssetType();
        assertEquals(assetType, AssetType.MaterialSet, "Asset type should be MaterialSet");

        IAssetPathAnchor anchor = refPath.getAnchor();
        assertEquals(anchor, new PlanAssetPathAnchor(planId), "Anchor does not match");

        String assetId = refPath.getAssetId();
        assertEquals(assetId, materialSetId, "Id of Material Set does not match");

        AssetRefPath materialRefPath = AssetRefPath.parse(path + "/" + materialName);

        assertEquals(materialRefPath.getOMaterialName().orElse(null), materialName, "Material name does not match");
    }

    @Test
    @DisplayName("Local Material Set in Support Object in library (Case 5)")
    public void testCase5() {
        String path = "/Libraries/" + libraryId + "/SupportObjects/" + supportObjectId + "/MaterialSets/" + materialSetId;

        AssetRefPath refPath = AssetRefPath.parse(path);
        AssetType assetType = refPath.getAssetType();
        assertEquals(assetType, AssetType.MaterialSet, "Asset type should be MaterialSet");

        IAssetPathAnchor anchor = refPath.getAnchor();
        LibraryAssetPathAnchor libraryAnchor = new LibraryAssetPathAnchor(libraryId);
        assertEquals(anchor, libraryAnchor, "Anchor does not match");

        String assetId = refPath.getAssetId();
        assertEquals(assetId, materialSetId, "Id of Material Set does not match");

        AssetRefPath materialRefPath = AssetRefPath.parse(path + "/" + materialName);

        assertEquals(materialRefPath.getOMaterialName().orElse(null), materialName, "Material name does not match");
    }

    @Test
    @DisplayName("Local Material Set in Support Object in Plan (Case 6)")
    public void testCase6() {
        String path = "/Plan/" + planId + "/SupportObjects/" + supportObjectId + "/MaterialSets/" + materialSetId;

        AssetRefPath refPath = AssetRefPath.parse(path);
        AssetType assetType = refPath.getAssetType();
        assertEquals(assetType, AssetType.MaterialSet, "Asset type should be MaterialSet");

        IAssetPathAnchor anchor = refPath.getAnchor();
        PlanAssetPathAnchor planAnchor = new PlanAssetPathAnchor(planId);
        assertEquals(anchor, planAnchor, "Anchor does not match");

        String assetId = refPath.getAssetId();
        assertEquals(assetId, materialSetId, "Id of Material Set does not match");

        AssetRefPath materialRefPath = AssetRefPath.parse(path + "/" + materialName);

        assertEquals(materialRefPath.getOMaterialName().orElse(null), materialName, "Material name does not match");
    }
}
