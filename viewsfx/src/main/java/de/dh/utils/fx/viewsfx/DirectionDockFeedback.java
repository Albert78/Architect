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
package de.dh.utils.fx.viewsfx;

import javafx.geometry.Bounds;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;

public class DirectionDockFeedback implements IDockFeedback {
    protected final IDockZone mParent;
    protected final Bounds mBounds;

    protected Rectangle mFeedbackShape = null;

    public DirectionDockFeedback(IDockZone parent, Bounds bounds) {
        mParent = parent;
        mBounds = bounds;
    }

    @Override
    public void install() {
        if (mFeedbackShape != null) {
            throw new IllegalStateException("Feedback is already installed");
        }
        mFeedbackShape = new Rectangle(mBounds.getMinX(), mBounds.getMinY(), mBounds.getWidth(), mBounds.getHeight());
        mFeedbackShape.setFill(null);
        mFeedbackShape.setStroke(Color.BLUE);
        mFeedbackShape.setStrokeWidth(2);
        mFeedbackShape.setStrokeType(StrokeType.INSIDE);
        mParent.getFeedbackPane().getChildren().add(mFeedbackShape);
    }

    @Override
    public void uninstall() {
        if (mFeedbackShape == null) {
            throw new IllegalStateException("Feedback is not installed");
        }
        mParent.getFeedbackPane().getChildren().remove(mFeedbackShape);
        mFeedbackShape = null;
    }
}
