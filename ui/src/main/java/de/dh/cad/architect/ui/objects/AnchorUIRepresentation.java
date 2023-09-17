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

import de.dh.cad.architect.model.coords.IPosition;
import de.dh.cad.architect.model.objects.Anchor;
import de.dh.cad.architect.model.objects.BaseObject;
import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.controller.UiController;
import de.dh.cad.architect.ui.properties.ConstantUiProperty;
import de.dh.cad.architect.ui.properties.UiProperty;
import de.dh.cad.architect.ui.properties.UiProperty.PropertyType;
import de.dh.cad.architect.ui.view.DefaultObjectReconciler;
import de.dh.cad.architect.ui.view.construction.Abstract2DView;
import de.dh.cad.architect.ui.view.threed.Abstract3DView;

public class AnchorUIRepresentation extends BaseObjectUIRepresentation {
    public static final String KEY_PROPERTY_POSITION = "position";
    public static final String KEY_PROPERTY_DOCKED = "is-docked";
    public static final String KEY_PROPERTY_IS_HANDLE = "is-handle";
    public static final String KEY_PROPERTY_OWNER = "owner";
    public static final String KEY_PROPERTY_ANCHOR_TYPE = "anchor-type";
    public static final String KEY_PROPERTY_FREE_COORDINATES = "open-coordinates";
    public static final String KEY_PROPERTY_SIGNIFICANT_COORDINATES = "significant-coordinates";

    public AnchorUIRepresentation() {
        super(new DefaultObjectReconciler());
    }

    @Override
    public String getTypeName(Cardinality cardinality) {
        return cardinality == Cardinality.Singular ? Strings.OBJECT_TYPE_NAME_ANCHOR_S : Strings.OBJECT_TYPE_NAME_ANCHOR_P;
    }

    @Override
    protected void addProperties(Map<String, Collection<UiProperty<?>>> result, BaseObject bo, UiController uiController) {
        super.addProperties(result, bo, uiController);
        Anchor anchor = (Anchor) bo;
        Collection<UiProperty<?>> properties = result.computeIfAbsent(getTypeName(Cardinality.Singular), cat -> new ArrayList<>());
        properties.addAll(Arrays.<UiProperty<?>>asList(
            new UiProperty<IPosition>(anchor, KEY_PROPERTY_POSITION, Strings.ANCHOR_PROPERTIES_POSITION, PropertyType.IPosition, anchor.isHandle() && anchor.getDockMaster().isEmpty()) {
                @Override
                public IPosition getValue() {
                    return anchor.getPosition();
                }

                @Override
                public void setValue(Object value) {
                    uiController.setHandleAnchorPosition(anchor, (IPosition) value, false);
                }
            },
            new ConstantUiProperty<>(anchor, KEY_PROPERTY_DOCKED, Strings.ANCHOR_PROPERTIES_DOCKED, PropertyType.YesNo, anchor.getDockMaster().isPresent()),
            new ConstantUiProperty<>(anchor, KEY_PROPERTY_IS_HANDLE, Strings.ANCHOR_PROPERTIES_IS_HANDLE, PropertyType.YesNo, anchor.isHandle()),
            new ConstantUiProperty<>(anchor, KEY_PROPERTY_OWNER, Strings.ANCHOR_PROPERTIES_OWNER, PropertyType.String, getObjName(anchor.getAnchorOwner())),
            new ConstantUiProperty<>(anchor, KEY_PROPERTY_ANCHOR_TYPE, Strings.ANCHOR_PROPERTIES_ANCHOR_TYPE, PropertyType.String, anchor.getAnchorType())
        ));
    }

    @Override
    public Abstract2DRepresentation create2DRepresentation(BaseObject modelObject, Abstract2DView parentView) {
        Anchor anchor = (Anchor) modelObject;
        return new AnchorConstructionRepresentation(anchor, parentView);
    }

    @Override
    public Abstract3DRepresentation create3DRepresentation(BaseObject modelObject, Abstract3DView parentView) {
        Anchor anchor = (Anchor) modelObject;
        return new Anchor3DRepresentation(anchor, parentView);
    }
}
