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
package de.dh.cad.architect.ui.controller;

import java.util.Collection;

import javafx.scene.control.MenuItem;

public interface IObjectContextMenuProvider {
    /**
     * Gets a localized name for this menu. Might be shown in UI or not.
     */
    String getMenuName();

    /**
     * Gets a collection of menu items which apply for the objects which are currently in context,
     * i.e. which are selected or on which the user clicked.
     * @param objectIdsInContext Collection of ids of the model objects which are in context.
     * @return Menu part to be contributed or {@code null}.
     */
    Collection<MenuItem> getMenuItems(Collection<String> objectIdsInContext);
}
