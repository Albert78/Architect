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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import de.dh.cad.architect.model.coords.Position3D;
import de.dh.cad.architect.model.objects.Anchor;
import de.dh.cad.architect.model.objects.Covering;
import de.dh.cad.architect.ui.Constants;
import de.dh.cad.architect.ui.utils.Axis;
import de.dh.cad.architect.ui.utils.CoordinateUtils;
import de.dh.cad.architect.ui.view.construction.Abstract2DView;
import de.dh.cad.architect.ui.view.construction.ConstructionView;
import javafx.beans.value.ChangeListener;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeType;

public class CoveringConstructionRepresentation extends AbstractAnchoredObjectConstructionRepresentation {
    protected final Polygon mShape;

    public CoveringConstructionRepresentation(Covering covering, Abstract2DView parentView) {
        super(covering, parentView);
        mShape = new Polygon();
        setViewOrder(Constants.VIEW_ORDER_COVERING);
        addScaled(mShape);

        ChangeListener<Boolean> propertiesUpdaterListener = (observable, oldValue, newValue) -> updateProperties();
        selectedProperty().addListener(propertiesUpdaterListener);
        objectSpottedProperty().addListener(propertiesUpdaterListener);
        objectFocusedProperty().addListener(propertiesUpdaterListener);
        objectEmphasizedProperty().addListener(propertiesUpdaterListener);
    }

    public Covering getCovering() {
        return (Covering) mModelObject;
    }

    @Override
    public ConstructionView getParentView() {
        return (ConstructionView) mParentView;
    }

    protected void updateProperties() {
        mShape.setFill(Color.LIGHTGRAY);
        mShape.setStrokeType(StrokeType.CENTERED);
        mShape.getStrokeDashArray().setAll(3d, 10d);
        if (isSelected()) {
            mShape.setStroke(SELECTED_OBJECTS_COLOR);
        } else {
            mShape.setStroke(Color.BLACK);
        }
        if (isObjectFocused()) {
            setViewOrder(Constants.VIEW_ORDER_COVERING + Constants.VIEW_ORDER_OFFSET_FOCUSED);
        } else {
            setViewOrder(Constants.VIEW_ORDER_COVERING + Constants.VIEW_ORDER_OFFSET_NORMAL);
        }
        configureMainBorderDefault(mShape);
    }

    protected void updatePoints() {
        List<Double> points = new ArrayList<>();
        Covering covering = getCovering();
        for (Anchor anchor : covering.getAnchors()) {
            Position3D position = anchor.requirePosition3D();
            points.add(CoordinateUtils.lengthToCoords(position.getX(), Axis.X));
            points.add(CoordinateUtils.lengthToCoords(position.getY(), Axis.Y));
        }
        mShape.getPoints().setAll(points);
    }

    @Override
    public void updateToModel() {
        super.updateToModel();
        updatePoints();
        updateProperties();
    }

    @Override
    protected Optional<Shape> getShapeForIntersectionCheck() {
        return Optional.of(mShape);
    }
}
