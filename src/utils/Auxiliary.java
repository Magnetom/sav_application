package utils;

import javafx.util.StringConverter;

import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Random;

public class Auxiliary {

    public static boolean isNumeric(String str)
    {
        NumberFormat formatter = NumberFormat.getInstance();
        ParsePosition pos = new ParsePosition(0);
        formatter.parse(str, pos);
        return str.length() == pos.getIndex();
    }

    /*
     *  Brief: Generating Unique Identification as integer.
     *  For example: 1853601
     */
    public static int getRandom32(){

        final int min = 0x00000000;
        final int max = 0xFFFFFFFF;

        Random r = new Random();
        return (r.nextInt(max - min + 1) + min);
    }

    public static String getRandom32String(){
        return Integer.valueOf(getRandom32()).toString();
    }

    public static String genStrongUidString(){
        String random = DateTime.getCurrentTimeStamp();
        if (random==null) random = getRandom32String();
        return Hash.MD5(random);
    }

    public static String genStrongUidString(int maxLength){
        return constrainLength(genStrongUidString(), maxLength);
    }

    public static String constrainLength(String text, int maxLength){
        if (maxLength == 0) return "";
        if (text != null && !text.isEmpty() && maxLength > 0){
            if (text.length()>maxLength) text = text.substring(0,maxLength-1);
        }
        return text;
    }

    public static String alignTwo(int src){
        String s1 = Integer.valueOf(src).toString();
        if (s1.length() == 1) s1 = "0" + s1;
        return s1;
    }

    public static StringConverter<String> getStdStringConverter(){
        return new StringConverter<String>() {
            @Override
            public String toString(String object) {
                return object;
            }

            @Override
            public String fromString(String string) {
                return string;
            }
        };
    }
}

