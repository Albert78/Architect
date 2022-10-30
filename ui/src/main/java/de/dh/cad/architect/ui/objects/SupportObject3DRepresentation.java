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
package de.dh.cad.architect.ui.objects;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dh.cad.architect.model.assets.AssetRefPath;
import de.dh.cad.architect.model.objects.SupportObject;
import de.dh.cad.architect.model.objects.SurfaceConfiguration;
import de.dh.cad.architect.ui.assets.AssetLoader;
import de.dh.cad.architect.ui.assets.AssetManager;
import de.dh.cad.architect.ui.assets.ThreeDObject;
import de.dh.cad.architect.ui.controller.UiController;
import de.dh.cad.architect.ui.utils.CoordinateUtils;
import de.dh.cad.architect.ui.view.threed.Abstract3DView;
import de.dh.cad.architect.ui.view.threed.ThreeDView;
import de.dh.utils.fx.Vector2D;
import de.dh.utils.fx.io.formats.obj.RawMaterialData;
import javafx.beans.InvalidationListener;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;

public class SupportObject3DRepresentation extends Abstract3DRepresentation {
    private static Logger log = LoggerFactory.getLogger(SupportObject3DRepresentation.class);

    protected static class SupportObjectSurfaceData extends SurfaceData {
        protected PhongMaterial mOriginalMaterial;
        protected Color mOriginalDiffuseColor;

        public SupportObjectSurfaceData(String surfaceId, MeshView meshViewWithPhongMaterial) {
            super(surfaceId, meshViewWithPhongMaterial);
            PhongMaterial originalMaterial = getMaterial();
            mOriginalMaterial = originalMaterial;
            mOriginalDiffuseColor = originalMaterial.getDiffuseColor();
        }

        public PhongMaterial getOriginalMaterial() {
            return mOriginalMaterial;
        }

        public Color getOriginalDiffuseColor() {
            return mOriginalDiffuseColor;
        }

        public void resetOriginalMaterial() {
            mOriginalMaterial.setDiffuseColor(mOriginalDiffuseColor);
        }
    }

    protected Group mObjectViewRoot;
    protected Bounds mRawBounds;
    protected Collection<SupportObjectSurfaceData> mSurfaces = new ArrayList<>();
    protected Scale mScale = new Scale();
    protected Rotate mRotation = new Rotate();
    protected Translate mTranslation = new Translate();

    public SupportObject3DRepresentation(SupportObject supportObject, Abstract3DView parentView) {
        super(supportObject, parentView);
        initializeNode();

        InvalidationListener updatePropertiesListener = change -> {
            updateProperties();
        };
        selectedProperty().addListener(updatePropertiesListener);
        objectSpottedProperty().addListener(updatePropertiesListener);
    }

    public SupportObject getSupportObject() {
        return (SupportObject) mModelObject;
    }

    protected ThreeDView getParentView() {
        return (ThreeDView) mParentView;
    }

    public void initializeNode() {
        SupportObject supportObject = getSupportObject();
        AssetLoader assetLoader = getAssetLoader();
        Map<String, AssetRefPath> overriddenSurfaceMaterialRefs = supportObject.getOverriddenSurfaceMaterialRefs();
        Map<String, RawMaterialData> overriddenMaterials;
        try {
            overriddenMaterials = assetLoader.loadMaterialData(overriddenSurfaceMaterialRefs);
        } catch (IOException e) {
            log.error("Error loading overridden materials", e);
            overriddenMaterials = Collections.emptyMap();
        }
        // Prepare object
        ThreeDObject object = assetLoader.loadSupportObject3DObject(supportObject.getSupportObjectDescriptorRef(), Optional.of(overriddenMaterials), true);
        Collection<MeshView> meshViews = object.getSurfaces();
        mObjectViewRoot = new Group();
        mObjectViewRoot.getChildren().addAll(meshViews);
        Optional<Transform> oTrans = object.getORootTransformation();
        Rotate r = new Rotate(90, new Point3D(1, 0, 0));
        ObservableList<Transform> transforms = mObjectViewRoot.getTransforms();
        transforms.add(r);
        oTrans.ifPresent(trans -> transforms.add(trans));
        Bounds origBounds = mObjectViewRoot.getBoundsInParent();
        Translate t = new Translate(
            -origBounds.getWidth() / 2 - origBounds.getMinX(),
            -origBounds.getHeight() / 2 - origBounds.getMinY(),
            -origBounds.getMaxZ());
        mRawBounds = mObjectViewRoot.getBoundsInParent();
        transforms.addAll(0, Arrays.asList(mTranslation, mRotation, mScale, t));
        add(mObjectViewRoot);

        // Prepare surfaces
        for (MeshView meshView : meshViews) {
            String surfaceTypeId = meshView.getId();
            if (surfaceTypeId == null) {
                continue;
            }
            SupportObjectSurfaceData sd = new SupportObjectSurfaceData(surfaceTypeId, meshView);
            mSurfaces.add(sd);

            SurfaceConfiguration sc = supportObject.getSurfaceTypeIdsToSurfaceConfigurations().get(surfaceTypeId);
            markSurfaceNode(meshView, new ObjectSurface(this, surfaceTypeId) {
                @Override
                public AssetRefPath getMaterialRef() {
                    return sc == null ? null : sc.getMaterialAssignment();
                }

                @Override
                public boolean assignMaterial(AssetRefPath materialRef) {
                    if (sc == null) {
                        return false;
                    }
                    sc.setMaterialAssignment(materialRef);
                    mParentView.getUiController().notifyObjectsChanged(supportObject);
                    return true;
                }
            });
        }
    }

    public void resetSupportObjectSurfaces(SupportObject supportObject, UiController uiController) {
        AssetManager assetManager = uiController.getAssetManager();
        AssetLoader assetLoader = assetManager.buildAssetLoader();
        ThreeDObject obj = assetLoader.loadSupportObject3DObject(supportObject.getSupportObjectDescriptorRef(), Optional.empty(), false);
        if (obj == null) {
            return;
        }
        Set<String> meshIds = obj.getSurfaces().stream().map(mv -> mv.getId()).collect(Collectors.toSet());
        supportObject.initializeSurfaces(meshIds);
        initializeNode();
        uiController.notifyObjectsChanged(supportObject);
    }

    protected void updateProperties() {
        SupportObject supportObject = getSupportObject();
        AssetLoader assetLoader = getAssetLoader();
        for (SupportObjectSurfaceData surfaceData : mSurfaces) {
            String surfaceTypeId = surfaceData.getSurfaceTypeId();
            MeshView meshView = surfaceData.getMeshView();
            SurfaceConfiguration surfaceConfiguration = supportObject.getSurfaceTypeIdsToSurfaceConfigurations().get(surfaceTypeId);
            AssetRefPath materialRefPath = surfaceConfiguration == null ? null : surfaceConfiguration.getMaterialAssignment();
            if (materialRefPath == null) {
                surfaceData.resetOriginalMaterial();
                meshView.setMaterial(surfaceData.getOriginalMaterial());
            } else {
                assetLoader.configureMaterial(meshView, materialRefPath, Optional.empty());
            }
            Color color = null;
            if (isSelected()) {
                color = SELECTED_OBJECTS_COLOR;
            }
            if (isObjectSpotted()) {
                color = Color.BLUE.interpolate(Color.DEEPSKYBLUE, 0.5);
            }
            if (color != null) {
                surfaceData.getMaterial().setDiffuseColor(color);
            }
        }
    }

    protected void updateAlignment() {
        SupportObject supportObject = getSupportObject();
        Point2D center = CoordinateUtils.positionToPoint2D(supportObject.getHandleAnchor().getPosition().projectionXY());
        Vector2D size = CoordinateUtils.dimensions2DToUiVector2D(supportObject.getSize());
        double height = CoordinateUtils.lengthToCoords(supportObject.getHeight());
        double elevation = CoordinateUtils.lengthToCoords(supportObject.getElevation());
        Point3D rotationAxis = new Point3D(0, 0, 1);
        float rotationDeg = supportObject.getRotationDeg();

        mScale.setX(size.getX() / mRawBounds.getWidth());
        mScale.setY(size.getY() / mRawBounds.getHeight());
        mScale.setZ(height / mRawBounds.getDepth());
        mRotation.setAxis(rotationAxis);
        mRotation.setAngle(rotationDeg);
        mTranslation.setX(center.getX());
        mTranslation.setY(center.getY());
        mTranslation.setZ(-elevation); // Negative Z is up
    }

    @Override
    public void updateToModel() {
        super.updateToModel();
        updateAlignment();
        updateProperties();
    }
}
