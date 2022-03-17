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
package de.dh.cad.architect.ui.persistence;

import javax.xml.bind.annotation.XmlElement;

public class CameraPosition {
    protected double mAngleX = 0;
    protected double mAngleZ = 0;
    protected double mX = 0;
    protected double mY = 0;
    protected double mZ = 0;
    protected double mCameraTranslateZ = 0;
    protected double mCameraNearClip = 0;
    protected double mFieldOfView = 0;

    public CameraPosition() {
        // For JAXB
    }

    /**
     * Copy constructor.
     */
    public CameraPosition(CameraPosition position) {
        mAngleX = position.getAngleX();
        mAngleZ = position.getAngleZ();
        mX = position.getX();
        mY = position.getY();
        mZ = position.getZ();
        mCameraTranslateZ = position.getCameraTranslateZ();
        mCameraNearClip = position.getCameraNearClip();
        mFieldOfView = position.getFieldOfView();
    }

    @XmlElement(name = "AngleX")
    public double getAngleX() {
        return mAngleX;
    }

    public void setAngleX(double value) {
        mAngleX = value;
    }

    @XmlElement(name = "AngleZ")
    public double getAngleZ() {
        return mAngleZ;
    }

    public void setAngleZ(double value) {
        mAngleZ = value;
    }

    @XmlElement(name = "X")
    public double getX() {
        return mX;
    }

    public void setX(double value) {
        mX = value;
    }

    @XmlElement(name = "Y")
    public double getY() {
        return mY;
    }

    public void setY(double value) {
        mY = value;
    }

    @XmlElement(name = "Z")
    public double getZ() {
        return mZ;
    }

    public void setZ(double value) {
        mZ = value;
    }

    @XmlElement(name = "CameraTranslateZ")
    public double getCameraTranslateZ() {
        return mCameraTranslateZ;
    }

    public void setCameraTranslateZ(double value) {
        mCameraTranslateZ = value;
    }

    @XmlElement(name = "CameraNearClip")
    public double getCameraNearClip() {
        return mCameraNearClip;
    }

    public void setCameraNearClip(double value) {
        mCameraNearClip = value;
    }

    @XmlElement(name = "FieldOfView")
    public double getFieldOfView() {
        return mFieldOfView;
    }

    public void setFieldOfView(double value) {
        mFieldOfView = value;
    }

    public void cleanup() {
        mAngleX = mAngleX % 360;
        mAngleZ = mAngleZ % 360;
    }
}
