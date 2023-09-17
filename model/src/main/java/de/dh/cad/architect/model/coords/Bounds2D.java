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

/**
 * Defines a 2D bounding box in global / Architect coordinate system.
 * The important aspect is that this class only stores X1/Y1 and X2/Y2, the other two corners are derived from those. That is only possible if we assume a rotation of zero.
 * To express a rotated box, use {@link Box2D}.
 *
 * X grows to the right,
 * Y grows to the top.
 *
 * <pre>
 * X1/Y2 ----- X2/Y2
 *   |           |
 *   |           |
 * X1/Y1 ----- X2/Y1
 * </pre>
 */
public class Bounds2D {
    protected static final Bounds2D EMPTY = new Bounds2D(Length.ZERO, Length.ZERO, Length.ZERO, Length.ZERO);
    protected final Length mX1;
    protected final Length mY1;
    protected final Length mX2;
    protected final Length mY2;

    public Bounds2D(Length left, Length bottom, Length right, Length top) {
        mX1 = left;
        mY1 = bottom;
        mX2 = right;
        mY2 = top;
    }

    public static Bounds2D empty() {
        return EMPTY;
    }

    public static Bounds2D of(Position2D bottomLeft, Dimensions2D size) {
        return new Bounds2D(bottomLeft.getX(), bottomLeft.getY(), bottomLeft.getX().plus(size.getX()), bottomLeft.getY().plus(size.getY()));
    }

    /**
     * Creates a new bounds object from the given X1/Y1 coordinate to the given X2/Y2 coordinate.
     * @param bottomLeft X1/Y1 position.
     * @param topRight X2/Y2 position.
     */
    public static Bounds2D of(Position2D bottomLeft, Position2D topRight) {
        return new Bounds2D(bottomLeft.getX(), bottomLeft.getY(), topRight.getX(), topRight.getY());
    }

    /**
     * The position in the top-left corner.
     */
    public Position2D tl() {
        return new Position2D(mX1, mY2);
    }

    /**
     * The position in the top-right corner.
     */
    public Position2D tr() {
        return new Position2D(mX2, mY2);
    }

    /**
     * The position in the bottom-left corner.
     */
    public Position2D bl() {
        return new Position2D(mX1, mY1);
    }

    /**
     * The position in the bottom-right corner.
     */
    public Position2D br() {
        return new Position2D(mX2, mY1);
    }

    public Length getX1() {
        return mX1;
    }

    public Length getY1() {
        return mY1;
    }

    public Length getX2() {
        return mX2;
    }

    public Length getY2() {
        return mY2;
    }

    public Dimensions2D size() {
        return new Dimensions2D(getWidth(), getHeight());
    }

    public Length getWidth() {
        return mX2.minus(mX1).abs();
    }

    public Length getHeight() {
        return mY2.minus(mY1).abs();
    }

    public Bounds2D move(Vector2D v) {
        return new Bounds2D(
            mX1.plus(v.getX()),
            mY1.plus(v.getY()),
            mX2.plus(v.getX()),
            mY2.plus(v.getY()));
    }

    public Bounds2D union(Position2D posToInclude) {
        Length x = posToInclude.getX();
        Length y = posToInclude.getY();
        return new Bounds2D(
            mX1.gt(x) ? x : mX1,
            mY1.gt(y) ? y : mY1,
            mX2.lt(x) ? x : mX2,
            mY2.lt(y) ? y : mY2);
    }

    public Bounds2D union(Bounds2D boundsToInclude) {
        // Other object is a Bounds2D and thus has ordered X1/X2 and Y1/Y2 coordinates
        Length x1 = boundsToInclude.getX1();
        Length y1 = boundsToInclude.getY1();
        Length x2 = boundsToInclude.getX2();
        Length y2 = boundsToInclude.getY2();
        return new Bounds2D(
            mX1.gt(x1) ? x1 : mX1,
            mY1.gt(y1) ? y1 : mY1,
            mX2.lt(x2) ? x2 : mX2,
            mY2.lt(y2) ? y2 : mY2);
    }

    public Bounds2D union(Box2D box) {
        // Other object is a Box2D and thus has independent corner positions
        return union(box.getX1Y1())
                .union(box.getX1Y2())
                .union(box.getX2Y1())
                .union(box.getX2Y2());
    }

    public Bounds2D withLeft(Length x) {
        return new Bounds2D(x, mY1, mX2, mY2);
    }

    public Bounds2D withRight(Length x) {
        return new Bounds2D(mX1, mY1, x, mY2);
    }

    public Bounds2D withTop(Length y) {
        return new Bounds2D(mX1, mY1, mX2, y);
    }

    public Bounds2D withBottom(Length y) {
        return new Bounds2D(mX1, y, mX2, mY2);
    }

    /**
     * Returns a copy of this bounds, scaled according to 0/0.
     */
    public Bounds2D scale(double factor) {
        return Bounds2D.of(
            bl().toVector2D().times(factor).toPosition2D(),
            bl().plus(size().scale(factor).toVector()));
    }

    /**
     * Returns a copy of this bounds, scaled according to a pivot point.
     */
    public Bounds2D scale(double factor, Position2D pivot) {
        return Bounds2D.of(
            bl().minus(pivot).times(factor).addedTo(pivot),
            bl().plus(size().scale(factor).toVector()));
    }

    /**
     * Returns a copy of this box whose corners are rotated counter-clockwise by <code>angleDeg</code> around 0/0.
     */
    public Box2D rotate(double angleDeg) {
        return new Box2D(
            bl().rotate(angleDeg),
            br().rotate(angleDeg),
            tl().rotate(angleDeg),
            tr().rotate(angleDeg));
    }

    /**
     * Returns a copy of this box whose corners are rotated counter-clockwise by <code>angleDeg</code> around a pivot point.
     */
    public Box2D rotateAround(double angleDeg, Position2D pivot) {
        return new Box2D(
            bl().rotateAround(angleDeg, pivot),
            br().rotateAround(angleDeg, pivot),
            tl().rotateAround(angleDeg, pivot),
            tr().rotateAround(angleDeg, pivot));
    }

    public Box2D toBox2D() {
        return new Box2D(bl(), br(), tl(), tr());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + mX1.hashCode();
        result = prime * result + mY1.hashCode();
        result = prime * result + mX2.hashCode();
        result = prime * result + mY2.hashCode();
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
        Bounds2D other = (Bounds2D) obj;
        if (!mX1.equals(other.mX1))
            return false;
        if (!mY1.equals(other.mY1))
            return false;
        if (!mX2.equals(other.mX2))
            return false;
        if (!mY2.equals(other.mY2))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Bounds2D [BL = " + mX1 + "/" + mY1 + "; BR = " + mX2 + "/" + mY1 + "; TL = " + mX1 + "/" + mY2 + "; TR = " + mX2 + "/" + mY2 + "]";
    }
}
