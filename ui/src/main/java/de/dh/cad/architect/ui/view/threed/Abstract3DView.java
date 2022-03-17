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
package de.dh.cad.architect.ui.view.threed;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import de.dh.cad.architect.fx.nodes.CombinedTransformGroup;
import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.model.objects.BaseObject;
import de.dh.cad.architect.ui.controller.UiController;
import de.dh.cad.architect.ui.objects.Abstract3DAncillaryObject;
import de.dh.cad.architect.ui.objects.Abstract3DRepresentation;
import de.dh.cad.architect.ui.objects.AbstractObjectUIRepresentation;
import de.dh.cad.architect.ui.objects.ObjectTypesRegistry;
import de.dh.cad.architect.ui.utils.CoordinateUtils;
import de.dh.cad.architect.ui.view.AbstractPlanView;
import de.dh.cad.architect.ui.view.threed.behaviors.ThreeDNullBehavior;
import javafx.collections.ObservableList;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

public abstract class Abstract3DView extends AbstractPlanView<Abstract3DRepresentation, Abstract3DAncillaryObject> {
    protected final Map<String, Abstract3DRepresentation> mRepresentationsById = new HashMap<>();
    protected final Pane mCenterPane;

    protected double mMinNearClip = CoordinateUtils.lengthToCoords(Length.ofCM(50));
    protected Group mLightGroup = null;
    protected AmbientLight mAmbientLight;
    protected PointLight mPointLight;
    protected CombinedTransformGroup mTransformedRoot = null;
    protected Group mRootGroup = null;
    protected PerspectiveCamera mCamera;
    protected SubScene mSubScene;

    protected Abstract3DView(UiController uiController) {
        super(uiController);
        mCenterPane = new Pane();
        setCenter(mCenterPane);
    }

    @Override
    public boolean isAlive() {
        return mTransformedRoot != null;
    }

    public Group getRootGroup() {
        return mRootGroup;
    }

    public CombinedTransformGroup getTransformedRoot() {
        return mTransformedRoot;
    }

    public Map<String, Abstract3DRepresentation> getObjectsById() {
        return mRepresentationsById;
    }

    @Override
    public boolean canClose() {
        return true;
    }

    protected void initializeLight() {
        ObservableList<Node> lights = mLightGroup.getChildren();
        mAmbientLight = new AmbientLight(Color.grayRgb(70));
        lights.add(mAmbientLight);
        mPointLight = new PointLight(Color.WHITE.deriveColor(0, 1, 0.5, 1));
        // TODO: Positioning of point lights -> configuration interface
        lights.add(mPointLight);
    }

    @Override
    protected void initialize() {
        setBehavior(new ThreeDNullBehavior());
        mCamera = new PerspectiveCamera(true);
        mCamera.setNearClip(mMinNearClip);
        mCamera.setFarClip(10000);
        mCamera.setTranslateZ(-1500);
        mCamera.setFieldOfView(60);

        mRootGroup = new Group();
        mLightGroup = new Group();
        mTransformedRoot = new CombinedTransformGroup();
        mRootGroup.getChildren().addAll(mTransformedRoot, mLightGroup);
        mSubScene = new SubScene(mRootGroup, 0, 0, true, SceneAntialiasing.DISABLED);

        mSubScene.setFill(Color.SILVER);
        mSubScene.setCamera(mCamera);

        mSubScene.setManaged(false); // Necessary to prevent the outer panel from taking the scene's size into account for its own size
        mSubScene.heightProperty().bind(heightProperty());
        mSubScene.widthProperty().bind(widthProperty());

        mRepresentationsById.clear();

        super.initialize();

        ObservableList<Node> children = mCenterPane.getChildren();
        children.add(mSubScene);

        initializeLight();
    }

    @Override
    protected void uninitialize() {
        super.uninitialize();
        mRootGroup = null;
        mTransformedRoot = null;
        mLightGroup = null;
        mSubScene = null;
        mCenterPane.getChildren().clear();
    }

    @Override
    protected abstract void initializeFromPlan();

    @Override
    protected Collection<Abstract3DRepresentation> doAddUIRepresentations(Collection<? extends BaseObject> addedObjects) {
        Collection<Abstract3DRepresentation> reprs = new ArrayList<>();
        ObservableList<Node> children = mTransformedRoot.getChildren();
        for (BaseObject modelObject : addedObjects) {
            Class<? extends BaseObject> modelObjectClass = modelObject.getClass();
            AbstractObjectUIRepresentation objRepr = ObjectTypesRegistry.getUIRepresentation(modelObjectClass);
            if (objRepr == null) {
                continue;
            }
            Abstract3DRepresentation repr = objRepr.create3DRepresentation(modelObject, this);
            if (repr != null) {
                reprs.add(repr);
            }
        }
        children.addAll(reprs);
        return reprs;
    }

    @Override
    protected Collection<Abstract3DRepresentation> doRemoveUIRepresentations(Collection<? extends BaseObject> removedObjects) {
        Collection<Abstract3DRepresentation> result = new ArrayList<>(removedObjects.size());
        ObservableList<Node> children = mTransformedRoot.getChildren();
        for (BaseObject object : removedObjects) {
            Abstract3DRepresentation objRepr = getRepresentationByModelId(object.getId());
            if (objRepr == null) {
                continue;
            }
            children.remove(objRepr);
            objRepr.dispose();
            result.add(objRepr);
        }
        return result;
    }

    @Override
    public void addAncillaryObject(Abstract3DAncillaryObject aao) {
        mAncillaryObjectsById.put(aao.getAncillaryObjectId(), aao);
        ObservableList<Node> children = mTransformedRoot.getChildren();
        children.add(aao);
    }

    @Override
    public void removeAncillaryObject(String id) {
        Node aao = mAncillaryObjectsById.remove(id);
        if (aao == null) {
            return;
        }
        ObservableList<Node> children = mTransformedRoot.getChildren();
        children.remove(aao);
    }

    /**
     * Only to be called from {@link Abstract3DRepresentation}.
     */
    @Override
    public void registerRepresentation(String modelId, Abstract3DRepresentation uiRepr) {
        mRepresentationsById.put(modelId, uiRepr);
    }

    /**
     * Only to be called from {@link Abstract3DRepresentation}.
     */
    @Override
    public void unregisterRepresentation(String modelId) {
        mRepresentationsById.remove(modelId);
    }

    @Override
    public Map<String, Abstract3DRepresentation> getRepresentationsById() {
        return mRepresentationsById;
    }

    @Override
    public Abstract3DRepresentation getRepresentationByModelId(String id) {
        return mRepresentationsById.get(id);
    }

    public Abstract3DRepresentation getRepresentation(Node shape) {
        return (Abstract3DRepresentation) shape.getUserData();
    }

    @Override
    public Collection<Abstract3DRepresentation> getAllRepresentations() {
        return mRepresentationsById.values();
    }

    @Override
    public Collection<Abstract3DRepresentation> getRepresentationsByIds(Collection<String> objIds) {
        Collection<Abstract3DRepresentation> result = new ArrayList<>(objIds.size());
        for (String id : objIds) {
            Abstract3DRepresentation repr = mRepresentationsById.get(id);
            if (repr != null) {
                result.add(repr);
            }
        }
        return result;
    }

    public void zoom(double value) {
        mCamera.setTranslateZ(mCamera.getTranslateZ() - value);
    }

    public void moveNearClip(double value) {
        mCamera.setNearClip(Math.max(mCamera.getNearClip() - value, mMinNearClip));
    }
}
