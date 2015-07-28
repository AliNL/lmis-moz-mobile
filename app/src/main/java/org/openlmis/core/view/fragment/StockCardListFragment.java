package org.openlmis.core.view.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.inject.Inject;

import org.openlmis.core.R;
import org.openlmis.core.exceptions.LMISException;
import org.openlmis.core.model.StockCard;
import org.openlmis.core.model.repository.StockRepository;
import org.openlmis.core.view.adapter.StockCardListAdapter;

import java.util.ArrayList;
import java.util.List;

import roboguice.fragment.RoboFragment;

public class StockCardListFragment extends RoboFragment {


    RecyclerView inventoryList;

    @Inject
    StockRepository stockRepository;

    List<StockCard> stockCardList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_inventory_list, null);
        inventoryList = (RecyclerView)view.findViewById(R.id.products_list);

        inventoryList.setHasFixedSize(true);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(container.getContext());
        inventoryList.setLayoutManager(mLayoutManager);

        inventoryList.setAdapter(new StockCardListAdapter(stockCardList));

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try{
            stockCardList = stockRepository.getStockCards();
        }catch (LMISException e){
            e.printStackTrace();
        }

    }
}
