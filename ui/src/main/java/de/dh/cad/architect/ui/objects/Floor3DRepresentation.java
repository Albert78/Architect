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

import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.model.objects.Anchor;
import de.dh.cad.architect.model.objects.Floor;
import de.dh.cad.architect.model.objects.SurfaceConfiguration;
import de.dh.cad.architect.ui.assets.AssetLoader;
import de.dh.cad.architect.ui.utils.CoordinateUtils;
import de.dh.cad.architect.ui.view.threed.Abstract3DView;
import de.dh.cad.architect.ui.view.threed.ThreeDView;
import de.dh.utils.Vector3D;
import de.dh.utils.csg.CSGSurfaceAwareAddon;
import de.dh.utils.csg.CSGs;
import de.dh.utils.csg.CSGs.ExtrusionSurfaceDataProvider;
import de.dh.utils.io.MeshData;
import de.dh.utils.io.fx.FxMeshBuilder;
import eu.mihosoft.jcsg.CSG;
import eu.mihosoft.jcsg.ext.org.poly2tri.PolygonUtil;
import eu.mihosoft.vvecmath.Vector3d;
import javafx.geometry.Point3D;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Mesh;
import javafx.scene.shape.MeshView;
import javafx.scene.transform.Rotate;

public class Floor3DRepresentation extends AbstractSolid3DRepresentation {
    protected final SurfaceData<MeshView> mSurfaceData;
    protected final Rotate mRotation = new Rotate();

    public Floor3DRepresentation(Floor floor, Abstract3DView parentView) {
        super(floor, parentView);
        SurfaceConfiguration surfaceConfiguration = floor.getSurfaceConfigurations().iterator().next();
        String surfaceTypeId = surfaceConfiguration.getSurfaceTypeId();
        MeshView meshView = new MeshView();
        mSurfaceData = new SurfaceData<>(this, surfaceTypeId, meshView);
        meshView.getTransforms().add(mRotation);
        meshView.getTransforms().addFirst(CoordinateUtils.createTransformArchitectToJavaFx());
        add(meshView);
        registerSurface(mSurfaceData);
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
        de.dh.cad.architect.model.objects.MaterialMappingConfiguration mmc = surfaceConfiguration.getMaterialMappingConfiguration();
        AssetLoader assetLoader = getAssetLoader();
        PhongMaterial material = assetLoader.buildMaterial(mmc, mSurfaceData.getSurfaceSize());
        mSurfaceData.setMaterial(material);
    }

    protected enum Surface {
        Top
    }

    protected void updateNode() {
        Floor floor = getFloor();
        List<Anchor> anchors = floor.getEdgePositionAnchors();

        if (anchors.size() < 3) {
            mSurfaceData.getShape().setMesh(null);
            return;
        }

        Vector3d a = CoordinateUtils.position3DToVecMathVector3d(anchors.get(0).requirePosition3D(), false);
        Vector3d b = CoordinateUtils.position3DToVecMathVector3d(anchors.get(1).requirePosition3D(), false);
        Vector3d ab = b.minus(a);

        double THICKNESS = CoordinateUtils.lengthToCoords(Length.ofMM(1), null);

        List<Vector3d> topPointsCW = new ArrayList<>();
        List<Vector3d> bottomPointsCW = new ArrayList<>();
        for (Anchor anchor : anchors) {
            Vector3D pos = CoordinateUtils.position3DToVector3D(anchor.requirePosition3D(), false);
            Vector3d posNormalZ = Vector3d.xyz(pos.getX(), pos.getY(), pos.getZ());

            bottomPointsCW.add(posNormalZ);
            topPointsCW.add(Vector3d.xyz(posNormalZ.getX(), posNormalZ.getY(), posNormalZ.getZ() + THICKNESS));
        }

        if (PolygonUtil.isCCW_XY(bottomPointsCW)) {
            Collections.reverse(bottomPointsCW);
            Collections.reverse(topPointsCW);
        }

        Vector3d textureDirectionX = ab;

        ExtrusionSurfaceDataProvider<Surface> floorSurfaceDataProvider = new ExtrusionSurfaceDataProvider<>() {
            @Override
            public List<Vector3d> getBottomPolygonPointsCW() {
                return bottomPointsCW;
            }

            @Override
            public List<Vector3d> getTopPolygonPointsCW() {
                return topPointsCW;
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
            public Surface getSurfaceCW(int startPointIndex) {
                return null;
            }

            @Override
            public Surface getTopSurface() {
                return Surface.Top;
            }

            @Override
            public Surface getBottomSurface() {
                return null;
            }
        };
        mSurfaceData.setSurfaceSize(floorSurfaceDataProvider.getTopPolygonTextureProjection().getSpannedSize());
        CSG csg = CSGs.extrudeSurfaces(floorSurfaceDataProvider, 0, false);
        Map<Surface, MeshData> meshes = CSGSurfaceAwareAddon.createMeshes(csg, Optional.empty());
        MeshData meshData = meshes.get(Surface.Top);
        Mesh mesh = FxMeshBuilder.buildMesh(meshData);
        MeshView meshView = mSurfaceData.getShape();
        meshView.setMesh(mesh);
        Point3D axisP3D = new Point3D(0, 0, 1);
        mRotation.setAngle(0);
        mRotation.setAxis(axisP3D);
    }

    @Override
    public void updateToModel() {
        super.updateToModel();
        updateNode();
        updateProperties();
    }
}
