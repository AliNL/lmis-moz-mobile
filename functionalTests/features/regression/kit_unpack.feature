@unpack_kit @regression @change_date
Feature: Unpack Kit

  Scenario: Unpack US kit and verify kit products SOH
    Given I change device date to "20160216.130000"
    And I try to log in with "kit" "password1"
    And I wait up to 120 seconds for "Initial Inventory" to appear
    Then I wait for "Initial inventory" to appear
    When I search product by fnm "08L01X" and select this item with quantity "888"
    When I search product by fnm "15C0ZY" and select this item with quantity "2"
    Then I press "Complete"

    Then I wait for "MMIA" to appear
    And I press "Stock Card Overview"
    And I wait for "Stock Overview" to appear

    Then I select stock card code called "[15C0ZY]"
    Then I wait for "Stock Card" to appear
    And I select a reason "Negative Adjustments" "Damaged on arrival"
    Then I wait for 1 second
    Then I swipe right
    And I enter negative adjustment number "2"
    Then I wait for "Complete" to appear
    And I press "Complete"
    And I wait for "Enter your initials" to appear
    And I sign with "super"
    And I wait for 1 second
    And I press the menu key
    And I press "Archive drugs"
    And I wait for "Stock Overview" to appear
    Then I navigate back
    Then I wait for "MMIA" to appear

    Then I press "KITS"
    Then I wait for "Kit Overview" to appear
    Then I press "KITS  (DE PME US)"
    Then I wait for "[SCOD10]" to appear
    And I select a reason "Entries" "District( DDM)"
    And I wait for 1 second
    And I enter received number "3"
    Then I wait for "Complete" to appear
    And I press "Complete"
    And I wait for "Enter your initials" to appear
    And I sign with "superuser"
    Then I wait for "Unpack Kit" to appear
    Then I see "1"
    Then I swipe right
    Then I wait for 1 second
    Then I swipe right
    Then I wait for 1 second
    Then I see "super" in signature field

    And I press "Unpack Kit"
    Then I wait for "Next" to appear
    When I press "3"
    And I press "Next"
    Then I wait for "02A03" to appear
    And I should see "Unpacking"

    And I enter document number for unpack kit

    And I enter quantity for all products in kit
    #signature
    And I wait for "Please enter your initials to confirm the amounts entered" to appear
    And I sign with "test"

    Then I wait for "[SCOD10]" to appear
    And I should not see "Unpack Kit"
    And I swipe right
    Then I see "test" in signature field

    Then I navigate back
    And I wait for 1 second
    Then I navigate back

    Then I wait for "MMIA" to appear
    And I press "Stock Card Overview"
    Then I should see "Total:44"
    When I search drug by fnm "08L01X"
    Then I should see "889"
    And I clean search bar
    When I search drug by fnm "15C0ZY"
    Then I should see "[15C0ZY]"
    Then I select stock card code called "[15C0ZY]"
    Then I should see "District( DDM)"
    And I swipe right
    And I swipe right
    Then I see "test" in signature field
    Then I navigate back
    And I wait for 1 second
    Then I navigate back
    And I wait for 1 second

    Given I change device date to "20160218.140000"
    And I navigate back
    And I wait for 2 seconds
    And I try to log in with "kit" "password1"

    Then I wait for "MMIA" to appear
    And I press "Via Classica Requisitions"
    Then I wait for "Via Classica Requisitions" to appear
    Then I should see text containing "No Via Classica Requisition has been created."

    Then I press "Complete Inventory"
    And I wait for "inventory" to appear

    When I search drug by fnm "15C0ZY"
    Then I should see "[15C0ZY]"
    Then I navigate back
    And I wait for 1 second
    Then I do physical inventory for all items

    Then I wait for "Via Classica Requisitions" to appear
    Then I should see text containing "Create Via Classica Requisition"

    And I press "Create Via Classica Requisition"
    Then I should see "Select inventory to close period"
    And I press "Thursday"
    And I press "Next"
    Then I should see "to 18 Feb"
    Then I should see kit receive number and open number is "3"

    Then I enter consultationsNub "2015"
    Then I wait for 1 second
    Then I navigate back
    Then I wait to see "Are you sure you want to quit without saving your work?"
    Then I press "Yes"
    Then I wait for "Via Classica Requisitions" to appear

    And I press "Create Via Classica Requisition"
    Then I should see "Select inventory to close period"
    And I press "Thursday"
    And I press "Next"
    Then I should see "to 18 Feb"
    Then I should see kit receive number and open number is "3"

    And I should see empty consultations number
    Then I enter consultationsNub "888"
    Then I swipe right
    Then I should see "889" on index "1" of "tx_theoretical" field
    Then I swipe right
    Then I swipe right
    Then I enter QuantityRequested "345"
    Then I wait for 1 second
    Then I press "Save"
    Then I wait for "Via Classica Requisitions" to appear

    And I press "Continue Working on Via Classica Requisition"
    # And I rotate the page to "landscape"
    # Then I swipe right
    # Then I should see "345" in the requisition form

    And I press "Submit for Approval"
    And I sign requisition with "superuser" "testUser" and complete
    Then I wait for "Via Classica Requisitions" to appear

    Then I navigate back
    Then I wait for "Stock Card Overview" to appear
    And I press the menu key
    Then I see "Sync Data"
    And I press "Sync Data"

    And I wait up to 120 seconds for "0 minutes since last sync" to appear
    And I click the last sync banner
    Then I see "Requisition last synced 0 minutes ago"
    Then I go back

    And I press "Via Classica Requisitions"
    Then I wait for "Via Classica Requisitions" to appear
    Then I should see text containing "View Via Classica Requisition"