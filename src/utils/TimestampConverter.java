package utils;

import javafx.util.StringConverter;

import java.time.format.DateTimeFormatter;

abstract class TimestampConverter<T> extends StringConverter<T> {
    DateTimeFormatter timestampFormatter;

    TimestampConverter(String pattern) {
        timestampFormatter = DateTimeFormatter.ofPattern(pattern);
    }
}
