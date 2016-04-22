package org.openlmis.core.view.holder;

import android.app.DialogFragment;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewStub;
import android.widget.TextView;

import org.joda.time.DateTime;
import org.openlmis.core.LMISApp;
import org.openlmis.core.R;
import org.openlmis.core.googleAnalytics.TrackerActions;
import org.openlmis.core.googleAnalytics.TrackerCategories;
import org.openlmis.core.model.service.StockService;
import org.openlmis.core.utils.DateUtil;
import org.openlmis.core.utils.TextStyleUtil;
import org.openlmis.core.utils.ToastUtil;
import org.openlmis.core.view.activity.BaseActivity;
import org.openlmis.core.view.fragment.SimpleDialogFragment;
import org.openlmis.core.view.viewmodel.InventoryViewModel;

import roboguice.RoboGuice;
import roboguice.inject.InjectView;

public class StockCardViewHolder extends BaseViewHolder {

    @InjectView(R.id.product_name)
    TextView tvProductName;
    @InjectView(R.id.product_unit)
    TextView tvProductUnit;
    @InjectView(R.id.tv_stock_on_hand)
    TextView tvStockOnHand;
    @InjectView(R.id.vg_stock_on_hand_bg)
    View stockOnHandBg;

    @InjectView(R.id.vs_warning)
    ViewStub vsWarning;
    View ivWarning;

    @InjectView(R.id.vs_expiry_date_warning)
    ViewStub vsExpiryDateWarning;
    View ivExpiryDateWarning;

    protected StockService stockService;
    private OnItemViewClickListener listener;

    protected static final int STOCK_ON_HAND_NORMAL = 1;
    protected static final int STOCK_ON_HAND_LOW_STOCK = 2;
    protected static final int STOCK_ON_HAND_STOCK_OUT = 3;

    public StockCardViewHolder(View itemView, OnItemViewClickListener listener) {
        super(itemView);
        this.listener = listener;
        this.stockService = RoboGuice.getInjector(context).getInstance(StockService.class);
    }

    public void populate(final InventoryViewModel inventoryViewModel, String queryKeyWord) {
        setListener(inventoryViewModel);
        inflateDate(inventoryViewModel, queryKeyWord);
    }

    protected void inflateDate(InventoryViewModel inventoryViewModel, String queryKeyWord) {
        tvStockOnHand.setText(inventoryViewModel.getStockOnHand() + "");
        tvProductName.setText(TextStyleUtil.getHighlightQueryKeyWord(queryKeyWord, inventoryViewModel.getStyledName()));
        tvProductUnit.setText(TextStyleUtil.getHighlightQueryKeyWord(queryKeyWord, inventoryViewModel.getStyledUnit()));

        initExpiryDateWarning(inventoryViewModel);

        initStockOnHandWarning(inventoryViewModel);
    }

    private void initExpiryDateWarning(InventoryViewModel inventoryViewModel) {
        String earliestExpiryDateString = inventoryViewModel.getStockCard().getEarliestExpireDate();
        if (TextUtils.isEmpty(earliestExpiryDateString)) {
            hideExpiryDate();
            return;
        }

        DateTime earliestExpiryDate = new DateTime(DateUtil.parseString(earliestExpiryDateString, DateUtil.SIMPLE_DATE_FORMAT));
        DateTime currentTime = new DateTime(LMISApp.getInstance().getCurrentTimeMillis());

        if (inventoryViewModel.getStockOnHand() > 0 && (earliestExpiryDate.isBefore(currentTime) || isExpiryDateInCurrentMonth(earliestExpiryDate, currentTime))) {
            showExpiryDate();
        } else {
            hideExpiryDate();
        }
    }

    private void showExpiryDate() {
        if (ivExpiryDateWarning != null) {
            ivExpiryDateWarning.setVisibility(View.VISIBLE);
        } else {
            ivExpiryDateWarning = vsExpiryDateWarning.inflate();
        }
        initWarningLister();
    }

    private void hideExpiryDate() {
        if (ivExpiryDateWarning != null) {
            ivExpiryDateWarning.setVisibility(View.GONE);
        }
    }

    protected void initWarningLister() {
        ivExpiryDateWarning.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment dialogFragment = SimpleDialogFragment.newInstance(null,
                        context.getString(R.string.msg_expiry_warning),
                        context.getString(R.string.btn_ok));
                dialogFragment.show(((BaseActivity) context).getFragmentManager(), "expiryDateWarningDialog");
            }
        });
    }

    private boolean isExpiryDateInCurrentMonth(DateTime earliestExpiryDate, DateTime currentTime) {
        return earliestExpiryDate.getYear() == currentTime.getYear() && earliestExpiryDate.getMonthOfYear() == currentTime.getMonthOfYear();
    }

    private void setListener(final InventoryViewModel inventoryViewModel) {
        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    LMISApp.getInstance().trackEvent(TrackerCategories.StockMovement, TrackerActions.SelectStockCard);
                    listener.onItemViewClick(inventoryViewModel);
                }
            }
        });
    }

    private void initStockOnHandWarning(final InventoryViewModel stockCard) {

        int stockOnHandLevel = getStockOnHandLevel(stockCard);

        switch (stockOnHandLevel) {
            case STOCK_ON_HAND_LOW_STOCK:
                stockOnHandBg.setBackgroundResource(R.color.color_warning);
                showWarning(context.getString(R.string.msg_low_stock_warning));
                break;
            case STOCK_ON_HAND_STOCK_OUT:
                stockOnHandBg.setBackgroundResource(R.color.color_stock_out);
                showWarning(context.getString(R.string.msg_stock_out_warning));
                break;
            default:
                stockOnHandBg.setBackgroundResource(R.color.color_primary_50);
                if (ivWarning != null) {
                    ivWarning.setVisibility(View.GONE);
                }
                break;
        }
    }

    private void showWarning(final String warningMsg) {
        if (ivWarning != null) {
            ivWarning.setVisibility(View.VISIBLE);
        } else {
            ivWarning = vsWarning.inflate();
        }
        ivWarning.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ToastUtil.showCustomToast(warningMsg);
            }
        });
    }

    protected int getStockOnHandLevel(InventoryViewModel inventoryViewModel) {

        long stockOnHand = inventoryViewModel.getStockOnHand();

        if (stockOnHand > inventoryViewModel.getLowStockAvg()) {
            return STOCK_ON_HAND_NORMAL;
        } else if (stockOnHand > 0) {
            return STOCK_ON_HAND_LOW_STOCK;
        } else {
            return STOCK_ON_HAND_STOCK_OUT;
        }
    }

    public interface OnItemViewClickListener {
        void onItemViewClick(InventoryViewModel inventoryViewModel);
    }
}
