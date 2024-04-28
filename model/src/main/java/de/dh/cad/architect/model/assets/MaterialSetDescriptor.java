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

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "MaterialSetDescriptor")
public class MaterialSetDescriptor extends AbstractAssetDescriptor {
    public MaterialSetDescriptor() {
        // For JAXB
    }

    public MaterialSetDescriptor(String id, AssetRefPath selfRef) {
        super(id, selfRef);
    }

    @Override
    public String toString() {
        return "MaterialSetDescriptor <" + mSelfRef + ">";
    }
}
