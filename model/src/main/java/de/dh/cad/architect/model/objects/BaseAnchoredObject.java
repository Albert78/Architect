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
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlSeeAlso;

import de.dh.cad.architect.model.ChangeSet;
import de.dh.cad.architect.model.coords.AnchorTarget;
import de.dh.cad.architect.model.coords.IPosition;
import de.dh.cad.architect.utils.jaxb.IDeserializationHandler;

/**
 * Some object in a plan which is positioned using anchors as reference points.
 * The positions in this object are defined as anchor points, which are all attached to
 * anchors.
 */
@XmlSeeAlso({ // Add all derived classes for JAXB
    BaseSolidObject.class,
    BaseLimitedPlane.class,
    Floor.class,
    Wall.class,
    Dimensioning.class,
})
public abstract class BaseAnchoredObject extends BaseObject implements IDeserializationHandler {
    protected List<Anchor> mAnchors = new ArrayList<>();

    public BaseAnchoredObject() {
        // For JAXB
    }

    public BaseAnchoredObject(String id, String name) {
        super(id, name);
    }

    @Override
    public void afterDeserialize(Object parent) {
        for (Anchor anchor : mAnchors) {
            anchor.setAnchorOwner(this);
        }
    }

    public Anchor createAnchor(String anchorType, IPosition position, ChangeSet changeSet) {
        return Anchor.create(this, anchorType, position, changeSet);
    }

    public IAnchorContainer getAnchorContainer() {
        return getOwnerContainer().getAnchorContainer();
    }

    public void addAnchor_Internal(Anchor a) {
        a.setAnchorOwner(this);
        mAnchors.add(a);
    }

    public void addAnchor_Internal(int index, Anchor a) {
        a.setAnchorOwner(this);
        mAnchors.add(index, a);
    }

    public Anchor removeAnchor_Internal(int index) {
        return mAnchors.remove(index);
    }

    public void removeAnchor_Internal(Anchor a) {
        mAnchors.remove(a);
    }

    @Override
    public Collection<? extends BaseObject> delete(ChangeSet changeSet) {
        Collection<BaseObject> result = new ArrayList<>();
        for (BaseObject object : new ArrayList<>(mAnchors)) {
            result.addAll(object.delete(changeSet));
        }
        result.addAll(super.delete(changeSet));
        return result;
    }

    @XmlElementWrapper(name = "Anchors")
    @XmlElement(name = "Anchor")
    @XmlIDREF
    public List<Anchor> getAnchors() {
        return mAnchors;
    }

    /**
     * Calculates and sets the new positions for dependent anchors after the handle anchors have been moved to new positions.
     * @return Collection of changed dependent, non-handle anchors and objects to be healed after the operation.
     * @throws IllegalStateException If the intended anchor moves are not possible for this object.
     */
    // To be overridden by classes which have dependent anchors
    public ReconcileResult reconcileAfterHandleChange(ChangeSet changeSet) throws IllegalStateException {
        return ReconcileResult.empty();
    }

    /**
     * Adapts the object to a new situation, for example updates the wall bevel points to new anchor positions.
     * @param healReasons Set of non-formalized healing reasons, can be used to transport hints about the defect
     * to limit the healing process to the defect parts.
     */
    public void healObject(Collection<ObjectHealReason> healReasons, ChangeSet changeSet) {
        // Empty, can be overridden
    }

    /**
     * Updates the given given anchor positions and other dependent object properties.
     */
    // To be overridden to do the actual position update
    public void updateAnchorPositions(Collection<AnchorTarget> anchorTargets, ChangeSet changeSet) {
        for (AnchorTarget anchorTarget : anchorTargets) {
            Anchor anchor = anchorTarget.getAnchor();
            IPosition targetPosition = anchorTarget.getTargetPosition();
            anchor.setPosition(targetPosition);
            changeSet.changed(anchor);
            changeSet.changed(anchor.getAnchorOwner());
        }
    }

    /**
     * Returns the information whether the given (child) anchor is a handle anchor, i.e. it can be moved by the user or by other modules than this object.
     * Anchors which are no handles must only be moved by this owner object.
     * This method must only be used for anchors whose owner is this object.
     */
    public boolean isHandle(Anchor anchor) {
        return false;
    }

    public Anchor getAnchorByAnchorType(String anchorType) {
        for (Anchor a : mAnchors) {
            String currentAnchorType = a.getAnchorType();
            if (currentAnchorType != null && currentAnchorType.equals(anchorType)) {
                return a;
            }
        }
        return null;
    }

    public Collection<Anchor> getHandleAnchors() {
        Collection<Anchor> result = new ArrayList<>(mAnchors.size());
        for (Anchor anchor : mAnchors) {
            if (anchor.isHandle()) {
                result.add(anchor);
            }
        }
        return result;
    }

    public Collection<Anchor> getDependentAnchors() {
        Collection<Anchor> result = new ArrayList<>(mAnchors.size());
        for (Anchor anchor : mAnchors) {
            if (!anchor.isHandle()) {
                result.add(anchor);
            }
        }
        return result;
    }

    @Override
    protected String attrsToString() {
        return super.attrsToString() +
                ", Anchors=[" + mAnchors.stream()
                    .map(a -> Optional.ofNullable(a.getAnchorType()).map(at -> at + " -> ").orElse("") + a.getPosition().coordsToString())
                    .collect(Collectors.joining(", ")) + "]";
    }

    public String anchorCoordsToStr() {
        return mAnchors
                .stream()
                .map(a -> a.getPosition().coordsToString())
                .collect(Collectors.joining(", "));
    }

    public boolean isPossibleDimensioningDockTargetAnchor(Anchor anchor) {
        return true;
    }
}
