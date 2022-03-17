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
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import de.dh.cad.architect.model.ChangeSet;
import de.dh.cad.architect.model.coords.AnchorTarget;
import de.dh.cad.architect.model.coords.Dimensions2D;
import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.model.coords.Position3D;
import de.dh.cad.architect.model.jaxb.LengthJavaTypeAdapter;
import de.dh.cad.architect.model.jaxb.OptionalPositionJavaTypeAdapter;
import de.dh.cad.architect.model.objects.WallHole.HoleAnchorPositions1D;
import de.dh.cad.architect.model.objects.WallHole.HoleAnchorPositions3D;
import de.dh.cad.architect.model.wallmodel.AdaptedModelWall;
import de.dh.cad.architect.model.wallmodel.IWallAnchor;
import de.dh.cad.architect.model.wallmodel.WallAnchorPositions;
import de.dh.cad.architect.model.wallmodel.WallBevelType;
import de.dh.cad.architect.model.wallmodel.WallDockEnd;
import de.dh.cad.architect.model.wallmodel.WallEndView;
import de.dh.cad.architect.model.wallmodel.WallSurface;
import de.dh.cad.architect.utils.IdGenerator;

/**
 * A wall is straight element which spans from one end (defined by handle A) to the other end (defined by handle B).
 * The extent of a wall is defined by the two 2D handle anchors which are located in the middle of the wall ends, the wall's thickness and
 * it's heights at the two wall ends.
 * The handle anchors are used for docking with neighbour walls.
 * The wall's corner positions depend on the docking situation with neighbour walls, so that the four base corner positions
 * A1, B1, B2, A2 reflect the (2D) docking situation with neighbour walls. The wall sides 1 and 2 typically will have different lengths.
 * Those four (2D) corner positions are used to build up the four lower corner anchor positions (LA1, LB1, LB2, LA2) and the four upper
 * corner anchor positions (UA1, UB1, UB2, UA2). The four lower anchor positions are typically located at Z=0 while the four upper
 * anchors are located at the wall height; if the wall ends have different heights, the Z coordinate of the upper wall corner anchors will differ.
 * So in sum, the wall provides 10 anchors: 2 (2D) handles, 4 (3D) lower courners and 4 (3D) upper corners.
 *
 * The two ends are named wall end A and wall end B, those can be connected to adjacent walls with a configurable connection link.
 * The wall sides are facing the rooms which are limited by the wall, those are named wall side 1 and wall side 2.
 * The four surfaces are in clockwise direction: A, 1, B, 2:
 *
 * <pre>
 *   x--------- 1 ---------x
 *   |                     |
 *   A                     B
 *   |                     |
 *   x--------- 2 ---------x
 * </pre>
 */
public class Wall extends BaseSolidObject implements IObjectsContainer {
    public static final String AP_HANDLE_PREFIX = "Handle-";

    /**
     * Anchor type of wall handle located at the bottom in the middle of wall end A.
     */
    public static final String AP_HANDLE_A = AP_HANDLE_PREFIX + "A";

    /**
     * Anchor type of wall handle located at the bottom in the middle of wall end B.
     */
    public static final String AP_HANDLE_B = AP_HANDLE_PREFIX + "B";

    // Dock types of corner anchors, L=Lower, U=Upper, A/B=End, 1/2=Side
    public static final String AP_CORNER_L_PREFIX = "Corner-L";
    public static final String AP_CORNER_U_PREFIX = "Corner-U";
    public static final String AP_CORNER_LA1 = AP_CORNER_L_PREFIX + "A1";
    public static final String AP_CORNER_LA2 = AP_CORNER_L_PREFIX + "A2";
    public static final String AP_CORNER_LB1 = AP_CORNER_L_PREFIX + "B1";
    public static final String AP_CORNER_LB2 = AP_CORNER_L_PREFIX + "B2";
    public static final String AP_CORNER_UA1 = AP_CORNER_U_PREFIX + "A1";
    public static final String AP_CORNER_UA2 = AP_CORNER_U_PREFIX + "A2";
    public static final String AP_CORNER_UB1 = AP_CORNER_U_PREFIX + "B1";
    public static final String AP_CORNER_UB2 = AP_CORNER_U_PREFIX + "B2";

    public static final ObjectHealReason HEAL_OUTLINE = new ObjectHealReason() { /* empty */ };

    // TODO: Add field mLevel, see Floor
    protected Length mThickness;
    protected Length mHeightA;
    protected Length mHeightB;
    protected WallBevelType mWallBevelA = WallBevelType.Miter;
    protected WallBevelType mWallBevelB = WallBevelType.Miter;

    // Bevel apexes, if present. The bevel apexes are the visual point which is drawn if this wall
    // has bevel priority over the wall docked at the corresponding position, if the bevel type is Bevel.
    protected Optional<Position2D> mOA1BevelApex = Optional.empty();
    protected Optional<Position2D> mOA2BevelApex = Optional.empty();
    protected Optional<Position2D> mOB1BevelApex = Optional.empty();
    protected Optional<Position2D> mOB2BevelApex = Optional.empty();

    protected Collection<WallHole> mWallHoles = new ArrayList<>();
    protected Map<String, WallHole> mWallHoleById = new TreeMap<>();

    public Wall() {
        // For JAXB
    }

    public Wall(String id, String name, Length thickness, Length heightA, Length heightB) {
        super(id, name);
        mThickness = thickness;
        mHeightA = heightA;
        mHeightB = heightB;
    }

    @Override
    public void initializeSurfaces() {
        clearSurfaces();
        createSurface(WallSurface.Top.getSurfaceType());
        createSurface(WallSurface.Bottom.getSurfaceType());
        createSurface(WallSurface.A.getSurfaceType());
        createSurface(WallSurface.B.getSurfaceType());
        createSurface(WallSurface.One.getSurfaceType());
        createSurface(WallSurface.Two.getSurfaceType());
        createSurface(WallSurface.Embrasure.getSurfaceType());
    }

    public static Wall create(String name, Length thickness, Length heightA, Length heightB, Position2D handleA, Position2D handleB, IObjectsContainer ownerContainer, ChangeSet changeSet) {
        Wall result = new Wall(IdGenerator.generateUniqueId(Wall.class), name, thickness, heightA, heightB);
        ownerContainer.addOwnedChild_Internal(result, changeSet);
        result.createAnchor(AP_HANDLE_A, handleA, changeSet);
        result.createAnchor(AP_HANDLE_B, handleB, changeSet);
        result.createAnchor(AP_CORNER_LA1, Position3D.zero(), changeSet);
        result.createAnchor(AP_CORNER_LA2, Position3D.zero(), changeSet);
        result.createAnchor(AP_CORNER_LB1, Position3D.zero(), changeSet);
        result.createAnchor(AP_CORNER_LB2, Position3D.zero(), changeSet);
        result.createAnchor(AP_CORNER_UA1, Position3D.zero(), changeSet);
        result.createAnchor(AP_CORNER_UA2, Position3D.zero(), changeSet);
        result.createAnchor(AP_CORNER_UB1, Position3D.zero(), changeSet);
        result.createAnchor(AP_CORNER_UB2, Position3D.zero(), changeSet);
        return result;
    }

    public static Wall createFromHandlePositions(String name, Length thickness, Length heightA, Length heightB,
        Position2D handleA, Position2D handleB, IObjectsContainer ownerContainer, ChangeSet changeSet) {
        WallAnchorPositions anchorPositions = WallAnchorPositions.fromHandles(handleA, handleB, thickness, false, false);
        return createFromAnchorPositions(name, thickness, heightA, heightB, anchorPositions, ownerContainer, changeSet);
    }

    public static Wall createFromSide1(String name, Length thickness, Length heightA, Length heightB,
        Position2D cornerA1, Position2D cornerB1, IObjectsContainer ownerContainer, ChangeSet changeSet) {
        WallAnchorPositions anchorPositions = WallAnchorPositions.fromSide1(cornerA1, cornerB1, thickness, false, false);
        return createFromAnchorPositions(name, thickness, heightA, heightB, anchorPositions, ownerContainer, changeSet);
    }

    public static Wall createFromSide2(String name, Length thickness, Length heightA, Length heightB,
        Position2D cornerA1, Position2D cornerB1, IObjectsContainer ownerContainer, ChangeSet changeSet) {
        WallAnchorPositions anchorPositions = WallAnchorPositions.fromSide2(cornerA1, cornerB1, thickness, false, false);
        return createFromAnchorPositions(name, thickness, heightA, heightB, anchorPositions, ownerContainer, changeSet);
    }

    protected static Wall createFromAnchorPositions(String name, Length thickness, Length heightA, Length heightB,
        WallAnchorPositions anchorPositions,
        IObjectsContainer ownerContainer, ChangeSet changeSet) {
        Wall result = Wall.create(name, thickness, heightA, heightB, anchorPositions.getHandleA(), anchorPositions.getHandleB(), ownerContainer, changeSet);
        Collection<AnchorTarget> anchorTargets = result.calculateAnchorTargets(anchorPositions, true);
        result.updateAnchorPositions(anchorTargets, changeSet);
        result.setOA1BevelApex(anchorPositions.getOA1BevelApex());
        result.setOA2BevelApex(anchorPositions.getOA2BevelApex());
        result.setOB1BevelApex(anchorPositions.getOB1BevelApex());
        result.setOB2BevelApex(anchorPositions.getOB2BevelApex());

        return result;
    }

    @Override
    public void afterDeserialize(Object parent) {
        super.afterDeserialize(parent);
        for (WallHole wallHole : mWallHoles) {
            wallHole.setOwnerContainer_Internal(this);
            mWallHoleById.put(wallHole.getId(), wallHole);
        }
    }

    public Collection<AnchorTarget> calculateAnchorTargets(WallAnchorPositions anchorPositions, boolean includeHandleTargets) {
        Collection<AnchorTarget> result = new ArrayList<>();
        if (includeHandleTargets) {
            result.add(new AnchorTarget(getAnchorWallHandleA(), anchorPositions.getHandleA()));
            result.add(new AnchorTarget(getAnchorWallHandleB(), anchorPositions.getHandleB()));
        }

        result.add(new AnchorTarget(getAnchorWallCornerLA1(), anchorPositions.getCornerA1().upscale()));
        result.add(new AnchorTarget(getAnchorWallCornerLA2(), anchorPositions.getCornerA2().upscale()));
        result.add(new AnchorTarget(getAnchorWallCornerLB1(), anchorPositions.getCornerB1().upscale()));
        result.add(new AnchorTarget(getAnchorWallCornerLB2(), anchorPositions.getCornerB2().upscale()));

        result.add(new AnchorTarget(getAnchorWallCornerUA1(), anchorPositions.getCornerA1().upscale(mHeightA)));
        result.add(new AnchorTarget(getAnchorWallCornerUA2(), anchorPositions.getCornerA2().upscale(mHeightA)));
        result.add(new AnchorTarget(getAnchorWallCornerUB1(), anchorPositions.getCornerB1().upscale(mHeightB)));
        result.add(new AnchorTarget(getAnchorWallCornerUB2(), anchorPositions.getCornerB2().upscale(mHeightB)));

        return result;
    }

    public Optional<WallAnchorPositions> extractWallAnchorPositions() {
        return Optional.of(new WallAnchorPositions(
            getAnchorWallHandleA().requirePosition2D(),
            getAnchorWallHandleB().requirePosition2D(),
            hasNeighborWallA(),
            hasNeighborWallB(),
            getAnchorWallCornerLA1().getPosition().projectionXY(), mOA1BevelApex,
            getAnchorWallCornerLA2().getPosition().projectionXY(), mOA2BevelApex,
            getAnchorWallCornerLB1().getPosition().projectionXY(), mOB1BevelApex,
            getAnchorWallCornerLB2().getPosition().projectionXY(), mOB2BevelApex));
    }

    protected Collection<AnchorTarget> updateAnchors(ChangeSet changeSet) {
        Optional<WallAnchorPositions> oWallAnchorPositions = WallAnchorPositions.calculateDockSituation(new AdaptedModelWall(this, Optional.empty()));
        if (oWallAnchorPositions.isEmpty()) {
            throw new IllegalStateException("Cannot move anchors according given handle positions");
        }
        WallAnchorPositions anchorPositions = oWallAnchorPositions.get();

        Collection<AnchorTarget> anchorTargets = calculateAnchorTargets(anchorPositions, false);
        updateAnchorPositions(anchorTargets, changeSet);
        setOA1BevelApex(anchorPositions.getOA1BevelApex());
        setOA2BevelApex(anchorPositions.getOA2BevelApex());
        setOB1BevelApex(anchorPositions.getOB1BevelApex());
        setOB2BevelApex(anchorPositions.getOB2BevelApex());
        return anchorTargets;
    }

    @Override
    public ReconcileResult reconcileAfterHandleChange(ChangeSet changeSet) throws IllegalStateException {
        Anchor handleA = getAnchorWallHandleA();
        Anchor handleB = getAnchorWallHandleB();

        Collection<AnchorTarget> anchorTargets = updateAnchors(changeSet);
        changeSet.changed(this);

        for (WallHole hole : mWallHoles) {
            Length wallSideLength = hole.getWall().calculateBaseLength();
            Dimensions2D holeDimensions = hole.getDimensions();
            WallDockEnd dockEnd = hole.getDockEnd();
            Length distanceFromWallEnd = hole.getDistanceFromWallEnd();
            HoleAnchorPositions1D hap1 = HoleAnchorPositions1D.fromParameters(holeDimensions, dockEnd, distanceFromWallEnd, wallSideLength);
            HoleAnchorPositions3D hap3 = HoleAnchorPositions3D.mapToWallPositions(hap1,
                hole.getParapetHeight(), hole.getParapetHeight().plus(holeDimensions.getY()), handleA.requirePosition2D(), handleB.requirePosition2D());

            Collection<AnchorTarget> holeAnchorTargets = hole.calculateAnchorTargets(hap3);
            hole.updateAnchorPositions(holeAnchorTargets, changeSet);
            anchorTargets.addAll(holeAnchorTargets);
        }

        Collection<Wall> changedNeighbourWalls = new ArrayList<>();
        changedNeighbourWalls.addAll(getDockedNeighbourWalls(handleA));
        changedNeighbourWalls.addAll(getDockedNeighbourWalls(handleB));

        MultiValuedMap<BaseAnchoredObject, ObjectHealReason> healObjects = new ArrayListValuedHashMap<>();
        for (Wall wall : changedNeighbourWalls) {
            healObjects.put(wall, HEAL_OUTLINE);
        }

        Collection<Anchor> changedDependentAnchors = new ArrayList<>();
        for (AnchorTarget anchorTarget : anchorTargets) {
            Anchor anchor = anchorTarget.getAnchor();
            if (!anchor.isHandle()) {
                changedDependentAnchors.add(anchor);
            }
        }

        return new ReconcileResult(changedDependentAnchors, healObjects);
    }

    @Override
    public void healObject(Collection<ObjectHealReason> healReasons, ChangeSet changeSet) {
        updateAnchors(changeSet);
    }

    @Override
    public boolean isPossibleDimensioningDockTargetAnchor(Anchor anchor) {
        return isLowerWallCornerAnchor(anchor);
    }

    @Override
    public boolean isHandle(Anchor anchor) {
        return isWallHandleAnchor(anchor);
    }

    /**
     * Gets the wall bevel of wall end A. Since multiple wall ends join together, the bevel of
     * the wall end is used which supplies the first handle of the handle join.
     * @see WallAnchorPositions#getWallBevelTypeOfAnchorDock(IWallAnchor)
     */
    @XmlElement(name = "WallBevelA")
    public WallBevelType getWallBevelA() {
        return mWallBevelA;
    }

    /**
     * Sets the wall bevel for this wall's side A.
     * @see WallAnchorPositions#setWallBevelTypeOfAnchorDock(Anchor, WallBevelType, ChangeSet)
     */
    public void setWallBevelA(WallBevelType value) {
        mWallBevelA = value;
    }

    /**
     * Gets the wall bevel of wall end B. Since multiple wall ends join together, the bevel of
     * the wall end is used which supplies the first handle of the handle join.
     * @see WallAnchorPositions#getWallBevelTypeOfAnchorDock(IWallAnchor)
     */
    @XmlElement(name = "WallBevelB")
    public WallBevelType getWallBevelB() {
        return mWallBevelB;
    }

    /**
     * Sets the wall bevel for this wall's side B.
     * @see WallAnchorPositions#setWallBevelTypeOfAnchorDock(Anchor, WallBevelType, ChangeSet)
     */
    public void setWallBevelB(WallBevelType value) {
        mWallBevelB = value;
    }

    @Override
    public Collection<? extends BaseObject> getOwnedChildren() {
        return mWallHoles;
    }

    @Override
    public Collection<? extends BaseObject> delete(ChangeSet changeSet) {
        Collection<BaseObject> result = new ArrayList<>();
        for (BaseObject object : new ArrayList<>(getOwnedChildren())) {
            result.addAll(object.delete(changeSet));
        }
        result.addAll(super.delete(changeSet));
        return result;
    }

    @Override
    public BaseObject getObjectById(String id) {
        if (mId.equals(id)) {
            return this;
        }
        return mWallHoleById.get(id);
    }

    public void addWallHole_Internal(WallHole wallHole, ChangeSet changeSet) {
        wallHole.setOwnerContainer_Internal(this);
        changeSet.added(wallHole);
        mWallHoles.add(wallHole);
        mWallHoleById.put(wallHole.getId(), wallHole);
    }

    @Override
    public void addOwnedChild_Internal(BaseObject child, ChangeSet changeSet) {
        if (!(child instanceof WallHole)) {
            throw new RuntimeException("Child object <" + child + "> cannot be added to wall");
        }
        addWallHole_Internal((WallHole) child, changeSet);
    }

    public void removeWallHole_Internal(WallHole wallHole, ChangeSet changeSet) {
        wallHole.setOwnerContainer_Internal(null);
        changeSet.removed(wallHole);
        changeSet.changed(this);
        mWallHoles.remove(wallHole);
        mWallHoleById.remove(wallHole.getId());
    }

    @Override
    public Collection<BaseObject> removeOwnedChild_Internal(BaseObject child, ChangeSet changeSet) {
        if (!(child instanceof WallHole)) {
            throw new RuntimeException("Child object <" + child + "> cannot be removed from wall");
        }
        removeWallHole_Internal((WallHole) child, changeSet);
        return Collections.singleton(child);
    }

    public WallHole getWallHoleById(String id) {
        return mWallHoleById.get(id);
    }

    @XmlJavaTypeAdapter(LengthJavaTypeAdapter.class)
    @XmlElement(name = "Thickness")
    public Length getThickness() {
        return mThickness;
    }

    public void setThickness(Length value) {
        mThickness = value;
    }

    @XmlJavaTypeAdapter(LengthJavaTypeAdapter.class)
    @XmlElement(name = "HeightA")
    public Length getHeightA() {
        return mHeightA;
    }

    public void setHeightA(Length value) {
        mHeightA = value;
    }

    @XmlJavaTypeAdapter(LengthJavaTypeAdapter.class)
    @XmlElement(name = "HeightB")
    public Length getHeightB() {
        return mHeightB;
    }

    public void setHeightB(Length value) {
        mHeightB = value;
    }

    @XmlElementWrapper(name = "WallHoles")
    @XmlElement(name = "Hole")
    public Collection<WallHole> getWallHoles() {
        return mWallHoles;
    }

    /**
     * Handle at wall bottom end A.
     */
    @XmlTransient
    public Anchor getAnchorWallHandleA() {
        return getAnchorByAnchorType(AP_HANDLE_A);
    }

    /**
     * Handle at wall bottom end B.
     */
    @XmlTransient
    public Anchor getAnchorWallHandleB() {
        return getAnchorByAnchorType(AP_HANDLE_B);
    }

    protected static boolean hasNeighborWallAtHandle(Anchor handleAnchor) {
        return handleAnchor.getAllDockedAnchors()
                        .stream()
                        .filter(a -> a.getAnchorOwner() instanceof Wall && !handleAnchor.equals(a)) // All other wall anchors
                        .findAny()
                        .isPresent();
    }

    public boolean hasNeighborWallA() {
        return hasNeighborWallAtHandle(getAnchorWallHandleA());
    }

    public boolean hasNeighborWallB() {
        return hasNeighborWallAtHandle(getAnchorWallHandleB());
    }

    /**
     * Corner 1 at wall bottom end A.
     */
    @XmlTransient
    public Anchor getAnchorWallCornerLA1() {
        return getAnchorByAnchorType(AP_CORNER_LA1);
    }

    /**
     * Corner 2 at wall bottom end A.
     */
    @XmlTransient
    public Anchor getAnchorWallCornerLA2() {
        return getAnchorByAnchorType(AP_CORNER_LA2);
    }

    /**
     * Corner 1 at wall bottom end B.
     */
    @XmlTransient
    public Anchor getAnchorWallCornerLB1() {
        return getAnchorByAnchorType(AP_CORNER_LB1);
    }

    /**
     * Corner 2 at wall bottom end B.
     */
    @XmlTransient
    public Anchor getAnchorWallCornerLB2() {
        return getAnchorByAnchorType(AP_CORNER_LB2);
    }

    /**
     * Corner 1 at wall top end A.
     */
    @XmlTransient
    public Anchor getAnchorWallCornerUA1() {
        return getAnchorByAnchorType(AP_CORNER_UA1);
    }

    /**
     * Corner 2 at wall top end A.
     */
    @XmlTransient
    public Anchor getAnchorWallCornerUA2() {
        return getAnchorByAnchorType(AP_CORNER_UA2);
    }

    /**
     * Corner 1 at wall top end B.
     */
    @XmlTransient
    public Anchor getAnchorWallCornerUB1() {
        return getAnchorByAnchorType(AP_CORNER_UB1);
    }

    public WallEndView getWallEndView(WallDockEnd dockEnd) {
        return dockEnd == WallDockEnd.A
                        ? WallEndView.fromWallHandle(getAnchorWallHandleA())
                        : WallEndView.fromWallHandle(getAnchorWallHandleB());
    }

    /**
     * Corner 2 at wall top end B.
     */
    @XmlTransient
    public Anchor getAnchorWallCornerUB2() {
        return getAnchorByAnchorType(AP_CORNER_UB2);
    }

    @XmlJavaTypeAdapter(OptionalPositionJavaTypeAdapter.class)
    @XmlElement(name = "A1BevelApex")
    public Optional<Position2D> getOA1BevelApex() {
        return mOA1BevelApex;
    }

    public void setOA1BevelApex(Optional<Position2D> value) {
        mOA1BevelApex = value;
    }

    @XmlJavaTypeAdapter(OptionalPositionJavaTypeAdapter.class)
    @XmlElement(name = "A2BevelApex")
    public Optional<Position2D> getOA2BevelApex() {
        return mOA2BevelApex;
    }

    public void setOA2BevelApex(Optional<Position2D> value) {
        mOA2BevelApex = value;
    }

    @XmlJavaTypeAdapter(OptionalPositionJavaTypeAdapter.class)
    @XmlElement(name = "B1BevelApex")
    public Optional<Position2D> getOB1BevelApex() {
        return mOB1BevelApex;
    }

    public void setOB1BevelApex(Optional<Position2D> value) {
        mOB1BevelApex = value;
    }

    @XmlJavaTypeAdapter(OptionalPositionJavaTypeAdapter.class)
    @XmlElement(name = "B2BevelApex")
    public Optional<Position2D> getOB2BevelApex() {
        return mOB2BevelApex;
    }

    public void setOB2BevelApex(Optional<Position2D> value) {
        mOB2BevelApex = value;
    }

    /**
     * Returns the length between wall handle A and wall handle B.
     */
    public Length calculateBaseLength() {
        return getAnchorWallHandleA().requirePosition2D().distance(getAnchorWallHandleB().requirePosition2D());
    }

    /**
     * Returns the length between wall anchor A1 and wall anchor B1.
     */
    public Length calculateLengthSide1() {
        return getAnchorWallCornerLA1().projectionXY().distance(getAnchorWallCornerLB1().projectionXY());
    }

    /**
     * Returns the length between wall anchor A2 and wall anchor B2.
     */
    public Length calculateLengthSide2() {
        return getAnchorWallCornerLA2().projectionXY().distance(getAnchorWallCornerLB2().projectionXY());
    }

    public static boolean isWallHandleAnchor(Anchor anchor) {
        String anchorType = anchor.getAnchorType();
        return anchorType != null && anchorType.startsWith(AP_HANDLE_PREFIX);
    }

    public static boolean isWallHandleAnchorA(Anchor anchor) {
        String anchorType = anchor.getAnchorType();
        return anchorType != null && anchorType.equals(AP_HANDLE_A);
    }

    public static boolean isWallHandleAnchorB(Anchor anchor) {
        String anchorType = anchor.getAnchorType();
        return anchorType != null && anchorType.equals(AP_HANDLE_B);
    }

    public static boolean isLowerWallCornerAnchor(Anchor anchor) {
        String anchorType = anchor.getAnchorType();
        return anchorType != null && anchorType.startsWith(AP_CORNER_L_PREFIX);
    }

    public static boolean isUpperWallCornerAnchor(Anchor anchor) {
        String anchorType = anchor.getAnchorType();
        return anchorType != null && anchorType.startsWith(AP_CORNER_U_PREFIX);
    }

    public static WallDockEnd getWallEndOfHandle(Anchor wallHandleAnchor) {
        if (!isWallHandleAnchor(wallHandleAnchor)) {
            throw new IllegalArgumentException("The given anchor <" + wallHandleAnchor + "> is not a wall handle");
        }
        return isWallHandleAnchorA(wallHandleAnchor)
                        ? WallDockEnd.A
                        : WallDockEnd.B;
    }

    /**
     * Returns all neighbour walls which are docked to the given wall handle anchor.
     * The owner wall of the given handle anchor is not included in the result.
     */
    public static Collection<Wall> getDockedNeighbourWalls(Anchor wallHandleAnchor) {
        Collection<Wall> result = new TreeSet<>();
        for (Anchor dockedAnchor : wallHandleAnchor.getAllDockedAnchors()) {
            if (dockedAnchor.equals(wallHandleAnchor)) {
                continue;
            }
            BaseAnchoredObject anchorOwner = dockedAnchor.getAnchorOwner();
            if (anchorOwner instanceof Wall wall) {
                result.add(wall);
            }
        }
        return result;
    }

    /**
     * Returns all neighbour walls which are docked to the given wall.
     * The given wall is not included in the result.
     */
    public static Collection<Wall> getDockedNeighbourWalls(Wall bo) {
        Collection<Wall> result = new TreeSet<>(getDockedNeighbourWalls(bo.getAnchorWallHandleA()));
        result.addAll(getDockedNeighbourWalls(bo.getAnchorWallHandleB()));
        return result;
    }
}
