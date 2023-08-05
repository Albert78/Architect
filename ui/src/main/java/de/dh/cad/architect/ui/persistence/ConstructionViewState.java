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
package de.dh.cad.architect.ui.persistence;

import javax.xml.bind.annotation.XmlElement;

public class ConstructionViewState extends ViewState {
    protected double mScale = 0;
    protected double mTranslateX = 0;
    protected double mTranslateY = 0;

    public ConstructionViewState() {
        // For JAXB
    }

    @XmlElement(name = "Scale")
    public double getScale() {
        return mScale;
    }

    public void setScale(double value) {
        mScale = value;
    }

    @XmlElement(name = "TranslateX")
    public double getTranslateX() {
        return mTranslateX;
    }

    public void setTranslateX(double value) {
        mTranslateX = value;
    }

    @XmlElement(name = "TranslateY")
    public double getTranslateY() {
        return mTranslateY;
    }

    public void setTranslateY(double value) {
        mTranslateY = value;
    }
}
