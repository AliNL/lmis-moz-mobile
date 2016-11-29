package org.openlmis.core.presenter;

import com.google.inject.Inject;

import org.openlmis.core.exceptions.LMISException;
import org.openlmis.core.exceptions.ViewNotMatchException;
import org.openlmis.core.model.Period;
import org.openlmis.core.model.ProgramDataForm;
import org.openlmis.core.model.repository.ProgramDataFormRepository;
import org.openlmis.core.model.repository.ProgramRepository;
import org.openlmis.core.model.service.PeriodService;
import org.openlmis.core.utils.Constants;
import org.openlmis.core.view.BaseView;
import org.openlmis.core.view.viewmodel.RapidTestReportViewModel;
import org.roboguice.shaded.goole.common.base.Optional;
import org.roboguice.shaded.goole.common.base.Predicate;
import org.roboguice.shaded.goole.common.collect.FluentIterable;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class RapidTestReportsPresenter extends Presenter {

    @Getter
    private List<RapidTestReportViewModel> viewModelList = new ArrayList<>();

    @Inject
    private ProgramDataFormRepository programDataFormRepository;

    @Inject
    private ProgramRepository programRepository;

    @Inject
    private PeriodService periodService;

    @Override
    public void attachView(BaseView v) throws ViewNotMatchException {

    }

    public Observable<List<RapidTestReportViewModel>> loadViewModels() {
        return Observable.just(generateViewModels())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    private List<RapidTestReportViewModel> generateViewModels() {
        viewModelList.clear();
        try {
            generateViewModelsForAllPeriods();
        } catch (LMISException e) {
            e.printStackTrace();
        }
        return viewModelList;
    }

    protected void generateViewModelsForAllPeriods() throws LMISException {
        Period period = periodService.getFirstStandardPeriod();

        if (period == null) {
            return;
        }

        List<ProgramDataForm> rapidTestForms = programDataFormRepository.listByProgramCode(Constants.RAPID_TEST_CODE);
        while (period != null) {
            RapidTestReportViewModel rapidTestReportViewModel = new RapidTestReportViewModel(period);
            viewModelList.add(rapidTestReportViewModel);
            final Period finalPeriod = period;
            Optional<ProgramDataForm> existingProgramDataForm = FluentIterable.from(rapidTestForms).firstMatch(new Predicate<ProgramDataForm>() {
                @Override
                public boolean apply(ProgramDataForm programDataForm) {
                    return programDataForm.getPeriodBegin().getTime() == finalPeriod.getBegin().getMillis();
                }
            });
            if (existingProgramDataForm.isPresent()) {
                rapidTestReportViewModel.setRapidTestForm(existingProgramDataForm.get());
            }
            period = periodService.generateNextPeriod(period);

        }
    }
}
