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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlTransient;

import de.dh.cad.architect.model.assets.AssetRefPath;
import de.dh.cad.architect.model.changes.IModelChange;
import de.dh.cad.architect.model.changes.ObjectModificationChange;

public abstract class BaseSolidObject extends BaseAnchoredObject {
    protected Map<String, SurfaceConfiguration> mSurfaceTypeIdsToSurfaceConfigurations = new TreeMap<>();
    protected Collection<SurfaceConfiguration> mSurfaceConfigurations = new ArrayList<>();

    public BaseSolidObject() {
        // For JAXB
    }

    public BaseSolidObject(String id, String name) {
        super(id, name);
    }

    /**
     * Should be overridden and called in each concrete sub classes factory methods.
     */
    protected abstract void initializeSurfaces(List<IModelChange> changeTrace);

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

    protected void addSurfaceConfiguration_Internal(SurfaceConfiguration surfaceConfiguration, List<IModelChange> changeTrace) {
        mSurfaceConfigurations.add(surfaceConfiguration);
        String surfaceTypeId = surfaceConfiguration.getSurfaceTypeId();
        mSurfaceTypeIdsToSurfaceConfigurations.put(surfaceTypeId, surfaceConfiguration);
        changeTrace.add(new ObjectModificationChange(this) {
            @Override
            public void undo(List<IModelChange> undoChangeTrace) {
                removeSurfaceConfiguration_Internal(surfaceTypeId, undoChangeTrace);
            }
        });
    }

    protected void removeSurfaceConfiguration_Internal(String surfaceTypeId, List<IModelChange> changeTrace) {
        SurfaceConfiguration surfaceConfiguration = mSurfaceTypeIdsToSurfaceConfigurations.get(surfaceTypeId);
        if (surfaceConfiguration == null) {
            throw new IllegalArgumentException("Surface type id '" + surfaceTypeId + "' is not present in <" + this + ">");
        }
        mSurfaceConfigurations.remove(surfaceConfiguration);
        mSurfaceTypeIdsToSurfaceConfigurations.remove(surfaceTypeId);
        changeTrace.add(new ObjectModificationChange(this) {
            @Override
            public void undo(List<IModelChange> undoChangeTrace) {
                addSurfaceConfiguration_Internal(surfaceConfiguration, undoChangeTrace);
            }
        });
    }

    protected void clearSurfaces(List<IModelChange> changeTrace) {
        if (!mSurfaceConfigurations.isEmpty() || !mSurfaceTypeIdsToSurfaceConfigurations.isEmpty()) {
            for (String surfaceTypeId : new ArrayList<>(mSurfaceTypeIdsToSurfaceConfigurations.keySet())) {
                removeSurfaceConfiguration_Internal(surfaceTypeId, changeTrace);
            }
        }
    }

    /**
     * Generates and adds a new (object specific) surface.
     * @return The generated surface id.
     */
    protected SurfaceConfiguration createSurface(String surfaceTypeId, List<IModelChange> changeTrace) {
        if (mSurfaceTypeIdsToSurfaceConfigurations.containsKey(surfaceTypeId)) {
            throw new IllegalArgumentException("Solid object <" + this + "> already contains a surface of type id '" + surfaceTypeId + "'");
        }
        SurfaceConfiguration result = new SurfaceConfiguration(surfaceTypeId);
        addSurfaceConfiguration_Internal(result, changeTrace);
        return result;
    }

    public void setSurfaceMaterial(String surfaceTypeId, AssetRefPath materialRefPath, List<IModelChange> changeTrace) {
        SurfaceConfiguration surfaceConfiguration = mSurfaceTypeIdsToSurfaceConfigurations.get(surfaceTypeId);
        if (surfaceConfiguration == null) {
            throw new IllegalArgumentException("Solid object <" + this + "> already does not contain a surface of type id '" + surfaceTypeId + "'");
        }
        AssetRefPath oldMaterialRef = surfaceConfiguration.getMaterialAssignment();
        surfaceConfiguration.setMaterialAssignment(materialRefPath);
        changeTrace.add(new ObjectModificationChange(this) {
            @Override
            public void undo(List<IModelChange> undoChangeTrace) {
                setSurfaceMaterial(surfaceTypeId, oldMaterialRef, undoChangeTrace);
            }
        });
    }

    @XmlTransient
    public Map<String, AssetRefPath> getSurfaceMaterialRefs() {
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
}
