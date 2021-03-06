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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
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
import de.dh.cad.architect.ui.utils.SurfaceAwareCSG;
import de.dh.cad.architect.ui.utils.SurfaceAwareCSG.ExtrusionSurfaceDataProvider;
import de.dh.cad.architect.ui.utils.SurfaceAwareCSG.ShapeSurfaceData;
import de.dh.cad.architect.ui.utils.SurfaceAwareCSG.SurfacePart;
import de.dh.cad.architect.ui.utils.TextureProjection;
import de.dh.cad.architect.ui.view.threed.Abstract3DView;
import de.dh.cad.architect.ui.view.threed.ThreeDView;
import de.dh.utils.fx.Vector2D;
import eu.mihosoft.vvecmath.Vector3d;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.shape.Mesh;
import javafx.scene.shape.MeshView;

public class Wall3DRepresentation extends Abstract3DRepresentation {
    protected static final double EPSILON = 0.01;

    protected final Map<String, SurfaceData> mSurfaces = new TreeMap<>();

    public Wall3DRepresentation(Wall wall, Abstract3DView parentView) {
        super(wall, parentView);
        for (SurfaceConfiguration surfaceConfig : wall.getSurfaceConfigurations()) {
            String surfaceTypeId = surfaceConfig.getSurfaceTypeId();
            SurfaceData sd = new SurfaceData(surfaceTypeId);
            mSurfaces.put(surfaceTypeId, sd);
            MeshView meshView = sd.getMeshView();
            add(meshView);
            markSurfaceNode(meshView, new ObjectSurface(this, surfaceTypeId) {
                @Override
                public AssetRefPath getMaterialRef() {
                    return surfaceConfig.getMaterialAssignment();
                }

                @Override
                public boolean assignMaterial(AssetRefPath materialRef) {
                    surfaceConfig.setMaterialAssignment(materialRef);
                    mParentView.getUiController().notifyObjectsChanged(wall);
                    return true;
                }
            });
        }

        ChangeListener<Boolean> updatePropertiesListener = new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                updateProperties();
            }
        };
        selectedProperty().addListener(updatePropertiesListener);
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
        for (SurfaceData surfaceData : mSurfaces.values()) {
            SurfaceConfiguration surfaceConfiguration = wall.getSurfaceTypeIdsToSurfaceConfigurations().get(surfaceData.getSurfaceTypeId());
            AssetRefPath materialRefPath = surfaceConfiguration.getMaterialAssignment();
            assetLoader.configureMaterial(surfaceData.getMeshView(), materialRefPath, Optional.ofNullable(surfaceData.getSurfaceSize()));
            if (isSelected()) {
                surfaceData.getMaterial().setDiffuseColor(SELECTED_OBJECTS_COLOR);
            }
        }
    }

    /**
     * Removes duplicate points. We don't remove points in the middle of a line segment,
     * those might belong to different wall connections and thus, we need those as different
     * entries to be able to attach different materials.
     */
    protected static boolean cleanupWallOutlineCorners(List<WallOutlineCorner> corners) {
        if (corners.size() < 3) {
            // Polygon must have a least 3 points
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
                    // All points are colinear
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

    // Attention: basePointsCW will be modified in this method
    protected SurfaceAwareCSG<WallSurface> createWallCSGNegatedZ(Wall wall, List<WallOutlineCorner> basePointsCW) {
        if (!cleanupWallOutlineCorners(basePointsCW)) {
            return null;
        }

        // We later work on the reverted points direction, so here we're interested in the index AFTER the surface change.
        // That's why we check the surfaces at the opposite side.
        WallSurface lastSurface = basePointsCW.get(0).getNext().getSurface();
        int firstSurfaceChange = -1;
        for (int i = 0; i < basePointsCW.size(); i++) {
            WallOutlineConnection currentConnection = basePointsCW.get(i).getPrevious();
            if (firstSurfaceChange == -1 && currentConnection.getSurface() != lastSurface) {
                firstSurfaceChange = i; // Found the first surface change - this is the start index for the extrusion process
            }
        }
        if (firstSurfaceChange == -1) {
            firstSurfaceChange = 0; // No surface change, start at index 0
        }

        double heightA = CoordinateUtils.lengthToCoords(wall.getHeightA());
        double heightB = CoordinateUtils.lengthToCoords(wall.getHeightB());
        Position2D handleA = wall.getAnchorWallHandleA().getPosition().projectionXY();
        Position2D handleB = wall.getAnchorWallHandleB().getPosition().projectionXY();

        Vector3d handleAPos = CoordinateUtils.position2DToVecMathVector3d(handleA);
        Vector3d handleBPos = CoordinateUtils.position2DToVecMathVector3d(handleB);

        Vector3d wallDirectionAB = CoordinateUtils.vector2DToVecMathVector3d(handleB.minus(handleA));
        double wallLength = wallDirectionAB.magnitude();
        Vector2D upperHandle;
        Vector2D lowerHandle;
        if (heightA > heightB) {
            upperHandle = new Vector2D(handleAPos.getX(), handleAPos.getY());
            lowerHandle = new Vector2D(handleBPos.getX(), handleBPos.getY());
        } else {
            upperHandle = new Vector2D(handleBPos.getX(), handleBPos.getY());
            lowerHandle = new Vector2D(handleAPos.getX(), handleAPos.getY());
        }
        double diffH = Math.abs(heightA - heightB);
        double diffM = diffH / (wallLength == 0 ? 1 : wallLength); // Actually, the wall length cannot be 0, can it?
        Vector2D descendingDirection = lowerHandle.minus(upperHandle).toUnitVector();

        double baseExtrudeHeight = Math.max(heightA, heightB);

        List<Vector3d> topPolygonPointsCCW = new ArrayList<>();
        List<Vector3d> bottomPolygonPointsCCW = new ArrayList<>();
        List<Position2D> basePointsCCW = basePointsCW
                        .stream()
                        .map(corner -> corner.getPosition())
                        .collect(Collectors.toCollection(ArrayList::new));
        Collections.reverse(basePointsCCW);

        for (Position2D pos : basePointsCCW) {
            Vector2D vv = new Vector2D(CoordinateUtils.lengthToCoords(pos.getX()), CoordinateUtils.lengthToCoords(pos.getY()));

            // Calculate top level; projection of top points to a plane which spans between the two upper handle positions.
            // This is necessary if the wall has different heights at the wall ends.
            double t = vv.minus(upperHandle).dotProduct(descendingDirection);
            // Attention: Y decreases to the top in JavaFX
            topPolygonPointsCCW.add(Vector3d.xyz(vv.getX(), vv.getY(), -(baseExtrudeHeight - diffM * t)));

            // Bottom level is simply at Z=0
            Vector3d bottomPoint = Vector3d.xy(vv.getX(), vv.getY());
            bottomPolygonPointsCCW.add(bottomPoint);
        }

        int numPoints = basePointsCCW.size();

        // Wall fragment without clipping of the top
        SurfaceAwareCSG<WallSurface> result = SurfaceAwareCSG.extrudeSurfaces(
            new ExtrusionSurfaceDataProvider<WallSurface>() {
                @Override
                public List<Vector3d> getConnectedTopPolygonPointsCCW() {
                    return topPolygonPointsCCW;
                }

                @Override
                public List<Vector3d> getBottomPolygonPointsCCW() {
                    return bottomPolygonPointsCCW;
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
                public WallSurface getSurfaceCCW(int startPointIndex) {
                    // Attention: Caller wants surface in CCW direction, so we must return the PREVIOUS one
                    return basePointsCW.get(numPoints - startPointIndex - 1).getPrevious().getSurface();
                }

                @Override
                public WallSurface getTopSurface() {
                    return WallSurface.Top;
                }

                @Override
                public WallSurface getBottomSurface() {
                    return WallSurface.Bottom;
                }
            }, firstSurfaceChange, true);
        return result;
    }

    protected void configureForInvalidWall() {
        for (SurfaceData sd : mSurfaces.values()) {
            sd.getMeshView().setMesh(null);
        }
    }

    protected void updateNode() {
        Wall wall = getWall();

        Vector2D pA = CoordinateUtils.positionToVector2D(wall.getAnchorWallHandleA().requirePosition2D());
        Vector2D pB = CoordinateUtils.positionToVector2D(wall.getAnchorWallHandleB().requirePosition2D());

        Length wallBaseLengthL = wall.calculateBaseLength();
        double wallBaseLengthC = CoordinateUtils.lengthToCoords(wallBaseLengthL);

        Vector2D longEdgeWall = pB.minus(pA);
        Vector2D shortEdgeWall = longEdgeWall.getNormalCW().scaleToLength(wallBaseLengthC);
        Vector2D longEdgeWallU = longEdgeWall.toUnitVector();


        Optional<WallAnchorPositions> oWap = wall.extractWallAnchorPositions();
        Optional<WallOutline> oWallOutlineCW = oWap.map(wap -> wap.calculateWallOutlineCW());
        if (oWallOutlineCW.isPresent()) {
            WallOutline wallOutlineCW = oWallOutlineCW.get();
            SurfaceAwareCSG<WallSurface> csg = createWallCSGNegatedZ(wall, wallOutlineCW.getCornersAsList());
            if (csg == null) {
                configureForInvalidWall();
                return;
            }

            for (WallHole wallHole : wall.getWallHoles()) {
                double distanceFromWallEndA = CoordinateUtils.lengthToCoords(wallHole.getDistanceFromWallEndA(wallBaseLengthL));
                Dimensions2D holeDimensions = wallHole.getDimensions();
                double holeWidthC = CoordinateUtils.lengthToCoords(holeDimensions.getX());
                double holeHeightC = CoordinateUtils.lengthToCoords(holeDimensions.getY());
                double holeParapetHeightC = CoordinateUtils.lengthToCoords(wallHole.getParapetHeight());

                Vector2D windowStartMiddle = pA.plus(longEdgeWallU.times(distanceFromWallEndA));
                Vector2D windowEndMiddle = windowStartMiddle.plus(longEdgeWallU.times(holeWidthC));
                Vector2D a1p = windowStartMiddle.minus(shortEdgeWall); // Window a1 plus x overhang for other walls bevels, if any. TODO: Calculate the overhang according to other wall's thickness.
                Vector2D a2p = windowStartMiddle.plus(shortEdgeWall);
                Vector2D b1p = windowEndMiddle.minus(shortEdgeWall);
                Vector2D b2p = windowEndMiddle.plus(shortEdgeWall);

                // Attention: Y decreases to the top in JavaFX
                List<Vector3d> bottomPoints = Arrays.asList(
                    Vector3d.xyz(a1p.getX(), a1p.getY(), -holeParapetHeightC),
                    Vector3d.xyz(a2p.getX(), a2p.getY(), -holeParapetHeightC),
                    Vector3d.xyz(b2p.getX(), b2p.getY(), -holeParapetHeightC),
                    Vector3d.xyz(b1p.getX(), b1p.getY(), -holeParapetHeightC)
                    );
                List<Vector3d> topPoints = bottomPoints
                                .stream()
                                .map(p -> Vector3d.xyz(p.getX(), p.getY(), -(holeParapetHeightC + holeHeightC)))
                                .collect(Collectors.toList());

                Vector3d textureDirectionX = Vector3d.xy(longEdgeWall.getX(), longEdgeWall.getY());
                SurfaceAwareCSG<WallSurface> holeCSG = SurfaceAwareCSG.extrudeSurfaces(
                    new ExtrusionSurfaceDataProvider<WallSurface>() {
                        @Override
                        public List<Vector3d> getConnectedTopPolygonPointsCCW() {
                            return topPoints;
                        }

                        @Override
                        public List<Vector3d> getBottomPolygonPointsCCW() {
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
                        public WallSurface getSurfaceCCW(int startPointIndex) {
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

            Map<WallSurface, ShapeSurfaceData<WallSurface>> meshes = csg.createJavaFXTrinagleMeshes();
            for (SurfaceData surfaceData : mSurfaces.values()) {
                // One surface of the wall, e.g. A or One
                String surfaceTypeId = surfaceData.getSurfaceTypeId();
                WallSurface wallSurface = WallSurface.ofWallSurfaceType(surfaceTypeId);
                ShapeSurfaceData<WallSurface> shapeSurfaceData = meshes.computeIfAbsent(wallSurface, s -> ShapeSurfaceData.empty());
                MeshView meshView = surfaceData.getMeshView();
                // The surface can potentially consist of several parts, e.g. a wall side might consist of the main part an a part of the bevel.
                Iterator<SurfacePart<WallSurface>> si = shapeSurfaceData.getSurfaceParts().iterator();
                if (!si.hasNext()) { // E.g. wall contains no embrasures
                    meshView.setMesh(null);
                    continue;
                }
                SurfacePart<WallSurface> firstSurfacePart = si.next();
                TextureProjection firstTextureProjection = firstSurfacePart.getTextureProjection();
                // The CSG builder tells us how big the "flattened" texture range for this surface is in the texture projection range:
                Vector2D surfaceSize = firstTextureProjection.getRangeTxy();
                // The CSG builder has generated the mesh in a way that the texture coordinates of the surface parts map to their part of the overall surface texture.
                // E.g. if wall side 1 extends over two surface parts, the main side 1 surface and the corner bevel apex, the algorithm places the texture coords
                // to cover both surface parts, i.e. texture coords (0; 0) at the beginning of part 1 and texture coords (1; 1) at the end of part 2.
                surfaceData.setSurfaceSize(surfaceSize);
                Mesh mesh = shapeSurfaceData.getMesh();
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
