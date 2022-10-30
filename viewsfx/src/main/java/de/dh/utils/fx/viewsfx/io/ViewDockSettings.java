package de.dh.utils.fx.viewsfx.io;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

public class ViewDockSettings {
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
