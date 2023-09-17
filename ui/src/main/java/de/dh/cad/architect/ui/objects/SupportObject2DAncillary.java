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

import de.dh.cad.architect.model.assets.SupportObjectDescriptor;
import de.dh.cad.architect.model.coords.Dimensions2D;
import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.ui.assets.AssetLoader;
import de.dh.cad.architect.ui.utils.CoordinateUtils;
import de.dh.cad.architect.ui.view.construction.Abstract2DView;
import javafx.geometry.Point2D;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class SupportObject2DAncillary extends Abstract2DAncillaryObject {
    protected final ImageView mImageView = new ImageView();

    protected SupportObjectDescriptor mSupportObject = null;

    public SupportObject2DAncillary(Abstract2DView parentView) {
        super(parentView);

        DropShadow ds = new DropShadow(BlurType.ONE_PASS_BOX, ANCILLARY_OBJECTS_COLOR, 20, 1.0, 0, 0);
        mImageView.setEffect(ds);
        addScaled(mImageView);
        setMouseTransparent(true);
    }

    public void updateSupportObject(SupportObjectDescriptor descriptor, Dimensions2D dimensions, AssetLoader assetLoader) {
        mSupportObject = descriptor;
        Image image = assetLoader.loadSupportObjectPlanViewImage(descriptor, true);
        double cWidth = CoordinateUtils.lengthToCoords(dimensions.getX(), null);
        double cDepth = CoordinateUtils.lengthToCoords(dimensions.getY(), null);
        mImageView.setImage(image);
        mImageView.setFitWidth(cWidth);
        mImageView.setFitHeight(cDepth);
    }

    public void updatePosition(Position2D position, float rotationDeg) {
        Point2D center = CoordinateUtils.positionToPoint2D(position);
        double width = mImageView.getFitWidth();
        double depth = mImageView.getFitHeight();
        mImageView.setX(center.getX() - width / 2);
        mImageView.setY(center.getY() - depth / 2);
        mImageView.setRotate(rotationDeg);
    }
}
