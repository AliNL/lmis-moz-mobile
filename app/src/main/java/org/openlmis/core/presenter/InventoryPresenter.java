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
import android.support.annotation.Nullable;

import com.google.inject.Inject;

import org.openlmis.core.exceptions.LMISException;
import org.openlmis.core.manager.MovementReasonManager;
import org.openlmis.core.manager.SharedPreferenceMgr;
import org.openlmis.core.model.DraftInventory;
import org.openlmis.core.model.Inventory;
import org.openlmis.core.model.Product;
import org.openlmis.core.model.StockCard;
import org.openlmis.core.model.StockMovementItem;
import org.openlmis.core.model.repository.InventoryRepository;
import org.openlmis.core.model.repository.ProductRepository;
import org.openlmis.core.model.repository.StockRepository;
import org.openlmis.core.utils.DateUtil;
import org.openlmis.core.view.BaseView;
import org.openlmis.core.view.viewmodel.InventoryViewModel;
import org.roboguice.shaded.goole.common.base.Function;
import org.roboguice.shaded.goole.common.base.Predicate;
import org.roboguice.shaded.goole.common.collect.FluentIterable;

import java.util.Date;
import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

import static org.openlmis.core.model.Product.IsKit;
import static org.roboguice.shaded.goole.common.collect.FluentIterable.from;

public class InventoryPresenter extends Presenter {

    @Inject
    ProductRepository productRepository;

    @Inject
    StockRepository stockRepository;

    @Inject
    InventoryRepository inventoryRepository;

    @Inject
    SharedPreferenceMgr sharedPreferenceMgr;

    InventoryView view;

    @Override
    public void attachView(BaseView v) {
        view = (InventoryView) v;
    }

    public Observable<List<InventoryViewModel>> loadInventory() {

        return Observable.create(new Observable.OnSubscribe<List<InventoryViewModel>>() {
            @Override
            public void call(final Subscriber<? super List<InventoryViewModel>> subscriber) {
                try {
                    List<Product> inventoryProducts = getValidProductsForInventory();

                    List<InventoryViewModel> availableStockCardsForAddNewDrug = from(inventoryProducts)
                            .transform(new Function<Product, InventoryViewModel>() {
                                @Override
                                public InventoryViewModel apply(Product product) {
                                    return convertProductToStockCardViewModel(product);
                                }
                            }).toList();
                    subscriber.onNext(availableStockCardsForAddNewDrug);
                    subscriber.onCompleted();
                } catch (LMISException e) {
                    e.reportToFabric();
                    subscriber.onError(e);
                }
            }
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io());
    }

    @NonNull
    private List<Product> getValidProductsForInventory() throws LMISException {
        List<Product> activeProducts = productRepository.listActiveProducts(IsKit.No);
        final List<Product> productsWithStockCards = getProductsThatHaveStockCards();

        return FluentIterable.from(activeProducts).filter(new Predicate<Product>() {
            @Override
            public boolean apply(@Nullable Product product) {
                return product.isArchived() || !productsWithStockCards.contains(product);
            }
        }).toList();
    }

    @Nullable
    private InventoryViewModel convertProductToStockCardViewModel(Product product) {
        try {
            InventoryViewModel viewModel;
            if (product.isArchived()) {
                viewModel = new InventoryViewModel(stockRepository.queryStockCardByProductId(product.getId()));
            } else {
                viewModel = new InventoryViewModel(product);
            }
            viewModel.setChecked(false);
            return viewModel;
        } catch (LMISException e) {
            e.reportToFabric();
        }
        return null;
    }

    private List<Product> getProductsThatHaveStockCards() throws LMISException {
        return from(stockRepository.list()).transform(new Function<StockCard, Product>() {
            @Override
            public Product apply(StockCard stockCard) {
                return stockCard.getProduct();
            }
        }).toList();
    }

    public Observable<List<InventoryViewModel>> loadPhysicalInventory() {
        return Observable.create(new Observable.OnSubscribe<List<InventoryViewModel>>() {
            @Override
            public void call(Subscriber<? super List<InventoryViewModel>> subscriber) {
                try {
                    List<StockCard> validStockCardsForPhysicalInventory = getValidStockCardsForPhysicalInventory();
                    List<InventoryViewModel> inventoryViewModels = convertStockCardsToStockCardViewModels(validStockCardsForPhysicalInventory);

                    restoreDraftInventory(inventoryViewModels);
                    subscriber.onNext(inventoryViewModels);
                    subscriber.onCompleted();
                } catch (LMISException e) {
                    subscriber.onError(e);
                }
            }
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io());
    }

    private List<InventoryViewModel> convertStockCardsToStockCardViewModels(List<StockCard> validStockCardsForPhysicalInventory) {
        return FluentIterable.from(validStockCardsForPhysicalInventory).transform(new Function<StockCard, InventoryViewModel>() {
            @Override
            public InventoryViewModel apply(StockCard stockCard) {
                return new InventoryViewModel(stockCard);
            }
        }).toList();
    }

    private List<StockCard> getValidStockCardsForPhysicalInventory() throws LMISException {
        return from(stockRepository.list()).filter(new Predicate<StockCard>() {
            @Override
            public boolean apply(StockCard stockCard) {
                return !stockCard.getProduct().isKit() && stockCard.getProduct().isActive() && !stockCard.getProduct().isArchived();
            }
        }).toList();
    }

    protected void restoreDraftInventory(List<InventoryViewModel> inventoryViewModels) throws LMISException {
        List<DraftInventory> draftList = stockRepository.listDraftInventory();

        for (InventoryViewModel model : inventoryViewModels) {
            for (DraftInventory draftInventory : draftList) {
                if (model.getStockCardId() == draftInventory.getStockCard().getId()) {
                    model.initExpiryDates(draftInventory.getExpireDates());
                    model.setQuantity(formatQuantity(draftInventory.getQuantity()));
                }
            }
        }
    }

    private String formatQuantity(Long quantity) {
        return quantity == null ? "" : quantity.toString();
    }

    protected void initStockCards(List<InventoryViewModel> list) {

        from(list).filter(new Predicate<InventoryViewModel>() {
            @Override
            public boolean apply(InventoryViewModel inventoryViewModel) {
                return inventoryViewModel.isChecked();
            }
        }).transform(new Function<InventoryViewModel, StockCard>() {
            @Override
            public StockCard apply(InventoryViewModel inventoryViewModel) {
                return initStockCard(inventoryViewModel);
            }
        }).toList();
    }

    private StockCard initStockCard(InventoryViewModel model) {
        try {
            boolean isArchivedStockCard = model.getStockCard() != null;

            StockCard stockCard = isArchivedStockCard ? model.getStockCard() : new StockCard();
            stockCard.setStockOnHand(Long.parseLong(model.getQuantity()));
            stockCard.setProduct(model.getProduct());
            stockCard.getProduct().setArchived(false);

            if (stockCard.getStockOnHand() != 0) {
                stockCard.setExpireDates(DateUtil.formatExpiryDateString(model.getExpiryDates()));
            } else {
                stockCard.setExpireDates("");
            }

            stockRepository.createOrUpdateStockCardWithStockMovement(stockCard);
            return stockCard;
        } catch (LMISException e) {
            e.reportToFabric();
        }
        return null;
    }


    protected StockMovementItem calculateAdjustment(InventoryViewModel model, StockCard stockCard) {
        long inventory = Long.parseLong(model.getQuantity());
        long stockOnHand = model.getStockOnHand();

        StockMovementItem item = new StockMovementItem();
        item.setSignature(model.getSignature());
        item.setMovementDate(new Date());
        item.setMovementQuantity(Math.abs(inventory - stockOnHand));
        item.setStockOnHand(inventory);
        item.setStockCard(stockCard);

        if (inventory > stockOnHand) {
            item.setReason(MovementReasonManager.INVENTORY_POSITIVE);
            item.setMovementType(StockMovementItem.MovementType.POSITIVE_ADJUST);
        } else if (inventory < stockOnHand) {
            item.setReason(MovementReasonManager.INVENTORY_NEGATIVE);
            item.setMovementType(StockMovementItem.MovementType.NEGATIVE_ADJUST);
        } else {
            item.setReason(MovementReasonManager.INVENTORY);
            item.setMovementType(StockMovementItem.MovementType.PHYSICAL_INVENTORY);
        }
        return item;
    }

    public void savePhysicalInventory(List<InventoryViewModel> list) {
        view.loading();
        Subscription subscription = saveDraftInventoryObservable(list).subscribe(nextMainPageAction, errorAction);
        subscriptions.add(subscription);
    }

    private Observable<Object> saveDraftInventoryObservable(final List<InventoryViewModel> list) {
        return Observable.create(new Observable.OnSubscribe<Object>() {
            @Override
            public void call(Subscriber<? super Object> subscriber) {
                try {
                    for (InventoryViewModel model : list) {
                        stockRepository.saveDraftInventory(model.parseDraftInventory());
                    }
                    subscriber.onNext(null);
                    subscriber.onCompleted();
                } catch (LMISException e) {
                    subscriber.onError(e);
                    e.reportToFabric();
                }
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    public void signPhysicalInventory() {
        if (view.validateInventory()) {
            view.showSignDialog();
        }
    }

    public void doPhysicalInventory(List<InventoryViewModel> list, final String sign) {
        view.loading();

        for (InventoryViewModel viewModel : list) {
            viewModel.setSignature(sign);
        }
        Subscription subscription = stockMovementObservable(list).subscribe(nextMainPageAction, errorAction);
        subscriptions.add(subscription);
    }

    protected Observable<Object> stockMovementObservable(final List<InventoryViewModel> list) {
        return Observable.create(new Observable.OnSubscribe<Object>() {
            @Override
            public void call(Subscriber<? super Object> subscriber) {
                try {
                    for (InventoryViewModel model : list) {
                        StockCard stockCard = model.getStockCard();
                        stockCard.setStockOnHand(Long.parseLong(model.getQuantity()));

                        if (stockCard.getStockOnHand() == 0) {
                            stockCard.setExpireDates("");
                        }

                        stockRepository.addStockMovementAndUpdateStockCard(calculateAdjustment(model, stockCard));
                    }
                    stockRepository.clearDraftInventory();
                    sharedPreferenceMgr.setLatestPhysicInventoryTime(DateUtil.formatDate(new Date(), DateUtil.DATE_TIME_FORMAT));
                    saveInventoryDate();

                    subscriber.onNext(null);
                    subscriber.onCompleted();
                } catch (LMISException e) {
                    subscriber.onError(e);
                    e.printStackTrace();
                }
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    private void saveInventoryDate() {
        inventoryRepository.save(new Inventory());
    }

    protected Action1<Object> nextMainPageAction = new Action1<Object>() {
        @Override
        public void call(Object o) {
            view.loaded();
            view.goToParentPage();
        }
    };

    protected Action1<Throwable> errorAction = new Action1<Throwable>() {
        @Override
        public void call(Throwable throwable) {
            view.loaded();
            view.showErrorMessage(throwable.getMessage());
        }
    };

    public void doInitialInventory(final List<InventoryViewModel> list) {
        if (view.validateInventory()) {
            view.loading();
            Subscription subscription = initStockCardObservable(list).subscribe(nextMainPageAction);
            subscriptions.add(subscription);
        }
    }

    protected Observable<Object> initStockCardObservable(final List<InventoryViewModel> list) {
        return Observable.create(new Observable.OnSubscribe<Object>() {
            @Override
            public void call(Subscriber<? super Object> subscriber) {
                initStockCards(list);
                subscriber.onNext(null);
                subscriber.onCompleted();
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    public interface InventoryView extends BaseView {
        void goToParentPage();

        boolean validateInventory();

        void showErrorMessage(String msg);

        void showSignDialog();
    }
}
