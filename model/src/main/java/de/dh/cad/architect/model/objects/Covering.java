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
package de.dh.cad.architect.model.objects;

import de.dh.cad.architect.model.ChangeSet;
import de.dh.cad.architect.model.coords.Position3D;
import de.dh.cad.architect.utils.IdGenerator;

/**
 * A covering is a triangle plane whose plane and location is defined by three covering anchors.
 * It provides different surfaces for the two sides.
 */
public class Covering extends BaseSolidObject {
    public static final String SURFACE_TYPE_1 = "Surface-1";
    public static final String SURFACE_TYPE_2 = "Surface-2";

    /**
     * First of the three covering (3D handle) anchors.
     */
    public static final String AP_COVERING_A = "Handle-A";

    /**
     * Second of the three covering (3D handle) anchors.
     */
    public static final String AP_COVERING_B = "Handle-B";

    /**
     * Third of the three covering (3D handle) anchors.
     */
    public static final String AP_COVERING_C = "Handle-C";

    public Covering() {
        // For JAXB
    }

    public Covering(String id, String name) {
        super(id, name);
    }

    @Override
    public void initializeSurfaces() {
        clearSurfaces();
        createSurface(SURFACE_TYPE_1);
        createSurface(SURFACE_TYPE_2);
    }

    /**
     * Creates a new covering with the given covering anchors; the covering anchors will determine the covering's plane and also limit it.
     */
    public static Covering create(String name, Position3D posA, Position3D posB, Position3D posC, IObjectsContainer ownerContainer, ChangeSet changeSet) {
        Covering result = new Covering(IdGenerator.generateUniqueId(Covering.class), name);
        ownerContainer.addOwnedChild_Internal(result, changeSet);
        result.createAnchor(AP_COVERING_A, posA, changeSet);
        result.createAnchor(AP_COVERING_B, posB, changeSet);
        result.createAnchor(AP_COVERING_C, posC, changeSet);
        return result;
    }

    public Anchor getAnchorA() {
        return getAnchorByAnchorType(AP_COVERING_A);
    }

    public Anchor getAnchorB() {
        return getAnchorByAnchorType(AP_COVERING_B);
    }

    public Anchor getAnchorC() {
        return getAnchorByAnchorType(AP_COVERING_C);
    }

    @Override
    public boolean isHandle(Anchor anchor) {
        String anchorType = anchor.getAnchorType();
        return AP_COVERING_A.equals(anchorType) ||
                        AP_COVERING_B.equals(anchorType) ||
                        AP_COVERING_C.equals(anchorType);
    }

    @Override
    public boolean isPossibleDimensioningDockTargetAnchor(Anchor anchor) {
        return true;
    }

    @Override
    protected String attrsToString() {
        return super.attrsToString() +
                        ", PosA = " + getAnchorByAnchorType(AP_COVERING_A).getPosition() +
                        "; PosB = " + getAnchorByAnchorType(AP_COVERING_B).getPosition() +
                        "; PosC = " + getAnchorByAnchorType(AP_COVERING_C).getPosition();
    }
}
