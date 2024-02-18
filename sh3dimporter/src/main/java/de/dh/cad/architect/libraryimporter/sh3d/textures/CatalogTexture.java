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
package de.dh.cad.architect.libraryimporter.sh3d.textures;

import de.dh.cad.architect.utils.vfs.IResourceLocator;

/**
 * A texture in textures catalog.
 * @author Emmanuel Puybaret
 */
public class CatalogTexture {
//  private static final long serialVersionUID = 1L;
//  private static final byte [][]  EMPTY_CRITERIA     = new byte [0][];

  private final String          id;
  private final String          name;
  private final IResourceLocator         image;
  private final float           width;
  private final float           height;
  private final String          creator;
//  private final boolean         modifiable;

//  private TexturesCategory      category;
  private String                category;
//  private byte []               filterCollationKey;

//  private static final Collator COMPARATOR;
//  private static final Map<String, byte [][]> recentFilters;
//
//  static {
//    COMPARATOR = Collator.getInstance();
//    COMPARATOR.setStrength(Collator.PRIMARY);
//    recentFilters = new WeakHashMap<String, byte[][]>();
//  }
//

  /**
   * Creates a catalog texture.
   * @param id the ID of this texture
   * @param name the name of this texture
   * @param image the content of the image used for this texture
 * @param category
   * @param width the width of the texture in centimeters
   * @param height the height of the texture in centimeters
   * @param modifiable <code>true</code> if this texture can be modified
   */
  public CatalogTexture(String id,
                        String name, IResourceLocator image,
                        String category, float width, float height,
                        String creator) {
    this.id = id;
    this.name = name;
    this.image = image;
    this.category = category;
    this.width = width;
    this.height = height;
    this.creator = creator;
  }

  /**
   * Returns the ID of this texture or <code>null</code>.
   * @since 2.3
   */
  public String getId() {
    return this.id;
  }

  /**
   * Returns the name of this texture.
   */
  public String getName() {
    return this.name;
  }

  /**
   * Returns the content of the image used for this texture.
   */
  public IResourceLocator getImage() {
    return this.image;
  }

  /**
   * Returns the icon of this texture.
   * @return the image of this texture.
   * @since 4.4
   */
  public IResourceLocator getIcon() {
    return getImage();
  }

  /**
   * Returns the width of the image in centimeters.
   */
  public float getWidth() {
    return this.width;
  }

  /**
   * Returns the height of the image in centimeters.
   */
  public float getHeight() {
    return this.height;
  }

  /**
   * Returns the creator of this texture or <code>null</code>.
   * @since 2.3
   */
  public String getCreator() {
    return this.creator;
  }

  /**
   * Returns the category of this texture.
   */
  public String getCategory() {
    return this.category;
  }

  /**
   * Sets the category of this texture.
   */
  void setCategory(String category) {
    this.category = category;
  }

  /**
   * Returns true if this texture and the one in parameter are the same objects.
   * Note that, from version 3.6, two textures can have the same name.
   */
  @Override
  public boolean equals(Object obj) {
    return super.equals(obj);
  }

  /**
   * Returns default hash code.
   */
   @Override
  public int hashCode() {
    return super.hashCode();
  }

//  /**
//   * Compares the names of this texture and the one in parameter.
//   */
//  public int compareTo(CatalogTexture texture) {
//    int nameComparison = COMPARATOR.compare(this.name, texture.name);
//    if (nameComparison != 0) {
//      return nameComparison;
//    } else {
//      return this.modifiable == texture.modifiable
//          ? 0
//          : (this.modifiable ? 1 : -1);
//    }
//  }
//
//  /**
//   * Returns <code>true</code> if this texture matches the given <code>filter</code> text.
//   * Each substring of the <code>filter</code> is considered as a search criterion that can match
//   * the name, the category name or the creator of this texture.
//   * @since 4.4
//   */
//  public boolean matchesFilter(String filter) {
//    byte [][] filterCriteriaCollationKeys = getFilterCollationKeys(filter);
//    int checkedCriteria = 0;
//    if (filterCriteriaCollationKeys.length > 0) {
//      byte [] furnitureCollationKey = getTextureCollationKey();
//      for (int i = 0; i < filterCriteriaCollationKeys.length; i++) {
//        if (isSubCollationKey(furnitureCollationKey, filterCriteriaCollationKeys [i], 0)) {
//          checkedCriteria++;
//        } else {
//          break;
//        }
//      }
//    }
//    return checkedCriteria == filterCriteriaCollationKeys.length;
//  }
//
//  /**
//   * Returns the collation key bytes of each criterion in the given <code>filter</code>.
//   */
//  private byte [][] getFilterCollationKeys(String filter) {
//    if (filter.length() == 0) {
//      return EMPTY_CRITERIA;
//    }
//    byte [][] filterCollationKeys = recentFilters.get(filter);
//    if (filterCollationKeys == null) {
//      // Each substring in filter is a search criterion that must be verified
//      String [] filterCriteria = filter.split("\\s|\\p{Punct}|\\|");
//      List<byte []> filterCriteriaCollationKeys = new ArrayList<byte []>(filterCriteria.length);
//      for (String criterion : filterCriteria) {
//        if (criterion.length() > 0) {
//          filterCriteriaCollationKeys.add(COMPARATOR.getCollationKey(criterion).toByteArray());
//        }
//      }
//      if (filterCriteriaCollationKeys.size() == 0) {
//        filterCollationKeys = EMPTY_CRITERIA;
//      } else {
//        filterCollationKeys = filterCriteriaCollationKeys.toArray(new byte [filterCriteriaCollationKeys.size()][]);
//      }
//      recentFilters.put(filter, filterCollationKeys);
//    }
//    return filterCollationKeys;
//  }
//
//  /**
//   * Returns the collation key bytes used to compare the given <code>texture</code> with filter.
//   */
//  private byte [] getTextureCollationKey() {
//    if (this.filterCollationKey == null) {
//      // Prepare filter string collation key
//      // (collect the name, category and creator of each texture)
//      StringBuilder search = new StringBuilder();
//      search.append(getName());
//      search.append('|');
//      if (getCategory() != null) {
//        search.append(getCategory().getName());
//        search.append('|');
//      }
//      if (getCreator() != null) {
//        search.append(getCreator());
//        search.append('|');
//      }
//      this.filterCollationKey = COMPARATOR.getCollationKey(search.toString()).toByteArray();
//    }
//    return this.filterCollationKey;
//  }
//
//  /**
//   * Returns <code>true</code> if the given filter collation key is a sub part of the first array collator key.
//   */
//  private boolean isSubCollationKey(byte [] collationKey, byte [] filterCollationKey, int start) {
//    // Ignore the last 4 bytes of the collator key
//    for (int i = start, n = collationKey.length - 4, m = filterCollationKey.length - 4; i < n && i < n - m + 1; i++) {
//      if (collationKey [i] == filterCollationKey [0]) {
//        for (int j = 1; j < m; j++) {
//          if (collationKey [i + j] != filterCollationKey [j]) {
//            return isSubCollationKey(collationKey, filterCollationKey, i + 1);
//          }
//        }
//        return true;
//      }
//    }
//    return false;
//  }
}
