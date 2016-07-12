package org.openlmis.core.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.openlmis.core.LMISTestApp;
import org.openlmis.core.LMISTestRunner;
import org.openlmis.core.R;
import org.openlmis.core.model.Product;
import org.openlmis.core.model.ProductProgram;
import org.openlmis.core.model.repository.ProductProgramRepository;
import org.openlmis.core.model.repository.ProductRepository;
import org.openlmis.core.network.LMISRestManagerMock;
import org.openlmis.core.utils.JsonFileReader;
import org.robolectric.RuntimeEnvironment;

import roboguice.RoboGuice;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

@RunWith(LMISTestRunner.class)
public class SyncDownManagerIT {
    private SyncDownManager syncDownManager;
    private LMISRestManagerMock lmisRestManager;
    private ProductRepository productRepository;
    private ProductProgramRepository productProgramRepository;

    @Test
    public void shouldSyncDownLatestProductWithArchivedStatus() throws Exception {
        //given
        String json = JsonFileReader.readJson(getClass(), "SyncDownLatestProductResponse.json");
        lmisRestManager = LMISRestManagerMock.getRestManagerWithMockClient("/rest-api/latest-products", 200, "OK", json, RuntimeEnvironment.application);
        syncDownManager = RoboGuice.getInjector(RuntimeEnvironment.application).getInstance(SyncDownManager.class);
        syncDownManager.lmisRestApi = lmisRestManager.getLmisRestApi();
        productRepository = RoboGuice.getInjector(RuntimeEnvironment.application).getInstance(ProductRepository.class);
        productProgramRepository = RoboGuice.getInjector(RuntimeEnvironment.application).getInstance(ProductProgramRepository.class);

        //when
        syncDownManager.syncDownLatestProducts();

        //then
        Product product = productRepository.getByCode("01A01");
        assertTrue(product.isArchived());
        assertEquals("Estavudina+Lamivudina+Nevirapi 6mg + 30mg +50mg, 60 Cps (BabyEmbalagem", product.getPrimaryName());
        assertEquals("Embalagem", product.getType());
        assertEquals("6mg + 30mg +50mg, 60 Cps (Baby", product.getStrength());

        ProductProgram productProgram = productProgramRepository.queryByCode("01A01", "ESS_MEDS");
        assertTrue(productProgram.isActive());
    }

}
