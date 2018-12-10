package utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateTime {

    public static long ONE_MINUTE = 60L*1000L;
    public static long ONE_HOUR   = 60L*ONE_MINUTE;
    public static long ONE_DAY    = 24L*ONE_HOUR;

    private final static String MMSS_TIME_FORMAT     = "mm:ss";
    private final static String KKMM_TIME_FORMAT     = "kk:mm";
    private final static String DDMMYYYY_DATA_FORMAT = "dd-MM-yyyy";
    private final static String YYYYMMDD_DATA_FORMAT = "yyyy-MM-dd";
    private final static long   MILLIS_TO_DATA  = 25L*24L*60L*60L*1000L;


    private static String getFormattedDataTime(long timestamp, final String format){
        SimpleDateFormat simpleFormat = new SimpleDateFormat(format, Locale.getDefault());
        if (timestamp == 0) return simpleFormat.format(0);
        return simpleFormat.format(new Date(timestamp));
    }

    public static String getTimeFromStringTimestamp (String strTimestamp){
        String[] a = strTimestamp.toString().split(" ");
        return a[1];
    }

    public static String timestampToStringYYYYMMDD(long timestamp){
        return getFormattedDataTime(timestamp, YYYYMMDD_DATA_FORMAT);
    }

    public static String getHHMMFromStringTimestamp (String strTimestamp){
        String tmp = getTimeFromStringTimestamp(strTimestamp);
        String[] a = tmp.toString().split(":");
        return a[0]+":"+a[1];
    }

}
