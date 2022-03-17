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
package de.dh.cad.architect.ui.persistence;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import de.dh.cad.architect.model.Plan;
import de.dh.cad.architect.model.jaxb.PlanJavaTypeAdapter;

@XmlRootElement(name = "Planfile")
public class PlanFile {
    protected Plan mPlan;
    protected UiState mUiState;

    public PlanFile() {
        // For JAXB
    }

    public PlanFile(Plan plan, UiState uiState) {
        mPlan = plan;
        mUiState = uiState;
    }

    @XmlElement(name = "Plan")
    @XmlJavaTypeAdapter(PlanJavaTypeAdapter.class)
    public Plan getPlan() {
        return mPlan;
    }

    public void setPlan(Plan value) {
        mPlan = value;
    }

    @XmlElement(name = "UiState")
    public UiState getUiState() {
        return mUiState;
    }

    public void setUiState(UiState value) {
        mUiState = value;
    }
}
