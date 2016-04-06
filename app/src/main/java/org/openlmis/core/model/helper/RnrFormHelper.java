package org.openlmis.core.model.helper;

import org.openlmis.core.exceptions.LMISException;
import org.openlmis.core.model.BaseInfoItem;
import org.openlmis.core.model.RegimenItem;
import org.openlmis.core.model.RnRForm;
import org.openlmis.core.model.RnrFormItem;
import org.openlmis.core.model.StockMovementItem;

import java.sql.SQLException;
import java.util.List;

public class RnrFormHelper {

    public void initRnrFormItemWithoutMovement(RnrFormItem rnrFormItem, long lastRnrInventory) throws LMISException {
        rnrFormItem.setReceived(0);
        rnrFormItem.setIssued(0);
        rnrFormItem.setAdjustment(0);
        rnrFormItem.setCalculatedOrderQuantity(0L);
        rnrFormItem.setInitialAmount(lastRnrInventory);
        rnrFormItem.setInventory(lastRnrInventory);
    }

    public void updateWrapperList(RnRForm form) throws SQLException {
        for (RnrFormItem item : form.getRnrFormItemListWrapper()) {
            form.getRnrFormItemList().update(item);
        }
        for (RegimenItem item : form.getRegimenItemListWrapper()) {
            form.getRegimenItemList().update(item);
        }
        for (BaseInfoItem item : form.getBaseInfoItemListWrapper()) {
            form.getBaseInfoItemList().update(item);
        }
    }

    public void assignTotalValues(RnrFormItem rnrFormItem, List<StockMovementItem> stockMovementItems) {
        long totalReceived = 0;
        long totalIssued = 0;
        long totalAdjustment = 0;

        for (StockMovementItem item : stockMovementItems) {
            if (StockMovementItem.MovementType.RECEIVE == item.getMovementType()) {
                totalReceived += item.getMovementQuantity();
            } else if (StockMovementItem.MovementType.ISSUE == item.getMovementType()) {
                totalIssued += item.getMovementQuantity();
            } else if (StockMovementItem.MovementType.NEGATIVE_ADJUST == item.getMovementType()) {
                totalAdjustment -= item.getMovementQuantity();
            } else if (StockMovementItem.MovementType.POSITIVE_ADJUST == item.getMovementType()) {
                totalAdjustment += item.getMovementQuantity();
            }
        }
        rnrFormItem.setReceived(totalReceived);
        rnrFormItem.setIssued(totalIssued);
        rnrFormItem.setAdjustment(totalAdjustment);

        Long inventory = stockMovementItems.get(stockMovementItems.size() - 1).getStockOnHand();
        rnrFormItem.setInventory(inventory);

        rnrFormItem.setCalculatedOrderQuantity(calculatedOrderQuantity(totalIssued, inventory));
    }

    private long calculatedOrderQuantity(long totalIssued, Long inventory) {
        Long totalRequest = totalIssued * 2 - inventory;
        return totalRequest > 0 ? totalRequest : 0;
    }

}
