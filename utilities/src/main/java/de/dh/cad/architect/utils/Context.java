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

/**
 * Wrapper class to hold a context containing a single value.
 * This can be used to trick the Java compiler in case a final variable is necessary.
 * Typically this is used to hold an artificial context during multiple calls to a lambda or anonymous class' method.
 *
 * For example:
 * <code><pre>
 * Context<Boolean> changed = new Context(false);
 *
 * mMyObjects.forEach(o -> {
 *     if (o.someMethod()) {
 *         changed.set(true);
 *     }
 * });
 *
 * if (changed.get()) {
 *     // ...
 * }
 * </pre></code>
 */
public class Context<T> {
    protected T mValue;

    public Context() {
        this(null);
    }

    public Context(T value) {
        mValue = value;
    }

    public static <T> Context<T> of(T value) {
        return new Context<>(value);
    }

    public T get() {
        return mValue;
    }

    public void set(T value) {
        mValue = value;
    }
}
