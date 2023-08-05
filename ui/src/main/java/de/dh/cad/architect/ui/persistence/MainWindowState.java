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

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

public class MainWindowState {
    protected Map<Class<? extends ViewState>, ViewState> mViewStates = new TreeMap<>(Comparator.comparing(Class::getSimpleName));

    public MainWindowState() {
        // For JAXB
    }

    @XmlJavaTypeAdapter(ViewStatesTypeAdapter.class)
    @XmlElement(name = "ViewStates")
    public Map<Class<? extends ViewState>, ViewState> getViewStates() {
        return mViewStates;
    }

    // Setter necessary for JAXB because we're working with a type adapter
    public void setViewStates(Map<Class<? extends ViewState>, ViewState> value) {
        mViewStates = value;
    }

    @SuppressWarnings("unchecked")
    public <T extends ViewState> Optional<T> getViewState(Class<T> cls) {
        return Optional.ofNullable((T) mViewStates.get(cls));
    }

    public void addViewState(ViewState viewState) {
        mViewStates.put(viewState.getClass(), viewState);
    }
}
