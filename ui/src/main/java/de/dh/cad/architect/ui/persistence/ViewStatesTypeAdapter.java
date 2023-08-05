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
package de.dh.cad.architect.ui.persistence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.adapters.XmlAdapter;

public class ViewStatesTypeAdapter extends XmlAdapter<ViewStatesTypeAdapter.ViewStatesMapProxy, Map<Class<? extends ViewState>, ViewState>> {
    @XmlSeeAlso({
        ConstructionViewState.class,
        ThreeDViewState.class
    })
    public static class ViewStatesMapProxy {
        protected Collection<ViewState> mViewStates = new ArrayList<>();

        @XmlElement(name = "ViewState")
        public Collection<ViewState> getViewStates() {
            return mViewStates;
        }
    }

    @Override
    public Map<Class<? extends ViewState>, ViewState> unmarshal(ViewStatesMapProxy v) throws Exception {
        Map<Class<? extends ViewState>, ViewState> result = new TreeMap<>(Comparator.comparing(Class::getSimpleName));
        for (ViewState viewState : v.getViewStates()) {
            result.put(viewState.getClass(), viewState);
        }
        return result;
    }

    @Override
    public ViewStatesMapProxy marshal(Map<Class<? extends ViewState>, ViewState> v) throws Exception {
        ViewStatesMapProxy result = new ViewStatesMapProxy();
        result.getViewStates().addAll(v.values());
        return result;
    }
}
