package org.openlmis.core.utils;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.openlmis.core.utils.DateUtil.SIMPLE_DATE_FORMAT;
import static org.openlmis.core.utils.DateUtil.parseString;


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
        now.set(2014, 11, 01);

        Date oneMonthBefore = DateUtil.dateMinusMonth(now.getTime(), 1);
        now.setTime(oneMonthBefore);
        assertThat(now.get(Calendar.MONTH), is(10));
    }

    @Test
    public void shouldReturnLastYearAndLatestMonthWhenMinusMonth(){
        Calendar now = Calendar.getInstance();
        now.set(2016, 00, 01);

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

    @Test
    public void shouldSortByDate() throws Exception {
        ArrayList<String> list = new ArrayList<>();
        list.add("18/10/2017");
        list.add("16/10/2016");
        list.add("18/10/2016");
        DateUtil.sortByDate(list);

        assertThat(list.get(0), is("16/10/2016"));
        assertThat(list.get(1), is("18/10/2016"));
        assertThat(list.get(2), is("18/10/2017"));
    }

    @Test
    public void shouldGeneratePeriodBegin() throws Exception {
        Date generatePeriodBegin = parseString("28/09/2015", SIMPLE_DATE_FORMAT);

        Date expectPeriodBegin = parseString("21/09/2015", SIMPLE_DATE_FORMAT);
        assertThat(DateUtil.generatePeriodBeginBy(generatePeriodBegin), is(expectPeriodBegin));
    }

    @Test
    public void shouldGeneratePeriodBeginWhenDateAcrossYear() throws Exception {
        Date generatePeriodBegin = parseString("10/01/2016", SIMPLE_DATE_FORMAT);

        Date expectPeriodBegin = parseString("21/12/2015", SIMPLE_DATE_FORMAT);
        assertThat(DateUtil.generatePeriodBeginBy(generatePeriodBegin), is(expectPeriodBegin));
    }

    @Test
    public void shouldGeneratePeriodEnd() throws Exception {
        Date periodBegin = parseString("21/10/2015", SIMPLE_DATE_FORMAT);

        Date expectPeriodEnd = parseString("20/11/2015", SIMPLE_DATE_FORMAT);
        assertThat(DateUtil.generatePeriodEndByBegin(periodBegin), is(expectPeriodEnd));
    }

    @Test
    public void shouldGeneratePreviousPeriodBegin() throws Exception {
        Date periodBegin = parseString("21/01/2016", SIMPLE_DATE_FORMAT);

        Date expectPeriodBegin = parseString("21/12/2015", SIMPLE_DATE_FORMAT);
        assertThat(DateUtil.generatePreviousMonthDateBy(periodBegin), is(expectPeriodBegin));
    }

    @Test
    public void shouldGeneratePreviousPeriodEnd() throws Exception {
        Date periodEnd = parseString("20/01/2016", SIMPLE_DATE_FORMAT);

        Date expectPeriodEnd = parseString("20/12/2015", SIMPLE_DATE_FORMAT);
        assertThat(DateUtil.generatePreviousMonthDateBy(periodEnd), is(expectPeriodEnd));
    }

    @Test
    public void shouldCountTwoPeriodMonthNubCorrectlyWhenInSameYear() throws Exception {
        Date firstPeriod = parseString("20/02/2015", SIMPLE_DATE_FORMAT);
        Date secondPeriod = parseString("20/10/2015", SIMPLE_DATE_FORMAT);

        int periodMonthNub = DateUtil.calculateDateMonthOffset(firstPeriod, secondPeriod);
        assertThat(periodMonthNub,is(8));
    }

    @Test
    public void shouldCountTwoPeriodMonthNubCorrectly() throws Exception {
        Date firstPeriod = parseString("20/12/2015", SIMPLE_DATE_FORMAT);
        Date secondPeriod = parseString("20/01/2016", SIMPLE_DATE_FORMAT);

        int periodMonthNub = DateUtil.calculateDateMonthOffset(firstPeriod, secondPeriod);
        assertThat(periodMonthNub,is(1));
    }
}