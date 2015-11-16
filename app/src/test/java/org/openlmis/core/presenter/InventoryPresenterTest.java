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

package org.openlmis.core.presenter;

import com.google.inject.AbstractModule;

import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openlmis.core.LMISRepositoryUnitTest;
import org.openlmis.core.LMISTestRunner;
import org.openlmis.core.exceptions.LMISException;
import org.openlmis.core.model.DraftInventory;
import org.openlmis.core.model.Product;
import org.openlmis.core.model.StockCard;
import org.openlmis.core.model.builder.StockCardBuilder;
import org.openlmis.core.model.StockMovementItem;
import org.openlmis.core.model.repository.StockRepository;
import org.openlmis.core.view.viewmodel.StockCardViewModel;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import roboguice.RoboGuice;
import rx.Observable;
import rx.Scheduler;
import rx.android.plugins.RxAndroidPlugins;
import rx.android.plugins.RxAndroidSchedulersHook;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(LMISTestRunner.class)
public class InventoryPresenterTest extends LMISRepositoryUnitTest {

    private InventoryPresenter inventoryPresenter;

    StockRepository stockRepositoryMock;
    InventoryPresenter.InventoryView view;
    private Product product;
    private StockCard stockCard;

    @Before
    public void setup() throws Exception {
        stockRepositoryMock = mock(StockRepository.class);

        view = mock(InventoryPresenter.InventoryView.class);
        RoboGuice.overrideApplicationInjector(RuntimeEnvironment.application, new MyTestModule());

        inventoryPresenter = RoboGuice.getInjector(RuntimeEnvironment.application).getInstance(InventoryPresenter.class);
        inventoryPresenter.attachView(view);

        product = new Product();
        product.setPrimaryName("Test Product");
        product.setCode("ABC");

        stockCard = new StockCard();
        stockCard.setStockOnHand(100);
        stockCard.setProduct(product);
        stockCard.setExpireDates(StringUtils.EMPTY);

        RxAndroidPlugins.getInstance().reset();
        RxAndroidPlugins.getInstance().registerSchedulersHook(new RxAndroidSchedulersHook() {
            @Override
            public Scheduler getMainThreadScheduler() {
                return Schedulers.immediate();
            }
        });
    }

    @After
    public void tearDown() {
        RoboGuice.Util.reset();
    }

    @Test
    public void shouldLoadStockCardList() throws LMISException {
        StockCard stockCard1 = StockCardBuilder.buildStockCard();
        StockCard stockCard2 = StockCardBuilder.buildStockCard();
        List<StockCard> stockCards = Arrays.asList(stockCard1, stockCard2);
        when(stockRepositoryMock.list()).thenReturn(stockCards);

        TestSubscriber<List<StockCardViewModel>> subscriber = new TestSubscriber<>();
        Observable<List<StockCardViewModel>> observable = inventoryPresenter.loadStockCardList();
        observable.subscribe(subscriber);

        subscriber.awaitTerminalEvent();

        verify(stockRepositoryMock).list();
        subscriber.assertNoErrors();
        subscriber.assertValue(Arrays.asList(new StockCardViewModel(stockCard1), new StockCardViewModel(stockCard2)));
    }

    @Test
    public void shouldInitStockCardAndCreateAInitInventoryMovementItem() throws LMISException {
        StockCardViewModel model = new StockCardViewModel(product);
        model.setChecked(true);
        model.setQuantity("100");
        StockCardViewModel model2 = new StockCardViewModel(product);
        model2.setChecked(false);
        model2.setQuantity("200");

        List<StockCardViewModel> stockCardViewModelList = new ArrayList<>();
        stockCardViewModelList.add(model);
        stockCardViewModelList.add(model2);

        inventoryPresenter.initStockCards(stockCardViewModelList);

        verify(stockRepositoryMock, times(1)).initStockCard(any(StockCard.class));
    }

    @Test
    public void shouldGoToMainPageWhenOnNextCalled() {
        inventoryPresenter.nextMainPageAction.call(null);

        verify(view).loaded();
        verify(view).goToMainPage();
    }

    @Test
    public void shouldShowErrorWhenOnErrorCalled() {
        String errorMessage = "This is throwable error";
        inventoryPresenter.errorAction.call(new Throwable(errorMessage));

        verify(view).loaded();
        verify(view).showErrorMessage(errorMessage);
    }

    @Test
    public void shouldMakePositiveAdjustment() throws LMISException {

        StockCardViewModel model = new StockCardViewModel(stockCard);
        model.setQuantity("120");

        StockMovementItem item = inventoryPresenter.calculateAdjustment(model);

        assertThat(item.getMovementType(), is(StockMovementItem.MovementType.POSITIVE_ADJUST));
        assertThat(item.getMovementQuantity(), is(20L));
    }

    @Test
    public void shouldMakeNegativeAdjustment() throws LMISException {
        StockCardViewModel model = new StockCardViewModel(stockCard);
        model.setQuantity("80");

        StockMovementItem item = inventoryPresenter.calculateAdjustment(model);

        assertThat(item.getMovementType(), is(StockMovementItem.MovementType.NEGATIVE_ADJUST));
        assertThat(item.getMovementQuantity(), is(20L));
    }


    @Test
    public void shouldCalculateStockAdjustment() throws LMISException {
        StockCardViewModel model = new StockCardViewModel(stockCard);
        model.setQuantity("100");

        StockMovementItem item = inventoryPresenter.calculateAdjustment(model);

        assertThat(item.getMovementType(), is(StockMovementItem.MovementType.PHYSICAL_INVENTORY));
        assertThat(item.getMovementQuantity(), is(0L));
    }

    @Test
    public void shouldRestoreDraftInventory() throws Exception {
        StockCard stockCard = StockCardBuilder.buildStockCard();
        stockCard.setId(9);

        ArrayList<StockCardViewModel> stockCardViewModels = new ArrayList<>();
        StockCardViewModel build = new StockCardViewModel(stockCard);
        build.setQuantity("11");
        build.setExpiryDates(new ArrayList<String>());
        build.setStockCardId(stockCard.getId());
        stockCardViewModels.add(build);

        StockCardViewModel build2 = new StockCardViewModel(stockCard);
        build2.setStockCardId(3);
        build2.setQuantity("15");
        ArrayList<String> expireDates = new ArrayList<>();
        expireDates.add("11/02/2015");
        build2.setExpiryDates(expireDates);
        stockCardViewModels.add(build2);

        ArrayList<DraftInventory> draftInventories = new ArrayList<>();
        DraftInventory draftInventory = new DraftInventory();
        draftInventory.setStockCard(stockCard);
        draftInventory.setQuantity(20L);
        draftInventory.setExpireDates("11/10/2015");
        draftInventories.add(draftInventory);
        when(stockRepositoryMock.listDraftInventory()).thenReturn(draftInventories);

        inventoryPresenter.restoreDraftInventory(stockCardViewModels);
        assertThat(stockCardViewModels.get(0).getQuantity(), is("20"));
        assertThat(stockCardViewModels.get(0).getExpiryDates().get(0),is("11/10/2015"));
        assertThat(stockCardViewModels.get(1).getQuantity(), is("15"));
        assertThat(stockCardViewModels.get(1).getExpiryDates().get(0), is("11/02/2015"));
    }

    public class MyTestModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(StockRepository.class).toInstance(stockRepositoryMock);
        }
    }
}
