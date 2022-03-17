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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import de.dh.cad.architect.model.ChangeSet;
import de.dh.cad.architect.model.coords.MathUtils;
import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.model.objects.Anchor;
import de.dh.cad.architect.model.objects.Floor;
import de.dh.cad.architect.ui.Constants;
import de.dh.cad.architect.ui.objects.IntermediatePoint.IntermediatePointCallback;
import de.dh.cad.architect.ui.utils.CoordinateUtils;
import de.dh.cad.architect.ui.view.construction.Abstract2DView;
import de.dh.cad.architect.ui.view.construction.ConstructionView;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.VPos;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeType;
import javafx.scene.text.Text;
import javafx.scene.transform.Scale;

public class FloorConstructionRepresentation extends AbstractAnchoredObjectConstructionRepresentation implements IModificationFeatureProvider {
    protected final Polygon mShape;
    protected final Text mAreaText;
    protected final Scale mAreaTextScaleCorrection;
    protected List<IntermediatePoint> mIntermediatePoints = null;
    protected boolean mModificationFeaturesEnabled = false;

    public FloorConstructionRepresentation(Floor floor, Abstract2DView parentView) {
        super(floor, parentView);
        mShape = new Polygon();
        mShape.setStrokeType(StrokeType.INSIDE);
        setViewOrder(Constants.VIEW_ORDER_WALL);
        addScaled(mShape);

        mAreaText = new Text("-");
        mAreaTextScaleCorrection = addUnscaled(mAreaText);

        ChangeListener<Boolean> propertiesUpdaterListener = new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                updateProperties();
            }
        };
        selectedProperty().addListener(propertiesUpdaterListener);
        objectSpottedProperty().addListener(propertiesUpdaterListener);
        objectFocusedProperty().addListener(propertiesUpdaterListener);
        objectEmphasizedProperty().addListener(propertiesUpdaterListener);
    }

    public Floor getFloor() {
        return (Floor) mModelObject;
    }

    @Override
    public ConstructionView getParentView() {
        return (ConstructionView) mParentView;
    }

    protected void updateProperties() {
        mShape.setFill(Color.LIGHTGRAY.deriveColor(1, 1, 1.1, 1));
        if (isSelected()) {
            mShape.setStroke(SELECTED_OBJECTS_COLOR);
        } else {
            mShape.setStroke(Color.BLACK);
        }
        if (isObjectFocused()) {
            setViewOrder(Constants.VIEW_ORDER_FLOOR + Constants.VIEW_ORDER_OFFSET_FOCUSED);
        } else {
            setViewOrder(Constants.VIEW_ORDER_FLOOR + Constants.VIEW_ORDER_OFFSET_NORMAL);
        }
        configureMainBorderDefault(mShape);
    }

    protected void updatePoints() {
        List<Double> points = new ArrayList<>();
        Floor floor = getFloor();
        for (Position2D position : floor.getEdgePositions()) {
            double x = CoordinateUtils.lengthToCoords(position.getX());
            double y = CoordinateUtils.lengthToCoords(position.getY());
            points.add(x);
            points.add(y);
        }
        mShape.getPoints().setAll(points);

        mAreaText.setTextOrigin(VPos.CENTER);
        mAreaText.setText(floor.getAreaString());
        double textWidth2 = mAreaText.getLayoutBounds().getWidth() / 2;
        Position2D middlePoint = MathUtils.calculateCentroid(floor.getEdgePositions());
        double middleX = CoordinateUtils.lengthToCoords(middlePoint.getX());
        mAreaText.setX(middleX - textWidth2);
        double middleY = CoordinateUtils.lengthToCoords(middlePoint.getY());
        mAreaText.setY(middleY);
        mAreaTextScaleCorrection.setPivotX(middleX);
        mAreaTextScaleCorrection.setPivotY(middleY);
    }

    protected IntermediatePoint createIntermediatePoint() {
        IntermediatePoint result = new IntermediatePoint(getParentView(), new IntermediatePointCallback() {
            @Override
            protected void detachIntermediatePoint(IntermediatePoint source) {
                mIntermediatePoints.remove(source);
            }

            @Override
            protected Anchor createHandleAnchor(IntermediatePoint source, Anchor anchorBefore, Anchor anchorAfter, Position2D bendPosition) {
                Floor floor = getFloor();
                ChangeSet changeSet = new ChangeSet();
                Anchor anchor = floor.createEdgeAnchor(anchorAfter, bendPosition, changeSet).getEdgeHandleAnchor();
                mParentView.getUiController().notifyChanges(changeSet);
                return anchor;
            }
        });
        result.updateScale(getParentView().getScaleCompensation());
        return result;
    }

    protected void doUpdateIntermediatePoints() {
        Floor floor = getFloor();
        List<Anchor> anchors = floor.getEdgeHandleAnchors();

        // Update amount of visible intermediate points
        while (anchors.size() < mIntermediatePoints.size()) {
            int lastIndex = mIntermediatePoints.size() - 1;
            IntermediatePoint lastIP = mIntermediatePoints.get(lastIndex);
            lastIP.dispose();
            mIntermediatePoints.remove(lastIndex);
        }
        while (anchors.size() > mIntermediatePoints.size()) {
            IntermediatePoint ip = createIntermediatePoint();
            mIntermediatePoints.add(ip);
        }
        Iterator<Anchor> ia = anchors.iterator();
        if (!ia.hasNext()) {
            return;
        }
        Anchor firstAnchor = ia.next();
        Anchor lastAnchor = firstAnchor;
        int ipIndex = 0;
        while (ia.hasNext() || firstAnchor != null) {
            Anchor currentDock;
            if (ia.hasNext()) {
                currentDock = ia.next();
            } else {
                // Position last intermediate point between last anchor and first anchor
                currentDock = firstAnchor;
                firstAnchor = null;
            }
            IntermediatePoint ip = mIntermediatePoints.get(ipIndex);
            ip.update(lastAnchor, currentDock);
            lastAnchor = currentDock;
            ipIndex++;
        }
        boolean visible = !floor.isHidden();
        for (IntermediatePoint ip : mIntermediatePoints) {
            ip.setVisible(visible);
        }
    }

    protected void addIntermediatePoints() {
        if (mIntermediatePoints != null) {
            // Intermediate points already added
            return;
        }
        mIntermediatePoints = new ArrayList<>();
        doUpdateIntermediatePoints();
    }

    protected void updateIntermediatePoints() {
        if (mIntermediatePoints == null) {
            // No intermediate points visible ATM
            return;
        }
        doUpdateIntermediatePoints();
    }

    protected void removeIntermediatePoints() {
        if (mIntermediatePoints == null) {
            // No intermediate points visible ATM
            return;
        }
        for (IntermediatePoint ip : mIntermediatePoints) {
            ip.dispose();
        }
        mIntermediatePoints = null;
    }

    @Override
    public void enableModificationFeatures() {
        mModificationFeaturesEnabled = true;
        addIntermediatePoints();
    }

    @Override
    public void disableModificationFeatures() {
        mModificationFeaturesEnabled = false;
        removeIntermediatePoints();
    }

    @Override
    public boolean isAnchorDragSupported(Anchor anchor) {
        return mModificationFeaturesEnabled && Floor.isEdgeHandleAnchor(anchor);
    }

    @Override
    public void updateScale(double scaleCompensation) {
        super.updateScale(scaleCompensation);
        if (mIntermediatePoints == null) {
            // No intermediate points visible ATM
            return;
        }
        for (IntermediatePoint ip : mIntermediatePoints) {
            ip.updateScale(scaleCompensation);
        }
    }

    @Override
    public void updateToModel() {
        super.updateToModel();
        updatePoints();
        updateIntermediatePoints();
        updateProperties();
    }

    @Override
    protected Optional<Shape> getShapeForIntersectionCheck() {
        return Optional.of(mShape);
    }
}
