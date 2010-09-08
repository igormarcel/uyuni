# feature/users.feature
@javascript
Feature: Check users page/tab
  Validate users page accessibility 

  Scenario: Check users page content
    Given I am authorized as "admin" with password "admin"
    When I go to the users page
    Then I should see a "Active Users" text
     And I should see a "create new user" link
     And I should see a "User List" link in the left menu
     And I should see a "Active" link in the left menu
     And I should see a "Deactivated" link in the left menu
     And I should see a "All" link in the left menu
     And I should see a "admin" link
     And I should see a "Download CSV" link

