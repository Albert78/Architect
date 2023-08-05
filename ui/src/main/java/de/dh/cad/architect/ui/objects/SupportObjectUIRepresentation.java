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
import java.util.List;
import java.util.Map;

import de.dh.cad.architect.model.changes.IModelChange;
import de.dh.cad.architect.model.coords.Dimensions2D;
import de.dh.cad.architect.model.coords.IPosition;
import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.model.objects.BaseObject;
import de.dh.cad.architect.model.objects.SupportObject;
import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.controller.UiController;
import de.dh.cad.architect.ui.properties.ConstantUiProperty;
import de.dh.cad.architect.ui.properties.UiProperty;
import de.dh.cad.architect.ui.properties.UiProperty.PropertyType;
import de.dh.cad.architect.ui.view.DefaultObjectReconciler;
import de.dh.cad.architect.ui.view.construction.Abstract2DView;
import de.dh.cad.architect.ui.view.threed.Abstract3DView;

public class SupportObjectUIRepresentation extends BaseObjectUIRepresentation {
    public static final String KEY_PROPERTY_MODEL = "model";
    public static final String KEY_PROPERTY_POSITION = "position";
    public static final String KEY_PROPERTY_SIZE = "size";
    public static final String KEY_PROPERTY_ROTATION = "rotation";
    public static final String KEY_PROPERTY_HEIGHT = "height";
    public static final String KEY_PROPERTY_ELEVATION = "elevation";

    public SupportObjectUIRepresentation() {
        super(new DefaultObjectReconciler());
    }


    @Override
    public String getTypeName(Cardinality cardinality) {
        return cardinality == Cardinality.Singular ? Strings.OBJECT_TYPE_NAME_SUPPORT_OBJECT_S : Strings.OBJECT_TYPE_NAME_SUPPORT_OBJECT_P;
    }

    @Override
    protected void addProperties(Map<String, Collection<UiProperty<?>>> result, BaseObject bo, UiController uiController) {
        super.addProperties(result, bo, uiController);
        SupportObject supportObject = (SupportObject) bo;
        Collection<UiProperty<?>> properties = result.computeIfAbsent(getTypeName(Cardinality.Singular), cat -> new ArrayList<>());
        properties.addAll(Arrays.<UiProperty<?>>asList(
            new ConstantUiProperty<>(bo, KEY_PROPERTY_MODEL, Strings.SUPPORT_OBJECT_PROPERTIES_MODEL, PropertyType.String, supportObject.getSupportObjectDescriptorRef()),
            new UiProperty<IPosition>(bo, KEY_PROPERTY_POSITION, Strings.SUPPORT_OBJECT_PROPERTIES_POSITION, PropertyType.IPosition, true) {
                @Override
                public IPosition getValue() {
                    return supportObject.getHandleAnchor().getPosition();
                }

                @Override
                public void setValue(Object value) {
                    IPosition position = (IPosition) value;
                    uiController.setHandleAnchorPosition(supportObject.getHandleAnchor(), position.projectionXY(), false);
                }
            },
            new UiProperty<Dimensions2D>(bo, KEY_PROPERTY_SIZE, Strings.SUPPORT_OBJECT_PROPERTIES_SIZE, PropertyType.Dimensions2DXY, true) {
                @Override
                public Dimensions2D getValue() {
                    return supportObject.getSize();
                }

                @Override
                public void setValue(Object value) {
                    List<IModelChange> changeTrace = new ArrayList<>();
                    Dimensions2D size = (Dimensions2D) value;
                    supportObject.setSize(size, changeTrace);
                    uiController.notifyChange(changeTrace, Strings.SUPPORT_OBJECT_SET_PROPERTY_CHANGE);
                }
            },
            new UiProperty<Integer>(bo, KEY_PROPERTY_ROTATION, Strings.SUPPORT_OBJECT_PROPERTIES_ROTATION_DEGREES, PropertyType.Integer, true) {
                @Override
                public Integer getValue() {
                    return Float.valueOf(supportObject.getRotationDeg()).intValue();
                }

                @Override
                public void setValue(Object value) {
                    List<IModelChange> changeTrace = new ArrayList<>();
                    Integer rotation = (Integer) value;
                    supportObject.setRotationDeg(rotation, changeTrace);
                    uiController.notifyChange(changeTrace, Strings.SUPPORT_OBJECT_SET_PROPERTY_CHANGE);
                }
            },
            new UiProperty<Length>(bo, KEY_PROPERTY_HEIGHT, Strings.SUPPORT_OBJECT_PROPERTIES_HEIGHT, PropertyType.Length, true) {
                @Override
                public Length getValue() {
                    return supportObject.getHeight();
                }

                @Override
                public void setValue(Object value) {
                    List<IModelChange> changeTrace = new ArrayList<>();
                    Length height = (Length) value;
                    supportObject.setHeight(height, changeTrace);
                    uiController.notifyChange(changeTrace, Strings.SUPPORT_OBJECT_SET_PROPERTY_CHANGE);
                }
            },
            new UiProperty<Length>(bo, KEY_PROPERTY_ELEVATION, Strings.SUPPORT_OBJECT_PROPERTIES_ELEVATION, PropertyType.Length, true) {
                @Override
                public Length getValue() {
                    return supportObject.getElevation();
                }

                @Override
                public void setValue(Object value) {
                    List<IModelChange> changeTrace = new ArrayList<>();
                    Length elevation = (Length) value;
                    supportObject.setElevation(elevation, changeTrace);
                    uiController.notifyChange(changeTrace, Strings.SUPPORT_OBJECT_SET_PROPERTY_CHANGE);
                }
            }
        ));
    }

    @Override
    public Abstract2DRepresentation create2DRepresentation(BaseObject modelObject, Abstract2DView parentView) {
        return new SupportObjectConstructionRepresentation((SupportObject) modelObject, parentView);
    }

    @Override
    public Abstract3DRepresentation create3DRepresentation(BaseObject modelObject, Abstract3DView parentView) {
        return new SupportObject3DRepresentation((SupportObject) modelObject, parentView);
    }
}
