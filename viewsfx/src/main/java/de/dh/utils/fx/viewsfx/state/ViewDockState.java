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
package de.dh.utils.fx.viewsfx.state;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

public class ViewDockState {
    protected String mViewId = null;
    protected boolean mSelected = false;
    protected Integer mLastFloatingWidth = null;
    protected Integer mLastFloatingHeight = null;

    @XmlAttribute(name = "viewId")
    public String getViewId() {
        return mViewId;
    }

    public void setViewId(String value) {
        mViewId = value;
    }

    @XmlTransient
    public boolean isSelected() {
        return mSelected;
    }

    public void setSelected(boolean value) {
        mSelected = value;
    }

    @XmlAttribute(name = "selected")
    public Boolean isSelected_JAXB() {
        return mSelected ? Boolean.TRUE : null;
    }

    public void setSelected_JAXB(Boolean value) {
        mSelected = value != null && value;
    }

    @XmlTransient
    public Double getLastFloatingWidth() {
        return mLastFloatingWidth == null ? null : mLastFloatingWidth.doubleValue();
    }

    public void setLastFloatingWidth(Double value) {
        mLastFloatingWidth = value == null ? null : value.intValue();
    }

    @XmlTransient
    public Double getLastFloatingHeight() {
        return mLastFloatingHeight == null ? null : mLastFloatingHeight.doubleValue();
    }

    public void setLastFloatingHeight(Double value) {
        mLastFloatingHeight = value == null ? null : value.intValue();
    }

    @XmlElement(name = "LastFloatingWidth")
    public Integer getLastFloatingWidth_JAXB() {
        return mLastFloatingWidth;
    }

    public void setLastFloatingWidth_JAXB(Integer value) {
        mLastFloatingWidth = value;
    }

    @XmlElement(name = "LastFloatingHeight")
    public Integer getLastFloatingHeight_JAXB() {
        return mLastFloatingHeight;
    }

    public void setLastFloatingHeight_JAXB(Integer value) {
        mLastFloatingHeight = value;
    }
}
