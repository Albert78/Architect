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
package de.dh.cad.architect.model.assets;

import java.util.Map;
import java.util.TreeMap;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.model.jaxb.LengthJavaTypeAdapter;
import de.dh.cad.architect.model.jaxb.MeshNamesToMeshConfigurationsJavaTypeAdapter;

/**
 * Descriptor for a support object in the asset library.
 */
@XmlRootElement(name = "SupportObjectDescriptor")
public class SupportObjectDescriptor extends AbstractAssetDescriptor {
    protected Length mWidth;
    protected Length mDepth;
    protected Length mHeight;
    protected Length mElevation;

    protected String mPlanViewImageResourceName;

    protected Map<String, MeshConfiguration> mMeshNamesToMeshConfigurations = new TreeMap<>();

    public SupportObjectDescriptor() {
        // For JAXB
    }

    public SupportObjectDescriptor(String id, AssetRefPath selfRef) {
        super(id, selfRef);
        mWidth = Length.ZERO;
        mDepth = Length.ZERO;
        mHeight = Length.ZERO;
        mElevation = Length.ZERO;

        mPlanViewImageResourceName = null;
    }

    /**
     * Gets the model size of the rectangular bounding box in X direction.
     */
    @XmlElement(name = "Width")
    @XmlJavaTypeAdapter(LengthJavaTypeAdapter.class)
    public Length getWidth() {
        return mWidth;
    }

    public void setWidth(Length value) {
        mWidth = value;
    }

    /**
     * Gets the model size of the rectangular bounding box in Y direction.
     */
    @XmlElement(name = "Depth")
    @XmlJavaTypeAdapter(LengthJavaTypeAdapter.class)
    public Length getDepth() {
        return mDepth;
    }

    public void setDepth(Length value) {
        mDepth = value;
    }

    /**
     * Gets the model size of this support object in Z direction, not including {@link #getElevation() elevation} over floor.
     */
    @XmlElement(name = "Height")
    @XmlJavaTypeAdapter(LengthJavaTypeAdapter.class)
    public Length getHeight() {
        return mHeight;
    }

    public void setHeight(Length value) {
        mHeight = value;
    }

    /**
     * Gets the default model elevation of this support object over the floor in Z direction, for example for wall paintings or lamps.
     */
    @XmlElement(name = "Elevation")
    @XmlJavaTypeAdapter(LengthJavaTypeAdapter.class)
    public Length getElevation() {
        return mElevation;
    }

    public void setElevation(Length value) {
        mElevation = value;
    }

    /**
     * Gets the name of the image to represent this support object in the ground plan.
     * The resource is located in the same directory as this descriptor.
     */
    @XmlElement(name = "PlanViewImageResource")
    public String getPlanViewImageResourceName() {
        return mPlanViewImageResourceName;
    }

    public void setPlanViewImageResourceName(String value) {
        mPlanViewImageResourceName = value;
    }

    @XmlElement(name = "MeshConfigurations")
    @XmlJavaTypeAdapter(MeshNamesToMeshConfigurationsJavaTypeAdapter.class)
    public Map<String, MeshConfiguration> getMeshNamesToMeshConfigurations() {
        return mMeshNamesToMeshConfigurations;
    }

    public void setMeshNamesToMeshConfigurations(Map<String, MeshConfiguration> value) {
        mMeshNamesToMeshConfigurations = value;
    }

    @Override
    public String toString() {
        return "SupportObjectDescriptor <" + mSelfRef + ">";
    }
}
