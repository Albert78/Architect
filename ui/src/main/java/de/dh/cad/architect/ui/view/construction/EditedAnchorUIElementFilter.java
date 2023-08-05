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
package de.dh.cad.architect.ui.view.construction;

import java.util.Objects;

import de.dh.cad.architect.ui.objects.Abstract2DRepresentation;
import de.dh.cad.architect.ui.objects.AnchorConstructionRepresentation;
import de.dh.cad.architect.ui.objects.CeilingConstructionRepresentation;

public class EditedAnchorUIElementFilter extends GroundPlanUIElementFilter {
    protected final AnchorConstructionRepresentation mEditAnchorRepr;

    public EditedAnchorUIElementFilter(AnchorConstructionRepresentation editAnchorRepr) {
        mEditAnchorRepr = editAnchorRepr;
    }

    @Override
    public boolean isUIElementVisible(Abstract2DRepresentation repr) {
        if (super.isUIElementVisible(repr)) {
            return true;
        }
        if (repr instanceof AnchorConstructionRepresentation) {
            if (Objects.equals(repr, mEditAnchorRepr)) {
                return true;
            }
        } else if (repr instanceof CeilingConstructionRepresentation) {
            Abstract2DRepresentation anchorOwnerRepresentation = mEditAnchorRepr.getAnchorOwnerRepresentation();
            if (repr.equals(anchorOwnerRepresentation)) {
                // Ceiling of selected anchor
                return true;
            } else {
                return false;
            }
        }
        return false;
    }
}
