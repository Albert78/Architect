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
import de.dh.cad.architect.model.objects.BaseObject;
import de.dh.cad.architect.model.objects.Wall;
import de.dh.cad.architect.model.wallmodel.AdaptedModelAnchor;
import de.dh.cad.architect.model.wallmodel.WallAnchorPositions;
import de.dh.cad.architect.model.wallmodel.WallBevelType;
import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.controller.UiController;
import de.dh.cad.architect.ui.properties.ConstantUiProperty;
import de.dh.cad.architect.ui.properties.UiProperty;
import de.dh.cad.architect.ui.properties.UiProperty.PropertyType;
import de.dh.cad.architect.ui.view.ObjectReconcileOperation;
import de.dh.cad.architect.ui.view.construction.Abstract2DView;
import de.dh.cad.architect.ui.view.construction.ConstructionView;
import de.dh.cad.architect.ui.view.threed.Abstract3DView;

public class WallUIRepresentation extends BaseObjectUIRepresentation {
    public static final String KEY_PROPERTY_THICKNESS = "thickness";
    public static final String KEY_PROPERTY_BEVEL_TYPE_A = "beveltype-A";
    public static final String KEY_PROPERTY_BEVEL_TYPE_B = "beveltype-B";
    public static final String KEY_PROPERTY_WALL_HEIGHT_A = "wall-height-A";
    public static final String KEY_PROPERTY_WALL_HEIGHT_B = "wall-height-B";
    public static final String KEY_PROPERTY_WALL_LENGTH_BASE = "wall-length-base";
    public static final String KEY_PROPERTY_WALL_LENGTH_SIDE_1 = "wall-length-side-1";
    public static final String KEY_PROPERTY_WALL_LENGTH_SIDE_2 = "wall-length-side-2";

    public WallUIRepresentation() {
        super(new WallReconciler());
    }

    @Override
    public String getTypeName(Cardinality cardinality) {
        return cardinality == Cardinality.Singular ? Strings.OBJECT_TYPE_NAME_WALL_S : Strings.OBJECT_TYPE_NAME_WALL_P;
    }

    @Override
    protected void addProperties(Map<String, Collection<UiProperty<?>>> result, BaseObject bo, UiController uiController) {
        super.addProperties(result, bo, uiController);
        Wall wall = (Wall) bo;
        Collection<UiProperty<?>> properties = result.computeIfAbsent(getTypeName(Cardinality.Singular), cat -> new ArrayList<>());
        properties.addAll(Arrays.<UiProperty<?>>asList(
                new UiProperty<Length>(wall, KEY_PROPERTY_THICKNESS, Strings.WALL_PROPERTIES_THICKNESS, PropertyType.Length, true) {
                    @Override
                    public Length getValue() {
                        return wall.getThickness();
                    }

                    @Override
                    public void setValue(Object value) {
                        Length thickness = (Length) value;

                        List<IModelChange> changeTrace = new ArrayList<>();
                        wall.setThickness(thickness, changeTrace);
                        WallReconciler.doUpdateWallAnchors(wall, uiController, changeTrace);
                        uiController.notifyChange(changeTrace, Strings.WALL_SET_PROPERTY_CHANGE);
                    }
                },
                new UiProperty<WallBevelType>(wall, KEY_PROPERTY_BEVEL_TYPE_A, Strings.WALL_PROPERTIES_BEVEL_TYPE_A, PropertyType.WallBevelType, true) {
                    @Override
                    public WallBevelType getValue() {
                        return WallAnchorPositions.getWallBevelTypeOfAnchorDock(new AdaptedModelAnchor(wall.getAnchorWallHandleA())).orElse(null);
                    }

                    @Override
                    public void setValue(Object value) {
                        WallBevelType bevelType = (WallBevelType) value;
                        uiController.setWallBevelTypeOfAnchorDock(wall.getAnchorWallHandleA(), bevelType);
                    }
                },
                new UiProperty<WallBevelType>(wall, KEY_PROPERTY_BEVEL_TYPE_B, Strings.WALL_PROPERTIES_BEVEL_TYPE_B, PropertyType.WallBevelType, true) {
                    @Override
                    public WallBevelType getValue() {
                        return WallAnchorPositions.getWallBevelTypeOfAnchorDock(new AdaptedModelAnchor(wall.getAnchorWallHandleB())).orElse(null);
                    }

                    @Override
                    public void setValue(Object value) {
                        WallBevelType bevelType = (WallBevelType) value;
                        uiController.setWallBevelTypeOfAnchorDock(wall.getAnchorWallHandleB(), bevelType);
                    }
                },
                new UiProperty<Length>(wall, KEY_PROPERTY_WALL_HEIGHT_A, Strings.WALL_PROPERTIES_HEIGHT_A, PropertyType.Length, true) {
                    @Override
                    public Length getValue() {
                        return wall.getHeightA();
                    }

                    @Override
                    public void setValue(Object value) {
                        Length height = (Length) value;
                        List<IModelChange> changeTrace = new ArrayList<>();
                        wall.setHeightA(height, changeTrace);
                        ObjectReconcileOperation omo = new ObjectReconcileOperation(Strings.WALL_PROPERTIES_OPERATION_HAME_SET_HEIGHT_A);
                        omo.tryAddObjectToProcess(wall);
                        uiController.doReconcileObjects(omo, changeTrace);
                        uiController.notifyChange(changeTrace, Strings.WALL_SET_PROPERTY_CHANGE);
                    }
                },
                new UiProperty<Length>(wall, KEY_PROPERTY_WALL_HEIGHT_B, Strings.WALL_PROPERTIES_HEIGHT_B, PropertyType.Length, true) {
                    @Override
                    public Length getValue() {
                        return wall.getHeightB();
                    }

                    @Override
                    public void setValue(Object value) {
                        Length height = (Length) value;
                        List<IModelChange> changeTrace = new ArrayList<>();
                        wall.setHeightB(height, changeTrace);
                        ObjectReconcileOperation omo = new ObjectReconcileOperation(Strings.WALL_PROPERTIES_OPERATION_HAME_SET_HEIGHT_B);
                        omo.tryAddObjectToProcess(wall);
                        uiController.doReconcileObjects(omo, changeTrace);
                        uiController.notifyChange(changeTrace, Strings.WALL_SET_PROPERTY_CHANGE);
                    }
                },
                new ConstantUiProperty<>(wall, KEY_PROPERTY_WALL_LENGTH_BASE, Strings.WALL_PROPERTIES_BASE_LENGTH, PropertyType.Length, wall.calculateBaseLength()),
                new ConstantUiProperty<>(wall, KEY_PROPERTY_WALL_LENGTH_SIDE_1, Strings.WALL_PROPERTIES_LENGTH_SIDE_1, PropertyType.Length, wall.calculateLengthSide1()),
                new ConstantUiProperty<>(wall, KEY_PROPERTY_WALL_LENGTH_SIDE_2, Strings.WALL_PROPERTIES_LENGTH_SIDE_2, PropertyType.Length, wall.calculateLengthSide2())
                ));
    }

    @Override
    public Abstract2DRepresentation create2DRepresentation(BaseObject modelObject, Abstract2DView parentView) {
        // Hack: Cast parent view to construction view should be removed when we have multiple 2D views
        return new WallConstructionRepresentation((Wall) modelObject, (ConstructionView) parentView);
    }

    @Override
    public Abstract3DRepresentation create3DRepresentation(BaseObject modelObject, Abstract3DView parentView) {
        return new Wall3DRepresentation((Wall) modelObject, parentView);
    }
}
