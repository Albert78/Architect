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
package de.dh.cad.architect.fx.nodes.objviewer;

import de.dh.cad.architect.fx.nodes.objviewer.CoordinateSystemConfiguration.Axis;
import de.dh.cad.architect.fx.nodes.objviewer.CoordinateSystemConfiguration.AxisConfiguration;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

public class CoordinateAxisNode extends Group {
    protected final AxisConfiguration mAxisConfiguration;
    protected final Cylinder mLine;
    protected final Text mAxisLabelP;
    protected final Text mAxisLabelN;

    protected final DoubleProperty mLabelDistHighProperty = new SimpleDoubleProperty(200);
    protected final DoubleProperty mLabelDistLowProperty = new SimpleDoubleProperty(10);
    protected final DoubleProperty mAxisLengthProperty = new SimpleDoubleProperty(500);
    protected final DoubleProperty mAxisRadiusProperty = new SimpleDoubleProperty(0.5);

    public CoordinateAxisNode(AxisConfiguration axisConfiguration) {
        mAxisConfiguration = axisConfiguration;

        mLine = new Cylinder();
        mLine.radiusProperty().bind(mAxisRadiusProperty);
        mLine.heightProperty().bind(mAxisLengthProperty.multiply(2));

        Axis axis = axisConfiguration.getAxis();
        switch (axis) {
        case X: {
            mLine.setRotationAxis(new Point3D(0, 0, 1));
            mLine.setRotate(90);

            mAxisLabelP = new Text(axisConfiguration.getMaxLabel());
            double labelPHeight = mAxisLabelP.getLayoutBounds().getHeight();
            mAxisLabelP.translateXProperty().bind(mLabelDistHighProperty);
            mAxisLabelP.translateYProperty().bind(mLabelDistLowProperty.add(labelPHeight));

            mAxisLabelN = new Text(axisConfiguration.getMinLabel());
            double labelNWidth = mAxisLabelN.getLayoutBounds().getWidth();
            double labelNHeight = mAxisLabelN.getLayoutBounds().getHeight();
            mAxisLabelN.translateXProperty().bind(mLabelDistHighProperty.negate().add(-labelNWidth));
            mAxisLabelN.translateYProperty().bind(mLabelDistLowProperty.add(labelNHeight));
            break;
        }
        case Y: {
            mAxisLabelP = new Text(axisConfiguration.getMaxLabel());
            double labelPWidth = mAxisLabelP.getLayoutBounds().getWidth();
            double labelPHeight = mAxisLabelP.getLayoutBounds().getHeight();
            mAxisLabelP.translateXProperty().bind(mLabelDistLowProperty.negate().add(-labelPWidth - 10));
            mAxisLabelP.translateYProperty().bind(mLabelDistHighProperty.add(-labelPHeight));
            mAxisLabelN = new Text(axisConfiguration.getMinLabel());
            double labelNWidth = mAxisLabelN.getLayoutBounds().getWidth();
            mAxisLabelN.translateXProperty().bind(mLabelDistLowProperty.negate().add(-labelNWidth - 10));
            mAxisLabelN.translateYProperty().bind(mLabelDistHighProperty.negate());
            break;
        }
        case Z: {
            mLine.setRotationAxis(new Point3D(0, 0, 1));
            mLine.setRotate(90);

            mAxisLabelP = new Text(axisConfiguration.getMaxLabel());
            double labelPHeight = mAxisLabelP.getLayoutBounds().getHeight();
            mAxisLabelP.translateXProperty().bind(mLabelDistHighProperty);
            mAxisLabelP.translateYProperty().bind(mLabelDistLowProperty.add(labelPHeight));

            mAxisLabelN = new Text(axisConfiguration.getMinLabel());
            double labelNWidth = mAxisLabelN.getLayoutBounds().getWidth();
            double labelNHeight = mAxisLabelN.getLayoutBounds().getHeight();
            mAxisLabelN.translateXProperty().bind(mLabelDistHighProperty.negate().add(-labelNWidth));
            mAxisLabelN.translateYProperty().bind(mLabelDistLowProperty.add(labelNHeight));

            setRotationAxis(new Point3D(0, -1, 0));
            setRotate(90);
            break;
        }
        default:
            throw new IllegalArgumentException("Unexpected value for axis: " + axis);
        }

        mLine.setMaterial(new PhongMaterial(Color.BLACK));
        mAxisLabelP.setFont(Font.font(20));
        mAxisLabelN.setFont(Font.font(20));
        ObservableList<Node> children = getChildren();
        children.add(mLine);
        children.add(mAxisLabelP);
        children.add(mAxisLabelN);
    }
}
