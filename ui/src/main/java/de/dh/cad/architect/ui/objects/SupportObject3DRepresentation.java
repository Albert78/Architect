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
package de.dh.cad.architect.ui.objects;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dh.cad.architect.model.assets.AssetRefPath;
import de.dh.cad.architect.model.assets.SupportObjectDescriptor;
import de.dh.cad.architect.model.changes.IModelChange;
import de.dh.cad.architect.model.objects.SupportObject;
import de.dh.cad.architect.model.objects.SurfaceConfiguration;
import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.assets.AssetLoader;
import de.dh.cad.architect.ui.assets.AssetManager;
import de.dh.cad.architect.ui.assets.ThreeDObject;
import de.dh.cad.architect.ui.controller.UiController;
import de.dh.cad.architect.ui.utils.CoordinateUtils;
import de.dh.cad.architect.ui.view.threed.Abstract3DView;
import de.dh.cad.architect.ui.view.threed.ThreeDView;
import de.dh.utils.MaterialMapping;
import de.dh.utils.Vector2D;
import de.dh.utils.io.fx.FxMeshBuilder;
import de.dh.utils.io.obj.RawMaterialData;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Shape3D;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;

public class SupportObject3DRepresentation extends AbstractSolid3DRepresentation {
    private static final Logger log = LoggerFactory.getLogger(SupportObject3DRepresentation.class);

    protected Group mObjectViewRoot;
    protected Map<String, PhongMaterial> mOrigModelMaterials;
    protected Bounds mRawBounds;
    protected Scale mScale = new Scale();
    protected Rotate mRotation = new Rotate();
    protected Translate mTranslation = new Translate();

    public SupportObject3DRepresentation(SupportObject supportObject, Abstract3DView parentView) {
        super(supportObject, parentView);
        initializeNode();

        ChangeListener<Boolean> bPropertiesUpdaterListener = (observable, oldValue, newValue) -> updateProperties();
        selectedProperty().addListener(bPropertiesUpdaterListener);
        objectFocusedProperty().addListener(bPropertiesUpdaterListener);
        objectEmphasizedProperty().addListener(bPropertiesUpdaterListener);
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
        AssetManager assetManager = assetLoader.getAssetManager();
        AssetRefPath supportObjectDescriptorRef = supportObject.getSupportObjectDescriptorRef();

        ThreeDObject object;
        try {
            SupportObjectDescriptor descriptor = assetManager.loadSupportObjectDescriptor(supportObjectDescriptorRef);

            object = assetLoader.loadSupportObject3DResource(descriptor);
            mOrigModelMaterials = new HashMap<>();
            for (MeshView mv : object.getSurfaceMeshViews()) {
                PhongMaterial material = (PhongMaterial) mv.getMaterial();
                mOrigModelMaterials.put(mv.getId(), material);
            }
        } catch (IOException e) {
            log.error("Error creating 3D representation for support object <" + supportObjectDescriptorRef + ">", e);
            object = AssetLoader.loadSupportObjectPlaceholder3DResource();
        }

        Collection<MeshView> meshViews = object.getSurfaceMeshViews();
        mObjectViewRoot = new Group();
        mObjectViewRoot.getChildren().addAll(meshViews);

        ObservableList<Transform> transforms = mObjectViewRoot.getTransforms();

        // Object rotation correction plus rotation to JavaFx coordinate system
        Transform transform = object.getTransform(CoordinateUtils.createTransformArchitectToJavaFx());
        transforms.add(transform);
        Bounds bounds = mObjectViewRoot.getBoundsInParent();
        mRawBounds = bounds;
        // Translate object to center X/Y and put its bottom on the ground
        // TODO: When we support multiple levels, take level into account for the Z coordinate
        Translate t = new Translate(
            -bounds.getWidth() / 2 - bounds.getMinX(),
            -bounds.getHeight() / 2 - bounds.getMinY(),
            -bounds.getMaxZ());
        transforms.addAll(0, Arrays.asList(mTranslation, mRotation, mScale, t));
        add(mObjectViewRoot);

        // Prepare surfaces
        for (MeshView meshView : meshViews) {
            String surfaceTypeId = meshView.getId();
            if (surfaceTypeId == null) {
                continue;
            }
            SurfaceConfiguration sc = supportObject.getSurfaceTypeIdsToSurfaceConfigurations().get(surfaceTypeId);
            if (sc == null) {
                // Loaded surface doesn't have a surface entry in our model, that means the 3D model is not in sync with our
                // support object anymore.
                // --> Entry must be updated to match the model
                continue;
            }

            SurfaceData<MeshView> sd = new SurfaceData<>(this, surfaceTypeId, meshView);
            registerSurface(sd);
        }
    }

    public void resetSupportObjectSurfaces(SupportObject supportObject, UiController uiController) {
        AssetManager assetManager = uiController.getAssetManager();
        AssetLoader assetLoader = assetManager.buildAssetLoader();
        ThreeDObject obj = assetLoader.loadSupportObject3DObject(supportObject.getSupportObjectDescriptorRef(), false);
        if (obj == null) {
            return;
        }
        Set<String> meshIds = obj.getSurfaceMeshViews().stream().map(Node::getId).collect(Collectors.toSet());
        List<IModelChange> changeTrace = new ArrayList<>();
        supportObject.initializeSurfaces(meshIds, changeTrace);
        initializeNode();
        uiController.notifyChange(changeTrace, Strings.SUPPORT_OBJECT_SET_PROPERTY_CHANGE);
    }

    protected void updateProperties() {
        SupportObject supportObject = getSupportObject();
        for (SurfaceData<? extends Shape3D> surfaceData : mSurfacesByTypeId.values()) {
            String surfaceTypeId = surfaceData.getSurfaceTypeId();
            SurfaceConfiguration surfaceConfiguration = supportObject.getSurfaceTypeIdsToSurfaceConfigurations().get(surfaceTypeId);
            de.dh.cad.architect.model.objects.MaterialMappingConfiguration mmc = surfaceConfiguration.getMaterialMappingConfiguration();
            if (mmc == null) {
                surfaceData.setMaterial(mOrigModelMaterials.get(surfaceTypeId));
            } else {
                AssetLoader assetLoader = getAssetLoader();
                // TODO: Somehow find the surface size of the surface in texture direction....
                Optional<Vector2D> oSurfaceSize = Optional.empty();
                PhongMaterial material = assetLoader.buildMaterial(mmc, oSurfaceSize);
                surfaceData.setMaterial(material);
            }
        }
    }

    protected void updateAlignment() {
        SupportObject supportObject = getSupportObject();
        Point2D center = CoordinateUtils.positionToPoint2D(supportObject.getHandleAnchor().getPosition().projectionXY());
        Vector2D size = CoordinateUtils.dimensions2DToUiVector2D(supportObject.getSize());
        double height = CoordinateUtils.lengthToCoords(supportObject.getHeight(), null);
        double elevation = CoordinateUtils.lengthToCoords(supportObject.getElevation(), null);
        Point3D rotationAxis = new Point3D(0, 0, 1);
        // Rotation direction is negated against global coordinate system
        float rotationDeg = -supportObject.getRotationDeg();

        mScale.setX(size.getX() / mRawBounds.getWidth());
        mScale.setY(size.getY() / mRawBounds.getHeight());
        mScale.setZ(height / mRawBounds.getDepth());
        mRotation.setAxis(rotationAxis);
        mRotation.setAngle(rotationDeg);
        mTranslation.setX(center.getX());
        mTranslation.setY(center.getY());
        mTranslation.setZ(-elevation);
    }

    @Override
    public void updateToModel() {
        super.updateToModel();
        updateAlignment();
        updateProperties();
    }
}
