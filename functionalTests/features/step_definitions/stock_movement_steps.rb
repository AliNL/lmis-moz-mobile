require 'calabash-android/calabash_steps'
require 'pry'

Then(/^I select stock card called "(.*?)"$/) do |name|
      q = query("android.widget.TextView id:'product_name' text:'#{name}'")
      touch(q.last);
end

And(/^I select a reason "(.*?)" "(.*?)"$/) do |first_reason, second_reason|
    q = query("android.widget.TextView id:'tx_reason'")
    touch(q.last);
    steps %Q{
        Then I press "#{first_reason}"
        Then I press "#{second_reason}"
    	}
end

And(/^I enter received number "(.*?)"$/) do |number|
    touch(query("android.widget.EditText id:'et_received'").last);
    keyboard_enter_text(number)
    hide_soft_keyboard
end

And(/^I enter issued number "(.*?)"$/) do |number|
    q = query("android.widget.EditText id:'et_issued'")
    touch(q.last)
    keyboard_enter_text(number)
    hide_soft_keyboard
end

And(/^I enter negative adjustment number "(.*?)"$/) do |number|
    q = query("android.widget.EditText id:'et_negative_adjustment'")
    touch(q.last)
    keyboard_enter_text(number)
    hide_soft_keyboard
end

And(/^I enter positive adjustment number "(.*?)"$/) do |number|
    q = query("android.widget.EditText id:'et_positive_adjustment'")
    touch(q.last)
    keyboard_enter_text(number)
    hide_soft_keyboard
end

Then(/^I make a movement "(.*?)" "(.*?)" "(.*?)" "(.*?)" "(.*?)"$/) do |stock_card_name, first_reason, second_reason, movement_column, number|
    steps %Q{
        Then I select stock card called "#{stock_card_name}"
        Then I wait for the "StockMovementActivity" screen to appear
        Then I wait for 1 second
        And I select a reason "#{first_reason}" "#{second_reason}"
    }

    if movement_column.eql? "positive adjustment" or movement_column.eql? "issued"
        steps %Q{
            Then I swipe right
        }
    end

    steps %Q{
        Then I wait for 1 second
        And I enter #{movement_column} number "#{number}"
        And I press "Complete"
        Then I go back
    }
end