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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import de.dh.cad.architect.model.assets.AssetRefPath;
import de.dh.cad.architect.model.objects.Anchor;
import de.dh.cad.architect.model.objects.Floor;
import de.dh.cad.architect.model.objects.SurfaceConfiguration;
import de.dh.cad.architect.ui.assets.AssetLoader;
import de.dh.cad.architect.ui.utils.CoordinateUtils;
import de.dh.cad.architect.ui.view.threed.Abstract3DView;
import de.dh.cad.architect.ui.view.threed.ThreeDView;
import de.dh.utils.Vector3D;
import de.dh.utils.csg.SurfaceAwareCSG;
import de.dh.utils.csg.SurfaceAwareCSG.ExtrusionSurfaceDataProvider;
import de.dh.utils.io.MeshData;
import de.dh.utils.io.fx.FxMeshBuilder;
import de.dh.utils.csg.TextureCoordinateSystem;
import de.dh.utils.csg.TextureProjection;
import eu.mihosoft.jcsg.ext.org.poly2tri.PolygonUtil;
import eu.mihosoft.vvecmath.Vector3d;
import javafx.geometry.Point3D;
import javafx.scene.shape.Mesh;
import javafx.scene.shape.MeshView;
import javafx.scene.transform.Rotate;

public class Floor3DRepresentation extends Abstract3DRepresentation {
    protected final SurfaceData mSurfaceData;
    protected final Rotate mRotation = new Rotate();

    public Floor3DRepresentation(Floor floor, Abstract3DView parentView) {
        super(floor, parentView);
        SurfaceConfiguration surfaceConfiguration = floor.getSurfaceConfigurations().iterator().next();
        String surfaceTypeId = surfaceConfiguration.getSurfaceTypeId();
        mSurfaceData = new SurfaceData(surfaceTypeId);
        MeshView meshView = mSurfaceData.getMeshView();
        meshView.getTransforms().add(mRotation);
        add(meshView);
        markSurfaceNode(meshView, new ObjectSurface(this, surfaceTypeId) {
            @Override
            public AssetRefPath getMaterialRef() {
                return surfaceConfiguration.getMaterialAssignment();
            }

            @Override
            public boolean assignMaterial(AssetRefPath materialRef) {
                setObjectSurface(floor, mSurfaceTypeId, materialRef);
                return true;
            }
        });

        selectedProperty().addListener(change -> {
            updateProperties();
        });
    }

    public Floor getFloor() {
        return (Floor) mModelObject;
    }

    protected ThreeDView getParentView() {
        return (ThreeDView) mParentView;
    }

    protected void updateProperties() {
        Floor floor = getFloor();
        SurfaceConfiguration surfaceConfiguration = floor.getSurfaceTypeIdsToSurfaceConfigurations().get(mSurfaceData.getSurfaceTypeId());
        AssetRefPath materialRefPath = surfaceConfiguration.getMaterialAssignment();
        AssetLoader assetLoader = getAssetLoader();
        assetLoader.configureMaterial(mSurfaceData.getMeshView(), materialRefPath, Optional.ofNullable(mSurfaceData.getSurfaceSize()));
        if (isSelected()) {
            mSurfaceData.getMaterial().setDiffuseColor(SELECTED_OBJECTS_COLOR);
        }
    }

    protected enum Surface {
        S1
    }

    protected void updateNode() {
        Floor floor = getFloor();
        List<Anchor> anchors = floor.getEdgePositionAnchors();

        if (anchors.size() < 3) {
            mSurfaceData.getMeshView().setMesh(null);
            return;
        }

        Vector3d a = CoordinateUtils.position3DToVecMathVector3d(anchors.get(0).requirePosition3D());
        Vector3d b = CoordinateUtils.position3DToVecMathVector3d(anchors.get(1).requirePosition3D());
        Vector3d ab = b.minus(a);

        double thicknessC = CoordinateUtils.lengthToCoords(floor.getThickness());

        List<Vector3d> topPoints = new ArrayList<>();
        List<Vector3d> bottomPoints = new ArrayList<>();
        for (Anchor anchor : anchors) {
            Vector3D pos = CoordinateUtils.position3DToVector3D(anchor.requirePosition3D());
            Vector3d posNormalZ = Vector3d.xyz(pos.getX(), pos.getY(), -pos.getZ());

            bottomPoints.add(posNormalZ);
            topPoints.add(Vector3d.xyz(posNormalZ.getX(), posNormalZ.getY(), posNormalZ.getZ() - thicknessC));
        }

        if (!PolygonUtil.isCCW(bottomPoints)) {
            Collections.reverse(bottomPoints);
            Collections.reverse(topPoints);
        }

        Vector3d textureDirectionX = ab;

        SurfaceAwareCSG<Surface> csg = SurfaceAwareCSG.extrudeSurfaces(new ExtrusionSurfaceDataProvider<Surface>() {
            @Override
            public List<Vector3d> getBottomPolygonPointsCCW() {
                return bottomPoints;
            }

            @Override
            public List<Vector3d> getConnectedTopPolygonPointsCCW() {
                return topPoints;
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
            public Surface getSurfaceCCW(int startPointIndex) {
                return Surface.S1;
            }

            @Override
            public Surface getTopSurface() {
                return Surface.S1;
            }

            @Override
            public Surface getBottomSurface() {
                return Surface.S1;
            }
        }, 0, false);
        Map<Surface, MeshData> meshes = csg.createMeshes(Optional.empty());
        MeshData meshData = meshes.get(Surface.S1);
        Mesh mesh = FxMeshBuilder.buildMesh(meshData);
        MeshView meshView = mSurfaceData.getMeshView();
        meshView.setMesh(mesh);
        Point3D axisP3D = new Point3D(0, 0, 1);
        mRotation.setAngle(0);
        mRotation.setAxis(axisP3D);

        TextureCoordinateSystem tcs = TextureCoordinateSystem.create(Vector3d.Z_ONE, textureDirectionX);
        TextureProjection tp = TextureProjection.fromPointsBorder(tcs, bottomPoints);
        mSurfaceData.setSurfaceSize(tp.getSpannedSize());
    }

    @Override
    public void updateToModel() {
        super.updateToModel();
        updateNode();
        updateProperties();
    }
}
