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
package de.dh.cad.architect.ui.view.construction.behaviors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.dh.cad.architect.model.objects.SupportObject;
import javafx.scene.layout.BorderPane;

/**
 * Shows settings for a selected support object.
 */
public class SupportObjectInfoControl extends BorderPane {
    public static interface IChangeListener {
        void changed(SupportObjectInfoControl pane);
    }

    protected final List<IChangeListener> mListeners = new ArrayList<>();

    protected Collection<SupportObject> mSupportObjects = new ArrayList<>();

    public SupportObjectInfoControl() {
    }

    public Collection<SupportObject> getSupportObject() {
        return mSupportObjects;
    }

    public void setSupportObjects(Collection<SupportObject> value) {
        mSupportObjects.clear();
        mSupportObjects.addAll(value);
        // TODO
    }

    /**
     * Registers a listener to become fired when the user selects a different target anchor.
     */
    public void addChangeListener(IChangeListener listener) {
        mListeners.add(listener);
    }

    public void removeChangeListener(IChangeListener listener) {
        mListeners.remove(listener);
    }

    protected void fireChangeListeners() {
        for (IChangeListener listener : mListeners) {
            listener.changed(this);
        }
    }
}
