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
package de.dh.utils.io.obj;

public class ParserUtils {
    public static class TokenIterator {
        protected final String[] mTokens;
        protected int mIndex = -1;

        public TokenIterator(String[] tokens) {
            mTokens = tokens;
        }

        public static TokenIterator tokenize(String str) {
            String[] tokens = str.split("\\s");
            return new TokenIterator(tokens);
        }

        public String[] getAllTokens() {
            return mTokens;
        }

        public boolean moveNext() {
            if (mIndex >= mTokens.length - 1) {
                return false;
            }
            mIndex++;
            return true;
        }

        public boolean moveBack() {
            if (mIndex <= 0) {
                return false;
            }
            mIndex--;
            return true;
        }

        public String getCurrentToken() {
            return mTokens[mIndex];
        }
    }

    public static String getLastPart(String line) {
        line = line.trim();
        int index = line.lastIndexOf(' ');
        if (index == -1) {
            return line;
        }
        return line.substring(index + 1);
    }
}
