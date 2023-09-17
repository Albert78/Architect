package de.dh.cad.architect.model.changes;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

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
}
