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

package org.openlmis.core.model;


import com.google.inject.Inject;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;


import org.openlmis.core.exceptions.LMISException;
import org.openlmis.core.model.repository.RnrFormRepository;
import org.openlmis.core.persistence.DbUtil;

import java.sql.SQLException;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@DatabaseTable(tableName = "products")
public class Product extends BaseModel{

    public static final String MEDICINE_TYPE_ADULT="Adult";
    public static final String MEDICINE_TYPE_BABY="Baby";
    public static final String MEDICINE_TYPE_OTHER="Other";

    @Inject
    DbUtil dbUtil;

    @DatabaseField(foreign = true, foreignAutoRefresh = true)
    private Program program;

    @DatabaseField
    String primaryName;

    @DatabaseField
    String strength;

    @DatabaseField
    String code;

    @DatabaseField
    String type;

    String medicine_type;


    @Override
    public boolean equals(Object o) {
        if (o instanceof  Product){
            Product product = (Product)o;
            return product.getCode().equals(getCode());
        }else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return getCode().hashCode();
    }

    public List<RnRForm.RnrFormItem> queryListForLowStockByProductId() throws LMISException {
        return dbUtil.withDao(RnRForm.RnrFormItem.class, new DbUtil.Operation<RnRForm.RnrFormItem, List<RnRForm.RnrFormItem>>() {
            @Override
            public List<RnRForm.RnrFormItem> operate(Dao<RnRForm.RnrFormItem, String> dao) throws SQLException {
                return dao.queryBuilder().orderBy("id", false).limit(3L).where().eq("product_id", getId()).and().ne("inventory", 0).query();
            }
        });
    }
}
