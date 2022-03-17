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
package de.dh.cad.architect.utils.todo;

/**
 * Marker class to show parts of the code to be implemented, changed or fixed.
 * Unfinished code parts call methods of this class to be able to be found easily when searching references to this class.
 */
public class Unimplemented {
    public static void change(String msg) {
        System.out.println("To be changed: " + msg);
    }

    public static void implement(String msg) {
        System.out.println("To be implemented: " + msg);
    }

    ////// Für Libraries-Umbau
    public static interface Interim {}

    public static void interim() {
    }
}
