/*******************************************************************************
 *     Architect - A free 2D/3D home and interior designer
 *     Copyright (C) 2021, 2022  Daniel Höh
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
package de.dh.cad.architect.model.coords;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import de.dh.cad.architect.model.objects.Anchor;

/**
 * Move position target for a given 2D or 3D anchor.
 */
public class AnchorTarget {
    protected final Anchor mAnchor;
    protected final IPosition mTargetPosition;

    /**
     * Creates a new {@link AnchorTarget} object.
     * @param anchor Target anchor for the given target position.
     * @param targetPosition New position for the given anchor, must fit to the anchor (2D/3D).
     */
    public AnchorTarget(Anchor anchor, IPosition targetPosition) {
        mAnchor = anchor;
        mTargetPosition = targetPosition;
    }

    public Anchor getAnchor() {
        return mAnchor;
    }

    public IPosition getTargetPosition() {
        return mTargetPosition;
    }

    @Override
    public String toString() {
        return "Anchor '" + mAnchor.getId() + "' -> " + mTargetPosition;
    }

    public static Map<String, AnchorTarget> mapAnchorIdToTargetFromAnchors(Collection<Anchor> anchors) {
        return mapAnchorIdToTarget(fromAnchors(anchors));
    }

    public static Map<String, AnchorTarget> mapAnchorIdToTarget(Collection<AnchorTarget> anchorTargets) {
        return anchorTargets.stream().collect(Collectors.toMap(at -> at.getAnchor().getId(), at -> at));
    }

    public static Collection<AnchorTarget> fromAnchors(Collection<Anchor> anchors) {
        Collection<AnchorTarget> result = new ArrayList<>();
        for (Anchor anchor : anchors) {
            result.add(new AnchorTarget(anchor, anchor.getPosition()));
        }
        return result;
    }
}