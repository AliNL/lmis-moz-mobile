package org.openlmis.core.view.widget;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Checkable;
import android.widget.ImageView;
import android.widget.TextView;

import org.joda.time.DateTime;
import org.openlmis.core.R;
import org.openlmis.core.model.Inventory;

public class SelectPeriodCardView extends CardView implements Checkable {

    private boolean checked;

    private View inventoryContainer;
    private View horizontalLine;
    private TextView inventoryDateDay;
    private TextView inventoryDateMonth;
    private TextView inventoryDateWeek;
    private ImageView checkmarkIcon;

    public SelectPeriodCardView(Context context) {
        super(context);
        init();
    }

    public SelectPeriodCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.view_card_checkable, this);
        inventoryDateDay = (TextView) findViewById(R.id.tv_inventory_date_day);
        inventoryDateMonth = (TextView) findViewById(R.id.tv_inventory_date_month);
        inventoryDateWeek = (TextView) findViewById(R.id.tv_inventory_date_week);
        inventoryContainer = findViewById(R.id.inventory_date_container);
        horizontalLine = findViewById(R.id.inventory_date_line);
        checkmarkIcon = (ImageView) findViewById(R.id.inventory_checkmark);
    }

    public void populate(Inventory inventory) {
        DateTime date = new DateTime(inventory.getUpdatedAt());

        inventoryDateDay.setText(date.dayOfMonth().getAsText());
        inventoryDateMonth.setText(date.monthOfYear().getAsShortText());
        inventoryDateWeek.setText(date.dayOfWeek().getAsText());
    }


    @Override
    public void setChecked(boolean checked) {
        this.checked = checked;

        if (checked) {
            inventoryContainer.setBackgroundColor(getResources().getColor(R.color.color_teal));
            inventoryDateWeek.setBackgroundColor(getResources().getColor(R.color.color_teal_dark));
            inventoryDateDay.setTextColor(getResources().getColor(R.color.color_white));
            inventoryDateMonth.setTextColor(getResources().getColor(R.color.color_white));
            inventoryDateWeek.setTextColor(getResources().getColor(R.color.color_white));
            horizontalLine.setVisibility(View.GONE);
            checkmarkIcon.setVisibility(View.VISIBLE);
        } else {
            inventoryContainer.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            inventoryDateWeek.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            inventoryDateDay.setTextColor(getResources().getColor(R.color.color_text_primary));
            inventoryDateMonth.setTextColor(getResources().getColor(R.color.color_text_primary));
            inventoryDateWeek.setTextColor(getResources().getColor(R.color.color_text_primary));
            horizontalLine.setVisibility(View.VISIBLE);
            checkmarkIcon.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean isChecked() {
        return checked;
    }

    @Override
    public void toggle() {
        setChecked(!checked);
    }

}
