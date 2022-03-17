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
package de.dh.cad.architect.libraryimporter.sh3d.furniture;

import de.dh.cad.architect.utils.vfs.IResourceLocator;

/**
 * A light of the catalog.
 * @author Emmanuel Puybaret
 * @since  1.7
 */
public class CatalogLight extends CatalogPieceOfFurniture {
  private final LightSource [] lightSources;

  /**
   * Creates an unmodifiable catalog light of the default catalog.
   * @param id    the id of the new light, or <code>null</code>
   * @param name  the name of the new light
   * @param description the description of the new light
   * @param information additional information associated to the new light
   * @param tags tags associated to the new light
   * @param creationDate creation date of the new light in milliseconds since the epoch
   * @param grade grade of the new light or <code>null</code>
   * @param icon content of the icon of the new light
   * @param planIcon content of the icon of the new piece displayed in plan
   * @param model content of the 3D model of the new light
   * @param width  the width in centimeters of the new light
   * @param depth  the depth in centimeters of the new light
   * @param height  the height in centimeters of the new light
   * @param dropOnTopElevation a percentage of the height at which should be placed
   *            an object dropped on the new piece
   * @param elevation  the elevation in centimeters of the new light
   * @param movable if <code>true</code>, the new light is movable
   * @param lightSources the light sources of the new light
   * @param staircaseCutOutShape the shape used to cut out upper levels when they intersect
   *            with the piece like a staircase
   * @param modelRotation the rotation 3 by 3 matrix applied to the light model
   * @param backFaceShown <code>true</code> if back face should be shown instead of front faces
   * @param modelSize size of the 3D model of the new light
   * @param creator the creator of the model
   * @param resizable if <code>true</code>, the size of the new light may be edited
   * @param deformable if <code>true</code>, the width, depth and height of the new piece may
   *            change independently from each other
   * @param texturable if <code>false</code> this piece should always keep the same color or texture
   * @param horizontallyRotatable if <code>false</code> this piece
   *            should not rotate around an horizontal axis
   * @param price the price of the new light, or <code>null</code>
   * @param valueAddedTaxPercentage the Value Added Tax percentage applied to the
   *             price of the new light or <code>null</code>
   * @param currency the price currency, noted with ISO 4217 code, or <code>null</code>
   * @param properties additional properties associating a key to a value or <code>null</code>
   * @since 5.7
   */
  public CatalogLight(String id, String name, String description, String category,
                      String information, String [] tags, Long creationDate, Float grade,
                      IResourceLocator icon, IResourceLocator planIcon, IResourceLocator model,
                      float width, float depth, float height, float elevation, float dropOnTopElevation,
                      boolean movable, LightSource [] lightSources, String staircaseCutOutShape,
                      float [][] modelRotation, boolean backFaceShown, Long modelSize, String creator,
                      boolean resizable, boolean deformable, boolean texturable, boolean horizontallyRotatable) {
    super(id, name, description, category, information, tags, creationDate, grade,
        icon, planIcon, model, width, depth, height, elevation, dropOnTopElevation, movable,
        false, staircaseCutOutShape, null, modelRotation, backFaceShown, modelSize, creator,
        resizable, deformable, texturable, horizontallyRotatable);
    this.lightSources = lightSources.length > 0
        ? lightSources.clone()
        : lightSources;
  }

  /**
   * Returns the sources managed by this light. Each light source point
   * is a percentage of the width, the depth and the height of this light,
   * with the abscissa origin at the left side of the piece,
   * the ordinate origin at the front side of the piece
   * and the elevation origin at the bottom side of the piece.
   * @return a copy of light sources array.
   */
  public LightSource [] getLightSources() {
    if (this.lightSources.length == 0) {
      return this.lightSources;
    } else {
      return this.lightSources.clone();
    }
  }
}
