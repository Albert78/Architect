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
package de.dh.cad.architect.model.objects;

import java.util.List;
import java.util.Optional;

import javax.xml.bind.annotation.XmlTransient;

import de.dh.cad.architect.model.changes.IModelChange;
import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.model.coords.LengthUnit;
import de.dh.cad.architect.model.coords.MathUtils;
import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.model.coords.Position3D;
import de.dh.cad.architect.model.coords.Vector3D;
import de.dh.cad.architect.utils.IdGenerator;

/**
 * A ceiling is a plane whose location is defined by three ceiling 3D (handle) anchors and which is limited by at least
 * three edge 2D (handle) anchors. This enables the model to decouple the anchors which define the ceiling's orientation
 * from the points which define the ceiling plane's extends.
 * It provides one surface which is shown on all sides.
 */
public class Ceiling extends BaseLimitedPlane {
    public static final String SURFACE_TYPE = "Surface";

    /**
     * First of the three ceiling (3D handle) anchors.
     */
    public static final String AP_CEILING_A = "Ceiling-A";

    /**
     * Second of the three ceiling (3D handle) anchors.
     */
    public static final String AP_CEILING_B = "Ceiling-B";

    /**
     * Third of the three ceiling (3D handle) anchors.
     */
    public static final String AP_CEILING_C = "Ceiling-C";

    public Ceiling() {
        // For JAXB
    }

    public Ceiling(String id, String name) {
        super(id, name);
    }

    @Override
    public void initializeSurfaces(List<IModelChange> changeTrace) {
        clearSurfaces(changeTrace);
        createSurface(SURFACE_TYPE, changeTrace);
    }

    /**
     * Creates a new ceiling with the given ceiling anchors; the ceiling anchors will also limit the ceiling's plane.
     */
    public static Ceiling create(String name, Position3D posA, Position3D posB, Position3D posC, IObjectsContainer ownerContainer, List<IModelChange> changeTrace) {
        Ceiling result = new Ceiling(IdGenerator.generateUniqueId(Ceiling.class), name);
        ownerContainer.addOwnedChild_Internal(result, changeTrace);
        result.createAnchor(AP_CEILING_A, posA, changeTrace);
        result.createAnchor(AP_CEILING_B, posB, changeTrace);
        result.createAnchor(AP_CEILING_C, posC, changeTrace);

        result.createEdgeAnchor(null, posA.projectionXY(), changeTrace);
        result.createEdgeAnchor(null, posB.projectionXY(), changeTrace);
        result.createEdgeAnchor(null, posC.projectionXY(), changeTrace);
        result.initializeSurfaces(changeTrace);
        return result;
    }

    /**
     * Calculates the Z coordinate in this ceiling's plane for the given {@code xyPosition}.
     */
    public static Optional<Length> getPlaneHeight(Position3D ceilingA, Position3D ceilingB, Position3D ceilingC, Position2D xyPosition) {
        // Ceiling plane
        Vector3D a = ceilingA.toVector();
        Vector3D b = ceilingB.toVector();
        Vector3D c = ceilingC.toVector();

        // Ceiling normal
        Vector3D n = b.minus(a).crossProduct(c.minus(a)).toUnitVector(LengthUnit.DEFAULT);

        Vector3D z1 = new Vector3D(Length.ofMM(0), Length.ofMM(0), Length.ofMM(1));

        return MathUtils.calculatePlaneLineIntersectionPoint(ceilingA, n, xyPosition.upscale(Length.ZERO), z1).map(Position3D::getZ);
    }

    @Override
    public Length getHeightAtPosition(Position2D xyPosition) {
        Position3D ceilingA = getAnchorA().requirePosition3D();
        Position3D ceilingB = getAnchorB().requirePosition3D();
        Position3D ceilingC = getAnchorC().requirePosition3D();
        return getPlaneHeight(ceilingA, ceilingB, ceilingC, xyPosition).orElse(Length.ZERO);
    }

    @XmlTransient
    public Anchor getAnchorA() {
        return getAnchorByAnchorType(AP_CEILING_A);
    }

    @XmlTransient
    public Anchor getAnchorB() {
        return getAnchorByAnchorType(AP_CEILING_B);
    }

    @XmlTransient
    public Anchor getAnchorC() {
        return getAnchorByAnchorType(AP_CEILING_C);
    }

    @Override
    public boolean isHandle(Anchor anchor) {
        if (super.isHandle(anchor)) {
            return true;
        }
        return isCeilingHandleAnchor(anchor);
    }

    public static boolean isCeilingHandleAnchor(Anchor anchor) {
        String anchorType = anchor.getAnchorType();
        return AP_CEILING_A.equals(anchorType) ||
                        AP_CEILING_B.equals(anchorType) ||
                        AP_CEILING_C.equals(anchorType);
    }

    @Override
    public boolean isPossibleDimensioningDockTargetAnchor(Anchor anchor) {
        return isEdgeHandleAnchor(anchor) || isCeilingHandleAnchor(anchor);
    }

    @Override
    protected String attrsToString() {
        return super.attrsToString() +
                        ", PosA = " + getAnchorByAnchorType(AP_CEILING_A).getPosition() +
                        "; PosB = " + getAnchorByAnchorType(AP_CEILING_B).getPosition() +
                        "; PosC = " + getAnchorByAnchorType(AP_CEILING_C).getPosition();
    }
}
