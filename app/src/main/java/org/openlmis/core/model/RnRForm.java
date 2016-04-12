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

package org.openlmis.core.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

import org.joda.time.DateTime;
import org.openlmis.core.LMISApp;
import org.openlmis.core.R;
import org.openlmis.core.utils.DateUtil;
import org.openlmis.core.utils.ListUtil;
import org.roboguice.shaded.goole.common.base.Predicate;
import org.roboguice.shaded.goole.common.collect.FluentIterable;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

import static org.openlmis.core.model.Product.IsKit;

@Getter
@Setter
@DatabaseTable(tableName = "rnr_forms")
public class RnRForm extends BaseModel {
    public enum STATUS {
        DRAFT,
        SUBMITTED,
        AUTHORIZED,
        DRAFT_MISSED,
        SUBMITTED_MISSED,
    }

    @ForeignCollectionField()
    private ForeignCollection<RnrFormItem> rnrFormItemList;

    @Expose
    @SerializedName("products")
    private List<RnrFormItem> rnrFormItemListWrapper;

    @ForeignCollectionField()
    private ForeignCollection<RegimenItem> regimenItemList;

    @Expose
    @SerializedName("regimens")
    private List<RegimenItem> regimenItemListWrapper;

    @ForeignCollectionField()
    private ForeignCollection<BaseInfoItem> baseInfoItemList;

    @Expose
    @SerializedName("patientQuantifications")
    private List<BaseInfoItem> baseInfoItemListWrapper;

    @Expose
    @SerializedName("clientSubmittedNotes")
    @DatabaseField
    private String comments;

    @DatabaseField(defaultValue = "DRAFT")
    private STATUS status;

    @DatabaseField(foreign = true, foreignAutoRefresh = true)
    private Program program;

    @DatabaseField
    private boolean synced = false;

    @Expose
    @SerializedName("actualPeriodStartDate")
    @DatabaseField
    private Date periodBegin;

    @Expose
    @SerializedName("actualPeriodEndDate")
    @DatabaseField
    private Date periodEnd;

    @Expose
    @SerializedName("clientSubmittedTime")
    @DatabaseField
    private Date submittedTime;

    @Expose
    @DatabaseField
    private boolean emergency;

    public boolean isDraft() {
        return getStatus() == STATUS.DRAFT || getStatus() == STATUS.DRAFT_MISSED;
    }

    public boolean isMissed() {
        return getStatus() == STATUS.DRAFT_MISSED || getStatus() == STATUS.SUBMITTED_MISSED;
    }

    public boolean isSubmitted() {
        return getStatus() == STATUS.SUBMITTED || getStatus() == STATUS.SUBMITTED_MISSED;
    }

    public boolean isAuthorized() {
        return getStatus() == STATUS.AUTHORIZED;
    }

    public static RnRForm init(Program program, Date generateDate) {
        RnRForm rnrForm = new RnRForm();
        rnrForm.program = program;
        Period period = DateUtil.generateRnRFormPeriodBy(generateDate);
        rnrForm.periodBegin = period.getBegin().toDate();
        rnrForm.periodEnd = period.getEnd().toDate();
        return rnrForm;
    }

    public static RnRForm init(Program program, Period period, boolean isEmergency) {
        RnRForm rnRForm = new RnRForm();
        rnRForm.program = program;
        rnRForm.periodBegin = period.getBegin().toDate();
        rnRForm.periodEnd = period.getEnd().toDate();
        rnRForm.setEmergency(isEmergency);

        if (isMissed(period) && !isEmergency) {
            rnRForm.status = RnRForm.STATUS.DRAFT_MISSED;
        } else {
            rnRForm.status = RnRForm.STATUS.DRAFT;
        }

        return rnRForm;
    }

    private static boolean isMissed(Period period) {
        DateTime today = new DateTime(LMISApp.getInstance().getCurrentTimeMillis());
        DateTime periodEnd = period.getEnd();
        int monthOffset = DateUtil.calculateMonthOffset(today, periodEnd);
        if (monthOffset > 0 || (monthOffset == 0 && today.getDayOfMonth() >= Period.INVENTORY_END_DAY_NEXT)) {
            return true;
        }
        return false;
    }

    public static long calculateTotalRegimenAmount(Collection<RegimenItem> list) {
        long totalRegimenNumber = 0;
        for (RegimenItem item : list) {
            if (item.getAmount() != null) {
                totalRegimenNumber += item.getAmount();
            }
        }

        return totalRegimenNumber;
    }

    public List<RnrFormItem> getRnrFormItemListWrapper() {
        rnrFormItemListWrapper = ListUtil.wrapOrEmpty(rnrFormItemList, rnrFormItemListWrapper);
        return rnrFormItemListWrapper;
    }

    public List<BaseInfoItem> getBaseInfoItemListWrapper() {
        baseInfoItemListWrapper = ListUtil.wrapOrEmpty(baseInfoItemList, baseInfoItemListWrapper);
        return baseInfoItemListWrapper;
    }

    public List<RegimenItem> getRegimenItemListWrapper() {
        regimenItemListWrapper = ListUtil.wrapOrEmpty(regimenItemList, regimenItemListWrapper);
        return regimenItemListWrapper;
    }

    public static void fillFormId(RnRForm rnRForm) {
        for (RnrFormItem item : rnRForm.getRnrFormItemListWrapper()) {
            item.setForm(rnRForm);
        }
        for (RegimenItem regimenItem : rnRForm.getRegimenItemListWrapper()) {
            regimenItem.setForm(rnRForm);
        }
        for (BaseInfoItem item : rnRForm.getBaseInfoItemListWrapper()) {
            item.setRnRForm(rnRForm);
        }
    }

    public List<RnrFormItem> getDeactivatedAndUnsupportedProductItems(final List<String> supportedProductCodes) {

        return FluentIterable.from(getRnrFormItemListWrapper()).filter(new Predicate<RnrFormItem>() {
            @Override
            public boolean apply(RnrFormItem rnrFormItem) {
                if (LMISApp.getInstance().getFeatureToggleFor(R.bool.feature_deactivate_program_product)) {
                    return !(rnrFormItem.getProduct().isActive()
                            && supportedProductCodes.contains(rnrFormItem.getProduct().getCode()));
                } else {
                    return !rnrFormItem.getProduct().isActive();
                }
            }
        }).toList();
    }

    public List<RnrFormItem> getRnrItems(final IsKit isKit) {
        return FluentIterable.from(getRnrFormItemListWrapper()).filter(new Predicate<RnrFormItem>() {
            @Override
            public boolean apply(RnrFormItem rnrFormItem) {
                return isKit.isKit() == rnrFormItem.getProduct().isKit();
            }
        }).toList();
    }
}
