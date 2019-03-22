package dialogs.time;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import javafx.util.converter.DateTimeStringConverter;
import utils.time.DateTime;

import java.text.ParseException;
import java.text.SimpleDateFormat;


public class TimeController {

    public TextField  timeField;
    public Button saveButton;
    public Slider hoursSlider;
    public Slider minutesSlider;

    private String timestamp;

    public TimeController() { }

    @FXML
    public void initialize() {

    }

    void init_and_show (String time){

        // Инициализация слушателей
        initListeners();

        if (time == null) time = DateTime.getTimeKKMM();

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
        timeField.setText(time);

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
        hoursSlider.setValue(Double.valueOf(time.split(":")[0]));

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
        minutesSlider.setValue(Double.valueOf(time.split(":")[1]));

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
    }

    String getTimestamp() {return timestamp;}

    private void saveTimestamp(){
        //timestamp = timeField.getText() + ":00";
        timestamp = timeField.getText();
        closeWindow();
    }


    private void initListeners() {

        /* Настраиваем кнопку "СОЗДАТЬ" */
        saveButton.setOnAction(event -> saveTimestamp());

        //saveButton.setGraphic(new ImageView(new Image("images/arrow-right-24.png")));

        ImageView image = new ImageView(new Image("images/arrow-right-24.png"));
        image.setFitWidth(16.0);
        image.setFitHeight(16.0);
        saveButton.setGraphic(image);
    }

    private void closeWindow(){
        // get a handle to the stage
        Stage stage = (Stage) saveButton.getScene().getWindow();
        stage.getOnCloseRequest().handle(null);
        // do what you have to do
        stage.close();
    }
}
