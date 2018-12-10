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
import marks.VehicleMark;

public class Main extends Application {

    private static MainController mainController;
    private Db db;
    private ScheduledService service;

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
        // Инициализируем сервис для периодического опроса базы данных.
        setupService();
    }

    private void dbInit() {
        db = new Db();
    }

    void fillGUI(){
        mainController.fillMarksLog(getUserList());
    }

    public static void main(String[] args) {
        launch(args);
    }


    private ObservableList<VehicleMark> getUserList() {

        VehicleMark mark1 = new VehicleMark("13:26","M750AM750");
        VehicleMark mark2 = new VehicleMark("18:15","A999VH99");
        VehicleMark mark3 = new VehicleMark("13:39","A999VH99");
        VehicleMark mark4 = new VehicleMark("12:45","B897BA42");

        ObservableList<VehicleMark> list = FXCollections.observableArrayList(mark1, mark2, mark3, mark4);
        return list;
    }

    // Настройка сервиса для периодических запросов в базу данных.
    private void setupService(){

        service  = new ScheduledService() {
            @Override
            protected Task createTask() {
                return new Task<Void>() {

                    @Override protected Void call() throws Exception {
                        if (isCancelled()) {/* Do some actions. */}
                        // Проверяем, изменился ли набор данных на сервере, чтобы не загружать лишний раз данные, которые
                        // не изменялись со времени последней выборки из БД.
                        if ( db.isDatasetModifed() ){
                            // Получаем список всех отметок за сегодняшнюю дату.
                            final ObservableList<VehicleMark> list = FXCollections.observableArrayList(db.getMarksRawList());
                            // Обновляем GUI элемент из основного потока GUI.
                            Platform.runLater(() -> mainController.fillMarksLog( list ));
                            Log.println("Список изменений в наборе данных успешно загружен.");
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
                        Log.println("Поток опроса базы данных завершился с ошибкой!");
                    }
                };
            }
        };
        //service.setDelay(new Duration(500));  // Задержка перед стартом.
        service.setPeriod(new Duration(1000)); // Период повторения.
        service.start();
    }
}
