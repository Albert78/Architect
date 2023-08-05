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

import de.dh.utils.fx.viewsfx.utils.JavaFXUtils;
import javafx.geometry.Bounds;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

public class TabDockFeedback implements IDockFeedback {
    protected final TabDockHost mParent;
    protected final Bounds mPosition;

    protected Line mFeedbackLine;

    public TabDockFeedback(TabDockHost parent, Bounds position) {
        mParent = parent;
        mPosition = position;
    }

    @Override
    public void install() {
        if (mFeedbackLine != null) {
            throw new IllegalStateException("Feedback is already installed");
        }

        mFeedbackLine = new Line(mPosition.getMinX(), mPosition.getMinY(), mPosition.getMaxX(), mPosition.getMaxY());
        mFeedbackLine.setStroke(Color.BLUE);
        mFeedbackLine.setStrokeWidth(2);
        mParent.getFeedbackPane().getChildren().add(mFeedbackLine);
    }

    @Override
    public void uninstall() {
        if (mFeedbackLine == null) {
            throw new IllegalStateException("Feedback is not installed");
        }
        mParent.getFeedbackPane().getChildren().remove(mFeedbackLine);
        mFeedbackLine = null;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " for bounds " + JavaFXUtils.toString2D(mPosition);
    }
}
