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

package org.openlmis.core.view.viewmodel;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openlmis.core.LMISRepositoryUnitTest;
import org.openlmis.core.LMISTestRunner;
import org.openlmis.core.manager.MovementReasonManager;
import org.openlmis.core.model.StockMovementItem;
import org.openlmis.core.utils.DateUtil;

import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(LMISTestRunner.class)
public class StockMovementViewModelTest extends LMISRepositoryUnitTest{

    private StockMovementViewModel stockMovementViewModel;
    private MovementReasonManager.MovementReason movementReason;

    @Before
    public void setup() {
        stockMovementViewModel = new StockMovementViewModel();
        movementReason = new MovementReasonManager.MovementReason(StockMovementItem.MovementType.RECEIVE, "RECEIVE", "receive");
    }

    @Test
    public void shouldReturnValidWhenStockMovementViewModelHasAllData() {
        stockMovementViewModel.setMovementDate(DateUtil.formatDate(new Date()));
        stockMovementViewModel.setStockExistence("123");
        stockMovementViewModel.setDocumentNo("111");
        stockMovementViewModel.setReason(movementReason);
        stockMovementViewModel.setReceived("100");
        assertTrue(stockMovementViewModel.validateInputValid());
    }

    @Test
    public void shouldReturnFalseIfMovementDateIsMissing() {
        stockMovementViewModel.setStockExistence("123");
        stockMovementViewModel.setDocumentNo("111");
        stockMovementViewModel.setReason(movementReason);
        stockMovementViewModel.setReceived("100");
        assertFalse(stockMovementViewModel.validateEmpty());
    }

    @Test
    public void shouldReturnFalseIfReasonIsMissing() {
        stockMovementViewModel.setStockExistence("123");
        stockMovementViewModel.setDocumentNo("111");
        stockMovementViewModel.setMovementDate("2016-11-20");
        stockMovementViewModel.setReceived("100");
        assertFalse(stockMovementViewModel.validateEmpty());
    }

    @Test
    public void shouldReturnFalseIfAllQuantitiesAreEmpty() {
        stockMovementViewModel.setStockExistence("123");
        stockMovementViewModel.setDocumentNo("111");
        stockMovementViewModel.setReason(movementReason);
        stockMovementViewModel.setMovementDate("2016-11-20");
        assertFalse(stockMovementViewModel.validateEmpty());
    }

    @Test
    public void shouldSetRequestedAsNullWhenRequestedIsNull() throws Exception {
        stockMovementViewModel.setMovementDate(DateUtil.formatDate(new Date()));
        stockMovementViewModel.setStockExistence("123");
        stockMovementViewModel.setDocumentNo("111");
        stockMovementViewModel.setReason(movementReason);
        stockMovementViewModel.setReceived("100");
        StockMovementItem stockMovementItem = stockMovementViewModel.convertViewToModel();
        assertNull(stockMovementItem.getRequested());
    }

    @Test
    public void shouldSetRequestedAsNullWhenRequestedIsEmpty() throws Exception {
        stockMovementViewModel.setMovementDate(DateUtil.formatDate(new Date()));
        stockMovementViewModel.setStockExistence("123");
        stockMovementViewModel.setDocumentNo("111");
        stockMovementViewModel.setReason(movementReason = new MovementReasonManager.MovementReason(StockMovementItem.MovementType.ISSUE, "issue", "issue"));
        stockMovementViewModel.setIssued("100");
        stockMovementViewModel.setRequested("");
        StockMovementItem stockMovementItem = stockMovementViewModel.convertViewToModel();
        assertNull(stockMovementItem.getRequested());
    }

    @Test
    public void shouldReturnFalseIfReceivedIsZero() {
        stockMovementViewModel.setStockExistence("123");
        stockMovementViewModel.setDocumentNo("111");
        stockMovementViewModel.setReason(movementReason);
        stockMovementViewModel.setReceived("0");
        assertFalse(stockMovementViewModel.validateReceived());
    }

    @Test
    public void shouldReturnTrueIfReceivedIsZero() {
        stockMovementViewModel.setStockExistence("123");
        stockMovementViewModel.setDocumentNo("111");
        stockMovementViewModel.setReason(movementReason);
        stockMovementViewModel.setReceived("12");
        assertTrue(stockMovementViewModel.validateReceived());
    }

    @Test
    public void shouldSetRequestedCorrectlyWhenRequestedNotEmptyAndNotNull() throws Exception {
        stockMovementViewModel.setMovementDate(DateUtil.formatDate(new Date()));
        stockMovementViewModel.setStockExistence("123");
        stockMovementViewModel.setDocumentNo("111");
        stockMovementViewModel.setReason(movementReason = new MovementReasonManager.MovementReason(StockMovementItem.MovementType.ISSUE, "issue", "issue"));
        stockMovementViewModel.setIssued("100");
        stockMovementViewModel.setRequested("999");
        StockMovementItem stockMovementItem = stockMovementViewModel.convertViewToModel();
        assertThat(stockMovementItem.getRequested(), is(999L));
    }
}
