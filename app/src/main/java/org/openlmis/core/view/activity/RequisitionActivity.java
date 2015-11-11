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

package org.openlmis.core.view.activity;

import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ListView;
import android.widget.TextView;

import org.openlmis.core.R;
import org.openlmis.core.model.RnRForm;
import org.openlmis.core.presenter.RequisitionPresenter;
import org.openlmis.core.utils.Constants;
import org.openlmis.core.utils.InjectPresenter;
import org.openlmis.core.utils.ToastUtil;
import org.openlmis.core.view.adapter.RequisitionFormAdapter;
import org.openlmis.core.view.adapter.RequisitionProductAdapter;
import org.openlmis.core.view.fragment.BaseDialogFragment;
import org.openlmis.core.view.holder.RequisitionFormViewHolder;
import org.openlmis.core.view.viewmodel.RnRFormViewModel;
import org.openlmis.core.view.widget.InputFilterMinMax;

import roboguice.inject.ContentView;
import roboguice.inject.InjectView;


@ContentView(R.layout.activity_requisition)
public class RequisitionActivity extends BaseActivity implements RequisitionPresenter.RequisitionView, View.OnClickListener, BaseDialogFragment.MsgDialogCallBack {

    @InjectView(R.id.requisition_form)
    ListView requisitionForm;

    @InjectView(R.id.product_name_list_view)
    ListView requisitionNameList;

    @InjectView(R.id.btn_complete)
    private Button btnComplete;

    @InjectView(R.id.btn_save)
    private View btnSave;

    @InjectView(R.id.action_panel)
    private View actionPanel;

    @InjectView(R.id.vg_container)
    private ViewGroup vgContainer;

    @InjectView(R.id.edit_text)
    private EditText etConsultationNumbers;

    @InjectPresenter(RequisitionPresenter.class)
    RequisitionPresenter presenter;

    @InjectView(R.id.requisition_header_right)
    View bodyHeaderView;

    @InjectView(R.id.requisition_header_left)
    View productHeaderView;

    @InjectView(R.id.form_layout)
    HorizontalScrollView formLayout;

    @InjectView(R.id.tv_label_request)
    TextView headerRequestAmount;

    @InjectView(R.id.tv_label_approve)
    TextView headerApproveAmount;

    RequisitionProductAdapter requisitionProductAdapter;
    RequisitionFormAdapter requisitionFormAdapter;

    private boolean consultationNumbersHasChanged;
    private boolean isHistoryForm;
    protected Boolean hasDataChanged;
    private static final String ON_BACK_PRESSED = "onBackPressed";

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        long formId = getIntent().getLongExtra(Constants.PARAM_FORM_ID, 0);
        isHistoryForm = formId != 0;

        hasDataChanged = (Boolean) dataFragment.getData(Constants.KEY_HAS_DATA_CHANGED);
        initUI();
        presenter.loadRequisitionFormList(formId);
    }


    @Override
    public void refreshRequisitionForm() {
        if (isHistoryForm) {
            setTitle(new RnRFormViewModel(presenter.getRnRForm()).getPeriod());
        }
        requisitionProductAdapter.notifyDataSetChanged();
        requisitionFormAdapter.notifyDataSetChanged();
        setConsultationNumbers();
    }


    @Override
    public void highLightApprovedAmount() {
        headerRequestAmount.setBackgroundResource(android.R.color.transparent);
        headerRequestAmount.setTextColor(getResources().getColor(R.color.primary_text));

        headerApproveAmount.setBackgroundResource(R.color.color_accent);
        headerApproveAmount.setTextColor(getResources().getColor(R.color.white));

        requisitionFormAdapter.updateStatus(RnRForm.STATUS.SUBMITTED);
    }

    @Override
    public void highLightRequestAmount() {
        headerRequestAmount.setBackgroundResource(R.color.color_accent);
        headerRequestAmount.setTextColor(getResources().getColor(R.color.white));

        headerApproveAmount.setBackgroundResource(android.R.color.transparent);
        headerApproveAmount.setTextColor(getResources().getColor(R.color.primary_text));

        requisitionFormAdapter.updateStatus(RnRForm.STATUS.DRAFT);
    }

    private void setEditable() {
        if (isHistoryForm) {
            vgContainer.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
            actionPanel.setVisibility(View.GONE);
        } else {
            vgContainer.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
            actionPanel.setVisibility(View.VISIBLE);
        }
    }

    private void setConsultationNumbers() {
        etConsultationNumbers.setText(presenter.getConsultationNumbers());
        etConsultationNumbers.post(new Runnable() {
            @Override
            public void run() {
                etConsultationNumbers.addTextChangedListener(etConsultationNumbersTextWatcher);
            }
        });
    }

    @Override
    public void showErrorMessage(String msg) {
        ToastUtil.show(msg);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_save:
                onSaveBtnClick();
                break;
            case R.id.btn_complete:
                onProcessButtonClick();
                break;
        }
    }

    @Override
    public void showListInputError(int index) {
        final int position = index;
        requisitionForm.setSelection(position);
        requisitionForm.post(new Runnable() {
            @Override
            public void run() {
                View childAt = getViewByPosition(position, requisitionForm);
                EditText requestAmount = (EditText) childAt.findViewById(R.id.et_request_amount);
                EditText approvedAmount = (EditText) childAt.findViewById(R.id.et_approved_amount);
                if (requestAmount.isEnabled()) {
                    requestAmount.requestFocus();
                    requestAmount.setError(getString(R.string.hint_error_input));
                } else {
                    approvedAmount.requestFocus();
                    approvedAmount.setError(getString(R.string.hint_error_input));
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (hasDataChanged()) {
            dataFragment.putData(Constants.KEY_HAS_DATA_CHANGED, hasDataChanged());
        }
        super.onDestroy();
    }

    private void initUI() {
        initRequisitionBodyList();
        initRequisitionProductList();

        requisitionNameList.post(new Runnable() {
            @Override
            public void run() {
                productHeaderView.getLayoutParams().height = bodyHeaderView.getHeight();
            }
        });

        btnComplete.setText(getString(R.string.btn_submit));
        etConsultationNumbers.setFilters(new InputFilter[]{new InputFilterMinMax(Integer.MAX_VALUE)});

        setUIListeners();
        setEditable();
    }

    private void setUIListeners() {
        requisitionForm.setOnScrollListener(new MyScrollListener(requisitionForm, requisitionNameList));
        requisitionNameList.setOnScrollListener(new MyScrollListener(requisitionNameList, requisitionForm));
        btnComplete.setOnClickListener(this);
        btnSave.setOnClickListener(this);

        etConsultationNumbers.post(new Runnable() {
            @Override
            public void run() {
                etConsultationNumbers.addTextChangedListener(etConsultationNumbersTextWatcher);
            }
        });

        formLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                hideImm();
                return false;
            }
        });
    }

    private void initRequisitionBodyList() {
        requisitionFormAdapter = new RequisitionFormAdapter(this, presenter.getRequisitionViewModelList());
        requisitionForm.setAdapter(requisitionFormAdapter);
    }

    private void initRequisitionProductList() {
        requisitionProductAdapter = new RequisitionProductAdapter(this, presenter.getRequisitionViewModelList());
        requisitionNameList.setAdapter(requisitionProductAdapter);
    }

    @Override
    public void setProcessButtonName(String name) {
        btnComplete.setText(name);
    }

    protected void onProcessButtonClick() {
        String consultationNumbers = etConsultationNumbers.getText().toString();
        if (TextUtils.isEmpty(consultationNumbers)) {
            etConsultationNumbers.setError(getString(R.string.hint_error_input));
            return;
        }
        presenter.processRequisition(consultationNumbers);
    }

    @Override
    public void completeSuccess() {
        ToastUtil.showForLongTime(R.string.msg_requisition_submit_tip);
        goToHomePage();
    }

    public View getViewByPosition(int pos, ListView listView) {
        final int firstListItemPosition = listView.getFirstVisiblePosition();
        final int lastListItemPosition = firstListItemPosition + listView.getChildCount() - 1;

        if (pos < firstListItemPosition || pos > lastListItemPosition) {
            return listView.getAdapter().getView(pos, null, listView);
        } else {
            final int childIndex = pos - firstListItemPosition;
            return listView.getChildAt(childIndex);
        }
    }

    private void onSaveBtnClick() {
        presenter.saveRequisition(etConsultationNumbers.getText().toString());
    }


    private class MyScrollListener implements AbsListView.OnScrollListener {

        ListView list1;
        ListView list2;

        public MyScrollListener(ListView list1, ListView list2) {
            this.list1 = list1;
            this.list2 = list2;
        }

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            if (scrollState == 0 || scrollState == 1) {
                View subView1 = view.getChildAt(0);

                if (subView1 != null) {
                    final int top1 = subView1.getTop();
                    View subview2 = list2.getChildAt(0);
                    if (subview2 != null) {
                        int top2 = subview2.getTop();
                        int position = view.getFirstVisiblePosition();

                        if (top1 != top2) {
                            list2.setSelectionFromTop(position, top1);
                        }
                    }
                }
            }
        }

        public void onScroll(AbsListView view, int firstVisibleItem,
                             int visibleItemCount, int totalItemCount) {
            View subView1 = view.getChildAt(0);
            if (subView1 != null) {
                int top1 = subView1.getTop();

                View subView2 = list2.getChildAt(0);
                if (subView2 != null) {
                    int top2 = list2.getChildAt(0).getTop();
                    if (top1 != top2) {
                        list1.setSelectionFromTop(firstVisibleItem, top1);
                        list2.setSelectionFromTop(firstVisibleItem, top1);
                    }
                }
            }
        }
    }


    @Override
    public void goToHomePage() {
        Intent intent = new Intent(RequisitionActivity.this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(HomeActivity.class, true);
    }

    private boolean hasDataChanged() {
        if (hasDataChanged == null) {
            hasDataChanged = requisitionFormChanged() || consultationNumbersHasChanged;
        }
        return hasDataChanged;
    }

    private boolean requisitionFormChanged() {
        for (int index = 0; index < requisitionForm.getChildCount(); index++) {
            Object requisitionItemTag = requisitionForm.getChildAt(index).getTag();
            if (requisitionItemTag != null &&
                    requisitionItemTag instanceof RequisitionFormViewHolder &&
                    ((RequisitionFormViewHolder) requisitionItemTag).isHasDataChanged()) {
                return true;
            }
        }
        return false;
    }

    TextWatcher etConsultationNumbersTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            String input = etConsultationNumbers.getText().toString();
            if (!input.equals(presenter.getConsultationNumbers())) {
                consultationNumbersHasChanged = true;
                presenter.setConsultationNumbers(input);
            }
        }
    };

    @Override
    public void onBackPressed() {
        if (hasDataChanged()) {
            DialogFragment dialogFragment = BaseDialogFragment.newInstance(null,
                    getString(R.string.msg_mmia_onback_confirm),
                    getString(R.string.btn_positive),
                    getString(R.string.btn_negative),
                    ON_BACK_PRESSED);
            dialogFragment.show(getFragmentManager(), "back_confirm_dialog");
        } else {
            removeTempForm();
            super.onBackPressed();
        }
    }

    private void removeTempForm() {
        if (!isHistoryForm) {
            presenter.removeRnrForm();
        }
    }

    public static Intent getIntentToMe(Context context, long formId) {
        Intent intent = new Intent(context, RequisitionActivity.class);
        intent.putExtra(Constants.PARAM_FORM_ID, formId);
        return intent;
    }

    @Override
    public void positiveClick(String tag) {
        if (tag.equals(ON_BACK_PRESSED)) {
            removeTempForm();
            finish();
        }
    }

    @Override
    public void negativeClick(String tag) {
    }
}
