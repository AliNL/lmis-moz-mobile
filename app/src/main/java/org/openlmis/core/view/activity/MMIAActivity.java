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
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import org.openlmis.core.R;
import org.openlmis.core.exceptions.LMISException;
import org.openlmis.core.model.RnRForm;
import org.openlmis.core.presenter.MMIAFormPresenter;
import org.openlmis.core.utils.ToastUtil;
import org.openlmis.core.view.fragment.BaseDialogFragment;
import org.openlmis.core.view.fragment.RetainedFragment;
import org.openlmis.core.view.viewmodel.RnRFormViewModel;
import org.openlmis.core.view.widget.MMIAInfoList;
import org.openlmis.core.view.widget.MMIARegimeList;
import org.openlmis.core.view.widget.MMIARnrForm;

import java.util.ArrayList;

import roboguice.RoboGuice;
import roboguice.inject.ContentView;
import roboguice.inject.InjectView;

@ContentView(R.layout.activity_mmia)
public class MMIAActivity extends BaseActivity implements MMIAFormPresenter.MMIAFormView, View.OnClickListener,BaseDialogFragment.PositiveListener,BaseDialogFragment.NegativeListener{

    @InjectView(R.id.rnr_form_list)
    private MMIARnrForm rnrFormList;

    @InjectView(R.id.regime_list)
    protected MMIARegimeList regimeListView;

    @InjectView(R.id.mmia_info_list)
    protected MMIAInfoList mmiaInfoListView;

    @InjectView(R.id.btn_complete)
    private Button btnComplete;

    @InjectView(R.id.tv_regime_total)
    protected TextView tvRegimeTotal;

    @InjectView(R.id.et_comment)
    private TextView etComment;

    @InjectView(R.id.scrollview)
    private ScrollView scrollView;

    @InjectView(R.id.btn_save)
    private View btnSave;

    @InjectView(R.id.tv_total_mismatch)
    protected TextView tvMismatch;

    MMIAFormPresenter presenter;

    Boolean hasDataChanged;

    private RetainedFragment dataFragment;
    private boolean commentHasChanged = false;
    private boolean isHistoryForm;
    private long formId;

    private static final String ON_BACK_PRESSED = "onBackPressed";
    private static final String MISMATCH = "mismatch";

    @Override
    public MMIAFormPresenter getPresenter() {
        initPresenter();
        return presenter;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    private void initPresenter() {
        FragmentManager fm = getFragmentManager();
        dataFragment = (RetainedFragment) fm.findFragmentByTag("RetainedFragment");

        if (dataFragment == null) {
            dataFragment = new RetainedFragment();
            fm.beginTransaction().add(dataFragment, "RetainedFragment").commit();
            presenter = RoboGuice.getInjector(getApplicationContext()).getInstance(MMIAFormPresenter.class);
            dataFragment.putData("presenter", presenter);
        } else {
            presenter = (MMIAFormPresenter) dataFragment.getData("presenter");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hasDataChanged = (Boolean) dataFragment.getData("hasDataChanged");
        scrollView.setVisibility(View.INVISIBLE);

        formId = getIntent().getLongExtra("formId", 0);
        isHistoryForm = formId != 0;
        presenter.loadData(formId);
    }

    @Override
    public void initView(final RnRForm form) {
        scrollView.setVisibility(View.VISIBLE);
        rnrFormList.initView(new ArrayList<>(form.getRnrFormItemListWrapper()));
        regimeListView.initView(form.getRegimenItemListWrapper(), tvRegimeTotal);
        mmiaInfoListView.initView(form.getBaseInfoItemListWrapper());


        if (presenter.formIsEditable()) {
            scrollView.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
            btnSave.setVisibility(View.VISIBLE);
            btnComplete.setVisibility(View.VISIBLE);
        } else {
            scrollView.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
            btnSave.setVisibility(View.GONE);
            btnComplete.setVisibility(View.GONE);

            setTitle(new RnRFormViewModel(form).getPeriod());
        }

        highlightTotalDifference();

        etComment.setText(form.getComments());

        etComment.post(new Runnable() {
            @Override
            public void run() {
                etComment.addTextChangedListener(commentTextWatcher);
            }
        });

        tvRegimeTotal.post(new Runnable() {
            @Override
            public void run() {
                tvRegimeTotal.addTextChangedListener(totalTextWatcher);
            }
        });

        final EditText patientTotalView = mmiaInfoListView.getPatientTotalView();
        patientTotalView.post(new Runnable() {
            @Override
            public void run() {
                patientTotalView.addTextChangedListener(totalTextWatcher);
            }
        });

        btnSave.setOnClickListener(this);

        btnComplete.setOnClickListener(this);

        scrollView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                hideImm();
                return false;
            }
        });
    }

    TextWatcher commentTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            commentHasChanged = true;
            try {
                presenter.getRnrForm(formId).setComments(s.toString());
            } catch (LMISException e) {
                e.printStackTrace();
            }
        }
    };

    TextWatcher totalTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            highlightTotalDifference();
        }
    };

    private void highlightTotalDifference() {
        if (isTotalEqual()) {
            regimeListView.deHighLightTotal();
            mmiaInfoListView.deHighLightTotal();
            tvMismatch.setVisibility(View.INVISIBLE);
        } else {
            regimeListView.highLightTotal();
            mmiaInfoListView.highLightTotal();
            tvMismatch.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onBackPressed() {
        if (hasDataChanged()) {
            DialogFragment dialogFragment = BaseDialogFragment
                    .newInstance()
                    .setMessage(getResources().getString(R.string.msg_mmia_onback_confirm))
                    .setPositiveText(getResources().getString(R.string.btn_positive))
                    .setNegativeText(getResources().getString(R.string.btn_negative))
                    .setTag(ON_BACK_PRESSED);
            dialogFragment.show(getFragmentManager(), "tag");
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

    private boolean hasDataChanged() {
        if (hasDataChanged == null) {
            hasDataChanged = regimeListView.hasDataChanged() || mmiaInfoListView.hasDataChanged() || commentHasChanged;
        }
        return hasDataChanged;
    }

    private void goToHomePage() {
        Intent intent = new Intent(MMIAActivity.this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(HomeActivity.class, true);
    }

    @Override
    public void showValidationAlert() {
        DialogFragment dialogFragment = BaseDialogFragment
                .newInstance()
                .setMessage(getResources().getString(R.string.msg_regime_total_and_patient_total_not_match))
                .setPositiveText(getString(R.string.btn_ok))
                .setTag(MISMATCH);
        dialogFragment.show(getFragmentManager(), "tag");
    }

    @Override
    public void showErrorMessage(String msg) {
        ToastUtil.show(msg);
    }

    @Override
    protected void onDestroy() {
        dataFragment.putData("presenter", presenter);
        if (hasDataChanged()) {
            dataFragment.putData("hasDataChanged", hasDataChanged());
        }
        super.onDestroy();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_save:
                onSaveBtnClick();
                break;
            case R.id.btn_complete:
                onCompleteBtnClick();
                break;
            default:
                break;

        }
    }

    private void onCompleteBtnClick() {
        if (!regimeListView.isCompleted() || !mmiaInfoListView.isCompleted()) {
            return;
        }

        presenter.completeMMIA(regimeListView.getDataList(), mmiaInfoListView.getDataList(), etComment.getText().toString());
    }

    @Override
    public void completeSuccess() {
        ToastUtil.showForLongTime(R.string.msg_mmia_submit_tip);
        goToHomePage();
    }

    @Override
    public void saveSuccess() {
        goToHomePage();
    }

    private boolean isTotalEqual() {
        return regimeListView.getTotal() == mmiaInfoListView.getTotal();
    }

    private void onSaveBtnClick() {
        presenter.saveDraftForm(regimeListView.getDataList(), mmiaInfoListView.getDataList(), etComment.getText().toString());
    }

    public static Intent getIntentToMe(Context context, long formId) {
        Intent intent = new Intent(context, MMIAActivity.class);
        intent.putExtra("formId", formId);
        return intent;
    }

    @Override
    public void positiveClick(String tag) {
        if (tag.equals(ON_BACK_PRESSED)){
            removeTempForm();
            finish();
        }
    }

    @Override
    public void negativeClick(String tag) {
    }
}
