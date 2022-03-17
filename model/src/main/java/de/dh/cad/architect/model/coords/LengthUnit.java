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

import java.util.Objects;

public enum LengthUnit {
    MM("mm"), CM("cm"), M("m");

    public static LengthUnit DEFAULT = MM;

    private final String mUnitStr;

    private LengthUnit(String unitStr) {
        mUnitStr = unitStr;
    }

    public String getUnitStr() {
        return mUnitStr;
    }

    public static LengthUnit parse(String unitStr) {
        for (LengthUnit unit : values()) {
            if (Objects.equals(unitStr, unit.getUnitStr())) {
                return unit;
            }
        }
        throw new IllegalArgumentException("Illegal length unit string '" + unitStr + "'");
    }

    @Override
    public String toString() {
        return mUnitStr;
    }
}
