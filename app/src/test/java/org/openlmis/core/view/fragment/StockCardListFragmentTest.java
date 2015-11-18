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

package org.openlmis.core.view.fragment;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openlmis.core.LMISTestRunner;
import org.openlmis.core.model.Product;
import org.openlmis.core.model.StockCard;
import org.openlmis.core.presenter.StockCardPresenter;
import org.openlmis.core.view.adapter.StockCardListAdapter;
import org.robolectric.shadows.support.v4.SupportFragmentTestUtil;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(LMISTestRunner.class)
public class StockCardListFragmentTest {

    private StockCardListFragment fragment;
    private List<StockCard> stockCards;

    @Before
    public void setUp() {
        fragment = new StockCardListFragment();
        SupportFragmentTestUtil.startFragment(fragment);

        fragment.presenter = mock(StockCardPresenter.class);
        fragment.mAdapter = mock(StockCardListAdapter.class);

        stockCards = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            StockCard stockCard = new StockCard();
            stockCard.setStockOnHand(10 - i);
            Product product = new Product();
            product.setPrimaryName((char) ('A' + i) + " Product");

            stockCard.setProduct(product);
            stockCards.add(stockCard);
        }
    }

    @Test
    public void shouldSortListWhenSelectSortSpinner() {
        when(fragment.presenter.getStockCards()).thenReturn(stockCards);
        fragment.sortSpinner.setSelection(0);
        verify(fragment.mAdapter).sortByName(true);

        fragment.sortSpinner.setSelection(1);
        verify(fragment.mAdapter).sortByName(false);

        fragment.sortSpinner.setSelection(2);
        verify(fragment.mAdapter).sortByName(false);

        fragment.sortSpinner.setSelection(3);
        verify(fragment.mAdapter).sortBySOH(true);
    }

    @Test
    public void shouldSortListByProductName() {
        when(fragment.presenter.getStockCards()).thenReturn(stockCards);
        List<StockCard> stockCards = fragment.presenter.getStockCards();
        StockCardListAdapter adapter = new StockCardListAdapter(stockCards, null);
        adapter.sortByName(true);

        List<StockCard> sortedList = adapter.getCurrentStockCards();
        assertThat(sortedList.get(0).getProduct().getPrimaryName(), is("A Product"));
        assertThat(sortedList.get(1).getProduct().getPrimaryName(), is("B Product"));
        assertThat(sortedList.get(2).getProduct().getPrimaryName(), is("C Product"));
    }

    @Test
    public void shouldSortListBySOH() {
        when(fragment.presenter.getStockCards()).thenReturn(stockCards);
        StockCardListAdapter adapter = new StockCardListAdapter(stockCards, null);
        adapter.sortBySOH(true);

        List<StockCard> sortedList = adapter.getCurrentStockCards();
        assertThat(sortedList.get(0).getStockOnHand(), is(1L));
        assertThat(sortedList.get(1).getStockOnHand(), is(2L));
        assertThat(sortedList.get(2).getStockOnHand(), is(3L));
    }
}
