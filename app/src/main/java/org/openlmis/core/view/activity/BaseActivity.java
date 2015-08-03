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


package org.openlmis.core.view.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import org.openlmis.core.R;
import org.openlmis.core.presenter.Presenter;
import org.openlmis.core.view.View;

import roboguice.activity.RoboActionBarActivity;

public abstract class BaseActivity extends RoboActionBarActivity implements View{


    private static final String MYPREFERENCE = "LMISPreference";
    SharedPreferences preferences;


    public abstract Presenter getPresenter();
    ProgressDialog loadingDialog;

    @Override
    protected void onStart() {
        super.onStart();
        getPresenter().onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        getPresenter().onStop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences(MYPREFERENCE, Context.MODE_PRIVATE);
        getPresenter().attachView(BaseActivity.this);


        if(getSupportActionBar() !=null){
            getSupportActionBar().setHomeButtonEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);

        SearchView searchView = (SearchView)menu.findItem(R.id.action_search).getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return onSearchStart(query);
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });


        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                return onSearchClosed();
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    public void startLoading() {
        if(loadingDialog == null){
            loadingDialog = new ProgressDialog(this);
            loadingDialog.setIndeterminate(false);
        }

        loadingDialog.show();
    }

    public void stopLoading() {
        if(loadingDialog !=null){
            loadingDialog.dismiss();
        }
    }

    public void saveString(String key, String value) {
        preferences.edit().putString(key,value).apply();
    }

    public void saveInt(String key, int value){
        preferences.edit().putInt(key, value).apply();
    }

    public void saveBoolean(String key, boolean value){
        preferences.edit().putBoolean(key, value).apply();
    }

    public SharedPreferences getPreferences(){
        return preferences;
    }

    public void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
    public void showMessage(int resId) {
        String msg = getResources().getString(resId);
        showMessage(msg);
    }

    public void showMessage(int resId, Object... args){
        String msg = getResources().getString(resId, args);
        showMessage(msg);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_search:
                return true;
            case R.id.action_settings:
                return onSettingClick();
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    public boolean onSearchStart(String query){
        return false;
    }

    public boolean onSearchClosed(){
        return false;
    }

    public boolean onSettingClick(){
        return false;
    }
}
