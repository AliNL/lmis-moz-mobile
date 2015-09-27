package org.openlmis.core.model.repository;

import android.support.annotation.NonNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openlmis.core.LMISRepositoryUnitTest;
import org.openlmis.core.LMISTestRunner;
import org.openlmis.core.exceptions.LMISException;
import org.openlmis.core.model.Product;
import org.openlmis.core.model.Program;
import org.openlmis.core.model.RnRForm;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

import roboguice.RoboGuice;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@RunWith(LMISTestRunner.class)
public class RnrFormItemRepositoryTest extends LMISRepositoryUnitTest {
    RnrFormRepository.RnrFormItemRepository rnrFormItemRepository;
    private RnrFormRepository rnrFormRepository;

    @Before
    public void setUp() throws LMISException {
        rnrFormItemRepository = RoboGuice.getInjector(RuntimeEnvironment.application).getInstance(RnrFormRepository.RnrFormItemRepository.class);
        rnrFormRepository = RoboGuice.getInjector(RuntimeEnvironment.application).getInstance(RnrFormRepository.class);
    }

    @Test
    public void shouldQueryListForLowStockByProductId() throws LMISException {
        RnRForm newFrom = rnrFormRepository.create(new RnRForm());
        List<RnRForm.RnrFormItem> rnrFormItemList = new ArrayList<>();

        Program program = new Program();
        program.setProgramCode("1");
        Product product = new Product();
        product.setProgram(program);
        product.setId(1);

        rnrFormItemList.add(getRnrFormItem(newFrom, product, 1));
        rnrFormItemList.add(getRnrFormItem(newFrom, product, 2));
        rnrFormItemList.add(getRnrFormItem(newFrom, product, 3));
        rnrFormItemList.add(getRnrFormItem(newFrom, product, 0));
        rnrFormItemList.add(getRnrFormItem(newFrom, product, 5));
        rnrFormItemList.add(getRnrFormItem(newFrom, product, 7));
        newFrom.addItems(rnrFormItemList);

        List<RnRForm.RnrFormItem> rnrFormItemListFromDB = rnrFormItemRepository.queryListForLowStockByProductId(product);

        assertThat(rnrFormItemListFromDB.size(), is(3));

        assertThat(rnrFormItemListFromDB.get(0).getInventory(), is(7L));
        assertThat(rnrFormItemListFromDB.get(1).getInventory(), is(5L));
        assertThat(rnrFormItemListFromDB.get(2).getInventory(), is(3L));
    }

    @NonNull
    private RnRForm.RnrFormItem getRnrFormItem(RnRForm form, Product product, long inventory) {
        RnRForm.RnrFormItem rnrFormItem = new RnRForm.RnrFormItem();
        rnrFormItem.setForm(form);
        rnrFormItem.setProduct(product);
        rnrFormItem.setInventory(inventory);
        return rnrFormItem;
    }


}
