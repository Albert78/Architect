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
package de.dh.utils.fx;

import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;

public class BoxMesh {
    // Copied from javafx.scene.shape.Box
    public static TriangleMesh createMesh(float w, float h, float d) {
        // NOTE: still create mesh for degenerated box
        float hw = w / 2f;
        float hh = h / 2f;
        float hd = d / 2f;

        float points[] = {
            -hw, -hh, -hd,
             hw, -hh, -hd,
             hw,  hh, -hd,
            -hw,  hh, -hd,
            -hw, -hh,  hd,
             hw, -hh,  hd,
             hw,  hh,  hd,
            -hw,  hh,  hd};

        float texCoords[] = {0, 0, 1, 0, 1, 1, 0, 1};

        // Specifies hard edges.
        int faceSmoothingGroups[] = {
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        };

        int faces[] = {
            0, 0, 2, 2, 1, 1,
            2, 2, 0, 0, 3, 3,
            1, 0, 6, 2, 5, 1,
            6, 2, 1, 0, 2, 3,
            5, 0, 7, 2, 4, 1,
            7, 2, 5, 0, 6, 3,
            4, 0, 3, 2, 0, 1,
            3, 2, 4, 0, 7, 3,
            3, 0, 6, 2, 2, 1,
            6, 2, 3, 0, 7, 3,
            4, 0, 1, 2, 5, 1,
            1, 2, 4, 0, 0, 3,
        };

        TriangleMesh mesh = new TriangleMesh(VertexFormat.POINT_TEXCOORD);
        mesh.getPoints().setAll(points);
        mesh.getTexCoords().setAll(texCoords);
        mesh.getFaces().setAll(faces);
        mesh.getFaceSmoothingGroups().setAll(faceSmoothingGroups);

        return mesh;
    }
}
