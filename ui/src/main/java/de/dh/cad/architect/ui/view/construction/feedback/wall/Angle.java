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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

import de.dh.cad.architect.model.coords.Vector2D;

public class Angle implements Comparable<Angle> {
    protected final double mAngleDeg;

    public Angle(double angle) {
        mAngleDeg = angle;
    }

    public static Angle ofVector(Vector2D v) {
        return new Angle(-Vector2D.angleBetween(v, Vector2D.X1M));
    }

    public double getAngleDeg() {
        return mAngleDeg;
    }

    public double getAngleRad() {
        return mAngleDeg * Math.PI / 180;
    }

    /**
     * Returns a unit vector which is {@link Vector2D#X1M} rotated counter-clockwise by this angle.
     */
    protected Vector2D calculateVector() {
        return Vector2D.X1M.rotate(mAngleDeg);
    }

    public Angle plusDeg(double deg) {
        return new Angle(mAngleDeg + deg);
    }

    public Angle opposite() {
        return new Angle(mAngleDeg + 180);
    }

    public Angle normalize() {
        double normalizedAngle = normalizeAngle(mAngleDeg);
        return mAngleDeg == normalizedAngle ? this : new Angle(normalizedAngle);
    }

    public Angle getSmallestAngleTo(Angle other) {
        double difference = other.mAngleDeg - mAngleDeg;
        difference = normalizeAngle(difference);
        if (difference > 180) difference = 360 - difference;
        return new Angle(difference);
    }

    public Angle abs() {
        return new Angle(Math.abs(mAngleDeg));
    }

    public Optional<Angle> findNearestAngle(Collection<Angle> refAngles, Double minDiff) {
        Angle result = null;
        double resultDiff = Double.MAX_VALUE;
        for (Angle refAngle : refAngles) {
            Angle diff = getSmallestAngleTo(refAngle);
            double diffDeg = diff.getAngleDeg();
            if ((result == null || diffDeg < resultDiff) && (minDiff == null || diffDeg > minDiff)) {
                result = refAngle.normalize();
                resultDiff = diffDeg;
            }
        }
        return Optional.ofNullable(result);
    }

    public static double normalizeAngle(double angle) {
        while (angle < 0) angle += 360;
        while (angle > 360) angle -= 360;
        return angle;
    }

    public static Collection<Angle> withOppositeAngles(Collection<Angle> refAngles) {
        Collection<Angle> result = new ArrayList<>();
        for (Angle refAngle : refAngles) {
            result.add(refAngle);
            result.add(refAngle.opposite());
        }
        return result;
    }

    @Override
    public int compareTo(Angle other) {
        return Double.compare(mAngleDeg, other.mAngleDeg);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(mAngleDeg);
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
        Angle other = (Angle) obj;
        if (Double.doubleToLongBits(mAngleDeg) != Double.doubleToLongBits(other.mAngleDeg))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return mAngleDeg + "°";
    }
}
