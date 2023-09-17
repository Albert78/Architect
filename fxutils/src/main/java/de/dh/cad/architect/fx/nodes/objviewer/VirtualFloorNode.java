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

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.shape.Box;

public class VirtualFloorNode extends Box {
    public static final double DEFAULT_FLOOR_HEIGHT = 5;

    protected DoubleProperty mSizeProperty = new SimpleDoubleProperty(100);

    public VirtualFloorNode() {
        this(100);
    }

    public VirtualFloorNode(int size) {
        setDepth(DEFAULT_FLOOR_HEIGHT);
        widthProperty().bind(mSizeProperty);
        heightProperty().bind(mSizeProperty);
        setSize(size);
        setTranslate(0);
    }

    public DoubleProperty getSizeProperty() {
        return mSizeProperty;
    }

    public double getSize() {
        return mSizeProperty.doubleValue();
    }

    public void setSize(double value) {
        mSizeProperty.set(value);
        setDepth(value / 100);
    }

    public void setTranslate(double translateZ) {
        translateZProperty().bind(new SimpleDoubleProperty(translateZ).subtract(depthProperty().divide(2)));
    }
}
