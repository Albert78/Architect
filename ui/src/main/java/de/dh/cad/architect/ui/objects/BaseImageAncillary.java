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

import java.util.function.Consumer;

import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.ui.utils.CoordinateUtils;
import de.dh.cad.architect.ui.view.construction.ConstructionView;
import de.dh.utils.fx.MouseHandlerContext;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.image.ImageView;
import javafx.scene.transform.Scale;

public class BaseImageAncillary extends Abstract2DAncillaryObject {
    protected final ImageView mOverlayImage;
    protected final Scale mOverlayImageScaleCorrection;

    public BaseImageAncillary(ImageView image, ConstructionView parentView) {
        super(parentView);

        mOverlayImage = image;
        mOverlayImageScaleCorrection = addUnscaled(mOverlayImage);
        mOverlayImage.setPickOnBounds(true);
    }

    public MouseHandlerContext installDragHandler(Consumer<Point2D> onStartDragHandler, IDragHandler onDragHandler, IDragHandler onEndDragHandler,
        Cursor mouseOverCursor, Cursor dragCursor) {
        MouseHandlerContext dragHandler = createDragHandler(onStartDragHandler, onDragHandler, onEndDragHandler, mouseOverCursor, dragCursor);
        dragHandler.install(this);
        return dragHandler;
    }

    /**
     * Sets the center position of the image.
     */
    public void setPosition(Position2D pos) {
        setPosition(new Point2D(CoordinateUtils.lengthToCoords(pos.getX()), CoordinateUtils.lengthToCoords(pos.getY())));
    }

    /**
     * Sets the center position of the image.
     */
    public void setPosition(Point2D pos) {
        mOverlayImage.setX(pos.getX() - mOverlayImage.getFitWidth() / 2);
        mOverlayImage.setY(pos.getY() - mOverlayImage.getFitHeight() / 2);
        mOverlayImageScaleCorrection.setPivotX(pos.getX());
        mOverlayImageScaleCorrection.setPivotY(pos.getY());
    }
}
