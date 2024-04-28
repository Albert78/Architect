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
package de.dh.cad.architect.model.jaxb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;

import de.dh.cad.architect.model.assets.RawMaterialModel;
import de.dh.cad.architect.model.jaxb.RawMaterialModelsJavaTypeAdapter.RawMaterialModelsProxy;

public class RawMaterialModelsJavaTypeAdapter extends XmlAdapter<RawMaterialModelsProxy, Map<String, RawMaterialModel>> {
    public static class RawMaterialModelsProxy {
        protected List<RawMaterialModel> mMaterials = new ArrayList<>();

        public RawMaterialModelsProxy() {
            // For JAXB
        }

        @XmlElement(name = "Material")
        public List<RawMaterialModel> getMaterials() {
            return mMaterials;
        }

        public Map<String, RawMaterialModel> toMaterialsMap() {
            Map<String, RawMaterialModel> result = new HashMap<>();
            for (RawMaterialModel material : mMaterials) {
                result.put(material.getName(), material);
            }
            return result;
        }

        public static RawMaterialModelsProxy fromMaterialsMap(Map<String, RawMaterialModel> v) {
            RawMaterialModelsProxy result = new RawMaterialModelsProxy();
            result.getMaterials().addAll(v.values());
            return result;
        }
    }

    @Override
    public Map<String, RawMaterialModel> unmarshal(RawMaterialModelsProxy v) throws Exception {
        return v.toMaterialsMap();
    }

    @Override
    public RawMaterialModelsProxy marshal(Map<String, RawMaterialModel> v) throws Exception {
        return RawMaterialModelsProxy.fromMaterialsMap(v);
    }
}
