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
package de.dh.cad.architect.ui.view;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.collections4.set.ListOrderedSet;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dh.cad.architect.model.changes.IModelChange;
import de.dh.cad.architect.model.coords.AnchorTarget;
import de.dh.cad.architect.model.coords.IPosition;
import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.model.coords.Position3D;
import de.dh.cad.architect.model.objects.Anchor;
import de.dh.cad.architect.model.objects.BaseAnchoredObject;
import de.dh.cad.architect.model.objects.ObjectHealReason;
import de.dh.cad.architect.model.objects.ReconcileResult;
import de.dh.cad.architect.ui.objects.AbstractObjectUIRepresentation;
import de.dh.cad.architect.ui.objects.ObjectTypesRegistry;

/**
 * Class for collecting moved objects or objects whose position related properties have been changed, for reconciling
 * the objects and docked anchors. This model is used during a move or modify operation of objects
 * to collect/calculate all derived anchor position updates in a transaction. After derived position updates are calcuated and
 * approved by the participating members, the collected updates are executed to match all new object constraints.
 */
public class ObjectReconcileOperation {
    private static final Logger log = LoggerFactory.getLogger(ObjectReconcileOperation.class);

    private static final Length EPSILON = Length.ofMM(0.01);

    protected final String mDescription;
    protected final Collection<BaseAnchoredObject> mUnprocessedObjects = ListOrderedSet.listOrderedSet(new TreeSet<>()); // Objects to be reconciled
    protected final Map<String, AnchorTarget> mProcessedHandleAnchorPositions = new TreeMap<>();
    protected final Collection<BaseAnchoredObject> mProcessedObjects = new TreeSet<>();
    protected final MultiValuedMap<BaseAnchoredObject, ObjectHealReason> mObjectsToHeal = new ArrayListValuedHashMap<>();

    /**
     * Use this constructor if you need to configure this object in separate calls to {@link #trySetTargetHandlePosition(Anchor, IPosition)} or
     * {@link #tryAddObjectToProcess(BaseAnchoredObject)} .
     * @param description Description of the reconcile operation, e.g. "D&D Operation".
     */
    public ObjectReconcileOperation(String description) {
        mDescription = description;
    }

    public ObjectReconcileOperation(String description, Collection<? extends BaseAnchoredObject> changedObjects) {
        mDescription = description;
        mUnprocessedObjects.addAll(changedObjects);
    }

    public String getDescription() {
        return mDescription;
    }

    public Collection<BaseAnchoredObject> getUnprocessedObjects() {
        return mUnprocessedObjects;
    }

    public Collection<BaseAnchoredObject> getProcessedObjects() {
        return mProcessedObjects;
    }

    public void tryAddObjectToProcess(BaseAnchoredObject object) {
        if (mProcessedObjects.contains(object)) {
            // This short-cut can lead to incompletely solved/reconciled situations if the order of objects
            // is wrong, for example if the object, which has already been reconciled, changes as a result
            // of a later reconcile operation.
            // But we also cannot simply remove this short-cut return, else we can get an infinite loop
            // if two objects return each other as included reconcile object.
            // To avoid this, it would require a sophisticated check if none of the properties has
            // changed from which the need for the object to reconcile derives.
            return;
        }
        mUnprocessedObjects.add(object);
    }

    public void addObjectsToHeal(MultiValuedMap<BaseAnchoredObject, ObjectHealReason> healObjects) {
        mObjectsToHeal.putAll(healObjects);
    }

    public void addObjectToHeal(BaseAnchoredObject object, ObjectHealReason reason) {
        mObjectsToHeal.put(object, reason);
    }

    protected void tryAddHandleAnchorChange(Anchor handleAnchor) {
        String anchorId = handleAnchor.getId();
        AnchorTarget processedHandleAnchorTarget = mProcessedHandleAnchorPositions.get(anchorId);
        if (processedHandleAnchorTarget != null && isCompatiblePosition(handleAnchor.getPosition(), processedHandleAnchorTarget.getTargetPosition())) {
            // No change in position
            return;
        }
        BaseAnchoredObject owner = handleAnchor.getAnchorOwner();
        tryAddObjectToProcess(owner);
    }

    protected void tryAddDependentAnchorChanges(Collection<Anchor> changedDependentAnchors, List<IModelChange> changeTrace) {
        for (Anchor changedAnchor : changedDependentAnchors) {
            IPosition position = changedAnchor.getPosition();
            for (Anchor dockedAnchor : changedAnchor.getAllDockedAnchors()) {
                if (!dockedAnchor.equals(changedAnchor)) {
                    dockedAnchor.setPosition(mapTargetPositionForAnchor(dockedAnchor, position), changeTrace);
                    tryAddHandleAnchorChange(dockedAnchor);
                }
            }
        }
    }

    public static IPosition mapTargetPositionForAnchor(Anchor anchor, IPosition targetPosition) {
        return mapTargetPosition(anchor.getPosition(), targetPosition);
    }

    public static IPosition mapTargetPosition(IPosition currentPosition, IPosition targetPosition) {
        if (currentPosition instanceof Position2D) {
            return targetPosition.projectionXY();
        } else if (currentPosition instanceof Position3D cp3d) {
            if (targetPosition instanceof Position3D) {
                return targetPosition;
            } else if (targetPosition instanceof Position2D tp2d) {
                return tp2d.upscale(cp3d.getZ());
            } else {
                throw new NotImplementedException("Unknown type of position " + targetPosition);
            }
        } else {
            throw new NotImplementedException("Unknown type of position " + currentPosition);
        }
    }

    public void reconcileObjects(List<IModelChange> changeTrace) {
        // Each scheduled object will be propagated to its UI reconciler.
        // This can generate more anchor moves, which means the set of anchors to be moved in the reconcile operation structure
        // will grow during the operation. Furthermore, more object properties might become invalid during that process
        // (think of a wall whose wall end handle anchor was moved. After that, the wall's apex points must be repositioned to
        // match the new wall's position and the positions of the connected walls).
        // So, the first loop will process all direct and transitive anchor moves.
        // After that, we go through all objects which were invalidated by the first run ("objects to heal") and heal them.
        try {
            // Step 1: Reconcile object by object
            while (!mUnprocessedObjects.isEmpty()) {
                BaseAnchoredObject object = mUnprocessedObjects.iterator().next();
                mUnprocessedObjects.remove(object);
                AbstractObjectUIRepresentation uiRepresentation = ObjectTypesRegistry.getUIRepresentation(object.getClass());
                IObjectReconciler reconciler = uiRepresentation.getReconciler();
                ReconcileResult reconcileResult = reconciler.reconcileObjectChange(object, changeTrace);
                // Remember positions of processed anchors to be able to determine further changes
                mProcessedHandleAnchorPositions.putAll(AnchorTarget.mapAnchorIdToTargetFromAnchors(object.getHandleAnchors()));
                // Remember processed objects to be able to break infinite loops
                mProcessedObjects.add(object);

                // Get the anchors of the processed object which have been changed during the reconcile operation and
                // propagate their new position to docked anchors, potentially cascading the reconcile process to their owner objects.
                Collection<Anchor> dependentAnchors = reconcileResult.getDependentAnchors();
                tryAddDependentAnchorChanges(dependentAnchors, changeTrace); // Propagate movements of dependent anchors

                // Schedule objects to heal later
                addObjectsToHeal(reconcileResult.getHealObjects());
            }

            // Step 2: Heal objects
            for (Entry<BaseAnchoredObject, Collection<ObjectHealReason>> entry : mObjectsToHeal.asMap().entrySet()) {
                BaseAnchoredObject object = entry.getKey();
                object.healObject(entry.getValue(), changeTrace);
            }
        } catch (Exception e) {
            log.debug("Could not execute object reconcile operation '" + mDescription + "'", e);
        }
    }

    @Override
    public String toString() {
        return mDescription + ": " + mProcessedObjects.size() + " objects processed, " + mUnprocessedObjects.size() + " objects to reconcile";
    }

    public static boolean isCompatiblePosition(IPosition sourcePosition, IPosition targetPosition) {
        if (sourcePosition.getX().difference(targetPosition.getX()).gt(EPSILON)) {
            return false;
        }
        if (sourcePosition.getY().difference(targetPosition.getY()).gt(EPSILON)) {
            return false;
        }
        if (sourcePosition instanceof Position2D || targetPosition instanceof Position2D) {
            return true;
        }
        if (((Position3D) sourcePosition).getZ().difference(((Position3D) targetPosition).getZ()).gt(EPSILON)) {
            return false;
        }
        return true;
    }
}
