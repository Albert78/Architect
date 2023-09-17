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
package de.dh.cad.architect.model.coords;

import java.text.MessageFormat;
import java.text.ParseException;

import org.apache.commons.lang3.StringUtils;

import de.dh.cad.architect.model.Constants;

/**
 * Defines a 1D length which can be converted to any length unit.
 */
public class Length implements Comparable<Length> {
    public static final Length ZERO = new Length(0);

    protected final double mLengthMM;

    public Length(double lengthMM) {
        mLengthMM = lengthMM;
    }

    public static Length ofMM(double mm) {
        return new Length(mm);
    }

    public static Length ofCM(double cm) {
        return new Length(cm * 10);
    }

    public static Length ofM(double m) {
        return new Length(m * 1000);
    }

    /**
     * Returns a {@link Length} object of the internal value which can be retrieved by {@link #inInternalFormat()}.
     */
    public static Length ofInternalFormat(double internal) {
        return new Length(internal);
    }

    public static Length of(double val, LengthUnit unit) {
        switch (unit) {
        case MM:
            return ofMM(val);
        case CM:
            return ofCM(val);
        case M:
            return ofM(val);
        default:
            throw new IllegalArgumentException("Unexpected length unit: " + unit);
        }
    }

    public static Length fromTransportableString(String lengthStr) throws ParseException {
        if (StringUtils.isEmpty(lengthStr)) {
            return null;
        }
        String[] parts = lengthStr.split(" ");
        if (parts.length != 2) {
            throw new ParseException("Error parsing length string '" + lengthStr + "', length with length unit expected", 0);
        }
        String valueStr = parts[0];
        String unitStr = parts[1];
        double value = Constants.TRANSPORTABLE_CANONICAL_FLOAT_FORMAT.parse(valueStr).doubleValue();
        LengthUnit unit;
        try {
            unit = LengthUnit.parse(unitStr);
        } catch (IllegalArgumentException e) {
            throw new ParseException("Error parsing length unit in length string '" + lengthStr + "'", lengthStr.indexOf(' '));
        }
        return Length.of(value, unit);
    }

    public double inMM() {
        return mLengthMM;
    }

    public double inCM() {
        return mLengthMM / 10.0;
    }

    public double inM() {
        return mLengthMM / 1000.0;
    }

    /**
     * Returns this value in internal format. This is sensible if calculations should be done
     * which don't depend on the scale, e.g. calculating a normal vector, ...
     */
    public double inInternalFormat() {
        return mLengthMM;
    }

    public double inUnit(LengthUnit unit) {
        switch (unit) {
        case MM:
            return inMM();
        case CM:
            return inCM();
        case M:
            return inM();
        default:
            throw new IllegalArgumentException("Unexpected length unit: " + unit);
        }
    }

    public Length plus(Length value) {
        return new Length(mLengthMM + value.mLengthMM);
    }

    public Length minus(Length value) {
        return new Length(mLengthMM - value.mLengthMM);
    }

    public Length times(double value) {
        return Length.ofMM(mLengthMM * value);
    }

    public Length abs() {
        return new Length(Math.abs(mLengthMM));
    }

    public Length difference(Length value) {
        return value.minus(this).abs();
    }

    public Length negated() {
        return Length.ofMM(-mLengthMM);
    }

    public double divideBy(Length value) {
        return mLengthMM / value.mLengthMM;
    }

    public Length divideBy(double value) {
        return new Length(mLengthMM / value);
    }

    public Length enlarge(Length value) {
        return lt(value) ? value : this;
    }

    public boolean eq(Length other) {
        return mLengthMM == other.mLengthMM;
    }

    public boolean ne(Length other) {
        return mLengthMM != other.mLengthMM;
    }

    public boolean lt(Length other) {
        return mLengthMM < other.mLengthMM;
    }

    public boolean le(Length other) {
        return mLengthMM <= other.mLengthMM;
    }

    public boolean gt(Length other) {
        return mLengthMM > other.mLengthMM;
    }

    public boolean ge(Length other) {
        return mLengthMM >= other.mLengthMM;
    }

    /**
     * Returns whether this length is a real length, i.e. not infinite or NaN.
     */
    public boolean isValid() {
        return Double.isFinite(mLengthMM);
    }

    public LengthUnit getBestUnitForEdit() {
        double absLength = Math.abs(mLengthMM);
        if (absLength < 0.5) { // Effective 0
            return LengthUnit.CM;
        } else if (absLength < 100) { // 10 cm
            return LengthUnit.MM;
        } else if (absLength < 10000) { // 10 m
            return LengthUnit.CM;
        } else {
            return LengthUnit.M;
        }
    }

    public LengthUnit getBestUnitForDisplay() {
        double absLength = Math.abs(mLengthMM);
        if (absLength < 0.5) { // Effective 0
            return LengthUnit.CM;
        } else if (absLength < 40) { // 4 cm
            return LengthUnit.MM;
        } else if (absLength < 1000) { // 1 m
            return LengthUnit.CM;
        } else {
            return LengthUnit.M;
        }
    }

    public String toHumanReadableString(LengthUnit unit, int numDecimalPlaces, boolean showUnit) {
        String valuePart = "{0,number,0";
        if (numDecimalPlaces > 0) {
            valuePart += ".";
        }
        valuePart += "0".repeat(numDecimalPlaces) + "}";
        if (showUnit) {
            return MessageFormat.format(valuePart + " {1}", inUnit(unit), unit.getUnitStr());
        } else {
            return MessageFormat.format(valuePart, inUnit(unit));
        }
    }

    public String toHumanReadableString(LengthUnit unit, boolean showUnit) {
        String result = Constants.DEFAULT_LOCALIZED_CANONICAL_FLOAT_FORMAT.format(inUnit(unit));
        if (showUnit) {
            result += " " + unit.getUnitStr();
        }
        return result;
    }

    public String toTransportableString() {
        LengthUnit unit = LengthUnit.MM;
        return Constants.TRANSPORTABLE_CANONICAL_FLOAT_FORMAT.format(inUnit(unit)) + " " + unit.getUnitStr();
    }

    public String toNormalPlanString() {
        return ((int) mLengthMM) / 10 < 100 ? toHumanReadableString(LengthUnit.CM, 0, false) : toHumanReadableString(LengthUnit.M, 2, false);
    }

    @Override
    public int compareTo(Length o) {
        return Double.compare(mLengthMM, o.mLengthMM);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(mLengthMM);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Length other = (Length) obj;
        if (Double.doubleToLongBits(mLengthMM) != Double.doubleToLongBits(other.mLengthMM))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return toHumanReadableString(getBestUnitForDisplay(), 2, true);
    }

    public static Length max(Length v1, Length v2) {
        return v1.lt(v2) ? v2 : v1;
    }

    public static Length max(Length v1, Length v2, Length v3) {
        Length m12 = max(v1, v2);
        return m12.lt(v3) ? v3 : m12;
    }

    public static Length min(Length v1, Length v2) {
        return v1.lt(v2) ? v1 : v2;
    }

    public static Length min(Length v1, Length v2, Length v3) {
        Length m12 = min(v1, v2);
        return m12.lt(v3) ? m12 : v3;
    }
}
