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
package de.dh.cad.architect.ui.view.construction.feedback.wall;

/**
 * Container holding the state of temporary wall states (in form of instances of {@link AncillaryWall} and
 * {@link AncillaryWallAnchor}) to be displayed during a create or edit wall operation until the new wall state is
 * committed into the plan.
 */
public class PrincipalWallAncillaryWallsModel extends AncillaryWallsModel {
    protected final AncillaryWall mPrincipalWall;

    public PrincipalWallAncillaryWallsModel() {
        mPrincipalWall = createNewWall();
    }

    public AncillaryWall getPrincipalWall() {
        return mPrincipalWall;
    }

    public AncillaryWallAnchor getPrincipalWallStartAnchor() {
        return mPrincipalWall.getAnchorWallHandleA();
    }

    public AncillaryWallAnchor getPrincipalWallEndAnchor() {
        return mPrincipalWall.getAnchorWallHandleB();
    }
}
