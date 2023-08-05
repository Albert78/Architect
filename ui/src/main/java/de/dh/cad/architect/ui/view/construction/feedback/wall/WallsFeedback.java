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
package de.dh.cad.architect.ui.view.construction.feedback.wall;

import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import de.dh.cad.architect.model.wallmodel.IWall;
import de.dh.cad.architect.ui.view.construction.ConstructionView;
import de.dh.cad.architect.ui.view.construction.feedback.IVisualObjectFeedback;

/**
 * Manages and draws the visual feedbacks for a set of {@link IWall walls}.
 */
public class WallsFeedback {
    protected final ConstructionView mParentView;

    protected final Map<String, IVisualObjectFeedback<IWall>> mGhostWalls = new TreeMap<>();

    public WallsFeedback(ConstructionView parentView) {
        mParentView = parentView;
    }

    public void update(Map<String, ? extends IWall> allWalls) {
        // Remove obsolete ghost walls which are not in wall model any more
        for (Entry<String, IVisualObjectFeedback<IWall>> currentGhostWallEntry : new ArrayList<>(mGhostWalls.entrySet())) {
            String id = currentGhostWallEntry.getKey();
            if (!allWalls.containsKey(id)) {
                IVisualObjectFeedback<IWall> ghostWall = currentGhostWallEntry.getValue();
                ghostWall.uninstall();
                mGhostWalls.remove(id);
            }
        }
        // Create and update ghost walls
        for (Entry<String, ? extends IWall> we : allWalls.entrySet()) {
            String id = we.getKey();
            IWall wall = we.getValue();
            IVisualObjectFeedback<IWall> ghostWall = mGhostWalls.get(id);
            if (ghostWall == null) {
                ghostWall = new WallShapeFeedback(mParentView, wall);
                mGhostWalls.put(id, ghostWall);
                ghostWall.install();
            }

            ghostWall.update();
        }
    }

    public void uninstall() {
        for (IVisualObjectFeedback<IWall> ghostWall : mGhostWalls.values()) {
            ghostWall.uninstall();
        }
        mGhostWalls.clear();
    }
}
