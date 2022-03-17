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
package de.dh.cad.architect.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import de.dh.cad.architect.model.objects.BaseObject;

/**
 * Collects changes during a model update.
 */
public class ChangeSet {
    protected Collection<BaseObject> mChanges = null;
    protected Collection<BaseObject> mAdditions = null;
    protected Collection<BaseObject> mRemovals = null;

    public void changed(Collection<? extends BaseObject> objs) {
        Collection<? extends BaseObject> objsModified = new ArrayList<>(objs);
        if (mAdditions != null) {
            objsModified.removeAll(mAdditions);
        }
        if (mRemovals != null) {
            objsModified.removeAll(mRemovals);
        }
        if (objsModified.isEmpty()) {
            return;
        }
        if (mChanges == null) {
            mChanges = new HashSet<>();
        }
        mChanges.addAll(objsModified);
    }

    public void changed(BaseObject... objs) {
        changed(Arrays.asList(objs));
    }

    public void added(Collection<? extends BaseObject> objs) {
        if (mAdditions == null) {
            mAdditions = new HashSet<>();
        }
        mAdditions.addAll(objs);
        if (mChanges != null) {
            mChanges.removeAll(objs);
        }
        if (mRemovals != null) {
            mRemovals.removeAll(objs);
        }
    }

    public void added(BaseObject... objs) {
        added(Arrays.asList(objs));
    }

    public void removed(Collection<? extends BaseObject> objs) {
        if (mRemovals == null) {
            mRemovals = new HashSet<>();
        }
        mRemovals.addAll(objs);
        if (mChanges != null) {
            mChanges.removeAll(objs);
        }
        if (mAdditions != null) {
            mAdditions.removeAll(objs);
        }
    }

    public void removed(BaseObject... objs) {
        removed(Arrays.asList(objs));
    }

    public Collection<? extends BaseObject> getChanges() {
        return mChanges;
    }

    public Collection<? extends BaseObject> getAdditions() {
        return mAdditions;
    }

    public Collection<? extends BaseObject> getRemovals() {
        return mRemovals;
    }
}