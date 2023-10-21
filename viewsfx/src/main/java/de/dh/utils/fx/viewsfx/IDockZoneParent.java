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
package de.dh.utils.fx.viewsfx;

/**
 * Sash dock host or root = dock area control.
 */
public interface IDockZoneParent {
    String getDockAreaId();

    /**
     * Tells this parent that the given child is invalid, i.e. doesn't have any more valid content to
     * show. The receiving parent will decide if the child should be removed or retained.
     */
    void invalidateLeaf(TabDockHost child);

    /**
     * Tells this parent that the given {@code obsoleteSash} child is not necessary any more because
     * it only has a single child left. This method makes this {@code moveUpChild} replace the
     * {@code obsoleteSash}.
     */
    void compressDockHierarchy(SashDockHost obsoleteSash, IDockZone moveUpChild);

    /**
     * Tells this parent to replace the given {@code replaceChild} with a {@link SashDockHost}, creating a new child
     * dock zone.
     * @param replaceChild Child dock zone of this dock zone parent which should be replaced by a sash.
     * @param newChildDockZoneId New dock zone ID which is assigned to the child when it is moved down the hierarchy.
     * @param emptySide Side where the new sash should be empty; the {@code replaceChild} will be set at the opposite side.
     * @param dockHostCreator Creator for dock hosts.
     */
    SashDockHost replaceWithSash(IDockZone replaceChild, String newChildDockZoneId, DockSide emptySide, IDockHostCreator dockHostCreator);
}
