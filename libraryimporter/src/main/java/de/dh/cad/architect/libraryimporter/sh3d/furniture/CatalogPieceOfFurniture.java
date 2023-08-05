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
package de.dh.cad.architect.libraryimporter.sh3d.furniture;

import de.dh.cad.architect.utils.vfs.IResourceLocator;

/**
 * A catalog piece of furniture.
 * This class is more or less copied from SweetHome3D in august 2020.
 * @author Emmanuel Puybaret
 */
public class CatalogPieceOfFurniture {
  private final String              id;
  private final String              name;
  private final String              description;
  private final String              information;
  private final String[]            tags;
  private final Long                creationDate;
  private final Float               grade;
  private final IResourceLocator    icon;
  private final IResourceLocator    planIcon;
  private final IResourceLocator    model;
  private final float               width;
  private final float               depth;
  private final float               height;
  private final float               elevation;
  private final float               dropOnTopElevation;
  private final boolean             movable;
  private final boolean             doorOrWindow;
  private final String              staircaseCutOutShape;
  private final float[][]           modelRotation;
  private final Long                modelSize;
  private final String              creator;
  private final boolean             backFaceShown;
  private final Integer             color;
  private final boolean             resizable;
  private final boolean             deformable;
  private final boolean             texturable;
  private final boolean             horizontallyRotatable;
//  private final BigDecimal          price;
//  private final BigDecimal          valueAddedTaxPercentage;
//  private final String              currency;
//  private final Map<String, String> properties;

//  private FurnitureCategory         category;
  private final String              category;
//  private byte []                   filterCollationKey;

  public CatalogPieceOfFurniture(String id, String name, String description, String category,
                                  String information, String [] tags, Long creationDate, Float grade,
                                  IResourceLocator icon, IResourceLocator planIcon, IResourceLocator model,
                                  float width, float depth, float height,
                                  float elevation, float dropOnTopElevation,
                                  boolean movable, boolean doorOrWindow, String staircaseCutOutShape,
                                  Integer color, float [][] modelRotation, boolean backFaceShown,
                                  Long modelSize, String creator, boolean resizable,
                                  boolean deformable, boolean texturable, boolean horizontallyRotatable) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.category = category;
    this.information = information;
    this.tags = tags;
    this.creationDate = creationDate;
    this.grade = grade;
    this.icon = icon;
    this.planIcon = planIcon;
    this.model = model;
    this.width = width;
    this.depth = depth;
    this.height = height;
    this.elevation = elevation;
    this.dropOnTopElevation = dropOnTopElevation;
    this.movable = movable;
    this.doorOrWindow = doorOrWindow;
    this.color = color;
    this.staircaseCutOutShape = staircaseCutOutShape;
    this.creator = creator;
    this.horizontallyRotatable = horizontallyRotatable;
    this.modelRotation = modelRotation == null ? null : deepCopy(modelRotation);
    this.backFaceShown = backFaceShown;
    this.modelSize = modelSize;
    this.resizable = resizable;
    this.deformable = deformable;
    this.texturable = texturable;
  }

  /**
   * Returns the ID of this piece of furniture or <code>null</code>.
   */
  public String getId() {
    return this.id;
  }

  /**
   * Returns the name of this piece of furniture.
   */
  public String getName() {
    return this.name;
  }

  /**
   * Returns the description of this piece of furniture.
   * The returned value may be <code>null</code>.
   */
  public String getDescription() {
    return this.description;
  }

  /**
   * Returns the category of this piece of furniture.
   */
  public String getCategory() {
    return this.category;
  }

  /**
   * Returns the additional information associated to this piece, or <code>null</code>.
   * @since 3.6
   */
  public String getInformation() {
    return this.information;
  }

  /**
   * Returns the tags associated to this piece.
   * @since 3.6
   */
  public String [] getTags() {
    return this.tags;
  }

  /**
   * Returns the creation date of this piece in milliseconds since the epoch,
   * or <code>null</code> if no date is given to this piece.
   * @since 3.6
   */
  public Long getCreationDate() {
    return this.creationDate;
  }

  /**
   * Returns the grade of this piece, or <code>null</code> if no grade is given to this piece.
   * @since 3.6
   */
  public Float getGrade() {
    return this.grade;
  }

  /**
   * Returns the depth of this piece of furniture.
   */
  public float getDepth() {
    return this.depth;
  }

  /**
   * Returns the height in cm.
   */
  public float getHeight() {
    return this.height;
  }

  /**
   * Returns the width in cm.
   */
  public float getWidth() {
    return this.width;
  }

  /**
   * Returns the elevation in cm.
   */
  public float getElevation() {
    return this.elevation;
  }

  /**
   * Returns the elevation at which should be placed an object dropped on this piece.
   * @return a percentage of the height of this piece. A negative value means that the piece
   *         should be ignored when an object is dropped on it.
   * @since 4.4
   */
  public float getDropOnTopElevation() {
    return this.dropOnTopElevation;
  }

  /**
   * Returns <code>true</code> if this piece of furniture is movable.
   */
  public boolean isMovable() {
    return this.movable;
  }

  /**
   * Returns <code>true</code> if this piece of furniture is a door or a window.
   * As this method existed before {@linkplain CatalogDoorOrWindow CatalogDoorOrWindow} class,
   * you shouldn't rely on the value returned by this method to guess if a piece
   * is an instance of <code>DoorOrWindow</code> class.
   */
  public boolean isDoorOrWindow() {
    return this.doorOrWindow;
  }

  /**
   * Returns the icon of this piece of furniture.
   */
  public IResourceLocator getIcon() {
    return this.icon;
  }

  /**
   * Returns the icon of this piece of furniture displayed in plan or <code>null</code>.
   * @since 2.2
   */
  public IResourceLocator getPlanIcon() {
    return this.planIcon;
  }

  /**
   * Returns the 3D model of this piece of furniture.
   */
  public IResourceLocator getModel() {
    return this.model;
  }

  /**
   * Returns the size of the 3D model of this piece of furniture.
   * @since 5.5
   */
  public Long getModelSize() {
    return this.modelSize;
  }

  /**
   * Returns the rotation 3 by 3 matrix of this piece of furniture that ensures
   * its model is correctly oriented. This rotation is a rotation in a right-handed coordinate system.
   * Values: {@code mij = modelRotation[i][j]}
   */
  public float [][] getModelRotation() {
    // Return a deep copy to avoid any misuse of piece data
    return deepCopy(this.modelRotation);
  }

  private static float [][] deepCopy(float [][] value) {
    return value == null ? null : new float [][] {{value[0][0], value[0][1], value[0][2]},
                           {value[1][0], value[1][1], value[1][2]},
                           {value[2][0], value[2][1], value[2][2]}};
  }

  /**
   * Returns the rotation 3 by 3 matrix of this piece of furniture that ensures
   * its model is correctly oriented. This rotation is defined in a left-handed coordinate system.
   * Values: {@code mij = modelRotation[i][j]}
   */
  public float [][] getModelRotationJavaFX() {
    return convertRotationToJavaFX(modelRotation);
  }

  public static float[][] convertRotationToJavaFX(float[][] modelRotation) {
      if (modelRotation == null) {
          return null;
      }
      // JavaFX uses LHS:
      // Z coordinate grows in direction away from the observer
      // Y coordinate grows to the bottom
      // SweetHome3D / Obj file format use RHS:
      // Z grows in direction to the observer
      // Y grows to the top

      // Let mi be the original model coordinates read by the Obj file reader.
      // Our object format reader already converts the raw model coordinates from Obj file format to JavaFX:
      // T = Scale(x = 1, y = -1, z = -1); // Transform from Obj format to JavaFX. This transform gets applied by the Obj reader.
      // Let ji be the converted model coordinates for JavaFX:
      // ji = T * mi
      // Unfortunately, the model rotation for the SH3D model must be applied first, so we have to revert T first, then apply the model rotation, then
      // We have: T = T^-1
      // Let R be the rotation matrix.
      // Then we calculate the final rotated model coords like this:
      // T * R * T^-1 * ji = T * R * T^-1 * T * mi
      // So we have to apply:
      // T * R * T^-1:
      return new float[][] {
          {modelRotation[0][0], -modelRotation[0][1], -modelRotation[0][2]},
          {-modelRotation[1][0], modelRotation[1][1], modelRotation[1][2]},
          {-modelRotation[2][0], modelRotation[2][1], modelRotation[2][2]}
      };
  }

  /**
   * Returns the shape used to cut out upper levels when they intersect with the piece
   * like a staircase.
   * @since 3.4
   */
  public String getStaircaseCutOutShape() {
    return this.staircaseCutOutShape;
  }

  /**
   * Returns the creator of this piece.
   */
  public String getCreator() {
    return this.creator;
  }

  /**
   * Returns <code>true</code> if the back face of the piece of furniture
   * model should be displayed.
   */
  public boolean isBackFaceShown() {
    return this.backFaceShown;
  }

  /**
   * Returns the color of this piece of furniture.
   */
  public Integer getColor() {
    return this.color;
  }

  /**
   * Returns <code>true</code> if this piece is resizable.
   */
  public boolean isResizable() {
    return this.resizable;
  }

  /**
   * Returns <code>true</code> if this piece is deformable.
   * @since 3.0
   */
  public boolean isDeformable() {
    return this.deformable;
  }

  /**
   * Returns <code>true</code> if this piece is deformable.
   * @since 5.5
   */
  public boolean isWidthDepthDeformable() {
    return isDeformable();
  }

  /**
   * Returns <code>false</code> if this piece should always keep the same color or texture.
   * @since 3.0
   */
  public boolean isTexturable() {
    return this.texturable;
  }

  /**
   * Returns <code>false</code> if this piece should not rotate around an horizontal axis.
   * @since 5.5
   */
  public boolean isHorizontallyRotatable() {
    return this.horizontallyRotatable;
  }
}
