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
package de.dh.cad.architect.ui.objects;

import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.model.objects.Anchor;
import de.dh.cad.architect.ui.utils.CoordinateUtils;
import de.dh.cad.architect.ui.view.threed.Abstract3DView;
import de.dh.cad.architect.ui.view.threed.ThreeDView;
import de.dh.utils.Vector3D;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Sphere;

public class Anchor3DRepresentation extends Abstract3DRepresentation {
    protected final Sphere mSphere = new Sphere();
    protected final int RADIUS = 5;

    public Anchor3DRepresentation(Anchor anchor, Abstract3DView parentView) {
        super(anchor, parentView);
        mSphere.setRadius(RADIUS);
        add(mSphere);

        selectedProperty().addListener(change -> {
            updateProperties();
        });
    }

    public Anchor getAnchor() {
        return (Anchor) mModelObject;
    }

    protected ThreeDView getParentView() {
        return (ThreeDView) mParentView;
    }

    protected void updateProperties() {
        Color color = isSelected() ? SELECTED_OBJECTS_COLOR : Color.YELLOW;
        mSphere.setMaterial(new PhongMaterial(color));
    }

    protected enum Surface {
        S1
    }

    protected void updateNode() {
        Anchor anchor = getAnchor();

        Vector3D pos = CoordinateUtils.position3DToVector3D(anchor.getPosition3D(Length.ZERO));
        mSphere.setTranslateX(pos.getX());
        mSphere.setTranslateY(pos.getY());
        mSphere.setTranslateZ(pos.getZ());
    }

    @Override
    public void updateToModel() {
        super.updateToModel();
        updateNode();
        updateProperties();
    }
}
