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
package de.dh.cad.architect.ui.view.libraries;

import java.util.Comparator;
import java.util.Objects;

import de.dh.cad.architect.model.assets.AssetLibrary;
import de.dh.cad.architect.ui.assets.AssetManager.LibraryData;
import de.dh.cad.architect.utils.vfs.IDirectoryLocator;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class CheckableLibraryEntry {
    public static class CheckableLibraryEntryByNameComparator implements Comparator<CheckableLibraryEntry> {
        @Override
        public int compare(CheckableLibraryEntry o1, CheckableLibraryEntry o2) {
            return o1.getLibrary().getName().compareToIgnoreCase(o2.getLibrary().getName());
        }
    }

    public static final Comparator<CheckableLibraryEntry> COMPARATOR_BY_NAME = new CheckableLibraryEntryByNameComparator();

    protected final IDirectoryLocator mRootDirectory;
    protected final AssetLibrary mLibrary;
    protected final SimpleBooleanProperty mSelectedProperty = new SimpleBooleanProperty(true);

    public CheckableLibraryEntry(AssetLibrary library, IDirectoryLocator rootDirectory) {
        mRootDirectory = rootDirectory;
        mLibrary = library;
    }

    public static CheckableLibraryEntry fromLibraryData(LibraryData libraryData) {
        return new CheckableLibraryEntry(libraryData.getLibrary(), libraryData.getRootDirectory());
    }

    public IDirectoryLocator getRootDirectory() {
        return mRootDirectory;
    }

    public AssetLibrary getLibrary() {
        return mLibrary;
    }

    public BooleanProperty selectedProperty() {
        return mSelectedProperty;
    }

    public boolean isSelected() {
        return mSelectedProperty.get();
    }

    public void setSelected(boolean value) {
        mSelectedProperty.set(value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mLibrary);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CheckableLibraryEntry other = (CheckableLibraryEntry) obj;
        return Objects.equals(mLibrary, other.mLibrary);
    }

    @Override
    public String toString() {
        return mLibrary.getName() + " (" + mRootDirectory.getAbsolutePath() + ")";
    }
}
