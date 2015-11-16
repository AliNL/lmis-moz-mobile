package org.openlmis.core.view.holder;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.DatePicker;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openlmis.core.LMISTestRunner;
import org.openlmis.core.R;
import org.openlmis.core.exceptions.LMISException;
import org.openlmis.core.manager.MovementReasonManager;
import org.openlmis.core.model.StockCard;
import org.openlmis.core.model.StockMovementItem;
import org.openlmis.core.model.builder.StockCardBuilder;
import org.openlmis.core.model.builder.StockMovementViewModelBuilder;
import org.openlmis.core.model.repository.StockRepository;
import org.openlmis.core.utils.DateUtil;
import org.openlmis.core.view.adapter.StockMovementAdapter;
import org.openlmis.core.view.viewmodel.StockMovementViewModel;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowDatePickerDialog;
import org.robolectric.shadows.ShadowToast;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

import roboguice.RoboGuice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(LMISTestRunner.class)
public class StockMovementViewHolderTest {

    private StockMovementViewHolder viewHolder;
    private StockMovementAdapter.MovementChangedListener mockedListener;
    private StockMovementViewModel viewModel;
    private StockCard stockCard;
    private View itemView;

    @Before
    public void setUp() throws LMISException, ParseException {
        itemView = LayoutInflater.from(RuntimeEnvironment.application).inflate(R.layout.item_stock_movement, null, false);
        mockedListener= mock(StockMovementAdapter.MovementChangedListener.class);
        viewHolder = new StockMovementViewHolder(itemView, mockedListener);

        viewModel = new StockMovementViewModelBuilder()
                .withMovementDate("2015-11-11")
                .withDocumentNo("12345")
                .withNegativeAdjustment(null)
                .withPositiveAdjustment(null)
                .withIssued("30")
                .withReceived(null)
                .withStockExistence("70")
                .withMovementReason(new MovementReasonManager.MovementReason(StockMovementItem.MovementType.ISSUE, "ISSUE_1", "issue description")).build();

        StockRepository stockRepository = RoboGuice.getInjector(RuntimeEnvironment.application).getInstance(StockRepository.class);

        stockCard = StockCardBuilder.buildStockCardWithOneMovement(stockRepository);
    }

    @Test
    public void shouldPopulateTextDataWhenPopulatingData() {
        viewHolder.populate(viewModel, stockCard);

        assertEquals("12345", viewHolder.etDocumentNo.getText().toString());
        assertEquals("2015-11-11", viewHolder.txMovementDate.getText().toString());
        assertEquals("", viewHolder.etReceived.getText().toString());
        assertEquals("", viewHolder.etNegativeAdjustment.getText().toString());
        assertEquals("", viewHolder.etPositiveAdjustment.getText().toString());
        assertEquals("30", viewHolder.etIssued.getText().toString());
        assertEquals("70", viewHolder.txStockExistence.getText().toString());
        assertEquals("issue description", viewHolder.txReason.getText().toString());
    }

    @Test
    public void shouldDisableLineWhenPopulatingData() {
        viewHolder.populate(viewModel, stockCard);

        assertFalse(viewHolder.etDocumentNo.isEnabled());
        assertFalse(viewHolder.etReceived.isEnabled());
        assertFalse(viewHolder.etNegativeAdjustment.isEnabled());
        assertFalse(viewHolder.etPositiveAdjustment.isEnabled());
        assertFalse(viewHolder.etIssued.isEnabled());
    }

    @Test
    public void shouldHideUnderline(){
        viewHolder.populate(viewModel, stockCard);

        assertNull(viewHolder.etIssued.getBackground());
        assertNull(viewHolder.etPositiveAdjustment.getBackground());
        assertNull(viewHolder.etNegativeAdjustment.getBackground());
        assertNull(viewHolder.etReceived.getBackground());
        assertNull(viewHolder.etDocumentNo.getBackground());
    }

    @Test
    public void shouldSetFontColorBlackIfNotInventoryAdjustment() {
        viewHolder.populate(viewModel, stockCard);

        int blackColor = RuntimeEnvironment.application.getResources().getColor(R.color.black);

        assertEquals(blackColor, viewHolder.txMovementDate.getCurrentTextColor());
        assertEquals(blackColor, viewHolder.txReason.getCurrentTextColor());
        assertEquals(blackColor, viewHolder.etDocumentNo.getCurrentTextColor());
        assertEquals(blackColor, viewHolder.etReceived.getCurrentTextColor());
        assertEquals(blackColor, viewHolder.etPositiveAdjustment.getCurrentTextColor());
        assertEquals(blackColor, viewHolder.etNegativeAdjustment.getCurrentTextColor());
        assertEquals(blackColor, viewHolder.txStockExistence.getCurrentTextColor());
    }

    @Test
    public void shouldShowMovementTypeDialogOnClick() {
        viewHolder.populate(viewModel, stockCard);

        viewHolder.txReason.performClick();

        AlertDialog typeDialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(typeDialog);
    }

    @Test
    public void shouldSetReasonAndDateOnComplete() {
        MovementReasonManager.MovementReason reason = new MovementReasonManager.MovementReason(StockMovementItem.MovementType.RECEIVE, "DON", "Donations");
        String today = DateUtil.formatDate(new Date());
        viewHolder.populate(viewModel, stockCard);
        viewHolder.txMovementDate.setText("");

        StockMovementViewHolder.MovementSelectListener listener = viewHolder.new MovementSelectListener(viewModel);
        listener.onComplete(reason);

        assertEquals(reason.getDescription(), viewHolder.txReason.getText().toString());
        assertEquals(reason.getDescription(), viewModel.getReason().getDescription());
        assertEquals(today, viewHolder.txMovementDate.getText().toString());
        assertEquals(today, viewModel.getMovementDate());
        assertTrue(viewHolder.etReceived.isEnabled());
        verify(mockedListener).movementChange();
    }

    @Test
    public void shouldShowMovementDateDialogOnClick() {
        viewHolder.populate(viewModel, stockCard);

        viewHolder.txMovementDate.performClick();

        DatePickerDialog datePickerDialog = (DatePickerDialog) ShadowDatePickerDialog.getLatestDialog();
        assertNotNull(datePickerDialog);
    }

    @Test
    public void shouldValidateMovementDateOnSelectionAndShowToastIfInvalid() throws ParseException, LMISException {
        viewHolder.populate(viewModel, stockCard);

        StockMovementViewHolder.MovementDateListener movementDateListener = viewHolder.new MovementDateListener(viewModel, new Date());
        movementDateListener.onDateSet(mock(DatePicker.class), 2015, 11, 10);
        assertNotNull(ShadowToast.getLatestToast());
    }

    @Test
    public void shouldValidateMovementDateOnSelectionAnd() throws ParseException, LMISException {
        viewHolder.populate(viewModel, stockCard);

        StockMovementViewHolder.MovementDateListener movementDateListener = viewHolder.new MovementDateListener(viewModel, DateUtil.parseString("11-11-2015", "MM-dd-YYYY"));
        movementDateListener.onDateSet(mock(DatePicker.class), 2015, 10, 15);
        assertEquals("15 Nov 2015", viewHolder.txMovementDate.getText().toString());
        assertEquals("15 Nov 2015", viewModel.getMovementDate());
        assertNull(ShadowToast.getLatestToast());
    }

    @Test
    public void shouldEnableMovementTypeAndReasonIfModelIsDraft() {
        viewModel.setDraft(true);
        viewHolder.populate(viewModel, stockCard);

        assertTrue(viewHolder.txMovementDate.isEnabled());
        assertTrue(viewHolder.txReason.isEnabled());
    }

    @Test
    public void shouldSetValueAfterTextChange() {
        viewHolder.populate(viewModel, stockCard);

        viewHolder.etIssued.setText("30");
        assertEquals("30", viewModel.getIssued());
        assertEquals("70", viewModel.getStockExistence());
        assertEquals("70", viewHolder.txStockExistence.getText().toString());


    }
}