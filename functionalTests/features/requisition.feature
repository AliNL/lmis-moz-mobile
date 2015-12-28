@REQUISITION
Feature: Requisition

  Scenario: Save requisition draft
    Given I try to log in with "via" "password1"
    Given I have initialized inventory
    And I press "Stock Card Overview"
    Then I wait for "Stock Overview" to appear
    Then I wait for 1 second
    And I make a movement "[01A01]" "Issues" "PAV" "issued" "10"
    Then I wait for 1 second
    Then I navigate back
    Then I wait for "Home Page" to appear
    And I press "Create a Via Classica Requisition"
    Then I enter consultationsNub "888"
    Then I swipe right
    Then I should see "113" on index "1" of "tx_theoretical" field
    Then I swipe right
    Then I swipe right
    Then I enter QuantityRequested "345"
    Then I wait for 1 second
    Then I press "Save"
    Then I wait for "Home Page" to appear
    When I press view with id "btn_requisition"
    And I wait for "Requisition -" to appear
    And I rotate the page to "landscape"
    Then I swipe right
    Then I should see "345" in the requisition form
    Then I press "Submit for Approval"
    And I sign requisition with "superuser" "testUser" and complete
    Then I wait for "Home Page" to appear

  Scenario: Pop up alert
    Given I try to log in with "via" "password1"
    And I press "Stock Card Overview"
    Then I wait for "Stock Overview" to appear
    Then I wait for 1 second
    And I make a movement "[01A01]" "Entries" "District( DDM)" "received" "10"
    Then I wait for 1 second
    Then I navigate back
    Then I wait for "Home Page" to appear
    And I press "Create a Via Classica Requisition"
    Then I wait for "Requisition -" to appear
    Then I enter consultationsNub "2015"
    Then I wait for 1 second
    Then I navigate back
    Then I wait to see "Are you sure you want to quit without saving your work?"
    Then I press "Yes"
    Then I wait for "Home Page" to appear