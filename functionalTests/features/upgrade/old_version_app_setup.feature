@upgrade_setup
Feature: Old version set up before upgrade

  Scenario: Install an older version of the app on tablet and have data available from the old version
    When I try to log in with "Michafutene" "password1"
    And I wait up to 120 seconds for "Stock Card Overview" to appear
    # to run this in a physical device, we need to wait longer, IO is slow on physical devices

    Given I press "Create a Via Classica Requisition"
    And I wait for "Requisition -" to appear
    And I enter consultationsNub "888"
    And I press "Save"
    Then I wait for "Via Classica Requisitions" to appear




