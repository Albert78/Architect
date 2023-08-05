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
package de.dh.cad.architect.model.objects;

import java.util.Collection;
import java.util.List;

import de.dh.cad.architect.model.changes.IModelChange;

/**
 * Class which is able to contain child objects.
 */
public interface IObjectsContainer {
    /**
     * Tries to find the object with the given id in this container.
     * @return Object with the given id or {@code null}.
     */
    BaseObject getObjectById(String id);

    /**
     * Gets all direct children which are owned by this instance.
     * The returned children return this instance via their {@link BaseObject#getOwnerContainer()} method.
     * Examples for owned children are wall holes of a wall.
     */
    Collection<? extends BaseObject> getOwnedChildren();

    /**
     * Internal method to be used in model module.
     * Adds the given child object to this instance as part of a add operation.
     */
    void addOwnedChild_Internal(BaseObject obj, List<IModelChange> changeTrace);

    /**
     * Internal method to be used in model module.
     * Removes the given owned child object from this instance as part of a delete operation of a child object.
     */
    void removeOwnedChild_Internal(BaseObject obj, List<IModelChange> changeTrace);

    /**
     * Returns the container which holds the anchors.
     * In a plan, that is the root container while a transport container can act as anchor container itself.
     */
    IAnchorContainer getAnchorContainer();
}
