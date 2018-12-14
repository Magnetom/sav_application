package general;

import bebug.Log;
import broadcast.Broadcast;
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
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.util.Duration;
import marks.VehicleItem;
import marks.VehicleMark;

import java.sql.SQLException;

public class Main extends Application {

    private static MainController mainController;

    private Db db; // Класс для доступа к БД.
    private ScheduledService dbPollService; // Сервис опроса БД.
    private ScheduledService clockService; // Сервис-часы реального времени.

    private Boolean manualSampleTrigger;


    @Override
    public void start(Stage primaryStage) throws Exception{

        // Инициализируем графику.
        //Parent root = FXMLLoader.load(getClass().getResource("main.fxml"));
        FXMLLoader loader = new FXMLLoader(getClass().getResource("main.fxml"));
        Parent root = loader.load();
        mainController = loader.getController();

        primaryStage.getIcons().add(new Image("images/favicon.png"));
        //primaryStage.setTitle("SAV v1.0 - the System of Accounting of Vehicles");
        primaryStage.setTitle("SAV v1.0 - система учета ходок автотранспорта");
        primaryStage.setScene(new Scene(root, -1, -1));
        primaryStage.show();

        // Инициализируем подключение к БД.
        dbInit();
        // Инициализация состояния основных элементов графического контроля и управления.
        initControls();
        // Инициализируем сервис для периодического опроса базы данных.
        setupService();
        // Инициализируем слушатаелей сообщений от различных модулей.
        setupBroadcastListeners();
    }

    private void setupBroadcastListeners() {

        // Были произведены изменения в настройках БД.
        Broadcast.setSettingsChangedInterface(() -> {
            // Загружаем все настройки с сервера БД и визуализируем их заново.
            initControls();
            Log.println("Набор переменных на сервере изменен вручную.");
        });

        // Набор данных на сервере был изменен вручную каким-либо компонентом системы.
        Broadcast.setDatasetInterface(() -> {
            manualSampleTrigger = true;
            Log.println("Набор данных на сервере изменен вручную.");
        });
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

        /* Отображение текущего интервала между отметками. */
        val = db.getSysVariable("mark_delay");
        if (val == null || val.toString().equals("0")){
            mainController.setMarkDelayView("15*");
        } else {
            mainController.setMarkDelayView(val.toString());
        }

        Log.println("Набор переменных был загружен с сервера.");
    }

    private void dbInit() {
        db = Db.getInstance();
    }

    public static void main(String[] args) {
        launch(args);
    }

    // Настройка сервисов.
    private void setupService(){

        //**************************************************/
        /* Сервис для отображения часов реального времени. */
        //**************************************************/
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
                            if (db.isDatasetModifed() || manualSampleTrigger) {
                                manualSampleTrigger = false;

                                // Получаем список всех отметок за сегодняшнюю дату.
                                final ObservableList<VehicleMark> markList = FXCollections.observableArrayList(db.getMarksRawList());
                                // Обновляем GUI элемент из основного потока GUI.
                                Platform.runLater(() -> mainController.printMarksLog(markList));

                                // Получаем статистику по всем ТС за сегодняшнюю дату.
                                final ObservableList<VehicleItem> statList = FXCollections.observableArrayList(db.getVehiclesStatistic(markList));
                                // Обновляем GUI элемент из основного потока GUI.
                                Platform.runLater(() -> mainController.printStatisticList(statList));

                                // Получаем список всех зарегистрированных ТС.
                                final ObservableList<VehicleItem> vehiclesList = FXCollections.observableArrayList(db.getAllVehicles());
                                // Обновляем GUI элемент из основного потока GUI.
                                Platform.runLater(() -> mainController.printAllVehicles(vehiclesList));

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
