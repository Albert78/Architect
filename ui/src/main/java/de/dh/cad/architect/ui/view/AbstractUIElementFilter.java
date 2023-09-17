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
package de.dh.cad.architect.ui.view;

import java.util.Optional;

import de.dh.cad.architect.ui.objects.IModelBasedObject;

public abstract class AbstractUIElementFilter<TRepr extends IModelBasedObject> {
    /**
     * Gets the opacity of the given UI element, if it's different from default.
     */
    protected Optional<Double> getUIElementOpacity(TRepr repr) {
        return Optional.empty();
    }

    /**
     * Gets the information if the given UI element is visible.
     */
    public boolean isUIElementVisible(TRepr repr) {
        return true;
    }

    public boolean isUIElementMouseTransparent(TRepr repr) {
        return false;
    }

    public abstract void configure(TRepr repr);

    public abstract void unconfigure(TRepr repr);
}
