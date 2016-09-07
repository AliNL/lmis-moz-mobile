package org.openlmis.core.view.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import org.openlmis.core.R;
import org.openlmis.core.model.StockCard;
import org.openlmis.core.presenter.InitialInventoryPresenter;
import org.openlmis.core.utils.Constants;
import org.openlmis.core.utils.InjectPresenter;
import org.openlmis.core.utils.ToastUtil;
import org.openlmis.core.view.adapter.InitialInventoryAdapter;
import org.openlmis.core.view.holder.InitialInventoryViewHolder;

import roboguice.inject.ContentView;
import rx.Subscription;

@ContentView(R.layout.activity_inventory)
public class InitialInventoryActivity extends InventoryActivity {
    @InjectPresenter(InitialInventoryPresenter.class)
    InitialInventoryPresenter presenter;

    protected boolean isAddNewDrug;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        isAddNewDrug = getIntent().getBooleanExtra(Constants.PARAM_IS_ADD_NEW_DRUG, false);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void initUI() {
        super.initUI();
        btnSave.setVisibility(View.GONE);
        btnDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validateInventory()) {
                    loading();
                    presenter.doInventory();
                }
            }
        });
        if (isAddNewDrug) {
            setTitle(getResources().getString(R.string.title_add_new_drug));
        } else if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }

        initRecyclerView();
        Subscription subscription = presenter.loadInventory().subscribe(populateInventorySubscriber);
        subscriptions.add(subscription);
    }

    @Override
    protected void initRecyclerView() {
        mAdapter = new InitialInventoryAdapter(presenter.getInventoryViewModelList(), viewHistoryListener);
        productListRecycleView.setAdapter(mAdapter);
    }

    @Override
    public void goToParentPage() {
        goToMainPage();
    }

    @Override
    public void onBackPressed() {
        if (isSearchViewActivity()) {
            searchView.onActionViewCollapsed();
            return;
        }
        if (!isAddNewDrug) {
            ToastUtil.show(R.string.msg_save_before_exit);
            return;
        }
        super.onBackPressed();
    }

    private void goToMainPage() {
        preferencesMgr.setIsNeedsInventory(false);
        startActivity(isAddNewDrug ? StockCardListActivity.getIntentToMe(this) : HomeActivity.getIntentToMe(this));
        this.finish();
    }

    public static Intent getIntentToMe(Context context, boolean isAddNewDrug) {
        return new Intent(context, InitialInventoryActivity.class)
                .putExtra(Constants.PARAM_IS_ADD_NEW_DRUG, isAddNewDrug);
    }

    public static Intent getIntentToMe(Context context) {
        return getIntentToMe(context, false);
    }

    protected InitialInventoryViewHolder.ViewHistoryListener viewHistoryListener = new InitialInventoryViewHolder.ViewHistoryListener() {
        @Override
        public void viewHistory(StockCard stockCard) {
            startActivity(StockMovementHistoryActivity.getIntentToMe(InitialInventoryActivity.this,
                    stockCard.getId(),
                    stockCard.getProduct().getFormattedProductName(),
                    true,
                    false));
        }
    };

}
