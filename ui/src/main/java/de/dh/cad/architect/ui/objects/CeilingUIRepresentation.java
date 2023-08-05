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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.model.objects.Anchor;
import de.dh.cad.architect.model.objects.BaseObject;
import de.dh.cad.architect.model.objects.Ceiling;
import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.controller.UiController;
import de.dh.cad.architect.ui.properties.ConstantUiProperty;
import de.dh.cad.architect.ui.properties.UiProperty;
import de.dh.cad.architect.ui.properties.UiProperty.PropertyType;
import de.dh.cad.architect.ui.view.construction.Abstract2DView;
import de.dh.cad.architect.ui.view.threed.Abstract3DView;

public class CeilingUIRepresentation extends BaseObjectUIRepresentation {
    public static final String KEY_PROPERTY_NUM_CORNERS = "num-corners";
    public static final String KEY_PROPERTY_HEIGHT = "height";

    public CeilingUIRepresentation() {
        super(new CeilingReconciler());
    }

    @Override
    public String getTypeName(Cardinality cardinality) {
        return cardinality == Cardinality.Singular ? Strings.OBJECT_TYPE_NAME_CEILING_S : Strings.OBJECT_TYPE_NAME_CEILING_P;
    }

    @Override
    protected void addProperties(Map<String, Collection<UiProperty<?>>> result, BaseObject bo, UiController uiController) {
        super.addProperties(result, bo, uiController);
        Ceiling ceiling = (Ceiling) bo;
        Collection<UiProperty<?>> properties = result.computeIfAbsent(getTypeName(Cardinality.Singular), cat -> new ArrayList<>());
        properties.addAll(Arrays.<UiProperty<?>>asList(
            new ConstantUiProperty<>(ceiling, KEY_PROPERTY_NUM_CORNERS, Strings.CEILING_PROPERTIES_NUM_CORNERS, PropertyType.Integer, ceiling.getEdgeHandleAnchors().size()),
            new ConstantUiProperty<>(ceiling, KEY_PROPERTY_HEIGHT, Strings.CEILING_PROPERTIES_HEIGHT, PropertyType.String, calculateHeightStr(ceiling))
        ));
    }

    protected String calculateHeightStr(Ceiling ceiling) {
        Length minZ = null;
        Length maxZ = null;
        for (Anchor anchor : ceiling.getEdgePositionAnchors()) {
            Length currentZ = anchor.requirePosition3D().getZ();
            if (minZ == null) {
                minZ = currentZ;
            } else if (minZ.gt(currentZ)) {
                minZ = currentZ;
            }
            if (maxZ == null) {
                maxZ = currentZ;
            } else if (maxZ.lt(currentZ)) {
                maxZ = currentZ;
            }
        }
        if (minZ == null || maxZ == null) {
            return "-";
        }
        String minZStr = minZ.toHumanReadableString(minZ.getBestUnitForDisplay(), 2, true);
        if (minZ.eq(maxZ)) {
            return minZStr;
        }
        return MessageFormat.format(Strings.CEILING_PROPERTIES_HEIGHT_FROM_TO, minZStr, maxZ.toHumanReadableString(maxZ.getBestUnitForDisplay(), 2, true));
    }

    @Override
    public Abstract2DRepresentation create2DRepresentation(BaseObject modelObject, Abstract2DView parentView) {
        return new CeilingConstructionRepresentation((Ceiling) modelObject, parentView);
    }

    @Override
    public Abstract3DRepresentation create3DRepresentation(BaseObject modelObject, Abstract3DView parentView) {
        return new Ceiling3DRepresentation((Ceiling) modelObject, parentView);
    }
}
