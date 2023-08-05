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
package de.dh.utils.fx.viewsfx.utils;

import java.util.Optional;

import de.dh.utils.fx.sash.Sash;
import de.dh.utils.fx.viewsfx.DockSide;
import javafx.geometry.Orientation;
import javafx.scene.Node;

public class SashUtils {
    public static void setSashItem(Sash sash, DockSide side, Node item) {
        switch (side) {
        case West:
        case North:
            sash.setFirstChild(item);
            break;
        case East:
        case South:
            sash.setLastChild(item);
            break;
        }
    }

    public static Optional<DockSide> getDockSideOfItem(Sash sash, Node item) {
        boolean isFirst = item == sash.getFirstChild();
        boolean isLast = item == sash.getLastChild();
        if (sash.getOrientation() == Orientation.HORIZONTAL) {
            return isFirst ? Optional.of(DockSide.West) : (isLast ? Optional.of(DockSide.East) : Optional.empty());
        } else {
            return isFirst ? Optional.of(DockSide.North) : (isLast ? Optional.of(DockSide.South) : Optional.empty());
        }
    }

    public static Node getSashItem(Sash sash, DockSide side) {
        switch (side) {
        case West:
        case North:
            return sash.getFirstChild();
        case East:
        case South:
            return sash.getLastChild();
        }
        throw new RuntimeException("Method getSashItem() is not implemented for DockSide " + side);
    }
}
