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

package org.openlmis.core.manager;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class SharedPreferenceMgr {

    public static final String MY_PREFERENCE = "LMISPreference";
    SharedPreferences sharedPreferences;

    public static final String KEY_LAST_SYNCED_TIME = "lastSyncedDate";
    public static final String KEY_LAST_LOGIN_USER = "last_user";
    public static final String KEY_INIT_INVENTORY = "init_inventory";
    public static final String KEY_HAS_GET_PRODUCTS = "has_get_products";
    public static final String KEY_HAS_GET_STOCK = "has_get_stock_cards_synced";
    public static final String KEY_IS_REQUISITION_DATA_SYNCED = "is_requisition_data_synced";

    @Inject
    public SharedPreferenceMgr(Context context) {
        sharedPreferences = context.getSharedPreferences(MY_PREFERENCE, Context.MODE_PRIVATE);
    }

    public SharedPreferences getPreference() {
        return sharedPreferences;
    }

    public boolean hasSyncedVersion() {
        return sharedPreferences.getBoolean(UserInfoMgr.getInstance().getVersion(), false);
    }

    public void setSyncedVersion(boolean hasUpdated) {
        sharedPreferences.edit().putBoolean(UserInfoMgr.getInstance().getVersion(), hasUpdated).apply();
    }
}
