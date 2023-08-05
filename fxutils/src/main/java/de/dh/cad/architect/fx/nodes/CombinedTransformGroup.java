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
package de.dh.cad.architect.fx.nodes;

import javafx.scene.Group;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

public class CombinedTransformGroup extends Group {
    protected final Rotate mXRotate;
    protected final Rotate mZRotate;
    protected final Translate mTranslate;

    public CombinedTransformGroup() {
        mXRotate = new Rotate(0, Rotate.X_AXIS);
        mZRotate = new Rotate(0, Rotate.Z_AXIS);
        mTranslate = new Translate(0, 0, 0);
        getTransforms().addAll(mXRotate, mZRotate, mTranslate);
    }

    public Rotate getXRotate() {
        return mXRotate;
    }

    public Rotate getZRotate() {
        return mZRotate;
    }

    public Translate getTranslate() {
        return mTranslate;
    }
}
