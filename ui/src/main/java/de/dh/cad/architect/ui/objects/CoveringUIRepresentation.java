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

import de.dh.cad.architect.model.objects.BaseObject;
import de.dh.cad.architect.model.objects.Covering;
import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.view.DefaultObjectReconciler;
import de.dh.cad.architect.ui.view.construction.Abstract2DView;
import de.dh.cad.architect.ui.view.threed.Abstract3DView;

public class CoveringUIRepresentation extends BaseObjectUIRepresentation {
    public CoveringUIRepresentation() {
        super(new DefaultObjectReconciler());
    }

    @Override
    public String getTypeName(Cardinality cardinality) {
        return cardinality == Cardinality.Singular ? Strings.OBJECT_TYPE_NAME_COVERING_S : Strings.OBJECT_TYPE_NAME_COVERING_P;
    }

    @Override
    public Abstract2DRepresentation create2DRepresentation(BaseObject modelObject, Abstract2DView parentView) {
        return new CoveringConstructionRepresentation((Covering) modelObject, parentView);
    }

    @Override
    public Abstract3DRepresentation create3DRepresentation(BaseObject modelObject, Abstract3DView parentView) {
        return new Covering3DRepresentation((Covering) modelObject, parentView);
    }
}
