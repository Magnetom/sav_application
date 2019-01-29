package general;

import bebug.Log;
import bebug.LogInterface;
import broadcast.Broadcast;
import db.Db;
import db.DbProc;
import dialogs.datetime.DateTimeDialog;
import enums.Users;
import frames.SettingsController;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.util.Callback;
import marks.Statuses;
import marks.VehicleItem;
import marks.VehicleMark;

import java.io.IOException;
import java.util.ArrayList;

import static utils.DateTime.*;
import static utils.Hash.MD5;

public class MainController {

    public TextArea  debugLogArea;
    public TableView todayVehiclesMarksLog;
    public TableView todayVehiclesStatistic;
    public TableView allDbVehiclesList;
    public ImageView OnOffImage;

    // Цифровые часы
    public Label clockHour;
    public Label clockColon;
    public Label clockMinutes;
    public Label clockDate;

    // Отображение текущей настройки времянного интервала.
    public Label markDelay;
    public AnchorPane headerPane;

    // Содержит список ТС, которые необходимо отображать в логе отметок (список справа).
    private ArrayList<String> filteredVehicles;

    private FilteredList<VehicleMark> filteredData;

    private boolean controlButton;

    private Users currentUserType;

    private ContextMenu contextMenu;

    private Stage setupStage;

    private static SettingsController settingsController;

    /* Конструктор класса */
    public  MainController(){

        controlButton = false;

        currentUserType = Users.USER;

        filteredVehicles = new ArrayList<>();

        // Инициализируем интерфейс для вывода отладочной информации.
        Log.setInterface(new LogInterface() {
            @Override
            public void println(String mess) {
                if (debugLogArea != null && (mess != null) ) debugLogArea.appendText(mess+"\r\n");
            }

            @Override
            public void print(String mess) {
                if (debugLogArea != null && (mess != null)) debugLogArea.appendText(mess);
            }
        });
    }

    // Закрывает (если они все-еще открыты) все дочерние окна, поражденные этим контроллером.
    public void closeAllChildStages(){
        // Закрываем инженерное меню, если оно все-еще открыто.
        if (setupStage!=null)setupStage.close();
    }

    /* Полная очистка GUI интерфейса и реинициализация. */
    private void initAllGUIs(){

        // Очищается лог событий.
        if (debugLogArea != null) debugLogArea.setText("");

        // Настраиваем список подробной статистики по каждому госномеру.
        if (todayVehiclesStatistic != null){
            todayVehiclesStatistic.getColumns().clear();

            todayVehiclesStatistic.setEditable(true);

            TableColumn <VehicleItem, String>   vehicleColumn  = new TableColumn<>("Госномер");
            TableColumn <VehicleItem, Integer>  loopsColumn    = new TableColumn<>("Рейсов");
            TableColumn <VehicleItem, Statuses> statusColumn   = new TableColumn<>("Статус");
            TableColumn <VehicleItem, Boolean>  filterColumn   = new TableColumn<>("Фильтр");

            vehicleColumn.setMinWidth(90);
            loopsColumn.setMinWidth(125);
            statusColumn.setMinWidth(100);

            loopsColumn.setStyle("-fx-alignment: CENTER;");
            statusColumn.setStyle("-fx-alignment: CENTER;");

            // Defines how to fill data for each cell.
            vehicleColumn.setCellValueFactory(new PropertyValueFactory<>("vehicle"));
            loopsColumn.setCellValueFactory(new PropertyValueFactory<>("loopsCnt"));

            // Set Sort type
            loopsColumn.setSortType(TableColumn.SortType.DESCENDING);
            loopsColumn.setSortable(true);

            /////////////////////////////////////////////////////////////////////////////
            // Делаем комбо-бокс "Статус" редактируемым и вешаем на него слушателя.
            /////////////////////////////////////////////////////////////////////////////
            setupStatusComboBox(statusColumn);

            /////////////////////////////////////////////////////////////////////////////
            // Делаем чекбокс "Фильтр" редактируемым и вешаем на него слушателя.
            /////////////////////////////////////////////////////////////////////////////
            filterColumn.setCellValueFactory(param -> {

                final VehicleItem info = param.getValue();
                SimpleBooleanProperty booleanProp = new SimpleBooleanProperty(info.isFiltered());
                // Note: singleCol.setOnEditCommit(): Not work for CheckBoxTableCell.
                // When "Filtered" column change.
                booleanProp.addListener((observable, oldValue, newValue) -> {
                    updateMarksLogFilter(newValue, info.getVehicle());
                    info.setFiltered(newValue);
                });
                return booleanProp;
            });

            filterColumn.setCellFactory(p -> {
                CheckBoxTableCell<VehicleItem, Boolean> cell = new CheckBoxTableCell<>();
                cell.setAlignment(Pos.CENTER);
                return cell;
            });

            // Настраиваем контекстное меню.
            setupStatisticContextMenu();

            // Добавляем новые колонки.
            todayVehiclesStatistic.getColumns().addAll(vehicleColumn, loopsColumn, statusColumn, filterColumn);
        }

        // Настраивается лог отметок.
        if (todayVehiclesMarksLog != null) {

            todayVehiclesMarksLog.getColumns().clear();

            TableColumn <VehicleMark, String> timestampColumn = new TableColumn<>("Время");
            TableColumn <VehicleMark, String> vehicleColumn   = new TableColumn<>("Госномер");
            TableColumn <VehicleMark, String> commentColumn   = new TableColumn<>("Комментарий");

            // Defines how to fill data for each cell.
            timestampColumn.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
            vehicleColumn.setCellValueFactory(new PropertyValueFactory<>("vehicle"));
            commentColumn.setCellValueFactory(new PropertyValueFactory<>("comment"));

            timestampColumn.setMinWidth(40);
            vehicleColumn.setMinWidth(90);

            timestampColumn.setStyle("-fx-alignment: CENTER;");
            //vehicleColumn.setStyle("-fx-alignment: CENTER;");

            // Set Sort type for userName column
            timestampColumn.setSortType(TableColumn.SortType.DESCENDING);
            timestampColumn.setSortable(true);

            // Настраиваем контекстное меню.
            setupTodayVehiclesMarksLogContextMenu();

            // Добавляем новые колонки.
            todayVehiclesMarksLog.getColumns().addAll(timestampColumn, vehicleColumn, commentColumn);
        }

        // Настраиваем отображение списока всех госномеров.
        if (allDbVehiclesList != null){
            allDbVehiclesList.getColumns().clear();
            allDbVehiclesList.setEditable(true);

            TableColumn <VehicleItem, String>   vehicleColumn    = new TableColumn<>("Госномер");
            TableColumn <VehicleItem, Integer>  popularityColumn = new TableColumn<>("Рейсов");
            TableColumn <VehicleItem, Statuses> statusColumn     = new TableColumn<>("Статус");

            vehicleColumn.setMinWidth(90);
            statusColumn.setMinWidth(90);
            popularityColumn.setMinWidth(30);

            popularityColumn.setStyle("-fx-alignment: CENTER;");
            statusColumn.setStyle("-fx-alignment: CENTER;");

            // Defines how to fill data for each cell.
            vehicleColumn.setCellValueFactory(new PropertyValueFactory<>("vehicle"));
            popularityColumn.setCellValueFactory(new PropertyValueFactory<>("popularity"));

            // Set Sort type
            popularityColumn.setSortType(TableColumn.SortType.DESCENDING);
            popularityColumn.setSortable(true);

            /////////////////////////////////////////////////////////////////////////////
            // Делаем комбо-бокс "Статус" редактируемым и вешаем на него слушателя.
            /////////////////////////////////////////////////////////////////////////////
            setupStatusComboBox(statusColumn);

            // Настройка выпадающего меню.
            setupVehiclesListContextMenu();

            // Добавляем новые колонки.
            allDbVehiclesList.getColumns().addAll(vehicleColumn, statusColumn, popularityColumn);
        }
    }


    // Контекстное меню для списка всех ТС.
    private void setupVehiclesListContextMenu(){
        allDbVehiclesList.setRowFactory((Callback<TableView<VehicleItem>, TableRow<VehicleItem>>) tableView -> {
            final TableRow<VehicleItem> row = new TableRow<>();
            final ContextMenu rowMenu = new ContextMenu();
            final SeparatorMenuItem separatorMenuItem = new SeparatorMenuItem();

              // Элемент выпадающего меню для администратора - "ДОБАВИТЬ ОТМЕТКУ".
            final MenuItem addItem = new MenuItem("Добавить отметку");
            addItem.setOnAction(event -> addMarkManually(row.getItem().getVehicle()));

            // Элемент выпадающего меню для обычного пользователя.
            final MenuItem dummyItem = new MenuItem( "dummy");
            dummyItem.setOnAction(event -> {
                /* ToDo: for future */
            });

            rowMenu.getItems().addAll(dummyItem);

            rowMenu.setOnShowing(event -> {
                // Удаляем все элементы и пересоздаем меню с учетом различных факторов и состояний.
                rowMenu.getItems().clear();
                // Устанавливаем имя для элемента "dummy" контекстного меню.
                dummyItem.setText(row.getItem().getVehicle());
                // Создаем меню с учетом прав текущего пользователя.
                switch (currentUserType){
                    case ADMIN: rowMenu.getItems().addAll(addItem,separatorMenuItem);
                        break;
                    case USER:  rowMenu.getItems().addAll(dummyItem);
                        break;
                }
            });

            // only display context menu for non-null items:
            row.contextMenuProperty().bind(
                    Bindings.when(Bindings.isNotNull(row.itemProperty()))
                            .then(rowMenu)
                            .otherwise((ContextMenu)null));
            return row;
        });
    }

    // Контекстное меню для списка текущих отметок.
    private void setupTodayVehiclesMarksLogContextMenu(){
        todayVehiclesMarksLog.setRowFactory((Callback<TableView<VehicleMark>, TableRow<VehicleMark>>) tableView -> {
            final TableRow<VehicleMark> row = new TableRow<>();
            final ContextMenu rowMenu = new ContextMenu();
            final SeparatorMenuItem separatorMenuItem = new SeparatorMenuItem();

            //row.styleProperty().set("");
            //row.setStyle("-fx-background-color: RED;");
            //row.setStyle("-fx-color-label-visible: RED;");
            //row.setStyle("-fx-text-fill: red;");
            //row.setTextFill(Color.BLUEVIOLET);


            // Элемент выпадающего меню для администратора - "УДАЛИТЬ ОТМЕТКУ".
            final MenuItem removeItem = new MenuItem("Удалить отметку");
            removeItem.setOnAction(event -> {
                DbProc.clearMark(row.getItem().getRecordId());
            });

            // Элемент выпадающего меню для администратора - "ДОБАВИТЬ ОТМЕТКУ".
            final MenuItem addItem = new MenuItem("Добавить отметку");
            addItem.setOnAction(event -> {
                addMarkManually(row.getItem().getVehicle());
            });

            // Элемент выпадающего меню для обычного пользователя.
            final MenuItem dummyItem = new MenuItem( "dummy");
            dummyItem.setOnAction(event -> {
                /* ToDo: for future */
            });

            rowMenu.getItems().addAll(dummyItem);

            rowMenu.setOnShowing(event -> {
                // Удаляем все элементы и пересоздаем меню с учетом различных факторов и состояний.
                rowMenu.getItems().clear();
                // Естанавливаем имя для элемента "dummy" контекстного меню.
                dummyItem.setText(row.getItem().getTimestamp());
                // Создаем меню с учетом прав текущего пользователя.
                switch (currentUserType){
                    case ADMIN: rowMenu.getItems().addAll(addItem,separatorMenuItem,removeItem);
                        break;
                    case USER:  rowMenu.getItems().addAll(dummyItem);
                        break;
                }
            });

            // only display context menu for non-null items:
            row.contextMenuProperty().bind(
                    Bindings.when(Bindings.isNotNull(row.itemProperty()))
                            .then(rowMenu)
                            .otherwise((ContextMenu)null));
            return row;
        });
    }

    // Контекстное меню для списка статистики.
    private void setupStatisticContextMenu(){

        todayVehiclesStatistic.setRowFactory((Callback<TableView<VehicleItem>, TableRow<VehicleItem>>) tableView -> {
                    final TableRow<VehicleItem> row = new TableRow<>();
                    final ContextMenu rowMenu = new ContextMenu();
                    final SeparatorMenuItem separatorMenuItem = new SeparatorMenuItem();

                    // Элемент выпадающего меню для администратора - "УДАЛИТЬ РЕЙСЫ".
                    final MenuItem removeItem = new MenuItem("Удалить рейсы (текущая дата)");
                    removeItem.setOnAction(event -> {
                        DbProc.clearTodayMarks(row.getItem().getVehicle());

                    });
                    // Элемент выпадающего меню для администратора - "ДОБАВИТЬ ОТМЕТКУ".
                    final MenuItem addItem = new MenuItem("Добавить отметку");
                    addItem.setOnAction(event -> {
                        addMarkManually(row.getItem().getVehicle());
                    });
                    // Элемент выпадающего меню для обычного пользователя.
                    final MenuItem dummyItem = new MenuItem( "dummy");
                        dummyItem.setOnAction(event -> {
                            /* ToDo: for future */
                    });

                    rowMenu.getItems().addAll(dummyItem);

                    rowMenu.setOnShowing(event -> {
                        // Удаляем все элементы и пересоздаем меню с учетом различных факторов и состояний.
                        rowMenu.getItems().clear();
                        // Естанавливаем имя для элемента "dummy" контекстного меню.
                        dummyItem.setText(row.getItem().getVehicle());
                        // Создаем меню с учетом прав текущего пользователя.
                        switch (currentUserType){
                            case ADMIN: rowMenu.getItems().addAll(addItem, separatorMenuItem, removeItem);
                                break;
                            case USER:  rowMenu.getItems().addAll(dummyItem);
                                break;
                        }
                    });

                    // only display context menu for non-null items:
                    row.contextMenuProperty().bind(
                            Bindings.when(Bindings.isNotNull(row.itemProperty()))
                                    .then(rowMenu)
                                    .otherwise((ContextMenu)null));
                    return row;
                });
    }

    private void setupStatusComboBox(TableColumn <VehicleItem, Statuses> column){
        /////////////////////////////////////////////////////////////////////////////
        // Делаем комбо-бокс "Статус" редактируемым и вешаем на него слушателя.
        /////////////////////////////////////////////////////////////////////////////
        ObservableList<Statuses> statusList = FXCollections.observableArrayList(Statuses.values());

        column.setCellValueFactory(param -> {
            VehicleItem vehicle = param.getValue();
            return new SimpleObjectProperty<>( vehicle.isBlocked() ? Statuses.BLOCKED : Statuses.NORMAL );
        });

        column.setCellFactory(ComboBoxTableCell.forTableColumn(statusList));

        column.setOnEditCommit((TableColumn.CellEditEvent<VehicleItem, Statuses> event) -> {
            TablePosition<VehicleItem, Statuses> pos = event.getTablePosition();

            Statuses newStatus  = event.getNewValue();
            VehicleItem vehicle = event.getTableView().getItems().get(pos.getRow());

            // Создаем новый экземпляр для работы с БД.
            Db db = Db.getInstance();
            // Проверяем наличие подключения еще раз.
            if (db.isConnected()){
                // Применяем новый статус к ТС.
                boolean result = db.setVehicleState(vehicle.getVehicle(), newStatus == Statuses.BLOCKED);

                // Если SQL-запрос выполнен с ошибкой - возвращаем предыдущий статус.
                if (!result) {newStatus = event.getOldValue();}
                else
                    Log.println("Госномер "+vehicle.getVehicle()+" был " + ((newStatus==Statuses.BLOCKED)?"заблокирован":"разблокирован")+".");

            } else {
                // Если мы так и не смогли подключиться к БД, то возвращаем предыдущий статус.
                newStatus = event.getOldValue();
            }

            // Применяем новый статус к единице данных.
            vehicle.setBlocked(newStatus == Statuses.BLOCKED);

            // Сообщаем верхнему уровню об изменении набора данных.
            Broadcast.getDatasetInterface().wasChanged();
        });
    }

    @FXML
    public void initialize() {
        refreshClocks();

        initAllGUIs();

        setImageOff();

        setupAccountVisualStyle();

        OnOffImage.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            boolean result = false;

            // Создаем новый экземпляр для работы с БД.
            Db db = Db.getInstance();

            // Если связь с БД не установленна, инициируется попытка установить в ней связь.
            if (!db.isConnected()){
                if (Broadcast.getDbReconectionRequest() != null) Broadcast.getDbReconectionRequest().doReconnect();
                return;
            }

            if (controlButton = !controlButton){
                if (db.setGlobalBlock(false)) {
                    Log.println("Установлено глобальное РАЗРЕШЕНИЕ на выполнение отметок.");
                    setImageOn();
                } else {
                    Log.println("Не удалось осуществить глобальное разрешение на выполнение отметок из-за непредвиденной ошибки.");
                    setImageOff();
                }
            } else {
                if (db.setGlobalBlock(true)) {
                    Log.println("Установлено глобальное ЗАПРЕЩЕНИЕ на выполнение отметок!");
                    setImageOff();
                } else {
                    Log.println("Не удалось осуществить глобальное запрещение на выполнение отметок из-за непредвиденной ошибки.");
                    setImageOn();
                }
            }
        });

        // Создаем слушателя на изменение текущего аккаунта.
        Broadcast.setAccountInterface(newUser -> {
            currentUserType = newUser;
            setupAccountVisualStyle();
        });
    }

    // Установить визуальное оформление согласно текущему пользователю/аккаунту.
    private void setupAccountVisualStyle(){
        switch (currentUserType){
            case USER:
                setUserVisualStyle();
                break;
            case ADMIN:
                setAdminVisualStyle();
                break;
                default:
                    setUserVisualStyle();
                    break;
        }
    }

    // Установить визуальный режим "Пользователь" - верхняя панель управления стандартным серым цветом.
    private void setUserVisualStyle(){
        headerPane.setStyle("-fx-background-color: transparent");
    }

    // Установить визуальный режим "Администратор" - верхняя панель управления красным цветом.
    private void setAdminVisualStyle(){
        headerPane.setStyle("-fx-background-color: orangered");
    }

    // Обновить список фильтра отметок.
    private void updateMarksLogFilter(boolean filtered, String vehicle){
        // Обновляем список фильтрованных ТС:
        // Добавляем ТС в список, если чекбокс отмечен.
        if (filtered) {
            if (!filteredVehicles.contains(vehicle)) filteredVehicles.add(vehicle);
        }
        else // Удаляем из списка в противном случае.
            filteredVehicles.remove(vehicle);

        refreshMarksLogFilter();
    }

    // Применить фильтр отметок к списку отметок.
    private void refreshMarksLogFilter(){
        // Если фильтр не пуст - применяем его, в противном случае - показываем все данные.
        if (filteredVehicles != null){
            if (!filteredVehicles.isEmpty()){
                filteredData.setPredicate(x -> filteredVehicles.contains(x.getVehicle()));
            } else {
                filteredData.setPredicate(x -> true);// разрешаем все данные.
            }
        }
    }

    // Копирует состояние всех чекбоксов "Фильтр" из старого списка в новый.
    private void copyFilterFlagList(ObservableList<VehicleItem> oldList, ObservableList<VehicleItem> newList){
        for (VehicleItem oldInfo:oldList) {
            if (oldInfo.isFiltered()){
                for (VehicleItem newInfo:newList) {
                    if (newInfo.getVehicle().equalsIgnoreCase(oldInfo.getVehicle())) newInfo.setFiltered(true);
                }
            }
        }
    }


    // Заполняется лог отметок.
    void printMarksLog(ObservableList<VehicleMark> list){
        // Заполняем список данными.
        if (todayVehiclesMarksLog != null) {
            filteredData = new FilteredList(list);
            SortedList<VehicleMark> sortableData = new SortedList<>(filteredData);
            todayVehiclesMarksLog.setItems(sortableData);
            sortableData.comparatorProperty().bind(todayVehiclesMarksLog.comparatorProperty());

            // Применить сконфигурированный ранее пользоватем фильтр лога отметок, если он не пуст.
            refreshMarksLogFilter();
        }
    }

    // Заполняется лог статиcтики.
    void printStatisticList(ObservableList<VehicleItem> list){
        // Заполняем список данными.
        if (todayVehiclesStatistic != null) {
            // Копируем все отмеченные ранее пользователем чекбоксы "Фильтр" из предыдущего списка.
            copyFilterFlagList( todayVehiclesStatistic.getItems(), list);
            // Заполняем таблицу данными.
            todayVehiclesStatistic.setItems(list);
        }
    }

    void setImageOffError(){
        OnOffImage.setImage(new Image("images/switch-off-red-48.png"));
        //OnOffImage.setDisable(true);
        OnOffImage.setDisable(false); // В случае ошибки перезапускает соединение с БД.
        controlButton = false;
    }

    void setImageOff(){
        OnOffImage.setImage(new Image("images/switch-off-gray-48.png"));
        OnOffImage.setDisable(false);
        controlButton = false;
    }

    void setImageOn(){
        OnOffImage.setImage(new Image("images/switch-on-green-48.png"));
        OnOffImage.setDisable(false);
        controlButton = true;
    }

    private static boolean oddTick = false;
    void refreshClocks(){

        clockHour.setText(getTimeKK());   // Устанавливаем часы.
        clockMinutes.setText(getTimeMM());// Устанавливаем минуты.

        clockDate.setText(getTimeDDMMYYYY()); // Устанавливаем текущую дату.

        // Манипулируем знаком "двоеточее".
        if (oddTick) clockColon.setStyle("-fx-text-fill: #646464; -fx-font-size: 20.0");
        else
            clockColon.setStyle("-fx-text-fill: #a8a8a8; -fx-font-size: 20.0");

        oddTick = !oddTick;
    }

    void setMarkDelayView(String value){
        markDelay.setText(value);
    }

    public void onSetupAction (ActionEvent event){

        // Настраиваем поле для ввода пароля.
        final PasswordField passwordPasswordField = new PasswordField();
        GridPane grid = new GridPane();
        grid.add(passwordPasswordField, 0, 0);
        grid.setStyle("-fx-alignment: center");
        passwordPasswordField.setFocusTraversable(false);
        passwordPasswordField.requestFocus();

        // Настраиваем диалоговое окно.
        Dialog<String> dialog = new Dialog<>();
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK,  ButtonType.CLOSE);

        // Вешаем слушателя на кнопку ОК.
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setOnAction(event1 -> {
            // Сравниваем хэш-суммы паролей.
            String str = MD5(passwordPasswordField.getText());
            // Если верный пароль, даем доступ к меню настроек.
            if ( str != null )
            if ( str.equalsIgnoreCase("eb0a191797624dd3a48fa681d3061212")){
                Log.println("Запуск инженерного меню ... ");
                /////////////////////////////
                // Запускаем окно настроек //
                /////////////////////////////
                //FXMLLoader loader = new FXMLLoader(getClass().getResource("../frames/settings.fxml"));
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/frames/settings.fxml"));

                Parent root;
                try {
                    root = loader.load();
                    //SettingsController settingsController = loader.getController();
                    setupStage = new Stage();
                    setupStage.getIcons().add(new Image("/images/services-32.png"));
                    setupStage.setTitle("Настройки сервера базы данных");
                    setupStage.setScene(new Scene(root, -1, -1));

                    // Событие при открытии окна.
                    setupStage.setOnShown(event2 -> {
                        Log.println("Инженерное меню -> открыто.");
                        // Изменяет текущий аккаунт на "ADMIN".
                        if (Broadcast.getAccountInterface() != null){
                            Broadcast.getAccountInterface().wasChanged(Users.ADMIN);
                        }
                    });

                    // Событие при закрытии окна.
                    setupStage.setOnCloseRequest(event22 -> {
                        Log.println("Инженерное меню -> закрыто.");
                        // Изменяет текущий аккаунт на "USER".
                        if (Broadcast.getAccountInterface() != null){
                            Broadcast.getAccountInterface().wasChanged(Users.USER);
                        }
                    });

                    setupStage.show();

                } catch (IOException e) {
                    e.printStackTrace();
                    Log.println("ОШИБКА! Подробнее:");
                    Log.println(e.toString());
                }
                /////////////////////////////////////////////////////////////////////////
            } else {
                Log.println("Ошибка ввода: мастер-пароль не принят!");
            }
        });

        // Стилизируем все и объединяем.
        dialog.getDialogPane().setStyle("-fx-min-width: 210; -fx-max-width: 210;");
        dialog.setTitle("Введите пароль доступа");
        Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();// Get the Stage.
        stage.getIcons().add(new Image(this.getClass().getResource("/images/key-32.png").toString()));// Add a custom icon.
        dialog.getDialogPane().setContent(grid);

        dialog.showAndWait();
    }

    void printAllVehicles(ObservableList<VehicleItem> list){
        if (allDbVehiclesList != null){
            // Заполняем таблицу данными.
            allDbVehiclesList.setItems(list);
        }
    }


    private void addMarkManually(String vehicle){

        // Настраиваем диалоговое окно для получения времянной метки новой отметки.
        DateTimeDialog dialog = new DateTimeDialog();
        dialog.setInterface((timestamp, comment) -> DbProc.addMark(vehicle, timestamp, comment));
        dialog.showAndWait();
    }

}
