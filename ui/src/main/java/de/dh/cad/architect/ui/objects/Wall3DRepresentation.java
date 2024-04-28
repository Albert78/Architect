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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import de.dh.cad.architect.model.assets.AssetRefPath;
import de.dh.cad.architect.model.coords.Dimensions2D;
import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.model.objects.SurfaceConfiguration;
import de.dh.cad.architect.model.objects.Wall;
import de.dh.cad.architect.model.objects.WallHole;
import de.dh.cad.architect.model.wallmodel.WallAnchorPositions;
import de.dh.cad.architect.model.wallmodel.WallOutline;
import de.dh.cad.architect.model.wallmodel.WallOutlineConnection;
import de.dh.cad.architect.model.wallmodel.WallOutlineCorner;
import de.dh.cad.architect.model.wallmodel.WallSurface;
import de.dh.cad.architect.ui.assets.AssetLoader;
import de.dh.cad.architect.ui.utils.CoordinateUtils;
import de.dh.cad.architect.ui.view.threed.Abstract3DView;
import de.dh.cad.architect.ui.view.threed.ThreeDView;
import de.dh.utils.MaterialMapping;
import de.dh.utils.Vector2D;
import de.dh.utils.csg.CSGSurfaceAwareAddon;
import de.dh.utils.csg.CSGs;
import de.dh.utils.csg.CSGs.ExtrusionSurfaceDataProvider;
import de.dh.utils.io.MeshData;
import de.dh.utils.io.fx.FxMeshBuilder;
import eu.mihosoft.jcsg.CSG;
import eu.mihosoft.vvecmath.Vector3d;
import javafx.beans.value.ChangeListener;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Mesh;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Shape3D;

public class Wall3DRepresentation extends AbstractSolid3DRepresentation {
    protected static final double EPSILON = 0.01;

    public Wall3DRepresentation(Wall wall, Abstract3DView parentView) {
        super(wall, parentView);
        for (SurfaceConfiguration surfaceConfig : wall.getSurfaceConfigurations()) {
            String surfaceTypeId = surfaceConfig.getSurfaceTypeId();

            // 3D wall model consists of multiple MeshViews, one for each surface type. This allows us to assign
            // different materials and to match mouse clicks to a surface.
            MeshView meshView = new MeshView();

            SurfaceData<MeshView> sd = new SurfaceData<>(this, surfaceTypeId, meshView);
            meshView.getTransforms().addFirst(CoordinateUtils.createTransformArchitectToJavaFx());
            add(meshView);
            registerSurface(sd);
        }

        ChangeListener<Boolean> bPropertiesUpdaterListener = (observable, oldValue, newValue) -> updateProperties();
        selectedProperty().addListener(bPropertiesUpdaterListener);
        objectFocusedProperty().addListener(bPropertiesUpdaterListener);
        objectEmphasizedProperty().addListener(bPropertiesUpdaterListener);
    }

    public Wall getWall() {
        return (Wall) mModelObject;
    }

    protected ThreeDView getParentView() {
        return (ThreeDView) mParentView;
    }

    protected void updateProperties() {
        Wall wall = getWall();
        AssetLoader assetLoader = getAssetLoader();
        for (SurfaceData<? extends Shape3D> surfaceData : mSurfacesByTypeId.values()) {
            SurfaceConfiguration surfaceConfiguration = wall.getSurfaceTypeIdsToSurfaceConfigurations().get(surfaceData.getSurfaceTypeId());
            de.dh.cad.architect.model.objects.MaterialMappingConfiguration mmc = surfaceConfiguration.getMaterialMappingConfiguration();
            PhongMaterial material = assetLoader.buildMaterial(mmc, surfaceData.getSurfaceSize());
            surfaceData.setMaterial(material);
        }
    }

    /**
     * Removes duplicate points. We don't remove points in the middle of a line segment,
     * those might belong to different wall connections and thus, we need those as different
     * entries to be able to attach different materials.
     */
    protected static boolean cleanupWallOutlineCorners(List<WallOutlineCorner> corners) {
        if (corners.size() < 3) {
            // Polygon must have at least 3 points
            return false;
        }
        // Build points list which exactly mirrors the corners list - and will be modified the same way
        List<Vector2D> points = corners
                        .stream()
                        .map(c -> new Vector2D(
                            c.getPosition().getX().inInternalFormat(),
                            c.getPosition().getY().inInternalFormat()))
                        .collect(Collectors.toCollection(ArrayList::new));
        int i = 0;
        while (i < points.size()) {
            int i1 = i;
            int i2 = (i + 1) % points.size();
            Vector2D p1 = points.get(i1);
            Vector2D p2 = points.get(i2);
            if (p2.minus(p1).getLength() < EPSILON) {
                points.remove(i2);
                if (points.size() < 3) {
                    // All points are collinear
                    return false;
                }
                WallOutlineCorner first = corners.get(i1);
                WallOutlineCorner second = corners.remove(i2);
                second.getPrevious().getPrevious().setNext(second.getNext());
                second.getNext().setPrevious(first);
            } else {
                i++;
            }
        }
        return true;
    }

    protected CSG createWallCSG(Wall wall, List<WallOutlineCorner> outlineCornersCW) {
        if (!cleanupWallOutlineCorners(outlineCornersCW)) {
            return null;
        }

        // We later work on the reverted points direction, so here we're interested in the index AFTER the surface change.
        // That's why we check the surfaces at the opposite side.
        WallSurface lastSurface = outlineCornersCW.get(0).getNext().getSurface();
        int firstSurfaceChange = -1;
        for (int i = 0; i < outlineCornersCW.size(); i++) {
            WallOutlineConnection currentConnection = outlineCornersCW.get(i).getPrevious();
            if (firstSurfaceChange == -1 && currentConnection.getSurface() != lastSurface) {
                firstSurfaceChange = i; // Found the first surface change - this is the start index for the extrusion process
            }
        }
        if (firstSurfaceChange == -1) {
            firstSurfaceChange = 0; // No surface change, start at index 0
        }

        double heightA = CoordinateUtils.lengthToCoords(wall.getHeightA(), null);
        double heightB = CoordinateUtils.lengthToCoords(wall.getHeightB(), null);
        Position2D handleA = wall.getAnchorWallHandleA().getPosition().projectionXY();
        Position2D handleB = wall.getAnchorWallHandleB().getPosition().projectionXY();

        Vector2D handleAPos = CoordinateUtils.positionToVector2D(handleA, false);
        Vector2D handleBPos = CoordinateUtils.positionToVector2D(handleB, false);

        Vector3d wallDirectionAB = CoordinateUtils.vector2DToVecMathVector3d(handleB.minus(handleA));
        double wallLength = wallDirectionAB.magnitude();
        Vector2D upperHandle = heightA > heightB ? handleAPos : handleBPos;
        Vector2D lowerHandle = heightA > heightB ? handleBPos : handleAPos;
        double diffH = Math.abs(heightA - heightB);
        double diffM = diffH / (wallLength == 0 ? 1 : wallLength); // Actually, the wall length cannot be 0, can it?
        Vector2D descendingDirection = lowerHandle.minus(upperHandle).toUnitVector();

        double baseExtrudeHeight = Math.max(heightA, heightB);

        // Those are in the coordinate system of JavaFX
        List<Vector3d> topPolygonPointsCW = new ArrayList<>();
        List<Vector3d> bottomPolygonPointsCW = new ArrayList<>();

        // This is in model coordinate system
        List<Position2D> basePointsCW = outlineCornersCW
                        .stream()
                        .map(WallOutlineCorner::getPosition)
                        .collect(Collectors.toCollection(ArrayList::new));

        for (Position2D pos : basePointsCW) {
            Vector2D vv = new Vector2D(CoordinateUtils.lengthToCoords(pos.getX(), null), CoordinateUtils.lengthToCoords(pos.getY(), null));

            // Calculate top level; projection of top points to a plane which spans between the two upper handle positions.
            // This is necessary if the wall has different heights at the wall ends.
            double t = vv.minus(upperHandle).dotProduct(descendingDirection);

            Vector3d topPoint = Vector3d.xyz(vv.getX(), vv.getY(), (baseExtrudeHeight - diffM * t));

            // TODO: Bottom level is currently simply at Z=0, should be at the same level as our floor
            Vector3d bottomPoint = Vector3d.xyz(vv.getX(), vv.getY(), 0);

            topPolygonPointsCW.add(topPoint);
            bottomPolygonPointsCW.add(bottomPoint);
        }

        // Wall fragment without clipping of the top
        ExtrusionSurfaceDataProvider<WallSurface> wallSurfaceDataProvider = new ExtrusionSurfaceDataProvider<>() {
            @Override
            public List<Vector3d> getTopPolygonPointsCW() {
                return topPolygonPointsCW;
            }

            @Override
            public List<Vector3d> getBottomPolygonPointsCW() {
                return bottomPolygonPointsCW;
            }

            @Override
            public Vector3d getTopPolygonTextureDirectionX() {
                return wallDirectionAB;
            }

            @Override
            public Vector3d getBottomPolygonTextureDirectionX() {
                return wallDirectionAB;
            }

            @Override
            public WallSurface getSurfaceCW(int startPointIndex) {
                return outlineCornersCW.get(startPointIndex).getNext().getSurface();
            }

            @Override
            public WallSurface getTopSurface() {
                return WallSurface.Top;
            }

            @Override
            public WallSurface getBottomSurface() {
                return WallSurface.Bottom;
            }
        };
        mSurfacesByTypeId.get(WallSurface.Top.getSurfaceType()).setSurfaceSize(wallSurfaceDataProvider.getTopPolygonTextureProjection().getSpannedSize());
        mSurfacesByTypeId.get(WallSurface.Bottom.getSurfaceType()).setSurfaceSize(wallSurfaceDataProvider.getBottomPolygonTextureProjection().getSpannedSize());

        double thicknessC = CoordinateUtils.lengthToCoords(wall.getThickness(), null);
        double maxWallHeightC = Math.max(heightA, heightB);
        double lengthSideOne_C = CoordinateUtils.lengthToCoords(wall.getAnchorWallCornerLA1().requirePosition3D().distance(wall.getAnchorWallCornerLB1().requirePosition3D()), null);
        double lengthSideTwo_C = CoordinateUtils.lengthToCoords(wall.getAnchorWallCornerLA2().requirePosition3D().distance(wall.getAnchorWallCornerLB2().requirePosition3D()), null);

        mSurfacesByTypeId.get(WallSurface.One.getSurfaceType()).setSurfaceSize(new Vector2D(lengthSideOne_C, maxWallHeightC));
        mSurfacesByTypeId.get(WallSurface.Two.getSurfaceType()).setSurfaceSize(new Vector2D(lengthSideTwo_C, maxWallHeightC));
        mSurfacesByTypeId.get(WallSurface.A.getSurfaceType()).setSurfaceSize(new Vector2D(thicknessC, heightA));
        mSurfacesByTypeId.get(WallSurface.B.getSurfaceType()).setSurfaceSize(new Vector2D(thicknessC, heightB));

        return CSGs.extrudeSurfaces(wallSurfaceDataProvider, firstSurfaceChange, true);
    }

    protected void configureForInvalidWall() {
        for (SurfaceData<? extends Shape3D> sd : mSurfacesByTypeId.values()) {
            ((MeshView) sd.getShape()).setMesh(null);
        }
    }

    protected void updateNode() {
        Wall wall = getWall();

        Vector2D pA = CoordinateUtils.positionToVector2D(wall.getAnchorWallHandleA().requirePosition2D(), false);
        Vector2D pB = CoordinateUtils.positionToVector2D(wall.getAnchorWallHandleB().requirePosition2D(), false);

        Length wallBaseLengthL = wall.calculateBaseLength();
        double wallBaseLengthC = CoordinateUtils.lengthToCoords(wallBaseLengthL, null);

        Vector2D longEdgeWall = pB.minus(pA);
        Vector2D shortEdgeWall = longEdgeWall.getNormalCW().scaleToLength(wallBaseLengthC);
        Vector2D longEdgeWallU = longEdgeWall.toUnitVector();


        Optional<WallAnchorPositions> oWap = wall.extractWallAnchorPositions();
        Optional<WallOutline> oWallOutlineCW = oWap.map(WallAnchorPositions::calculateWallOutlineCW);
        if (oWallOutlineCW.isPresent()) {
            WallOutline wallOutlineCW = oWallOutlineCW.get();
            CSG csg = createWallCSG(wall, wallOutlineCW.getCornersAsList());
            if (csg == null) {
                configureForInvalidWall();
                return;
            }

            for (WallHole wallHole : wall.getWallHoles()) {
                double distanceFromWallEndA = CoordinateUtils.lengthToCoords(wallHole.getDistanceFromWallEndA(wallBaseLengthL), null);
                Dimensions2D holeDimensions = wallHole.getDimensions();
                double holeWidthC = CoordinateUtils.lengthToCoords(holeDimensions.getX(), null);
                double holeHeightC = CoordinateUtils.lengthToCoords(holeDimensions.getY(), null);
                double holeParapetHeightC = CoordinateUtils.lengthToCoords(wallHole.getParapetHeight(), null);

                Vector2D windowStartMiddle = pA.plus(longEdgeWallU.times(distanceFromWallEndA));
                Vector2D windowEndMiddle = windowStartMiddle.plus(longEdgeWallU.times(holeWidthC));
                Vector2D a1p = windowStartMiddle.minus(shortEdgeWall); // Window a1 plus x overhang for other walls bevels, if any. TODO: Calculate the overhang according to other wall's thickness.
                Vector2D a2p = windowStartMiddle.plus(shortEdgeWall);
                Vector2D b1p = windowEndMiddle.minus(shortEdgeWall);
                Vector2D b2p = windowEndMiddle.plus(shortEdgeWall);

                // Attention: Y decreases to the top in JavaFX
                List<Vector3d> bottomPoints = Arrays.asList(
                    Vector3d.xyz(b1p.getX(), b1p.getY(), holeParapetHeightC),
                    Vector3d.xyz(b2p.getX(), b2p.getY(), holeParapetHeightC),
                    Vector3d.xyz(a2p.getX(), a2p.getY(), holeParapetHeightC),
                    Vector3d.xyz(a1p.getX(), a1p.getY(), holeParapetHeightC)
                    );
                List<Vector3d> topPoints = bottomPoints
                                .stream()
                                .map(p -> Vector3d.xyz(p.getX(), p.getY(), (holeParapetHeightC + holeHeightC)))
                                .collect(Collectors.toList());

                Vector3d textureDirectionX = Vector3d.xy(longEdgeWall.getX(), longEdgeWall.getY());
                CSG holeCSG = CSGs.extrudeSurfaces(
                    new ExtrusionSurfaceDataProvider<WallSurface>() {
                        @Override
                        public List<Vector3d> getTopPolygonPointsCW() {
                            return topPoints;
                        }

                        @Override
                        public List<Vector3d> getBottomPolygonPointsCW() {
                            return bottomPoints;
                        }

                        @Override
                        public Vector3d getTopPolygonTextureDirectionX() {
                            return textureDirectionX;
                        }

                        @Override
                        public Vector3d getBottomPolygonTextureDirectionX() {
                            return textureDirectionX;
                        }

                        @Override
                        public WallSurface getSurfaceCW(int startPointIndex) {
                            return WallSurface.Embrasure;
                        }

                        @Override
                        public WallSurface getTopSurface() {
                            return WallSurface.Embrasure;
                        }

                        @Override
                        public WallSurface getBottomSurface() {
                            return WallSurface.Embrasure;
                        }
                    }, 0, false);
                csg = csg.difference(holeCSG);
            }

            // TODO: To get a correct material mapping to our window and door embrasures, we would need to have separate
            //  entries for all embrasure sides of all holes.
            //mSurfacesByTypeId.get(WallSurface.Embrasure.getSurfaceType()).setSurfaceSize(...);

            Map<WallSurface, MeshData> meshes = CSGSurfaceAwareAddon.createMeshes(csg, Optional.empty());
            for (SurfaceData<? extends Shape3D> surfaceData : mSurfacesByTypeId.values()) {
                // One surface of the wall, e.g. A or One
                String surfaceTypeId = surfaceData.getSurfaceTypeId();
                WallSurface wallSurface = WallSurface.ofWallSurfaceType(surfaceTypeId);
                MeshData meshData = meshes.get(wallSurface);
                if (meshData == null) { // E.g. wall contains no embrasures
                    continue;
                }
                MeshView meshView = (MeshView) surfaceData.getShape();
                // The CSG builder has generated the mesh in a way that the texture coordinates of the surface parts map
                // to their corresponding part of the overall surface texture, as if the texture would be a wallpaper.
                // E.g. if wall side 1 extends over two surface parts, the main side 1 surface and the corner bevel apex, the algorithm places the texture coords
                // to cover both surface parts, i.e. texture coords (0; 0) at the beginning of part 1 and texture coords (1; 1) at the end of part 2.
                Mesh mesh = FxMeshBuilder.buildMesh(meshData);
                meshView.setMesh(mesh);
            }
        } else {
            configureForInvalidWall();
        }
    }

    @Override
    public void updateToModel() {
        super.updateToModel();
        updateNode();
        updateProperties();
    }
}
