package org.openlmis.core.view.activity;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;

import org.openlmis.core.R;
import org.openlmis.core.exceptions.LMISException;
import org.openlmis.core.googleAnalytics.ScreenName;
import org.openlmis.core.manager.MovementReasonManager;
import org.openlmis.core.model.StockMovementItem;
import org.openlmis.core.presenter.NewStockMovementPresenter;
import org.openlmis.core.utils.Constants;
import org.openlmis.core.utils.DateUtil;
import org.openlmis.core.utils.InjectPresenter;
import org.openlmis.core.utils.ToastUtil;
import org.openlmis.core.view.fragment.SimpleSelectDialogFragment;
import org.openlmis.core.view.viewmodel.StockMovementViewModel;
import org.roboguice.shaded.goole.common.base.Function;
import org.roboguice.shaded.goole.common.collect.FluentIterable;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;

import roboguice.inject.ContentView;
import roboguice.inject.InjectView;

@ContentView(R.layout.activity_stock_card_new_movement)
public class StockCardNewMovementActivity extends BaseActivity implements NewStockMovementPresenter.NewStockMovementView, View.OnClickListener{

    @InjectView(R.id.ly_requested_quantity)
    View lyRequestedQuantity;

    @InjectView(R.id.et_movement_date)
    EditText etMovementDate;

    @InjectView(R.id.ly_movement_date)
    TextInputLayout lyMovementDate;

    @InjectView(R.id.et_document_number)
    EditText etDocumentNumber;

    @InjectView(R.id.et_movement_reason)
    EditText etMovementReason;

    @InjectView(R.id.ly_movement_reason)
    TextInputLayout lyMovementReason;

    @InjectView(R.id.et_requested_quantity)
    EditText etRequestedQuantity;

    @InjectView(R.id.et_movement_quantity)
    EditText etMovementQuantity;

    @InjectView(R.id.ly_movement_quantity)
    TextInputLayout lyMovementQuantity;

    @InjectView(R.id.et_movement_signature)
    EditText etMovementSignature;

    @InjectView(R.id.ly_movement_signature)
    TextInputLayout lyMovementSignature;

    @InjectView(R.id.btn_complete)
    View btnComplete;

    @InjectView(R.id.btn_cancel)
    TextView tvCancel;

    @InjectPresenter(NewStockMovementPresenter.class)
    NewStockMovementPresenter presenter;

    private String stockName;
    private StockMovementItem.MovementType movementType;
    private Long stockCardId;

    private StockMovementItem previousMovement;

    private List<MovementReasonManager.MovementReason> movementReasons;

    private MovementReasonManager movementReasonManager;

    SimpleSelectDialogFragment reasonsDialog;

    private StockMovementViewModel viewModel;
    private String[] reasonListStr;

    @Override
    protected ScreenName getScreenName() {
        return ScreenName.StockCardNewMovementScreen;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        movementReasonManager = MovementReasonManager.getInstance();

        stockName = getIntent().getStringExtra(Constants.PARAM_STOCK_NAME);
        String movementTypeStr = getIntent().getStringExtra(Constants.PARAM_MOVEMENT_TYPE);
        stockCardId =  getIntent().getLongExtra(Constants.PARAM_STOCK_CARD_ID, 0L);
        movementType = movementReasonManager.getMovementTypeByDescription(movementTypeStr);
        movementReasons = movementReasonManager.buildReasonListForMovementType(movementType);

        try {
            previousMovement = presenter.loadPreviousMovement(stockCardId);
        } catch (LMISException e) {
            e.printStackTrace();
        }

        viewModel = new StockMovementViewModel();
        initUI();
    }

    private void initUI() {
        setTitle(movementType + " " + stockName);

        if (!movementType.equals("Issues")) {
            lyRequestedQuantity.setVisibility(View.GONE);
        }

        btnComplete.setOnClickListener(this);
        tvCancel.setOnClickListener(this);

        etMovementDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDatePickerDialog(presenter.getStockMovementModel(), previousMovement.getMovementDate());
            }
        });

        etMovementReason.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                reasonListStr = FluentIterable.from(movementReasons).transform(new Function<MovementReasonManager.MovementReason, String>() {
                    @Override
                    public String apply(MovementReasonManager.MovementReason movementReason) {
                        return movementReason.getDescription();
                    }
                }).toArray(String.class);
                reasonsDialog = new SimpleSelectDialogFragment(new MovementTypeOnClickListener(viewModel), reasonListStr);
                reasonsDialog.show(getFragmentManager(), "");
            }
        });
    }

    public static Intent getIntentToMe(StockMovementsActivityNew context, String stockName, String movementType, Long stockCardId) {
        Intent intent = new Intent(context, StockCardNewMovementActivity.class);
        intent.putExtra(Constants.PARAM_STOCK_NAME, stockName);
        intent.putExtra(Constants.PARAM_MOVEMENT_TYPE, movementType);
        intent.putExtra(Constants.PARAM_STOCK_CARD_ID, stockCardId);
        return intent;
    }

    private void showDatePickerDialog(StockMovementViewModel model, Date previousMovementDate) {
        final Calendar today = GregorianCalendar.getInstance();

        DatePickerDialog dialog = new DatePickerDialog(this, DatePickerDialog.BUTTON_NEUTRAL,
                new MovementDateListener(model, previousMovementDate),
                today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_complete:
                viewModel.setMovementDate(etMovementDate.getText().toString());
                viewModel.setDocumentNo(etDocumentNumber.getText().toString());
                HashMap<StockMovementItem.MovementType, String> quantityMap = new HashMap<>();
                quantityMap.put(movementType, etMovementQuantity.getText().toString());
                viewModel.setTypeQuantityMap(quantityMap);
                viewModel.setSignature(etMovementSignature.getText().toString());
                presenter.saveStockMovement(viewModel);
                break;
            case R.id.btn_cancel:
                break;
        }
    }

    public void clearErrorAlerts() {
        lyMovementDate.setErrorEnabled(false);
        lyMovementReason.setErrorEnabled(false);
        lyMovementQuantity.setErrorEnabled(false);
        lyMovementSignature.setErrorEnabled(false);
    }

    @Override
    public void showMovementDateEmpty() {
        clearErrorAlerts();
        lyMovementDate.setError(getResources().getString(R.string.msg_empty_movement_date));
        etMovementDate.getBackground().setColorFilter(getResources().getColor(R.color.color_red), PorterDuff.Mode.SRC_ATOP);
    }

    @Override
    public void showMovementReasonEmpty() {
        clearErrorAlerts();
        lyMovementReason.setError(getResources().getString(R.string.msg_empty_movement_reason));
        etMovementReason.getBackground().setColorFilter(getResources().getColor(R.color.color_red), PorterDuff.Mode.SRC_ATOP);
    }

    @Override
    public void showQuantityEmpty() {
        clearErrorAlerts();
        lyMovementQuantity.setError(getResources().getString(R.string.msg_empty_quantity));
        etMovementQuantity.getBackground().setColorFilter(getResources().getColor(R.color.color_red), PorterDuff.Mode.SRC_ATOP);
    }

    @Override
    public void showSignatureEmpty() {
        clearErrorAlerts();
        lyMovementSignature.setError(getResources().getString(R.string.msg_empty_signature));
        etMovementSignature.getBackground().setColorFilter(getResources().getColor(R.color.color_red), PorterDuff.Mode.SRC_ATOP);
    }

    @Override
    public void showSOHError() {
        clearErrorAlerts();
        lyMovementQuantity.setError(getResources().getString(R.string.msg_invalid_quantity));
        etMovementQuantity.getBackground().setColorFilter(getResources().getColor(R.color.color_red), PorterDuff.Mode.SRC_ATOP);
    }

    @Override
    public void showQuantityZero() {
        clearErrorAlerts();
        lyMovementQuantity.setError(getResources().getString(R.string.msg_entries_error));
        etMovementQuantity.getBackground().setColorFilter(getResources().getColor(R.color.color_red), PorterDuff.Mode.SRC_ATOP);
    }

    @Override
    public void showSignatureError() {
        clearErrorAlerts();
        lyMovementSignature.setError(getString(R.string.hint_signature_error_message));
        etMovementSignature.getBackground().setColorFilter(getResources().getColor(R.color.color_red), PorterDuff.Mode.SRC_ATOP);
    }


    class MovementDateListener implements DatePickerDialog.OnDateSetListener {

        private Date previousMovementDate;
        private StockMovementViewModel model;

        public MovementDateListener(StockMovementViewModel model, Date previousMovementDate) {
            this.previousMovementDate = previousMovementDate;
            this.model = model;
        }

        @Override
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {

            Date chosenDate = new GregorianCalendar(year, monthOfYear, dayOfMonth).getTime();
            if (validateStockMovementDate(previousMovementDate, chosenDate)) {
                etMovementDate.setText(DateUtil.formatDate(chosenDate));
                model.setMovementDate(DateUtil.formatDate(chosenDate));
            } else {
                ToastUtil.show(R.string.msg_invalid_stock_movement_date);
            }
        }

        private boolean validateStockMovementDate(Date previousMovementDate, Date chosenDate) {
            Calendar today = GregorianCalendar.getInstance();

            return previousMovementDate == null || !previousMovementDate.after(chosenDate) && !chosenDate.after(today.getTime());
        }
    }

    class MovementTypeOnClickListener implements SimpleSelectDialogFragment.SelectorOnClickListener {

        StockMovementViewModel movementViewModel;

        public MovementTypeOnClickListener(StockMovementViewModel movementViewModel) {
            this.movementViewModel = movementViewModel;
        }

        @Override
        public void onClick(DialogInterface dialogInterface, int selectedItem) {
            etMovementReason.setText(reasonListStr[selectedItem]);
            viewModel.setReason(movementReasons.get(selectedItem));
            reasonsDialog.dismiss();
        }
    }

}
