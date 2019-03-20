package general;

import bebug.Log;
import broadcast.Broadcast;
import broadcast.OnDbConnectionChanged;
import db.Db;
import db.DbTimestampRange;
import enums.Users;
import items.VehicleItem;
import items.VehicleMarkItem;
import items.VehicleStatisticItem;
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
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import javafx.util.Duration;
import settings.LocalSettings;
import utils.Auxiliary;

import java.sql.SQLException;

public class Main extends Application {

    private static int SW_STAGE     = 1;  // Стадия/этап.
    private static int SW_BUILD     = 12; // Сборка.
    private static int SW_REVISION  = 1;  // Ревизия.
    // Текущая версия программного обеспечения.
    private static final String SW_VERSION_S = SW_STAGE + "." + Auxiliary.alignTwo(SW_BUILD) + "." + Auxiliary.alignTwo(SW_REVISION);

    private static MainController mainController;

    private Db db; // Класс для доступа к БД.
    private ScheduledService dbPollService; // Сервис опроса БД.

    private boolean manualSampleTrigger;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception{
        // Инициализация все глобальных и вспомогательных переменных.
        initVariables();

        // Загрузка локальных настоек в кеш настроек.
        initLocalSettings();

        // Инициализация GUI.
        initGUI (primaryStage);

        // Инициализируем слушатаелей сообщений от различных модулей.
        setupBroadcastListeners();

        // Инициализируем основные сервисы.
        setupServices();

        // Инициализируем базу данных и удаленное подключение к ней.
        dbInit();
    }

    // Инициализация графического интерфейса пользователя.
    private void initGUI (Stage primaryStage) throws Exception{
        // Инициализируем графику.
        //Parent root = FXMLLoader.load(getClass().getResource("main.fxml"));
        FXMLLoader loader = new FXMLLoader(getClass().getResource("main.fxml"));
        Parent root = loader.load();

        primaryStage.getIcons().add(new Image("images/favicon.png"));
        primaryStage.setTitle("\"АУРА\" v"+ SW_VERSION_S +" - система Автоматизированного Учета Рейсов Автотранспорта");
        primaryStage.setScene(new Scene(root, -1, -1));
        primaryStage.show();

        mainController = loader.getController();
        // Закрываем все дочерние окна, поражденные основным контроллером.
        primaryStage.setOnCloseRequest(event -> mainController.closeAllChildStages());
        // При нажатии на F5 принудительно обновляем содержимое всех визуальных форм.
        root.setOnKeyPressed(event -> {
            // Ручное обновление набора всех данных.
            if (event.getCode() == KeyCode.F5){
                manualSampleTrigger = true;
            }
            // Переключение между пользователями.
            if (event.getCode() == KeyCode.F8){
                mainController.onMasterSetupRequest(null);
            }
        });
    }

    private void initLocalSettings() {
        LocalSettings.reloadSettings();
    }

    private void initVariables() {
        manualSampleTrigger = false;
    }

    private void setupBroadcastListeners() {

        // Были произведены изменения в настройках БД.
        Broadcast.setSettingsChangedInterface(() -> {
            // Загружаем все настройки с сервера БД и визуализируем их заново.
            initDbControls();
            Log.println("Набор переменных на сервере изменен вручную.");
        });

        // Набор данных на сервере был изменен вручную каким-либо компонентом системы.
        Broadcast.setDatasetInterface(() -> {
            manualSampleTrigger = true;
            Log.println("Набор данных на сервере изменен вручную.");
        });

        // Требование перезапустить соединение с БД (от кнопку управления глобальным разрешением/запрещением отметок).
        Broadcast.setDbReconnectionRequest(() -> {
            Log.println("Выполняется ручное переподключение к серверу БД ...");
            if (db!=null) db.connect();
        });
    }

    // Инициализация элементов контроля и управления.
    private void initDbControls() {
        Log.println("Инициализация элементов контроля и управления.");

        /* Кнопка глобального Запрещения/Разрешения отметок. */
        // Проверяется наличие системной переменной global_blocked в БД.
        Object val = db.getSysVariable(Db.TABLE_VARIABLES_ROWS.SYS_VAR_GLOBAL_BLOCKED);
        // Если она отсутствует, то считаем, что эта переменная включена.
        // Если ее значение "1", то это также означает, что переменная включена.
        if (val == null || val.toString().equals("1")){
            // Так как эта функция может быть вызвана из другого потока, а нам необходимо гарантированно запустить
            // изменение графических элементов из потока GUI, то делаем таким образом:
            Platform.runLater(() -> mainController.setImageOff());
        } else {
            Platform.runLater(() -> mainController.setImageOn());
        }

        /* Отображение текущего интервала между отметками. */
        val = db.getSysVariable(Db.TABLE_VARIABLES_ROWS.SYS_VAR_MARK_DELAY);
        if (val == null || val.toString().equals("0")){
            Platform.runLater(() -> mainController.setMarkDelayView("15*"));
        } else {
            Object finalVal = val;
            Platform.runLater(() -> mainController.setMarkDelayView(finalVal.toString()));
        }

        // Запуск потока опроса значения набора данных БД.
        if(!dbPollService.isRunning()) dbPollService.start();
    }

    // Перевод всех элементов в неативное состояние (в случае разрыва связи с БД).
    private void disableDbControls (){
        Log.println("Деактивация элементов контроля и управления.");

        Platform.runLater(() -> mainController.setImageOffError());
        Platform.runLater(() -> mainController.setMarkDelayView("15*"));

        // Остановка потока опроса значения набора данных БД.
        if(dbPollService.isRunning()) dbPollService.cancel();
    }

    private void dbInit() {
        // Получаем/создаем экземпляр класса для работы с БД.
        db = Db.getInstance();

        // Устанавливаем слушателя на изменения состояния в работе с удаленной БД.
        db.setOnDbConnectionChangedListener(new OnDbConnectionChanged() {

            @Override
            public void onConnect() {
                initDbControls();
            }

            @Override
            public void onDisconnect(boolean failFlag) {
                if (failFlag){
                    Log.println("Обнаружены неполадки в подключении к серверу БД! Все сервисы приостановлены.");
                }
                disableDbControls();
            }
        });

        // Подсоединяемся к БД.
        db.connect();
    }

    // Настройка сервисов.
    private void setupServices(){

        //**************************************************/
        /* Сервис для отображения часов реального времени. */
        //**************************************************/
        // Сервис-часы реального времени.
        ScheduledService clockService = new ScheduledService() {
            @Override
            protected Task createTask() {
                return new Task() {
                    @Override
                    protected Object call() {
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

                    @Override protected Void call() {

                        // Проверяем, изменился ли набор данных на сервере, чтобы не загружать лишний раз данные, которые
                        // не изменялись со времени последней выборки из БД.
                        try {
                            if (db.isDatasetModified() || manualSampleTrigger) {
                                manualSampleTrigger = false;

                                // Получаем диапазон выборки, выбранный пользователем.
                                final DbTimestampRange dateRange = mainController.getUserSelectedTimestampRange();
                                // Плучаем текущего пользователя.
                                final Users currUser = mainController.getCurrentUser();

                                // Определяем (в зависимости от текущего пользователя), отображать ли скрытые/удаленные элементы в списках.
                                boolean showDeletedItems = false;

                                switch (currUser){
                                    case USER: showDeletedItems = false; break; // Для пользователя USER скрываем удаленные ранее элементы.
                                    case ADMIN:showDeletedItems = true;  break; // Для пользователя ADMIN отображаем.
                                }

                                // Получаем список всех грузовместимостей
                                //final ObservableList<VehicleCapacityItem> capacityList = FXCollections.observableArrayList(db.getInstance().getCapacity(null));


                                // Получаем список всех отметок за дату.
                                final ObservableList<VehicleMarkItem> markList = FXCollections.observableArrayList(db.getMarksRawList(dateRange, showDeletedItems));
                                // Обновляем GUI элемент из основного потока GUI.
                                Platform.runLater(() -> mainController.printMarksLog(markList));

                                // Получаем статистику по всем ТС за дату.
                                final ObservableList<VehicleStatisticItem> statList = FXCollections.observableArrayList(db.getVehiclesStatistic(dateRange, markList));
                                // Обновляем GUI элемент из основного потока GUI.
                                Platform.runLater(() -> mainController.printStatisticList(statList));

                                // Получаем список всех зарегистрированных ТС.
                                final ObservableList<VehicleItem> vehiclesList = FXCollections.observableArrayList(db.getAllVehicles(showDeletedItems));
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
                        //dbPollService.restart();
                        Log.println("Поток опроса базы данных завершился с ошибкой!");
                    }
                };
            }
        };
        //dbPollService.setDelay(new Duration(500));  // Задержка перед стартом.
        dbPollService.setPeriod(new Duration(1000)); // Период повторения.
        // Если инициализация прошла успешно, тогда запускаем сервис опроса БД.
        //dbPollService.start();
    }
}
