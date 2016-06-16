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

package org.openlmis.core.view.viewmodel;


import org.apache.commons.lang3.StringUtils;
import org.openlmis.core.exceptions.MovementReasonNotFoundException;
import org.openlmis.core.manager.MovementReasonManager;
import org.openlmis.core.model.StockMovementItem;
import org.openlmis.core.utils.DateUtil;

import java.util.HashMap;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class StockMovementViewModel {

    MovementReasonManager.MovementReason reason;

    String movementDate;
    String stockExistence;
    String documentNo;
    String signature;
    String requested;
    boolean isDraft = true;

    private HashMap<StockMovementItem.MovementType, String> typeQuantityMap = new HashMap<>();

    public StockMovementViewModel(StockMovementItem item) {
        movementDate = DateUtil.formatDate(item.getMovementDate());
        documentNo = item.getDocumentNumber();
        stockExistence = String.valueOf(item.getStockOnHand());
        signature = item.getSignature();
        if (null == item.getRequested()) {
            requested = "";
        } else {
            requested = String.valueOf(item.getRequested());
        }
        isDraft = false;

        try {
            reason = MovementReasonManager.getInstance().queryByCode(item.getReason());
        } catch (MovementReasonNotFoundException e) {
            throw new RuntimeException("MovementReason Cannot be find " + e.getMessage());
        }

        typeQuantityMap.put(item.getMovementType(), String.valueOf(item.getMovementQuantity()));
    }

    public String getReceived() {
        return typeQuantityMap.get(StockMovementItem.MovementType.RECEIVE);
    }

    public void setReceived(String received) {
        typeQuantityMap.put(StockMovementItem.MovementType.RECEIVE, received);
    }

    public String getIssued() {
        return typeQuantityMap.get(StockMovementItem.MovementType.ISSUE);
    }

    public void setIssued(String issued) {
        typeQuantityMap.put(StockMovementItem.MovementType.ISSUE, issued);
    }

    public String getNegativeAdjustment() {
        return typeQuantityMap.get(StockMovementItem.MovementType.NEGATIVE_ADJUST);
    }

    public void setNegativeAdjustment(String negativeAdjustment) {
        typeQuantityMap.put(StockMovementItem.MovementType.NEGATIVE_ADJUST, negativeAdjustment);
    }

    public String getPositiveAdjustment() {
        return typeQuantityMap.get(StockMovementItem.MovementType.POSITIVE_ADJUST);
    }

    public void setPositiveAdjustment(String positiveAdjustment) {
        typeQuantityMap.put(StockMovementItem.MovementType.POSITIVE_ADJUST, positiveAdjustment);
    }

    public StockMovementItem convertViewToModel() {
        StockMovementItem stockMovementItem = new StockMovementItem();
        stockMovementItem.setStockOnHand(Long.parseLong(getStockExistence()));

        stockMovementItem.setReason(reason.getCode());
        stockMovementItem.setDocumentNumber(getDocumentNo());
        stockMovementItem.setMovementType(reason.getMovementType());

        Long movementQuantity = Long.parseLong(typeQuantityMap.get(reason.getMovementType()));
        stockMovementItem.setMovementQuantity(movementQuantity);

        stockMovementItem.setRequested((null == requested || requested.isEmpty()) ? null : Long.valueOf(requested));

        stockMovementItem.setSignature(signature);

        stockMovementItem.setMovementDate(DateUtil.parseString(getMovementDate(), DateUtil.DEFAULT_DATE_FORMAT));

        return stockMovementItem;
    }

    public boolean validateEmpty() {
        return reason != null && StringUtils.isNoneEmpty(movementDate) && !allQuantitiesEmpty();
    }

    public boolean validateInputValid() {
        return (isAnyQuantitiesNumeric() && Long.parseLong(stockExistence) >= 0);
    }

    public boolean validateReceived() {
        if (typeQuantityMap.get(StockMovementItem.MovementType.RECEIVE).length() > 0) {
            return Long.parseLong(typeQuantityMap.get(StockMovementItem.MovementType.RECEIVE)) > 0;
        }
        return true;
    }

    public boolean isIssuedReason() {
        return getReason() != null && getReason().isIssueAdjustment();
    }

    private boolean allQuantitiesEmpty() {
        return StringUtils.isEmpty(getReceived())
                && StringUtils.isEmpty(getIssued())
                && StringUtils.isEmpty(getPositiveAdjustment())
                && StringUtils.isEmpty(getNegativeAdjustment());
    }

    private boolean isAnyQuantitiesNumeric() {
        return StringUtils.isNumeric(getReceived())
                || StringUtils.isNumeric(getNegativeAdjustment())
                || StringUtils.isNumeric(getPositiveAdjustment())
                || StringUtils.isNumeric(getIssued());
    }
}
