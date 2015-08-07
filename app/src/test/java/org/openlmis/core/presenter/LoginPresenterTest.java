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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.openlmis.core.LMISTestRunner;
import org.openlmis.core.model.User;
import org.openlmis.core.model.repository.ProductRepository;
import org.openlmis.core.model.repository.UserRepository;
import org.openlmis.core.service.SyncManager;
import org.openlmis.core.view.activity.LoginActivity;
import org.robolectric.Robolectric;

import retrofit.Callback;
import roboguice.RoboGuice;
import rx.Observable;
import rx.Observer;
import rx.functions.Func1;
import rx.observers.TestObserver;
import rx.observers.TestSubscriber;
import rx.schedulers.TestScheduler;
import rx.subjects.TestSubject;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(LMISTestRunner.class)
public class LoginPresenterTest {

    UserRepository userRepository;
    LoginActivity mockActivity;
    LoginPresenter presenter;
    ProductRepository.ProductsResponse mockProductsResponse;
    SyncManager syncManager;

    @Captor
    private ArgumentCaptor<Callback<UserRepository.UserResponse>> loginCB;
    @Captor
    private ArgumentCaptor<Observer<Void>> getProductsCB;

    @Before
    public void setup() {
        userRepository = mock(UserRepository.class);
        mockActivity = mock(LoginActivity.class);
        mockProductsResponse = mock(ProductRepository.ProductsResponse.class);
        syncManager = mock(SyncManager.class);

        RoboGuice.overrideApplicationInjector(Robolectric.application, new MyTestModule());
        MockitoAnnotations.initMocks(this);

        presenter = RoboGuice.getInjector(Robolectric.application).getInstance(LoginPresenter.class);
        presenter.attachView(mockActivity);
    }

    @After
    public void teardown() {
        RoboGuice.Util.reset();
    }


    @Test
    public void shouldSaveUserToLocalDBWhenSuccess() throws InterruptedException {
        when(mockActivity.isConnectionAvailable()).thenReturn(true);
        presenter.startLogin("user", "password");

        verify(mockActivity).startLoading();

        verify(userRepository).authorizeUser(any(User.class), loginCB.capture());
        UserRepository.UserResponse userResponse = userRepository.new UserResponse();
        userResponse.setUserInformation(new User("user", "password"));

        loginCB.getValue().success(userResponse, null);
        verify(userRepository).save(any(User.class));
    }

    @Test
    public void shouldGetProductsWhenLoginSuccFromNet() {
        when(mockActivity.isConnectionAvailable()).thenReturn(true);
        when(mockActivity.hasGetProducts()).thenReturn(false);

        presenter.startLogin("user", "password");
        verify(userRepository).authorizeUser(any(User.class), loginCB.capture());
        UserRepository.UserResponse userResponse = userRepository.new UserResponse();
        userResponse.setUserInformation(new User("user", "password"));

        loginCB.getValue().success(userResponse, null);

        verify(syncManager).syncProductsWithProgramAsync(getProductsCB.capture());
        getProductsCB.getValue().onCompleted();
    }


    @Test
    public void shouldGoToInventoryPageIfGetProductsSuccess() throws InterruptedException {
        when(mockActivity.needInitInventory()).thenReturn(true);
        when(mockActivity.isConnectionAvailable()).thenReturn(true);
        when(mockActivity.hasGetProducts()).thenReturn(false);


        presenter.startLogin("user", "password");
        verify(userRepository).authorizeUser(any(User.class), loginCB.capture());
        UserRepository.UserResponse userResponse = userRepository.new UserResponse();
        userResponse.setUserInformation(new User("user", "password"));

        loginCB.getValue().success(userResponse, null);

        verify(syncManager).syncProductsWithProgramAsync(getProductsCB.capture());
        getProductsCB.getValue().onCompleted();

        verify(mockActivity).stopLoading();
        verify(mockActivity).goToInitInventory();
    }

    @Test
    public void shouldDoOfflineLoginWhenNoConnection() {
        when(mockActivity.isConnectionAvailable()).thenReturn(false);
        presenter.startLogin("user", "password");
        verify(userRepository).getUserForLocalDatabase(any(User.class));
    }


    public class MyTestModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(UserRepository.class).toInstance(userRepository);
            bind(SyncManager.class).toInstance(syncManager);
        }
    }
}
