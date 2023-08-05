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
package de.dh.cad.architect.ui.view.threed;

import de.dh.cad.architect.fx.nodes.CombinedTransformGroup;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.NonInvertibleTransformException;
import javafx.scene.transform.Translate;

public class DragController3D {
    public enum DragMode {
        None,
        Pan,
        Rotate,
    }

    protected Point2D mStartPoint;
    protected double mStartAngleX;
    protected double mStartAngleZ;
    protected Point3D mStartTranslate;

    protected boolean mCreateCenterPoint = true;
    protected Sphere mCenterPoint = null;
    protected Group mCenterPointParent = null;

    protected DragMode mCurrentDragMode = DragMode.None;

    public boolean isCreateCenterPoint() {
        return mCreateCenterPoint;
    }

    public void setCreateCenterPoint(boolean value) {
        mCreateCenterPoint = value;
    }

    public Point2D getStartPoint() {
        return mStartPoint;
    }

    public void setStartPoint(Point2D value) {
        mStartPoint = value;
    }

    public double getStartAngleX() {
        return mStartAngleX;
    }

    public double getStartAngleZ() {
        return mStartAngleZ;
    }

    public void setStartAngleX(double value) {
        mStartAngleX = value;
    }

    public void setStartAngleZ(double value) {
        mStartAngleZ = value;
    }

    public Point3D getStartTranslate() {
        return mStartTranslate;
    }

    public void setStartTranslate(Point3D value) {
        mStartTranslate = value;
    }

    public DragMode getCurrentDragMode() {
        return mCurrentDragMode;
    }

    protected void createCenterPoint(Group rootGroup) {
        if (mCenterPoint == null) {
            mCenterPoint = new Sphere(5);
            mCenterPointParent = rootGroup;
            mCenterPointParent.getChildren().add(mCenterPoint);
            mCenterPoint.setMaterial(new PhongMaterial(Color.YELLOW));
        }
    }

    protected void removeCenterPoint() {
        if (mCenterPoint == null) {
            return;
        }
        mCenterPointParent.getChildren().remove(mCenterPoint);
        mCenterPoint = null;
    }

    public void mousePressed(MouseEvent event, CombinedTransformGroup group) {
        mStartPoint = new Point2D(event.getSceneX(), event.getSceneY());
        mStartAngleX = group.getXRotate().getAngle();
        mStartAngleZ = group.getZRotate().getAngle();
        Translate translate = group.getTranslate();
        mStartTranslate = new Point3D(translate.getX(), translate.getY(), translate.getZ());

        if (mCurrentDragMode != DragMode.None) {
            mCurrentDragMode = DragMode.None;
            removeCenterPoint();
            return;
        }
        if (event.isMiddleButtonDown()) {
            mCurrentDragMode = DragMode.Pan;
        } else if (event.isSecondaryButtonDown()) {
            mCurrentDragMode = DragMode.Rotate;
        } else {
            return;
        }
    }

    public void mouseReleased(MouseEvent event) {
        if (mCurrentDragMode == DragMode.None) {
            return;
        }
        removeCenterPoint();
        mCurrentDragMode = DragMode.None;
    }

    public void mouseDragged(MouseEvent event, Group rootGroup, CombinedTransformGroup group) throws NonInvertibleTransformException {
        if (mCurrentDragMode == DragMode.None) {
            return;
        }

        if (mCreateCenterPoint) {
            createCenterPoint(rootGroup);
        }

        double sceneX = event.getSceneX();
        double sceneY = event.getSceneY();

        double deltaX = sceneX - mStartPoint.getX();
        double deltaY = sceneY - mStartPoint.getY();

        if (mCurrentDragMode == DragMode.Pan) {
            Translate translate = group.getTranslate();
            Point3D inversePoint = group.getLocalToSceneTransform().inverseDeltaTransform(deltaX, deltaY, 0);
            translate.setX(inversePoint.getX() + mStartTranslate.getX());
            translate.setY(inversePoint.getY() + mStartTranslate.getY());
            translate.setZ(inversePoint.getZ() + mStartTranslate.getZ());
        } else if (mCurrentDragMode == DragMode.Rotate) {
            group.getXRotate().setAngle(mStartAngleX + deltaY); // A mouse movement in Y direction will rotate the object around the X axis
            group.getZRotate().setAngle(mStartAngleZ - deltaX); // A mouse movement in X direction will rotate the object around the Z axis
        }
    }
}
