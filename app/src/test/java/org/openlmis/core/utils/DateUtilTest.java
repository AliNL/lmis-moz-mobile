package org.openlmis.core.utils;

import org.junit.Test;

import java.util.Calendar;
import java.util.Date;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;


public class DateUtilTest {

    @Test
    public void shouldTruncateTimeStampInDate() throws Exception {
        Date timeStampDate = DateUtil.parseString("2015-07-20 11:33:44", DateUtil.DATE_TIME_FORMAT);
        Date expectedDate = DateUtil.parseString("20/07/2015", DateUtil.SIMPLE_DATE_FORMAT);
        Date date = DateUtil.truncateTimeStampInDate(timeStampDate);

        assertThat(date.getTime(), is(expectedDate.getTime()));
    }

    @Test
    public void shouldReturnLastMonthMinusMonth(){
        Calendar now = Calendar.getInstance();
        now.set(2014,11,01);

        Date oneMonthBefore = DateUtil.dateMinusMonth(now.getTime(), 1);
        now.setTime(oneMonthBefore);
        assertThat(now.get(Calendar.MONTH), is(10));
    }

    @Test
    public void shouldReturnLastYearAndLatestMonthWhenMinusMonth(){
        Calendar now = Calendar.getInstance();
        now.set(2016,00,01);

        Date oneMonthBefore = DateUtil.dateMinusMonth(now.getTime(), 1);
        now.setTime(oneMonthBefore);

        assertThat(now.get(Calendar.MONTH), is(11));
        assertThat(now.get(Calendar.YEAR), is(2015));
    }


    @Test
    public void shouldReturnNextDayWhenAddOneDayOfMonth() {
        Calendar now = Calendar.getInstance();
        now.set(2016, 00, 01);

        Date nextDay = DateUtil.addDayOfMonth(now.getTime(), 1);
        now.setTime(nextDay);

        assertThat(now.get(Calendar.MONTH), is(0));
        assertThat(now.get(Calendar.DAY_OF_MONTH), is(2));
        assertThat(now.get(Calendar.YEAR), is(2016));
    }

    @Test
    public void shouldReturnPreviousDayWhenMinusOneDayOfMonth() {
        Calendar now = Calendar.getInstance();
        now.set(2016, 0, 3);

        Date nextDay = DateUtil.addDayOfMonth(now.getTime(), -1);
        now.setTime(nextDay);

        assertThat(now.get(Calendar.MONTH), is(0));
        assertThat(now.get(Calendar.DAY_OF_MONTH), is(2));
        assertThat(now.get(Calendar.YEAR), is(2016));
    }
}