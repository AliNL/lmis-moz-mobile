package org.openlmis.core.view.activity;

import android.content.Intent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openlmis.core.LMISTestRunner;
import org.openlmis.core.utils.Constants;
import org.robolectric.Robolectric;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(LMISTestRunner.class)
public class StockMovementHistoryActivityTest {

    private StockMovementHistoryActivity activity;

    @Before
    public void setUp() {
        Intent intent = new Intent();
        intent.putExtra(Constants.PARAM_STOCK_NAME, "Stock Name");

        activity = Robolectric.buildActivity(StockMovementHistoryActivity.class).withIntent(intent).create().get();
    }

    @Test
    public void shouldSetTitleWhenActivityCreated() {
        assertThat(activity).isNotNull();
        assertThat(activity.getTitle()).isEqualTo("Stock Name");
    }
}