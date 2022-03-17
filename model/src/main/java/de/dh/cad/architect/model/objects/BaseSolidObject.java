/*******************************************************************************
 *     Architect - A free 2D/3D home and interior designer
 *     Copyright (C) 2021, 2022  Daniel Höh
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlTransient;

import de.dh.cad.architect.model.assets.AssetRefPath;

public abstract class BaseSolidObject extends BaseAnchoredObject {
    protected Map<String, SurfaceConfiguration> mSurfaceTypeIdsToSurfaceConfigurations = new TreeMap<>();
    protected Collection<SurfaceConfiguration> mSurfaceConfigurations = new ArrayList<>();

    public BaseSolidObject() {
        // For JAXB
    }

    public BaseSolidObject(String id, String name) {
        super(id, name);
        initializeSurfaces();
    }

    public abstract void initializeSurfaces();

    @Override
    public void afterDeserialize(Object parent) {
        super.afterDeserialize(parent);
        for (SurfaceConfiguration surfaceConfiguration : mSurfaceConfigurations) {
            mSurfaceTypeIdsToSurfaceConfigurations.put(surfaceConfiguration.getSurfaceTypeId(), surfaceConfiguration);
        }
    }

    /**
     * Returns all configurable surfaces of this object.
     */
    @XmlElementWrapper(name = "SurfaceConfigurations")
    @XmlElement(name = "Surface")
    public Collection<SurfaceConfiguration> getSurfaceConfigurations() {
        return mSurfaceConfigurations;
    }

    /**
     * Same data as {@link #getSurfaceConfigurations()} but held as map (surface configuration id -> surface configuration).
     */
    @XmlTransient
    public Map<String, SurfaceConfiguration> getSurfaceTypeIdsToSurfaceConfigurations() {
        return mSurfaceTypeIdsToSurfaceConfigurations;
    }

    public void clearSurfaces() {
        mSurfaceConfigurations.clear();
        mSurfaceTypeIdsToSurfaceConfigurations.clear();
    }

    /**
     * Generates and adds a new (object specific) surface.
     * @return The generated surface id.
     */
    protected SurfaceConfiguration createSurface(String typeId) {
        SurfaceConfiguration result = new SurfaceConfiguration(typeId);
        mSurfaceConfigurations.add(result);
        mSurfaceTypeIdsToSurfaceConfigurations.put(typeId, result);
        return result;
    }

    @XmlTransient
    public Map<String, AssetRefPath> getOverriddenSurfaceMaterialRefs() {
        Map<String, AssetRefPath> result = new HashMap<>();
        for (SurfaceConfiguration sc : getSurfaceConfigurations()) {
            AssetRefPath materialRefPath = sc.getMaterialAssignment();
            if (materialRefPath == null) {
                continue;
            }
            result.put(sc.getSurfaceTypeId(), materialRefPath);
        }
        return result;
    }

    public void setOverriddenSurfaceMaterialRefs(Map<String, AssetRefPath> value) {
        for (SurfaceConfiguration sc : getSurfaceConfigurations()) {
            String surfaceName = sc.getSurfaceTypeId();
            AssetRefPath overriddenMaterialAssetRefPath = value.get(surfaceName);
            sc.setMaterialAssignment(overriddenMaterialAssetRefPath);
        }
    }
}
