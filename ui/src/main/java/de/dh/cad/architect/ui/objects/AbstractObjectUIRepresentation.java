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
import de.dh.cad.architect.ui.controller.UiController;
import de.dh.cad.architect.ui.view.IObjectReconciler;
import de.dh.cad.architect.ui.view.construction.Abstract2DView;
import de.dh.cad.architect.ui.view.threed.Abstract3DView;

public abstract class AbstractObjectUIRepresentation {
    public static enum Cardinality {
        Singular,
        Plural
    }

    protected final IObjectReconciler mReconciler;

    protected AbstractObjectUIRepresentation(IObjectReconciler reconciler) {
        mReconciler = reconciler;
    }

    public IObjectReconciler getReconciler() {
        return mReconciler;
    }

    public abstract String getTypeName(Cardinality cardinality);
    public abstract ObjectProperties getProperties(BaseObject modelObject, UiController uiController);
    public abstract Abstract2DRepresentation create2DRepresentation(BaseObject modelObject, Abstract2DView parentView);
    public abstract Abstract3DRepresentation create3DRepresentation(BaseObject modelObject, Abstract3DView parentView);
}
