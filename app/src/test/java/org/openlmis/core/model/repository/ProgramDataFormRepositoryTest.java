package org.openlmis.core.model.repository;

import com.google.inject.AbstractModule;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openlmis.core.LMISRepositoryUnitTest;
import org.openlmis.core.LMISTestRunner;
import org.openlmis.core.exceptions.LMISException;
import org.openlmis.core.model.Program;
import org.openlmis.core.model.ProgramDataForm;
import org.openlmis.core.model.ProgramDataFormItem;
import org.openlmis.core.model.builder.ProgramDataFormBuilder;
import org.openlmis.core.utils.DateUtil;
import org.robolectric.RuntimeEnvironment;

import java.util.List;

import roboguice.RoboGuice;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(LMISTestRunner.class)
public class ProgramDataFormRepositoryTest extends LMISRepositoryUnitTest {

    private ProgramRepository mockProgramRepository;
    private ProgramDataFormRepository programDataFormRepository;
    private Program programRapidTest;

    @Before
    public void setup() throws LMISException {
        mockProgramRepository = mock(ProgramRepository.class);

        RoboGuice.overrideApplicationInjector(RuntimeEnvironment.application, new MyTestModule());

        programDataFormRepository = RoboGuice.getInjector(RuntimeEnvironment.application).getInstance(ProgramDataFormRepository.class);

        programRapidTest = new Program("RapidTest", "Rapid Test", null, false, null);
        programRapidTest.setId(1l);
    }


    public class MyTestModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(ProgramRepository.class).toInstance(mockProgramRepository);
        }
    }

    @Test
    public void shouldSaveProgramDataFormsAndListAllProgramDataForms() throws Exception {
        ProgramDataForm programDataForm1 = new ProgramDataFormBuilder()
                .setPeriod(DateUtil.parseString("2016-09-23", DateUtil.DB_DATE_FORMAT))
                .setStatus(ProgramDataForm.STATUS.SUBMITTED)
                .setProgram(programRapidTest)
                .build();

        ProgramDataFormItem dataFormItem1 = new ProgramDataFormItem("name1", "POSITIVE_COLUMN1", 1);
        ProgramDataFormItem dataFormItem2 = new ProgramDataFormItem("name2", "CONSUME_COLUMN1", 9);
        dataFormItem1.setForm(programDataForm1);
        dataFormItem2.setForm(programDataForm1);
        programDataForm1.getProgramDataFormItemListWrapper().add(dataFormItem1);
        programDataForm1.getProgramDataFormItemListWrapper().add(dataFormItem2);

        ProgramDataForm programDataForm2 = new ProgramDataFormBuilder()
                .setPeriod(DateUtil.parseString("2016-10-23", DateUtil.DB_DATE_FORMAT))
                .setProgram(programRapidTest)
                .setStatus(ProgramDataForm.STATUS.SUBMITTED)
                .build();
        programDataFormRepository.batchCreateOrUpdate(programDataForm1);
        programDataFormRepository.batchCreateOrUpdate(programDataForm2);

        when(mockProgramRepository.queryByCode("RapidTest")).thenReturn(programRapidTest);

        List<ProgramDataForm> programDataFormRetrieved = programDataFormRepository.listByProgramCode("RapidTest");

        assertThat(programDataFormRetrieved.get(0).getPeriodBegin(), is(programDataForm1.getPeriodBegin()));
        assertThat(programDataFormRetrieved.get(0).getPeriodEnd(), is(programDataForm1.getPeriodEnd()));
        assertThat(programDataFormRetrieved.get(0).getProgramDataFormItemListWrapper().get(0).getName(), is("name1"));
        assertThat(programDataFormRetrieved.get(0).getProgramDataFormItemListWrapper().get(0).getProgramDataColumnCode(), is("POSITIVE_COLUMN1"));
        assertThat(programDataFormRetrieved.get(0).getProgramDataFormItemListWrapper().get(0).getValue(), is(1));
        assertThat(programDataFormRetrieved.get(0).getProgramDataFormItemListWrapper().get(1).getName(), is("name2"));
        assertThat(programDataFormRetrieved.get(0).getProgramDataFormItemListWrapper().get(1).getProgramDataColumnCode(), is("CONSUME_COLUMN1"));
        assertThat(programDataFormRetrieved.get(0).getProgramDataFormItemListWrapper().get(1).getValue(), is(9));
        assertThat(programDataFormRetrieved.get(1).getPeriodBegin(), is(programDataForm2.getPeriodBegin()));
        assertThat(programDataFormRetrieved.get(1).getStatus(), is(programDataForm2.getStatus()));

        List<ProgramDataForm> programDataNonExist = programDataFormRepository.listByProgramCode("Some other program");
        assertThat(programDataNonExist.size(), is(0));


        //update
        programDataForm1.setStatus(ProgramDataForm.STATUS.AUTHORIZED);
        programDataForm1.setPeriodBegin(DateUtil.parseString("2015-09-30", DateUtil.DB_DATE_FORMAT));
        programDataForm1.getProgramDataFormItemListWrapper().clear();

        ProgramDataFormItem dataFormItem3 = new ProgramDataFormItem("name3", "POSITIVE_COLUMN3", 100);
        ProgramDataFormItem dataFormItem4 = new ProgramDataFormItem("name4", "CONSUME_COLUMN4", 900);
        dataFormItem3.setForm(programDataForm1);
        dataFormItem4.setForm(programDataForm1);

        programDataForm1.getProgramDataFormItemListWrapper().add(dataFormItem3);
        programDataForm1.getProgramDataFormItemListWrapper().add(dataFormItem4);

        programDataFormRepository.batchCreateOrUpdate(programDataForm1);
        List<ProgramDataForm> programDataFormRetrievedAfterRevision = programDataFormRepository.listByProgramCode("RapidTest");
        assertThat(programDataFormRetrievedAfterRevision.get(0).getPeriodBegin(), is(programDataForm1.getPeriodBegin()));
        assertThat(programDataFormRetrievedAfterRevision.get(0).getStatus(), is(ProgramDataForm.STATUS.AUTHORIZED));
        assertThat(programDataFormRetrievedAfterRevision.get(0).getProgramDataFormItemListWrapper().size(), is(2));
        assertThat(programDataFormRetrievedAfterRevision.get(0).getProgramDataFormItemListWrapper().get(0).getName(), is("name3"));
        assertThat(programDataFormRetrievedAfterRevision.get(0).getProgramDataFormItemListWrapper().get(0).getProgramDataColumnCode(), is("POSITIVE_COLUMN3"));
        assertThat(programDataFormRetrievedAfterRevision.get(0).getProgramDataFormItemListWrapper().get(0).getValue(), is(100));
        assertThat(programDataFormRetrievedAfterRevision.get(0).getProgramDataFormItemListWrapper().get(1).getName(), is("name4"));
        assertThat(programDataFormRetrievedAfterRevision.get(0).getProgramDataFormItemListWrapper().get(1).getProgramDataColumnCode(), is("CONSUME_COLUMN4"));
        assertThat(programDataFormRetrievedAfterRevision.get(0).getProgramDataFormItemListWrapper().get(1).getValue(), is(900));
    }

    @Test
    public void shouldQueryFormById() throws Exception {
        ProgramDataForm programDataForm1 = new ProgramDataFormBuilder()
                .setPeriod(DateUtil.parseString("2016-09-23", DateUtil.DB_DATE_FORMAT))
                .setStatus(ProgramDataForm.STATUS.SUBMITTED)
                .setProgram(programRapidTest)
                .build();
        ProgramDataForm programDataForm2 = new ProgramDataFormBuilder()
                .setPeriod(DateUtil.parseString("2016-10-23", DateUtil.DB_DATE_FORMAT))
                .setProgram(programRapidTest)
                .setStatus(ProgramDataForm.STATUS.SUBMITTED)
                .build();
        programDataFormRepository.batchCreateOrUpdate(programDataForm1);
        programDataFormRepository.batchCreateOrUpdate(programDataForm2);

        when(mockProgramRepository.queryByCode("RapidTest")).thenReturn(programRapidTest);

        ProgramDataForm programDataFormQueried1 = programDataFormRepository.queryById(1L);
        ProgramDataForm programDataFormQueried2 = programDataFormRepository.queryById(2L);

        assertThat(programDataFormQueried1.getPeriodBegin(), is(programDataForm1.getPeriodBegin()));
        assertThat(programDataFormQueried1.getPeriodEnd(), is(programDataForm1.getPeriodEnd()));
        assertThat(programDataFormQueried2.getPeriodBegin(), is(programDataForm2.getPeriodBegin()));
        assertThat(programDataFormQueried2.getStatus(), is(programDataForm2.getStatus()));
    }
}