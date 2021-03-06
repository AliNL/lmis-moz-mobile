package org.openlmis.core.view.viewmodel;

import org.openlmis.core.LMISApp;
import org.openlmis.core.R;
import org.openlmis.core.manager.MovementReasonManager;
import org.openlmis.core.model.Period;
import org.openlmis.core.model.Program;
import org.openlmis.core.model.ProgramDataForm;
import org.openlmis.core.model.ProgramDataFormItem;
import org.openlmis.core.model.ProgramDataFormSignature;
import org.openlmis.core.model.Signature;
import org.roboguice.shaded.goole.common.base.Predicate;
import org.roboguice.shaded.goole.common.collect.FluentIterable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Data;
import lombok.Getter;

@Data
public class RapidTestReportViewModel implements Serializable {
    Period period;
    Status status;
    private Date syncedTime;

    MovementReasonManager movementReasonManager;

    RapidTestFormItemViewModel itemTotal;
    List<RapidTestFormItemViewModel> itemViewModelList = new ArrayList<>();
    Map<String, RapidTestFormItemViewModel> itemViewModelMap = new HashMap<>();

    private ProgramDataForm rapidTestForm = new ProgramDataForm();

    public static long DEFAULT_FORM_ID = 0;

    public RapidTestReportViewModel(Period period) {
        this.period = period;
        status = Status.MISSING;
        setupCategories();
        setItemViewModelMap();
    }

    private void setupCategories() {
        movementReasonManager = MovementReasonManager.getInstance();
        List<MovementReasonManager.MovementReason> issueReasons = FluentIterable.from(movementReasonManager.buildReasonListForMovementType(MovementReasonManager.MovementType.ISSUE))
                .filter(new Predicate<MovementReasonManager.MovementReason>() {
                    @Override
                    public boolean apply(MovementReasonManager.MovementReason movementReason) {
                        return !movementReason.getCode().equals("PUB_PHARMACY");
                    }
                }).toList();

        for (MovementReasonManager.MovementReason movementReason: issueReasons) {
            RapidTestFormItemViewModel item = new RapidTestFormItemViewModel(movementReason);
            itemViewModelList.add(item);
        }

        MovementReasonManager.MovementReason totalCategory = new MovementReasonManager.MovementReason(MovementReasonManager.MovementType.ISSUE, "TOTAL", LMISApp.getInstance().getString(R.string.total));

        itemTotal = new RapidTestFormItemViewModel(totalCategory);
        itemViewModelList.add(itemTotal);
    }

    private void setItemViewModelMap() {
        for (RapidTestFormItemViewModel viewModel : itemViewModelList) {
            itemViewModelMap.put(viewModel.getIssueReason().getCode(), viewModel);
        }
    }

    public RapidTestReportViewModel(ProgramDataForm programDataForm) {
        setRapidTestForm(programDataForm);
        period = Period.of(programDataForm.getPeriodBegin());
        setupCategories();
        setItemViewModelMap();
        setFormItemViewModels(programDataForm.getProgramDataFormItemListWrapper());
    }

    private void setFormItemViewModels(List<ProgramDataFormItem> programDataFormItemList) {
        for (ProgramDataFormItem item : programDataFormItemList) {
            itemViewModelMap.get(item.getName()).setColumnValue(item.getProgramDataColumn(), item.getValue());
        }
        for (RapidTestFormGridViewModel.ColumnCode columnCode : RapidTestFormGridViewModel.ColumnCode.values()) {
            updateTotal(columnCode, true);
            updateTotal(columnCode, false);
        }
    }

    public void setRapidTestForm(ProgramDataForm rapidTestForm) {
        this.rapidTestForm = rapidTestForm;
        switch (rapidTestForm.getStatus()) {
            case DRAFT:
            case SUBMITTED:
                this.status = Status.INCOMPLETE;
                break;
            case AUTHORIZED:
                this.status = Status.COMPLETED;
                break;
            default:
                this.status = Status.MISSING;
        }
        if (rapidTestForm.isSynced()) {
            this.status = Status.SYNCED;
        }
    }

    public Date getSyncedTime() {
        return rapidTestForm.getSubmittedTime();
    }

    public void convertFormViewModelToDataModel(Program program) {
        rapidTestForm.setProgram(program);
        rapidTestForm.setPeriodBegin(period.getBegin().toDate());
        rapidTestForm.setPeriodEnd(period.getEnd().toDate());
        rapidTestForm.getProgramDataFormItemListWrapper().clear();
        convertFormItemViewModelToDataModel();
    }

    private void convertFormItemViewModelToDataModel() {
        for (RapidTestFormItemViewModel itemViewModel : itemViewModelList) {
            rapidTestForm.getProgramDataFormItemListWrapper().addAll(itemViewModel.convertToDataModel());
        }
        for (ProgramDataFormItem item : rapidTestForm.getProgramDataFormItemListWrapper()) {
            item.setForm(rapidTestForm);
        }
    }

    public boolean isSynced() {
        return status == Status.SYNCED;
    }

    public boolean isEditable() {
        return status.isEditable() && isDraft();
    }

    public boolean isDraft() {
        return rapidTestForm.getStatus() == null || rapidTestForm.getStatus() == ProgramDataForm.STATUS.DRAFT;
    }

    public boolean validate() {
        for (RapidTestFormItemViewModel itemViewModel : itemViewModelList) {
            if (!itemViewModel.validate()) {
                return false;
            }
        }
        return true;
    }

    public void addSignature(String signature) {
        if (rapidTestForm.getSignaturesWrapper().size() == 0) {
            rapidTestForm.getSignaturesWrapper().add(new ProgramDataFormSignature(rapidTestForm, signature, Signature.TYPE.SUBMITTER));
            rapidTestForm.setStatus(ProgramDataForm.STATUS.SUBMITTED);
        } else {
            rapidTestForm.getSignaturesWrapper().add(new ProgramDataFormSignature(rapidTestForm, signature, Signature.TYPE.APPROVER));
            rapidTestForm.setStatus(ProgramDataForm.STATUS.AUTHORIZED);
            rapidTestForm.setSubmittedTime(new Date());
            status = Status.COMPLETED;
        }
    }

    public boolean isAuthorized() {
        return rapidTestForm.getStatus() == ProgramDataForm.STATUS.AUTHORIZED;
    }

    public boolean isFormEmpty() {
        for (RapidTestFormItemViewModel itemViewModel : itemViewModelList) {
            if (!itemViewModel.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public boolean isSubmitted() {
        return rapidTestForm.getStatus() == ProgramDataForm.STATUS.SUBMITTED;
    }

    public void updateTotal(RapidTestFormGridViewModel.ColumnCode columnCode, boolean isConsume) {
        itemTotal.clearValue(columnCode, isConsume);
        long total = 0;
        for (RapidTestFormItemViewModel itemViewModel : itemViewModelList) {
            RapidTestFormGridViewModel gridViewModel = itemViewModel.getRapidTestFormGridViewModelMap().get(columnCode.toString());
            if (isConsume && !gridViewModel.getConsumptionValue().equals("")) {
                total += Long.parseLong(gridViewModel.getConsumptionValue());
            } else if (!isConsume && !gridViewModel.getPositiveValue().equals("")) {
                total += Long.parseLong(gridViewModel.getPositiveValue());
            }
        }
        if (isConsume) {
            itemTotal.getRapidTestFormGridViewModelMap().get(columnCode.toString()).setConsumptionValue(String.valueOf(total));
        } else {
            itemTotal.getRapidTestFormGridViewModelMap().get(columnCode.toString()).setPositiveValue(String.valueOf(total));
        }
    }

    public enum Status {
        MISSING(true, 0),
        INCOMPLETE(true, 1),
        COMPLETED(false, 2),
        SYNCED(false, 3);

        @Getter
        private boolean editable;
        private int viewType;

        Status(boolean editable, int viewType) {
            this.editable = editable;
            this.viewType = viewType;
        }

        public int getViewType() {
            return viewType;
        }
    }
}

