package com.kanashi.srb.app.utility;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DateTextFormatter {
    private static final String DATE_FORMAT = "dd-MM-yyyy";
    private final static Map<String, String> WEEKDAY_TRANSLATION = new HashMap<>() {{
        put("MONDAY", "ПН");
        put("TUESDAY", "ВТ");
        put("WEDNESDAY", "СР");
        put("THURSDAY", "ЧТ");
        put("FRIDAY", "ПТ");
        put("SATURDAY", "СБ");
        put("SUNDAY", "ВС");
    }};

    public static String getDate(LocalDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ofPattern(DATE_FORMAT));
    }

    public static String getWeekDay(LocalDateTime dateTime) {
        String weekDay = dateTime.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        return WEEKDAY_TRANSLATION.getOrDefault(weekDay.toUpperCase(), "");
    }

    public static String getTimePeriod(int currentHour) {
        String startPeriod = "##";
        String endPeriod = "##";

        if (currentHour == 0) {
            startPeriod = "23";
            endPeriod = "0";
        } else {
            startPeriod = String.valueOf(currentHour - 1);
            endPeriod = String.valueOf(currentHour);
        }

        if (startPeriod.length() < 2) {
            startPeriod = "0" + startPeriod;
        }

        if (endPeriod.length() < 2) {
            endPeriod = "0" + endPeriod;
        }

        startPeriod += ":00";
        endPeriod += ":00";

        return startPeriod + "-" + endPeriod;
    }
}
