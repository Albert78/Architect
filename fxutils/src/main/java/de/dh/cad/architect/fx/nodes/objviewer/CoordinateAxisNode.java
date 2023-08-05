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
    public enum Axis {
        X, Y, Z
    }

    protected static final double AXIS_RADIUS = 0.5;
    protected static final int AXIS_LENGTH = 500;
    protected static final int LABEL_DIST_LOW = 10;
    protected static final int LABEL_DIST_HIGH = 100;

    protected final Axis mAxis;
    protected final Cylinder mLine;
    protected final Text mAxisLabelP;
    protected final Text mAxisLabelN;

    public CoordinateAxisNode(Axis axis) {
        mAxis = axis;

        mLine = new Cylinder(AXIS_RADIUS, AXIS_LENGTH * 2);
        switch (mAxis) {
        case X: {
            mLine.setRotationAxis(new Point3D(0, 0, 1));
            mLine.setRotate(90);

            mAxisLabelP = new Text("+X");
            double labelPHeight = mAxisLabelP.getLayoutBounds().getHeight();
            mAxisLabelP.setTranslateX(LABEL_DIST_HIGH);
            mAxisLabelP.setTranslateY(LABEL_DIST_LOW + labelPHeight);

            mAxisLabelN = new Text("-X");
            double labelNWidth = mAxisLabelN.getLayoutBounds().getWidth();
            double labelNHeight = mAxisLabelN.getLayoutBounds().getHeight();
            mAxisLabelN.setTranslateX(-LABEL_DIST_HIGH - labelNWidth);
            mAxisLabelN.setTranslateY(LABEL_DIST_LOW + labelNHeight);
            break;
        }
        case Y: {
            mAxisLabelP = new Text("+Y");
            double labelPWidth = mAxisLabelP.getLayoutBounds().getWidth();
            double labelPHeight = mAxisLabelP.getLayoutBounds().getHeight();
            mAxisLabelP.setTranslateX(-LABEL_DIST_LOW - labelPWidth - 10);
            mAxisLabelP.setTranslateY(LABEL_DIST_HIGH - labelPHeight);
            mAxisLabelN = new Text("-Y");
            double labelNWidth = mAxisLabelN.getLayoutBounds().getWidth();
            mAxisLabelN.setTranslateX(-LABEL_DIST_LOW - labelNWidth - 10);
            mAxisLabelN.setTranslateY(-LABEL_DIST_HIGH);
            break;
        }
        case Z: {
            mLine.setRotationAxis(new Point3D(0, 0, 1));
            mLine.setRotate(90);

            mAxisLabelP = new Text("+Z");
            double labelPHeight = mAxisLabelP.getLayoutBounds().getHeight();
            mAxisLabelP.setTranslateX(LABEL_DIST_HIGH);
            mAxisLabelP.setTranslateY(LABEL_DIST_LOW + labelPHeight);

            mAxisLabelN = new Text("-Z");
            double labelNWidth = mAxisLabelN.getLayoutBounds().getWidth();
            double labelNHeight = mAxisLabelN.getLayoutBounds().getHeight();
            mAxisLabelN.setTranslateX(-LABEL_DIST_HIGH - labelNWidth);
            mAxisLabelN.setTranslateY(LABEL_DIST_LOW + labelNHeight);

            setRotationAxis(new Point3D(0, -1, 0));
            setRotate(90);
            break;
        }
        default:
            throw new IllegalArgumentException("Unexpected value for axis: " + mAxis);
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
