package general;

import db.Db;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.stage.Stage;
import javafx.util.converter.IntegerStringConverter;

import java.util.function.UnaryOperator;

import static broadcast.Broadcast.DatasetManualChangedNotification;
import static broadcast.Broadcast.SettingsChangedNotification;
import static utils.utils.*;

public class SettingsController {

    public TextField loopDelayText;
    public Button saveButton;
    public Button closeButton;
    public Button clearTodayMarksButton;
    public Button clearAllMarksButton;
    public Button resetPopularityButton;
    public Button removeAllVehiclesButton;
    public Button allDbRemoveButton;

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
        loopDelayText.setTextFormatter(new TextFormatter<>(new IntegerStringConverter(), 0, integerFilter));
        // Получаем экземпляр класса для работы с БД.
        Db db = Db.getInstance();
        // Получаем актуальное значение из БД.
        Object val = db.getSysVariable("mark_delay");
        if (val == null){
            loopDelayText.setText("0");
        } else
            if (val.toString().equals("0")){
                loopDelayText.setText("15");
            }
            else {
                loopDelayText.setText(val.toString());
            }

        /* Настраиваем кнопку "СОХРАНИТЬ НАСТРОЙКИ" */
        saveButton.setOnAction(event -> saveAllSettings());
        /* Настраиваем кнопку "ЗАКРЫТЬ ОКНО НАСТРОЕК" */
        closeButton.setOnAction(event -> closeWindow());
        /* Настраиваем кнопку "ОЧИСТИТЬ ВСЕ ОТМЕТКИ (текущая дата)" */
        clearTodayMarksButton.setOnAction(event -> clearTodayMarks());
        /* Настраиваем кнопку "ОЧИСТИТЬ ВСЕ ОТМЕТКИ (все дата)" */
        clearAllMarksButton.setOnAction(event -> clearAllMarks());
        /* Настраиваем кнопку "ОБНУЛИТЬ ВСЕ РЕЙТИНГИ" */
        resetPopularityButton.setOnAction(event -> resetPopularity());
        /* Настраиваем кнопку "УДАЛИТЬ ВСЕ ТРАНСПОРТНЫЕ СРЕДСТВА" */
        removeAllVehiclesButton.setOnAction(event -> removeAllVehicles());
        /* Настраиваем кнопку "ОЧИСТИТЬ БАЗУ ДАННЫХ ПОЛНОСТЬЮ" */
        allDbRemoveButton.setOnAction(event -> allDbRemove());
    }

    private void allDbRemove() {
        // Получаем экземпляр класса для работы с БД.
        Db.getInstance().allDbRemove();
        // Уведомляем подписчика о том, что набор данных был изменен.
        DatasetManualChangedNotification();
        // Уведомляем подписчика о том, что настройки были изменены.
        SettingsChangedNotification();
    }

    private void removeAllVehicles() {
        // Получаем экземпляр класса для работы с БД.
        Db.getInstance().removeAllVehicles();
        // Уведомляем подписчика о том, что набор данных был изменен.
        DatasetManualChangedNotification();
    }

    /*  */
    private void resetPopularity() {
        // Получаем экземпляр класса для работы с БД.
        Db.getInstance().resetPopularity();
        // Уведомляем подписчика о том, что набор данных был изменен.
        DatasetManualChangedNotification();
    }

    private void clearAllMarks() {
        // Получаем экземпляр класса для работы с БД.
        Db.getInstance().removeMarks("");
        // Уведомляем подписчика о том, что набор данных был изменен.
        DatasetManualChangedNotification();
    }

    private void clearTodayMarks() {
        // Получаем экземпляр класса для работы с БД.
        Db.getInstance().removeMarks(null);
        // Уведомляем подписчика о том, что набор данных был изменен.
        DatasetManualChangedNotification();
    }

    private void saveAllSettings(){
        // Получаем экземпляр класса для работы с БД.
        Db db = Db.getInstance();

        /* Сохранение значения: МИНИМАЛЬНОЕ ВРЕМЯ КРУГА */
        String val = loopDelayText.getText();
        // Проверка на соответствие критерию: строка содержит только числа.
        if ( (val != null) && (!val.isEmpty()) && isNumeric(val)){
            db.setSysVariable("mark_delay", val);
        }

        // Уведомляем подписчика о том, что настройки были изменены.
        SettingsChangedNotification();
    }

    protected void closeWindow(){
        // get a handle to the stage
        Stage stage = (Stage) closeButton.getScene().getWindow();
        // do what you have to do
        stage.close();
    }

}
