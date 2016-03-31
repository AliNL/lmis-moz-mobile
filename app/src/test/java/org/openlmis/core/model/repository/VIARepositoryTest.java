package org.openlmis.core.model.repository;

import com.google.inject.AbstractModule;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.openlmis.core.LMISTestRunner;
import org.openlmis.core.exceptions.LMISException;
import org.openlmis.core.model.Program;
import org.robolectric.RuntimeEnvironment;

import roboguice.RoboGuice;

import static org.mockito.Mockito.mock;

@RunWith(LMISTestRunner.class)
public class VIARepositoryTest {

    private StockRepository mockStockRepository;
    private ProductRepository productRepository;
    private VIARepository viaRepository;
    private Program viaProgram;

    @Before
    public void setup() throws LMISException {

        mockStockRepository = mock(StockRepository.class);
        productRepository = mock(ProductRepository.class);
        RoboGuice.overrideApplicationInjector(RuntimeEnvironment.application, new MyTestModule());

        viaRepository = RoboGuice.getInjector(RuntimeEnvironment.application).getInstance(VIARepository.class);

        viaProgram = new Program("VIA", "VIA", null, null);
        viaProgram.setId(1l);
    }

    public class MyTestModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(StockRepository.class).toInstance(mockStockRepository);
            bind(ProductRepository.class).toInstance(productRepository);
        }
    }
}