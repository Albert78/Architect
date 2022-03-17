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
package de.dh.cad.architect.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public class ObjectStringAdapter<T> {
    protected final T mObj;
    protected final String mString;

    public ObjectStringAdapter(T obj, String string) {
        mObj = obj;
        mString = string;
    }

    public T getObj() {
        return mObj;
    }

    public static <T> List<ObjectStringAdapter<T>> wrap(Collection<T> items, Function<T, String> itemStringProvider) {
        List<ObjectStringAdapter<T>> result = new ArrayList<>();
        for (T item : items) {
            result.add(new ObjectStringAdapter<>(item, itemStringProvider.apply(item)));
        }
        return result;
    }

    public static <T> List<ObjectStringAdapter<T>> wrap(T[] items, Function<T, String> itemStringProvider) {
        return wrap(Arrays.asList(items), itemStringProvider);
    }

    public static <T> ObjectStringAdapter<T> compareDummy(T obj) {
        return new ObjectStringAdapter<>(obj, null);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((mObj == null) ? 0 : mObj.hashCode());
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
        @SuppressWarnings("rawtypes")
        ObjectStringAdapter other = (ObjectStringAdapter) obj;
        if (mObj == null) {
            if (other.mObj != null)
                return false;
        } else if (!mObj.equals(other.mObj))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return mString;
    }
}
