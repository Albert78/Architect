package de.dh.cad.architect.model.changes;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.apache.commons.collections4.CollectionUtils;

import de.dh.cad.architect.model.objects.BaseObject;

public abstract class ObjectModificationChange implements IModelChange {
    protected final Collection<BaseObject> mTargetObjects;

    protected ObjectModificationChange(Collection<BaseObject> targetObjects) {
        mTargetObjects = targetObjects;
    }

    protected ObjectModificationChange(BaseObject... targetObjects) {
        this(Arrays.asList(targetObjects));
    }

    public Collection<BaseObject> getTargetObjects() {
        return mTargetObjects;
    }

    @Override
    public Collection<BaseObject> getAdditions() {
        return Collections.emptyList();
    }

    @Override
    public Collection<BaseObject> getModifications() {
        return mTargetObjects;
    }

    @Override
    public Collection<BaseObject> getRemovals() {
        return Collections.emptyList();
    }

    /**
     * When change classes are derived from this class and a {@link #tryMerge(IModelChange) merge functionality} should be implemented,
     * it is typically necessary to check if the other (to-be-merged) change is of the same class and if the target objects are equal.
     * This is done by this method.
     */
    protected boolean sameKindOfChange(IModelChange otherChange) {
        if (!getClass().equals(otherChange.getClass())) {
            return false;
        }
        ObjectModificationChange other = (ObjectModificationChange) otherChange;
        if (!other.targetsEqual(this)) {
            return false;
        }
        return true;
    }

    /**
     * Returns the information whether the target objects of this change and the given other change are the same.
     */
    public boolean targetsEqual(ObjectModificationChange other) {
        return CollectionUtils.isEqualCollection(mTargetObjects, other.mTargetObjects);
    }
}
