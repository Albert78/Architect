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

import java.util.Objects;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Represents the XML file at the root of an architect asset library.
 */
@XmlRootElement(name = "AssetLibrary")
public class AssetLibrary {
    protected String mId;
    protected String mName;
    protected String mDescription;

    public AssetLibrary() {
        // For JAXB
    }

    public AssetLibrary(String id) {
        mId = id;
    }

    @XmlAttribute(name = "id")
    public String getId() {
        return mId;
    }

    public void setId(String value) {
        mId = value;
    }

    @XmlElement(name = "Name")
    public String getName() {
        return mName;
    }

    public void setName(String value) {
        mName = value;
    }

    @XmlElement(name = "Description")
    public String getDescription() {
        return mDescription;
    }

    public void setDescription(String value) {
        mDescription = value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AssetLibrary other = (AssetLibrary) obj;
        return Objects.equals(mId, other.mId);
    }

    @Override
    public String toString() {
        return mName + " (" + mId + ")";
    }
}
