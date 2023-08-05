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
import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.model.objects.BaseObject;
import de.dh.cad.architect.model.objects.WallHole;
import de.dh.cad.architect.model.wallmodel.WallDockEnd;
import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.controller.UiController;
import de.dh.cad.architect.ui.properties.UiProperty;
import de.dh.cad.architect.ui.properties.UiProperty.PropertyType;
import de.dh.cad.architect.ui.view.construction.Abstract2DView;
import de.dh.cad.architect.ui.view.threed.Abstract3DView;

public class WallHoleUIRepresentation extends BaseObjectUIRepresentation {
    public static final String KEY_PROPERTY_DIMENSIONS = "dimensions";
    public static final String KEY_PROPERTY_PARAPET_HEIGHT = "parapet-height";
    public static final String KEY_PROPERTY_DISTANCE_FROM_WALL_END = "distance-from-wall-end";
    public static final String KEY_PROPERTY_WALL_DOCK_END = "wall-dock-end";

    public WallHoleUIRepresentation() {
        super(new WallHoleReconciler());
    }

    @Override
    public String getTypeName(Cardinality cardinality) {
        return cardinality == Cardinality.Singular ? Strings.OBJECT_TYPE_NAME_WALL_HOLE_S : Strings.OBJECT_TYPE_NAME_WALL_HOLE_P;
    }

    @Override
    protected void addProperties(Map<String, Collection<UiProperty<?>>> result, BaseObject bo, UiController uiController) {
        super.addProperties(result, bo, uiController);
        WallHole wallHole = (WallHole) bo;
        Collection<UiProperty<?>> properties = result.computeIfAbsent(getTypeName(Cardinality.Singular), cat -> new ArrayList<>());
        properties.addAll(Arrays.<UiProperty<?>>asList(
            new UiProperty<Dimensions2D>(bo, KEY_PROPERTY_DIMENSIONS, Strings.WALL_HOLE_PROPERTIES_DIMENSIONS, PropertyType.Dimensions2DXZ, true) {
                @Override
                public Dimensions2D getValue() {
                    return wallHole.getDimensions();
                }

                @Override
                public void setValue(Object value) {
                    Dimensions2D dimensions = (Dimensions2D) value;
                    // TODO: Limit dimensions by owner wall extends
                    List<IModelChange> changeTrace = new ArrayList<>();
                    wallHole.setDimensions(dimensions, changeTrace);
                    WallHoleReconciler.doUpdateWallHoleAnchors(wallHole, uiController, changeTrace);
                    uiController.notifyChange(changeTrace, Strings.WALL_HOLE_SET_PROPERTY_CHANGE);
                }
            },
            new UiProperty<Length>(bo, KEY_PROPERTY_PARAPET_HEIGHT, Strings.WALL_HOLE_PROPERTIES_PARAPET_HEIGHT, PropertyType.Length, true) {
                @Override
                public Length getValue() {
                    return wallHole.getParapetHeight();
                }

                @Override
                public void setValue(Object value) {
                    Length parapetHeight = (Length) value;
                    List<IModelChange> changeTrace = new ArrayList<>();
                    wallHole.setParapetHeight(parapetHeight, changeTrace);
                    WallHoleReconciler.doUpdateWallHoleAnchors(wallHole, uiController, changeTrace);
                    uiController.notifyChange(changeTrace, Strings.WALL_HOLE_SET_PROPERTY_CHANGE);
                }
            },
            new UiProperty<Length>(bo, KEY_PROPERTY_DISTANCE_FROM_WALL_END, Strings.WALL_HOLE_PROPERTIES_DISTANCE_FROM_WALL_END, PropertyType.Length, true) {
                @Override
                public Length getValue() {
                    return wallHole.getDistanceFromWallEnd();
                }

                @Override
                public void setValue(Object value) {
                    Length distanceFromWallEnd = (Length) value;
                    List<IModelChange> changeTrace = new ArrayList<>();
                    wallHole.setDistanceFromWallEnd(distanceFromWallEnd, changeTrace);
                    WallHoleReconciler.doUpdateWallHoleAnchors(wallHole, uiController, changeTrace);
                    uiController.notifyChange(changeTrace, Strings.WALL_HOLE_SET_PROPERTY_CHANGE);
                }
            },
            new UiProperty<WallDockEnd>(bo, KEY_PROPERTY_WALL_DOCK_END, Strings.WALL_HOLE_PROPERTIES_DOCK_END, PropertyType.WallDockEnd, true) {
                @Override
                public WallDockEnd getValue() {
                    return wallHole.getDockEnd();
                }

                @Override
                public void setValue(Object value) {
                    WallDockEnd dockEnd = (WallDockEnd) value;
                    if (wallHole.getDockEnd().equals(dockEnd)) {
                        return;
                    }
                    List<IModelChange> changeTrace = new ArrayList<>();
                    WallHoleReconciler.doSwapWallHoleDockEnd(wallHole, uiController, changeTrace);
                    uiController.notifyChange(changeTrace, Strings.WALL_HOLE_SET_PROPERTY_CHANGE);
                }
            }
        ));
    }

    @Override
    public Abstract2DRepresentation create2DRepresentation(BaseObject modelObject, Abstract2DView parentView) {
        return new WallHoleConstructionRepresentation((WallHole) modelObject, parentView);
    }

    @Override
    public Abstract3DRepresentation create3DRepresentation(BaseObject modelObject, Abstract3DView parentView) {
        return null;
    }
}
