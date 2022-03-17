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
package de.dh.cad.architect.ui.view;

import java.util.Collection;

import de.dh.cad.architect.ui.objects.IModelBasedObject;
import javafx.scene.Node;

/**
 * Dummy mode to avoid superfluous checks during initialization.
 */
public class NullMode<TRepr extends IModelBasedObject, TAnc extends Node> extends AbstractUiMode<TRepr, TAnc> {
    public NullMode() {
        super(null);
    }

    @Override
    public void install(AbstractPlanView<TRepr, TAnc> view) {
        // Nothing to do
    }

    @Override
    public void uninstall() {
        // Nothing to do
    }

    @Override
    public void resetBehavior() {
        // Nothing to do
    }

    @Override
    public void setBehavior(AbstractViewBehavior<TRepr, TAnc> behavior) {
        // Nothing to do
    }

    @Override
    public AbstractViewBehavior<TRepr, TAnc> getBehaviorForSelectedReprs(Collection<TRepr> selectedReprs) {
        return null;
    }
}
