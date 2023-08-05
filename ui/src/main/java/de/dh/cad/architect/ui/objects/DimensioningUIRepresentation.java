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
import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.model.coords.LengthUnit;
import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.model.coords.Vector2D;
import de.dh.cad.architect.model.objects.Anchor;
import de.dh.cad.architect.model.objects.BaseObject;
import de.dh.cad.architect.model.objects.Dimensioning;
import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.controller.UiController;
import de.dh.cad.architect.ui.properties.UiProperty;
import de.dh.cad.architect.ui.properties.UiProperty.PropertyType;
import de.dh.cad.architect.ui.view.DefaultObjectReconciler;
import de.dh.cad.architect.ui.view.construction.Abstract2DView;
import de.dh.cad.architect.ui.view.threed.Abstract3DView;

public class DimensioningUIRepresentation extends BaseObjectUIRepresentation {
    public static final String KEY_PROPERTY_LENGTH = "length";
    public static final String KEY_PROPERTY_LABEL = "label";

    public DimensioningUIRepresentation() {
        super(new DefaultObjectReconciler());
    }

    @Override
    public String getTypeName(Cardinality cardinality) {
        return cardinality == Cardinality.Singular ? Strings.OBJECT_TYPE_NAME_DIMENSIONING_S : Strings.OBJECT_TYPE_NAME_DIMENSIONING_P;
    }

    @Override
    protected void addProperties(Map<String, Collection<UiProperty<?>>> result, BaseObject bo, UiController uiController) {
        super.addProperties(result, bo, uiController);
        Dimensioning dimensioning = (Dimensioning) bo;
        Anchor anchor1 = dimensioning.getAnchor1();
        Anchor anchor2 = dimensioning.getAnchor2();
        Collection<UiProperty<?>> properties = result.computeIfAbsent(getTypeName(Cardinality.Singular), cat -> new ArrayList<>());
        properties.addAll(Arrays.<UiProperty<?>>asList(
                new UiProperty<Length>(bo, KEY_PROPERTY_LENGTH, Strings.DIMENSIONING_PROPERTIES_LENGTH, PropertyType.Length, anchor2.isHandle()) {
                    protected Length calculateLength() {
                        Position2D p1 = dimensioning.getAnchor1().getPosition().projectionXY();
                        Position2D p2 = dimensioning.getAnchor2().getPosition().projectionXY();
                        return p1.distance(p2);
                    }

                    @Override
                    public Length getValue() {
                        return calculateLength();
                    }

                    @Override
                    public void setValue(Object value) {
                        Length newLength = (Length) value;
                        Position2D p1 = anchor1.getPosition().projectionXY();
                        Position2D p2 = anchor2.getPosition().projectionXY();
                        Vector2D v = p2.minus(p1);
                        Position2D newP2 = p1.plus(v.toUnitVector(LengthUnit.MM).times(newLength.inMM()));
                        uiController.setHandleAnchorPosition(anchor2, newP2.upscale(), false);
                    }
                },
                new UiProperty<String>(bo, KEY_PROPERTY_LABEL, Strings.DIMENSIONING_PROPERTIES_LABEL, PropertyType.String, true) {
                    @Override
                    public String getValue() {
                        return dimensioning.getLabel();
                    }

                    @Override
                    public void setValue(Object value) {
                        List<IModelChange> changeTrace = new ArrayList<>();
                        dimensioning.setLabel((String) value, changeTrace);
                        uiController.notifyChange(changeTrace, Strings.DIMENSIONING_SET_PROPERTY_CHANGE);
                    }
                }
        ));
    }

    @Override
    public Abstract2DRepresentation create2DRepresentation(BaseObject modelObject, Abstract2DView parentView) {
        return new DimensioningConstructionRepresentation((Dimensioning) modelObject, parentView);
    }

    @Override
    public Abstract3DRepresentation create3DRepresentation(BaseObject modelObject, Abstract3DView parentView) {
        // TODO
        return null;
    }
}
