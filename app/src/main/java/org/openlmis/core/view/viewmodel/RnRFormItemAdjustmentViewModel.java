package org.openlmis.core.view.viewmodel;

import org.openlmis.core.LMISApp;
import org.openlmis.core.R;

import lombok.Data;

@Data
public class RnRFormItemAdjustmentViewModel {
    private long kitStockOnHand;
    private String kitName;
    private int quantity;

    public String formatAdjustmentContentForProduct(String productName) {
        return LMISApp.getContext().getResources().getString(R.string.label_adjustment_dialog_adjust_content,
                quantity,
                kitName,
                kitStockOnHand,
                productName
        );
    }

}
