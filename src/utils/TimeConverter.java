package utils;

import javafx.util.StringConverter;

import java.time.format.DateTimeFormatter;

abstract class TimeConverter<T> extends StringConverter<T> {
    DateTimeFormatter timeFormatter;

    TimeConverter(String pattern) {
        timeFormatter = DateTimeFormatter.ofPattern(pattern);
    }
}
