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
package de.dh.cad.architect.ui.objecttree;

import de.dh.cad.architect.model.objects.BaseObject;

public interface IObjectTreeItemData extends ITreeItemData, IIdObjectTreeItemData {
    BaseObject getObject();

    /**
     * Returns the information if the tree item with this data entry is a shadow entry for the our
     * {@link #getObject() object}. An object has one main entry and multiple shadow entries. A shadow
     * entry behaves like the main entry but doesn't have all functions. For example structured objects
     * don't have children under their shadow entries.
     */
    boolean isShadowEntry();
}
