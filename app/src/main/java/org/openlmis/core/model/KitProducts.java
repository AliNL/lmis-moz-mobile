package org.openlmis.core.model;


import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@DatabaseTable(tableName = "kit_products")
public class KitProducts extends BaseModel {
    @DatabaseField(foreign = true, foreignAutoRefresh = true)
    Kit kit;

    @DatabaseField(foreign = true, foreignAutoRefresh = true)
    private Product product;

    @DatabaseField
    private int quantity;

    public KitProducts(Kit kit, Product product, int quantity) {
        this.kit = kit;
        this.product = product;
        this.quantity = quantity;
    }
}
