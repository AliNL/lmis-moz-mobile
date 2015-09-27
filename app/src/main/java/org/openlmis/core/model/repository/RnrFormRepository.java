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
import com.j256.ormlite.misc.TransactionManager;

import org.openlmis.core.exceptions.LMISException;
import org.openlmis.core.exceptions.PeriodNotUniqueException;
import org.openlmis.core.model.BaseInfoItem;
import org.openlmis.core.model.Product;
import org.openlmis.core.model.Program;
import org.openlmis.core.model.Regimen;
import org.openlmis.core.model.RnRForm;
import org.openlmis.core.model.StockCard;
import org.openlmis.core.persistence.DbUtil;
import org.openlmis.core.persistence.GenericDao;
import org.openlmis.core.persistence.LmisSqliteOpenHelper;
import org.openlmis.core.utils.DateUtil;
import org.roboguice.shaded.goole.common.base.Predicate;
import org.roboguice.shaded.goole.common.collect.FluentIterable;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;


public class RnrFormRepository {

    private static final String TAG = "RnrFormRepository";

    @Inject
    DbUtil dbUtil;

    @Inject
    StockRepository stockRepository;

    @Inject
    RegimenRepository regimenRepository;

    @Inject
    RnrFormItemRepository rnrFormItemRepository;

    @Inject
    ProgramRepository programRepository;

    GenericDao<RnRForm> genericDao;
    GenericDao<RnRForm.RnrFormItem> rnrFormItemGenericDao;

    private Context context;

    @Inject
    public RnrFormRepository(Context context) {
        genericDao = new GenericDao<>(RnRForm.class, context);
        rnrFormItemGenericDao = new GenericDao<>(RnRForm.RnrFormItem.class, context);
        this.context = context;
    }


    public RnRForm initRnrForm(final Program program) throws LMISException {
        if (program == null) {
            throw new LMISException("Program cannot be null !");
        }
        final RnRForm form = RnRForm.init(program, DateUtil.today());
        try {
            TransactionManager.callInTransaction(LmisSqliteOpenHelper.getInstance(context).getConnectionSource(), new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    create(form);
                    createRnrFormItems(generateRnrFormItems(form));
                    createRegimenItems(generateRegimeItems(form));
                    createBaseInfoItems(generateBaseInfoItems(form));
                    genericDao.refresh(form);
                    return null;
                }
            });
        } catch (SQLException e) {
            throw new LMISException(e);
        }
        return form;
    }

    protected void create(RnRForm rnRForm) throws LMISException {
        genericDao.create(rnRForm);
    }

    public void save(final RnRForm form) throws LMISException {
        try {
            TransactionManager.callInTransaction(LmisSqliteOpenHelper.getInstance(context).getConnectionSource(), new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    updateWrapperList(form);
                    genericDao.update(form);
                    return null;
                }
            });
        } catch (SQLException e) {
            throw new LMISException(e);
        }
    }

    public void authorise(RnRForm form) throws LMISException {
        if (!isPeriodUnique(form)) {
            throw new PeriodNotUniqueException("Already have a authorized form");
        }

        form.setStatus(RnRForm.STATUS.AUTHORIZED);
        save(form);
    }

    protected boolean isPeriodUnique(final RnRForm form) {
        try {
            RnRForm rnRForm = dbUtil.withDao(RnRForm.class, new DbUtil.Operation<RnRForm, RnRForm>() {
                @Override
                public RnRForm operate(Dao<RnRForm, String> dao) throws SQLException {
                    return dao.queryBuilder().where().eq("program_id", form.getProgram().getId()).and().eq("status", RnRForm.STATUS.AUTHORIZED).and().eq("periodBegin", form.getPeriodBegin()).and().eq("periodEnd", form.getPeriodEnd()).queryForFirst();
                }
            });

            return rnRForm == null;
        } catch (LMISException e) {
            e.printStackTrace();
        }
        return false;
    }


    public void updateWrapperList(RnRForm form) throws SQLException {
        for (RnRForm.RnrFormItem item : form.getRnrFormItemListWrapper()) {
            form.getRnrFormItemList().update(item);
        }
        for (Regimen.RegimenItem item : form.getRegimenItemListWrapper()) {
            form.getRegimenItemList().update(item);
        }
        for (BaseInfoItem item : form.getBaseInfoItemListWrapper()) {
            form.getBaseInfoItemList().update(item);
        }
    }

    public List<RnRForm> list() throws LMISException {
        return genericDao.queryForAll();
    }

    public List<RnRForm> listUnSynced() throws LMISException {
        return dbUtil.withDao(RnRForm.class, new DbUtil.Operation<RnRForm, List<RnRForm>>() {
            @Override
            public List<RnRForm> operate(Dao<RnRForm, String> dao) throws SQLException {
                return dao.queryBuilder().where().eq("synced", false).and().eq("status", RnRForm.STATUS.AUTHORIZED).query();
            }
        });
    }

    public List<RnRForm> listMMIA() throws LMISException {
        final long mmiaId = programRepository.queryByCode(MMIARepository.MMIA_PROGRAM_CODE).getId();

        return dbUtil.withDao(RnRForm.class, new DbUtil.Operation<RnRForm, List<RnRForm>>() {
            @Override
            public List<RnRForm> operate(Dao<RnRForm, String> dao) throws SQLException {
                return dao.queryBuilder().where().eq("program_id", mmiaId).query();
            }
        });
    }

    public RnRForm queryDraft(final Program program) throws LMISException {
        if (program == null) {
            throw new LMISException("Program cannot be null !");
        }
        return dbUtil.withDao(RnRForm.class, new DbUtil.Operation<RnRForm, RnRForm>() {
            @Override
            public RnRForm operate(Dao<RnRForm, String> dao) throws SQLException {
                return dao.queryBuilder().where().eq("program_id", program.getId()).and().eq("status", RnRForm.STATUS.DRAFT).queryForFirst();
            }
        });
    }

    public RnRForm queryRnRForm(final long id) throws LMISException {
        return dbUtil.withDao(RnRForm.class, new DbUtil.Operation<RnRForm, RnRForm>() {
            @Override
            public RnRForm operate(Dao<RnRForm, String> dao) throws SQLException {
                return dao.queryBuilder().where().eq("id", id).queryForFirst();
            }
        });
    }


    public void approve(RnRForm form) throws LMISException {
        form.setStatus(RnRForm.STATUS.AUTHORIZED);
        genericDao.update(form);
    }

    private void createRnrFormItems(List<RnRForm.RnrFormItem> form) throws LMISException {
        rnrFormItemRepository.create(form);
    }

    public void createRegimenItems(final List<Regimen.RegimenItem> regimenItemList) throws LMISException {
        dbUtil.withDao(Regimen.RegimenItem.class, new DbUtil.Operation<Regimen.RegimenItem, Void>() {
            @Override
            public Void operate(Dao<Regimen.RegimenItem, String> dao) throws SQLException {
                for (Regimen.RegimenItem item : regimenItemList) {
                    dao.create(item);
                }
                return null;
            }
        });
    }

    public void createBaseInfoItems(final List<BaseInfoItem> baseInfoItemList) throws LMISException {
        dbUtil.withDaoAsBatch(BaseInfoItem.class, new DbUtil.Operation<BaseInfoItem, Void>() {
            @Override
            public Void operate(Dao<BaseInfoItem, String> dao) throws SQLException {
                for (BaseInfoItem item : baseInfoItemList) {
                    dao.create(item);
                }
                return null;
            }
        });
    }


    protected List<BaseInfoItem> generateBaseInfoItems(RnRForm form) {
        return null;
    }

    protected List<RnRForm.RnrFormItem> generateRnrFormItems(final RnRForm form) throws LMISException {
        List<StockCard> stockCards = FluentIterable.from(stockRepository.list(form.getProgram().getProgramCode())).filter(new Predicate<StockCard>() {
            @Override
            public boolean apply(StockCard stockCard) {
                return !form.getPeriodEnd().before(stockCard.getCreatedAt());
            }
        }).toList();

        List<RnRForm.RnrFormItem> rnrFormItems = new ArrayList<>();

        for (StockCard stockCard : stockCards) {
            RnRForm.RnrFormItem rnrFormItem = createRnrFormItemByPeriod(stockCard, form.getPeriodBegin(), form.getPeriodEnd());
            rnrFormItem.setForm(form);
            rnrFormItems.add(rnrFormItem);
        }
        return rnrFormItems;
    }

    private RnRForm.RnrFormItem createRnrFormItemByPeriod(StockCard stockCard, Date startDate, Date endDate) throws LMISException {
        List<StockCard.StockMovementItem> stockMovementItems = stockRepository.queryStockItems(stockCard, startDate, endDate);

        RnRForm.RnrFormItem rnrFormItem = new RnRForm.RnrFormItem();
        if (!stockMovementItems.isEmpty()) {

            StockCard.StockMovementItem firstItem = stockMovementItems.get(0);
            if (firstItem.getMovementType() == StockCard.StockMovementItem.MovementType.ISSUE
                    || firstItem.getMovementType() == StockCard.StockMovementItem.MovementType.NEGATIVE_ADJUST) {

                rnrFormItem.setInitialAmount(firstItem.getStockOnHand() + firstItem.getMovementQuantity());
            } else {
                rnrFormItem.setInitialAmount(firstItem.getStockOnHand() - firstItem.getMovementQuantity());
            }

            long totalReceived = 0;
            long totalIssued = 0;
            long totalAdjustment = 0;

            for (StockCard.StockMovementItem item : stockMovementItems) {
                if (StockCard.StockMovementItem.MovementType.RECEIVE == item.getMovementType()) {
                    totalReceived += item.getMovementQuantity();
                } else if (StockCard.StockMovementItem.MovementType.ISSUE == item.getMovementType()) {
                    totalIssued += item.getMovementQuantity();
                } else if (StockCard.StockMovementItem.MovementType.NEGATIVE_ADJUST == item.getMovementType()) {
                    totalAdjustment -= item.getMovementQuantity();
                } else if (StockCard.StockMovementItem.MovementType.POSITIVE_ADJUST == item.getMovementType()) {
                    totalAdjustment += item.getMovementQuantity();
                }
            }
            rnrFormItem.setProduct(stockCard.getProduct());
            rnrFormItem.setReceived(totalReceived);
            rnrFormItem.setIssued(totalIssued);
            rnrFormItem.setAdjustment(totalAdjustment);

            Long inventory = stockMovementItems.get(stockMovementItems.size() - 1).getStockOnHand();
            rnrFormItem.setInventory(inventory);
            rnrFormItem.setValidate(stockCard.getEarliestExpireDate());

            Long totalRequest = totalIssued * 2 - inventory;
            totalRequest = totalRequest > 0 ? totalRequest : 0;
            rnrFormItem.setCalculatedOrderQuantity(totalRequest);

        } else {
            rnrFormItem.setInitialAmount(stockCard.getStockOnHand());
            rnrFormItem.setProduct(stockCard.getProduct());
            rnrFormItem.setReceived(0);
            rnrFormItem.setIssued(0);
            rnrFormItem.setAdjustment(0);
            rnrFormItem.setInventory(stockCard.getStockOnHand());
            rnrFormItem.setValidate(stockCard.getEarliestExpireDate());
            rnrFormItem.setCalculatedOrderQuantity(0L);
        }

        return rnrFormItem;
    }

    protected List<Regimen.RegimenItem> generateRegimeItems(RnRForm form) throws LMISException {
        return new ArrayList<>();
    }

    public void removeRnrForm(RnRForm form) throws LMISException {
        if (form != null) {
            deleteRnrFormItems(form.getRnrFormItemListWrapper());
            deleteRegimenItems(form.getRegimenItemListWrapper());
            deleteBaseInfoItems(form.getBaseInfoItemListWrapper());
            genericDao.delete(form);
        }
    }

    private void deleteBaseInfoItems(final ArrayList<BaseInfoItem> baseInfoItemListWrapper) throws LMISException {
        dbUtil.withDaoAsBatch(BaseInfoItem.class, new DbUtil.Operation<BaseInfoItem, Void>() {
            @Override
            public Void operate(Dao<BaseInfoItem, String> dao) throws SQLException {
                for (BaseInfoItem item : baseInfoItemListWrapper) {
                    dao.delete(item);
                }
                return null;
            }
        });
    }

    private void deleteRegimenItems(final ArrayList<Regimen.RegimenItem> regimenItemListWrapper) throws LMISException {
        dbUtil.withDao(Regimen.RegimenItem.class, new DbUtil.Operation<Regimen.RegimenItem, Void>() {
            @Override
            public Void operate(Dao<Regimen.RegimenItem, String> dao) throws SQLException {
                for (Regimen.RegimenItem item : regimenItemListWrapper) {
                    dao.delete(item);
                }
                return null;
            }
        });
    }

    private void deleteRnrFormItems(final List<RnRForm.RnrFormItem> rnrFormItemListWrapper) throws LMISException {
        rnrFormItemRepository.delete(rnrFormItemListWrapper);
    }

    public static class RnrFormItemRepository {
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
}
