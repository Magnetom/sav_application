package utils;

import javafx.util.StringConverter;

import java.time.format.DateTimeFormatter;

public abstract class DateConverter<T> extends StringConverter<T> {
    DateTimeFormatter dateFormatter;

    DateConverter(String pattern) {
        dateFormatter = DateTimeFormatter.ofPattern(pattern);
    }
}
