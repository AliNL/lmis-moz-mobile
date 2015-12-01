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

import android.support.annotation.NonNull;

import com.google.inject.AbstractModule;

import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openlmis.core.LMISRepositoryUnitTest;
import org.openlmis.core.LMISTestApp;
import org.openlmis.core.LMISTestRunner;
import org.openlmis.core.exceptions.LMISException;
import org.openlmis.core.model.DraftInventory;
import org.openlmis.core.model.Product;
import org.openlmis.core.model.StockCard;
import org.openlmis.core.model.StockMovementItem;
import org.openlmis.core.model.builder.ProductBuilder;
import org.openlmis.core.model.builder.StockCardBuilder;
import org.openlmis.core.model.repository.ProductRepository;
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
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(LMISTestRunner.class)
public class InventoryPresenterTest extends LMISRepositoryUnitTest {

    private InventoryPresenter inventoryPresenter;

    StockRepository stockRepositoryMock;
    ProductRepository productRepositoryMock;
    InventoryPresenter.InventoryView view;
    private Product product;
    private StockCard stockCard;

    @Before
    public void setup() throws Exception {
        stockRepositoryMock = mock(StockRepository.class);
        productRepositoryMock = mock(ProductRepository.class);

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
    public void shouldLoadSortedStockCardList() throws LMISException {
        ((LMISTestApp)RuntimeEnvironment.application).setFeatureToggle(true);

        StockCard stockCardVIA = StockCardBuilder.buildStockCard();
        stockCardVIA.setProduct(new ProductBuilder().setPrimaryName("VIA Product").build());
        StockCard stockCardMMIA = StockCardBuilder.buildStockCard();
        stockCardMMIA.setProduct(new ProductBuilder().setPrimaryName("MMIA Product").build());
        List<StockCard> stockCards = Arrays.asList(stockCardVIA, stockCardMMIA);
        when(stockRepositoryMock.list()).thenReturn(stockCards);

        TestSubscriber<List<StockCardViewModel>> subscriber = new TestSubscriber<>();
        Observable<List<StockCardViewModel>> observable = inventoryPresenter.loadStockCardList();
        observable.subscribe(subscriber);

        subscriber.awaitTerminalEvent();

        verify(stockRepositoryMock).list();
        subscriber.assertNoErrors();

        List<StockCardViewModel> receivedStockCardViewModels = subscriber.getOnNextEvents().get(0);
        assertEquals(receivedStockCardViewModels.size(), 2);
        assertEquals(receivedStockCardViewModels.get(0).getProductName(), "MMIA Product");
    }

    @Test
    public void shouldLoadSortedMasterProductsList() throws LMISException {
        ((LMISTestApp)RuntimeEnvironment.application).setFeatureToggle(true);

        StockCard stockCardVIA = StockCardBuilder.buildStockCard();
        Product productVIA = new ProductBuilder().setPrimaryName("VIA Product").setCode("VIA Code").build();
        stockCardVIA.setProduct(productVIA);
        StockCard stockCardMMIA = StockCardBuilder.buildStockCard();
        Product productMMIA = new ProductBuilder().setPrimaryName("MMIA Product").setCode("MMIA Code").build();
        stockCardMMIA.setProduct(productMMIA);

        StockCard unknownAStockCard = StockCardBuilder.buildStockCard();
        Product productUnknownA = new ProductBuilder().setPrimaryName("A Unknown Product").setCode("A Code").build();
        unknownAStockCard.setProduct(productUnknownA);
        StockCard unknownBStockCard = StockCardBuilder.buildStockCard();
        Product productUnknownB = new ProductBuilder().setPrimaryName("B Unknown Product").setCode("B Code").build();
        unknownBStockCard.setProduct(productUnknownB);

        when(stockRepositoryMock.list()).thenReturn(Arrays.asList(stockCardVIA, stockCardMMIA));
        when(productRepositoryMock.list()).thenReturn(Arrays.asList(productMMIA, productVIA, productUnknownB, productUnknownA));

        TestSubscriber<List<StockCardViewModel>> subscriber = new TestSubscriber<>();
        Observable<List<StockCardViewModel>> observable = inventoryPresenter.loadMasterProductList();
        observable.subscribe(subscriber);

        subscriber.awaitTerminalEvent();

        subscriber.assertNoErrors();
        List<StockCardViewModel> receivedStockCardViewModels = subscriber.getOnNextEvents().get(0);
        assertEquals(receivedStockCardViewModels.size(), 2);
        assertEquals(receivedStockCardViewModels.get(0).getProductName(), "A Unknown Product");
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

        StockMovementItem item = inventoryPresenter.calculateAdjustment(model, stockCard);

        assertThat(item.getMovementType(), is(StockMovementItem.MovementType.POSITIVE_ADJUST));
        assertThat(item.getMovementQuantity(), is(20L));
        assertThat(item.getStockOnHand(), is(Long.parseLong(model.getQuantity())));
        assertThat(item.getStockCard(), is(stockCard));
    }

    @Test
    public void shouldMakeNegativeAdjustment() throws LMISException {
        StockCardViewModel model = new StockCardViewModel(stockCard);
        model.setQuantity("80");

        StockMovementItem item = inventoryPresenter.calculateAdjustment(model, stockCard);

        assertThat(item.getMovementType(), is(StockMovementItem.MovementType.NEGATIVE_ADJUST));
        assertThat(item.getMovementQuantity(), is(20L));
        assertThat(item.getStockOnHand(), is(Long.parseLong(model.getQuantity())));
        assertThat(item.getStockCard(), is(stockCard));
    }


    @Test
    public void shouldCalculateStockAdjustment() throws LMISException {
        StockCardViewModel model = new StockCardViewModel(stockCard);
        model.setQuantity("100");

        StockMovementItem item = inventoryPresenter.calculateAdjustment(model, stockCard);

        assertThat(item.getMovementType(), is(StockMovementItem.MovementType.PHYSICAL_INVENTORY));
        assertThat(item.getMovementQuantity(), is(0L));
        assertThat(item.getStockOnHand(), is(Long.parseLong(model.getQuantity())));
        assertThat(item.getStockCard(), is(stockCard));
    }

    @Test
    public void shouldRestoreDraftInventory() throws Exception {

        ArrayList<StockCardViewModel> stockCardViewModels = getStockCardViewModels();

        ArrayList<DraftInventory> draftInventories = new ArrayList<>();
        DraftInventory draftInventory = new DraftInventory();
        stockCard.setId(9);
        draftInventory.setStockCard(stockCard);
        draftInventory.setQuantity(20L);
        draftInventory.setExpireDates("11/10/2015");
        draftInventories.add(draftInventory);
        when(stockRepositoryMock.listDraftInventory()).thenReturn(draftInventories);

        inventoryPresenter.restoreDraftInventory(stockCardViewModels);
        assertThat(stockCardViewModels.get(0).getQuantity(), is("20"));
        assertThat(stockCardViewModels.get(0).getExpiryDates().get(0), is("11/10/2015"));
        assertThat(stockCardViewModels.get(1).getQuantity(), is("15"));
        assertThat(stockCardViewModels.get(1).getExpiryDates().get(0), is("11/02/2015"));
    }

    private ArrayList<StockCardViewModel> getStockCardViewModels() {
        ArrayList<StockCardViewModel> stockCardViewModels = new ArrayList<>();
        stockCardViewModels.add(buildStockCardWithOutDraft(9,"11",null));
        StockCardViewModel stockCardViewModelWithOutDraft = buildStockCardWithOutDraft(3, "15", "11/02/2015");
        stockCardViewModels.add(stockCardViewModelWithOutDraft);
        return stockCardViewModels;
    }

    @NonNull
    private StockCardViewModel buildStockCardWithOutDraft(int stockCardId, String quantity, String expireDate) {
        StockCardViewModel stockCardViewModelWithOutDraft = new StockCardViewModel(stockCard);
        stockCardViewModelWithOutDraft.setStockCardId(stockCardId);
        stockCardViewModelWithOutDraft.setQuantity(quantity);
        ArrayList<String> expireDates = new ArrayList<>();
        expireDates.add(expireDate);
        stockCardViewModelWithOutDraft.setExpiryDates(expireDates);
        return stockCardViewModelWithOutDraft;
    }

    @Test
    public void shouldShowSignatureDialog() throws Exception {
        when(view.validateInventory()).thenReturn(true);
        inventoryPresenter.signPhysicalInventory();
        verify(view).showSignDialog();
    }

    @Test
    public void shouldSetSignatureToViewModel() throws Exception {
        ArrayList<StockCardViewModel> stockCardViewModels = getStockCardViewModels();
        String signature = "signature";
        inventoryPresenter.doPhysicalInventory(stockCardViewModels, signature);
        assertThat(stockCardViewModels.get(0).getSignature(),is(signature));
        assertThat(stockCardViewModels.get(1).getSignature(),is(signature));
    }

    public class MyTestModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(StockRepository.class).toInstance(stockRepositoryMock);
            bind(ProductRepository.class).toInstance(productRepositoryMock);
        }
    }
}
