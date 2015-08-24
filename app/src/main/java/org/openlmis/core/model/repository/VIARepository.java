/*
 * This program is part of the OpenLMIS logistics management information
 * system platform software.
 *
 * Copyright © 2015 ThoughtWorks, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version. This program is distributed in the
 * hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details. You should
 * have received a copy of the GNU Affero General Public License along with
 * this program. If not, see http://www.gnu.org/licenses. For additional
 * information contact info@OpenLMIS.org
 */

package org.openlmis.core.model.repository;

import android.content.Context;

import com.google.inject.Inject;

import org.openlmis.core.exceptions.LMISException;
import org.openlmis.core.model.BaseInfoItem;
import org.openlmis.core.model.RnRForm;

import java.util.ArrayList;
import java.util.List;

public class VIARepository extends RnrFormRepository{

    public static final String VIA_PROGRAM_CODE = "ESS_MEDS";

    public static final String ATTR_CONSULTATION = "consultation";

    @Inject
    ProgramRepository programRepository;

    @Inject
    public VIARepository(Context context) {
        super(context);
    }

    public RnRForm initVIA() throws LMISException{
        return initRnrForm(programRepository.queryByCode(VIA_PROGRAM_CODE));
    }

    @Override
    protected List<BaseInfoItem> generateBaseInfoItems(RnRForm form) {
        BaseInfoItem newPatients = new BaseInfoItem(ATTR_CONSULTATION, BaseInfoItem.TYPE.INT, form);
        List<BaseInfoItem> baseInfoItemList = new ArrayList<>();
        baseInfoItemList.add(newPatients);
        return baseInfoItemList;
    }
}
