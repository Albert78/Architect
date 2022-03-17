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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.TreeMap;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import de.dh.cad.architect.libraryimporter.sh3d.AbstractCatalog;
import de.dh.cad.architect.libraryimporter.sh3d.DefaultLibrary;
import de.dh.cad.architect.utils.vfs.IDirectoryLocator;
import de.dh.cad.architect.utils.vfs.IResourceLocator;

/**
 * Furniture default catalog read from resources localized in <code>.properties</code> files.
 * Taken from SweetHome3D, modified by Daniel Hoeh
 * @author Emmanuel Puybaret
 */
public class DefaultFurnitureCatalog extends AbstractCatalog {
  /**
   * The keys of the properties values read in <code>.properties</code> files.
   */
  public enum PropertyKey {
    /**
     * The key for the ID of a piece of furniture (optional).
     * Two pieces of furniture read in a furniture catalog can't have the same ID
     * and the second one will be ignored.
     */
    ID("id"),
    /**
     * The key for the name of a piece of furniture (mandatory).
     */
    NAME("name"),
    /**
     * The key for the description of a piece of furniture (optional).
     * This may give detailed information about a piece of furniture.
     */
    DESCRIPTION("description"),
    /**
     * The key for some additional information associated to a piece of furniture (optional).
     * This information may contain some HTML code or a link to an external web site.
     */
    INFORMATION("information"),
    /**
     * The key for the tags or keywords associated to a piece of furniture (optional).
     * Tags are separated by commas with possible heading or trailing spaces.
     */
    TAGS("tags"),
    /**
     * The key for the creation or publication date of a piece of furniture at
     * <code>yyyy-MM-dd</code> format (optional).
     */
    CREATION_DATE("creationDate"),
    /**
     * The key for the grade of a piece of furniture (optional).
     */
    GRADE("grade"),
    /**
     * The key for the category's name of a piece of furniture (mandatory).
     * A new category with this name will be created if it doesn't exist.
     */
    CATEGORY("category"),
    /**
     * The key for the icon file of a piece of furniture (mandatory).
     * This icon file can be either the path to an image relative to classpath
     * or an absolute URL. It should be encoded in application/x-www-form-urlencoded
     * format if needed.
     */
    ICON("icon"),
    /**
     * The key for the SHA-1 digest of the icon file of a piece of furniture (optional).
     * This property is used to compare faster catalog resources with the ones of a read home,
     * and should be encoded in Base64.
     */
    ICON_DIGEST("iconDigest"),
    /**
     * The key for the plan icon file of a piece of furniture (optional).
     * This icon file can be either the path to an image relative to classpath
     * or an absolute URL. It should be encoded in application/x-www-form-urlencoded
     * format if needed.
     */
    PLAN_ICON("planIcon"),
    /**
     * The key for the SHA-1 digest of the plan icon file of a piece of furniture (optional).
     * This property is used to compare faster catalog resources with the ones of a read home,
     * and should be encoded in Base64.
     */
    PLAN_ICON_DIGEST("planIconDigest"),
    /**
     * The key for the 3D model file of a piece of furniture (mandatory).
     * The 3D model file can be either a path relative to classpath
     * or an absolute URL.  It should be encoded in application/x-www-form-urlencoded
     * format if needed.
     */
    MODEL("model"),
    /**
     * The key for the size of the 3D model of a piece of furniture (optional).
     * If model content is a file this should contain the file size.
     */
    MODEL_SIZE("modelSize"),
    /**
     * The key for the SHA-1 digest of the 3D model file of a piece of furniture (optional).
     * This property is used to compare faster catalog resources with the ones of a read home,
     * and should be encoded in Base64.
     */
    MODEL_DIGEST("modelDigest"),
    /**
     * The key for a piece of furniture with multiple parts (optional).
     * If the value of this key is <code>true</code>, all the files
     * stored in the same folder as the 3D model file (MTL, texture files...)
     * will be considered as being necessary to view correctly the 3D model.
     */
    MULTI_PART_MODEL("multiPartModel"),
    /**
     * The key for the width in centimeters of a piece of furniture (mandatory).
     */
    WIDTH("width"),
    /**
     * The key for the depth in centimeters of a piece of furniture (mandatory).
     */
    DEPTH("depth"),
    /**
     * The key for the height in centimeters of a piece of furniture (mandatory).
     */
    HEIGHT("height"),
    /**
     * The key for the movability of a piece of furniture (mandatory).
     * If the value of this key is <code>true</code>, the piece of furniture
     * will be considered as a movable piece.
     */
    MOVABLE("movable"),
    /**
     * The key for the door or window type of a piece of furniture (mandatory).
     * If the value of this key is <code>true</code>, the piece of furniture
     * will be considered as a door or a window.
     */
    DOOR_OR_WINDOW("doorOrWindow"),
    /**
     * The key for the shape of a door or window used to cut out walls when they intersect it (optional).
     * This shape should be defined with the syntax of the d attribute of a
     * <a href="http://www.w3.org/TR/SVG/paths.html">SVG path element</a>
     * and should fit in a square spreading from (0, 0) to (1, 1) which will be
     * scaled afterwards to the real size of the piece.
     * If not specified, then this shape will be automatically computed from the actual shape of the model.
     */
    DOOR_OR_WINDOW_CUT_OUT_SHAPE("doorOrWindowCutOutShape"),
    /**
     * The key for the wall thickness in centimeters of a door or a window (optional).
     * By default, a door or a window has the same depth as the wall it belongs to.
     */
    DOOR_OR_WINDOW_WALL_THICKNESS("doorOrWindowWallThickness"),
    /**
     * The key for the distance in centimeters of a door or a window to its wall (optional).
     * By default, this distance is zero.
     */
    DOOR_OR_WINDOW_WALL_DISTANCE("doorOrWindowWallDistance"),
    /**
     * The key for the wall cut out rule of a door or a window (optional, <code>true</code> by default).
     * By default, a door or a window placed on a wall and parallel to it will cut out the both sides of that wall
     * even if its depth is smaller than the wall thickness or if it intersects only one side of the wall.
     * If the value of this key is <code>false</code>, a door or a window will only dig the wall
     * at its intersection, and will cut the both sides of a wall only if it intersects both of them.
     */
    DOOR_OR_WINDOW_WALL_CUT_OUT_ON_BOTH_SIDES("doorOrWindowWallCutOutOnBothSides"),
    /**
     * The key for the width/depth deformability of a door or a window (optional, <code>true</code> by default).
     * By default, the depth of a door or a window can be changed and adapted to
     * the wall thickness where it's placed regardless of its width. To avoid this deformation
     * in the case of open doors, the value of this key can be set to <code>false</code>.
     * Doors and windows with their width/depth deformability set to <code>false</code>
     * and their {@link HomeDoorOrWindow#isBoundToWall() bouldToWall} flag set to <code>true</code>
     * will also make a hole in the wall when they are placed whatever their depth.
     */
    DOOR_OR_WINDOW_WIDTH_DEPTH_DEFORMABLE("doorOrWindowWidthDepthDeformable"),
    /**
     * The key for the sash axis distance(s) of a door or a window along X axis (optional).
     * If a door or a window has more than one sash, the values of each sash should be
     * separated by spaces.
     */
    DOOR_OR_WINDOW_SASH_X_AXIS("doorOrWindowSashXAxis"),
    /**
     * The key for the sash axis distance(s) of a door or a window along Y axis
     * (mandatory if sash axis distance along X axis is defined).
     */
    DOOR_OR_WINDOW_SASH_Y_AXIS("doorOrWindowSashYAxis"),
    /**
     * The key for the sash width(s) of a door or a window
     * (mandatory if sash axis distance along X axis is defined).
     */
    DOOR_OR_WINDOW_SASH_WIDTH("doorOrWindowSashWidth"),
    /**
     * The key for the sash start angle(s) of a door or a window
     * (mandatory if sash axis distance along X axis is defined).
     */
    DOOR_OR_WINDOW_SASH_START_ANGLE("doorOrWindowSashStartAngle"),
    /**
     * The key for the sash end angle(s) of a door or a window
     * (mandatory if sash axis distance along X axis is defined).
     */
    DOOR_OR_WINDOW_SASH_END_ANGLE("doorOrWindowSashEndAngle"),
    /**
     * The key for the abscissa(s) of light sources in a light (optional).
     * If a light has more than one light source, the values of each light source should
     * be separated by spaces.
     */
    LIGHT_SOURCE_X("lightSourceX"),
    /**
     * The key for the ordinate(s) of light sources in a light (mandatory if light source abscissa is defined).
     */
    LIGHT_SOURCE_Y("lightSourceY"),
    /**
     * The key for the elevation(s) of light sources in a light (mandatory if light source abscissa is defined).
     */
    LIGHT_SOURCE_Z("lightSourceZ"),
    /**
     * The key for the color(s) of light sources in a light (mandatory if light source abscissa is defined).
     */
    LIGHT_SOURCE_COLOR("lightSourceColor"),
    /**
     * The key for the diameter(s) of light sources in a light (optional).
     */
    LIGHT_SOURCE_DIAMETER("lightSourceDiameter"),
    /**
     * The key for the shape used to cut out upper levels when they intersect with a piece
     * like a staircase (optional). This shape should be defined with the syntax of
     * the d attribute of a <a href="http://www.w3.org/TR/SVG/paths.html">SVG path element</a>
     * and should fit in a square spreading from (0, 0) to (1, 1) which will be scaled afterwards
     * to the real size of the piece.
     */
    STAIRCASE_CUT_OUT_SHAPE("staircaseCutOutShape"),
    /**
     * The key for the elevation in centimeters of a piece of furniture (optional).
     */
    ELEVATION("elevation"),
    /**
     * The key for the preferred elevation (from the bottom of a piece) at which should be placed
     * an object dropped on a piece (optional). A negative value means that the piece should be ignored
     * when an object is dropped on it. By default, this elevation is equal to its height.
     */
    DROP_ON_TOP_ELEVATION("dropOnTopElevation"),
    /**
     * The key for the transformation matrix values applied to a piece of furniture (optional).
     * If the 3D model of a piece of furniture isn't correctly oriented,
     * the value of this key should give the 9 values of the transformation matrix
     * that will orient it correctly.
     */
    MODEL_ROTATION("modelRotation"),
    /**
     * The key for the creator of a piece of furniture (optional).
     * By default, creator is eTeks.
     */
    CREATOR("creator"),
    /**
     * The key for the resizability of a piece of furniture (optional, <code>true</code> by default).
     * If the value of this key is <code>false</code>, the piece of furniture
     * will be considered as a piece with a fixed size.
     */
    RESIZABLE("resizable"),
    /**
     * The key for the deformability of a piece of furniture (optional, <code>true</code> by default).
     * If the value of this key is <code>false</code>, the piece of furniture
     * will be considered as a piece that should always keep its proportions when resized.
     */
    DEFORMABLE("deformable"),
    /**
     * The key for the texturable capability of a piece of furniture (optional, <code>true</code> by default).
     * If the value of this key is <code>false</code>, the piece of furniture
     * will be considered as a piece that will always keep the same color or texture.
     */
    TEXTURABLE("texturable"),
    /**
     * The key for the ability of a piece of furniture to rotate around a horizontal axis (optional, <code>true</code> by default).
     * If the value of this key is <code>false</code>, the piece of furniture
     * will be considered as a piece that can't be horizontally rotated.
     */
    HORIZONTALLY_ROTATABLE("horizontallyRotatable"),
    /**
     * The key for the price of a piece of furniture (optional).
     */
    PRICE("price"),
    /**
     * The key for the VAT percentage of a piece of furniture (optional).
     */
    VALUE_ADDED_TAX_PERCENTAGE("valueAddedTaxPercentage"),
    /**
     * The key for the currency ISO 4217 code of the price of a piece of furniture (optional).
     */
    CURRENCY("currency");

    private String keyPrefix;

    private PropertyKey(String keyPrefix) {
      this.keyPrefix = keyPrefix;
    }

    /**
     * Returns the key for the piece property of the given index.
     */
    public String getKey(int pieceIndex) {
      return keyPrefix + "#" + pieceIndex;
    }

    /**
     * Returns the <code>PropertyKey</code> instance matching the given key prefix.
     */
    public static PropertyKey fromPrefix(String keyPrefix) {
      for (PropertyKey key : PropertyKey.values()) {
        if (key.keyPrefix.equals(keyPrefix)) {
          return key;
        }
      }
      throw new IllegalArgumentException("Unknow prefix " + keyPrefix);
    }
  }

  /**
   * The name of <code>.properties</code> family files in plugin furniture catalog files.
   */
  public static final String PLUGIN_FURNITURE_CATALOG_FAMILY = "PluginFurnitureCatalog";
  public static final String PLUGIN_FURNITURE_CATALOG_MAIN_FILE = PLUGIN_FURNITURE_CATALOG_FAMILY + ".properties";
  public static final String PLUGIN_FURNITURE_CATALOG_DE_FILE = PLUGIN_FURNITURE_CATALOG_FAMILY + "_de.properties";

  public static final String FURNITURE_LIBRARY_TYPE = "Furniture library";

  public static class SH3DFurnitureLibrary {
      protected final DefaultLibrary mLibrary;
      protected final Map<String, CatalogPieceOfFurniture>  mFurniture;

      public SH3DFurnitureLibrary(DefaultLibrary library, Map<String, CatalogPieceOfFurniture> furniture) {
          mLibrary = library;
          mFurniture = furniture;
      }

      public DefaultLibrary getLibrary() {
        return mLibrary;
      }

      public Map<String, CatalogPieceOfFurniture> getFurniture() {
        return mFurniture;
      }
  }

  /**
   * Reads each piece of furniture described in <code>PluginFurnitureCatalog.properties</code> file.
   * @throws IOException
   * @throws FileNotFoundException
   */
  public static SH3DFurnitureLibrary readFurniture(IDirectoryLocator catalog) throws IOException {
      Properties resource = new Properties();
      // Hack: Read the german version of the names
      IResourceLocator mainFileLocator = catalog.resolveResource(PLUGIN_FURNITURE_CATALOG_MAIN_FILE);
      try (InputStream is = mainFileLocator.inputStream()) {
          resource.load(is);
      }
      IResourceLocator deFileLocator = catalog.resolveResource(PLUGIN_FURNITURE_CATALOG_DE_FILE);
      try (InputStream is = deFileLocator.inputStream()) {
          resource.load(is);
      }
      Map<String, CatalogPieceOfFurniture> furniture = new TreeMap<>();
      int index = 0;
      while (true) {
          // Ignore furniture with a key ignored# set at true
          boolean ignored = false;
          try {
              String ignoredStr = resource.getProperty("ignored#" + (++index));
              ignored = !StringUtils.isEmpty(ignoredStr) && Boolean.parseBoolean(ignoredStr);
          } catch (MissingResourceException ex) {
              // Ignore
          }

          if (!ignored) {
              try {
                  CatalogPieceOfFurniture piece = readPieceOfFurniture(resource, index, catalog);
                  if (piece == null) {
                      // Read furniture until no data is found at current index
                      break;
                  } else {
                      String id = piece.getId();
                      if (id == null) {
                          id = UUID.randomUUID().toString();
                      }
                      furniture.put(id, piece);
                  }
              } catch (Exception e) {
                  throw new RuntimeException("Error parsing piece of furniture of index " + index, e);
              }
          }
      }
      DefaultLibrary library = new DefaultLibrary(catalog.toString(), FURNITURE_LIBRARY_TYPE, resource);
      return new SH3DFurnitureLibrary(library, furniture);
  }

  protected static final SimpleDateFormat DATE_FORMAT_yyyyMMdd = new SimpleDateFormat("yyyy-MM-dd");

  /**
   * Returns the piece of furniture at the given <code>index</code> of a
   * localized <code>resource</code> bundle.
   * @param resource             a resource bundle
   * @param index                the index of the read piece
   * @param furnitureCatalogUrl  the URL from which piece resources will be loaded
   *            or <code>null</code> if it's read from current classpath.
   * @param furnitureResourcesUrlBase the URL used as a base to build the URL to piece resources
   *            or <code>null</code> if it's read from current classpath or <code>furnitureCatalogUrl</code>
   * @return the read piece of furniture or <code>null</code> if the piece at the given index doesn't exist.
   * @throws MissingResourceException if mandatory keys are not defined.
   */
  protected static CatalogPieceOfFurniture readPieceOfFurniture(Properties resource, int index, IDirectoryLocator catalog) {
    String name = resource.getProperty(PropertyKey.NAME.getKey(index));
    if (StringUtils.isEmpty(name)) {
        // Return null if key name# doesn't exist
        return null;
    }
    String id = getOptionalString(resource, PropertyKey.ID.getKey(index), null);
    String description = getOptionalString(resource, PropertyKey.DESCRIPTION.getKey(index), null);
    String information = getOptionalString(resource, PropertyKey.INFORMATION.getKey(index), null);
    String tagsString = getOptionalString(resource, PropertyKey.TAGS.getKey(index), null);
    String category = resource.getProperty(PropertyKey.CATEGORY.getKey(index));
    String [] tags;
    if (tagsString != null) {
      tags = tagsString.split("\\s*,\\s*");
    } else {
      tags = new String[0];
    }
    String creationDateString = getOptionalString(resource, PropertyKey.CREATION_DATE.getKey(index), null);
    Long creationDate = null;
    if (creationDateString != null) {
      try {
        creationDate = DATE_FORMAT_yyyyMMdd.parse(creationDateString).getTime();
      } catch (ParseException ex) {
        throw new IllegalArgumentException("Can't parse date '"+ creationDateString + "'", ex);
      }
    }
    String gradeString = getOptionalString(resource, PropertyKey.GRADE.getKey(index), null);
    Float grade = null;
    if (gradeString != null) {
      grade = Float.valueOf(gradeString);
    }
    String iconStr = removeTrailingSlashFromPath(getOptionalString(resource, PropertyKey.ICON.getKey(index), null));
    String planIconStr = removeTrailingSlashFromPath(getOptionalString(resource, PropertyKey.PLAN_ICON.getKey(index), null));
    String modelStr = removeTrailingSlashFromPath(getOptionalString(resource, PropertyKey.MODEL.getKey(index), null));
//    boolean multiPartModel = getOptionalBoolean(resource, PropertyKey.MULTI_PART_MODEL.getKey(index), false);
    IResourceLocator icon = StringUtils.isEmpty(iconStr) ? null : catalog.resolveResource(iconStr);
    IResourceLocator planIcon = StringUtils.isEmpty(planIconStr) ? null : catalog.resolveResource(planIconStr);
    IResourceLocator model = StringUtils.isEmpty(modelStr) ? null : catalog.resolveResource(modelStr);
    float width = Float.parseFloat(resource.getProperty(PropertyKey.WIDTH.getKey(index)));
    float depth = Float.parseFloat(resource.getProperty(PropertyKey.DEPTH.getKey(index)));
    float height = Float.parseFloat(resource.getProperty(PropertyKey.HEIGHT.getKey(index)));
    float elevation = getOptionalFloat(resource, PropertyKey.ELEVATION.getKey(index), 0);
    float dropOnTopElevation = getOptionalFloat(resource, PropertyKey.DROP_ON_TOP_ELEVATION.getKey(index), height) / height;
    boolean movable = Boolean.parseBoolean(resource.getProperty(PropertyKey.MOVABLE.getKey(index)));
    boolean doorOrWindow = Boolean.parseBoolean(resource.getProperty(PropertyKey.DOOR_OR_WINDOW.getKey(index)));
    String staircaseCutOutShape = getOptionalString(resource, PropertyKey.STAIRCASE_CUT_OUT_SHAPE.getKey(index), null);
    float[][] modelRotation = getModelRotation(resource, PropertyKey.MODEL_ROTATION.getKey(index));
    // By default creator is eTeks
    String modelSizeString = getOptionalString(resource, PropertyKey.MODEL_SIZE.getKey(index), null);
    Long modelSize = null;
    if (modelSizeString != null) {
      modelSize = Long.parseLong(modelSizeString);
   }
    String creator = getOptionalString(resource, PropertyKey.CREATOR.getKey(index), null);
    boolean resizable = getOptionalBoolean(resource, PropertyKey.RESIZABLE.getKey(index), true);
    boolean deformable = getOptionalBoolean(resource, PropertyKey.DEFORMABLE.getKey(index), true);
    boolean texturable = getOptionalBoolean(resource, PropertyKey.TEXTURABLE.getKey(index), true);
    boolean horizontallyRotatable = getOptionalBoolean(resource, PropertyKey.HORIZONTALLY_ROTATABLE.getKey(index), true);

    if (doorOrWindow) {
      String doorOrWindowCutOutShape = getOptionalString(resource, PropertyKey.DOOR_OR_WINDOW_CUT_OUT_SHAPE.getKey(index), null);
      float wallThicknessPercentage = getOptionalFloat(
          resource, PropertyKey.DOOR_OR_WINDOW_WALL_THICKNESS.getKey(index), depth) / depth;
      float wallDistancePercentage = getOptionalFloat(
          resource, PropertyKey.DOOR_OR_WINDOW_WALL_DISTANCE.getKey(index), 0) / depth;
      boolean wallCutOutOnBothSides = getOptionalBoolean(
          resource, PropertyKey.DOOR_OR_WINDOW_WALL_CUT_OUT_ON_BOTH_SIDES.getKey(index), true);
      boolean widthDepthDeformable = getOptionalBoolean(
          resource, PropertyKey.DOOR_OR_WINDOW_WIDTH_DEPTH_DEFORMABLE.getKey(index), true);
      Sash [] sashes = getDoorOrWindowSashes(resource, index, width, depth);
      return new CatalogDoorOrWindow(id, name, description, category, information, tags, creationDate, grade,
          icon, planIcon, model, width, depth, height, elevation, dropOnTopElevation, movable,
          doorOrWindowCutOutShape, wallThicknessPercentage, wallDistancePercentage, wallCutOutOnBothSides, widthDepthDeformable, sashes,
          modelRotation, false, modelSize, creator, resizable, deformable, texturable);
    } else {
      LightSource [] lightSources = getLightSources(resource, index, width, depth, height);
      if (lightSources != null) {
        return new CatalogLight(id, name, description, category, information, tags, creationDate, grade,
            icon, planIcon, model, width, depth, height, elevation, dropOnTopElevation, movable,
            lightSources, staircaseCutOutShape, modelRotation, false, modelSize, creator,
            resizable, deformable, texturable, horizontallyRotatable);
      } else {
        return new CatalogPieceOfFurniture(id, name, description, category, information, tags, creationDate, grade,
            icon, planIcon, model, width, depth, height, elevation, dropOnTopElevation, movable,
            false, staircaseCutOutShape, null, modelRotation, false, modelSize, creator,
            resizable, deformable, texturable, horizontallyRotatable);
      }
    }
  }

  /**
   * Returns model rotation parsed from key value.
   */
  private static float [][] getModelRotation(Properties resource, String key) {
    try {
      String modelRotationString = resource.getProperty(key);
      if (StringUtils.isEmpty(modelRotationString)) {
          return null;
      }
      String [] values = modelRotationString.split(" ", 9);

      if (values.length == 9) {
        return new float [][] {{Float.parseFloat(values [0]),
                                Float.parseFloat(values [1]),
                                Float.parseFloat(values [2])},
                               {Float.parseFloat(values [3]),
                                Float.parseFloat(values [4]),
                                Float.parseFloat(values [5])},
                               {Float.parseFloat(values [6]),
                                Float.parseFloat(values [7]),
                                Float.parseFloat(values [8])}};
      } else {
        return null;
      }
    } catch (MissingResourceException ex) {
      return null;
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  /**
   * Returns optional door or windows sashes.
   */
  private static Sash[] getDoorOrWindowSashes(Properties resource, int index,
                                        float doorOrWindowWidth,
                                        float doorOrWindowDepth) throws MissingResourceException {
    Sash[] sashes;
    String sashXAxisString = getOptionalString(resource, PropertyKey.DOOR_OR_WINDOW_SASH_X_AXIS.getKey(index), null);
    if (sashXAxisString != null) {
      String [] sashXAxisValues = sashXAxisString.split(" ");
      // If doorOrWindowHingesX#i key exists the 3 other keys with the same count of numbers must exist too
      String [] sashYAxisValues = resource.getProperty(PropertyKey.DOOR_OR_WINDOW_SASH_Y_AXIS.getKey(index)).split(" ");
      if (sashYAxisValues.length != sashXAxisValues.length) {
        throw new IllegalArgumentException(
            "Expected " + sashXAxisValues.length + " values in " + PropertyKey.DOOR_OR_WINDOW_SASH_Y_AXIS.getKey(index) + " key");
      }
      String [] sashWidths = resource.getProperty(PropertyKey.DOOR_OR_WINDOW_SASH_WIDTH.getKey(index)).split(" ");
      if (sashWidths.length != sashXAxisValues.length) {
        throw new IllegalArgumentException(
            "Expected " + sashXAxisValues.length + " values in " + PropertyKey.DOOR_OR_WINDOW_SASH_WIDTH.getKey(index) + " key");
      }
      String [] sashStartAngles = resource.getProperty(PropertyKey.DOOR_OR_WINDOW_SASH_START_ANGLE.getKey(index)).split(" ");
      if (sashStartAngles.length != sashXAxisValues.length) {
        throw new IllegalArgumentException(
            "Expected " + sashXAxisValues.length + " values in " + PropertyKey.DOOR_OR_WINDOW_SASH_START_ANGLE.getKey(index) + " key");
      }
      String [] sashEndAngles = resource.getProperty(PropertyKey.DOOR_OR_WINDOW_SASH_END_ANGLE.getKey(index)).split(" ");
      if (sashEndAngles.length != sashXAxisValues.length) {
        throw new IllegalArgumentException(
            "Expected " + sashXAxisValues.length + " values in " + PropertyKey.DOOR_OR_WINDOW_SASH_END_ANGLE.getKey(index) + " key");
      }

      sashes = new Sash [sashXAxisValues.length];
      for (int i = 0; i < sashes.length; i++) {
        // Create the matching sash, converting cm to percentage of width or depth, and degrees to radians
        sashes [i] = new Sash(Float.parseFloat(sashXAxisValues [i]) / doorOrWindowWidth,
            Float.parseFloat(sashYAxisValues [i]) / doorOrWindowDepth,
            Float.parseFloat(sashWidths [i]) / doorOrWindowWidth,
            (float)Math.toRadians(Float.parseFloat(sashStartAngles [i])),
            (float)Math.toRadians(Float.parseFloat(sashEndAngles [i])));
      }
    } else {
      sashes = new Sash [0];
    }

    return sashes;
  }

  /**
   * Returns optional light sources.
   */
  private static LightSource[] getLightSources(Properties resource, int index,
                                         float lightWidth,
                                         float lightDepth,
                                         float lightHeight) throws MissingResourceException {
    LightSource [] lightSources = null;
    String lightSourceXString = getOptionalString(resource, PropertyKey.LIGHT_SOURCE_X.getKey(index), null);
    if (lightSourceXString != null) {
      String [] lightSourceX = lightSourceXString.split(" ");
      // If doorOrWindowHingesX#i key exists the 3 other keys with the same count of numbers must exist too
      String [] lightSourceY = resource.getProperty(PropertyKey.LIGHT_SOURCE_Y.getKey(index)).split(" ");
      if (lightSourceY.length != lightSourceX.length) {
        throw new IllegalArgumentException(
            "Expected " + lightSourceX.length + " values in " + PropertyKey.LIGHT_SOURCE_Y.getKey(index) + " key");
      }
      String [] lightSourceZ = resource.getProperty(PropertyKey.LIGHT_SOURCE_Z.getKey(index)).split(" ");
      if (lightSourceZ.length != lightSourceX.length) {
        throw new IllegalArgumentException(
            "Expected " + lightSourceX.length + " values in " + PropertyKey.LIGHT_SOURCE_Z.getKey(index) + " key");
      }
      String [] lightSourceColors = resource.getProperty(PropertyKey.LIGHT_SOURCE_COLOR.getKey(index)).split(" ");
      if (lightSourceColors.length != lightSourceX.length) {
        throw new IllegalArgumentException(
            "Expected " + lightSourceX.length + " values in " + PropertyKey.LIGHT_SOURCE_COLOR.getKey(index) + " key");
      }
      String lightSourceDiametersString = getOptionalString(resource, PropertyKey.LIGHT_SOURCE_DIAMETER.getKey(index), null);
      String [] lightSourceDiameters;
      if (lightSourceDiametersString != null) {
        lightSourceDiameters = lightSourceDiametersString.split(" ");
        if (lightSourceDiameters.length != lightSourceX.length) {
          throw new IllegalArgumentException(
              "Expected " + lightSourceX.length + " values in " + PropertyKey.LIGHT_SOURCE_DIAMETER.getKey(index) + " key");
        }
      } else {
        lightSourceDiameters = null;
      }

      lightSources = new LightSource [lightSourceX.length];
      for (int i = 0; i < lightSources.length; i++) {
        int color = lightSourceColors [i].startsWith("#")
            ? Integer.parseInt(lightSourceColors [i].substring(1), 16)
            : Integer.parseInt(lightSourceColors [i]);
        // Create the matching light source, converting cm to percentage of width, depth and height
        lightSources [i] = new LightSource(Float.parseFloat(lightSourceX [i]) / lightWidth,
            Float.parseFloat(lightSourceY [i]) / lightDepth,
            Float.parseFloat(lightSourceZ [i]) / lightHeight,
            color,
            lightSourceDiameters != null
                ? Float.parseFloat(lightSourceDiameters [i]) / lightWidth
                : null);
      }
    }
    return lightSources;
  }
}
