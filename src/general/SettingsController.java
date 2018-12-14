package general;

import db.Db;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.stage.Stage;
import javafx.util.converter.IntegerStringConverter;

import java.util.function.UnaryOperator;

import static broadcast.Broadcast.SettingsChangedNotification;
import static utils.utils.*;

public class SettingsController {

    public TextField loopDelay;
    public Button saveButton;
    public Button closeButton;

    @FXML
    public void initialize() {

        // Создаем фильтр: только цифры и не более трех символов.
        UnaryOperator<TextFormatter.Change> integerFilter = change -> {
            String newText = change.getControlNewText();
            if (newText.length() > 3) return null;
            if (newText.matches("-?([1-9][0-9]*)?")) {
                return change;
            }
            return null;
        };

        /* Настраиваем поле ввода: МИНИМАЛЬНАЯ ВРЕМЯ КРУГА */
        loopDelay.setTextFormatter(new TextFormatter<>(new IntegerStringConverter(), 0, integerFilter));
        // Получаем экземпляр класса для работы с БД.
        Db db = Db.getInstance();
        // Получаем актуальное значение из БД.
        Object val = db.getSysVariable("mark_delay");
        if (val == null){
            loopDelay.setText("0");
        } else
            if (val.toString().equals("0")){
                loopDelay.setText("15");
            }
            else {
                loopDelay.setText(val.toString());
            }

        /* Настраиваем кнопку "СОХРАНИТЬ НАСТРОЙКИ" */
        saveButton.setOnAction(event -> saveAllSettings());
        /* Настраиваем кнопку "ЗАКРЫТЬ ОКНО НАСТРОЕК" */
        closeButton.setOnAction(event -> closeWindow());
    }

    private void saveAllSettings(){
        // Получаем экземпляр класса для работы с БД.
        Db db = Db.getInstance();

        /* Сохранение значения: МИНИМАЛЬНОЕ ВРЕМЯ КРУГА */
        String val = loopDelay.getText();
        // Проверка на соответствие критерию: строка содержит только числа.
        if ( (val != null) && (!val.isEmpty()) && isNumeric(val)){
            db.setSysVariable("mark_delay", val);
        }


        // Уеведомляем пописчика о том, что настройки были изменены.
        SettingsChangedNotification();
    }

    protected void closeWindow(){
        // get a handle to the stage
        Stage stage = (Stage) closeButton.getScene().getWindow();
        // do what you have to do
        stage.close();
    }

}
