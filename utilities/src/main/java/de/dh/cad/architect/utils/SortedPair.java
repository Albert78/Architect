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
package de.dh.cad.architect.utils;

import java.util.Objects;

public class SortedPair<T extends Comparable<T>> implements Comparable<SortedPair<T>> {
    protected final T mFirst;
    protected final T mSecond;

    public SortedPair(T a, T b) {
        if (a.compareTo(b) < 0) {
            mFirst = b;
            mSecond = a;
        } else {
            mFirst = a;
            mSecond = b;
        }
    }

    public T getFirst() {
        return mFirst;
    }

    public T getSecond() {
        return mSecond;
    }

    @Override
    public int compareTo(SortedPair<T> o) {
        int ret = mFirst.compareTo(o.mFirst);
        if (ret != 0) {
            return ret;
        }
        ret = mSecond.compareTo(o.mSecond);
        if (ret != 0) {
            return ret;
        }
        return 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mFirst, mSecond);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        @SuppressWarnings("rawtypes")
        SortedPair other = (SortedPair) obj;
        return Objects.equals(mFirst, other.mFirst) && Objects.equals(mSecond, other.mSecond);
    }
}
