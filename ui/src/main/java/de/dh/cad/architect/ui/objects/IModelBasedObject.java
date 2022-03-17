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
package de.dh.cad.architect.ui.objects;

import de.dh.cad.architect.model.objects.BaseObject;

public interface IModelBasedObject {
    String getModelId();
    BaseObject getModelObject();
    void updateToModel();

    /**
     * Property indicating that this object is contained in the set of selected objects.
     * Normally, this makes the stroke become blue.
     */
    boolean isSelected();
    void setSelected(boolean value);

    /**
     * Property indicating that this object is temporary spotted.
     * Normally, this makes the stroke become thicker.
     * This happens if the mouse cursor is over any shape of this object representation or if the object is
     * selected in the objects tree.
     * This property is decoupled from MouseOver property to enable object spot without mouse interaction.
     * The state of this property is also independent from the object's selection or from its focus.
     */
    boolean isObjectSpotted();
    void setObjectSpotted(boolean value);

    /**
     * Property indicating that this object is the primary focused object. This is the case if exactly one object
     * is selected.
     * Typically, the focused object is the one for which editing controls are shown.
     */
    boolean isObjectFocused();
    void setObjectFocused(boolean value);

    /**
     * Property indicating that this object is emphasised to show it has a special role in the current context,
     * e.g. it is the owner of the currently selected object/anchor.
     * Normally, this makes the stroke become dashed.
     */
    boolean isObjectEmphasized();
    void setObjectEmphasized(boolean value);
}
