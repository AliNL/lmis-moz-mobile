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
package org.openlmis.core.view.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.openlmis.core.R;
import org.openlmis.core.model.Product;
import org.openlmis.core.model.RnrFormItem;
import org.openlmis.core.utils.DateUtil;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class MMIARnrForm extends LinearLayout {
    private ViewGroup leftViewGroup;
    private ViewGroup rightViewGroup;
    private LayoutInflater layoutInflater;
    private ViewGroup vg_right_scrollview;
    ArrayList<RnrFormItem> rnrFormItemList = new ArrayList<>();
    private HashMap<String, List<String>> rnrFormItemConfigList = new HashMap<>();

    public MMIARnrForm(Context context) {
        super(context);
        init(context);
    }

    public MMIARnrForm(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        layoutInflater = LayoutInflater.from(context);
        View container = layoutInflater.inflate(R.layout.view_mmia_rnr_form, this, true);
        vg_right_scrollview = (ViewGroup) container.findViewById(R.id.vg_right_scrollview);
        leftViewGroup = (ViewGroup) container.findViewById(R.id.rnr_from_list_product_name);
        rightViewGroup = (ViewGroup) container.findViewById(R.id.rnr_from_list);
    }

    public void initView(ArrayList<RnrFormItem> list) {

        ArrayList<RnrFormItem> dataList = new ArrayList<>();
        dataList.addAll(list);
        sortAndSetType(dataList);

        View leftHeaderView = addLeftHeaderView();
        ViewGroup rightHeaderView = addRightHeaderView();
        setItemSize(leftHeaderView, rightHeaderView);
        for (RnrFormItem item : rnrFormItemList) {
            if (item != null) {
                View leftView = addLeftView(item);
                ViewGroup rightView = addRightView(item);
                setItemSize(leftView, rightView);
            }
        }
    }

    private void sortAndSetType(ArrayList<RnrFormItem> rnrFormItemList) {
        initRnrFormItemConfigList();
        setMedicineType(rnrFormItemList, Product.MEDICINE_TYPE_ADULT);
        setMedicineType(rnrFormItemList, Product.MEDICINE_TYPE_BABY);
        setMedicineType(rnrFormItemList, Product.MEDICINE_TYPE_OTHER);
    }

    private void setMedicineType(ArrayList<RnrFormItem> rnrFormItemList, String medicineTypeName) {
        List<String> medicineType = rnrFormItemConfigList.get(medicineTypeName);
        for (RnrFormItem item : rnrFormItemList) {
            for (String FNM : medicineType) {
                if (FNM.equals(item.getProduct().getCode())) {
                    item.getProduct().setMedicine_type(medicineTypeName);
                    rnrFormItemList.add(item);
                }
            }
        }
    }

    public void initRnrFormItemConfigList() {
        rnrFormItemConfigList.put(Product.MEDICINE_TYPE_ADULT, Arrays.asList(getResources().getStringArray(R.array.medicine_adult)));
        rnrFormItemConfigList.put(Product.MEDICINE_TYPE_BABY, Arrays.asList(getResources().getStringArray(R.array.medicine_baby)));
        rnrFormItemConfigList.put(Product.MEDICINE_TYPE_OTHER, Arrays.asList(getResources().getStringArray(R.array.medicine_other)));
    }

    private void setItemSize(final View leftView, final ViewGroup rightView) {
        rightView.post(new Runnable() {
            @Override
            public void run() {
                setRightItemWidth(rightView);
                syncItemHeight(leftView, rightView);
            }
        });
    }

    private void syncItemHeight(final View leftView, final View rightView) {
        rightView.post(new Runnable() {
            @Override
            public void run() {
                int leftHeight = leftView.getHeight();
                int rightHeight = rightView.getHeight();
                if (leftHeight > rightHeight) {
                    rightView.getLayoutParams().height = leftHeight;
                } else {
                    leftView.getLayoutParams().height = rightHeight;
                }
            }
        });
    }

    private ViewGroup addRightView(RnrFormItem item) {
        return addRightView(item, false);
    }

    private View addLeftView(RnrFormItem item) {
        return addLeftView(item, false);
    }

    private View addLeftHeaderView() {
        return addLeftView(null, true);
    }

    private View inflaterLeftView() {
        return layoutInflater.inflate(R.layout.item_rnr_from_product_name, this, false);
    }

    private View addLeftView(RnrFormItem item, boolean isHeaderView) {
        View view = inflaterLeftView();
        TextView tvPrimaryName = (TextView) view.findViewById(R.id.tv_primary_name);
        if (isHeaderView) {
            tvPrimaryName.setText(R.string.label_rnrfrom_left_header);
            tvPrimaryName.setGravity(Gravity.CENTER);
            view.setBackgroundResource(R.color.color_mmia_info_name);
        } else {
            Product product = item.getProduct();
            tvPrimaryName.setText(product.getPrimaryName());
            setLeftViewColor(item, view);
        }

        leftViewGroup.addView(view);
        return view;
    }

    private void setLeftViewColor(RnrFormItem item, View view) {
        switch (item.getProduct().getMedicine_type()) {
            case Product.MEDICINE_TYPE_ADULT:
                view.setBackgroundResource(R.color.color_regime_adult);
                break;
            case Product.MEDICINE_TYPE_BABY:
                view.setBackgroundResource(R.color.color_regime_baby);
                break;
            case Product.MEDICINE_TYPE_OTHER:
                view.setBackgroundResource(R.color.color_regime_other);
                break;
            default:
                break;
        }
    }

    private ViewGroup addRightHeaderView() {
        return addRightView(null, true);
    }

    private ViewGroup addRightView(RnrFormItem item, boolean isHeaderView) {
        ViewGroup inflate = (ViewGroup) layoutInflater.inflate(R.layout.item_rnr_from, this, false);

        TextView tvIssuedUnit = (TextView) inflate.findViewById(R.id.tv_issued_unit);
        TextView tvInitialAmount = (TextView) inflate.findViewById(R.id.tv_initial_amount);
        TextView tvReceived = (TextView) inflate.findViewById(R.id.tv_received);
        TextView tvIssued = (TextView) inflate.findViewById(R.id.tv_issued);
        TextView tvAdjustment = (TextView) inflate.findViewById(R.id.tv_adjustment);
        TextView tvInventory = (TextView) inflate.findViewById(R.id.tv_inventory);
        TextView tvValidate = (TextView) inflate.findViewById(R.id.tv_validate);

        if (isHeaderView) {
            tvIssuedUnit.setText(R.string.label_issued_unit);
            tvInitialAmount.setText(R.string.label_initial_amount);
            tvReceived.setText(R.string.label_received);
            tvIssued.setText(R.string.label_issued);
            tvAdjustment.setText(R.string.label_adjustment);
            tvInventory.setText(R.string.label_inventory);
            tvValidate.setText(R.string.label_validate);

            inflate.setBackgroundResource(R.color.color_mmia_info_name);
        } else {
            //TODO refactor api field tvIssuedUnit
            tvIssuedUnit.setText(String.valueOf(item.getProduct().getStrength()));
            tvInitialAmount.setText(String.valueOf(item.getInitialAmount()));
            tvReceived.setText(String.valueOf(item.getReceived()));
            tvIssued.setText(String.valueOf(item.getIssued()));
            tvAdjustment.setText(String.valueOf(item.getAdjustment()));
            tvInventory.setText(String.valueOf(item.getInventory()));

            try {
                if (item.getValidate() != null) {
                    tvValidate.setText(DateUtil.convertDate(item.getValidate(), "dd/MM/yyyy", "MMM - yy"));
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        rightViewGroup.addView(inflate);
        return inflate;
    }

    private void setRightItemWidth(final ViewGroup rightView) {
        int rightWidth = vg_right_scrollview.getWidth();
        int rightViewGroupWidth = rightViewGroup.getWidth();

        if (rightViewGroupWidth < rightWidth) {
            int childCount = rightView.getChildCount();
            for (int i = 0; i < childCount; i++) {
                rightView.getChildAt(i).getLayoutParams().width = getRightViewWidth(rightWidth, childCount);
            }
            rightView.getChildAt(0).getLayoutParams().width = getRightViewWidth(rightWidth, childCount) + getRightViewRemainderWidth(rightWidth, childCount);
        }
    }

    private int getRightViewWidth(int rightWidth, int childCount) {
        return (rightWidth - (childCount - 1) * getDividerWidth()) / childCount;
    }

    private int getRightViewRemainderWidth(int rightWidth, int childCount) {
        return (rightWidth - (childCount - 1) * getDividerWidth()) % childCount;
    }

    private int getDividerWidth() {
        return (int) getResources().getDimension(R.dimen.divider);
    }


}
