package org.openlmis.core.view.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.openlmis.core.R;
import org.openlmis.core.manager.SharedPreferenceMgr;
import org.openlmis.core.utils.DateUtil;

import java.util.Date;

import roboguice.RoboGuice;
import roboguice.inject.InjectView;

public class SyncTimeView extends LinearLayout {

    @InjectView(R.id.tx_sync_time)
    TextView txSyncTime;

    @InjectView(R.id.iv_sync_time_icon)
    ImageView ivSyncTimeIcon;

    public SyncTimeView(Context context) {
        super(context);
        init(context);
    }

    public SyncTimeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.view_sync_time, this);
        RoboGuice.injectMembers(getContext(), this);
        RoboGuice.getInjector(getContext()).injectViewMembers(this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        txSyncTime.setText("");
    }

    public void showLastSyncTime() {
        long rnrLastSyncTime = SharedPreferenceMgr.getInstance().getPreference().getLong(SharedPreferenceMgr.KEY_LAST_SYNCED_TIME_RNR_FORM, 0);
        long stockLastSyncTime = SharedPreferenceMgr.getInstance().getPreference().getLong(SharedPreferenceMgr.KEY_LAST_SYNCED_TIME_STOCKCARD, 0);
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
}
