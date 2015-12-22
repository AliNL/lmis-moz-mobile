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
import org.mockito.MockitoAnnotations;
import org.openlmis.core.LMISTestApp;
import org.openlmis.core.LMISTestRunner;
import org.openlmis.core.R;
import org.openlmis.core.exceptions.LMISException;
import org.openlmis.core.model.BaseInfoItem;
import org.openlmis.core.model.RegimenItem;
import org.openlmis.core.model.RnRForm;
import org.openlmis.core.model.RnRFormSignature;
import org.openlmis.core.model.repository.MMIARepository;
import org.openlmis.core.model.repository.ProgramRepository;
import org.openlmis.core.service.SyncManager;
import org.robolectric.RuntimeEnvironment;

import java.sql.SQLException;
import java.util.ArrayList;

import roboguice.RoboGuice;
import rx.Scheduler;
import rx.android.plugins.RxAndroidPlugins;
import rx.android.plugins.RxAndroidSchedulersHook;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(LMISTestRunner.class)
public class MMIARequisitionPresenterTest {

    private SyncManager syncManager;
    private MMIARequisitionPresenter presenter;
    private MMIARepository mmiaRepository;
    private ProgramRepository programRepository;
    private MMIARequisitionPresenter.MMIARequisitionView mockMMIAformView;
    private RnRForm rnRForm;

    @Before
    public void setup() throws Exception {
        mmiaRepository = mock(MMIARepository.class);
        programRepository = mock(ProgramRepository.class);
        mockMMIAformView = mock(MMIARequisitionPresenter.MMIARequisitionView.class);
        syncManager = mock(SyncManager.class);
        RoboGuice.overrideApplicationInjector(RuntimeEnvironment.application, new MyTestModule());
        MockitoAnnotations.initMocks(this);

        presenter = RoboGuice.getInjector(RuntimeEnvironment.application).getInstance(MMIARequisitionPresenter.class);
        presenter.attachView(mockMMIAformView);
        rnRForm = new RnRForm();
        rnRForm.setStatus(RnRForm.STATUS.DRAFT);

        when(mmiaRepository.initRnrForm()).thenReturn(rnRForm);
        when(mmiaRepository.getTotalPatients(rnRForm)).thenReturn(100L);

        presenter.loadDataOnNextAction.call(rnRForm);

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
    public void shouldGetInitMMIAForm() throws LMISException, SQLException {
        presenter.rnRForm = null;
        when(mmiaRepository.queryUnAuthorized()).thenReturn(null);
        presenter.getRnrForm(0);
        verify(mmiaRepository).queryUnAuthorized();
        verify(mmiaRepository).initRnrForm();
    }

    @Test
    public void shouldGetDraftMMIAForm() throws LMISException {
        presenter.rnRForm = null;
        when(mmiaRepository.queryUnAuthorized()).thenReturn(new RnRForm());
        presenter.getRnrForm(0);
        verify(mmiaRepository).queryUnAuthorized();
        verify(mmiaRepository, never()).initRnrForm();
    }

    @Test
    public void shouldCompleteMMIAIfTotalsMatch() throws Exception {
        ArrayList<RegimenItem> regimenItems = generateRegimenItems();
        ArrayList<BaseInfoItem> baseInfoItems = new ArrayList<>();

        RnRForm rnRForm = new RnRForm();

        when(mmiaRepository.initRnrForm()).thenReturn(rnRForm);
        when(mmiaRepository.getTotalPatients(rnRForm)).thenReturn(100L);
        presenter.getRnrForm(0);

        presenter.processRequisition(regimenItems, baseInfoItems, "");
        verify(mockMMIAformView, never()).showValidationAlert();
    }

    @Test
    public void shouldShowSignDialogIfTotalsMatch() throws Exception {
        ArrayList<RegimenItem> regimenItems = generateRegimenItems();
        ArrayList<BaseInfoItem> baseInfoItems = new ArrayList<>();

        when(mmiaRepository.isPeriodUnique(rnRForm)).thenReturn(true);

        presenter.processRequisition(regimenItems, baseInfoItems, "");

        verify(mockMMIAformView).showSignDialog(true);
    }

    @Test
    public void shouldCompleteFormAfterSignSuccess() throws Exception {
        ArrayList<RegimenItem> regimenItems = generateRegimenItems();
        ArrayList<BaseInfoItem> baseInfoItems = new ArrayList<>();

        RnRForm rnRForm = new RnRForm();

        when(mmiaRepository.initRnrForm()).thenReturn(rnRForm);
        when(mmiaRepository.getTotalPatients(rnRForm)).thenReturn(100L);
        presenter.getRnrForm(0);

        presenter.processRequisition(regimenItems, baseInfoItems, "");
    }

    @Test
    public void shouldNotCompleteMMIAIfTotalsMismatchAndCommentInvalid() throws Exception {
        ArrayList<RegimenItem> regimenItems = generateRegimenItems();
        ArrayList<BaseInfoItem> baseInfoItems = new ArrayList<>();
        RnRForm rnRForm = new RnRForm();
        presenter.rnRForm = rnRForm;

        when(mmiaRepository.getTotalPatients(rnRForm)).thenReturn(99L);
        presenter.getRnrForm(0);

        presenter.processRequisition(regimenItems, baseInfoItems, "1234");
        verify(mockMMIAformView).showValidationAlert();
    }

    @Test
    public void shouldShowErrorWhenLoadRnRFormOnError() {
        reset(mockMMIAformView);
        presenter.loadDataOnErrorAction.call(new Exception("I am testing the onError action"));
        verify(mockMMIAformView).loaded();
        verify(mockMMIAformView).showErrorMessage("I am testing the onError action");
    }

    @Test
    public void shouldRefreshViewWhenLoadRnRFormOnNext() {
        reset(mockMMIAformView);
        RnRForm rnRForm = new RnRForm();
        presenter.loadDataOnNextAction.call(rnRForm);

        verify(mockMMIAformView).refreshRequisitionForm(rnRForm);
        verify(mockMMIAformView).loaded();
    }

    @Test
    public void shouldQueryRnRFormWhenFormIdIsValid() throws LMISException {
        presenter.rnRForm = null;
        presenter.getRnrForm(100L);
        verify(mmiaRepository).queryRnRForm(100L);
    }

    @Test
    public void shouldUnAuthorizedWhenFormIdIsInvalid() throws Exception {
        presenter.rnRForm = null;
        presenter.getRnrForm(0);

        verify(mmiaRepository).queryUnAuthorized();
    }

    @Test
    public void shouldAuthoriseFormObservableAuthoriseForm() throws LMISException {
        RnRForm form = new RnRForm();

        presenter.rnRForm = form;

        TestSubscriber<Void> subscriber = new TestSubscriber<>();
        presenter.getAuthoriseFormObservable().subscribe(subscriber);
        subscriber.awaitTerminalEvent();

        verify(mmiaRepository).authorise(form);
    }

    @Test
    public void shouldSubmitFormWhenTheStatusIsDraft() throws LMISException {
        RnRForm form = new RnRForm();
        form.setStatus(RnRForm.STATUS.DRAFT);

        String signature = "signature";
        presenter.processSign(signature, form);
        waitObservableToExecute();

        verify(mmiaRepository).setSignature(form, signature, RnRFormSignature.TYPE.SUBMITTER);
        verify(mmiaRepository).submit(form);
        verify(mockMMIAformView).setProcessButtonName(LMISTestApp.getContext().getString(R.string.btn_complete));
        verify(mockMMIAformView).showMessageNotifyDialog();
    }

    @Test
    public void shouldAuthorizeFormWhenStatusIsSubmitted() throws LMISException {
        RnRForm form = new RnRForm();
        form.setStatus(RnRForm.STATUS.SUBMITTED);

        String signature = "signature";
        presenter.processSign(signature, form);

        waitObservableToExecute();

        verify(mmiaRepository).setSignature(form, signature, RnRFormSignature.TYPE.APPROVER);
        verify(mmiaRepository).authorise(form);
    }

    @Test
    public void shouldShowErrorMSGWhenThereWasARequisitionInTheSamePeriod() throws Exception {
        ArrayList<RegimenItem> regimenItems = generateRegimenItems();
        ArrayList<BaseInfoItem> baseInfoItems = new ArrayList<>();

        RnRForm rnRForm = new RnRForm();

        when(mmiaRepository.initRnrForm()).thenReturn(rnRForm);
        when(mmiaRepository.getTotalPatients(rnRForm)).thenReturn(100L);
        when(mmiaRepository.isPeriodUnique(any(RnRForm.class))).thenReturn(false);

        presenter.processRequisition(regimenItems, baseInfoItems, anyString());
        verify(mockMMIAformView).showErrorMessage(LMISTestApp.getContext().getResources().getString(R.string.msg_requisition_not_unique));
    }

    @Test
    public void shouldNotShowErrorMSGWhenThereWasNoARequisitionInTheSamePeriod() throws Exception {
        ArrayList<RegimenItem> regimenItems = generateRegimenItems();
        ArrayList<BaseInfoItem> baseInfoItems = new ArrayList<>();

        RnRForm rnRForm = new RnRForm();

        when(mmiaRepository.initRnrForm()).thenReturn(rnRForm);
        when(mmiaRepository.getTotalPatients(rnRForm)).thenReturn(100L);
        when(mmiaRepository.isPeriodUnique(any(RnRForm.class))).thenReturn(true);

        presenter.processRequisition(regimenItems, baseInfoItems, anyString());
        verify(mockMMIAformView, never()).showErrorMessage(anyString());

    }

    private void waitObservableToExecute() {
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private ArrayList<RegimenItem> generateRegimenItems() {
        ArrayList<RegimenItem> regimenItems = new ArrayList<>();
        RegimenItem regimenItem = new RegimenItem();
        regimenItem.setAmount(100L);
        regimenItems.add(regimenItem);

        return regimenItems;
    }

    public class MyTestModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(MMIARepository.class).toInstance(mmiaRepository);
            bind(ProgramRepository.class).toInstance(programRepository);
            bind(MMIARequisitionPresenter.MMIARequisitionView.class).toInstance(mockMMIAformView);
            bind(SyncManager.class).toInstance(syncManager);
        }
    }
}
