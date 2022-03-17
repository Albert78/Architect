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
package de.dh.cad.architect.libraryimporter.sh3d.textures;

import java.io.IOException;
import java.io.InputStream;
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
 * Textures default catalog read from localized resources.
 * @author Emmanuel Puybaret
 */
public class DefaultTexturesCatalog extends AbstractCatalog {
  /**
   * The keys of the properties values read in <code>.properties</code> files.
   */
  public enum PropertyKey {
    /**
     * The key for the ID of a texture (optional).
     * Two textures read in a texture catalog can't have the same ID
     * and the second one will be ignored.
     */
    ID("id"),
    /**
     * The key for the name of a texture (mandatory).
     */
    NAME("name"),
    /**
     * The key for the category's name of a texture (mandatory).
     * A new category with this name will be created if it doesn't exist.
     */
    CATEGORY("category"),
    /**
     * The key for the image file of a texture (mandatory).
     * This image file can be either the path to an image relative to classpath
     * or an absolute URL. It should be encoded in application/x-www-form-urlencoded format
     * if needed.
     */
    IMAGE("image"),
    /**
     * The key for the SHA-1 digest of the image file of a texture (optional).
     * This property is used to compare faster catalog resources with the ones of a read home,
     * and should be encoded in Base64.
     */
    IMAGE_DIGEST("imageDigest"),
    /**
     * The key for the width in centimeters of a texture (mandatory).
     */
    WIDTH("width"),
    /**
     * The key for the height in centimeters of a texture (mandatory).
     */
    HEIGHT("height"),
    /**
     * The key for the creator of a texture (optional).
     * By default, creator is <code>null</code>.
     */
    CREATOR("creator");

    private String keyPrefix;

    private PropertyKey(String keyPrefix) {
      this.keyPrefix = keyPrefix;
    }

    /**
     * Returns the key for the texture property of the given index.
     */
    public String getKey(int textureIndex) {
      return keyPrefix + "#" + textureIndex;
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
   * The name of <code>.properties</code> family files in plugin textures catalog files.
   */
  public static final String PLUGIN_TEXTURES_CATALOG_FAMILY = "PluginTexturesCatalog";
  public static final String PLUGIN_TEXTURES_CATALOG_MAIN_FILE = PLUGIN_TEXTURES_CATALOG_FAMILY + ".properties";
  public static final String PLUGIN_TEXTURES_CATALOG_DE_FILE = PLUGIN_TEXTURES_CATALOG_FAMILY + "_de.properties";

  public static final String TEXTURES_LIBRARY_TYPE = "Textures library";

  public static class SH3DTexturesLibrary {
      protected final DefaultLibrary mLibrary;
      protected final Map<String, CatalogTexture>  mTextures;

      public SH3DTexturesLibrary(DefaultLibrary library, Map<String, CatalogTexture> textures) {
          mLibrary = library;
          mTextures = textures;
      }

      public DefaultLibrary getLibrary() {
        return mLibrary;
      }

      public Map<String, CatalogTexture> getTextures() {
        return mTextures;
      }
  }

  public static SH3DTexturesLibrary readTextures(IDirectoryLocator catalog) throws IOException {
      Properties resource = new Properties();
      // Hack: Read the german version of the names
      IResourceLocator mainFileLocator = catalog.resolveResource(PLUGIN_TEXTURES_CATALOG_MAIN_FILE);
      try (InputStream is = mainFileLocator.inputStream()) {
          resource.load(is);
      }
      IResourceLocator deFileLocator = catalog.resolveResource(PLUGIN_TEXTURES_CATALOG_DE_FILE);
      try (InputStream is = deFileLocator.inputStream()) {
          resource.load(is);
      }
      Map<String, CatalogTexture> textures = new TreeMap<>();
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
                  CatalogTexture texture = readTexture(resource, index, catalog);
                  if (texture == null) {
                      // Read furniture until no data is found at current index
                      break;
                  } else {
                      String id = texture.getId();
                      if (id == null) {
                          id = UUID.randomUUID().toString();
                      }
                      textures.put(id, texture);
                  }
              } catch (Exception e) {
                  throw new RuntimeException("Error parsing piece of furniture of index " + index, e);
              }
          }
      }
      DefaultLibrary library = new DefaultLibrary(catalog.toString(), TEXTURES_LIBRARY_TYPE, resource);
      return new SH3DTexturesLibrary(library, textures);
  }

  protected static CatalogTexture readTexture(Properties resource, int index, IDirectoryLocator catalog) {
      String name = resource.getProperty(PropertyKey.NAME.getKey(index));
      if (StringUtils.isEmpty(name)) {
          // Return null if key name# doesn't exist
          return null;
      }
      String id = getOptionalString(resource, PropertyKey.ID.getKey(index), null);
      String imageStr = removeTrailingSlashFromPath(getOptionalString(resource, PropertyKey.IMAGE.getKey(index), null));
      IResourceLocator image = StringUtils.isEmpty(imageStr) ? null : catalog.resolveResource(imageStr);

      float width = Float.parseFloat(resource.getProperty(PropertyKey.WIDTH.getKey(index)));
      float height = Float.parseFloat(resource.getProperty(PropertyKey.HEIGHT.getKey(index)));
      String creator = getOptionalString(resource, PropertyKey.CREATOR.getKey(index), null);

      String category = getOptionalString(resource, PropertyKey.CATEGORY.getKey(index), null);

    return new CatalogTexture(id, name, image, category, width, height, creator);
  }
}
