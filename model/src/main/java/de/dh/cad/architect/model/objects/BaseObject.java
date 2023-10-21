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
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlTransient;

import de.dh.cad.architect.model.changes.IModelChange;
import de.dh.cad.architect.model.changes.ObjectModificationChange;

/**
 * An object in a plan, visible like a wall or invisible like a group.
 */
@XmlSeeAlso({ // Add all derived classes for JAXB
    Anchor.class,
    BaseAnchoredObject.class,
    ObjectsGroup.class
})
public abstract class BaseObject implements Comparable<BaseObject> {
    protected Set<ObjectsGroup> mGroups = new TreeSet<>();
    protected IObjectsContainer mOwnerContainer = null;
    protected String mId;
    protected String mName;
    protected boolean mHidden = false;

    public BaseObject() {
        // For JAXB
    }

    public BaseObject(String id, String name) {
        mId = id;
        mName = name;
    }

    public Collection<? extends BaseObject> delete(List<IModelChange> changeTrace) {
        for (ObjectsGroup group : new ArrayList<>(mGroups)) {
            group.removeObject(this, changeTrace);
        }
        if (mOwnerContainer != null) {
            mOwnerContainer.removeOwnedChild_Internal(this, changeTrace);
        }
        return Collections.singleton(this);
    }

    @XmlTransient
    public String getId() {
        return mId;
    }

    @XmlTransient
    public IObjectsContainer getOwnerContainer() {
        return mOwnerContainer;
    }

    public void setOwnerContainer_Internal(IObjectsContainer value) {
        mOwnerContainer = value;
    }

    @XmlTransient
    public Set<ObjectsGroup> getGroups() {
        return mGroups;
    }

    @XmlTransient
    public String getName() {
        return mName;
    }

    public void setName(String value, List<IModelChange> changeTrace) {
        String oldName = mName;
        mName = value;
        changeTrace.add(new ObjectModificationChange(this) {
            @Override
            public void undo(List<IModelChange> undoChangeTrace) {
                setName(oldName, undoChangeTrace);
            }
        });
    }

    /**
     * Returns the information whether this object is hidden from the view.
     */
    // We use "hidden" and not "visible" to express that the hidden state is something special
    @XmlTransient
    public boolean isHidden() {
        return mHidden;
    }

    public void setHidden(boolean value, List<IModelChange> changeTrace) {
        boolean oldHidden = mHidden;
        mHidden = value;
        changeTrace.add(new ObjectModificationChange(this) {
            @Override
            public void undo(List<IModelChange> undoChangeTrace) {
                setHidden(oldHidden, undoChangeTrace);
            }
        });
    }

    /**
     * Internal method to be used in model module.
     * Clients should call {@link ObjectsGroup#addObject(BaseObject, ChangeSet)}.
     */
    protected void addToGroup_Internal(ObjectsGroup group) {
        mGroups.add(group);
    }

    /**
     * Internal method to be used in model module.
     * Clients should call {@link ObjectsGroup#removeObject(BaseObject, ChangeSet)}.
     */
    protected void removeFromGroup_Internal(ObjectsGroup group) {
        mGroups.remove(group);
    }

    @XmlID
    @XmlAttribute(name = "id")
    public String getId_JAXB() {
        return mId;
    }

    public void setId_JAXB(String value) {
        mId = value;
    }

    @XmlAttribute(name = "name")
    public String getName_JAXB() {
        return mName;
    }

    public void setName_JAXB(String value) {
        mName = value;
    }

    @XmlElement(name = "Hidden")
    public boolean isHidden_JAXB() {
        return mHidden;
    }

    public void setHidden_JAXB(boolean value) {
        mHidden = value;
    }

    protected String attrsToString() {
        return "Id=" + mId + ", Name=" + (mName == null ? "<Empty>" : ("'" + mName + "'")) + ", OwnerContainer=" + (mOwnerContainer == null ? "<Empty>" : mOwnerContainer)
                + ", #Groups=" + mGroups.size();
    }

    @Override
    public int compareTo(BaseObject o) {
        return mId.compareTo(o.mId);
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
        BaseObject other = (BaseObject) obj;
        return Objects.equals(mId, other.mId);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [" + attrsToString() + "]";
    }
}
