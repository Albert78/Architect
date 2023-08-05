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

import de.dh.cad.architect.model.changes.IModelChange;
import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.model.objects.Dimensioning;
import de.dh.cad.architect.ui.Constants;
import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.view.construction.Abstract2DView;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;

public class DimensioningConstructionRepresentation extends AbstractAnchoredObjectConstructionRepresentation implements IModificationFeatureProvider {
    protected final DimensioningVisual mVisual = new DimensioningVisual(this);

    public DimensioningConstructionRepresentation(Dimensioning dimensioning, Abstract2DView parentView) {
        super(dimensioning, parentView);
        setViewOrder(Constants.VIEW_ORDER_DIMENSIONING);

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

        mVisual.setLabelDistance(dimensioning.getLabelDistance());
        mVisual.labelDistanceProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                if (Double.compare(dimensioning.getLabelDistance(), newValue.doubleValue()) == 0) {
                    // Break loop if DimensioningConstructionRepresentation updated the visual's label distance from model
                    return;
                }
                List<IModelChange> changeTrace = new ArrayList<>();
                dimensioning.setLabelDistance(newValue.doubleValue(), changeTrace);
                mParentView.getUiController().notifyChange(changeTrace, Strings.DIMENSIONING_SET_PROPERTY_CHANGE, true);
            }
        });
    }

    @Override
    public void enableModificationFeatures() {
        mVisual.enableDrag();
    }

    @Override
    public void disableModificationFeatures() {
        mVisual.disableDrag();
    }

    @Override
    protected Optional<Shape> getShapeForIntersectionCheck() {
        return Optional.of(mVisual.getText());
    }

    public Dimensioning getDimensioning() {
        return (Dimensioning) mModelObject;
    }

    protected void updateProperties() {
        mVisual.setProperties(isSelected() ? SELECTED_OBJECTS_COLOR : Color.BLACK, isObjectSpotted(), isObjectEmphasized());
        if (isObjectFocused()) {
            setViewOrder(Constants.VIEW_ORDER_DIMENSIONING + Constants.VIEW_ORDER_OFFSET_FOCUSED);
        } else {
            setViewOrder(Constants.VIEW_ORDER_DIMENSIONING + Constants.VIEW_ORDER_OFFSET_NORMAL);
        }
        updateShape();
    }

    @Override
    public void updateScale(double scaleCompensation) {
        super.updateScale(scaleCompensation);
        updateShape();
    }

    protected void updateShape() {
        Dimensioning dimensioning = getDimensioning();
        Position2D position1 = dimensioning.getAnchor1().getPosition().projectionXY();
        Position2D position2 = dimensioning.getAnchor2().getPosition().projectionXY();

        double labelDistance = dimensioning.getLabelDistance();

        mVisual.setLabelDistance(labelDistance);
        mVisual.updateShape(position1, position2, Optional.ofNullable(dimensioning.getLabel()), getScaleCompensation());
    }

    @Override
    public void updateToModel() {
        super.updateToModel();
        updateShape();
        updateProperties();
    }
}
