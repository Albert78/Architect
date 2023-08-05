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

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;

/**
 * General implementation of a namespace containing unique names mapped to objects.
 */
public class Namespace<T> {
    protected final Map<String, T> mNamespaceMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    public Namespace() {
        // Empty
    }

    public Namespace(Map<String, T> initialContents) {
        mNamespaceMap.putAll(initialContents);
    }

    public Map<String, T> getMappings() {
        return mNamespaceMap;
    }

    public void setMappings(Map<String, T> value) {
        mNamespaceMap.clear();
        mNamespaceMap.putAll(value);
    }

    public Object get(String name) {
        return mNamespaceMap.get(name);
    }

    public boolean contains(String name) {
        return name != null && mNamespaceMap.containsKey(name);
    }

    public boolean add(String name, T value) {
        if (name == null || mNamespaceMap.containsKey(name)) {
            return false;
        }
        mNamespaceMap.put(name, value);
        return true;
    }

    public T remove(String name) {
        return mNamespaceMap.remove(name);
    }

    /**
     * Convenience method for {@link #generateName(String, int)} starting with a number of {@code 0}.
     */
    public String generateName(String namePattern) {
        return generateName(namePattern, 0);
    }

    /**
     * Generates a unique name in this namespace. Attention: The generated name is not automatically added to the namespace, you need
     * to manually add it if needed.
     * @param namePattern Either a prefix string which will be appended with an underscore character and a number to make the
     * string unique (e.g. <code>"prefix_5"</code>) or a pattern with a <code>{0}</code> placeholder to place the number somewhere else in
     * the string, like <code>DesiredName ({0})</code>.
     * @return Name which is unique in this namespace.
     */
    public String generateName(String namePattern, int startNum) {
        return generateName(namePattern, mNamespaceMap.keySet(), startNum);
    }

    public static String generateName(String namePattern, Collection<String> availableNames, int startNum) {
        if (StringUtils.isEmpty(namePattern)) {
            return namePattern;
        }
        if (!namePattern.contains("{0}"))
            namePattern = namePattern + "_{0}";
        int num = startNum;
        String result;
        do {
            result = MessageFormat.format(namePattern, String.valueOf(num++));
        } while (availableNames.contains(result));
        return result;
    }
}
