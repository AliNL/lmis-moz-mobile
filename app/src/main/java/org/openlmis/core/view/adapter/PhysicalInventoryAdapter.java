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
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputFilter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import org.openlmis.core.R;
import org.openlmis.core.utils.SimpleTextWatcher;
import org.openlmis.core.view.viewmodel.StockCardViewModel;
import org.openlmis.core.view.widget.InputFilterMinMax;

import java.util.List;


public class PhysicalInventoryAdapter extends InventoryListAdapter<PhysicalInventoryAdapter.ViewHolder> implements FilterableAdapter {


    public PhysicalInventoryAdapter(Context context, List<StockCardViewModel> data) {
        super(context, data);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view  = inflater.inflate(R.layout.item_physical_inventory, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        StockCardViewModel viewModel = currentList.get(position);
        EditTextWatcher textWatcher=new EditTextWatcher(viewModel);
        holder.etQuantity.removeTextChangedListener(textWatcher);

        holder.tvProductName.setText(viewModel.getStyledName());
        holder.tvProductUnit.setText(viewModel.getStyledUnit());
        holder.etQuantity.setText(viewModel.getQuantity());
        holder.etQuantity.addTextChangedListener(textWatcher);

        if (!viewModel.isValidate()) {
            holder.lyQuantity.setError(context.getResources().getString(R.string.msg_inventory_check_failed));
        } else {
            holder.lyQuantity.setErrorEnabled(false);
        }
    }


    class EditTextWatcher extends SimpleTextWatcher {

        private final StockCardViewModel viewModel;

        public EditTextWatcher(StockCardViewModel viewModel) {
            this.viewModel = viewModel;
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            return true;
        }

        @Override
        public void afterTextChanged(Editable editable) {
            viewModel.setQuantity(editable.toString());
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvProductName;
        TextView tvProductUnit;
        EditText etQuantity;
        TextView tvAddExpiryDate;
        TextInputLayout lyQuantity;

        public ViewHolder(View itemView) {
            super(itemView);

            tvProductName = (TextView)itemView.findViewById(R.id.product_name);
            tvProductUnit = (TextView)itemView.findViewById(R.id.product_unit);
            etQuantity = (EditText)itemView.findViewById(R.id.tx_quantity);
            lyQuantity = (TextInputLayout)itemView.findViewById(R.id.ly_quantity);
            tvAddExpiryDate = (TextView)itemView.findViewById(R.id.tx_expire_date);

            etQuantity.setFilters(new InputFilter[]{new InputFilterMinMax(Integer.MAX_VALUE)});
        }
    }
}
