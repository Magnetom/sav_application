package general;

import bebug.Log;
import db.Db;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;
import marks.VehicleInfo;
import marks.VehicleMark;

import java.sql.SQLException;

public class Main extends Application {

    private static MainController mainController;

    private Db db; // Класс для доступа к БД.
    private ScheduledService dbPollService; // Сервис опроса БД.
    private ScheduledService clockService; // Сервис-часы реального времени.


    @Override
    public void start(Stage primaryStage) throws Exception{

        // Инициализируем графику.
        //Parent root = FXMLLoader.load(getClass().getResource("sample.fxml"));
        FXMLLoader loader = new FXMLLoader(getClass().getResource("sample.fxml"));
        Parent root = loader.load();
        mainController = loader.getController();

        primaryStage.setTitle("SAV - the System of Accounting of Vehicles");
        primaryStage.setScene(new Scene(root, -1, -1));
        primaryStage.show();

        // Инициализируем подключение к БД.
        dbInit();
        // Инициализация состояния основных элементов графического контроля и управления.
        initControls();
        // Инициализируем сервис для периодического опроса базы данных.
        setupService();
    }

    private void initControls() {
        /* Кнопка глобального Запрещения/Разрешения отметок. */
        // Проверяется наличие системной переменной global_blocked в БД.
        Object val = db.getSysVariable("global_blocked");
        // Если она отсутствует, то считаем, что эта переменная включена.
        // Если ее значение "1", то это также означает, что переменная включена.
        if (val == null || val.toString().equals("1")){
            mainController.setImageOff();
        } else {
            mainController.setImageOn();
        }

    }

    private void dbInit() {
        db = Db.getInstance();
    }

    public static void main(String[] args) {
        launch(args);
    }

    // Настройка сервиса для периодических запросов в базу данных.
    private void setupService(){

        /* Сервис для отображения часов реального времени. */
        clockService = new ScheduledService() {
            @Override
            protected Task createTask() {
                return new Task() {
                    @Override
                    protected Object call() throws Exception {
                        Platform.runLater(() -> mainController.refreshClocks());
                        return null;
                    }
                };
            }
        };
        clockService.setPeriod(new Duration(1000)); // Период повторения.
        clockService.start();

        //**********************************************************************************************************/
        /* Основной сервис. Делает периодические опросы БД. Инициирует вывод полученных данных в визуальные формы. */
        //**********************************************************************************************************/
        dbPollService = new ScheduledService() {
            @Override
            protected Task createTask() {
                return new Task<Void>() {

                    @Override protected Void call() throws Exception {
                        if (isCancelled()) {/* Do some actions. */}
                        // Проверяем, изменился ли набор данных на сервере, чтобы не загружать лишний раз данные, которые
                        // не изменялись со времени последней выборки из БД.

                        try {
                            if (db.isDatasetModifed()) {
                                // Получаем список всех отметок за сегодняшнюю дату.
                                final ObservableList<VehicleMark> markList = FXCollections.observableArrayList(db.getMarksRawList());
                                // Обновляем GUI элемент из основного потока GUI.
                                Platform.runLater(() -> mainController.fillMarksLog(markList));

                                // Получаем статистику по всем ТС за сегодняшнюю дату.
                                final ObservableList<VehicleInfo> statList = FXCollections.observableArrayList(db.getVehiclesStatistic(markList));
                                // Обновляем GUI элемент из основного потока GUI.
                                Platform.runLater(() -> mainController.fillStatisticList(statList));

                                Log.println("Список изменений в наборе данных успешно загружен.");
                            }
                        } catch (SQLException e){
                            e.printStackTrace();
                            Log.printerror(Db.TAG_SQL, "MAIN_THREAD",e.getMessage(), null);
                        }

                        return null;
                    }

                    @Override protected void succeeded() {
                        super.succeeded();
                        //Log.println("Запрос в базу данных выполнен.");
                    }

                    @Override protected void cancelled() {
                        super.cancelled();
                        Log.println("Поток опроса базы данных приостановлен!");
                    }

                    @Override protected void failed() {
                        super.failed();
                        //dbPollService.cancel();
                        dbPollService.restart();
                        Log.println("Поток опроса базы данных завершился с ошибкой!");
                    }
                };
            }
        };
        //dbPollService.setDelay(new Duration(500));  // Задержка перед стартом.
        dbPollService.setPeriod(new Duration(1000)); // Период повторения.
        dbPollService.start();
    }
}
