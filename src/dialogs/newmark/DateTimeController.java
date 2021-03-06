package dialogs.newmark;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import javafx.util.converter.DateTimeStringConverter;
import utils.Auxiliary;
import utils.time.DateTime;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;

import static utils.time.DateTime.getVisualDateConverter;

public class DateTimeController {

    private static final int MAX_COMMENT_LENGTH = 40;

    public DatePicker dateField;
    public TextField  timeField;
    public Button saveButton;
    public Slider hoursSlider;
    public Slider minutesSlider;
    public Button cancelButton;
    public TextField commentField;
    public TextField repeatField;
    public TextField stepField;

    private String timestamp;

    public DateTimeController() {
    }

    @FXML
    public void initialize() {

        // Инициализация слушателей
        initListeners();

        /////////////////////////
        // Настройка поля ДАТА //
        /////////////////////////
        dateField.setValue(LocalDate.now());
        dateField.setConverter(getVisualDateConverter());
        dateField.setPromptText("dd-MM-yyyy");

        //////////////////////////
        // Настройка поля ВРЕМЯ //
        //////////////////////////
        final SimpleDateFormat format = new SimpleDateFormat("HH:mm");
        try {
            timeField.setTextFormatter(new TextFormatter<>(new DateTimeStringConverter(format), format.parse("00:00")));
        } catch (ParseException e) {
            e.printStackTrace();
            return;
        }
        timeField.setText(DateTime.getTimeKKMM());

        // restrict key input to numerals.
        timeField.addEventFilter(KeyEvent.KEY_TYPED, keyEvent -> {
            if (!"0123456789:".contains(keyEvent.getCharacter())) {
                keyEvent.consume();
            }
        });
        timeField.setOnKeyTyped(event -> {
            // for future
        });

        //////////////////////////////
        // Настройка бегунка "ЧАСЫ" //
        //////////////////////////////
        hoursSlider.setValue(Double.valueOf(DateTime.getTimeKK()));

        hoursSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (hoursSlider.isValueChanging()){
                // Получаем текущее значение часов и минут.
                String minutes = timeField.getText().split(":")[1];

                // Получаем строку - количество часов.
                int value = newValue.intValue();
                String newHours = Integer.valueOf(value).toString();
                if (newHours.length()==1) newHours = "0"+newHours;

                String newTime = newHours + ":" + minutes;

                timeField.setText(newTime);
                timeField.commitValue();
            }
        });

        ////////////////////////////////
        // Настройка бегунка "МИНУТЫ" //
        ////////////////////////////////
        minutesSlider.setValue(Double.valueOf(DateTime.getTimeMM()));

        minutesSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (minutesSlider.isValueChanging()){
                // Получаем текущее значение часов и минут.
                String hours   = timeField.getText().split(":")[0];

                // Получаем строку - количество часов.
                int value = newValue.intValue();
                String newMinutes = Integer.valueOf(value).toString();
                if (newMinutes.length()==1) newMinutes = "0"+newMinutes;

                String newTime = hours + ":" + newMinutes;

                timeField.setText(newTime);
                timeField.commitValue();
            }
        });

        ///////////////////////////////////////
        // Настройка поля "ЧИСЛО ПОВТОРЕНИЙ" //
        ///////////////////////////////////////
        repeatField.addEventFilter(KeyEvent.KEY_TYPED, keyEvent -> {
            if (!"0123456789".contains(keyEvent.getCharacter()) || repeatField.getLength() > 5) {
                keyEvent.consume();
            }
        });
        repeatField.setText("1");

        /////////////////////////////////////
        // Настройка поля "ШАГ ПОВТОРЕНИЙ" //
        /////////////////////////////////////
        stepField.addEventFilter(KeyEvent.KEY_TYPED, keyEvent -> {
            if (!"0123456789".contains(keyEvent.getCharacter()) || stepField.getLength() > 5) {
                keyEvent.consume();
            }
        });
        stepField.setText("1");

        //////////////////////////////////
        // Настройка поля "КОММЕНТАРИЙ" //
        //////////////////////////////////
        commentField.addEventFilter(KeyEvent.KEY_TYPED, keyEvent -> {
            // Ограничим количество вводимых символов
            if (commentField.getText().length() > MAX_COMMENT_LENGTH)keyEvent.consume();
        });
    }

    public String getComment(){return Auxiliary.constrainLength(commentField.getText(), MAX_COMMENT_LENGTH);}

    public String getTimestamp() {return timestamp;}
    public int getRepeatCount() {
        if (!repeatField.getText().isEmpty()){
            return Integer.parseInt(repeatField.getText());
        }
        return 1;
    }
    public int getStepsCount() {
        if (!stepField.getText().isEmpty()){
            return Integer.parseInt(stepField.getText());
        }
        return 1;
    }

    private void saveTimestamp(){
        timestamp = dateField.getValue() + " " + timeField.getText() + ":00";
        closeWindow();
    }


    private void initListeners() {
        /* Настраиваем кнопку "ЗАКРЫТЬ ОКНО" */
        cancelButton.setOnAction(event -> closeWindow());
        /* Настраиваем кнопку "СОЗДАТЬ" */
        saveButton.setOnAction(event -> saveTimestamp());
    }

    private void closeWindow(){
        // get a handle to the stage
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.getOnCloseRequest().handle(null);
        // do what you have to do
        stage.close();
    }
}
