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
package de.dh.cad.architect.model.objects;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

/**
 * Holds the information about a single object surface (surface id, name) together with its material assignment.
 */
public class SurfaceConfiguration {
    protected String mSurfaceTypeId;

    protected MaterialMappingConfiguration mMaterialMappingConfiguration = null;

    public SurfaceConfiguration() {
        // For JAXB
    }

    public SurfaceConfiguration(String surfaceTypeId) {
        mSurfaceTypeId = surfaceTypeId;
    }

    @XmlTransient
    public String getSurfaceTypeId() {
        return mSurfaceTypeId;
    }

    @XmlElement(name = "MaterialMapping")
    public MaterialMappingConfiguration getMaterialMappingConfiguration() {
        return mMaterialMappingConfiguration;
    }

    public void setMaterialMappingConfiguration(MaterialMappingConfiguration value) {
        mMaterialMappingConfiguration = value;
    }

    @XmlAttribute(name = "surfaceTypeId")
    public String getSurfaceTypeId_JAXB() {
        return mSurfaceTypeId;
    }

    public void setSurfaceTypeId_JAXB(String value) {
        mSurfaceTypeId = value;
    }
}
