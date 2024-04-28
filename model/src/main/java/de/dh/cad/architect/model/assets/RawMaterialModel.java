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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import de.dh.cad.architect.model.coords.Vector2D;
import de.dh.cad.architect.model.jaxb.OptionalVector2DJavaTypeAdapter;

/**
 * A material model containing metadata together with the raw material commands.
 */
public class RawMaterialModel {
    protected String mName;
    protected Optional<Vector2D> mTileSize = Optional.empty();
    protected List<String> mCommands = new ArrayList<>();

    public RawMaterialModel() {
        // For JAXB
    }

    public RawMaterialModel(String name, Optional<Vector2D> tileSize, List<String> commands) {
        mName = name;
        mTileSize = tileSize;
        mCommands.addAll(commands);
    }

    @XmlElement(name = "Name")
    public String getName() {
        return mName;
    }

    public void setName(String value) {
        mName = value;
    }

    @XmlElement(name = "TileSize")
    @XmlJavaTypeAdapter(OptionalVector2DJavaTypeAdapter.class)
    public Optional<Vector2D> getTileSize() {
        return mTileSize;
    }

    public void setTileSize(Optional<Vector2D> value) {
        mTileSize = value;
    }

    @XmlElementWrapper(name = "Commands")
    @XmlElement(name = "Line")
    public List<String> getCommands() {
        return mCommands;
    }

    public void setCommands(List<String> value) {
        mCommands = value;
    }
}
