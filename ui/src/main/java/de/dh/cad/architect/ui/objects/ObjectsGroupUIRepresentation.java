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
package de.dh.cad.architect.ui.objects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import de.dh.cad.architect.model.objects.BaseObject;
import de.dh.cad.architect.model.objects.ObjectsGroup;
import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.controller.UiController;
import de.dh.cad.architect.ui.properties.ConstantUiProperty;
import de.dh.cad.architect.ui.properties.UiProperty;
import de.dh.cad.architect.ui.properties.UiProperty.PropertyType;
import de.dh.cad.architect.ui.view.DefaultObjectReconciler;
import de.dh.cad.architect.ui.view.construction.Abstract2DView;
import de.dh.cad.architect.ui.view.threed.Abstract3DView;

public class ObjectsGroupUIRepresentation extends BaseObjectUIRepresentation {
    public static final String KEY_PROPERTY_NUM_OBJECTS = "num-objects";

    public ObjectsGroupUIRepresentation() {
        super(new DefaultObjectReconciler());
    }

    @Override
    public String getTypeName(Cardinality cardinality) {
        return cardinality == Cardinality.Singular ? Strings.OBJECT_TYPE_NAME_OBJECTS_GROUP_S : Strings.OBJECT_TYPE_NAME_OBJECTS_GROUP_P;
    }

    @Override
    protected void addProperties(Map<String, Collection<UiProperty<?>>> result, BaseObject bo, UiController uiController) {
        super.addProperties(result, bo, uiController);
        ObjectsGroup group = (ObjectsGroup) bo;
        Collection<UiProperty<?>> properties = result.computeIfAbsent(getTypeName(Cardinality.Singular), cat -> new ArrayList<>());
        properties.addAll(Arrays.<UiProperty<?>>asList(
                new ConstantUiProperty<>(group, KEY_PROPERTY_NUM_OBJECTS, Strings.OBJECTS_GROUP_PROPERTIES_NUM_OBJECTS, PropertyType.Integer, group.getGroupedObjectIds().size())
            ));
    }

    @Override
    public Abstract2DRepresentation create2DRepresentation(BaseObject modelObject, Abstract2DView parentView) {
        return null;
    }

    @Override
    public Abstract3DRepresentation create3DRepresentation(BaseObject modelObject, Abstract3DView parentView) {
        return null;
    }
}
