package servcmd;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Parser {

    private final static String PATTERN_DECIMAL_PAIR = "\\{[0-9]*:[0-9]*}";
    private final static String STD_SPLIT_PATTERN = ":";


    public static List<Integer[]> parseIntPair(StringBuffer text, boolean cleanup){
        return parseIntArrayList(text, PATTERN_DECIMAL_PAIR, cleanup);
    }

    // Возвращает спиок из числовых массивов, соответствующих регулярному выражению.
    private static List<Integer[]> parseIntArrayList(StringBuffer text, String pattern, boolean cleanup){

        List<Integer[]> resultList = new ArrayList<>();

        List<String> stringList = parseServiceCommands(text.toString(), pattern);

        if (stringList != null && !stringList.isEmpty()){

            // Производим очистку исходного текста от служебных символов, если это требуется.
            if (cleanup) {
                String dirtyText = text.toString();
                // Удаляем старое содержимое.
                text.setLength(0);
                // Устанавливаем новое содержимое.
                text.append(cleanupText(dirtyText, pattern));
            }

            // Перебираем все найденные совпадения.
            for (String stringPair: stringList) {
                resultList.add(splitToIntArray(trimWrapes(stringPair.trim())));
            }
        }

        return resultList;
    }

    // Разбить строку из десятичных символов, разделенных шаблоном STD_SPLIT_PATTERN и вернуть
    // массив из десятичных чисел.
    private static Integer[] splitToIntArray(String string){

        if (isEmpty(string)) return new Integer[0];

        String[] stringArray = string.split(STD_SPLIT_PATTERN);

        Integer[] intArray = new Integer[stringArray.length];

        for (int ii = 0; ii < stringArray.length; ii++) intArray[ii] = Integer.parseInt(stringArray[ii]);

        return intArray;
    }

    // Возвращает набор сервисных команд, которые присутствуют в произвольном тексте.
    private static List<String> parseServiceCommands(String text, String regex){

        List<String> list = new ArrayList<>();

        if (!isEmpty(text) && !isEmpty(regex)) {
            Pattern p = Pattern.compile(regex);
            Matcher matcher = p.matcher(text);

            while (matcher.find()) list.add(matcher.group());
        }
        return list;
    }

    // Очищает текст от всех найденных служебных команд.
    private static String cleanupText(String text, String regex){

        if (!isEmpty(text) && !isEmpty(regex)) {
            Pattern p = Pattern.compile(regex);
            Matcher matcher = p.matcher(text);

            while (matcher.find()) text = text.replace(matcher.group(), "");
        }
        return text;
    }

    // Удаляет фигурные скобки в конце и начале строки.
    public static String trimWrapes(String str) {
        if (!isEmpty(str)) {
            return str.replaceAll("^\\{*", "").replaceAll("\\}*$", "");
        }
        return str;
    }

    public static String trimQuotes(String str) {
        if (!isEmpty(str)) {
            return str.replaceAll("^\"*", "").replaceAll("\"*$", "");
        }
        return str;
    }

    private static boolean isEmpty(CharSequence str) {
        return str == null || str.toString().isEmpty();
    }
}
