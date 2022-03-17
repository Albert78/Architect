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
package de.dh.cad.architect.ui.objecttree;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import de.dh.cad.architect.model.objects.BaseObject;
import de.dh.cad.architect.ui.objects.AbstractObjectUIRepresentation.Cardinality;
import de.dh.cad.architect.ui.objects.BaseObjectUIRepresentation;

public class BaseObjectTreeItemData implements IObjectTreeItemData {
    protected final BaseObject mObject;
    protected final boolean mShadowEntry;

    public BaseObjectTreeItemData(BaseObject object, boolean shadowEntry) {
        mObject = object;
        mShadowEntry = shadowEntry;
    }

    @Override
    public Optional<Boolean> isVisible() {
        return Optional.of(!mObject.isHidden());
    }

    @Override
    public Collection<BaseObject> getObjects() {
        return Arrays.asList(mObject);
    }

    @Override
    public boolean isShadowEntry() {
        return mShadowEntry;
    }

    @Override
    public String getTitle() {
        return BaseObjectUIRepresentation.getObjectTypeName(mObject, Cardinality.Singular);
    }

    @Override
    public String getId() {
        return mObject.getId();
    }

    @Override
    public BaseObject getObject() {
        return mObject;
    }
}
