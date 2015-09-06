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

import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;

import org.apache.commons.lang3.StringUtils;
import org.openlmis.core.LMISApp;
import org.openlmis.core.R;
import org.openlmis.core.model.Product;
import org.openlmis.core.model.StockCard;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

import static org.roboguice.shaded.goole.common.collect.Lists.newArrayList;

@Data
public class StockCardViewModel {

    long productId;

    String productName;
    String fnm;
    String strength;
    String type;

    String quantity;

    List<String> expiryDates;

    long stockCardId;
    long stockOnHand;

    SpannableStringBuilder styledName;
    SpannableStringBuilder styledUnit;

    boolean validate = true;
    boolean checked;

    public StockCardViewModel(StockCard stockCard){
        this.productName = stockCard.getProduct().getPrimaryName();
        this.fnm = stockCard.getProduct().getCode();
        this.strength = stockCard.getProduct().getStrength();
        this.stockCardId = stockCard.getId();
        this.productId = stockCard.getProduct().getId();

        Product product = stockCard.getProduct();
        formatProductDisplay(product);

        this.expiryDates = newArrayList(stockCard.getExpireDates().split(StockCard.DIVIDER));
        this.stockOnHand = stockCard.getStockOnHand();
        this.checked = true;
    }

    public StockCardViewModel(Product product){
        this.productName = product.getPrimaryName();
        this.fnm = product.getCode();
        this.strength = product.getStrength();
        this.type = product.getType();
        this.productId = product.getId();
        this.checked = false;
    }


    private void formatProductDisplay(Product product) {
        String productName = product.getPrimaryName() + " [" + product.getCode() + "]";
        styledName = new SpannableStringBuilder(productName);
        styledName.setSpan(new ForegroundColorSpan(LMISApp.getContext().getResources().getColor(R.color.secondary_text)),
                product.getPrimaryName().length(), productName.length(), Spannable.SPAN_POINT_MARK);

        String unit = product.getStrength() + " " + product.getType();
        styledUnit = new SpannableStringBuilder(unit);
        int length = 0;
        if (product.getStrength() != null) {
            length = product.getStrength().length();
        }
        styledUnit.setSpan(new ForegroundColorSpan(LMISApp.getContext().getResources().getColor(R.color.secondary_text)),
                length, unit.length(), Spannable.SPAN_POINT_MARK);
    }

    public String formatExpiryDateString(){
        if (expiryDates ==null){
            return StringUtils.EMPTY;
        }

        String expiryDateString = StringUtils.EMPTY;
        for (String date : expiryDates){
            expiryDateString += date;
        }

        return expiryDateString;
    }


    public String optFirstExpiryDate(){
        if (expiryDates !=null && expiryDates.size() >0){
            return expiryDates.get(0);
        }else {
            return StringUtils.EMPTY;
        }
    }

    public void addExpiryDate(String date){
        if (expiryDates == null){
            expiryDates = new ArrayList<>();
        }
        expiryDates.add(date);
    }


    public boolean validate() {
        validate = !checked || StringUtils.isNumeric(quantity);
        return validate;
    }
}
