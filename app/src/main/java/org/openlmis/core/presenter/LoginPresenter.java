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


import com.google.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.openlmis.core.LMISApp;
import org.openlmis.core.R;
import org.openlmis.core.manager.SharedPreferenceMgr;
import org.openlmis.core.manager.UserInfoMgr;
import org.openlmis.core.model.User;
import org.openlmis.core.model.repository.UserRepository;
import org.openlmis.core.model.repository.UserRepository.NewCallback;
import org.openlmis.core.service.SyncBackManager;
import org.openlmis.core.service.SyncBackManager.SyncProgress;
import org.openlmis.core.service.SyncManager;
import org.openlmis.core.utils.ToastUtil;
import org.openlmis.core.view.BaseView;

import rx.Subscriber;

public class LoginPresenter implements Presenter {

    LoginView view;

    boolean shouldShowSyncedSuccessMsg = false;

    @Inject
    UserRepository userRepository;

    @Inject
    SyncManager syncManager;

    @Inject
    SyncBackManager syncBackManager;
    private boolean hasGoneToNextPage;

    @Override
    public void onStart() {

    }

    @Override
    public void onStop() {

    }

    @Override
    public void attachView(BaseView v) {
        this.view = (LoginView) v;
    }

    public void startLogin(String userName, String password) {

        if (StringUtils.EMPTY.equals(userName.trim())) {
            view.showUserNameEmpty();
            return;
        }
        if (StringUtils.EMPTY.equals(password)) {
            view.showPasswordEmpty();
            return;
        }
        view.loading();

        User user = new User(userName.trim(), password);
        if (LMISApp.getInstance().isConnectionAvailable()) {
            authorizeAndLoginUserRemote(user);
        } else {
            authorizeAndLoginUserLocal(user);
        }
    }

    private void authorizeAndLoginUserLocal(User user) {
        User localUser = userRepository.getUserFromLocal(user);

        if (localUser == null) {
            onLoginFailed();
            return;
        }

        user = localUser;
        UserInfoMgr.getInstance().setUser(user);

        if (!SharedPreferenceMgr.getInstance().hasGetProducts()) {
            view.loaded();
            ToastUtil.show(R.string.msg_sync_products_list_failed);
            return;
        }

        if (!SharedPreferenceMgr.getInstance().isLastMonthStockDataSynced() && LMISApp.getInstance().getFeatureToggleFor(R.bool.feature_sync_back_stock_movement_273)) {
            view.loaded();
            ToastUtil.show(R.string.msg_sync_stockmovement_failed);
            return;
        }
        if (!SharedPreferenceMgr.getInstance().isRequisitionDataSynced()) {
            view.loaded();
            ToastUtil.show(R.string.msg_sync_requisition_failed);
            return;
        }

        goToNextPage();
    }

    private void authorizeAndLoginUserRemote(final User user) {
        userRepository.authorizeUser(user, new NewCallback<User>() {
            @Override
            public void success(User remoteUser) {
                remoteUser.setUsername(user.getUsername());
                remoteUser.setPassword(user.getPassword());

                onLoginSuccess(remoteUser);
            }

            @Override
            public void failure(String error) {
                onLoginFailed();
            }

            @Override
            public void timeout(String error) {
                authorizeAndLoginUserLocal(user);
            }
        });
    }

    private void saveUserToLocalDatabase(User user) {
        userRepository.save(user);
    }

    protected void onLoginSuccess(User user) {
        syncManager.createSyncAccount(user);
        syncManager.kickOff();

        saveUserToLocalDatabase(user);
        UserInfoMgr.getInstance().setUser(user);
        view.clearErrorAlerts();

        checkSyncServerData();
    }

    public void onLoginFailed() {
        view.loaded();
        view.showInvalidAlert();
        view.clearPassword();
    }

    private void checkSyncServerData() {
        syncBackManager.syncBackServerData(getSyncSubscriber());
    }

    protected Subscriber<SyncProgress> getSyncSubscriber() {
        return new Subscriber<SyncProgress>() {
            @Override
            public void onCompleted() {
                if (!hasGoneToNextPage) {
                    goToNextPage();
                }
            }

            @Override
            public void onError(Throwable e) {
                ToastUtil.show(e.getMessage());
                view.loaded();
            }

            @Override
            public void onNext(SyncProgress progress) {
                switch (progress) {
                    case SyncingProduct:
                    case SyncingStockCardsLastMonth:
                    case SyncingRequisition:
                        view.loading(LMISApp.getInstance().getString(progress.getMessageCode()));
                        break;

                    case ProductSynced:
                        view.loaded();
                        break;
                    case StockCardsLastMonthSynced:
                        shouldShowSyncedSuccessMsg = true;
                        view.loaded();
                        break;
                    case RequisitionSynced:
                        goToNextPage();
                        break;
                }
            }
        };
    }

    private void goToNextPage() {
        view.loaded();

        if (view.needInitInventory()) {
            view.goToInitInventory();
        } else {
            if (shouldShowSyncedSuccessMsg) {
                ToastUtil.showLongTimeAsOfficialWay(R.string.msg_initial_sync_success);
            }
            view.goToHomePage();
        }
        hasGoneToNextPage = true;
    }

    public interface LoginView extends BaseView {

        void clearPassword();

        void goToHomePage();

        void goToInitInventory();

        boolean needInitInventory();

        void showInvalidAlert();

        void showUserNameEmpty();

        void showPasswordEmpty();

        void clearErrorAlerts();
    }
}
