package de.dh.cad.architect.model.changes;

import java.util.Collection;
import java.util.HashSet;

import de.dh.cad.architect.model.objects.BaseObject;

public abstract class ObjectChange implements IModelChange {
    protected final Collection<BaseObject> mAdditions = new HashSet<>();
    protected final Collection<BaseObject> mModifications = new HashSet<>();
    protected final Collection<BaseObject> mRemovals = new HashSet<>();

    public ObjectChange objectAdded(BaseObject bo) {
        mAdditions.add(bo);
        return this;
    }

    public ObjectChange objectsAdded(Collection<? extends BaseObject> bos) {
        mAdditions.addAll(bos);
        return this;
    }

    public ObjectChange objectModified(BaseObject bo) {
        mModifications.add(bo);
        return this;
    }

    public ObjectChange objectsModified(Collection<? extends BaseObject> bos) {
        mModifications.addAll(bos);
        return this;
    }

    public ObjectChange objectRemoved(BaseObject bo) {
        mRemovals.add(bo);
        return this;
    }

    public ObjectChange objectsRemoved(Collection<? extends BaseObject> bos) {
        mRemovals.addAll(bos);
        return this;
    }

    @Override
    public Collection<BaseObject> getAdditions() {
        return mAdditions;
    }

    @Override
    public Collection<BaseObject> getModifications() {
        return mModifications;
    }

    @Override
    public Collection<BaseObject> getRemovals() {
        return mRemovals;
    }
}
