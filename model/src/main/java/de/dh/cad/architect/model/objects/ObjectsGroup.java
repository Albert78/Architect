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
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.commons.lang3.StringUtils;

import de.dh.cad.architect.model.ChangeSet;
import de.dh.cad.architect.utils.jaxb.IDeserializationHandler;

public class ObjectsGroup extends BaseObject implements IDeserializationHandler {
    protected Set<BaseObject> mGroupedObjects = new TreeSet<>();

    public ObjectsGroup() {
        // For JAXB
    }

    public ObjectsGroup(String id, String name, IObjectsContainer ownerContainer, ChangeSet changeSet) {
        super(id, name);
        ownerContainer.addOwnedChild_Internal(this, changeSet);
    }

    @Override
    public void afterDeserialize(Object parent) {
        for (BaseObject baseObject : mGroupedObjects) {
            baseObject.addToGroup_Internal(this);
        }
    }

    @XmlElementWrapper(name = "Objects")
    @XmlElement(name = "Object")
    @XmlIDREF
    public Set<BaseObject> getGroupedObjects() {
        return mGroupedObjects;
    }

    /**
     * Adds the given object to this group.
     */
    public void addObject(BaseObject bo, ChangeSet changeSet) {
        changeSet.changed(this);
        changeSet.changed(bo);
        mGroupedObjects.add(bo);
        bo.addToGroup_Internal(this);
    }

    public void removeObject(BaseObject bo, ChangeSet changeSet) {
        changeSet.changed(this);
        changeSet.changed(bo);
        mGroupedObjects.remove(bo);
        bo.removeFromGroup_Internal(this);
    }

    public void removeAllGroupedObjects(ChangeSet changeSet) {
        for (BaseObject object : new ArrayList<>(mGroupedObjects)) {
            removeObject(object, changeSet);
        }
    }

    @Override
    public Collection<? extends BaseObject> delete(ChangeSet changeSet) {
        removeAllGroupedObjects(changeSet);
        return super.delete(changeSet);
    }

    @XmlTransient
    public Set<String> getGroupedObjectIds() {
        return mGroupedObjects
                .stream()
                .map(o -> o.getId())
                .collect(Collectors.toCollection(TreeSet::new));
    }

    public Set<BaseObject> getGroupedObjectsRecursive() {
        Set<BaseObject> result = new TreeSet<>();
        for (BaseObject bo : mGroupedObjects) {
            result.add(bo);
            if (bo instanceof ObjectsGroup group) {
                result.addAll(group.getGroupedObjectsRecursive());
            }
        }
        return result;
    }

    public Set<String> getGroupedObjectIdsRecursive() {
        Set<String> result = new TreeSet<>();
        for (BaseObject bo : mGroupedObjects) {
            result.add(bo.getId());
            if (bo instanceof ObjectsGroup group) {
                result.addAll(group.getGroupedObjectIdsRecursive());
            }
        }
        return result;
    }

    public boolean isTopLevelGroup() {
        return mGroups.isEmpty();
    }

    @Override
    protected String attrsToString() {
        return super.attrsToString() + ", Objects=[" + StringUtils.join(getGroupedObjectIds(), ", ") + "]";
    }
}
