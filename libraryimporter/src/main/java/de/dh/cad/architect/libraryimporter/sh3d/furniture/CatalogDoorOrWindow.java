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
 * A door or a window of the catalog.
 * @author Emmanuel Puybaret
 * @since  1.7
 */
public class CatalogDoorOrWindow extends CatalogPieceOfFurniture {
  private final float   wallThickness;
  private final float   wallDistance;
  private final boolean wallCutOutOnBothSides;
  private final boolean widthDepthDeformable;
  private final Sash[] sashes;
  private final String  cutOutShape;

  /**
   * Creates an unmodifiable catalog door or window of the default catalog.
   * @param id    the id of the new door or window, or <code>null</code>
   * @param name  the name of the new door or window
   * @param description the description of the new door or window
   * @param information additional information associated to the new door or window
   * @param tags tags associated to the new door or window
   * @param creationDate creation date of the new door or window in milliseconds since the epoch
   * @param grade grade of the new door or window or <code>null</code>
   * @param icon content of the icon of the new door or window
   * @param planIcon content of the icon of the new piece displayed in plan
   * @param model content of the 3D model of the new door or window
   * @param width  the width in centimeters of the new door or window
   * @param depth  the depth in centimeters of the new door or window
   * @param height  the height in centimeters of the new door or window
   * @param elevation  the elevation in centimeters of the new door or window
   * @param dropOnTopElevation a percentage of the height at which should be placed
   *            an object dropped on the new piece
   * @param movable if <code>true</code>, the new door or window is movable
   * @param cutOutShape the shape used to cut out walls that intersect the new door or window
   * @param wallThickness a value in percentage of the depth of the new door or window
   * @param wallDistance a distance in percentage of the depth of the new door or window
   * @param wallCutOutOnBothSides  if <code>true</code> the new door or window should cut out
   *            the both sides of the walls it intersects
   * @param widthDepthDeformable if <code>false</code>, the width and depth of the new door or window may
   *            not be changed independently from each other
   * @param sashes the sashes attached to the new door or window
   * @param modelRotation the rotation 3 by 3 matrix applied to the door or window model
   * @param backFaceShown <code>true</code> if back face should be shown instead of front faces
   * @param modelSize size of the 3D model of the new light
   * @param creator the creator of the model
   * @param resizable if <code>true</code>, the size of the new door or window may be edited
   * @param deformable if <code>true</code>, the width, depth and height of the new piece may
   *            change independently from each other
   * @param texturable if <code>false</code> this piece should always keep the same color or texture.
   * @param price the price of the new door or window, or <code>null</code>
   * @param valueAddedTaxPercentage the Value Added Tax percentage applied to the
   *             price of the new door or window or <code>null</code>
   * @param currency the price currency, noted with ISO 4217 code, or <code>null</code>
   * @param properties additional properties associating a key to a value or <code>null</code>
   * @since 5.7
   */
  public CatalogDoorOrWindow(String id, String name, String description, String category,
                             String information, String [] tags, Long creationDate, Float grade,
                             IResourceLocator icon, IResourceLocator planIcon, IResourceLocator model,
                             float width, float depth, float height, float elevation, float dropOnTopElevation, boolean movable,
                             String cutOutShape, float wallThickness, float wallDistance,
                             boolean wallCutOutOnBothSides, boolean widthDepthDeformable, Sash [] sashes,
                             float [][] modelRotation, boolean backFaceShown, Long modelSize, String creator,
                             boolean resizable, boolean deformable, boolean texturable) {
    super(id, name, description, category, information, tags, creationDate, grade,
        icon, planIcon, model, width, depth, height, elevation, dropOnTopElevation, movable,
        true, null, null, modelRotation, backFaceShown, modelSize, creator, resizable, deformable, texturable, false);
    this.cutOutShape = cutOutShape;
    this.wallThickness = wallThickness;
    this.wallDistance = wallDistance;
    this.wallCutOutOnBothSides = wallCutOutOnBothSides;
    this.widthDepthDeformable = widthDepthDeformable;
    this.sashes = sashes;
  }

  /**
   * Returns the default thickness of the wall in which this door or window should be placed.
   * @return a value in percentage of the depth of the door or the window.
   */
  public float getWallThickness() {
    return this.wallThickness;
  }

  /**
   * Returns the default distance that should lie at the back side of this door or window.
   * @return a distance in percentage of the depth of the door or the window.
   */
  public float getWallDistance() {
    return this.wallDistance;
  }

  /**
   * Returns <code>true</code> if this door or window should cut out the both sides
   * of the walls it intersects, even if its front or back side are within the wall thickness.
   * @since 5.5
   */
  public boolean isWallCutOutOnBothSides() {
    return this.wallCutOutOnBothSides;
  }

  /**
   * Returns <code>false</code> if the width and depth of the new door or window may
   * not be changed independently from each other.
   * @since 5.5
   */
  @Override
public boolean isWidthDepthDeformable() {
    return this.widthDepthDeformable;
  }

  /**
   * Returns a copy of the sashes attached to this door or window.
   * If no sash is defined an empty array is returned.
   */
  public Sash [] getSashes() {
    if (this.sashes.length == 0) {
      return this.sashes;
    } else {
      return this.sashes.clone();
    }
  }

  /**
   * Returns the shape used to cut out walls that intersect this new door or window.
   */
  public String getCutOutShape() {
    return this.cutOutShape;
  }

  /**
   * Returns always <code>true</code>.
   */
  @Override
  public boolean isDoorOrWindow() {
    return true;
  }

  /**
   * Returns always <code>false</code>.
   */
  @Override
  public boolean isHorizontallyRotatable() {
    return false;
  }
}
