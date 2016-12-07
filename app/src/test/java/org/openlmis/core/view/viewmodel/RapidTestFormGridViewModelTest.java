package org.openlmis.core.view.viewmodel;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.openlmis.core.LMISTestRunner;
import org.openlmis.core.model.ProgramDataFormItem;

import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(LMISTestRunner.class)
public class RapidTestFormGridViewModelTest {
    RapidTestFormGridViewModel viewModel = new RapidTestFormGridViewModel(RapidTestFormGridViewModel.ColumnCode.Malaria);

    @Test
    public void shouldValidate() throws Exception {
        assertTrue(viewModel.validate());

        viewModel.setConsumptionValue("100");
        assertFalse(viewModel.validate());

        viewModel.setPositiveValue("100");
        viewModel.setConsumptionValue("");
        assertFalse(viewModel.validate());

        viewModel.setConsumptionValue("99");
        assertFalse(viewModel.validate());

        viewModel.setConsumptionValue("100");
        assertTrue(viewModel.validate());
    }

    @Test
    public void shouldConvertFormGridViewModelToDataModel() throws Exception {
        viewModel.setConsumptionValue("20");
        viewModel.setPositiveValue("1001");
        List<ProgramDataFormItem> programDataFormItems = viewModel.convertFormGridViewModelToDataModel(RapidTestReportViewModel.ACC_EMERGENCY);
        assertThat(programDataFormItems.get(0).getName(), is(RapidTestReportViewModel.ACC_EMERGENCY));
        assertThat(programDataFormItems.get(0).getProgramDataColumnCode(), is("CONSUME_MALARIA"));
        assertThat(programDataFormItems.get(0).getValue(), is(20));
        assertThat(programDataFormItems.get(1).getName(), is(RapidTestReportViewModel.ACC_EMERGENCY));
        assertThat(programDataFormItems.get(1).getProgramDataColumnCode(), is("POSITIVE_MALARIA"));
        assertThat(programDataFormItems.get(1).getValue(), is(1001));
    }
}