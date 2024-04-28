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
import java.util.TreeMap;

import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.model.coords.Position3D;
import de.dh.cad.architect.model.objects.Anchor;
import de.dh.cad.architect.model.objects.Covering;
import de.dh.cad.architect.model.objects.SurfaceConfiguration;
import de.dh.cad.architect.ui.assets.AssetLoader;
import de.dh.cad.architect.ui.utils.CoordinateUtils;
import de.dh.cad.architect.ui.view.threed.Abstract3DView;
import de.dh.cad.architect.ui.view.threed.ThreeDView;
import de.dh.utils.Vector2D;
import de.dh.utils.Vector3D;
import de.dh.utils.csg.CSGSurfaceAwareAddon;
import de.dh.utils.csg.CSGs;
import de.dh.utils.csg.CSGs.ExtrusionSurfaceDataProvider;
import de.dh.utils.io.MeshData;
import de.dh.utils.io.fx.FxMeshBuilder;
import eu.mihosoft.jcsg.CSG;
import eu.mihosoft.jcsg.ext.org.poly2tri.PolygonUtil;
import eu.mihosoft.vvecmath.Transform;
import eu.mihosoft.vvecmath.Vector3d;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Point3D;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Mesh;
import javafx.scene.shape.MeshView;
import javafx.scene.transform.Rotate;

public class Covering3DRepresentation extends AbstractSolid3DRepresentation {
    protected final Map<Surface, SurfaceData<MeshView>> mSurfaces = new TreeMap<>();
    protected final Rotate mRotation = new Rotate();

    public Covering3DRepresentation(Covering covering, Abstract3DView parentView) {
        super(covering, parentView);
        for (SurfaceConfiguration surfaceConfiguration : covering.getSurfaceTypeIdsToSurfaceConfigurations().values()) {
            String surfaceTypeId = surfaceConfiguration.getSurfaceTypeId();
            MeshView meshView = new MeshView();
            SurfaceData<MeshView> sd = new SurfaceData<>(this, surfaceTypeId, meshView);
            Surface surface = Surface.ofSurfaceType(surfaceTypeId);
            mSurfaces.put(surface, sd);
            meshView.getTransforms().add(mRotation);
            meshView.getTransforms().add(0, CoordinateUtils.createTransformArchitectToJavaFx());
            add(meshView);
            registerSurface(sd);
        }

        ChangeListener<Boolean> bPropertiesUpdaterListener = (observable, oldValue, newValue) -> updateProperties();
        selectedProperty().addListener(bPropertiesUpdaterListener);
        objectFocusedProperty().addListener(bPropertiesUpdaterListener);
        objectEmphasizedProperty().addListener(bPropertiesUpdaterListener);
    }

    public Covering getCovering() {
        return (Covering) mModelObject;
    }

    protected ThreeDView getParentView() {
        return (ThreeDView) mParentView;
    }

    protected void updateProperties() {
        Covering covering = getCovering();
        AssetLoader assetLoader = getAssetLoader();
        for (SurfaceData<MeshView> surfaceData : mSurfaces.values()) {
            String surfaceTypeId = surfaceData.getSurfaceTypeId();
            SurfaceConfiguration surfaceConfiguration = covering.getSurfaceTypeIdsToSurfaceConfigurations().get(surfaceTypeId);
            de.dh.cad.architect.model.objects.MaterialMappingConfiguration mmc = surfaceConfiguration.getMaterialMappingConfiguration();
            PhongMaterial material = assetLoader.buildMaterial(mmc, surfaceData.getSurfaceSize());
            surfaceData.setMaterial(material);
        }
    }

    protected enum Surface {
        S1(Covering.SURFACE_TYPE_1),
        S2(Covering.SURFACE_TYPE_2);

        private final String mSurfaceType;

        private Surface(String surfaceType) {
            mSurfaceType = surfaceType;
        }

        public String getSurfaceType() {
            return mSurfaceType;
        }

        public static Surface ofSurfaceType(String type) {
            for (Surface surface : values()) {
                if (surface.getSurfaceType().equals(type)) {
                    return surface;
                }
            }
            throw new IllegalArgumentException("No surface of type '" + type + "'");
        }
    }

    protected void updateNode() {
        Covering covering = getCovering();

        // Covering plane
        Position3D posA = covering.getAnchorA().requirePosition3D();
        Position3D posB = covering.getAnchorB().requirePosition3D();
        Position3D posC = covering.getAnchorC().requirePosition3D();

        Vector3d a = CoordinateUtils.position3DToVecMathVector3d(posA, false);
        Vector3d b = CoordinateUtils.position3DToVecMathVector3d(posB, false);
        Vector3d c = CoordinateUtils.position3DToVecMathVector3d(posC, false);

        Vector3d ab = b.minus(a);

        // Covering normal vector
        Vector3d n1 = ab.crossed(c.minus(a)).normalized();

        // Rotate object coordinates temporarily in X/Y plane to extrude CSG object
        // Code taken from Transform.rot(Vector3d, Vector3d)
        Vector3d z1 = Vector3d.xyz(0, 0, 1);
        Vector3d _axis = n1.crossed(z1);
        double l = _axis.magnitude(); // sine of angle

        Vector3d axis = Vector3d.X_ONE;
        double angle = 0;
        Transform rotation = Transform.unity();
        if (l > 1e-9) {
            axis = _axis.normalized();
            angle = n1.angle(z1);

            rotation = rotation.rot(Vector3d.ZERO, axis, angle);
        }

        double THICKNESS = CoordinateUtils.lengthToCoords(Length.ofMM(1), null);

        List<Vector3d> bottomPointsCW = new ArrayList<>();
        for (Anchor anchor : covering.getAnchors()) {
            Vector3D pos = CoordinateUtils.position3DToVector3D(anchor.requirePosition3D(), false);
            Vector3d posNormalZ = Vector3d.xyz(pos.getX(), pos.getY(), pos.getZ()).transformed(rotation);

            bottomPointsCW.add(posNormalZ);
        }
        if (PolygonUtil.isCCW_XY(bottomPointsCW)) {
            Collections.reverse(bottomPointsCW);
        }
        List<Vector3d> topPointsCW = new ArrayList<>();
        for (Vector3d bottomPoint : bottomPointsCW) {
            topPointsCW.add(Vector3d.xyz(bottomPoint.getX(), bottomPoint.getY(), bottomPoint.getZ() + THICKNESS));
        }

        Vector3d textureDirectionX = ab.transformed(rotation);

        ExtrusionSurfaceDataProvider<Surface> coveringSurfaceDataProvider = new ExtrusionSurfaceDataProvider<>() {
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
                return Surface.S1;
            }

            @Override
            public Surface getBottomSurface() {
                return Surface.S2;
            }
        };
        Vector2D surfaceSize = coveringSurfaceDataProvider.getTopPolygonTextureProjection().getSpannedSize();
        CSG csg = CSGs.extrudeSurfaces(coveringSurfaceDataProvider, 0, false);
        Map<Surface, MeshData> meshes = CSGSurfaceAwareAddon.createMeshes(csg, Optional.empty());
        for (Surface surface : Surface.values()) {
            MeshData meshData = meshes.get(surface);
            SurfaceData<MeshView> surfaceData = mSurfaces.get(surface);
            MeshView meshView = surfaceData.getShape();
            Mesh mesh = FxMeshBuilder.buildMesh(meshData);
            meshView.setMesh(mesh);
            Point3D axisP3D = new Point3D(axis.getX(), axis.getY(), axis.getZ());
            mRotation.setAngle(-angle);
            mRotation.setAxis(axisP3D);

            surfaceData.setSurfaceSize(surfaceSize);
        }
    }

    @Override
    public void updateToModel() {
        super.updateToModel();
        updateNode();
        updateProperties();
    }
}
