package org.openlmis.core.presenter;

import org.openlmis.core.LMISApp;
import org.openlmis.core.R;
import org.openlmis.core.exceptions.LMISException;
import org.openlmis.core.manager.MovementReasonManager;
import org.openlmis.core.model.DraftInventory;
import org.openlmis.core.model.DraftLotItem;
import org.openlmis.core.model.Inventory;
import org.openlmis.core.model.LotOnHand;
import org.openlmis.core.model.StockCard;
import org.openlmis.core.model.StockMovementItem;
import org.openlmis.core.utils.DateUtil;
import org.openlmis.core.view.viewmodel.InventoryViewModel;
import org.openlmis.core.view.viewmodel.LotMovementViewModel;
import org.openlmis.core.view.viewmodel.PhysicalInventoryViewModel;
import org.roboguice.shaded.goole.common.base.Function;
import org.roboguice.shaded.goole.common.base.Predicate;
import org.roboguice.shaded.goole.common.collect.FluentIterable;

import java.util.Comparator;
import java.util.Date;
import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import static org.roboguice.shaded.goole.common.collect.FluentIterable.from;

public class PhysicalInventoryPresenter extends InventoryPresenter {
    @Override
    public Observable<List<InventoryViewModel>> loadInventory() {
        return Observable.create(new Observable.OnSubscribe<List<InventoryViewModel>>() {
            @Override
            public void call(Subscriber<? super List<InventoryViewModel>> subscriber) {
                try {
                    List<StockCard> validStockCardsForPhysicalInventory = getValidStockCardsForPhysicalInventory();
                    inventoryViewModelList.addAll(convertStockCardsToStockCardViewModels(validStockCardsForPhysicalInventory));
                    restoreDraftInventory();
                    subscriber.onNext(inventoryViewModelList);
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
                InventoryViewModel inventoryViewModel = new PhysicalInventoryViewModel(stockCard);
                if (LMISApp.getInstance().getFeatureToggleFor(R.bool.feature_lot_management)) {
                    setExistingLotViewModels(inventoryViewModel);
                }
                return inventoryViewModel;
            }
        }).toList();
    }

    protected List<StockCard> getValidStockCardsForPhysicalInventory() throws LMISException {
        return from(stockRepository.list()).filter(new Predicate<StockCard>() {
            @Override
            public boolean apply(StockCard stockCard) {
                return !stockCard.getProduct().isKit() && stockCard.getProduct().isActive() && !stockCard.getProduct().isArchived();
            }
        }).toList();
    }

    protected void restoreDraftInventory() throws LMISException {
        List<DraftInventory> draftList = inventoryRepository.queryAllDraft();

        for (InventoryViewModel viewModel : inventoryViewModelList) {
            for (DraftInventory draftInventory : draftList) {
                if (viewModel.getStockCardId() == draftInventory.getStockCard().getId()) {
                    ((PhysicalInventoryViewModel) viewModel).setDraftInventory(draftInventory);
                    viewModel.initExpiryDates(draftInventory.getExpireDates());
                    viewModel.setQuantity(formatQuantity(draftInventory.getQuantity()));
                    if (LMISApp.getInstance().getFeatureToggleFor(R.bool.feature_lot_management)) {
                        populateLotMovementModelWithDraftLotItem(viewModel, draftInventory);
                    }
                }
            }
        }
    }

    private void populateLotMovementModelWithDraftLotItem(InventoryViewModel inventoryViewModel, DraftInventory draftInventory) {
        List<LotMovementViewModel> existingLotMovementViewModelList = inventoryViewModel.getExistingLotMovementViewModelList();
        List<LotMovementViewModel> newAddedLotMovementVieModelList = inventoryViewModel.getLotMovementViewModelList();
        for (DraftLotItem draftLotItem : draftInventory.getDraftLotItemListWrapper()) {
            if (draftLotItem.isNewAdded()) {
                if (isNotInExistingLots(draftLotItem, existingLotMovementViewModelList, newAddedLotMovementVieModelList)) {
                    LotMovementViewModel newLotMovementViewModel = new LotMovementViewModel();
                    newLotMovementViewModel.setQuantity(formatQuantity(draftLotItem.getQuantity()));
                    newLotMovementViewModel.setLotNumber(draftLotItem.getLotNumber());
                    newLotMovementViewModel.setExpiryDate(DateUtil.formatDate(draftLotItem.getExpirationDate(), DateUtil.DATE_FORMAT_ONLY_MONTH_AND_YEAR));
                    newAddedLotMovementVieModelList.add(newLotMovementViewModel);
                }
            } else {
                for (LotMovementViewModel existingLotMovementViewModel : existingLotMovementViewModelList) {
                    if (draftLotItem.getLotNumber().equals(existingLotMovementViewModel.getLotNumber())) {
                        existingLotMovementViewModel.setQuantity(formatQuantity(draftLotItem.getQuantity()));
                    }
                }
            }
        }
    }

    private boolean isNotInExistingLots(DraftLotItem draftLotItem, List<LotMovementViewModel> existingLotMovementViewModelList, List<LotMovementViewModel> newAddedLotMovementVieModelList) {
        for (LotMovementViewModel lotMovementViewModel : existingLotMovementViewModelList) {
            if (draftLotItem.getLotNumber().toUpperCase().equals(lotMovementViewModel.getLotNumber().toUpperCase())) {
                return false;
            }
        }

        for (LotMovementViewModel lotMovementViewModel : newAddedLotMovementVieModelList) {
            if (draftLotItem.getLotNumber().toUpperCase().equals(lotMovementViewModel.getLotNumber().toUpperCase())) {
                return false;
            }
        }
        return true;
    }

    private String formatQuantity(Long quantity) {
        return quantity == null ? "" : quantity.toString();
    }

    protected StockMovementItem calculateAdjustment(InventoryViewModel model, StockCard stockCard) {
        Long inventory;
        if (LMISApp.getInstance().getFeatureToggleFor(R.bool.feature_lot_management)) {
            inventory = model.getLotListQuantityTotalAmount();
        } else {
            inventory = Long.parseLong(model.getQuantity());
        }
        long stockOnHand = model.getStockOnHand();
        StockMovementItem item = new StockMovementItem();
        item.setSignature(model.getSignature());
        item.setMovementDate(new Date());
        item.setMovementQuantity(Math.abs(inventory - stockOnHand));
        item.setStockOnHand(inventory);
        item.setStockCard(stockCard);

        if (inventory > stockOnHand) {
            item.setReason(MovementReasonManager.INVENTORY_POSITIVE);
            item.setMovementType(MovementReasonManager.MovementType.POSITIVE_ADJUST);
        } else if (inventory < stockOnHand) {
            item.setReason(MovementReasonManager.INVENTORY_NEGATIVE);
            item.setMovementType(MovementReasonManager.MovementType.NEGATIVE_ADJUST);
        } else {
            item.setReason(MovementReasonManager.INVENTORY);
            item.setMovementType(MovementReasonManager.MovementType.PHYSICAL_INVENTORY);
        }

        if (LMISApp.getInstance().getFeatureToggleFor(R.bool.feature_lot_management)) {
            item.populateLotAndResetStockOnHandOfLotAccordingPhysicalAdjustment(model.getExistingLotMovementViewModelList(), model.getLotMovementViewModelList());
        }

        return item;
    }

    public Observable<Object> saveDraftInventoryObservable() {
        return Observable.create(new Observable.OnSubscribe<Object>() {
            @Override
            public void call(Subscriber<? super Object> subscriber) {
                try {
                    inventoryRepository.clearDraft();
                    for (InventoryViewModel model : inventoryViewModelList) {
                        inventoryRepository.createDraft(new DraftInventory(model));
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

    public Observable<Object> doInventory(final String sign) {
        return Observable.create(new Observable.OnSubscribe<Object>() {
            @Override
            public void call(Subscriber<? super Object> subscriber) {
                try {
                    for (InventoryViewModel viewModel : inventoryViewModelList) {
                        viewModel.setSignature(sign);
                        StockCard stockCard = viewModel.getStockCard();
                        if (LMISApp.getInstance().getFeatureToggleFor(R.bool.feature_lot_management)) {
                            stockCard.setStockOnHand(viewModel.getLotListQuantityTotalAmount());
                        } else {
                            stockCard.setStockOnHand(Long.parseLong(viewModel.getQuantity()));
                        }

                        if (stockCard.getStockOnHand() == 0) {
                            stockCard.setExpireDates("");
                        }

                        stockRepository.addStockMovementAndUpdateStockCard(calculateAdjustment(viewModel, stockCard));
                    }
                    inventoryRepository.clearDraft();
                    sharedPreferenceMgr.setLatestPhysicInventoryTime(DateUtil.formatDate(new Date(), DateUtil.DATE_TIME_FORMAT));
                    saveInventoryDate();

                    subscriber.onNext(null);
                    subscriber.onCompleted();
                } catch (LMISException e) {
                    subscriber.onError(e);
                    e.reportToFabric();
                }
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    private void saveInventoryDate() {
        inventoryRepository.save(new Inventory());
    }

    private void setExistingLotViewModels(InventoryViewModel inventoryViewModel) {
        List<LotMovementViewModel> lotMovementViewModels = FluentIterable.from(inventoryViewModel.getStockCard().getNonEmptyLotOnHandList()).transform(new Function<LotOnHand, LotMovementViewModel>() {
            @Override
            public LotMovementViewModel apply(LotOnHand lotOnHand) {
                return new LotMovementViewModel(lotOnHand.getLot().getLotNumber(),
                        DateUtil.formatDate(lotOnHand.getLot().getExpirationDate(), DateUtil.DATE_FORMAT_ONLY_MONTH_AND_YEAR),
                        lotOnHand.getQuantityOnHand().toString(), MovementReasonManager.MovementType.RECEIVE);
            }
        }).toSortedList(new Comparator<LotMovementViewModel>() {
            @Override
            public int compare(LotMovementViewModel lot1, LotMovementViewModel lot2) {
                return DateUtil.parseString(lot1.getExpiryDate(), DateUtil.DATE_FORMAT_ONLY_MONTH_AND_YEAR).compareTo(DateUtil.parseString(lot2.getExpiryDate(), DateUtil.DATE_FORMAT_ONLY_MONTH_AND_YEAR));
            }
        });
        inventoryViewModel.setExistingLotMovementViewModelList(lotMovementViewModels);
    }
}
