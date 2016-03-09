package org.openlmis.core.googleAnalytics;

public enum TrackerActions {
    SelectStockCard("Select Stock Card"),
    SelectReason("Select Reason"),
    SelectMovementDate("Select Movement Date"),
    SelectComplete("Select Complete"),
    SelectApprove("Select Approve"),
    SelectMMIA("Select MMIA"),
    SelectVIA("Select VIA"),
    CreateRnR("Create RnR"),
    SelectPeriod("Select Period"),
    SubmitRnR("First Time Approve"),
    AuthoriseRnR("Second Time Approve"),
    SelectInventory("Select Inventory"),
    CompleteInventory("Complete Inventory"),
    ApproveInventory("Approve Inventory"),
    SaveInventory("Save Inventory");

    private final String trackerAction;

    TrackerActions(String trackerAction) {
        this.trackerAction = trackerAction;
    }

    public String getString() {
        return this.trackerAction;
    }
}
