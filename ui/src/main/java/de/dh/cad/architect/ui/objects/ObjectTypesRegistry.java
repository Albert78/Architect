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

import java.util.HashMap;
import java.util.Map;

import de.dh.cad.architect.model.objects.Anchor;
import de.dh.cad.architect.model.objects.BaseObject;
import de.dh.cad.architect.model.objects.Ceiling;
import de.dh.cad.architect.model.objects.Covering;
import de.dh.cad.architect.model.objects.Dimensioning;
import de.dh.cad.architect.model.objects.Floor;
import de.dh.cad.architect.model.objects.GuideLine;
import de.dh.cad.architect.model.objects.ObjectsGroup;
import de.dh.cad.architect.model.objects.SupportObject;
import de.dh.cad.architect.model.objects.Wall;
import de.dh.cad.architect.model.objects.WallHole;

public class ObjectTypesRegistry {
    protected static Map<Class<? extends BaseObject>, AbstractObjectUIRepresentation> UI_REPRESENTATIONS = new HashMap<>();

    static {
        UI_REPRESENTATIONS.put(Anchor.class, new AnchorUIRepresentation());
        UI_REPRESENTATIONS.put(Dimensioning.class, new DimensioningUIRepresentation());
        UI_REPRESENTATIONS.put(Floor.class, new FloorUIRepresentation());
        UI_REPRESENTATIONS.put(Wall.class, new WallUIRepresentation());
        UI_REPRESENTATIONS.put(WallHole.class, new WallHoleUIRepresentation());
        UI_REPRESENTATIONS.put(Ceiling.class, new CeilingUIRepresentation());
        UI_REPRESENTATIONS.put(Covering.class, new CoveringUIRepresentation());
        UI_REPRESENTATIONS.put(SupportObject.class, new SupportObjectUIRepresentation());
        UI_REPRESENTATIONS.put(GuideLine.class, new GuideLineUIRepresentation());
        UI_REPRESENTATIONS.put(ObjectsGroup.class, new ObjectsGroupUIRepresentation());
    }

    public static <T extends BaseObject> AbstractObjectUIRepresentation getUIRepresentation(Class<T> modelObjectClass) {
        return UI_REPRESENTATIONS.get(modelObjectClass);
    }
}
