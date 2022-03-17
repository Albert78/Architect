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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import de.dh.cad.architect.model.ChangeSet;
import de.dh.cad.architect.model.coords.IPosition;
import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.model.coords.Position3D;
import de.dh.cad.architect.model.jaxb.PositionJavaTypeAdapter;
import de.dh.cad.architect.utils.IdGenerator;
import de.dh.cad.architect.utils.jaxb.IDeserializationHandler;

/**
 * Well defined position in an object (like end of a wall), 2D or 3D.
 * An anchor can either be a <b>handle</b> which means the anchor's position can be changed from the outside, or it is a
 * <b>dependent anchor</b> which means it's position always depends on the owner's handles and properties and will be calculated
 * by its owner object.
 */
@XmlSeeAlso({Position2D.class, Position3D.class})
public class Anchor extends BaseObject implements IDeserializationHandler {
    protected BaseAnchoredObject mAnchorOwner;
    protected String mAnchorType; // Unique in the owner object or null. Used to distinguish between different anchors in an object.
    protected IPosition mPosition;

    protected Optional<Anchor> mODockMaster = Optional.empty();
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

    public static Anchor create(BaseAnchoredObject anchorOwner, String anchorType, IPosition position, ChangeSet changeSet) {
        Anchor result = new Anchor(IdGenerator.generateUniqueId(Anchor.class.getSimpleName() + "-" + anchorOwner.getClass().getSimpleName()), anchorOwner, anchorType, position);
        anchorOwner.addAnchor_Internal(result);
        anchorOwner.getAnchorContainer().addAnchor_Internal(result, changeSet);
        changeSet.added(result);
        return result;
    }

    @Override
    public void afterDeserialize(Object parent) {
        mODockMaster.ifPresent(dockMaster -> dockMaster.getDockSlaves().add(Anchor.this));
    }

    @Override
    public Collection<? extends BaseObject> delete(ChangeSet changeSet) {
        // If docked, remove this anchor from the dock
        Optional<Anchor> oNewDockMaster;
        if (mODockMaster.isPresent()) {
            Anchor dockMaster = mODockMaster.get();
            oNewDockMaster = Optional.of(dockMaster);
            Collection<Anchor> dockMasterSlaves = dockMaster.getDockSlaves();
            dockMasterSlaves.remove(this);
            changeSet.changed(this);
            dockMasterSlaves.addAll(mDockSlaves);
            setODockMaster(Optional.empty());
            changeSet.changed(dockMaster);
        } else {
            oNewDockMaster = Optional.empty();
        }
        for (Anchor mySlave : mDockSlaves) {
            changeSet.changed(mySlave, mySlave.getAnchorOwner());
            mySlave.setODockMaster(oNewDockMaster);
        }
        mDockSlaves.clear();
        mAnchorOwner.removeAnchor_Internal(this);
        mAnchorOwner.getAnchorContainer().removeAnchor_Internal(this, changeSet);
        return super.delete(changeSet);
    }

    @XmlTransient
    public BaseAnchoredObject getAnchorOwner() {
        return mAnchorOwner;
    }

    // Called by the owner after deserialization
    public void setAnchorOwner(BaseAnchoredObject value) {
        mAnchorOwner = value;
    }

    /**
     * Anchor type which uniquely identifies this anchor in its owner.
     */
    @XmlElement(name = "AnchorType")
    public String getAnchorType() {
        return mAnchorType;
    }

    public void setAnchorType(String value) {
        mAnchorType = value;
    }

    @XmlJavaTypeAdapter(PositionJavaTypeAdapter.class)
    @XmlElement(name = "Position")
    public IPosition getPosition() {
        return mPosition;
    }

    /**
     * Sets the anchor's position. The new position must be of the same type as the current position, i.e. a 2D anchor must remain
     * in the 2D space and a 3D anchor must remain in the 3D space.
     */
    // The definition that an anchor will remain in its coordinate space seems to be sensible but is actually not necessary
    public void setPosition(IPosition value) {
        if (mPosition != null && mPosition.getClass() != value.getClass()) {
            throw new RuntimeException("It is not allowed to change the coordinate space of an anchor from 2D to 3D or vice-versa");
        }
        mPosition = value;
    }

    public void setPosition_Internal(IPosition value) {
        mPosition = value;
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
    public Optional<Anchor> getODockMaster() {
        return mODockMaster;
    }

    public void setODockMaster(Optional<Anchor> value) {
        mODockMaster = value;
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
        if (mODockMaster.isPresent()) {
            return mODockMaster.get().getAllDockedAnchors();
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
        if (mODockMaster.isPresent()) {
            return mODockMaster.get().getRootMasterOfAnchorDock();
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
        return mODockMaster.orElse(null);
    }

    public void setDockMaster_JAXB(Anchor value) {
        mODockMaster = Optional.ofNullable(value);
    }

    public String ownerToString() {
        return mAnchorOwner == null ? "Empty" : mAnchorOwner.getClass().getSimpleName() + " " + mAnchorOwner.getId();
    }

    @Override
    protected String attrsToString() {
        return super.attrsToString() + ", AnchorType='" + (mAnchorType == null ? "<Empty>" : mAnchorType) + "', Owner=<" + ownerToString() + ">, Position=[" +
                        mPosition.coordsToString() + "]";
    }
}
