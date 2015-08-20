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

import android.os.Bundle;
import android.view.Menu;
import android.widget.Button;
import android.widget.TextView;

import com.google.inject.Inject;

import org.openlmis.core.R;
import org.openlmis.core.manager.SharedPreferenceMgr;
import org.openlmis.core.presenter.Presenter;
import org.openlmis.core.service.SyncManager;
import org.openlmis.core.utils.DateUtil;
import org.openlmis.core.utils.ToastUtil;
import org.openlmis.core.view.View;

import java.util.Date;

import roboguice.inject.ContentView;
import roboguice.inject.InjectView;


@ContentView(R.layout.activity_main_page)
public class HomeActivity extends BaseActivity{

    @InjectView(R.id.btn_stock_card)
    Button btnStockCard;

    @InjectView(R.id.btn_inventory)
    Button btnInventory;

    @InjectView(R.id.btn_mmia)
    Button btnMMIA;

    @InjectView(R.id.btn_requisition)
    Button btnRequisition;

    @InjectView(R.id.btn_sync_data)
    Button btnSyncData;

    @InjectView(R.id.tx_last_synced)
    TextView txLastSynced;

    @Inject
    SyncManager syncManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        btnStockCard.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                startActivity(StockCardListActivity.class, false);
            }
        });

        btnInventory.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                ToastUtil.show("Jump to Inventory...");
            }
        });

        btnMMIA.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                startActivity(MMIAActivity.class, false);
            }
        });

        btnRequisition.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                startActivity(RequisitionActivity.class, false);
            }
        });

        btnSyncData.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                syncManager.requestSyncImmediately();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        showLastSyncedTime();
    }


    private void showLastSyncedTime(){
        long lastSyncedTimestamp = getPreferences().getLong(SharedPreferenceMgr.KEY_LAST_SYNCED_TIME, 0);
        if (lastSyncedTimestamp == 0){
            return;
        }

        long currentTimestamp = new Date().getTime();

        long diff = currentTimestamp - lastSyncedTimestamp;

        if (diff < DateUtil.MILLISECONDS_HOUR){
            txLastSynced.setText(getResources().getString(R.string.label_last_synced_mins_ago, (diff /DateUtil.MILLISECONDS_MINUTE)));
        } else if (diff < DateUtil.MILLISECONDS_DAY){
            txLastSynced.setText(getResources().getString(R.string.label_last_synced_hours_ago, (diff /DateUtil.MILLISECONDS_HOUR)));
        } else {
            txLastSynced.setText(getResources().getString(R.string.label_last_synced_days_ago, (diff /DateUtil.MILLISECONDS_DAY)));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    @Override
    public Presenter getPresenter() {
        return new Presenter() {
            @Override
            public void onStart() {

            }

            @Override
            public void onStop() {

            }

            @Override
            public void attachView(View v) {

            }
        };
    }
}
