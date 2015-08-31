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

package org.openlmis.core.view.adapter;

import android.content.Context;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;
import org.openlmis.core.R;
import org.openlmis.core.model.StockCard;
import org.openlmis.core.presenter.StockMovementPresenter;
import org.openlmis.core.utils.DateUtil;
import org.openlmis.core.view.viewmodel.StockMovementViewModel;
import org.openlmis.core.view.widget.MovementTypeDialog;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.roboguice.shaded.goole.common.base.Preconditions.checkNotNull;


public class StockMovementAdapter extends BaseAdapter {

    List<StockMovementViewModel> stockMovementViewModels;

    LayoutInflater layoutInflater;

    Context context;

    MovementTypeDialog dialog;

    ViewHolder editableLine;
    private final StockCard stockCard;

    public StockMovementAdapter(Context context, StockMovementPresenter presenter, long stockId) {
        stockMovementViewModels = presenter.getStockMovementModels();
        if (stockMovementViewModels == null) {
            stockMovementViewModels = new ArrayList<>();
        }
        this.context = context;
        stockCard = presenter.getStockCard(stockId);
        layoutInflater = LayoutInflater.from(context);

        setupMovementTypeDialog();
    }

    @Override
    public int getCount() {
        return stockMovementViewModels.size() + 1;
    }

    @Override
    public StockMovementViewModel getItem(int position) {
        if (position >= stockMovementViewModels.size()) {
            return null;
        }
        return stockMovementViewModels.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.item_stock_movement, parent, false);
            holder = new ViewHolder(convertView);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        onBindViewHolder(holder, position);
        return convertView;
    }

    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final StockMovementViewModel model = getItem(position);

        holder.etDocumentNo.setEnabled(false);
        holder.etReceived.setEnabled(false);
        holder.etNegativeAdjustment.setEnabled(false);
        holder.etPositiveAdjustment.setEnabled(false);
        holder.etIssued.setEnabled(false);

        if (model != null) {
            holder.txMovementDate.setText(model.getMovementDate());
            holder.txReason.setText(model.getReason());
            holder.etDocumentNo.setText(model.getDocumentNo());
            holder.etReceived.setText(model.getReceived());
            holder.etNegativeAdjustment.setText(model.getNegativeAdjustment());
            holder.etPositiveAdjustment.setText(model.getPositiveAdjustment());
            holder.etIssued.setText(model.getIssued());
            holder.txStockExistence.setText(model.getStockExistence());
        } else {
            holder.txMovementDate.setText(StringUtils.EMPTY);
            holder.txReason.setText(StringUtils.EMPTY);
            holder.etDocumentNo.setText(StringUtils.EMPTY);
            holder.etReceived.setText(StringUtils.EMPTY);
            holder.etNegativeAdjustment.setText(StringUtils.EMPTY);
            holder.etPositiveAdjustment.setText(StringUtils.EMPTY);
            holder.etIssued.setText(StringUtils.EMPTY);
            holder.txStockExistence.setText(StringUtils.EMPTY);

            editableLine = holder;
        }

        holder.txReason.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (model == null) {
                    dialog.show();
                }
            }
        });

        MyOnKeyListener listener = new MyOnKeyListener(holder);
        holder.etReceived.setOnKeyListener(listener);
        holder.etNegativeAdjustment.setOnKeyListener(listener);
        holder.etPositiveAdjustment.setOnKeyListener(listener);
        holder.etIssued.setOnKeyListener(listener);
    }

    private long getCurrentStockOnHand() {
        return checkNotNull(stockCard).getStockOnHand();
    }

    private void setupMovementTypeDialog() {
        dialog = new MovementTypeDialog(context, new MovementTypeDialog.OnMovementSelectListener() {
            @Override
            public void onReceive() {
                editableLine.etReceived.setEnabled(true);
            }

            @Override
            public void onIssue() {
                editableLine.etIssued.setEnabled(true);
            }

            @Override
            public void onPositiveAdjustment() {
                editableLine.etPositiveAdjustment.setEnabled(true);
            }

            @Override
            public void onNegativeAdjustment() {
                editableLine.etNegativeAdjustment.setEnabled(true);
            }

            @Override
            public void onComplete(String result) {
                editableLine.etDocumentNo.setEnabled(true);
                editableLine.txReason.setText(result);
                editableLine.txMovementDate.setText(DateUtil.formatDate(new Date()));
            }
        });
    }

    public StockMovementViewModel getEditableStockMovement() {

        StockMovementViewModel item = new StockMovementViewModel();
        if (editableLine.etReceived.isEnabled()) {
            item.setReceived(editableLine.etReceived.getText().toString());
        } else if (editableLine.etNegativeAdjustment.isEnabled()) {
            item.setNegativeAdjustment(editableLine.etNegativeAdjustment.getText().toString());
        } else if (editableLine.etPositiveAdjustment.isEnabled()) {
            item.setPositiveAdjustment(editableLine.etPositiveAdjustment.getText().toString());
        } else {
            item.setIssued(editableLine.etIssued.getText().toString());
        }

        item.setDocumentNo(editableLine.etDocumentNo.getText().toString());
        item.setStockExistence(editableLine.txStockExistence.getText().toString());
        item.setReason(editableLine.txReason.getText().toString());
        item.setMovementDate(editableLine.txMovementDate.getText().toString());
        return item;
    }


    class ViewHolder {

        TextView txMovementDate;
        TextView txReason;
        EditText etDocumentNo;
        EditText etReceived;
        EditText etNegativeAdjustment;
        EditText etPositiveAdjustment;
        EditText etIssued;
        TextView txStockExistence;


        public ViewHolder(View view) {
            txMovementDate = (TextView) view.findViewById(R.id.tx_date);
            txReason = (TextView) view.findViewById(R.id.tx_reason);
            etDocumentNo = (EditText) view.findViewById(R.id.et_document_no);
            etReceived = (EditText) view.findViewById(R.id.et_received);
            etNegativeAdjustment = (EditText) view.findViewById(R.id.et_negative_adjustment);
            etPositiveAdjustment = (EditText) view.findViewById(R.id.et_positive_adjustment);
            etIssued = (EditText) view.findViewById(R.id.et_issued);
            txStockExistence = (TextView) view.findViewById(R.id.tx_stock_on_hand);
        }
    }

    class MyOnKeyListener implements View.OnKeyListener {

        ViewHolder viewHolder;

        public MyOnKeyListener(ViewHolder viewHolder) {
            this.viewHolder = viewHolder;
        }

        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            String text = ((TextView) v).getText().toString();
            long number = 0;
            if (!StringUtils.isEmpty(text)) {
                number = Long.parseLong(text);
            }

            if (v == viewHolder.etReceived || v == viewHolder.etPositiveAdjustment){
                viewHolder.txStockExistence.setText(String.valueOf(getCurrentStockOnHand() + number));
            } else {
                viewHolder.txStockExistence.setText(String.valueOf(getCurrentStockOnHand() - number));
            }

            return false;
        }
    }
}
