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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.apache.commons.lang3.StringUtils;

import de.dh.cad.architect.utils.jaxb.LocalDateTimeJavaTypeAdapter;

@XmlSeeAlso({
    MaterialSetDescriptor.class,
    SupportObjectDescriptor.class,

    ObjModelResource.class,
    MtlModelResource.class,
})
public class AbstractAssetDescriptor {
    protected String mId;
    protected AssetRefPath mSelfRef;
    protected String mName;
    protected String mCategory;
    protected String mType;
    protected AbstractModelResource mModel;
    protected String mIconImageResourceName;
    protected List<String> mTags = new ArrayList<>();
    protected String mDescription;
    protected String mAuthor;
    protected String mOrigin;
    protected LocalDateTime mLastModified;

    public AbstractAssetDescriptor() {
        // For JAXB
    }

    public AbstractAssetDescriptor(String id, AssetRefPath selfRef) {
        mId = id;
        mSelfRef = selfRef;
    }

    @XmlAttribute(name = "id")
    public String getId() {
        return mId;
    }

    public void setId(String value) {
        mId = value;
    }

    @XmlTransient
    public AssetRefPath getSelfRef() {
        return mSelfRef;
    }

    // Called by the deserialization algorithm
    public void setSelfRef(AssetRefPath value) {
        mSelfRef = value;
    }

    @XmlElement(name = "Name")
    public String getName() {
        return mName;
    }

    public void setName(String value) {
        mName = value;
    }

    /**
     * Category or room where this asset is typically used, for example "Living room" or "Other".
     */
    @XmlElement(name = "Category")
    public String getCategory() {
        return mCategory;
    }

    public void setCategory(String value) {
        mCategory = value;
    }

    /**
     * Gets the resource which contains the model of this asset, i.e. the {@code .obj} or {@code .mtl} file.
     */
    @XmlElement(name = "Model")
    public AbstractModelResource getModel() {
        return mModel;
    }

    public void setModel(AbstractModelResource value) {
        mModel = value;
    }

    /**
     * Gets the name of the icon image for overview listings.
     * The resource is located in the same directory as this descriptor.
     */
    @XmlElement(name = "IconImageResource")
    public String getIconImageResourceName() {
        return mIconImageResourceName;
    }

    public void setIconImageResourceName(String value) {
        mIconImageResourceName = value;
    }

    /**
     * Type of the asset, for example "Rack".
     */
    @XmlElement(name = "Type")
    public String getType() {
        return mType;
    }

    public void setType(String value) {
        mType = value;
    }

    @XmlElementWrapper(name = "Tags")
    @XmlElement(name = "Tag")
    public List<String> getTags() {
        return mTags;
    }

    @XmlElement(name = "Description")
    public String getDescription() {
        return mDescription;
    }

    public void setDescription(String value) {
        mDescription = value;
    }

    /**
     * Contains an origin information, for example an internet resource or the name of
     * a library this resource was imported from.
     */
    @XmlElement(name = "Origin")
    public String getOrigin() {
        return mOrigin;
    }

    public void setOrigin(String value) {
        mOrigin = value;
    }

    @XmlElement(name = "Author")
    public String getAuthor() {
        return mAuthor;
    }

    public void setAuthor(String value) {
        mAuthor = value;
    }

    @XmlJavaTypeAdapter(LocalDateTimeJavaTypeAdapter.class)
    @XmlElement(name = "LastModified")
    public LocalDateTime getLastModified() {
        return mLastModified;
    }

    public void setLastModified(LocalDateTime value) {
        mLastModified = value;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + mId.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AbstractAssetDescriptor other = (AbstractAssetDescriptor) obj;
        if (!mId.equals(other.mId))
            return false;
        return true;
    }

    @Override
    public String toString() {
        String result = mName;
        List<String> strs = new ArrayList<>(2);
        if (!StringUtils.isEmpty(mCategory)) {
            strs.add(mCategory);
        }
        if (!StringUtils.isEmpty(mType)) {
            strs.add(mType);
        }
        if (!strs.isEmpty()) {
            result = result + " (" + StringUtils.join(strs, " / ") + ")";
        }
        return result;
    }
}
