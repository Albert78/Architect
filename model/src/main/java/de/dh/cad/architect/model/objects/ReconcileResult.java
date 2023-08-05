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

import java.util.Collection;
import java.util.Collections;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

public class ReconcileResult {
    protected final Collection<Anchor> mDependentAnchors;
    protected final MultiValuedMap<BaseAnchoredObject, ObjectHealReason> mHealObjects = new ArrayListValuedHashMap<>();

    public ReconcileResult(Collection<Anchor> dependentAnchors) {
        mDependentAnchors = dependentAnchors;
    }

    public ReconcileResult(Collection<Anchor> dependentAnchors, MultiValuedMap<BaseAnchoredObject, ObjectHealReason> healObjects) {
        this(dependentAnchors);
        if (healObjects != null) {
            mHealObjects.putAll(healObjects);
        }
    }

    public static ReconcileResult empty() {
        return new ReconcileResult(Collections.emptyList());
    }

    public Collection<Anchor> getDependentAnchors() {
        return mDependentAnchors;
    }

    public MultiValuedMap<BaseAnchoredObject, ObjectHealReason> getHealObjects() {
        return mHealObjects;
    }
}
