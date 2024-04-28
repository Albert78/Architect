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

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import de.dh.cad.architect.model.assets.AssetRefPath;
import de.dh.cad.architect.model.coords.Dimensions2D;
import de.dh.cad.architect.model.coords.Vector2D;
import de.dh.cad.architect.model.jaxb.AssetRefPathJavaTypeAdapter;
import de.dh.cad.architect.model.jaxb.Dimensions2DJavaTypeAdapter;
import de.dh.cad.architect.model.jaxb.Vector2DJavaTypeAdapter;

/**
 * Holds the information about how to apply a material to a surface.
 */
public class MaterialMappingConfiguration {
    public enum LayoutMode {
        Stretch, Tile
    }

    protected AssetRefPath mMaterialRef;
    protected LayoutMode mLayoutMode = LayoutMode.Stretch;
    protected Vector2D mOffset = null;
    protected Dimensions2D mTileSize = null;
    protected Double mMaterialRotationDeg;

    public MaterialMappingConfiguration() {
        // For JAXB
    }

    public MaterialMappingConfiguration(AssetRefPath materialRef, LayoutMode layoutMode) {
        mMaterialRef = materialRef;
        mLayoutMode = layoutMode;
    }

    public static MaterialMappingConfiguration stretch(AssetRefPath materialRef) {
        return new MaterialMappingConfiguration(materialRef, LayoutMode.Stretch);
    }

    public static MaterialMappingConfiguration tile(AssetRefPath materialRef) {
        return new MaterialMappingConfiguration(materialRef, LayoutMode.Tile);
    }

    public static MaterialMappingConfiguration tile(AssetRefPath materialRef, Vector2D offset, Dimensions2D tileSize) {
        MaterialMappingConfiguration result = new MaterialMappingConfiguration(materialRef, LayoutMode.Tile);
        result.setOffset(offset);
        result.setTileSize(tileSize);
        return result;
    }

    /**
     * Gets the ref path to the material or {@code null} if the default material should not be overridden.
     */
    @XmlElement(name = "MaterialRef")
    @XmlJavaTypeAdapter(AssetRefPathJavaTypeAdapter.class)
    public AssetRefPath getMaterialRef() {
        return mMaterialRef;
    }

    public void setMaterialRef(AssetRefPath value) {
        mMaterialRef = value;
    }

    /**
     * Gets the layout mode of the material or {@code null}. If this value is not given,
     * {@link LayoutMode#Stretch} is the default.
     */
    @XmlElement(name = "Layout")
    public LayoutMode getLayoutMode() {
        return mLayoutMode;
    }

    public void setLayoutMode(LayoutMode value) {
        mLayoutMode = value;
    }

    /**
     * Gets the offset to be applied to the material when being mapped to the object or {@code null}.
     */
    @XmlJavaTypeAdapter(Vector2DJavaTypeAdapter.class)
    @XmlElement(name = "Offset")
    public Vector2D getOffset() {
        return mOffset;
    }

    public void setOffset(Vector2D value) {
        mOffset = value;
    }

    /**
     * Gets the overridden tile size of the material tile or {@code null}.
     */
    @XmlJavaTypeAdapter(Dimensions2DJavaTypeAdapter.class)
    @XmlElement(name = "TileSize")
    public Dimensions2D getTileSize() {
        return mTileSize;
    }

    public void setTileSize(Dimensions2D value) {
        mTileSize = value;
    }

    /**
     * Gets the rotation of the material when being applied to the object or {@code null}.
     */
    @XmlElement(name = "MaterialRotation")
    public Double getMaterialRotationDeg() {
        return mMaterialRotationDeg;
    }

    public void setMaterialRotationDeg(Double value) {
        mMaterialRotationDeg = value;
    }
}
