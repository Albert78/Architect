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
package de.dh.cad.architect.ui.objects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import de.dh.cad.architect.model.ChangeSet;
import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.model.objects.BaseObject;
import de.dh.cad.architect.model.objects.Floor;
import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.controller.UiController;
import de.dh.cad.architect.ui.properties.ConstantUiProperty;
import de.dh.cad.architect.ui.properties.UiProperty;
import de.dh.cad.architect.ui.properties.UiProperty.PropertyType;
import de.dh.cad.architect.ui.view.ObjectReconcileOperation;
import de.dh.cad.architect.ui.view.construction.Abstract2DView;
import de.dh.cad.architect.ui.view.threed.Abstract3DView;

public class FloorUIProperties extends BaseObjectUIRepresentation {
    public static final String KEY_PROPERTY_LEVEL = "level";
    public static final String KEY_PROPERTY_HEIGHT = "height";
    public static final String KEY_PROPERTY_THICKNESS = "thickness";
    public static final String KEY_PROPERTY_AREA = "area";

    public FloorUIProperties() {
        super(new FloorReconciler());
    }

    @Override
    public String getTypeName(Cardinality cardinality) {
        return cardinality == Cardinality.Singular ? Strings.OBJECT_TYPE_NAME_FLOOR_S : Strings.OBJECT_TYPE_NAME_FLOOR_P;
    }

    @Override
    protected void addProperties(Map<String, Collection<UiProperty<?>>> result, BaseObject bo, UiController uiController) {
        super.addProperties(result, bo, uiController);
        Floor floor = (Floor) bo;
        Collection<UiProperty<?>> properties = result.computeIfAbsent("Ebene", cat -> new ArrayList<>());
        properties.addAll(Arrays.<UiProperty<?>>asList(
                new UiProperty<Integer>(floor, KEY_PROPERTY_LEVEL, "Etage", PropertyType.Integer, true) {
                    @Override
                    public Integer getValue() {
                        return floor.getLevel();
                    }

                    @Override
                    public void setValue(Object value) {
                        floor.setLevel((Integer) value);
                        uiController.notifyObjectsChanged(floor);
                    }

                },
                new UiProperty<Length>(floor, KEY_PROPERTY_HEIGHT, "Höhe", PropertyType.Length, true) {
                    @Override
                    public Length getValue() {
                        return floor.getHeight();
                    }

                    @Override
                    public void setValue(Object value) {
                        ChangeSet changeSet = new ChangeSet();
                        floor.setHeight((Length) value);
                        changeSet.changed(floor);
                        ObjectReconcileOperation omo = new ObjectReconcileOperation("Fußbodenhöhe setzen");
                        omo.tryAddObjectToProcess(floor);
                        uiController.doReconcileObjects(omo, changeSet);
                        uiController.notifyChanges(changeSet);
                    }
                },
                new UiProperty<Length>(floor, KEY_PROPERTY_THICKNESS, "Dicke", PropertyType.Length, true) {
                    @Override
                    public Length getValue() {
                        return floor.getThickness();
                    }

                    @Override
                    public void setValue(Object value) {
                        ChangeSet changeSet = new ChangeSet();
                        floor.setThickness((Length) value);
                        changeSet.changed(floor);
                        ObjectReconcileOperation omo = new ObjectReconcileOperation("Fußbodendicke setzen");
                        omo.tryAddObjectToProcess(floor);
                        uiController.doReconcileObjects(omo, changeSet);
                        uiController.notifyChanges(changeSet);
                    }
                },
                new ConstantUiProperty<>(floor, KEY_PROPERTY_AREA, "Fläche", PropertyType.String, floor.getAreaString())
        ));
    }

    @Override
    public Abstract2DRepresentation create2DRepresentation(BaseObject modelObject, Abstract2DView parentView) {
        return new FloorConstructionRepresentation((Floor) modelObject, parentView);
    }

    @Override
    public Abstract3DRepresentation create3DRepresentation(BaseObject modelObject, Abstract3DView parentView) {
        return new Floor3DRepresentation((Floor) modelObject, parentView);
    }
}
