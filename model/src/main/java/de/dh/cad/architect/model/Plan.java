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
package de.dh.cad.architect.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.model.objects.Anchor;
import de.dh.cad.architect.model.objects.BaseObject;
import de.dh.cad.architect.model.objects.Ceiling;
import de.dh.cad.architect.model.objects.Covering;
import de.dh.cad.architect.model.objects.Dimensioning;
import de.dh.cad.architect.model.objects.Floor;
import de.dh.cad.architect.model.objects.GuideLine;
import de.dh.cad.architect.model.objects.IAnchorContainer;
import de.dh.cad.architect.model.objects.ObjectsGroup;
import de.dh.cad.architect.model.objects.SupportObject;
import de.dh.cad.architect.model.objects.Wall;
import de.dh.cad.architect.utils.IdGenerator;

// For serialization/deserialization with JAXB, use the API in the UI project
public class Plan implements IRootContainer, IAnchorContainer {
    protected String mId;
    protected Map<String, Anchor> mAnchors = new TreeMap<>();
    protected Map<String, Dimensioning> mDimensionings = new TreeMap<>();
    protected Map<String, Floor> mFloors = new TreeMap<>();
    protected Map<String, Wall> mWalls = new TreeMap<>();
    protected Map<String, Ceiling> mCeilings = new TreeMap<>();
    protected Map<String, Covering> mCoverings = new TreeMap<>();
    protected Map<String, SupportObject> mSupportObjects = new TreeMap<>();
    protected Map<String, GuideLine> mGuideLines = new TreeMap<>();
    protected Map<String, ObjectsGroup> mGroups = new TreeMap<>();

    public Plan() {
        // For JAXB
    }

    public Plan(String id) {
        mId = id;
    }

    public static Plan newPlan() {
        return new Plan(IdGenerator.generateUniqueId(Plan.class));
    }

    // Explicitly called by PlanTypeAdapter.PlanProxy after deserialization
    public void afterDeserialize() {
        forEach(bo -> {
            bo.setOwnerContainer_Internal(Plan.this);
        });
    }

    public void forEachMap(Consumer<Map<String, ? extends BaseObject>> c) {
        c.accept(mAnchors);
        c.accept(mFloors);
        c.accept(mDimensionings);
        c.accept(mWalls);
        c.accept(mCeilings);
        c.accept(mCoverings);
        c.accept(mSupportObjects);
        c.accept(mGuideLines);
        c.accept(mGroups);
    }

    public void forEach(Consumer<? super BaseObject> c) {
        forEachMap(new Consumer<Map<String, ? extends BaseObject>>() {
            @Override
            public void accept(Map<String, ? extends BaseObject> map) {
                for (BaseObject bo : map.values()) {
                    c.accept(bo);
                }
            }
        });
    }

    public String getId() {
        return mId;
    }

    public Map<String, Anchor> getAnchors() {
        return mAnchors;
    }

    public Map<String, Dimensioning> getDimensionings() {
        return mDimensionings;
    }

    public Map<String, Floor> getFloors() {
        return mFloors;
    }

    public Map<String, Wall> getWalls() {
        return mWalls;
    }

    public Map<String, Ceiling> getCeilings() {
        return mCeilings;
    }

    public Map<String, Covering> getCoverings() {
        return mCoverings;
    }

    public Map<String, SupportObject> getSupportObjects() {
        return mSupportObjects;
    }

    public Map<String, GuideLine> getGuideLines() {
        return mGuideLines;
    }

    public Map<String, ObjectsGroup> getGroups() {
        return mGroups;
    }

    public Map<String, ObjectsGroup> getTopLevelGroups() {
        Map<String, ObjectsGroup> result = new TreeMap<>();
        for (ObjectsGroup group : mGroups.values()) {
            if (group.getGroups().isEmpty()) {
                result.put(group.getId(), group);
            }
        }
        return result;
    }

    @Override
    public IAnchorContainer getAnchorContainer() {
        return this;
    }

    public Dimensioning createDimensioning(String id, String name, Position2D pos1, Position2D pos2, double viewDistance, ChangeSet changeSet) {
        Dimensioning result = Dimensioning.create(name, pos1, pos2, viewDistance, this, changeSet);
        changeSet.added(result);
        return result;
    }

    public boolean isRootObject(BaseObject obj) {
        return obj instanceof Dimensioning
            || obj instanceof Floor
            || obj instanceof Wall
            || obj instanceof Ceiling
            || obj instanceof Covering
            || obj instanceof SupportObject
            || obj instanceof GuideLine
            || obj instanceof ObjectsGroup;
    }

    @Override
    public BaseObject getObjectById(String id) {
        BaseObject result = mAnchors.get(id);
        if (result != null) {
            return result;
        }
        result = mDimensionings.get(id);
        if (result != null) {
            return result;
        }
        result = mFloors.get(id);
        if (result != null) {
            return result;
        }
        result = mWalls.get(id);
        if (result != null) {
            return result;
        }
        for (Wall wall : mWalls.values()) {
            result = wall.getWallHoleById(id);
            if (result != null) {
                return result;
            }
        }
        result = mCeilings.get(id);
        if (result != null) {
            return result;
        }
        result = mCoverings.get(id);
        if (result != null) {
            return result;
        }
        result = mSupportObjects.get(id);
        if (result != null) {
            return result;
        }
        result = mGuideLines.get(id);
        if (result != null) {
            return result;
        }
        result = mGroups.get(id);
        if (result != null) {
            return result;
        }
        // Other object types here
        return null;
    }

    @Override
    public Collection<BaseObject> getOwnedChildren() {
        Collection<BaseObject> result = new ArrayList<>(1000);
        forEach(bo -> result.add(bo));
        return result;
    }

    @Override
    public void addAnchor_Internal(Anchor anchor, ChangeSet changeSet) {
        mAnchors.put(anchor.getId(), anchor);
        changeSet.added(anchor);
    }

    @Override
    public void removeAnchor_Internal(Anchor anchor, ChangeSet changeSet) {
        mAnchors.remove(anchor.getId());
        changeSet.removed(anchor);
    }

    @Override
    public void addOwnedChild_Internal(BaseObject obj, ChangeSet changeSet) {
        if (obj instanceof Dimensioning o) {
            addObject_Internal(o, mDimensionings, changeSet);
        } else if (obj instanceof Floor o) {
            addObject_Internal(o, mFloors, changeSet);
        } else if (obj instanceof Wall o) {
            addObject_Internal(o, mWalls, changeSet);
        } else if (obj instanceof Ceiling o) {
            addObject_Internal(o, mCeilings, changeSet);
        } else if (obj instanceof Covering o) {
            addObject_Internal(o, mCoverings, changeSet);
        } else if (obj instanceof SupportObject o) {
            addObject_Internal(o, mSupportObjects, changeSet);
        } else if (obj instanceof GuideLine o) {
            addObject_Internal(o, mGuideLines, changeSet);
        } else if (obj instanceof ObjectsGroup o) {
            addObject_Internal(o, mGroups, changeSet);
        } else {
            throw new IllegalArgumentException("Object '" + obj + "' of unsupported type cannot be added");
        }
    }

    protected <T extends BaseObject> void addObject_Internal(T object, Map<String, T> dataStructure, ChangeSet changeSet) {
        String id = object.getId();
        object.setOwnerContainer_Internal(this);
        dataStructure.put(id, object);
        changeSet.added(object);
    }

    public Collection<BaseObject> removeObjects_Internal(ChangeSet changeSet, BaseObject... objs) {
        return removeObjects_Internal(Arrays.asList(objs), changeSet);
    }

    public Collection<BaseObject> removeObjects_Internal(Collection<? extends BaseObject> objs, ChangeSet changeSet) {
        Collection<BaseObject> result = new ArrayList<>();
        for (BaseObject obj : objs) {
            result.addAll(removeOwnedChild_Internal(obj, changeSet));
        }
        return result;
    }

    @Override
    public Collection<BaseObject> removeOwnedChild_Internal(BaseObject object, ChangeSet changeSet) {
        Collection<BaseObject> result = new ArrayList<>();
        String id = object.getId();
        if (object instanceof Anchor) {
            mAnchors.remove(id);
        } else if (object instanceof Dimensioning) {
            mDimensionings.remove(id);
        } else if (object instanceof Floor) {
            mFloors.remove(id);
        } else if (object instanceof Wall) {
            mWalls.remove(id);
        } else if (object instanceof Ceiling) {
            mCeilings.remove(id);
        } else if (object instanceof Covering) {
            mCoverings.remove(id);
        } else if (object instanceof SupportObject) {
            mSupportObjects.remove(id);
        } else if (object instanceof GuideLine) {
            mGuideLines.remove(id);
        } else if (object instanceof ObjectsGroup) {
            mGroups.remove(id);
        } else {
            throw new IllegalArgumentException("Object '" + object + "' of unknown type cannot be removed");
        }
        result.add(object);
        changeSet.removed(result);
        return result;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [Id=" + mId + "]";
    }
}
