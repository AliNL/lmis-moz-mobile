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
import android.content.Intent;
import android.view.MenuItem;
import android.view.MotionEvent;

import org.hamcrest.core.Is;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openlmis.core.LMISApp;
import org.openlmis.core.LMISTestApp;
import org.openlmis.core.LMISTestRunner;
import org.openlmis.core.R;
import org.openlmis.core.manager.SharedPreferenceMgr;
import org.openlmis.core.utils.Constants;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.fakes.RoboMenuItem;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowIntent;
import org.robolectric.shadows.ShadowToast;

import roboguice.RoboGuice;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.mock;
import static org.robolectric.Shadows.shadowOf;

@RunWith(LMISTestRunner.class)
public class HomeActivityTest {

    private HomeActivity homeActivity;
    private LMISTestApp testApp;
    protected SharedPreferenceMgr sharedPreferenceMgr;

    @Before
    public void setUp() {
        testApp = (LMISTestApp) RuntimeEnvironment.application;
        homeActivity = Robolectric.buildActivity(HomeActivity.class).create().get();
        sharedPreferenceMgr = RoboGuice.getInjector(RuntimeEnvironment.application).getInstance(SharedPreferenceMgr.class);
    }

    @Test
    public void shouldGoToStockCardsPage() {
        homeActivity.btnStockCard.performClick();

        Intent nextStartedIntent = shadowOf(homeActivity).getNextStartedActivity();
        assertEquals(StockCardListActivity.class.getName(), nextStartedIntent.getComponent().getClassName());
    }

    @Test
    public void shouldGoToKitsStockCardsPage() throws Exception {
        homeActivity.btnKitStockCard.performClick();

        Intent nextStartedIntent = shadowOf(homeActivity).getNextStartedActivity();
        assertEquals(KitStockCardListActivity.class.getName(), nextStartedIntent.getComponent().getClassName());
    }

    @Test
    public void shouldGoToInventoryPage() {
        homeActivity.btnInventory.performClick();

        Intent startedIntent = shadowOf(homeActivity).getNextStartedActivity();

        assertThat(startedIntent.getComponent().getClassName(), equalTo(PhysicalInventoryActivity.class.getName()));
    }

    @Test
    public void shouldGoToMMIAHistoryPage() {
        homeActivity.btnMMIAList.performClick();

        Intent startedIntent = shadowOf(homeActivity).getNextStartedActivity();

        assertThat(startedIntent.getComponent().getClassName(), equalTo(RnRFormListActivity.class.getName()));
        assertThat(startedIntent.getStringExtra(Constants.PARAM_PROGRAM_CODE), is(Constants.MMIA_PROGRAM_CODE));
    }

    @Test
    public void shouldGoToViaHistoryPage() {
        homeActivity.btnVIAList.performClick();

        Intent startedIntent = shadowOf(homeActivity).getNextStartedActivity();

        assertThat(startedIntent.getComponent().getClassName(), equalTo(RnRFormListActivity.class.getName()));
        assertThat(startedIntent.getStringExtra(Constants.PARAM_PROGRAM_CODE), is(Constants.VIA_PROGRAM_CODE));
    }

    private void verifyNextPage(String className) {
        ShadowActivity shadowActivity = shadowOf(homeActivity);
        Intent startedIntent = shadowActivity.getNextStartedActivity();
        ShadowIntent shadowIntent = shadowOf(startedIntent);
        assertThat(shadowIntent.getComponent().getClassName(), equalTo(className));
    }

    @Test
    public void shouldNotLogOutOrResetTimeIfFirstTimeOperation() throws Exception {
        testApp.setCurrentTimeMillis(1234L);
        homeActivity.dispatchTouchEvent(mock(MotionEvent.class));
        Assert.assertThat(LMISApp.lastOperateTime, Is.is(not(0L)));
    }

    @Test
    public void shouldNotLogOutOrResetTimeIfNotTimeOut() throws Exception {
        testApp.setCurrentTimeMillis(10000L);
        homeActivity.dispatchTouchEvent(mock(MotionEvent.class));

        testApp.setCurrentTimeMillis(9000L + Long.parseLong(homeActivity.getString(R.string.app_time_out)));
        homeActivity.dispatchTouchEvent(mock(MotionEvent.class));

        Assert.assertThat(LMISApp.lastOperateTime, Is.is(not(0L)));
        Intent startedIntent = shadowOf(homeActivity).getNextStartedActivity();
        assertNull(startedIntent);
    }

    @Test
    public void shouldLogOutAndResetTimeIfTimeOut() throws Exception {
        testApp.setCurrentTimeMillis(10000L);
        homeActivity.dispatchTouchEvent(mock(MotionEvent.class));

        testApp.setCurrentTimeMillis(11000L + Long.parseLong(homeActivity.getString(R.string.app_time_out)));
        homeActivity.dispatchTouchEvent(mock(MotionEvent.class));

        Assert.assertThat(LMISApp.lastOperateTime, is(0L));

        Intent startedIntent = shadowOf(homeActivity).getNextStartedActivity();
        assertThat(startedIntent.getComponent().getClassName(), equalTo(LoginActivity.class.getName()));
    }

    @Test
    public void shouldToastWarningMessageWhenClickBackButtonFirstTime() {
        homeActivity.onBackPressed();

        String warningMessage = ShadowToast.getTextOfLatestToast();

        assertThat(warningMessage, equalTo(homeActivity.getString(R.string.msg_back_twice_to_exit)));
    }

    @Test
    public void shouldFinishMainActivityAndStartLoginActivityWhenSighOutClicked() {
        MenuItem signoutAction = new RoboMenuItem(R.id.action_sign_out);

        homeActivity.onOptionsItemSelected(signoutAction);

        assertTrue(homeActivity.isFinishing());
        verifyNextPage(LoginActivity.class.getName());
    }

    @Test
    public void shouldShowNewTextOfMMIAListAndVIALIstButtons() throws Exception {
        HomeActivity activity = Robolectric.buildActivity(HomeActivity.class).create().get();

        assertThat(activity.btnMMIAList.getText().toString(), is(activity.getString(R.string.mmia_list)));
        assertThat(activity.btnVIAList.getText().toString(), is(activity.getString(R.string.requisition_list)));
    }

    @Test
    public void shouldShowWarningDialogWhenWipeDataWiped() throws Exception {
        LMISTestApp.getInstance().setNetworkConnection(true);

        homeActivity.onOptionsItemSelected(new RoboMenuItem(R.id.action_wipe_data));
        DialogFragment dialogFragment = (DialogFragment) homeActivity.getFragmentManager().findFragmentByTag("WipeDataWarning");

        assertNotNull(dialogFragment);
    }

    @Test
    public void shouldShowToastWhenResyncWithoutNetwork() {
        LMISTestApp.getInstance().setNetworkConnection(false);

        homeActivity.onOptionsItemSelected(new RoboMenuItem(R.id.action_wipe_data));

        String toastMessage = ShadowToast.getTextOfLatestToast();
        assertThat(toastMessage, is(LMISApp.getInstance().getString(R.string.message_wipe_no_connection)));
    }
}
