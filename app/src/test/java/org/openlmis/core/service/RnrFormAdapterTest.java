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
package org.openlmis.core.service;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.inject.AbstractModule;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openlmis.core.LMISTestRunner;
import org.openlmis.core.exceptions.LMISException;
import org.openlmis.core.manager.UserInfoMgr;
import org.openlmis.core.model.BaseInfoItem;
import org.openlmis.core.model.Product;
import org.openlmis.core.model.Program;
import org.openlmis.core.model.RegimenItem;
import org.openlmis.core.model.RnRForm;
import org.openlmis.core.model.RnrFormItem;
import org.openlmis.core.model.User;
import org.openlmis.core.model.repository.MMIARepository;
import org.openlmis.core.model.repository.ProductRepository;
import org.openlmis.core.model.repository.ProgramRepository;
import org.openlmis.core.network.adapter.RnrFormAdapter;
import org.openlmis.core.utils.DateUtil;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.Date;

import roboguice.RoboGuice;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(LMISTestRunner.class)
public class RnrFormAdapterTest {
    private RnrFormAdapter rnrFormAdapter;
    private RnRForm rnRForm;
    private ProductRepository mockProductRepository;
    private ProgramRepository mockProgramRepository;

    @Before
    public void setUp() throws LMISException {
        mockProductRepository = mock(ProductRepository.class);
        mockProgramRepository = mock(ProgramRepository.class);
        RoboGuice.overrideApplicationInjector(RuntimeEnvironment.application, new MyTestModule());
        rnrFormAdapter = RoboGuice.getInjector(RuntimeEnvironment.application).getInstance(RnrFormAdapter.class);
        rnRForm = new RnRForm();
        Program program = new Program();
        program.setProgramCode(MMIARepository.MMIA_PROGRAM_CODE);
        rnRForm.setProgram(program);
    }

    @Test
    public void shouldSerializeRnrFormWithCommentsToJsonObject() throws LMISException {

        UserInfoMgr.getInstance().setUser(new User("user", "password"));
        rnRForm.setComments("XYZ");
        rnRForm.setSubmittedTime(DateUtil.today());

        JsonElement rnrJson = rnrFormAdapter.serialize(rnRForm, RnRForm.class, null);
        assertEquals("\"XYZ\"", rnrJson.getAsJsonObject().get("clientSubmittedNotes").toString());
    }

    @Test
    public void shouldSerializeRnrFormWithSubmittedTime() throws Exception {
        UserInfoMgr.getInstance().setUser(new User("user", "password"));

        rnRForm.setSubmittedTime(DateUtil.parseString("2015-10-14 01:01:11", "yyyy-MM-dd HH:mm:ss"));

        JsonElement rnrJson = rnrFormAdapter.serialize(rnRForm, RnRForm.class, null);
        assertThat(rnrJson.getAsJsonObject().get("clientSubmittedTime").toString(), is("\"2015-10-14 01:01:11\""));
    }

    @Test
    public void shouldSerializeRnrFormWithExpirationDate() throws Exception {
        UserInfoMgr.getInstance().setUser(new User("user", "password"));

        ArrayList<RnrFormItem> rnrFormItemListWrapper = new ArrayList<>();
        RnrFormItem rnrFormItem = new RnrFormItem();
        rnrFormItem.setProduct(new Product());
        rnrFormItem.setValidate("10/11/2015");
        rnrFormItemListWrapper.add(rnrFormItem);
        rnRForm.setRnrFormItemListWrapper(rnrFormItemListWrapper);

        rnRForm.setSubmittedTime(DateUtil.parseString("2015-10-14 01:01:11", "yyyy-MM-dd HH:mm:ss"));

        JsonElement rnrJson = rnrFormAdapter.serialize(rnRForm, RnRForm.class, null);
        assertThat(rnrJson.getAsJsonObject().get("products").getAsJsonArray().get(0).getAsJsonObject().get("expirationDate").toString(), is("\"10/11/2015\""));
    }

    @Test
    public void shouldDeserializeRnrFormJson() throws LMISException {
        when(mockProductRepository.getByCode(anyString())).thenReturn(new Product());
        when(mockProgramRepository.queryByCode(anyString())).thenReturn(new Program());

        String json = "{\"products\":[{\"id\":81,\"rnrId\":130,\"product\":\"Zidovudina 50mg/5ml Sol Oral Solution 10mg \",\"productDisplayOrder\":29,\"productCode\":\"08S17\",\"productCategory\":\"Antibiotics\",\"productCategoryDisplayOrder\":1,\"roundToZero\":false,\"packRoundingThreshold\":1,\"packSize\":10,\"dosesPerMonth\":13,\"dosesPerDispensingUnit\":10,\"dispensingUnit\":\"Strip\",\"maxMonthsOfStock\":3,\"fullSupply\":true,\"quantityReceived\":10,\"quantityDispensed\":10,\"beginningBalance\":10,\"totalLossesAndAdjustments\":0,\"stockInHand\":10,\"stockOutDays\":0,\"newPatientCount\":0,\"quantityRequested\":0,\"reasonForRequestedQuantity\":\"reason\",\"amc\":10,\"normalizedConsumption\":10,\"periodNormalizedConsumption\":10,\"calculatedOrderQuantity\":20,\"maxStockQuantity\":30,\"quantityApproved\":0,\"reportingDays\":30,\"packsToShip\":1,\"expirationDate\":\"10/10/2016\",\"price\":0,\"skipped\":false}],\"nonFullSupplyProducts\":[],\"regimens\":[{\"id\":21,\"rnrId\":130,\"code\":\"018\",\"name\":\"ABC+3TC+EFZ\",\"patientsOnTreatment\":1,\"category\":{\"id\":null,\"code\":null,\"name\":\"Paediatrics\",\"displayOrder\":2},\"regimenDisplayOrder\":3,\"skipped\":false}],\"patientQuantifications\":[{\"id\":17,\"rnrId\":130,\"category\":\"Total Patients\",\"total\":30}],\"emergency\":false,\"clientSubmittedTime\": 1445937080000,\"clientSubmittedNotes\":\"I don't know\",\"programCode\": \"ESS_MEDS\",\"periodStartDate\":1388527200000}";

        RnRForm rnRForm = rnrFormAdapter.convertRnrForms(new JsonParser().parse(json));
        assertThat(rnRForm.getComments(), is("I don't know"));

        RnrFormItem rnrFormItem = rnRForm.getRnrFormItemListWrapper().get(0);
        assertThat(rnrFormItem.getInitialAmount(), is(10L));
        assertThat(rnrFormItem.getInventory(), is(10L));
        verify(mockProductRepository).getByCode("08S17");

        RegimenItem regimenItem = rnRForm.getRegimenItemListWrapper().get(0);
        assertThat(regimenItem.getAmount(), is(1L));

        BaseInfoItem baseInfoItem = rnRForm.getBaseInfoItemListWrapper().get(0);
        assertThat(baseInfoItem.getName(), is("Total Patients"));
        assertThat(baseInfoItem.getValue(), is("30"));

        assertThat(rnRForm.getSubmittedTime(), is(new Date(1445937080000L)));
    }

    public class MyTestModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(ProductRepository.class).toInstance(mockProductRepository);
            bind(ProgramRepository.class).toInstance(mockProgramRepository);
        }
    }

}
