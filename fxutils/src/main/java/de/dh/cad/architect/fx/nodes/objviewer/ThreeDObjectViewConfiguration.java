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

public class ThreeDObjectViewConfiguration {
    public static enum CameraType {
        Perspective, Parallel
    }

    protected CameraType mCameraType = CameraType.Perspective;

    protected double mRotationAngleX = 0;
    protected double mRotationAngleY = 0;
    protected double mScaleFactor = 1;

    protected boolean mPointLightOn = true;
    protected double mPointLightIntensity = 0.5;

    protected boolean mAmbientLightOn = true;
    protected double mAmbientLightIntensity = 0.5;

    protected double mLightAngleX = 0;
    protected double mLightAngleZ = 0;

    protected boolean mCoordinateSystemVisible = true;

    public static ThreeDObjectViewConfiguration standardPerspective() {
        return new ThreeDObjectViewConfiguration()
                .setCameraType(CameraType.Perspective)

                .setRotationAngleX(-70)
                .setRotationAngleY(20)

                .setPointLightOn(true)
                .setPointLightIntensity(0.5)
                .setAmbientLightOn(true)
                .setAmbientLightIntensity(0.7)

                .setLightAngleX(0)
                .setLightAngleZ(0)

                .setCoordinateSystemVisible(false);
    }

    public static ThreeDObjectViewConfiguration standardPlanView() {
        return standardPerspective()
                .setCameraType(CameraType.Parallel)

                .setRotationAngleX(0)
                .setRotationAngleY(0)

                .setPointLightIntensity(0.35)
                .setAmbientLightIntensity(0.7)

                .setLightAngleX(90)
                .setLightAngleZ(0);
    }

    public CameraType getCameraType() {
        return mCameraType;
    }

    public ThreeDObjectViewConfiguration setCameraType(CameraType value) {
        mCameraType = value;
        return this;
    }

    public double getRotationAngleX() {
        return mRotationAngleX;
    }

    public ThreeDObjectViewConfiguration setRotationAngleX(double value) {
        mRotationAngleX = value;
        return this;
    }

    public double getRotationAngleY() {
        return mRotationAngleY;
    }

    public ThreeDObjectViewConfiguration setRotationAngleY(double value) {
        mRotationAngleY = value;
        return this;
    }

    public double getScaleFactor() {
        return mScaleFactor;
    }

    public ThreeDObjectViewConfiguration setScaleFactor(double value) {
        mScaleFactor = value;
        return this;
    }

    public boolean isPointLightOn() {
        return mPointLightOn;
    }

    public ThreeDObjectViewConfiguration setPointLightOn(boolean value) {
        mPointLightOn = value;
        return this;
    }

    public boolean isAmbientLightOn() {
        return mAmbientLightOn;
    }

    public ThreeDObjectViewConfiguration setAmbientLightOn(boolean value) {
        mAmbientLightOn = value;
        return this;
    }

    public double getPointLightIntensity() {
        return mPointLightIntensity;
    }

    public ThreeDObjectViewConfiguration setPointLightIntensity(double value) {
        mPointLightIntensity = value;
        return this;
    }

    public double getAmbientLightIntensity() {
        return mAmbientLightIntensity;
    }

    public ThreeDObjectViewConfiguration setAmbientLightIntensity(double value) {
        mAmbientLightIntensity = value;
        return this;
    }

    public double getLightAngleX() {
        return mLightAngleX;
    }

    public ThreeDObjectViewConfiguration setLightAngleX(double value) {
        mLightAngleX = value;
        return this;
    }

    public double getLightAngleZ() {
        return mLightAngleZ;
    }

    public ThreeDObjectViewConfiguration setLightAngleZ(double value) {
        mLightAngleZ = value;
        return this;
    }

    public boolean isCoordinateSystemVisible() {
        return mCoordinateSystemVisible;
    }

    public ThreeDObjectViewConfiguration setCoordinateSystemVisible(boolean value) {
        mCoordinateSystemVisible = value;
        return this;
    }
}
