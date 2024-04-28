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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import de.dh.cad.architect.model.jaxb.RawMaterialModelsJavaTypeAdapter;

/**
 * A material resource model containing metadata together with the raw material commands.
 */
public class MaterialsModel extends AbstractModelResource {
    protected Map<String, RawMaterialModel> mMaterials = new HashMap<>();

    public MaterialsModel() {
        // For JAXB
    }

    public MaterialsModel(Collection<RawMaterialModel> materials) {
        for (RawMaterialModel material : materials) {
            mMaterials.put(material.getName(), material);
        }
    }

    @XmlElement(name = "Materials")
    @XmlJavaTypeAdapter(RawMaterialModelsJavaTypeAdapter.class)
    public Map<String, RawMaterialModel> getMaterials() {
        return mMaterials;
    }

    public void setMaterials(Map<String, RawMaterialModel> value) {
        mMaterials = value;
    }
}
