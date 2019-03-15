package frames;

import bebug.Log;
import db.Db;
import db.DbDateRange;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.converter.IntegerStringConverter;
import settings.CachedSettings;
import utils.FileIO;

import java.io.File;
import java.util.List;
import java.util.function.UnaryOperator;

import static broadcast.Broadcast.DatasetManualChangedNotification;
import static broadcast.Broadcast.SettingsChangedNotification;
import static utils.Auxiliary.isNumeric;

public class SettingsController {

    private static final String TAG = "SETTINGS_CONTROLLER";

    public TextField loopDelayText;
    public Button saveButton;
    public Button closeButton;
    public Button clearTodayMarksButton;
    public Button clearAllMarksButton;
    public Button resetPopularityButton;
    public Button removeAllVehiclesButton;
    public Button allDbRemoveButton;
    public Button runScriptButton;

    private File initialDirectory;

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

        /* Настраиваем кнопку "Запустить скрипт" */
        runScriptButton.setGraphic(new ImageView(new Image("images/run-script-file-24.png")));
        runScriptButton.setOnAction(event -> runScriptDialog());
    }

    private void runScriptDialog() {
        final FileChooser fileChooser = new FileChooser();
        // Set title for FileChooser
        fileChooser.setTitle("Выберите скрипт-файлы для выполнения");
        // Add Extension Filters
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("SQL", "*.sql"),
                new FileChooser.ExtensionFilter("TXT", "*.txt"),
                new FileChooser.ExtensionFilter("Все файлы", "*.*"));
        // Устанавливается домашняя директория.
        //fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        if (initialDirectory != null) fileChooser.setInitialDirectory(initialDirectory);
        // Вызывается диалог выбора файлов.
        List<File> files = fileChooser.showOpenMultipleDialog(closeButton.getScene().getWindow());
        // Если пользователь выбрал хотя бы один файл.
        if (files != null && !files.isEmpty()){
            // Сохраняется выбранная директория на время сессии.
            initialDirectory = files.get(0).getParentFile();
            // Проверяется количество файлов в списке
            if (files.size() > 50) {
                Log.printerror(TAG, "runScriptDialog","Ошибка: общее количество файлов скриптов "+ files.size()+" шт. превышает установленный порог в 50 шт.", null);
                return;
            }
            Log.println("Выполняются файлы скриптов. Общее количество: "+ files.size()+" шт.");
            // Перебираем список файлов.
            for (File file:files) {
                Log.println("Обрабатывается файл: "+ file.toString());
                // Защита от БОЛЬШИХ файлов: если размер файла превышает установленный предел - переходим к следующему файлу.
                if (file.length() > CachedSettings.MAX_SQL_SCRIPT_SIZE) {
                    Log.printerror(TAG, "runScriptDialog","Ошибка: размер файла в "+file.length()+" байт превышает установленный порога в "+CachedSettings.MAX_SQL_SCRIPT_SIZE+" байт.", null);
                    continue;
                }
                String script = FileIO.readFile(file);
                // Если скрипт не пуст, то выполняем его.
                if (!script.isEmpty()){
                    Db db = Db.getInstance();
                    //if (db.executeScript("SET FOREIGN_KEY_CHECKS=0;\nSET FOREIGN_KEY_CHECKS=1;"/*script*/)){
                    if (db.executeScript(script)){
                        Log.println("Скрипт выполнен - [ОК].");
                    } else
                        Log.println("Скрипт не выполнен - [FAIL].");
                }
            }
            Log.println("Выполнение скриптов завершено.");
        }
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
        Db.getInstance().toggleMarks(null, null, new DbDateRange(true),  Db.DataToggleTypes.REAL_DELETE);
        // Уведомляем подписчика о том, что набор данных был изменен.
        DatasetManualChangedNotification();
    }

    private void clearTodayMarks() {
        // Получаем экземпляр класса для работы с БД.
        Db.getInstance().toggleMarks(null,null, null, Db.DataToggleTypes.REAL_DELETE);
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

    private void closeWindow(){
        // get a handle to the stage
        Stage stage = (Stage) closeButton.getScene().getWindow();
        // do what you have to do
        //stage.close();
        stage.hide();
    }
}
