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
import android.content.res.Resources;

import com.google.inject.Inject;
import com.j256.ormlite.dao.Dao;

import org.openlmis.core.R;
import org.openlmis.core.exceptions.LMISException;
import org.openlmis.core.model.Product;
import org.openlmis.core.model.Program;
import org.openlmis.core.persistence.DbUtil;
import org.openlmis.core.persistence.GenericDao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class ProgramRepository {

    GenericDao<Program> genericDao;

    @Inject
    ProductRepository productRepository;

    @Inject
    DbUtil dbUtil;

    @Inject
    Resources resources;

    private HashMap<String, List<String>> rnrFormItemConfigList = new HashMap<>();

    @Inject
    public ProgramRepository(Context context) {
        genericDao = new GenericDao<>(Program.class, context);
    }

    public List<Program> list() throws LMISException {
        return genericDao.queryForAll();
    }

    public void create(Program program) throws LMISException {
        genericDao.create(program);
    }

    public void saveProgramWithProduct(Program program) throws LMISException {
        if (MMIARepository.MMIA_PROGRAM_CODE.equals(program.getProgramCode())) {
            setMMIAProductMedicineType(program);
        }
        create(program);
        for (Product product : program.getProducts()) {
            product.setProgram(program);
        }
        productRepository.save(new ArrayList<>(program.getProducts()));
        refresh(program);
    }

    private void setMMIAProductMedicineType(Program program) {
        initRnrFormItemConfigList();
        Collection<Product> products = program.getProducts();
        setMedicineType(products, Product.MEDICINE_TYPE_ADULT);
        setMedicineType(products, Product.MEDICINE_TYPE_BABY);
        setMedicineType(products, Product.MEDICINE_TYPE_OTHER);
    }

    private void setMedicineType(Collection<Product> dataList, String medicineType) {
        List<String> fnms = rnrFormItemConfigList.get(medicineType);
        for (Product item : dataList) {
            for (String fnm : fnms) {
                if (fnm.equals(item.getCode())) {
                    item.setMedicine_type(medicineType);
                }
            }
        }
    }


    public void initRnrFormItemConfigList() {
        rnrFormItemConfigList.put(Product.MEDICINE_TYPE_ADULT, Arrays.asList(resources.getStringArray(R.array.medicine_adult)));
        rnrFormItemConfigList.put(Product.MEDICINE_TYPE_BABY, Arrays.asList(resources.getStringArray(R.array.medicine_baby)));
        rnrFormItemConfigList.put(Product.MEDICINE_TYPE_OTHER, Arrays.asList(resources.getStringArray(R.array.medicine_other)));
    }

    public void refresh(Program programsWithProducts) {
        try {
            genericDao.refresh(programsWithProducts);
        } catch (LMISException e) {
            e.printStackTrace();
        }
    }

    public Program queryByCode(final String programCode) throws LMISException {
        return dbUtil.withDao(Program.class, new DbUtil.Operation<Program, Program>() {
            @Override
            public Program operate(Dao<Program, String> dao) throws SQLException {
                return dao.queryBuilder().where().eq("programCode", programCode).queryForFirst();
            }
        });
    }
}
