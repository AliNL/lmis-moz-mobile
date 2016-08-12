package org.openlmis.core.view.holder;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputFilter;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;
import org.openlmis.core.LMISApp;
import org.openlmis.core.R;
import org.openlmis.core.exceptions.LMISException;
import org.openlmis.core.manager.NestedRecyclerViewLinearLayoutManager;
import org.openlmis.core.model.StockCard;
import org.openlmis.core.utils.DateUtil;
import org.openlmis.core.utils.SingleTextWatcher;
import org.openlmis.core.utils.TextStyleUtil;
import org.openlmis.core.utils.ToastUtil;
import org.openlmis.core.view.adapter.LotMovementAdapter;
import org.openlmis.core.view.viewmodel.InventoryViewModel;
import org.openlmis.core.view.viewmodel.LotMovementViewModel;
import org.openlmis.core.view.widget.AddLotDialogFragment;
import org.openlmis.core.view.widget.DatePickerDialogWithoutDay;
import org.openlmis.core.view.widget.InputFilterMinMax;

import java.text.ParseException;
import java.util.Calendar;
import java.util.GregorianCalendar;

import roboguice.inject.InjectView;


public class InitialInventoryViewHolder extends BaseViewHolder {

    @InjectView(R.id.product_name)
    TextView productName;

    @InjectView(R.id.product_unit)
    TextView productUnit;

    @InjectView(R.id.ly_quantity)
    TextInputLayout lyQuantity;

    @InjectView(R.id.tx_quantity)
    EditText txQuantity;

    @InjectView(R.id.tx_expire_date)
    TextView txExpireDate;

    @InjectView(R.id.action_divider)
    View actionDivider;

    @InjectView(R.id.checkbox)
    CheckBox checkBox;

    @InjectView(R.id.action_panel)
    View actionPanel;

    @InjectView(R.id.action_view_history)
    TextView tvHistoryAction;

    @InjectView(R.id.touchArea_checkbox)
    LinearLayout taCheckbox;

    @InjectView(R.id.add_new_lot_panel)
    private View actionPanelForAddLot;

    @InjectView(R.id.tx_add_new_lot)
    private TextView txAddNewLot;

    @InjectView(R.id.rv_added_lot)
    private RecyclerView lotListRecyclerView;

    private LotMovementAdapter lotMovementAdapter;

    private AddLotDialogFragment addLotDialogFragment;

    public InitialInventoryViewHolder(View itemView) {
        super(itemView);
        initView();
    }

    private void initView() {
        txQuantity.setFilters(new InputFilter[]{new InputFilterMinMax(Integer.MAX_VALUE)});
        txQuantity.setHint(R.string.hint_quantity_in_stock);
        taCheckbox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerCheckbox();
            }
        });
    }

    private void triggerCheckbox() {
        if (checkBox.isChecked()) {
            checkBox.setChecked(false);
        } else {
            checkBox.setChecked(true);
            txQuantity.requestFocus();
        }
    }

    public void populate(final InventoryViewModel viewModel, String queryKeyWord, ViewHistoryListener listener) {
        setItemViewListener(viewModel);

        checkBox.setChecked(viewModel.isChecked());

        productName.setText(TextStyleUtil.getHighlightQueryKeyWord(queryKeyWord, viewModel.getStyledName()));
        productUnit.setText(TextStyleUtil.getHighlightQueryKeyWord(queryKeyWord, viewModel.getStyleType()));

        populateEditPanel(viewModel.getQuantity(), viewModel.optFirstExpiryDate());

        if (viewModel.isValid()) {
            lyQuantity.setErrorEnabled(false);
        } else {
            lyQuantity.setError(context.getResources().getString(R.string.msg_inventory_check_failed));
        }
        initLotListRecyclerView(viewModel);
        initHistoryView(viewModel, listener);
    }

    protected void setItemViewListener(final InventoryViewModel viewModel) {
        txExpireDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker(viewModel);
            }
        });

        txAddNewLot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddNewLotDialog(viewModel);
            }
        });

        final EditTextWatcher textWatcher = new EditTextWatcher(viewModel);
        txQuantity.removeTextChangedListener(textWatcher);

        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                checkedChangeAction(isChecked, viewModel);
            }
        });

        txQuantity.addTextChangedListener(textWatcher);
    }

    private void checkedChangeAction(boolean isChecked, InventoryViewModel viewModel) {
        if (isChecked && !viewModel.getProduct().isArchived()) {
            if (LMISApp.getInstance().getFeatureToggleFor(R.bool.feature_lot_management)) {
                if (viewModel.getLotMovementViewModelList().isEmpty()) {
                    showAddNewLotDialog(viewModel);
                }
                showAddNewLotPanel(View.VISIBLE);
            } else {
                showEditPanel(View.VISIBLE);
            }
        } else {
            if (LMISApp.getInstance().getFeatureToggleFor(R.bool.feature_lot_management)) {
                showAddNewLotPanel(View.GONE);
                viewModel.clearLotMovementViewModelList();
                lotMovementAdapter.notifyDataSetChanged();
            } else {
                showEditPanel(View.GONE);
            }
            populateEditPanel(StringUtils.EMPTY, StringUtils.EMPTY);

            viewModel.setQuantity(StringUtils.EMPTY);
            viewModel.clearExpiryDates();
        }
        viewModel.setChecked(isChecked);
    }

    private void initLotListRecyclerView(InventoryViewModel viewModel) {
        lotMovementAdapter = new LotMovementAdapter(viewModel.getLotMovementViewModelList());
        lotListRecyclerView.setLayoutManager(new NestedRecyclerViewLinearLayoutManager(context));
        lotListRecyclerView.setAdapter(lotMovementAdapter);
    }

    private void showAddNewLotDialog(final InventoryViewModel viewModel) {
        addLotDialogFragment = new AddLotDialogFragment();
        addLotDialogFragment.setListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.btn_complete:
                        if (addLotDialogFragment.validate()) {
                            addLotView(new LotMovementViewModel(addLotDialogFragment.getLotNumber(), addLotDialogFragment.getExpiryDate()), viewModel);
                            addLotDialogFragment.dismiss();
                        }
                        break;
                    case R.id.btn_cancel:
                        addLotDialogFragment.dismiss();
                        if (viewModel.getLotMovementViewModelList().isEmpty()) {
                            checkBox.setChecked(false);
                        }
                        break;
                }
            }
        });
        addLotDialogFragment.show(((Activity) context).getFragmentManager(), "add_new_lot");
    }

    private void addLotView(LotMovementViewModel lotMovementViewModel, InventoryViewModel viewModel) {
        viewModel.addLotMovementViewModel(lotMovementViewModel);
        lotListRecyclerView.setLayoutManager(new NestedRecyclerViewLinearLayoutManager(context));
        lotMovementAdapter.notifyDataSetChanged();
    }

    private void initHistoryView(final InventoryViewModel viewModel, final ViewHistoryListener listener) {
        tvHistoryAction.setVisibility(viewModel.getProduct().isArchived() ? View.VISIBLE : View.GONE);
        tvHistoryAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.viewHistory(viewModel.getStockCard());
                }
            }
        });
    }

    protected void populateEditPanel(String quantity, String expireDate) {
        txQuantity.setText(quantity);
        txExpireDate.setText(expireDate);
    }

    protected void showEditPanel(int visible) {
        actionDivider.setVisibility(visible);
        actionPanel.setVisibility(visible);
    }

    public void showAddNewLotPanel(int visible) {
        actionDivider.setVisibility(visible);
        actionPanelForAddLot.setVisibility(visible);
    }

    class EditTextWatcher extends SingleTextWatcher {

        private final InventoryViewModel viewModel;

        public EditTextWatcher(InventoryViewModel viewModel) {
            this.viewModel = viewModel;
        }

        @Override
        public void afterTextChanged(Editable editable) {
            viewModel.setQuantity(editable.toString());
        }
    }

    public void showDatePicker(final InventoryViewModel viewModel) {
        final Calendar today = GregorianCalendar.getInstance();

        DatePickerDialog dialog = new DatePickerDialogWithoutDay(context, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                GregorianCalendar date = new GregorianCalendar(year, monthOfYear, dayOfMonth);
                if (today.before(date)) {
                    String dateString = new StringBuilder().append(dayOfMonth).append("/").append(monthOfYear + 1).append("/").append(year).toString();
                    try {
                        txExpireDate.setText(DateUtil.convertDate(dateString, "dd/MM/yyyy", "MMM yyyy"));
                    } catch (ParseException e) {
                        new LMISException(e).reportToFabric();
                    }
                    viewModel.addExpiryDate(dateString, false);
                } else {
                    ToastUtil.show(R.string.msg_invalid_date);
                }
            }
        }, today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH));

        dialog.show();
    }

    public interface ViewHistoryListener {
        void viewHistory(StockCard stockCard);
    }
}
