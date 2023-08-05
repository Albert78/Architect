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
module de.dh.cad.architect.model {
    exports de.dh.cad.architect.model.jaxb;
    exports de.dh.cad.architect.model;
    exports de.dh.cad.architect.model.objects;
    exports de.dh.cad.architect.model.changes;
    exports de.dh.cad.architect.model.wallmodel;
    exports de.dh.cad.architect.model.assets;
    exports de.dh.cad.architect.model.coords;

    opens de.dh.cad.architect.model.jaxb;
    opens de.dh.cad.architect.model.objects;
    opens de.dh.cad.architect.model.changes;
    opens de.dh.cad.architect.model.assets;
    opens de.dh.cad.architect.model.coords;

    requires transitive java.xml.bind;

    requires org.slf4j;
    requires transitive java.prefs;
    requires de.dh.cad.architect.utils;
    requires org.apache.commons.lang3;
    requires org.apache.commons.collections4;
}