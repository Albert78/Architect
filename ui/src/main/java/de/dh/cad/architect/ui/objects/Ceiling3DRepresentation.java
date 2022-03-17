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
import java.util.List;
import java.util.Optional;

import de.dh.cad.architect.model.assets.AssetRefPath;
import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.model.coords.Position3D;
import de.dh.cad.architect.model.objects.Anchor;
import de.dh.cad.architect.model.objects.Ceiling;
import de.dh.cad.architect.model.objects.SurfaceConfiguration;
import de.dh.cad.architect.ui.assets.AssetLoader;
import de.dh.cad.architect.ui.utils.CoordinateUtils;
import de.dh.cad.architect.ui.utils.MathUtils;
import de.dh.cad.architect.ui.utils.MathUtils.RotationData;
import de.dh.cad.architect.ui.utils.SurfaceAwareCSG;
import de.dh.cad.architect.ui.utils.SurfaceAwareCSG.ExtrusionSurfaceDataProvider;
import de.dh.cad.architect.ui.utils.SurfaceAwareCSG.ShapeSurfaceData;
import de.dh.cad.architect.ui.utils.TextureCoordinateSystem;
import de.dh.cad.architect.ui.utils.TextureProjection;
import de.dh.cad.architect.ui.view.threed.Abstract3DView;
import de.dh.cad.architect.ui.view.threed.ThreeDView;
import de.dh.utils.fx.Vector3D;
import eu.mihosoft.vvecmath.Transform;
import eu.mihosoft.vvecmath.Vector3d;
import javafx.geometry.Point3D;
import javafx.scene.shape.Mesh;
import javafx.scene.shape.MeshView;
import javafx.scene.transform.Rotate;

public class Ceiling3DRepresentation extends Abstract3DRepresentation {
    protected final SurfaceData mSurfaceData;
    protected final Rotate mRotation = new Rotate();

    public Ceiling3DRepresentation(Ceiling ceiling, Abstract3DView parentView) {
        super(ceiling, parentView);
        SurfaceConfiguration surfaceConfiguration = ceiling.getSurfaceConfigurations().iterator().next();
        mSurfaceData = new SurfaceData(surfaceConfiguration.getSurfaceTypeId());
        MeshView meshView = mSurfaceData.getMeshView();
        meshView.getTransforms().add(mRotation);
        add(meshView);
        markSurfaceNode(meshView, new ObjectSurface(this, surfaceConfiguration.getSurfaceTypeId()) {
            @Override
            public AssetRefPath getMaterialRef() {
                return surfaceConfiguration.getMaterialAssignment();
            }

            @Override
            public boolean assignMaterial(AssetRefPath materialRef) {
                surfaceConfiguration.setMaterialAssignment(materialRef);
                mParentView.getUiController().notifyObjectsChanged(ceiling);
                return true;
            }
        });

        selectedProperty().addListener(change -> {
            updateProperties();
        });
    }

    public Ceiling getCeiling() {
        return (Ceiling) mModelObject;
    }

    protected ThreeDView getParentView() {
        return (ThreeDView) mParentView;
    }

    protected void updateProperties() {
        Ceiling ceiling = getCeiling();
        SurfaceConfiguration surfaceConfiguration = ceiling.getSurfaceTypeIdsToSurfaceConfigurations().get(mSurfaceData.getSurfaceTypeId());
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
        Ceiling ceiling = getCeiling();

        // Ceiling plane
        Position3D posA = ceiling.getAnchorA().requirePosition3D();
        Position3D posB = ceiling.getAnchorB().requirePosition3D();
        Position3D posC = ceiling.getAnchorC().requirePosition3D();

        Vector3d a = CoordinateUtils.position3DToVecMathVector3d(posA);
        Vector3d b = CoordinateUtils.position3DToVecMathVector3d(posB);
        Vector3d c = CoordinateUtils.position3DToVecMathVector3d(posC);

        Vector3d ab = b.minus(a);

        // Ceiling normal vector
        Vector3d n = ab.crossed(c.minus(a)).normalized();

        Vector3d z1 = Vector3d.xyz(0, 0, 1);

        RotationData rotationData = MathUtils.calculateRotation(z1, n);
        Vector3d axis = rotationData.getAxis();
        double angle = rotationData.getAngle();
        Transform rotation = Transform.unity().rot(Vector3d.ZERO, axis, angle);

        double THICKNESS = CoordinateUtils.lengthToCoords(Length.ofMM(1));

        List<Vector3d> topPoints = new ArrayList<>();
        List<Vector3d> bottomPoints = new ArrayList<>();
        for (Anchor anchor : ceiling.getEdgePositionAnchors()) {
            Vector3D pos = CoordinateUtils.position3DToVector3D(anchor.requirePosition3D());
            Vector3d posNormalZ = Vector3d.xyz(pos.getX(), pos.getY(), -pos.getZ()).transformed(rotation);

            bottomPoints.add(posNormalZ);
            topPoints.add(Vector3d.xyz(posNormalZ.getX(), posNormalZ.getY(), posNormalZ.getZ() - THICKNESS));
        }

        Vector3d textureDirectionX = ab.transformed(rotation);

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
        ShapeSurfaceData<Surface> shapeSurfaceData = csg.createJavaFXTrinagleMesh(Surface.S1);
        Mesh mesh = shapeSurfaceData.getMesh();
        MeshView meshView = mSurfaceData.getMeshView();
        meshView.setMesh(mesh);
        Point3D axisP3D = new Point3D(axis.getX(), axis.getY(), axis.getZ());
        mRotation.setAngle(-angle);
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
