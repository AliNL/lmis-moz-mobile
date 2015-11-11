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

package org.openlmis.core.utils;

public final class Constants {
    // Intent Params
    public static final String PARAM_STOCK_CARD_ID = "stockCardId";
    public static final String PARAM_STOCK_NAME = "stockName";
    public static final String PARAM_IS_PHYSICAL_INVENTORY = "isPhysicalInventory";
    public static final String PARAM_IS_ADD_NEW_DRUG = "isAddNewDrug";
    public static final String PARAM_PROGRAM_CODE = "programCode";
    public static final String PARAM_FORM_ID = "formId";
    // Keys of Map
    public static final String KEY_HAS_DATA_CHANGED = "hasDataChanged";
    // Keys of SharedPreferences
    public static final String KEY_LAST_LOGIN_USER = "last_user";
    public static final String KEY_INIT_INVENTORY = "init_inventory";
    public static final String KEY_HAS_GET_PRODUCTS = "has_get_products";
    public static final String KEY_IS_REQUISITION_DATA_SYNCED = "is_requisition_data_synced";

    private Constants(){

    }
}
