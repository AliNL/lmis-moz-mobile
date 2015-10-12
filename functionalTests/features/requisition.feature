@Requisition
Feature: Requisition

  Scenario: Go to requisition page and display all VIA products
    Given I am logged in
    Given I am Initialized Inventory

    And I press "Create a Via Classica Requesition"
    Then I wait for the "RequisitionActivity" screen to appear
    Then I should see text containing "Acyclovir, tablet 400mg"
    Then I should see "4" products

  Scenario: Pop up alert
    Given I am logged in
    And I press "Create a Via Classica Requesition"
    Then I wait for the "RequisitionActivity" screen to appear
    Then I enter consultationsNub "2015"
    Then I wait for 1 second
    Then I go back
    Then I wait to see "Are you sure you want to quit without saving your work?"
    Then I press "Yes"
    Then I wait for the "HomeActivity" screen to appear

  Scenario: Save requisition draft and complete
    Given I am logged in
    And I press "Create a Via Classica Requesition"
    When I enter consultationsNub "888"
    Then I swipe right
    Then I swipe right
    Then I swipe right
    Then I enter QuantityRequested "345"
    And I press "Save"
    Then I wait for the "RequisitionActivity" screen to appear

    When I press view with id "btn_requisition"
    Then I swipe right
    Then I swipe right
    Then I should see "345"
    Then I press "Submit"
    Then I press "Complete"
    Then I wait for the "RequisitionActivity" screen to appear

Scenario: Add A Issued Movement on VIA product,then the quantity should change
    Given I am logged in
    And I press "Stock Card"
    Then I wait for the "StockCardListActivity" screen to appear
    Then I wait for 1 second
    And I make a movement "Acetylsalicylic Acid, tablet 300mg [P1]" "Issues" "PAV" "issued" "10"
    Then I wait for 1 second
    Then I go back
    Then I wait for the "HomeActivity" screen to appear

    When I press view with id "btn_requisition"
    Then I swipe right
    Then I should see "113" on index "1" of "tx_theoretical" field

