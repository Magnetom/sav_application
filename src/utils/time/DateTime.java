package utils.time;

import javafx.util.StringConverter;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.Locale;

public class DateTime {

    private static long ONE_MINUTE = 60L*1000L;
    private static long ONE_HOUR   = 60L*ONE_MINUTE;
    private static long ONE_DAY    = 24L*ONE_HOUR;

    private final static String MMSS_TIME_FORMAT     = "mm:ss";
    private final static String KKMM_TIME_FORMAT     = "kk:mm";
    private final static String KKMM_SPACE_TIME_FORMAT = "kk mm";

    private final static String HHMMSS_TIME_FORMAT = "HH:mm:ss";
    private final static String KK_TIME_FORMAT     = "kk";
    private final static String MM_TIME_FORMAT     = "mm";

    private final static String DDMMYYYY_DATA_FORMAT = "dd-MM-yyyy";
    private final static String YYYYMMDD_DATA_FORMAT = "yyyy-MM-dd";
    private final static String TIMESTAMP_FORMAT     = "yyyy-MM-dd HH:mm:ss";
    private final static long   MILLIS_TO_DATA  = 25L*24L*60L*60L*1000L;


    /**
     *
     * @return yyyy-MM-dd HH:mm:ss formate date as string
     */
    public static String getCurrentTimeStamp(){
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            return dateFormat.format(new Date(System.currentTimeMillis()));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String getFormattedDataTime(long timestamp, final String format){
        SimpleDateFormat simpleFormat = new SimpleDateFormat(format, Locale.getDefault());
        if (timestamp == 0) return simpleFormat.format(0);
        return simpleFormat.format(new Date(timestamp));
    }

    public static String getTimeFromStringTimestamp (String strTimestamp){
        String[] a = strTimestamp.split(" ");
        return a[1];
    }

    public static String timestampToStringYYYYMMDD(long timestamp){
        return getFormattedDataTime(timestamp, YYYYMMDD_DATA_FORMAT);
    }

    public static String getHHMMFromStringTimestamp (String strTimestamp){
        String tmp = getTimeFromStringTimestamp(strTimestamp);
        String[] a = tmp.split(":");
        return a[0]+":"+a[1];
    }

    public static String getTimeDDMMYYYY(){
        return getFormattedDataTime(System.currentTimeMillis(), DDMMYYYY_DATA_FORMAT);
    }

    public static String getTimeYYYYMMDD(){
        return getFormattedDataTime(System.currentTimeMillis(), YYYYMMDD_DATA_FORMAT);
    }
    public static String getTimeKK(){
        return getFormattedDataTime(System.currentTimeMillis(), KK_TIME_FORMAT);
    }
    public static String getTimeMM(){
        return getFormattedDataTime(System.currentTimeMillis(), MM_TIME_FORMAT);
    }
    public static String getTimeKKMM(long timestamp){
        return getFormattedDataTime(timestamp, KKMM_TIME_FORMAT);
    }
    public static String getTimeKKMM(){
        return getFormattedDataTime(System.currentTimeMillis(), KKMM_TIME_FORMAT);
    }
    public static String getTimeKKMM(String format){
        return getFormattedDataTime(System.currentTimeMillis(), format);
    }


    // Конвертер даты в строку для визуальных элементов.
    public static StringConverter<LocalDate> getVisualDateConverter(){
        return getLocalDateConverter (DDMMYYYY_DATA_FORMAT);
    }

    // Конвертер даты в строку в формате TIMESTAMP для sql-запросов и пр.
    public static StringConverter<LocalDate> getDbDateConverter(){
        return getLocalDateConverter (YYYYMMDD_DATA_FORMAT);
    }

    // Конвертер даты в строку в формате TIMESTAMP для sql-запросов и пр.
    public static StringConverter<LocalTime> getDbTimeConverter(){
        return getLocalTimeConverter (HHMMSS_TIME_FORMAT);
    }

    // Конвертер даты в строку в формате TIMESTAMP для sql-запросов и пр.
    public static StringConverter<LocalDateTime> getDbTimestampConverter(){
        return getLocalTimestampConverter (TIMESTAMP_FORMAT);
    }

    private static StringConverter<LocalDateTime> getLocalTimestampConverter(String pattern) {
        if (pattern == null) pattern = TIMESTAMP_FORMAT;

        return new TimestampConverter<LocalDateTime>(pattern) {

            @Override
            public String toString(LocalDateTime date) {
                if (date != null) {
                    return timestampFormatter.format(date);
                } else {
                    return "";
                }
            }
            @Override
            public LocalDateTime fromString(String string) {
                if (string != null && !string.isEmpty()) {
                    return LocalDateTime.parse(string, timestampFormatter);
                } else {
                    return null;
                }
            }
        };
    }

    private static StringConverter<LocalDate> getLocalDateConverter(String pattern) {
        if (pattern == null) pattern = DDMMYYYY_DATA_FORMAT;

        return new DateConverter<LocalDate>(pattern) {

            @Override
            public String toString(LocalDate date) {
                if (date != null) {
                    return dateFormatter.format(date);
                } else {
                    return "";
                }
            }
            @Override
            public LocalDate fromString(String string) {
                if (string != null && !string.isEmpty()) {
                    return LocalDate.parse(string, dateFormatter);
                } else {
                    return null;
                }
            }
        };
    }

    private static StringConverter<LocalTime> getLocalTimeConverter(String pattern) {
        if (pattern == null) pattern = DDMMYYYY_DATA_FORMAT;

        return new TimeConverter<LocalTime>(pattern) {

            @Override
            public String toString(LocalTime date) {
                if (date != null) {
                    return timeFormatter.format(date);
                } else {
                    return "";
                }
            }
            @Override
            public LocalTime fromString(String string) {
                if (string != null && !string.isEmpty()) {
                    return LocalTime.parse(string, timeFormatter);
                } else {
                    return null;
                }
            }
        };
    }

    // Дополняет время, если в ней не указаны минуты или секунды.
    public String alignTime (String time){
        return "";
    }
}
