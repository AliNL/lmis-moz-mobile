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

package org.openlmis.core.model.repository;


import android.content.Context;

import com.google.inject.Inject;
import com.j256.ormlite.dao.Dao;

import org.openlmis.core.exceptions.LMISException;
import org.openlmis.core.model.User;
import org.openlmis.core.network.LMISRestManager;
import org.openlmis.core.persistence.DbUtil;
import org.openlmis.core.persistence.GenericDao;

import java.net.ConnectException;
import java.sql.SQLException;
import java.util.List;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class UserRepository extends LMISRestManager {

    @Inject
    DbUtil dbUtil;

    GenericDao<User> genericDao;


    @Inject
    public UserRepository(Context context){
        genericDao = new GenericDao<>(User.class, context);
    }

    public void authorizeUser(final User user, final NewCallback<User> callback) {
        lmisRestApi.authorizeUser(user, new Callback<UserResponse>() {
            @Override
            public void success(UserResponse userResponse, Response response) {
                if (userResponse.getUserInformation() !=null){
                    callback.success(userResponse.getUserInformation());
                } else {
                    callback.failure(null);
                }
            }

            @Override
            public void failure(RetrofitError error) {
                if (error.getCause() instanceof ConnectException){
                    callback.timeout(error.getMessage());
                }else {
                    callback.failure(error.getMessage());
                }
            }
        });
    }

    public User getUserFromLocal(final User user) {
        List<User> users = null;
        try {
            users = dbUtil.withDao(User.class, new DbUtil.Operation<User, List<User>>() {
                @Override
                public List<User> operate(Dao<User, String> dao) throws SQLException {
                    return dao.queryBuilder().where().eq("username", user.getUsername()).and().eq("password", user.getPassword()).query();
                }
            });
        } catch (LMISException e) {
            e.reportToFabric();
        }

        if (users !=null && users.size() >0){
            return users.get(0);
        } else {
            return null;
        }
    }

    public void save(final User user) {
        try {
            genericDao.createOrUpdate(user);
        } catch (LMISException e) {
            e.reportToFabric();
        }
    }


    public interface NewCallback<T> {
        void success(T t);
        void failure(String error);
        void timeout(String error);
    }
}
