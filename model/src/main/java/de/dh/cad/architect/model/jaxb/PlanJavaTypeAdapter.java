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
package de.dh.cad.architect.model.jaxb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.adapters.XmlAdapter;

import de.dh.cad.architect.model.ChangeSet;
import de.dh.cad.architect.model.Plan;
import de.dh.cad.architect.model.objects.Anchor;
import de.dh.cad.architect.model.objects.Ceiling;
import de.dh.cad.architect.model.objects.Covering;
import de.dh.cad.architect.model.objects.Dimensioning;
import de.dh.cad.architect.model.objects.Floor;
import de.dh.cad.architect.model.objects.GuideLine;
import de.dh.cad.architect.model.objects.ObjectsGroup;
import de.dh.cad.architect.model.objects.SupportObject;
import de.dh.cad.architect.model.objects.Wall;

/**
 * Used to support JAXB serialization/deserialization because {@link Plan} uses {@link Map}.
 */
public class PlanJavaTypeAdapter extends XmlAdapter<PlanJavaTypeAdapter.PlanProxy, Plan> {
    public static class PlanProxy {
        protected String mId;
        protected List<Anchor> mAnchors = new ArrayList<>();
        protected List<Dimensioning> mDimensionings = new ArrayList<>();
        protected List<Floor> mFloors = new ArrayList<>();
        protected List<Wall> mWalls = new ArrayList<>();
        protected List<Ceiling> mCeilings = new ArrayList<>();
        protected List<Covering> mCoverings = new ArrayList<>();
        protected List<SupportObject> mSupportObjects = new ArrayList<>();
        protected List<GuideLine> mGuideLines = new ArrayList<>();
        protected List<ObjectsGroup> mGroups = new ArrayList<>();

        public PlanProxy() {
            // For JAXB
        }

        protected PlanProxy(String id,
            Collection<Anchor> anchors,
            Collection<Dimensioning> dimensionings,
            Collection<Floor> floors,
            Collection<Wall> walls,
            Collection<Ceiling> ceilings,
            Collection<Covering> coverings,
            Collection<SupportObject> supportObjects,
            Collection<GuideLine> guideLines,
            Collection<ObjectsGroup> groups) {
            mId = id;
            mAnchors.addAll(anchors);
            mFloors.addAll(floors);
            mWalls.addAll(walls);
            mCeilings.addAll(ceilings);
            mCoverings.addAll(coverings);
            mSupportObjects.addAll(supportObjects);
            mDimensionings.addAll(dimensionings);
            mGuideLines.addAll(guideLines);
            mGroups.addAll(groups);
        }

        public static PlanProxy from(Plan plan) {
            return new PlanProxy(
                plan.getId(),
                plan.getAnchors().values(),
                plan.getDimensionings().values(),
                plan.getFloors().values(),
                plan.getWalls().values(),
                plan.getCeilings().values(),
                plan.getCoverings().values(),
                plan.getSupportObjects().values(),
                plan.getGuideLines().values(),
                plan.getGroups().values());
        }

        public Plan toPlan() {
            Plan result = new Plan(mId);
            ChangeSet unUsed = new ChangeSet();
            for (Anchor anchor : mAnchors) {
                result.addAnchor_Internal(anchor, unUsed);
            }
            for (Dimensioning dimensioning : mDimensionings) {
                result.addOwnedChild_Internal(dimensioning, unUsed);
            }
            for (Floor floor : mFloors) {
                result.addOwnedChild_Internal(floor, unUsed);
            }
            for (Wall wall : mWalls) {
                result.addOwnedChild_Internal(wall, unUsed);
            }
            for (Ceiling ceiling : mCeilings) {
                result.addOwnedChild_Internal(ceiling, unUsed);
            }
            for (Covering covering : mCoverings) {
                result.addOwnedChild_Internal(covering, unUsed);
            }
            for (SupportObject so : mSupportObjects) {
                result.addOwnedChild_Internal(so, unUsed);
            }
            for (GuideLine guideLine : mGuideLines) {
                result.addOwnedChild_Internal(guideLine, unUsed);
            }
            for (ObjectsGroup group : mGroups) {
                result.addOwnedChild_Internal(group, unUsed);
            }
            result.afterDeserialize();
            return result;
        }

        @XmlAttribute(name = "id")
        public String getId() {
            return mId;
        }

        public void setId(String value) {
            mId = value;
        }

        @XmlElementWrapper(name = "Anchors")
        @XmlElement(name = "Anchor")
        public List<Anchor> getAnchors() {
            return mAnchors;
        }

        public void setAnchors(List<Anchor> value) {
            mAnchors = value;
        }

        @XmlElementWrapper(name = "Dimensionings")
        @XmlElement(name = "Dimensioning")
        public List<Dimensioning> getDimensionings() {
            return mDimensionings;
        }

        public void setDimensionings(List<Dimensioning> value) {
            mDimensionings = value;
        }

        @XmlElementWrapper(name = "Floors")
        @XmlElement(name = "Floor")
        public List<Floor> getFloors() {
            return mFloors;
        }

        public void setFloors(List<Floor> value) {
            mFloors = value;
        }

        @XmlElementWrapper(name = "Walls")
        @XmlElement(name = "Wall")
        public List<Wall> getWalls() {
            return mWalls;
        }

        public void setWalls(List<Wall> value) {
            mWalls = value;
        }

        @XmlElementWrapper(name = "Ceilings")
        @XmlElement(name = "Ceiling")
        public List<Ceiling> getCeilings() {
            return mCeilings;
        }

        public void setCeilings(List<Ceiling> value) {
            mCeilings = value;
        }

        @XmlElementWrapper(name = "Coverings")
        @XmlElement(name = "Covering")
        public List<Covering> getCoverings() {
            return mCoverings;
        }

        @XmlElementWrapper(name = "SupportObjects")
        @XmlElement(name = "SupportObject")
        public List<SupportObject> getSupportObjects() {
            return mSupportObjects;
        }

        public void setCoverings(List<Covering> value) {
            mCoverings = value;
        }

        @XmlElementWrapper(name = "GuideLines")
        @XmlElement(name = "GuideLine")
        public List<GuideLine> getGuideLines() {
            return mGuideLines;
        }

        public void setGuideLines(List<GuideLine> value) {
            mGuideLines = value;
        }

        @XmlElementWrapper(name = "Groups")
        @XmlElement(name = "Group")
        public List<ObjectsGroup> getGroups() {
            return mGroups;
        }

        public void setGroups(List<ObjectsGroup> value) {
            mGroups = value;
        }
    }

    @Override
    public Plan unmarshal(PlanProxy v) throws Exception {
        return v.toPlan();
    }

    @Override
    public PlanProxy marshal(Plan v) throws Exception {
        return PlanProxy.from(v);
    }
}
