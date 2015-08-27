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


import android.view.Menu;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openlmis.core.LMISTestRunner;
import org.openlmis.core.model.Product;
import org.openlmis.core.model.StockCard;
import org.openlmis.core.presenter.StockCardListPresenter;
import org.openlmis.core.view.activity.StockCardListActivity;
import org.openlmis.core.view.activity.StockMovementActivity;
import org.openlmis.core.view.adapter.StockCardListAdapter;
import static org.robolectric.util.FragmentTestUtil.startFragment;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(LMISTestRunner.class)
public class StockCardListFragmentTest {

    StockCardListFragment stockCardListFragment;
    List<StockCard> stockCards;

    @Before
    public void setUp() {
        stockCardListFragment = new StockCardListFragment();
        startFragment(stockCardListFragment);
        stockCardListFragment.presenter = mock(StockCardListPresenter.class);
        stockCardListFragment.mAdapter =  mock(StockCardListAdapter.class);

        stockCards = new ArrayList<>();
        for (int i=0; i< 10 ;i ++){
            StockCard stockCard = new StockCard();
            stockCard.setStockOnHand(10 -i);
            Product product = new Product();
            product.setPrimaryName((char)('A' + i) + " Product");

            stockCard.setProduct(product);
            stockCards.add(stockCard);
        }
    }

    @Test
    public  void shouldSortListWhenSelectSortSpinner(){
        when(stockCardListFragment.presenter.getStockCards()).thenReturn(stockCards);
        stockCardListFragment.sortSpinner.setSelection(0);
        verify(stockCardListFragment.mAdapter).sortByName(true);

        stockCardListFragment.sortSpinner.setSelection(1);
        verify(stockCardListFragment.mAdapter).sortByName(false);

        stockCardListFragment.sortSpinner.setSelection(2);
        verify(stockCardListFragment.mAdapter).sortByName(false);

        stockCardListFragment.sortSpinner.setSelection(3);
        verify(stockCardListFragment.mAdapter).sortBySOH(true);
    }

    @Test
    public void shouldSortListByProductName(){
        when(stockCardListFragment.presenter.getStockCards()).thenReturn(stockCards);
        StockCardListAdapter adapter = new StockCardListAdapter(stockCardListFragment.presenter, StockMovementActivity.class.getName());
        adapter.sortByName(true);

        List<StockCard> sortedList = adapter.getCurrentStockCards();
        assertThat(sortedList.get(0).getProduct().getPrimaryName(), is("A Product"));
        assertThat(sortedList.get(1).getProduct().getPrimaryName(), is("B Product"));
        assertThat(sortedList.get(2).getProduct().getPrimaryName(), is("C Product"));
    }

    @Test
    public void shouldSortListBySOH(){
        when(stockCardListFragment.presenter.getStockCards()).thenReturn(stockCards);
        StockCardListAdapter adapter = new StockCardListAdapter(stockCardListFragment.presenter, StockMovementActivity.class.getName());
        adapter.sortBySOH(true);

        List<StockCard> sortedList = adapter.getCurrentStockCards();
        assertThat(sortedList.get(0).getStockOnHand(), is(1));
        assertThat(sortedList.get(1).getStockOnHand(), is(2));
        assertThat(sortedList.get(2).getStockOnHand(), is(3));
    }


    static class StockCardListActivityMock extends StockCardListActivity {
        @Override
        public boolean onCreateOptionsMenu(Menu menu) {
            return false;
        }
    }

}
