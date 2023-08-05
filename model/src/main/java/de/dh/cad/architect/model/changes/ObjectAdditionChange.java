package de.dh.cad.architect.model.changes;

import java.util.Collection;
import java.util.Collections;

import de.dh.cad.architect.model.objects.BaseObject;

public abstract class ObjectAdditionChange implements IModelChange {
    protected final BaseObject mTargetObject;

    protected ObjectAdditionChange(BaseObject targetObject) {
        mTargetObject = targetObject;
    }

    public BaseObject getTargetObject() {
        return mTargetObject;
    }

    @Override
    public Collection<BaseObject> getAdditions() {
        return Collections.singleton(mTargetObject);
    }

    @Override
    public Collection<BaseObject> getModifications() {
        return Collections.emptyList();
    }

    @Override
    public Collection<BaseObject> getRemovals() {
        return Collections.emptyList();
    }
}
