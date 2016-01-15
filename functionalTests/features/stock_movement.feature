@STOCK_MOVEMENT
Feature: stock movement Page

  Background: Navigate to Home Page
    Given I try to log in with "stock_card" "password1"

  Scenario: Navigate to Home Page
    Given I have initialized inventory

  Scenario: Bottom Btn Logic
    When I press "Stock Card Overview"
    Then I wait for "Stock Overview" to appear
    Then I wait for 1 second
    Then I select stock card code called "[08S42B]"
    Then I wait for "Stock Card" to appear
    Then I wait for 1 second
    Then I don't see "Complete"
    Then I don't see "Cancel"
    Then I select a reason "Entries" "District( DDM)"
    Then I should see "Complete"
    Then I should see "CANCEL"
    And I press "CANCEL"
    Then I wait for 1 second
    Then I don't see "Complete"
    Then I don't see "CANCEL"

  Scenario: deactivated product show notify banner
    When I press "Stock Card Overview"
    Then I wait for "Stock Overview" to appear
    Then I wait for 1 second
    Then I select stock card code called "[01A04Z]"
    Then I wait for "Stock Card" to appear
    Then I wait for 1 second
    Then I should not see "This product has been deactivated and is not available to reorder"
    And I select a reason "Negative Adjustments" "Damaged on arrival"
    Then I wait for 1 second
    Then I swipe right
    And I enter negative adjustment number "123"
    Then I wait for "Complete" to appear
    And I press "Complete"
    And I sign with "superuser"
    Then I see "123"
    Then I see "super" in signature field
    Then I navigate back
    Then I navigate back
    Then I wait for "STOCK CARD OVERVIEW" to appear

    #deactivate product
    Given server deactivates products has stock movement
    And I press the menu key
    Then I see "Sync Data"
    And I press "Sync Data"
    When I press "Stock Card Overview"
    Then I wait for "Stock Overview" to appear
    Then I should see "has been deactivated and removed"

    # reactive product
    When server reactive products
    Then I navigate back
    Then I wait for "STOCK CARD OVERVIEW" to appear
    And I press the menu key
    Then I see "Sync Data"
    And I press "Sync Data"
    When I press "Stock Card Overview"
    Then I wait for "Stock Overview" to appear
    Then I should not see "has been deactivated and removed"

    #issued stock movement
    Then I select stock card code called "[01A03Z]"
    Then I wait for "Stock Card" to appear
    Then I wait for 1 second
    Then I should see "This product has been deactivated and is not available to reorder"
    Then I wait for 1 second
    And I select a reason "Issues" "PAV"
    Then I wait for 1 second
    Then I swipe right
    And I enter issued number "123"
    Then I wait for "Complete" to appear
    And I press "Complete"
    And I sign with "superuser"
    Then I see "123"
    Then I see "super" in signature field
    Then I navigate back
    Then I should see "has been deactivated and removed"

    #clear warning banner
    And I clear banner message
    Then I should not see "has been deactivated and removed"


  Scenario: Add A Receive Movement
    When I press "Stock Card Overview"
    Then I wait for "Stock Overview" to appear
    Then I wait for 1 second
    Then I select stock card code called "[01A05]"
    Then I wait for "Stock Card" to appear
    Then I wait for 1 second
    And I select a reason "Entries" "District( DDM)"
    Then I wait for 1 second
    And I enter received number "2"
    Then I wait for "Complete" to appear
    And I press "Complete"
    And I sign with "superuser"
    Then I see "125"
    Then I see "super" in signature field
    Then I navigate back
    Then I wait for 1 second
    Then I navigate back
    Then I wait for "STOCK CARD OVERVIEW" to appear

  Scenario: Add A Positive Adjustment
    When I press "Stock Card Overview"
    Then I wait for "Stock Overview" to appear
    Then I wait for 1 second
    Then I select stock card code called "[01A02]"
    Then I wait for "Stock Card" to appear
    Then I wait for 1 second
    And I select a reason "Positive Adjustments" "Donations to Deposit"
    Then I swipe right
    Then I wait for 1 second
    And I enter positive adjustment number "2"
    Then I wait for "Complete" to appear
    And I press "Complete"
    And I sign with "superuser"
    Then I see "123"
    Then I navigate back
    Then I wait for 1 second
    Then I navigate back
    Then I wait for "STOCK CARD OVERVIEW" to appear

  Scenario: Add all movements for one drug when is STRESS TEST
    When I press "Stock Card Overview"
    Then I wait for "Stock Overview" to appear
    Then I wait for 1 second
    Then I make all movements for "08S18Y"
    Then I wait for 1 second
    Then I navigate back
    Then I wait for "STOCK CARD OVERVIEW" to appear

  Scenario: View stock movement page when rotate the page
    When I press "Stock Card Overview"
    Then I wait for "Stock Overview" to appear
    Then I wait for 1 second
    Then I select stock card code called "[01A01]"
    And I wait for "Stock Card" to appear
    And I select a reason "Positive Adjustments" "Donations to Deposit"
    Then I swipe right
    Then I wait for 1 second
    And I enter positive adjustment number "2"
    And I rotate the page to "landscape"
    Then I wait for "Complete" to appear
    And I press "Complete"
    And I sign with "superuser"
    Then I see the text "Donations to Deposit"
    Then I see "125"
    Then I see "super" in signature field

  Scenario: ReSelect Adjust Reason
    When I press "Stock Card Overview"
    Then I wait for "Stock Overview" to appear
    Then I select stock card code called "08S36"
    Then I wait for "Stock Card" to appear
    And I rotate the page to "landscape"
    And I wait for "Stock Card" to appear
    And I select a reason "Positive Adjustments" "Donations to Deposit"
    Then I wait for 1 second
    And I enter "888" into documentNo
    Then I wait for 1 second
    And I enter positive adjustment number "41"
    Then I wait for 1 second
    And I select a reason "Positive Adjustments" "Donations to Deposit"
    And I wait for 1 second
    Then I should not see "888"
    Then I should not see "41"



