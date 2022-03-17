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
package de.dh.cad.architect.model.jaxb;

import java.util.Optional;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import de.dh.cad.architect.model.coords.IPosition;
import de.dh.cad.architect.model.jaxb.PositionJavaTypeAdapter.PositionProxy;

public class OptionalPositionJavaTypeAdapter extends XmlAdapter<PositionJavaTypeAdapter.PositionProxy, Optional<IPosition>> {
    @Override
    public Optional<IPosition> unmarshal(PositionJavaTypeAdapter.PositionProxy v) throws Exception {
        return Optional.ofNullable(v.toPosition());
    }

    @Override
    public PositionJavaTypeAdapter.PositionProxy marshal(Optional<IPosition> v) throws Exception {
        return v.isPresent() ? PositionProxy.from(v.get()) : null;
    }
}