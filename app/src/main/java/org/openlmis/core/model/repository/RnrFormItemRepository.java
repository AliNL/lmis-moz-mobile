package org.openlmis.core.model.repository;

import android.content.Context;

import com.google.inject.Inject;
import com.j256.ormlite.dao.Dao;

import org.openlmis.core.exceptions.LMISException;
import org.openlmis.core.model.Product;
import org.openlmis.core.model.RnRForm;
import org.openlmis.core.persistence.DbUtil;
import org.openlmis.core.persistence.GenericDao;

import java.sql.SQLException;
import java.util.List;

public class RnrFormItemRepository {
    GenericDao<RnRForm.RnrFormItem> genericDao;

    @Inject
    DbUtil dbUtil;


    @Inject
    public RnrFormItemRepository(Context context) {
        this.genericDao = new GenericDao<>(RnRForm.RnrFormItem.class, context);
    }

    public List<RnRForm.RnrFormItem> queryListForLowStockByProductId(final Product product) throws LMISException {
        return dbUtil.withDao(RnRForm.RnrFormItem.class, new DbUtil.Operation<RnRForm.RnrFormItem, List<RnRForm.RnrFormItem>>() {
            @Override
            public List<RnRForm.RnrFormItem> operate(Dao<RnRForm.RnrFormItem, String> dao) throws SQLException {
                return dao.queryBuilder().orderBy("id", false).limit(3L).where().eq("product_id", product.getId()).and().ne("inventory", 0).query();
            }
        });
    }

    public void create(final List<RnRForm.RnrFormItem> rnrFormItemList) throws LMISException {
        dbUtil.withDaoAsBatch(RnRForm.RnrFormItem.class, new DbUtil.Operation<RnRForm.RnrFormItem, Void>() {
            @Override
            public Void operate(Dao<RnRForm.RnrFormItem, String> dao) throws SQLException {
                for (RnRForm.RnrFormItem item : rnrFormItemList) {
                    dao.create(item);
                }
                return null;
            }
        });
    }

    public void delete(final List<RnRForm.RnrFormItem> rnrFormItemListWrapper) throws LMISException {
        dbUtil.withDaoAsBatch(RnRForm.RnrFormItem.class, new DbUtil.Operation<RnRForm.RnrFormItem, Void>() {
            @Override
            public Void operate(Dao<RnRForm.RnrFormItem, String> dao) throws SQLException {
                for (RnRForm.RnrFormItem item : rnrFormItemListWrapper) {
                    dao.delete(item);
                }
                return null;
            }
        });
    }
}
