package de.dh.cad.architect.model.changes;

import java.util.Collection;
import java.util.Collections;

import de.dh.cad.architect.model.objects.BaseObject;

public abstract class ObjectRemovalChange implements IModelChange {
    protected final BaseObject mTargetObject;

    protected ObjectRemovalChange(BaseObject targetObject) {
        mTargetObject = targetObject;
    }

    public BaseObject getTargetObject() {
        return mTargetObject;
    }

    @Override
    public Collection<BaseObject> getAdditions() {
        return Collections.emptyList();
    }

    @Override
    public Collection<BaseObject> getModifications() {
        return Collections.emptyList();
    }

    @Override
    public Collection<BaseObject> getRemovals() {
        return Collections.singleton(mTargetObject);
    }
}
