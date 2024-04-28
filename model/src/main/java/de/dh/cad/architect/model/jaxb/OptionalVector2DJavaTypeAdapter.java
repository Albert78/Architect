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
package de.dh.cad.architect.model.jaxb;

import java.util.Optional;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import de.dh.cad.architect.model.coords.IPosition;
import de.dh.cad.architect.model.coords.Vector2D;
import de.dh.cad.architect.model.jaxb.PositionJavaTypeAdapter.PositionProxy;

public class OptionalVector2DJavaTypeAdapter extends XmlAdapter<Vector2DJavaTypeAdapter.Vector2DProxy, Optional<Vector2D>> {
    @Override
    public Optional<Vector2D> unmarshal(Vector2DJavaTypeAdapter.Vector2DProxy v) throws Exception {
        return Optional.ofNullable(v.toVector2D());
    }

    @Override
    public Vector2DJavaTypeAdapter.Vector2DProxy marshal(Optional<Vector2D> v) throws Exception {
        return v.map(Vector2DJavaTypeAdapter.Vector2DProxy::from).orElse(null);
    }
}
