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
package de.dh.cad.architect.model;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.Locale;

public class Constants {
    public static class DoubleFormat {
        protected final DecimalFormat mDecimalFormat;

        public DoubleFormat(String format) {
            mDecimalFormat = new DecimalFormat();
        }

        public DoubleFormat(String format, DecimalFormatSymbols symbols) {
            mDecimalFormat = new DecimalFormat(format, symbols);
        }

        public synchronized Number parse(String str) throws ParseException {
            return mDecimalFormat.parse(str);
        }

        public synchronized String format(double number) {
            return mDecimalFormat.format(number);
        }
    }

    /**
     * Float decimal format localized for the default locale in this system.
     * The returned instance is safe to be used by multiple threads.
     */
    public static final DoubleFormat DEFAULT_LOCALIZED_CANONICAL_FLOAT_FORMAT = new DoubleFormat("#.##");

    /**
     * Float decimal format localized for the english language.
     * The returned instance is safe to be used by multiple threads.
     */
    public static final DoubleFormat TRANSPORTABLE_CANONICAL_FLOAT_FORMAT = new DoubleFormat("#.##", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
}
