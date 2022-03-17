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
package de.dh.cad.architect.model.objects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import de.dh.cad.architect.model.ChangeSet;
import de.dh.cad.architect.model.coords.AnchorTarget;
import de.dh.cad.architect.model.coords.Dimensions2D;
import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.model.coords.LengthUnit;
import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.model.coords.Position3D;
import de.dh.cad.architect.model.coords.Vector2D;
import de.dh.cad.architect.model.jaxb.Dimensions2DJavaTypeAdapter;
import de.dh.cad.architect.model.jaxb.LengthJavaTypeAdapter;
import de.dh.cad.architect.model.wallmodel.WallDockEnd;
import de.dh.cad.architect.utils.IdGenerator;

public class WallHole extends BaseSolidObject {
    /**
     * Positions of the two wall hole sides as a length on the wall relative to wall end A.
     */
    public static class HoleAnchorPositions1D {
        protected final WallDockEnd mWallDockEnd;
        protected final Length mDistanceFromWallEnd;
        protected final Length mCornerA;
        protected final Length mCornerB;

        public HoleAnchorPositions1D(WallDockEnd wallDockEnd, Length distanceFromWallEnd, Length cornerA, Length cornerB) {
            mWallDockEnd = wallDockEnd;
            mDistanceFromWallEnd = distanceFromWallEnd;
            mCornerA = cornerA;
            mCornerB = cornerB;
        }

        public static HoleAnchorPositions1D fromParameters(Dimensions2D holeDimensions, WallDockEnd dockEnd, Length distanceFromWallEnd,
            Length wallSideLength) {
            Length lAFromA;
            if (dockEnd == WallDockEnd.A) {
                lAFromA = distanceFromWallEnd;
            } else if (dockEnd == WallDockEnd.B) {
                lAFromA = wallSideLength.minus(distanceFromWallEnd).minus(holeDimensions.getX());
            } else throw new RuntimeException("Unknown wall end '" + dockEnd + "'");

            Length lBFromA = lAFromA.plus(holeDimensions.getX());
            return new HoleAnchorPositions1D(dockEnd, distanceFromWallEnd, lAFromA, lBFromA);
        }

        public WallDockEnd getWallDockEnd() {
            return mWallDockEnd;
        }

        public Length getDistanceFromWallEnd() {
            return mDistanceFromWallEnd;
        }

        public Length getCornerA() {
            return mCornerA;
        }

        public Length getCornerB() {
            return mCornerB;
        }

        @Override
        public String toString() {
            return "HoleAnchorPositions1D [CornerA=" + mCornerA + ", CornerB=" + mCornerB + "]";
        }
    }

    /**
     * Positions of the four wall hole anchors in 3D space / projected to the wall.
     */
    public static class HoleAnchorPositions3D {
        protected final Position3D mCornerLA;
        protected final Position3D mCornerLB;
        protected final Position3D mCornerUA;
        protected final Position3D mCornerUB;

        public HoleAnchorPositions3D(Position3D cornerLA, Position3D cornerLB, Position3D cornerUA, Position3D cornerUB) {
            mCornerLA = cornerLA;
            mCornerLB = cornerLB;
            mCornerUA = cornerUA;
            mCornerUB = cornerUB;
        }

        public static HoleAnchorPositions3D mapToWallPositions(HoleAnchorPositions1D anchorPositions, Length parapetHeight, Length topHeight,
            Position2D wallHandleA, Position2D wallHandleB) {

            // Vectors in the plane of the wall
            Vector2D longSideU = wallHandleB.minus(wallHandleA).toUnitVector(LengthUnit.MM);

            Position3D cornerLA = new Position3D(
                wallHandleA.getX().plus(longSideU.getX().times(anchorPositions.getCornerA().inMM())),
                wallHandleA.getY().plus(longSideU.getY().times(anchorPositions.getCornerA().inMM())),
                parapetHeight);
            Position3D cornerLB = new Position3D(
                wallHandleA.getX().plus(longSideU.getX().times(anchorPositions.getCornerB().inMM())),
                wallHandleA.getY().plus(longSideU.getY().times(anchorPositions.getCornerB().inMM())),
                parapetHeight);
            return new HoleAnchorPositions3D(cornerLA, cornerLB, cornerLA.withZ(topHeight), cornerLB.withZ(topHeight));
        }

        public static HoleAnchorPositions3D mapToWall(HoleAnchorPositions1D anchorPositions, Length parapetHeight, Length topHeight, Wall wall) {
            // Positions in the ground (i.e. level) plane
            Position2D wallHandleA = wall.getAnchorWallHandleA().getPosition().projectionXY();
            Position2D wallHandleB = wall.getAnchorWallHandleB().getPosition().projectionXY();
            return mapToWallPositions(anchorPositions, parapetHeight, topHeight, wallHandleA, wallHandleB);
        }

        public static HoleAnchorPositions3D forWallHole(WallHole wallHole) {
            Wall wall = wallHole.getWall();
            Length wallSideLength = wall.calculateBaseLength();
            Dimensions2D holeDimensions = wallHole.getDimensions();
            WallDockEnd dockEnd = wallHole.getDockEnd();
            Length distanceFromWallEnd = wallHole.getDistanceFromWallEnd();
            HoleAnchorPositions1D hap1 = HoleAnchorPositions1D.fromParameters(holeDimensions, dockEnd, distanceFromWallEnd, wallSideLength);
            return HoleAnchorPositions3D.mapToWall(hap1, wallHole.getParapetHeight(), wallHole.getParapetHeight().plus(holeDimensions.getY()), wall);
        }

        public Position3D getCornerLA() {
            return mCornerLA;
        }

        public Position3D getCornerLB() {
            return mCornerLB;
        }

        public Position3D getCornerUA() {
            return mCornerUA;
        }

        public Position3D getCornerUB() {
            return mCornerUB;
        }

        @Override
        public String toString() {
            return "HoleAnchorPositions3D [CornerLA=" + mCornerLA + ", CornerLB=" + mCornerLB
                    + ", CornerUA=" + mCornerUA + ", CornerUB=" + mCornerUB + "]";
        }
    }

    protected static class HoleAnchors {
        Anchor CornerLA;
        Anchor CornerLB;
        Anchor CornerUA;
        Anchor CornerUB;
    }

    public static final String AP_CORNER_LA = "Corner-LA"; // Lower corner wall end A
    public static final String AP_CORNER_LB = "Corner-LB"; // Lower corner wall end B
    public static final String AP_CORNER_UA = "Corner-UA"; // Upper corner wall end A
    public static final String AP_CORNER_UB = "Corner-UB"; // Upper corner wall end B

    public static final String SURFACE_TYPE_EMBRASURE = "Surface-Embrasure";

    protected Length mParapetHeight;
    protected Dimensions2D mDimensions;
    protected WallDockEnd mDockEnd;
    protected Length mDistanceFromWallEnd;

    public WallHole() {
        // For JAXB
    }

    public WallHole(String id, String name, Length parapetHeight, Dimensions2D dimensions,
        WallDockEnd dockEnd, Length distanceFromWallEnd) {
        super(id, name);
        mParapetHeight = parapetHeight;
        mDimensions = dimensions;
        mDockEnd = dockEnd;
        mDistanceFromWallEnd = distanceFromWallEnd;
    }

    @Override
    public void initializeSurfaces() {
        clearSurfaces();
        createSurface(SURFACE_TYPE_EMBRASURE);
    }

    public static WallHole create(String name, Length parapetHeight, Dimensions2D dimensions,
        WallDockEnd dockEnd, Length distanceFromWallEnd, Wall wall, ChangeSet changeSet) {
        WallHole result = new WallHole(IdGenerator.generateUniqueId(WallHole.class), name, parapetHeight, dimensions, dockEnd, distanceFromWallEnd);
        wall.addWallHole_Internal(result, changeSet);
        // Those anchors are managed, they can only be changed by changing the other parameters
        result.createAnchor(AP_CORNER_LA, Position3D.zero(), changeSet);
        result.createAnchor(AP_CORNER_LB, Position3D.zero(), changeSet);
        result.createAnchor(AP_CORNER_UA, Position3D.zero(), changeSet);
        result.createAnchor(AP_CORNER_UB, Position3D.zero(), changeSet);
        return result;
    }

    public static WallHole createFromParameters(String name, Length parapetHeight, Dimensions2D dimensions, WallDockEnd dockEnd, Length distanceFromWallEnd,
        Wall wall, ChangeSet changeSet) {
        WallHole result = create(name, parapetHeight, dimensions, dockEnd, distanceFromWallEnd, wall, changeSet);
        HoleAnchorPositions3D hap3 = HoleAnchorPositions3D.forWallHole(result);
        Collection<AnchorTarget> anchorTargets = result.calculateAnchorTargets(hap3);
        result.updateAnchorPositions(anchorTargets, changeSet);
        changeSet.added(result);
        return result;
    }

    public Collection<AnchorTarget> calculateAnchorTargets(HoleAnchorPositions3D hap3) {
        Collection<AnchorTarget> result = new ArrayList<>();
        result.add(new AnchorTarget(getAnchorCornerLA(), hap3.getCornerLA()));
        result.add(new AnchorTarget(getAnchorCornerLB(), hap3.getCornerLB()));
        result.add(new AnchorTarget(getAnchorCornerUA(), hap3.getCornerUA()));
        result.add(new AnchorTarget(getAnchorCornerUB(), hap3.getCornerUB()));

        return result;
    }

    // Currently, we don't support an own reconcile method; our owner wall does this for us
    public Collection<AnchorTarget> calculateAnchorMoves(Map<String, AnchorTarget> handleAnchorTargets) {
        HoleAnchorPositions3D hap3 = HoleAnchorPositions3D.forWallHole(this);
        return calculateAnchorTargets(hap3);
    }

    @XmlTransient
    public Wall getWall() {
        return (Wall) getOwnerContainer();
    }

    @XmlJavaTypeAdapter(LengthJavaTypeAdapter.class)
    @XmlElement(name = "ParapetHeight")
    public Length getParapetHeight() {
        return mParapetHeight;
    }

    public void setParapetHeight(Length value) {
        mParapetHeight = value;
    }

    @XmlJavaTypeAdapter(Dimensions2DJavaTypeAdapter.class)
    @XmlElement(name = "Dimensions")
    public Dimensions2D getDimensions() {
        return mDimensions;
    }

    public void setDimensions(Dimensions2D value) {
        mDimensions = value;
    }

    @XmlElement(name = "DockEnd")
    public WallDockEnd getDockEnd() {
        return mDockEnd;
    }

    public void setDockEnd(WallDockEnd value) {
        mDockEnd = value;
    }

    @XmlJavaTypeAdapter(LengthJavaTypeAdapter.class)
    @XmlElement(name = "DistanceFromWallEnd")
    public Length getDistanceFromWallEnd() {
        return mDistanceFromWallEnd;
    }

    public void setDistanceFromWallEnd(Length value) {
        mDistanceFromWallEnd = value;
    }

    public Length getDistanceFromWallEndA(Length wallSideLength) {
        return mDockEnd == WallDockEnd.A ? mDistanceFromWallEnd : wallSideLength.minus(mDistanceFromWallEnd).minus(mDimensions.getX());
    }

    /**
     * Corner A at the bottom.
     */
    @XmlTransient
    public Anchor getAnchorCornerLA() {
        return getAnchorByAnchorType(AP_CORNER_LA);
    }

    /**
     * Corner B at the bottom.
     */
    @XmlTransient
    public Anchor getAnchorCornerLB() {
        return getAnchorByAnchorType(AP_CORNER_LB);
    }

    /**
     * Corner A at the top.
     */
    @XmlTransient
    public Anchor getAnchorCornerUA() {
        return getAnchorByAnchorType(AP_CORNER_UA);
    }

    /**
     * Corner B at the top.
     */
    @XmlTransient
    public Anchor getAnchorCornerUB() {
        return getAnchorByAnchorType(AP_CORNER_UB);
    }
}
