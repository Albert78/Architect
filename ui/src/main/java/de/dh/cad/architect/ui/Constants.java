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
package de.dh.cad.architect.ui;

import java.net.URL;
import java.text.NumberFormat;
import java.util.Locale;

import de.dh.cad.architect.model.coords.Length;

public class Constants {
    public static Length DEFAULT_WALL_THICKNESS = Length.ofCM(30);
    public static Length DEFAULT_WALL_HEIGHT = Length.ofCM(250);

    public static final int TWO_D_INFO_SYMBOLS_SIZE = 32;

    public static final double VIEW_ORDER_OFFSET_NORMAL = 0;
    public static final double VIEW_ORDER_OFFSET_FOCUSED = -10;

    public static final double VIEW_ORDER_ANCILLARY = -1;
    public static final double VIEW_ORDER_ANCHOR = 0;
    public static final double VIEW_ORDER_DIMENSIONING = 1;
    public static final double VIEW_ORDER_CEILING = 2;
    public static final double VIEW_ORDER_WALL_HOLE = 3;
    public static final double VIEW_ORDER_WALL = 4;
    public static final double VIEW_ORDER_COVERING = 5;
    public static final double VIEW_ORDER_SUPPORT_OBJECTS = 6;
    public static final double VIEW_ORDER_FLOOR = 7;

    public static final double VIEW_ORDER_UNKNOWN = 10;

    public static final double VIEW_ORDER_INTERACTION = -20; // On top

    // View order in ruler
    public static final double VIEW_ORDER_RULER_GUIDE_ARROW = 0;
    public static final double VIEW_ORDER_RULER = 1;

    // View order in top layer
    public static final double VIEW_ORDER_GUIDE_LINE = 0;

    public static final NumberFormat LOCALIZED_NUMBER_FORMAT = NumberFormat.getInstance(Locale.GERMAN);

    public static final URL APPLICATION_CSS = Constants.class.getResource("view/application.css");

    public static String BEHAVIOR_TITLE_STYLE = "-fx-font-size: 150%; -fx-font-weight: bold;";

    public static String INTERACTIONS_TITLE_STYLE = "-fx-font-size: 150%; -fx-font-weight: bold;";
    public static String INTERACTIONS_TEXT_STYLE = "-fx-font-size: 100%; -fx-font-weight: bold;";
}
