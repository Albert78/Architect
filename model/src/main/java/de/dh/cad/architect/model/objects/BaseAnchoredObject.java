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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlSeeAlso;

import de.dh.cad.architect.model.changes.IModelChange;
import de.dh.cad.architect.model.changes.ObjectChange;
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
            anchor.setAnchorOwner_Internal(this);
        }
    }

    public Anchor createAnchor(String anchorType, IPosition position, List<IModelChange> changeTrace) {
        return Anchor.create(this, anchorType, position, changeTrace);
    }

    public IAnchorContainer getAnchorContainer() {
        return getOwnerContainer().getAnchorContainer();
    }

    public void addAnchor_Internal(Anchor a, List<IModelChange> changeTrace) {
        addAnchor_Internal(mAnchors.size(), a, changeTrace);
    }

    public void addAnchor_Internal(int index, Anchor a, List<IModelChange> changeTrace) {
        a.setAnchorOwner_Internal(this);
        mAnchors.add(index, a);
        changeTrace.add(
                new ObjectChange() {
                    @Override
                    public void undo(List<IModelChange> undoChangeTrace) {
                        removeAnchor_Internal(a, undoChangeTrace);
                    }
                }
                .objectAdded(a)
                .objectModified(this));
    }

    protected void doRemoveAnchorInternal(int index, Anchor anchor, List<IModelChange> changeTrace) {
        anchor.setAnchorOwner_Internal(null);
        mAnchors.remove(index);
        changeTrace.add(
                new ObjectChange() {
                    @Override
                    public void undo(List<IModelChange> undoChangeTrace) {
                        addAnchor_Internal(index, anchor, undoChangeTrace);
                    }
                }
                .objectRemoved(anchor)
                .objectModified(this));
    }

    public Anchor removeAnchor_Internal(int index, List<IModelChange> changeTrace) {
        Anchor result = mAnchors.get(index);
        doRemoveAnchorInternal(index, result, changeTrace);
        return result;
    }

    public void removeAnchor_Internal(Anchor a, List<IModelChange> changeTrace) {
        int index = mAnchors.indexOf(a);
        if (index == -1) {
            return;
        }
        doRemoveAnchorInternal(index, a, changeTrace);
    }

    @Override
    public Collection<? extends BaseObject> delete(List<IModelChange> changeTrace) {
        Collection<BaseObject> result = new ArrayList<>();
        for (BaseObject object : new ArrayList<>(mAnchors)) {
            result.addAll(object.delete(changeTrace));
        }
        result.addAll(super.delete(changeTrace));
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
     *
     * The idea of the reconcile process is like this:
     *
     * Object handle anchors can be moved by the user or automatically follow the dock master anchor, if docked.
     * For example a wall end is moved by the user or a docked ceiling handle anchor is moved as a result of a wall movement.
     * As result of such a handle movement, the object whose (handle) anchor was moved is potentially in an inconsistent state.
     * For example, the wall's corner anchors do not match the new wall end position any more after the wall end handle was repositioned.
     *
     * The reconcile step will correct the positions of dependent anchors, for example will update the wall corners to the new wall
     * end handle position.
     * That update can lead to more anchor movements if handle anchors of other objects are docked to the anchors which just have
     * been updated.
     * So the system will first loop through all objects whose handle anchors are directly or indirectly moved. During that process,
     * each reconcile call produces a {@link ReconcileResult} which collects objects which might have become invalid, for example
     * if a wall end handle anchor was moved, that wall's apex positions become invalid as well as the apex positions of connected
     * walls. All potentially affected objects are added to the reconcile result.
     *
     * After the reconcile step, all objects which have become invalid ("objects to heal") must be healed by calling
     * {@link #healObject()}.
     *
     * @return Collection of changed dependent, non-handle anchors and objects to be healed after the operation.
     * @throws IllegalStateException If the intended anchor moves are not possible for this object.
     */
    // To be overridden by classes which have dependent anchors
    public ReconcileResult reconcileAfterHandleChange(List<IModelChange> changeTrace) throws IllegalStateException {
        return ReconcileResult.empty();
    }

    /**
     * Adapts the object to a new situation, for example updates the wall apex points to new anchor positions.
     * @param healReasons Set of non-formalized healing reasons, can be used to transport hints about the defect
     * to limit the healing process to the defect parts.
     */
    public void healObject(Collection<ObjectHealReason> healReasons, List<IModelChange> changeTrace) {
        // Empty, can be overridden
    }

    /**
     * Updates the given anchor positions and other object properties which depend on the anchor positions.
     */
    // To be overridden to do the actual position update
    public static void updateAnchorPositions(Collection<AnchorTarget> anchorTargets, List<IModelChange> changeTrace) {
        for (AnchorTarget anchorTarget : anchorTargets) {
            Anchor anchor = anchorTarget.getAnchor();
            IPosition targetPosition = anchorTarget.getTargetPosition();
            anchor.setPosition(targetPosition, changeTrace);
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
