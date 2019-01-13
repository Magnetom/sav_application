package utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateTime {

    public static long ONE_MINUTE = 60L*1000L;
    public static long ONE_HOUR   = 60L*ONE_MINUTE;
    public static long ONE_DAY    = 24L*ONE_HOUR;

    public final static String MMSS_TIME_FORMAT     = "mm:ss";
    public final static String KKMM_TIME_FORMAT     = "kk:mm";
    public final static String KKMM_SPACE_TIME_FORMAT = "kk mm";

    public final static String KK_TIME_FORMAT     = "kk";
    public final static String MM_TIME_FORMAT     = "mm";

    public final static String DDMMYYYY_DATA_FORMAT = "dd-MM-yyyy";
    public final static String YYYYMMDD_DATA_FORMAT = "yyyy-MM-dd";
    public final static long   MILLIS_TO_DATA  = 25L*24L*60L*60L*1000L;


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

}
