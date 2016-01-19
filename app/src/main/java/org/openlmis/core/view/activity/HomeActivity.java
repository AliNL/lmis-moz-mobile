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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.inject.Inject;

import org.openlmis.core.LMISApp;
import org.openlmis.core.R;
import org.openlmis.core.manager.SharedPreferenceMgr;
import org.openlmis.core.manager.UserInfoMgr;
import org.openlmis.core.model.repository.MMIARepository;
import org.openlmis.core.model.repository.VIARepository;
import org.openlmis.core.service.SyncService;
import org.openlmis.core.utils.Constants;
import org.openlmis.core.utils.DateUtil;
import org.openlmis.core.utils.ToastUtil;
import org.openlmis.core.view.widget.SyncDateBottomSheet;

import java.util.Date;

import roboguice.inject.InjectResource;
import roboguice.inject.InjectView;


public class HomeActivity extends BaseActivity {

    @InjectView(R.id.btn_stock_card)
    Button btnStockCard;

    @InjectView(R.id.btn_inventory)
    Button btnInventory;

    @InjectView(R.id.tx_last_synced_rnrform)
    TextView txLastSyncedRnrForm;

    @InjectView(R.id.tx_last_synced_stockcard)
    TextView txLastSyncedStockCard;

    @InjectView(R.id.btn_mmia_list)
    Button btnMMIAList;

    @InjectView(R.id.btn_via_list)
    Button btnVIAList;

    @InjectView(R.id.btn_kit_stock_card)
    Button btnKitStockCard;

    @InjectView(R.id.iv_sync_time_icon)
    ImageView ivSyncTimeIcon;

    @InjectView(R.id.tx_sync_time)
    TextView txSyncTime;

    @InjectResource(R.integer.back_twice_interval)
    int BACK_TWICE_INTERVAL;

    @Inject
    SyncService syncService;

    private boolean exitPressedOnce = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (LMISApp.getInstance().getFeatureToggleFor(R.bool.feature_home_page_update)) {
            setContentView(R.layout.activity_home_page);
            setTitle(UserInfoMgr.getInstance().getFacilityName());
        } else {
            setContentView(R.layout.activity_home_page_old);
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }

        if (!LMISApp.getInstance().getFeatureToggleFor(R.bool.feature_home_page_update)) {
            btnVIAList.setText(LMISApp.getInstance().getText(R.string.btn_requisition_list_old));
            btnMMIAList.setText(LMISApp.getInstance().getText(R.string.btn_mmia_list_old));
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.INTENT_FILTER_SET_SYNCED_TIME);
        registerReceiver(syncedTimeReceiver, filter);

        if (!LMISApp.getInstance().getFeatureToggleFor(R.bool.feature_kit)) {
            btnKitStockCard.setVisibility(View.GONE);
        }
    }

    @Override
    protected int getThemeRes() {
        return R.style.AppTheme_Gray;
    }

    BroadcastReceiver syncedTimeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            setSyncedTime();
        }
    };

    @Override
    protected void onDestroy() {
        unregisterReceiver(syncedTimeReceiver);
        super.onDestroy();
    }

    public void onClickStockCard(View view) {
        startActivity(StockCardListActivity.class);
    }

    public void onClickKitStockCard(View view) {
        startActivity(KitStockCardListActivity.class);
    }

    public void onClickInventory(View view) {
        Intent intent = new Intent(HomeActivity.this, InventoryActivity.class);
        intent.putExtra(Constants.PARAM_IS_PHYSICAL_INVENTORY, true);
        startActivity(intent);
    }

    public void onClickMMIA(View view) {
        startActivity(MMIARequisitionActivity.class, false);
    }

    public void onClickRequisition(View view) {
        startActivity(VIARequisitionActivity.class, false);
    }

    public void onClickSyncData(View view) {
        Log.d("HomeActivity", "requesting immediate sync");
        syncService.requestSyncImmediately();
    }

    public void onClickSyncData() {
        Log.d("HomeActivity", "requesting immediate sync");
        syncService.requestSyncImmediately();
    }

    public void onClickMMIAHistory(View view) {
        Intent intent = new Intent(this, RnRFormListActivity.class);
        intent.putExtra(Constants.PARAM_PROGRAM_CODE, MMIARepository.MMIA_PROGRAM_CODE);
        startActivity(intent);
    }

    public void onClickVIAHistory(View view) {
        Intent intent = new Intent(this, RnRFormListActivity.class);
        intent.putExtra(Constants.PARAM_PROGRAM_CODE, VIARepository.VIA_PROGRAM_CODE);
        startActivity(intent);
    }

    public void onClickLastSyncTime(View view) {
        SyncDateBottomSheet syncDateBottomSheet = new SyncDateBottomSheet();
        syncDateBottomSheet.setArguments(createSyncDateBundle());
        syncDateBottomSheet.show(getFragmentManager());
    }

    private Bundle createSyncDateBundle() {
        Bundle bundle = new Bundle();
        bundle.putString(SyncDateBottomSheet.RNR_SYNC_TIME, "2015-12-12");
        bundle.putString(SyncDateBottomSheet.STOCK_SYNC_TIME, "2016-1-1");
        return bundle;
    }

    @Override
    protected void onResume() {
        super.onResume();
        setSyncedTime();
    }

    protected void setSyncedTime() {
        showRnrFormLastSyncedTime();
        showStockCardLastSyncedTime();

        showLastSyncTime();
    }

    private void showLastSyncTime() {
        long rnrLastSyncTime = getPreferences().getLong(SharedPreferenceMgr.KEY_LAST_SYNCED_TIME_RNR_FORM, 0);
        long stockLastSyncTime = getPreferences().getLong(SharedPreferenceMgr.KEY_LAST_SYNCED_TIME_STOCKCARD, 0);
        if (rnrLastSyncTime == 0 && stockLastSyncTime == 0){
            return;
        }

        long syncTimeInterval = getSyncTimeInterval(rnrLastSyncTime, stockLastSyncTime);
        txSyncTime.setText(syncTimeInterval + " " + "since last sync");

        if (syncTimeInterval < DateUtil.MILLISECONDS_DAY) {
            ivSyncTimeIcon.setImageResource(R.drawable.ic_done);
        } else if (syncTimeInterval < DateUtil.MILLISECONDS_DAY * 3) {
            ivSyncTimeIcon.setImageResource(R.drawable.ic_clear);
        } else {
            ivSyncTimeIcon.setImageResource(R.drawable.ic_save);
        }
    }

    private long getSyncTimeInterval(long rnrLastSyncTime, long stockLastSyncTime) {
        long syncTimeInterval;
        long rnrSyncInterval = calculateTimeInterval(rnrLastSyncTime);
        long stockSyncInterval = calculateTimeInterval(stockLastSyncTime);
        if (rnrLastSyncTime == 0) {
            syncTimeInterval = stockSyncInterval;
        }else if(stockLastSyncTime == 0){
            syncTimeInterval = rnrSyncInterval;
        }else{
            syncTimeInterval = rnrSyncInterval < stockSyncInterval ? rnrSyncInterval : stockSyncInterval;
        }
        return syncTimeInterval;
    }

    private long calculateTimeInterval(long lastSyncedTimestamp) {
        return new Date().getTime() - lastSyncedTimestamp;
    }

    private void showRnrFormLastSyncedTime() {
        long rnrSyncedTimestamp = getPreferences().getLong(SharedPreferenceMgr.KEY_LAST_SYNCED_TIME_RNR_FORM, 0);
        if (rnrSyncedTimestamp == 0) {
            return;
        }

        long diff = calculateTimeInterval(rnrSyncedTimestamp);

        if (diff < DateUtil.MILLISECONDS_HOUR) {
            txLastSyncedRnrForm.setText(getResources().getString(R.string.label_rnr_form_last_synced_mins_ago, (diff / DateUtil.MILLISECONDS_MINUTE)));
        } else if (diff < DateUtil.MILLISECONDS_DAY) {
            txLastSyncedRnrForm.setText(getResources().getString(R.string.label_rnr_form_last_synced_hours_ago, (diff / DateUtil.MILLISECONDS_HOUR)));
        } else {
            txLastSyncedRnrForm.setText(getResources().getString(R.string.label_rnr_form_last_synced_days_ago, (diff / DateUtil.MILLISECONDS_DAY)));
        }
    }

    private void showStockCardLastSyncedTime() {
        long stockSyncedTimestamp = getPreferences().getLong(SharedPreferenceMgr.KEY_LAST_SYNCED_TIME_STOCKCARD, 0);
        if (stockSyncedTimestamp == 0) {
            return;
        }

        long diff = calculateTimeInterval(stockSyncedTimestamp);

        if (diff < DateUtil.MILLISECONDS_HOUR) {
            txLastSyncedStockCard.setText(getResources().getString(R.string.label_stock_card_last_synced_mins_ago, (diff / DateUtil.MILLISECONDS_MINUTE)));
        } else if (diff < DateUtil.MILLISECONDS_DAY) {
            txLastSyncedStockCard.setText(getResources().getString(R.string.label_stock_card_last_synced_hours_ago, (diff / DateUtil.MILLISECONDS_HOUR)));
        } else {
            txLastSyncedStockCard.setText(getResources().getString(R.string.label_stock_card_last_synced_days_ago, (diff / DateUtil.MILLISECONDS_DAY)));
        }
    }

    @Override
    public void onBackPressed() {
        if (exitPressedOnce) {
            moveTaskToBack(true);
        } else {
            ToastUtil.show(R.string.msg_back_twice_to_exit);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    exitPressedOnce = false;
                }
            }, BACK_TWICE_INTERVAL);
        }
        exitPressedOnce = !exitPressedOnce;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        if (LMISApp.getInstance().getFeatureToggleFor(R.bool.feature_home_page_update)) {
            inflater.inflate(R.menu.menu_home, menu);
        } else {
            inflater.inflate(R.menu.menu_home_old, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_sign_out) {
            startActivity(LoginActivity.class);
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_sync_data) {
            onClickSyncData();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static Intent getIntentToMe(Context context) {
        Intent intent = new Intent(context, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return intent;
    }

}
