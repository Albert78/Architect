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

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dh.cad.architect.model.changes.IModelChange;
import de.dh.cad.architect.model.changes.ObjectChange;
import de.dh.cad.architect.model.changes.ObjectModificationChange;
import de.dh.cad.architect.model.coords.IPosition;
import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.model.coords.Position3D;
import de.dh.cad.architect.model.jaxb.PositionJavaTypeAdapter;
import de.dh.cad.architect.utils.IdGenerator;
import de.dh.cad.architect.utils.jaxb.IDeserializationHandler;

/**
 * Well defined position in an object (like end of a wall), 2D or 3D, which can be docked to another anchor or to which other
 * anchors can be docked.
 * An anchor can either be a <b>handle</b> which means the anchor's position can be changed from the outside, or it is a
 * <b>dependent anchor</b> which means it's position always depends on the owner's handles and properties and will be calculated
 * by its owner object. Only handles can be docked to other anchors.
 *
 * Anchors don't participate in the {@link #getOwnerContainer() owner container} containment hierarchy, they are managed by their
 * {@link #getAnchorOwner() anchor owner} and its {@link BaseAnchoredObject#getAnchorContainer() anchor container}.
 */
@XmlSeeAlso({Position2D.class, Position3D.class})
public class Anchor extends BaseObject implements IDeserializationHandler {
    private static final Logger log = LoggerFactory.getLogger(Anchor.class);

    protected BaseAnchoredObject mAnchorOwner;
    protected String mAnchorType; // Unique in the owner object or null. Used to distinguish between different anchors in an object.
    protected IPosition mPosition;

    protected Optional<Anchor> mDockMaster = Optional.empty();
    protected List<Anchor> mDockSlaves = new ArrayList<>();

    public Anchor() {
        // For JAXB
    }

    public Anchor(String id, BaseAnchoredObject anchorOwner, String anchorType, IPosition position) {
        super(id, null);
        mAnchorOwner = anchorOwner;
        mAnchorType = anchorType;
        mPosition = position;
    }

    public static Anchor create(BaseAnchoredObject anchorOwner, String anchorType, IPosition position, List<IModelChange> changeTrace) {
        Anchor result = new Anchor(IdGenerator.generateUniqueId(Anchor.class.getSimpleName() + "-" + anchorOwner.getClass().getSimpleName()), anchorOwner, anchorType, position);
        anchorOwner.addAnchor_Internal(result, changeTrace);
        anchorOwner.getAnchorContainer().addAnchor_Internal(result, changeTrace);
        return result;
    }

    @Override
    public void afterDeserialize(Object parent) {
        mDockMaster.ifPresent(dockMaster -> dockMaster.getDockSlaves().add(Anchor.this));
    }

    public void setDockMaster(Anchor newDockMaster, Optional<Integer> oNewDockSlaveIndex, List<IModelChange> changeTrace) {
        mDockMaster.ifPresent(oldDockMaster -> {
            List<Anchor> oldMasterSlaves = oldDockMaster.getDockSlaves();
            int oldDockSlaveIndex = oldMasterSlaves.indexOf(this);
            if (oldDockSlaveIndex == -1) {
                log.error("Inconsistent state: DockMaster of anchor <" + this + "> doesn't know that anchor as dock slave", new IllegalStateException());
                return;
            }
            changeTrace.add(
                new ObjectChange() {
                    @Override
                    public void undo(List<IModelChange> undoChangeTrace) {
                        setDockMaster(oldDockMaster, Optional.of(oldDockSlaveIndex), undoChangeTrace);
                    }
                }
                .objectModified(oldDockMaster)
                .objectModified(this)
                .objectsModified(oldDockMaster.getAllDockOwners()));
            oldMasterSlaves.remove(this);
            mDockMaster = Optional.empty();
        });
        if (newDockMaster != null) {
            mDockMaster = Optional.of(newDockMaster);
            List<Anchor> newDockMasterSlaves = newDockMaster.getDockSlaves();
            if (oNewDockSlaveIndex.isPresent()) {
                int newDockSlaveIndex = oNewDockSlaveIndex.get();
                int newDockMasterSlavesCount = newDockMasterSlaves.size();
                if (newDockSlaveIndex < 0 || newDockSlaveIndex > newDockMasterSlavesCount) {
                    log.warn("Cannot add anchor <" + this + "> as dock slave to master anchor <" + newDockMaster + " at position " + newDockSlaveIndex + ", master has only " + newDockMasterSlavesCount + " dock slaves. Adding anchor at the end of the dock slaves list.", new InvalidParameterException("oNewDockSlaveIndex"));
                    newDockSlaveIndex = newDockMasterSlavesCount;
                }
                newDockMasterSlaves.add(newDockSlaveIndex, this);
            } else {
                newDockMasterSlaves.add(this);
            }
            changeTrace.add(
                new ObjectChange() {
                    @Override
                    public void undo(List<IModelChange> undoChangeTrace) {
                        setDockMaster(null, undoChangeTrace);
                    }
                }
                .objectModified(newDockMaster)
                .objectModified(this)
                .objectsModified(newDockMaster.getAllDockOwners()));
        }
    }

    public void setDockMaster(Anchor newDockMaster, int newDockSlaveIndex, List<IModelChange> changeTrace) {
        setDockMaster(newDockMaster, Optional.of(newDockSlaveIndex), changeTrace);
    }

    public void setDockMaster(Anchor newDockMaster, List<IModelChange> changeTrace) {
        setDockMaster(newDockMaster, Optional.empty(), changeTrace);
    }

    public void undockFromDockMaster(List<IModelChange> changeTrace) {
        setDockMaster(null, changeTrace);
    }

    public void undockAllDockSlaves(List<IModelChange> changeTrace) {
        for (Anchor dockSlave : mDockSlaves) {
            dockSlave.setDockMaster(null, changeTrace);
        }
        mDockSlaves.clear();
    }

    @Override
    public Collection<? extends BaseObject> delete(List<IModelChange> changeTrace) {
        undockAllDockSlaves(changeTrace);
        setDockMaster(null, -1, changeTrace);
        BaseAnchoredObject anchorOwner = mAnchorOwner; // mAnchorOwner will be set to null by side effect of next method call
        anchorOwner.removeAnchor_Internal(this, changeTrace);
        anchorOwner.getAnchorContainer().removeAnchor_Internal(this, changeTrace);
        return super.delete(changeTrace);
    }

    @XmlTransient
    public BaseAnchoredObject getAnchorOwner() {
        return mAnchorOwner;
    }

    /**
     * Overridden to show that anchors don't have an owner container by design.
     * Anchors are managed by their anchor owner and its anchor container.
     */
    @Override
    public IObjectsContainer getOwnerContainer() {
        return null;
    }

    @Override
    public void setOwnerContainer_Internal(IObjectsContainer value) {
        // Don't set owner container for anchors
    }

    // Called by the owner after deserialization
    protected void setAnchorOwner_Internal(BaseAnchoredObject value) {
        mAnchorOwner = value;
    }

    /**
     * Anchor type which uniquely identifies this anchor in its owner.
     */
    @XmlTransient
    public String getAnchorType() {
        return mAnchorType;
    }

    public void setAnchorType_Internal(String value) {
        mAnchorType = value;
    }

    @XmlTransient
    public IPosition getPosition() {
        return mPosition;
    }

    /**
     * Sets the anchor's position. The new position must be of the same type as the current position, i.e. a 2D anchor must remain
     * in the 2D space and a 3D anchor must remain in the 3D space.
     */
    // The definition that an anchor will remain in its coordinate space seems to be sensible but is actually not necessary
    public void setPosition(IPosition value, List<IModelChange> changeTrace) {
        if (mPosition != null && mPosition.getClass() != value.getClass()) {
            throw new RuntimeException("It is not allowed to change the coordinate space of an anchor from 2D to 3D or vice-versa");
        }
        IPosition oldValue = mPosition;
        mPosition = value;
        changeTrace.add(new ObjectModificationChange(this, getAnchorOwner()) {
            @Override
            public void undo(List<IModelChange> undoChangeTrace) {
                setPosition(oldValue, undoChangeTrace);
            }
        });
    }

    public void setDockPosition(IPosition position, List<IModelChange> changeTrace) {
        for (Anchor anchor : getAllDockedAnchors()) {
            anchor.setPosition(position, changeTrace);
        }
    }

    @XmlTransient
    public boolean is2D() {
        return mPosition instanceof Position2D;
    }

    @XmlTransient
    public boolean is3D() {
        return mPosition instanceof Position3D;
    }

    public Position2D requirePosition2D() {
        return (Position2D) mPosition;
    }

    public Position2D projectionXY() {
        return mPosition.projectionXY();
    }

    public Position3D requirePosition3D() {
        return (Position3D) mPosition;
    }

    public Position3D getPosition3D(Length optionalZ) {
        return mPosition instanceof Position3D p3d ? p3d : mPosition.withZ(optionalZ);
    }

    @XmlTransient
    public boolean isHandle() {
        return mAnchorOwner.isHandle(this);
    }

    /**
     * Gets the anchor, to which this anchor is docked.
     */
    @XmlTransient
    public Optional<Anchor> getDockMaster() {
        return mDockMaster;
    }

    @XmlTransient
    public List<Anchor> getDockSlaves() {
        return mDockSlaves;
    }

    /**
     * Convenience method to get all anchors which are docked with this anchor, also containing this anchor.
     * The order of the returned collection is the same no matter on which of the docked anchors this method is called.
     */
    @XmlTransient
    public List<Anchor> getAllDockedAnchors() {
        // We always start at the dock master, going through its slaves
        if (mDockMaster.isPresent()) {
            return mDockMaster.get().getAllDockedAnchors();
        }
        // We're the master
        return getAllDockedAnchorsDownStream();
    }

    public List<BaseAnchoredObject> getAllDockOwners() {
        return getAllDockedAnchors()
                        .stream()
                        .map(anchor -> anchor.getAnchorOwner())
                        .collect(Collectors.toList());
    }

    /**
     * Convenience method to find the root master anchor of the dock to wich this anchor is docked.
     * That root master is the root of the dock hierarchy which controls the dock position. All other anchors
     * in the dock are direct or indirect slaves of that root master anchor and cannot change their
     * positions theirselves while docked.
     */
    @XmlTransient
    public Anchor getRootMasterOfAnchorDock() {
        if (mDockMaster.isPresent()) {
            return mDockMaster.get().getRootMasterOfAnchorDock();
        }
        return this;
    }

    /**
     * Gets all direct and indirect docked slave anchors.
     */
    @XmlTransient
    public List<Anchor> getAllDockedAnchorsDownStream() {
        List<Anchor> result = new ArrayList<>();
        result.add(this);
        for (Anchor dockSlave : mDockSlaves) {
            result.addAll(dockSlave.getAllDockedAnchorsDownStream());
        }
        return result;
    }

    @XmlElement(name = "DockMaster")
    @XmlIDREF
    public Anchor getDockMaster_JAXB() {
        return mDockMaster.orElse(null);
    }

    public void setDockMaster_JAXB(Anchor value) {
        mDockMaster = Optional.ofNullable(value);
    }

    @XmlElement(name = "AnchorType")
    public String getAnchorType_JAXB() {
        return mAnchorType;
    }

    public void setAnchorType_JAXB(String value) {
        mAnchorType = value;
    }

    @XmlJavaTypeAdapter(PositionJavaTypeAdapter.class)
    @XmlElement(name = "Position")
    public IPosition getPosition_JAXB() {
        return mPosition;
    }

    public void setPosition_JAXB(IPosition value) {
        mPosition = value;
    }

    public String ownerToString() {
        return mAnchorOwner == null ? "Empty" : mAnchorOwner.getClass().getSimpleName() + " " + mAnchorOwner.getId();
    }

    @Override
    protected String attrsToString() {
        return super.attrsToString() + ", AnchorType='" + (mAnchorType == null ? "<Empty>" : mAnchorType) + "', AnchorOwner=<" + ownerToString() + ">, Position=[" +
                        mPosition.coordsToString() + "]";
    }
}
