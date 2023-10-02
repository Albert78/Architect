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
package de.dh.cad.architect.ui.view.construction;

import java.util.ArrayList;
import java.util.Collection;

import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.model.objects.BaseObject;
import de.dh.cad.architect.ui.controller.UiController;
import de.dh.cad.architect.ui.objects.Abstract2DAncillaryObject;
import de.dh.cad.architect.ui.objects.Abstract2DRepresentation;
import de.dh.cad.architect.ui.objects.AbstractObjectUIRepresentation;
import de.dh.cad.architect.ui.objects.ObjectTypesRegistry;
import de.dh.cad.architect.ui.utils.CoordinateUtils;
import de.dh.cad.architect.ui.view.AbstractPlanView;
import de.dh.cad.architect.ui.view.construction.behaviors.ConstructionNullBehavior;
import de.dh.utils.fx.ClippingStackPane;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

public abstract class Abstract2DView extends AbstractPlanView<Abstract2DRepresentation, Abstract2DAncillaryObject> {
    protected final StackPane mCenterPane;

    protected Pane mTransformedRoot = null;
    protected Pane mTopLayer = null;

    protected Abstract2DView(UiController uiController) {
        super(uiController);
        mCenterPane = new StackPane();
        setCenter(mCenterPane);
    }

    @Override
    public boolean isAlive() {
        return mTransformedRoot != null;
    }

    public Pane getTransformedRoot() {
        return mTransformedRoot;
    }

    public Pane getTopLayer() {
        return mTopLayer;
    }

    public Pane getCenterPane() {
        return mCenterPane;
    }

    @Override
    public boolean canClose() {
        return true;
    }

    @Override
    protected void initialize() {
        setBehavior(new ConstructionNullBehavior());
        mTransformedRoot = new Pane();
        mTopLayer = new Pane();

        super.initialize();

        ObservableList<Node> children = mCenterPane.getChildren();
        children.addAll(new ClippingStackPane(mTransformedRoot), mTopLayer);

        mTopLayer.setMouseTransparent(true);
    }

    @Override
    protected void uninitialize() {
        super.uninitialize();
        mTransformedRoot = null;
        mTopLayer = null;
        mCenterPane.getChildren().clear();
    }

    public Point2D getPointOnPlanFromScene(double sceneX, double sceneY) {
        return mTransformedRoot.sceneToLocal(sceneX, sceneY);
    }

    public UiPlanPosition getPlanPositionFromScene(double sceneX, double sceneY) {
        Point2D ptInScene = new Point2D(sceneX, sceneY);
        return new UiPlanPosition(ptInScene) {
            @Override
            public Position2D getModelPosition() {
                return CoordinateUtils.point2DToPosition2D(getPointOnPlan());
            }

            @Override
            public Point2D getPointOnPlan() {
                return getPointOnPlanFromScene(ptInScene.getX(), ptInScene.getY());
            }
        };
    }

    // To be overridden
    @Override
    protected Collection<Abstract2DRepresentation> doAddUIRepresentations(Collection<? extends BaseObject> addedObjects) {
        Collection<Abstract2DRepresentation> result = new ArrayList<>();
        ObservableList<Node> children = mTransformedRoot.getChildren();
        for (BaseObject modelObject : addedObjects) {
            Class<? extends BaseObject> modelObjectClass = modelObject.getClass();
            AbstractObjectUIRepresentation objRepr = ObjectTypesRegistry.getUIRepresentation(modelObjectClass);
            if (objRepr == null) {
                continue;
            }
            Abstract2DRepresentation repr = objRepr.create2DRepresentation(modelObject, this);
            if (repr != null) {
                registerRepresentation(modelObject.getId(), repr);
                result.add(repr);
                repr.updateToModel();
            }
        }
        children.addAll(result);
        return result;
    }

    // To be overridden
    @Override
    protected Collection<Abstract2DRepresentation> doRemoveUIRepresentations(Collection<? extends BaseObject> removedObjects) {
        Collection<Abstract2DRepresentation> result = new ArrayList<>(removedObjects.size());
        ObservableList<Node> children = mTransformedRoot.getChildren();
        for (BaseObject object : removedObjects) {
            Abstract2DRepresentation objRepr = getRepresentationByModelId(object.getId());
            if (objRepr == null) {
                continue;
            }
            children.remove(objRepr);
            objRepr.dispose();
            result.add(objRepr);
            unregisterRepresentation(objRepr.getModelId());
        }
        return result;
    }

    @Override
    public void addAncillaryObject(Abstract2DAncillaryObject aao) {
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
}
