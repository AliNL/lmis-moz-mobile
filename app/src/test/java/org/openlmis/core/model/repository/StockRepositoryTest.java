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

package org.openlmis.core.model.repository;


import android.support.annotation.NonNull;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.internal.matchers.NotNull;
import org.openlmis.core.LMISRepositoryUnitTest;
import org.openlmis.core.LMISTestApp;
import org.openlmis.core.LMISTestRunner;
import org.openlmis.core.R;
import org.openlmis.core.exceptions.LMISException;
import org.openlmis.core.model.DraftInventory;
import org.openlmis.core.model.Period;
import org.openlmis.core.model.Product;
import org.openlmis.core.model.ProductProgram;
import org.openlmis.core.model.Program;
import org.openlmis.core.model.RnRForm;
import org.openlmis.core.model.StockCard;
import org.openlmis.core.model.StockMovementItem;
import org.openlmis.core.model.builder.ProductBuilder;
import org.openlmis.core.model.builder.ProductProgramBuilder;
import org.openlmis.core.model.builder.ProgramBuilder;
import org.openlmis.core.utils.Constants;
import org.openlmis.core.utils.DateUtil;
import org.robolectric.RuntimeEnvironment;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import roboguice.RoboGuice;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.openlmis.core.model.StockMovementItem.MovementType.ISSUE;
import static org.openlmis.core.model.StockMovementItem.MovementType.RECEIVE;
import static org.openlmis.core.model.builder.StockCardBuilder.saveStockCardWithOneMovement;
import static org.openlmis.core.utils.DateUtil.DATE_TIME_FORMAT;
import static org.openlmis.core.utils.DateUtil.SIMPLE_DATE_FORMAT;
import static org.openlmis.core.utils.DateUtil.parseString;
import static org.roboguice.shaded.goole.common.collect.Lists.newArrayList;

@RunWith(LMISTestRunner.class)
public class StockRepositoryTest extends LMISRepositoryUnitTest {

    StockRepository stockRepository;
    ProductRepository productRepository;
    Product product;
    private Date lastFirstMonthDate;
    private Date lastSecondMonthDate;
    private ProgramRepository programRepository;
    private ProductProgramRepository productProgramRepository;
    private StockCard stockCard;

    @Before
    public void setup() throws LMISException {

        stockRepository = RoboGuice.getInjector(RuntimeEnvironment.application).getInstance(StockRepository.class);
        productRepository = RoboGuice.getInjector(RuntimeEnvironment.application).getInstance(ProductRepository.class);
        programRepository = RoboGuice.getInjector(RuntimeEnvironment.application).getInstance(ProgramRepository.class);
        productProgramRepository = RoboGuice.getInjector(RuntimeEnvironment.application).getInstance(ProductProgramRepository.class);

        saveTestProduct();

        Date today = DateUtil.today();
        lastFirstMonthDate = DateUtil.generatePreviousMonthDateBy(today);
        lastSecondMonthDate = DateUtil.generatePreviousMonthDateBy(lastFirstMonthDate);
        stockCard = new StockCard();
    }

    @Test
    public void shouldSaveStockCardsSuccessful() throws LMISException {
        StockCard stockCard = new StockCard();
        stockCard.setStockOnHand(1);
        stockCard.setProduct(product);

        stockRepository.createOrUpdate(stockCard);

        assertThat(stockRepository.list().size(), is(1));
        assertThat(stockRepository.list().get(0).getProduct(), is(NotNull.NOT_NULL));
    }

    @Test
    public void shouldBatchSaveStockCardAndMovementSuccessful() throws LMISException {
        ArrayList<StockCard> stockCards = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            StockCard stockCard = new StockCard();
            stockCard.setStockOnHand(i);
            product.setArchived(true);
            stockCard.setProduct(product);
            stockCard.setStockMovementItemsWrapper(Arrays.asList(stockCard.generateInitialStockMovementItem()));

            stockCards.add(stockCard);
        }

        stockRepository.batchSaveStockCardsWithMovementItemsAndUpdateProduct(stockCards);

        List<StockCard> stockCardList = stockRepository.list();

        assertThat(stockCardList.size(), is(10));
        assertThat(stockCardList.get(0).getProduct(), is(NotNull.NOT_NULL));
        assertThat(stockCardList.get(0).getForeignStockMovementItems().size(), is(1));
        assertTrue(stockCardList.get(0).getProduct().isArchived());
    }

    @Test
    public void shouldGetCorrectDataAfterSavedStockMovementItem() throws Exception {
        //given saved
        StockCard savedStockCard = saveStockCardWithOneMovement(stockRepository);
        StockMovementItem savedMovementItem = savedStockCard.getForeignStockMovementItems().iterator().next();

        //when retrieve
        List<StockMovementItem> retrievedStockMovementItems = stockRepository.listLastFive(savedStockCard.getId());
        StockMovementItem retrievedMovementItem = retrievedStockMovementItems.get(retrievedStockMovementItems.size() - 1);

        //then
        assertEquals(savedMovementItem.getId(), retrievedMovementItem.getId());
        assertEquals(savedMovementItem.getMovementQuantity(), retrievedMovementItem.getMovementQuantity());
        assertEquals(savedMovementItem.getStockOnHand(), retrievedMovementItem.getStockOnHand());
        assertEquals(savedMovementItem.getMovementType(), retrievedMovementItem.getMovementType());
        assertEquals(savedMovementItem.getDocumentNumber(), retrievedMovementItem.getDocumentNumber());
        assertEquals(savedMovementItem.getReason(), retrievedMovementItem.getReason());
    }

    @Test
    public void shouldListUnsyncedStockMovementItems() throws LMISException, ParseException {
        //given one movement was saved but NOT SYNCED
        StockCard stockCard = saveStockCardWithOneMovement(stockRepository);
        assertThat(stockCard.getForeignStockMovementItems().size(), is(1));
        assertThat(stockRepository.listUnSynced().size(), is(1));

        //when save another SYNCED movement
        createMovementItem(RECEIVE, 100, stockCard, new Date(), DateUtil.today(), true);

        //then
        assertThat(stockCard.getForeignStockMovementItems().size(), is(2));
        assertThat(stockRepository.listUnSynced(), notNullValue());
        assertThat(stockRepository.listUnSynced().size(), is(1));
    }

    @Test
    public void shouldBatchUpdateStockMovements() throws LMISException, ParseException {
        //given
        StockCard stockCard = saveStockCardWithOneMovement(stockRepository);

        //when
        createMovementItem(RECEIVE, 100, stockCard, new Date(), DateUtil.today(), false);

        //then
        List<StockMovementItem> items = newArrayList(stockCard.getForeignStockMovementItems());
        assertThat(items.size(), is(2));
        assertThat(items.get(0).isSynced(), is(false));
        assertThat(items.get(1).isSynced(), is(false));

        //when
        for (StockMovementItem entry : items) {
            entry.setSynced(true);
        }
        stockRepository.batchCreateOrUpdateStockMovements(items);

        //then
        stockCard = stockRepository.list().get(0);
        items = newArrayList(stockCard.getForeignStockMovementItems());
        assertThat(items.size(), is(2));
        assertThat(items.get(0).isSynced(), is(true));
        assertThat(items.get(1).isSynced(), is(true));
    }

    @Test
    public void shouldListDraftInventory() throws Exception {
        saveDraftInventory();

        List<DraftInventory> draftInventories = stockRepository.listDraftInventory();
        assertThat(draftInventories.get(0).getQuantity(), is(10L));
        assertThat(draftInventories.get(0).getExpireDates(), is("11/10/2015"));
        assertThat(draftInventories.get(1).getQuantity(), is(20L));
        assertThat(draftInventories.get(1).getExpireDates(), is("12/10/2015"));
    }

    @Test
    public void shouldClearDraftInventory() throws Exception {
        saveDraftInventory();
        Assert.assertThat(stockRepository.listDraftInventory().size(), is(2));
        stockRepository.clearDraftInventory();
        Assert.assertThat(stockRepository.listDraftInventory().size(), is(0));
    }

    @Test
    public void shouldGetFirstPeriodDate() throws Exception {
        StockCard stockCard = saveStockCardWithOneMovement(stockRepository);

        Date firstMovementDate = DateUtil.parseString("10/09/2014", DateUtil.SIMPLE_DATE_FORMAT);
        createMovementItem(ISSUE, 100, stockCard, new Date(), firstMovementDate, false);
        createMovementItem(ISSUE, 100, stockCard, new Date(), DateUtil.parseString("11/10/2015", DateUtil.SIMPLE_DATE_FORMAT), false);
        createMovementItem(ISSUE, 100, stockCard, new Date(), DateUtil.parseString("12/11/2015", DateUtil.SIMPLE_DATE_FORMAT), false);

        Date firstPeriodBegin = stockRepository.queryFirstPeriodBegin(stockCard);
        assertThat(firstPeriodBegin, is(parseString("21/08/2014", SIMPLE_DATE_FORMAT)));
    }


    @Test
    public void shouldGetStockCardsByProgramIdWithoutKitAndDeacitivatedWhenMultipleProgramsToggleOnAndDeactivateProgramToggleOff() throws Exception {
        //when
        LMISTestApp.getInstance().setFeatureToggle(R.bool.feature_deactivate_program_product, false);

        batchCreateNewStockCards();

        //then
        List<StockCard> stockCardsBeforeTimeLine = stockRepository.listActiveStockCards("code1", ProductRepository.IsWithKit.No);
        assertThat(stockCardsBeforeTimeLine.size(), is(2));

        List<StockCard> stockCardsBeforeTimeLine2 = stockRepository.listActiveStockCards("code2", ProductRepository.IsWithKit.No);
        assertThat(stockCardsBeforeTimeLine2.size(), is(1));
    }

    @Test
    public void shouldGetStockCardsByProgramIdWithoutKitAndDeacitivatedWhenMultipleProgramsToggleOnAndDeactivateProgramToggleOn() throws Exception {
        //when
        LMISTestApp.getInstance().setFeatureToggle(R.bool.feature_deactivate_program_product, true);

        batchSaveNewStockCardsWithProductPrograms();

        //then
        List<StockCard> stockCardsBeforeTimeLine = stockRepository.listActiveStockCards("code1", ProductRepository.IsWithKit.No);
        assertThat(stockCardsBeforeTimeLine.size(), is(2));

        List<StockCard> stockCardsBeforeTimeLine2 = stockRepository.listActiveStockCards("code2", ProductRepository.IsWithKit.No);
        assertThat(stockCardsBeforeTimeLine2.size(), is(1));
    }

    private void batchSaveNewStockCardsWithProductPrograms() throws LMISException {
        createNewStockCardWithProductPrograms("code1", null, ProductBuilder.create().setCode("p1").setIsActive(true).setIsKit(false).build());
        createNewStockCardWithProductPrograms("code3", "code1", ProductBuilder.create().setCode("p5").setIsActive(true).setIsKit(false).build());
        createNewStockCardWithProductPrograms("code2", null, ProductBuilder.create().setCode("p2").setIsActive(true).setIsKit(false).build());
        createNewStockCardWithProductPrograms("code1", null, ProductBuilder.create().setCode("p3").setIsActive(false).setIsKit(false).build());
        createNewStockCardWithProductPrograms("code2", null, ProductBuilder.create().setCode("p4").setIsActive(true).setIsKit(true).build());
        createNewStockCardWithProductPrograms("code4", null, ProductBuilder.create().setCode("p6").setIsActive(true).setIsArchived(false).setIsKit(true).build());
        createNewStockCardWithProductPrograms("code4", null, ProductBuilder.create().setCode("p7").setIsActive(true).setIsArchived(true).setIsKit(true).build());
    }

    @Test
    public void shouldGetStockCardsWithKitWhenMultiplePrgramsToggleOnAndDeactivateProgramToggleOff() throws Exception {
        //when
        LMISTestApp.getInstance().setFeatureToggle(R.bool.feature_deactivate_program_product, false);

        batchCreateNewStockCards();

        //then
        List<StockCard> stockCardsBeforeTimeLine = stockRepository.listActiveStockCards("code1", ProductRepository.IsWithKit.Yes);
        assertThat(stockCardsBeforeTimeLine.size(), is(2));

        List<StockCard> stockCardsBeforeTimeLine2 = stockRepository.listActiveStockCards("code2", ProductRepository.IsWithKit.Yes);
        assertThat(stockCardsBeforeTimeLine2.size(), is(2));
    }

    @Test
    public void shouldGetActiveStockCardsWithKit() throws Exception {
        //when
        LMISTestApp.getInstance().setFeatureToggle(R.bool.feature_deactivate_program_product, true);

        batchSaveNewStockCardsWithProductPrograms();

        Product product = ProductBuilder.buildAdultProduct();
        product.setKit(true);
        productRepository.createOrUpdate(product);

        //then
        List<StockCard> stockCardsBeforeTimeLine = stockRepository.listActiveStockCards("code1", ProductRepository.IsWithKit.Yes);
        assertThat(stockCardsBeforeTimeLine.size(), is(2));

        List<StockCard> stockCardsBeforeTimeLine2 = stockRepository.listActiveStockCards("code2", ProductRepository.IsWithKit.Yes);
        assertThat(stockCardsBeforeTimeLine2.size(), is(2));

        List<StockCard> stockCardsBeforeTimeLine3 = stockRepository.listActiveStockCards("code4", ProductRepository.IsWithKit.Yes);
        assertThat(stockCardsBeforeTimeLine3.size(), is(1));
    }

    private void batchCreateNewStockCards() throws LMISException {
        createNewStockCard("code1", null, ProductBuilder.create().setCode("p1").setIsActive(true).setIsKit(false).build());
        createNewStockCard("code3", "code1", ProductBuilder.create().setCode("p5").setIsActive(true).setIsKit(false).build());
        createNewStockCard("code2", null, ProductBuilder.create().setCode("p2").setIsActive(true).setIsKit(false).build());
        createNewStockCard("code1", null, ProductBuilder.create().setCode("p3").setIsActive(false).setIsKit(false).build());
        createNewStockCard("code2", null, ProductBuilder.create().setCode("p4").setIsActive(true).setIsKit(true).build());
    }

    @Test
    public void shouldGetFirstItemWhenMovementDateDiff() throws Exception {
        //given
        DateTime dateTime = new DateTime();

        stockRepository.createOrUpdate(stockCard);

        ArrayList<StockMovementItem> stockMovementItems = new ArrayList<>();

        stockMovementItems.add(getStockMovementItem(dateTime, dateTime));

        DateTime earlierTime = dateTime.minusMonths(1);
        StockMovementItem movementEarlierItem = getStockMovementItem(dateTime, earlierTime);
        stockMovementItems.add(movementEarlierItem);
        stockRepository.batchCreateOrUpdateStockMovements(stockMovementItems);

        //when
        StockMovementItem stockMovementItem = stockRepository.queryFirstStockMovementItem(stockCard);

        //then
        assertThat(stockMovementItem.getId(), is(movementEarlierItem.getId()));
    }

    @Test
    public void shouldGetFirstItemWhenCreatedTimeDiff() throws Exception {
        //given
        DateTime dateTime = new DateTime();
        stockRepository.createOrUpdate(stockCard);

        ArrayList<StockMovementItem> stockMovementItems = new ArrayList<>();

        stockMovementItems.add(getStockMovementItem(dateTime, dateTime));

        DateTime earlierTime = dateTime.minusMonths(1);
        StockMovementItem createdEarlierMovementItem = getStockMovementItem(earlierTime, dateTime);
        stockMovementItems.add(createdEarlierMovementItem);

        stockRepository.batchCreateOrUpdateStockMovements(stockMovementItems);

        //when
        StockMovementItem stockMovementItem = stockRepository.queryFirstStockMovementItem(stockCard);

        //then
        assertThat(stockMovementItem.getId(), is(createdEarlierMovementItem.getId()));
    }

    @Test
    public void shouldGetStockMovementsCreatedBetweenTwoDatesExclusiveOfBeginDate() throws LMISException {
        stockCard.setStockOnHand(100);
        stockRepository.createOrUpdate(stockCard);
        StockMovementItem movementItem = createMovementItem(ISSUE, 1, stockCard, DateUtil.parseString("2020-01-21 13:00:00", DATE_TIME_FORMAT), DateUtil.parseString("2020-01-21 13:00:00", DATE_TIME_FORMAT), false);
        createMovementItem(RECEIVE, 2, stockCard, DateUtil.parseString("2020-02-01 11:00:00", DATE_TIME_FORMAT), DateUtil.parseString("2020-02-01 11:00:00", DATE_TIME_FORMAT), false);
        createMovementItem(ISSUE, 3, stockCard, DateUtil.parseString("2020-02-22 20:00:00", DATE_TIME_FORMAT), DateUtil.parseString("2020-02-22 20:00:00", DATE_TIME_FORMAT), false);
        createMovementItem(ISSUE, 4, stockCard, DateUtil.parseString("2020-02-25 13:00:00", DATE_TIME_FORMAT), DateUtil.parseString("2020-02-25 13:00:00", DATE_TIME_FORMAT), false);

        List<StockMovementItem> stockMovementItems = stockRepository.queryStockItemsByPeriodDates(stockCard,
                movementItem.getCreatedTime(), DateUtil.parseString("2020-02-22 20:00:00", DATE_TIME_FORMAT));

        Assert.assertThat(stockMovementItems.size(), is(2));
    }

    private StockMovementItem getStockMovementItem(DateTime createdTime, DateTime movementDate) {
        StockMovementItem movementItem = new StockMovementItem();
        movementItem.setStockCard(stockCard);
        movementItem.setCreatedTime(createdTime.toDate());
        movementItem.setMovementDate(movementDate.toDate());
        return movementItem;
    }

    private long createNewStockCard(String code, String parentCode, Product product) throws LMISException {
        return createNewStockCard(code, parentCode, product, false);
    }

    private long createNewStockCard(String code, String parentCode, Product product, boolean isEmergency) throws LMISException {
        StockCard stockCard = new StockCard();
        Program program = createNewProgram(code, parentCode, isEmergency);
        programRepository.createOrUpdate(program);

        product.setProgram(program);
        productRepository.createOrUpdate(product);

        stockCard.setProduct(product);
        stockCard.setCreatedAt(new Date());
        stockRepository.createOrUpdate(stockCard);

        return program.getId();
    }

    private long createNewStockCardWithProductPrograms(String programCode, String parentCode, Product product) throws LMISException {
        StockCard stockCard = new StockCard();
        Program program = createNewProgram(programCode, parentCode, false);

        createNewProductProgram(programCode, product.getCode());

        productRepository.createOrUpdate(product);

        stockCard.setProduct(product);
        stockCard.setCreatedAt(new Date());
        stockRepository.createOrUpdate(stockCard);

        return program.getId();
    }

    private void createNewProductProgram(String code, String productCode) throws LMISException {
        ProductProgram productProgram = new ProductProgramBuilder().setProgramCode(code).setProductCode(productCode).setActive(true).build();
        productProgramRepository.createOrUpdate(productProgram);
    }

    @NonNull
    private Program createNewProgram(String code, String parentCode, boolean isSupportEmergency) throws LMISException {
        Program program = new ProgramBuilder().setProgramCode(code).setParentCode(parentCode).setSupportEmergency(isSupportEmergency).build();
        programRepository.createOrUpdate(program);
        return program;
    }

    @Test
    public void shouldUpdateStockCardAndProduct() throws Exception {
        StockCard stockCard = new StockCard();
        product.setArchived(true);
        stockCard.setProduct(product);
        stockCard.setExpireDates("01/01/2016");

        stockRepository.createOrUpdate(stockCard);

        stockCard.setExpireDates("");
        product.setArchived(false);
        stockRepository.updateStockCardWithProduct(stockCard);

        assertThat(stockCard.getExpireDates(), is(""));

    }

    @Test
    public void shouldUpdateProductOfStockCard() throws Exception {
        StockCard stockCard = new StockCard();
        product.setArchived(true);
        stockCard.setProduct(product);
        stockCard.setExpireDates("01/01/2016");

        stockRepository.createOrUpdate(stockCard);

        stockCard.setExpireDates("");
        product.setArchived(false);
        stockRepository.updateProductOfStockCard(stockCard.getProduct());

        assertThat(stockCard.getProduct().isArchived(), is(false));
    }

    @Test
    public void shouldQueryEarliestStockMovementItemCreatedTimeByProgram() throws Exception {
        LMISTestApp.getInstance().setFeatureToggle(R.bool.feature_deactivate_program_product, true);

        Program mmia = new ProgramBuilder().setProgramCode("MMIA").build();
        Program via = new ProgramBuilder().setProgramCode("VIA").build();
        programRepository.createOrUpdate(mmia);
        programRepository.createOrUpdate(via);
        Product mmiaProduct = new ProductBuilder().setCode("B1").build();
        productRepository.createOrUpdate(mmiaProduct);
        Product viaProduct = new ProductBuilder().setCode("A1").build();
        productRepository.createOrUpdate(viaProduct);

        ArrayList<ProductProgram> productPrograms = new ArrayList<>();
        ProductProgram viaProductProgram = new ProductProgram();
        viaProductProgram.setProductCode(viaProduct.getCode());
        viaProductProgram.setProgramCode(via.getProgramCode());
        viaProductProgram.setActive(true);
        productPrograms.add(viaProductProgram);

        ProductProgram mmiaProductProgram = new ProductProgram();
        mmiaProductProgram.setProductCode(viaProduct.getCode());
        mmiaProductProgram.setProgramCode(via.getProgramCode());
        mmiaProductProgram.setActive(true);
        productPrograms.add(mmiaProductProgram);

        productProgramRepository.batchSave(productPrograms);

        StockCard stockCard = new StockCard();
        stockCard.setProduct(mmiaProduct);
        stockRepository.createOrUpdate(stockCard);
        StockCard stockCard2 = new StockCard();
        stockCard2.setProduct(viaProduct);
        stockRepository.createOrUpdate(stockCard2);

        createMovementItem(ISSUE, 100, stockCard, new DateTime("2017-01-01").toDate(), new DateTime("2017-01-01").toDate(), false);
        createMovementItem(ISSUE, 100, stockCard2, new DateTime("2018-01-01").toDate(), new DateTime("2018-01-01").toDate(), false);
        DateTime dateTime = new DateTime("2018-01-01");
        Date expectedDate = new DateTime().withDate(dateTime.getYear(), dateTime.getMonthOfYear(), dateTime.getDayOfMonth()).toDate();
        createMovementItem(ISSUE, 100, stockCard2, new DateTime("2018-03-02").toDate(), new DateTime("2018-03-02").toDate(), false);

        Date earliestDate = stockRepository.queryEarliestStockMovementDateByProgram(via.getProgramCode());
        Assert.assertThat(DateUtil.cutTimeStamp(new DateTime(earliestDate)), is(DateUtil.cutTimeStamp(new DateTime(expectedDate))));
    }

    @Test
    public void shouldQueryEarliestStockMovementDateByProgramWhenToggleOff() throws LMISException {
        LMISTestApp.getInstance().setFeatureToggle(R.bool.feature_deactivate_program_product, false);

        Program mmia = new ProgramBuilder().setProgramCode("MMIA").build();
        Program via = new ProgramBuilder().setProgramCode("VIA").build();
        programRepository.createOrUpdate(mmia);
        programRepository.createOrUpdate(via);
        Product mmiaProduct = new ProductBuilder().setProgram(mmia).setCode("B1").build();
        productRepository.createOrUpdate(mmiaProduct);
        Product viaProduct = new ProductBuilder().setProgram(via).setCode("A1").build();
        productRepository.createOrUpdate(viaProduct);
        StockCard stockCard = new StockCard();
        stockCard.setProduct(mmiaProduct);
        stockRepository.createOrUpdate(stockCard);
        StockCard stockCard2 = new StockCard();
        stockCard2.setProduct(viaProduct);
        stockRepository.createOrUpdate(stockCard2);

        createMovementItem(ISSUE, 100, stockCard, new DateTime("2017-01-01").toDate(), new DateTime("2017-01-01").toDate(), false);
        createMovementItem(ISSUE, 100, stockCard2, new DateTime("2018-01-01").toDate(), new DateTime("2018-01-01").toDate(), false);
        DateTime dateTime = new DateTime("2018-01-01");
        Date expectedDate = new DateTime().withDate(dateTime.getYear(), dateTime.getMonthOfYear(), dateTime.getDayOfMonth()).toDate();
        createMovementItem(ISSUE, 100, stockCard2, new DateTime("2018-03-02").toDate(), new DateTime("2018-03-02").toDate(), false);

        Date earliestDate = stockRepository.queryEarliestStockMovementDateByProgram(via.getProgramCode());
        Assert.assertThat(DateUtil.cutTimeStamp(new DateTime(earliestDate)), is(DateUtil.cutTimeStamp(new DateTime(expectedDate))));
    }

    @Test
    public void shouldLoadEmergencyProducts() throws Exception {
        //when
        createNewStockCard("code", null, ProductBuilder.create().setCode("p1").setIsActive(true).setIsKit(false).build(), true);
        createNewStockCard("otherCode", "parentCode", ProductBuilder.create().setCode("p2").setIsActive(true).setIsKit(false).build(), false);

        Product product = ProductBuilder.buildAdultProduct();
        product.setKit(true);
        productRepository.createOrUpdate(product);

        //then
        List<StockCard> stockCardsBeforeTimeLine = stockRepository.listEmergencyStockCards();
        assertThat(stockCardsBeforeTimeLine.size(), is(1));
    }

    @Test
    public void shouldKeepStockCardWhenItsInPeriod() {
        Program program = new Program();
        program.setId(123);
        program.setProgramCode(Constants.MMIA_PROGRAM_CODE);

        Date movementDate = DateUtil.parseString("2015-07-21", DateUtil.DB_DATE_FORMAT);
        Date createdTime = DateUtil.parseString("2015-07-21 11:00:00", DateUtil.DATE_TIME_FORMAT);

        DateTime periodBegin = new DateTime(DateUtil.parseString("2015-06-21 10:10:10", DateUtil.DATE_TIME_FORMAT));
        DateTime periodEnd = new DateTime(DateUtil.parseString("2015-07-21 11:11:11", DateUtil.DATE_TIME_FORMAT));
        RnRForm form = RnRForm.init(program, new Period(periodBegin, periodEnd), false);

        StockMovementItem movementItem = new StockMovementItem();
        movementItem.setMovementDate(movementDate);
        movementItem.setCreatedTime(createdTime);

        assertFalse(stockRepository.shouldRemoveStockCard(movementItem, form.getPeriodEnd()));
    }

    @Test
    public void shouldRemoveStockCardWhenItsOutPeriod() {
        Program program = new Program();
        program.setId(123);
        program.setProgramCode(Constants.MMIA_PROGRAM_CODE);

        Date movementDate = DateUtil.parseString("2015-07-21", DateUtil.DB_DATE_FORMAT);
        Date createdTime = DateUtil.parseString("2015-07-21 11:11:13", DateUtil.DATE_TIME_FORMAT);

        DateTime periodBegin = new DateTime(DateUtil.parseString("2015-06-21 10:10:10", DateUtil.DATE_TIME_FORMAT));
        DateTime periodEnd = new DateTime(DateUtil.parseString("2015-07-21 11:11:11", DateUtil.DATE_TIME_FORMAT));
        RnRForm form = RnRForm.init(program, new Period(periodBegin, periodEnd), false);

        StockMovementItem movementItem = new StockMovementItem();
        movementItem.setMovementDate(movementDate);
        movementItem.setCreatedTime(createdTime);

        assertTrue(stockRepository.shouldRemoveStockCard(movementItem, form.getPeriodEnd()));
    }

    private void saveDraftInventory() throws LMISException {
        DraftInventory draftInventory1 = new DraftInventory();
        draftInventory1.setQuantity(10L);
        draftInventory1.setExpireDates("11/10/2015");
        DraftInventory draftInventory2 = new DraftInventory();
        draftInventory2.setQuantity(20L);
        draftInventory2.setExpireDates("12/10/2015");

        stockRepository.saveDraftInventory(draftInventory1);
        stockRepository.saveDraftInventory(draftInventory2);
    }

    private void saveTestProduct() throws LMISException {
        product = new Product();
        product.setPrimaryName("Test Product");
        product.setStrength("200");
        product.setCode("test code");

        productRepository.createOrUpdate(product);
    }

    private StockMovementItem createMovementItem(StockMovementItem.MovementType type, long quantity, StockCard stockCard, Date createdTime, Date movementDate, boolean synced) throws LMISException {
        StockMovementItem stockMovementItem = new StockMovementItem();
        stockMovementItem.setMovementQuantity(quantity);
        stockMovementItem.setMovementType(type);
        stockMovementItem.setMovementDate(movementDate);
        stockMovementItem.setStockCard(stockCard);
        stockMovementItem.setSynced(synced);
        LMISTestApp.getInstance().setCurrentTimeMillis(createdTime.getTime());

        if (stockMovementItem.isPositiveMovement()) {
            stockMovementItem.setStockOnHand(stockCard.getStockOnHand() + quantity);
        } else {
            stockMovementItem.setStockOnHand(stockCard.getStockOnHand() - quantity);
        }

        stockCard.setStockOnHand(stockMovementItem.getStockOnHand());
        stockRepository.addStockMovementAndUpdateStockCard(stockMovementItem);
        stockRepository.refresh(stockCard);

        return stockMovementItem;
    }
}
