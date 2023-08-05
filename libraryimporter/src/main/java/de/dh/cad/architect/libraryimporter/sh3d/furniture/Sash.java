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

/**
 * A sash (moving part) of a door or a window.
 * @author Emmanuel Puybaret
 * @since  1.7
 */
public class Sash {
  private final float xAxis;
  private final float yAxis;
  private final float width;
  private final float startAngle;
  private final float endAngle;

  /**
   * Creates a window sash.
   */
  public Sash(float xAxis, float yAxis,
              float width,
              float startAngle,
              float endAngle) {
    this.xAxis = xAxis;
    this.yAxis = yAxis;
    this.width = width;
    this.startAngle = startAngle;
    this.endAngle = endAngle;
  }

  /**
   * Returns the abscissa of the axis around which this sash turns, relatively to
   * the top left corner of the window or the door.
   * @return a value in percentage of the width of the door or the window.
   */
  public float getXAxis() {
    return this.xAxis;
  }

  /**
   * Returns the ordinate of the axis around which this sash turns, relatively to
   * the top left corner of the window or the door.
   * @return a value in percentage of the depth of the door or the window.
   */
  public float getYAxis() {
    return this.yAxis;
  }

  /**
   * Returns the width of this sash.
   * @return a value in percentage of the width of the door or the window.
   */
  public float getWidth() {
    return this.width;
  }

  /**
   * Returns the opening start angle of this sash.
   * @return an angle in radians.
   */
  public float getStartAngle() {
    return this.startAngle;
  }

  /**
   * Returns the opening end angle of this sash.
   * @return an angle in radians.
   */
  public float getEndAngle() {
    return this.endAngle;
  }
}