package org.openlmis.core.view.activity;

import android.app.Activity;
import android.content.Intent;
import android.view.MenuItem;
import android.view.View;

import com.google.inject.AbstractModule;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openlmis.core.LMISTestApp;
import org.openlmis.core.LMISTestRunner;
import org.openlmis.core.R;
import org.openlmis.core.exceptions.LMISException;
import org.openlmis.core.model.Product;
import org.openlmis.core.model.StockCard;
import org.openlmis.core.model.builder.ProductBuilder;
import org.openlmis.core.model.builder.StockCardBuilder;
import org.openlmis.core.presenter.StockMovementPresenter;
import org.openlmis.core.utils.Constants;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowToast;

import roboguice.RoboGuice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

@RunWith(LMISTestRunner.class)
public class StockMovementActivityTest {

    private StockMovementActivity stockMovementActivity;
    private StockMovementPresenter mockedPresenter;
    private StockCard stockCard;

    @Before
    public void setUp() throws Exception {
        mockedPresenter = mock(StockMovementPresenter.class);

        Product product = new ProductBuilder().setPrimaryName("Lamivudina 150mg").setCode("08S40").setStrength("10mg").setType("VIA").build();
        stockCard = new StockCardBuilder()
                .setProduct(product)
                .setStockOnHand(100)
                .setExpireDates("11/11/2011")
                .build();

        when(mockedPresenter.getStockCard()).thenReturn(stockCard);

        RoboGuice.overrideApplicationInjector(RuntimeEnvironment.application, new AbstractModule() {
            @Override
            protected void configure() {
                bind(StockMovementPresenter.class).toInstance(mockedPresenter);
            }
        });

        Intent intent = new Intent()
                .putExtra(Constants.PARAM_STOCK_CARD_ID, 100L)
                .putExtra(Constants.PARAM_STOCK_NAME, "Stock Name");
        stockMovementActivity = Robolectric.buildActivity(StockMovementActivity.class).withIntent(intent).create().visible().get();
    }

    @After
    public void tearDown() throws Exception {
        RoboGuice.Util.reset();
    }

    @Test
    public void shouldSetStockCardIdAndPageTitle() throws LMISException {
        verify(mockedPresenter).setStockCard(100L);

        assertThat(stockMovementActivity.getTitle()).isEqualTo("Stock Name");
    }

    @Test
    public void shouldRefreshStockCardWhenUnpackKitSuccessful() throws LMISException {
        stockMovementActivity.onActivityResult(Constants.REQUEST_UNPACK_KIT, Activity.RESULT_OK, new Intent());

        verify(mockedPresenter, times(2)).setStockCard(100L);
        verify(mockedPresenter, times(2)).loadStockMovementViewModels();
    }

    @Test
    public void shouldArchiveStockMovementWhenArchiveMenuClicked() throws Exception {

        shadowOf(stockMovementActivity).clickMenuItem(R.id.action_archive);

        verify(mockedPresenter).archiveStockCard();
        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo("Drug archived");
        assertThat(stockMovementActivity.isFinishing()).isTrue();
    }

    @Test
    public void shouldGoToHistoryMovementWhenMenuItemSelected() {
        shadowOf(stockMovementActivity).clickMenuItem(R.id.action_history);
        Intent startedIntent = ShadowApplication.getInstance().getNextStartedActivity();

        assertThat(startedIntent).isNotNull();
        assertThat(startedIntent.getComponent().getClassName()).isEqualTo(StockMovementHistoryActivity.class.getName());
        assertThat(startedIntent.getLongExtra(Constants.PARAM_STOCK_CARD_ID, 0)).isEqualTo(100L);
        assertThat(startedIntent.getStringExtra(Constants.PARAM_STOCK_NAME)).isEqualTo("Stock Name");
    }

    @Test
    public void shouldGoToUnpackKitActivity() throws Exception {
        LMISTestApp.getInstance().setFeatureToggle(R.bool.feature_unpack_btn_update_580, false);
        shadowOf(stockMovementActivity).clickMenuItem(R.id.action_unpack);
        Intent startedIntent = ShadowApplication.getInstance().getNextStartedActivity();

        assertThat(startedIntent).isNotNull();
        assertThat(startedIntent.getComponent().getClassName()).isEqualTo(UnpackKitActivity.class.getName());
        assertThat(startedIntent.getStringExtra(Constants.PARAM_KIT_CODE)).isEqualTo("08S40");
    }

    @Test
    public void shouldShowUnpackMenu() throws Exception {
        LMISTestApp.getInstance().setFeatureToggle(R.bool.feature_unpack_btn_update_580, false);

        stockMovementActivity.updateUnpackKitMenu(true);

        assertThat(shadowOf(stockMovementActivity).getOptionsMenu().findItem(R.id.action_unpack).isVisible()).isTrue();
    }

    @Test
    public void shouldHideUnpackKitMenu() throws Exception {
        LMISTestApp.getInstance().setFeatureToggle(R.bool.feature_unpack_btn_update_580, false);

        stockMovementActivity.updateUnpackKitMenu(false);

        assertThat(shadowOf(stockMovementActivity).getOptionsMenu().findItem(R.id.action_unpack).isVisible()).isFalse();
    }

    @Test
    public void shouldGoToUnpackKitActivityWhenUnpackKitClicked() throws Exception {
        LMISTestApp.getInstance().setFeatureToggle(R.bool.feature_unpack_btn_update_580, true);
        stockMovementActivity.updateUnpackKitMenu(true);
        stockMovementActivity.btnUnpack.performClick();

        Intent startedIntent = ShadowApplication.getInstance().getNextStartedActivity();

        assertThat(startedIntent).isNotNull();
        assertThat(startedIntent.getComponent().getClassName()).isEqualTo(UnpackKitActivity.class.getName());
        assertThat(startedIntent.getStringExtra(Constants.PARAM_KIT_CODE)).isEqualTo("08S40");
    }

    @Test
    public void shouldShowUnpackContainer() {
        LMISTestApp.getInstance().setFeatureToggle(R.bool.feature_unpack_btn_update_580, true);

        stockMovementActivity.updateUnpackKitMenu(false);
        assertThat(stockMovementActivity.unpackContainer.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void shouldHideUnpackContainer() {
        LMISTestApp.getInstance().setFeatureToggle(R.bool.feature_unpack_btn_update_580, true);

        stockMovementActivity.updateUnpackKitMenu(true);
        assertThat(stockMovementActivity.unpackContainer.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void shouldHideExpiryDateWhenStockCardSOHIsZero() {
        LMISTestApp.getInstance().setFeatureToggle(R.bool.feature_remove_expiry_date_when_soh_is_0_393, true);

        stockCard.setStockOnHand(0);

        stockMovementActivity.updateExpiryDateViewGroup();

        assertThat(stockMovementActivity.expireDateViewGroup.getVisibility()).isEqualTo(View.INVISIBLE);
    }

    @Test
    public void shouldUpdateArchiveMenu() throws Exception {
        MenuItem archiveMenu = shadowOf(stockMovementActivity).getOptionsMenu().findItem(R.id.action_archive);
        assertThat(archiveMenu.isVisible()).isFalse();

        stockMovementActivity.updateArchiveMenus(true);
        archiveMenu = shadowOf(stockMovementActivity).getOptionsMenu().findItem(R.id.action_archive);
        assertThat(archiveMenu.isVisible()).isTrue();
    }
}