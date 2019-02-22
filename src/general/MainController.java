package general;

import bebug.Log;
import bebug.LogInterface;
import broadcast.Broadcast;
import db.Db;
import db.DbDateRange;
import db.DbProc;
import dialogs.datetime.DateTimeDialog;
import enums.Users;
import frames.SettingsController;
import javafx.application.Platform;
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
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Callback;
import marks.Statuses;
import marks.VehicleItem;
import marks.VehicleMark;
import utils.DateTime;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;

import static utils.DateTime.*;
import static utils.Hash.MD5;

public class MainController {

    public TextArea  debugLogArea;
    public TableView todayVehiclesMarksLog;
    public TableView todayVehiclesStatistic;
    public TableView allDbVehiclesList;
    public ImageView OnOffImage;

    // Цифровые часы и выбор дат.
    public Label clockHour;
    public Label clockColon;
    public Label clockMinutes;
    public Label clockDateStart;
    public Label clockDateStop;
    public ImageView resetDateButton;
    public DatePicker datepicker_hidden_start;
    public DatePicker datepicker_hidden_stop;

    // Отображение текущей настройки времянного интервала.
    public Label markDelay;
    public AnchorPane headerPane;

    // Лог событий
    private int unseenCount; // Количество непросмотренных сообщений.
    public TitledPane logTitledPane; // Панель, на которой располагается лог событий.

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
    }

    @FXML
    public void initialize() {
        refreshClocks();

        initAllGUIs();

        setImageOff();

        setupAccountVisualStyle();

        setupDatePickers();

        initListeners();
    }

    public Users getCurrentUser(){
        return currentUserType;
    }

    // Закрывает (если они все-еще открыты) все дочерние окна, поражденные этим контроллером.
    void closeAllChildStages(){
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
            TableColumn <VehicleItem, Statuses> statusColumn   = new TableColumn<>("Статус блокировки");
            TableColumn <VehicleItem, Boolean>  filterColumn   = new TableColumn<>("Фильтр");

            vehicleColumn.setMinWidth(90);
            loopsColumn.setMinWidth(80);
            statusColumn.setMinWidth(125);

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
            todayVehiclesMarksLog.setEditable(true);

            TableColumn <VehicleMark, String> timestampColumn = new TableColumn<>("Дата/Время");
            TableColumn <VehicleMark, String> vehicleColumn   = new TableColumn<>("Госномер");
            TableColumn <VehicleMark, String> commentColumn   = new TableColumn<>("Комментарий");

            // Defines how to fill data for each cell.
            timestampColumn.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
            vehicleColumn.setCellValueFactory(new PropertyValueFactory<>("vehicle"));
            commentColumn.setCellValueFactory(new PropertyValueFactory<>("comment"));

            timestampColumn.setMinWidth(150);
            vehicleColumn.setMinWidth(90);

            timestampColumn.setStyle("-fx-alignment: CENTER;");
            //vehicleColumn.setStyle("-fx-alignment: CENTER;");

            // Set Sort type for userName column
            timestampColumn.setSortType(TableColumn.SortType.DESCENDING);
            timestampColumn.setSortable(true);

            /////////////////////////////////////////////////////////////////////////
            // Настройка ячеек: отметки, помеченные как "удаленные" выделяются красным шрифтом во всех столбцах.
            timestampColumn.setCellFactory(column -> setupTableCellFactoryMARKS());
            vehicleColumn.setCellFactory(column -> setupTableCellFactoryMARKS());
            //commentColumn.setCellFactory(column -> setupTableCellFactoryMARKS());
            ////////////////////////////////////////////////////////////////////////

            // Вешаем слушателя на изменение содержимого ячейки "ТИП".
            commentColumn.setEditable(true);
            commentColumn.setCellFactory(TextFieldTableCell.forTableColumn());
            commentColumn.setOnEditCommit((TableColumn.CellEditEvent<VehicleMark, String> event) -> {
                VehicleMark item = event.getTableView().getItems().get(event.getTablePosition().getRow());
                item.setComment(event.getNewValue());
                //Запись нового значения в БД.
                Db.getInstance().updateMarkComment(String.valueOf(item.getRecordId()), event.getNewValue());
            });


            // Настраиваем контекстное меню.
            setupTodayVehiclesMarksLogContextMenu();

            // Добавляем новые колонки.
            todayVehiclesMarksLog.getColumns().addAll(timestampColumn, vehicleColumn, commentColumn);
        }

        // Настраиваем отображение списока всех госномеров.
        if (allDbVehiclesList != null){
            allDbVehiclesList.getColumns().clear();
            allDbVehiclesList.setEditable(true);

            TableColumn <VehicleItem, Object>   vehicleColumn    = new TableColumn<>("Госномер");
            TableColumn <VehicleItem, Statuses> statusColumn     = new TableColumn<>("Статус блокировки");
            TableColumn <VehicleItem, Object>   popularityColumn = new TableColumn<>("Всего рейсов");

            vehicleColumn.setMinWidth(90);
            statusColumn.setMinWidth(125);
            popularityColumn.setMinWidth(90);

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

            /////////////////////////////////////////////////////////////////////////
            // Настройка ячеек: отметки, помеченные как "удаленные" выделяются красным шрифтом во всех столбцах.
            vehicleColumn.setCellFactory(column -> setupTableCellFactoryVEHICLES());
            popularityColumn.setCellFactory(column -> setupTableCellFactoryVEHICLES());
            /////////////////////////////////////////////////////////////////////////

            // Настройка выпадающего меню.
            setupVehiclesListContextMenu();

            // Добавляем новые колонки.
            allDbVehiclesList.getColumns().addAll(vehicleColumn, statusColumn, popularityColumn);
        }
    }

    // Настройка ячеек для таблицы "СПИСОК ТЕКУЩИХ ОТМЕТОК".
    private TableCell<VehicleMark, String> setupTableCellFactoryMARKS(){
        return new TableCell<VehicleMark, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) setText("");
                else {
                    setText(item);
                    TableRow<VehicleMark> currentRow = getTableRow();
                    if (currentRow != null) {
                        VehicleMark markItem = currentRow.getItem();
                        if (markItem != null) {
                            if (markItem.isVehicleDeleted()) setTextFill(Color.ORANGE);
                            else
                            if (markItem.isDeleted()) setTextFill(Color.RED);
                            else
                                setTextFill(Color.BLACK);
                        }
                    }
                }
            }
        };
    }

    // Настройка ячеек для таблицы "СПИСОК ВСЕХ ТРАНСПОРТНЫХ СРЕДСТВ".
    private TableCell<VehicleItem, Object> setupTableCellFactoryVEHICLES(){
        return new TableCell<VehicleItem, Object>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) setText("");
                else {
                    setText(item.toString());
                    TableRow<VehicleItem> currentRow = getTableRow();
                    if (currentRow != null) {
                        VehicleItem markItem = currentRow.getItem();
                        if (markItem != null) if (markItem.isDeleted()) setTextFill(Color.ORANGE); else setTextFill(Color.BLACK);
                    }
                }
            }
        };
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

            // Элемент выпадающего меню для администратора - "ВОССТАНОВИТЬ ГОСНОМЕР".
            final MenuItem restoreItem = new MenuItem("Восстановить госномер");
            restoreItem.setOnAction(event -> DbProc.restoreVehicle(row.getItem().getVehicle()));

            // Элемент выпадающего меню для администратора - "УДАЛИТЬ ГОСНОМЕР".
            final MenuItem deleteItem = new MenuItem("Удалить госномер");
            deleteItem.setOnAction(event -> DbProc.deleteVehicle(row.getItem().getVehicle()) );

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
                    case ADMIN:
                        if (row.getItem().isDeleted()) rowMenu.getItems().addAll(addItem,separatorMenuItem,restoreItem);
                        else rowMenu.getItems().addAll(addItem,separatorMenuItem,deleteItem);
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

    // Настраивает контекстное меню для таблицы "СПИСОК ТЕКУЩИХ ОТМЕТОК".
    private void setupTodayVehiclesMarksLogContextMenu(){

        todayVehiclesMarksLog.setRowFactory((Callback<TableView<VehicleMark>, TableRow<VehicleMark>>) tableView -> {
            final ContextMenu rowMenu = new ContextMenu();
            final SeparatorMenuItem separatorMenuItem = new SeparatorMenuItem();
            final TableRow<VehicleMark> row = new TableRow<>();

            // Элемент выпадающего меню для администратора - "УДАЛИТЬ ОТМЕТКУ".
            final MenuItem removeItem = new MenuItem("Удалить отметку");
            removeItem.setOnAction(event -> {
                DbProc.clearMark(row.getItem().getRecordId(), row.getItem().getVehicle());
            });

            // Элемент выпадающего меню для администратора - "ВОССТАНОВИТЬ ОТМЕТКУ".
            final MenuItem restoreItem = new MenuItem("Восстановить отметку");
            restoreItem.setOnAction(event -> {
                DbProc.restoreMark(row.getItem().getRecordId() ,row.getItem().getVehicle());
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
                    case ADMIN:
                        if (row.getItem().isDeleted()) rowMenu.getItems().addAll(addItem,separatorMenuItem,restoreItem);
                        else rowMenu.getItems().addAll(addItem,separatorMenuItem,removeItem);
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
                    final SeparatorMenuItem separator1 = new SeparatorMenuItem();
                    final SeparatorMenuItem separator2 = new SeparatorMenuItem();

                    // Элемент выпадающего меню для администратора - "УДАЛИТЬ РЕЙСЫ - ВЫБРАННЫЙ ДИАПАЗОН ДАТ".
                    final MenuItem removeItemDateRange = new MenuItem("Удалить рейсы (диапазон дат)");
                    removeItemDateRange.setOnAction(event -> {
                        DbProc.clearDateRangeMarks(row.getItem().getVehicle(), getUserSelectedDateRange());
                    });

                    // Элемент выпадающего меню для администратора - "УДАЛИТЬ РЕЙСЫ - ТЕКУЩАЯ ДАТА".
                    final MenuItem removeItemCurrDate = new MenuItem("Удалить рейсы (текущая дата)");
                    removeItemCurrDate.setOnAction(event -> {
                        DbProc.clearTodayMarks(row.getItem().getVehicle());
                    });

                    // Элемент выпадающего меню для администратора - "ВОССТАНОВИТЬ РЕЙСЫ - ВЫБРАННЫЙ ДИАПАЗОН ДАТ".
                    final MenuItem restoreDateRangeMarks = new MenuItem("Восстановить рейсы (диапазон дат)");
                    restoreDateRangeMarks.setOnAction(event -> {
                        DbProc.restoreDateRangeMarks(row.getItem().getVehicle(), getUserSelectedDateRange());
                    });

                    // Элемент выпадающего меню для администратора - "ВОССТАНОВИТЬ РЕЙСЫ - ТЕКУЩАЯ ДАТА".
                    final MenuItem restoreItemCurrDate = new MenuItem("Восстановить рейсы (текущая дата)");
                    restoreItemCurrDate.setOnAction(event -> {
                        DbProc.restoreTodayMarks(row.getItem().getVehicle());
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
                            case ADMIN: rowMenu.getItems().addAll(
                                    addItem,
                                    separator1,
                                    restoreItemCurrDate,
                                    restoreDateRangeMarks,
                                    separator2,
                                    removeItemCurrDate,
                                    removeItemDateRange);
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
                boolean result = db.setVehicleBlocked(vehicle.getVehicle(), newStatus == Statuses.BLOCKED);

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

            // Сообщаем верхнему уровню об изменении набора данных или изменении параметров выборки.
            requestAllDatasetReload();
        });
    }

    private void initListeners(){

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
            if (currentUserType != newUser) {
                currentUserType = newUser;
                setupAccountVisualStyle();
                // Обновляем набор данных.
                requestAllDatasetReload();
            }
        });


        // Инициализируем интерфейс для вывода отладочной информации.
        Log.setInterface(new LogInterface() {

            private void updateUnseenCount(){

                if (!logTitledPane.isExpanded()){
                    unseenCount++;
                    Platform.runLater(() -> logTitledPane.setText("Лог событий (новых: "+ unseenCount +")"));
                }
            }

            @Override
            public void println(String mess) {
                if (debugLogArea != null && (mess != null) ) {
                    Platform.runLater(() -> debugLogArea.appendText(mess+"\r\n"));
                    updateUnseenCount();
                }
            }

            @Override
            public void print(String mess) {
                if (debugLogArea != null && (mess != null)) {
                    Platform.runLater(() -> debugLogArea.appendText(mess));
                    updateUnseenCount();
                }
            }
        });
        // Вешаем слушателя на сворачивание/разворачивание лога событий.
        logTitledPane.setOnMouseClicked(event -> {
            if (logTitledPane.isExpanded()){
                unseenCount = 0;
                logTitledPane.setText("Лог событий");
            }
        });
    }

    // Запрос о перезагрузке/актуализации всех визуальных данных.
    private void requestAllDatasetReload(){
        // Сообщаем верхнему уровню об изменении набора данных или изменении параметров выборки.
        if (Broadcast.getDatasetInterface() != null) Broadcast.getDatasetInterface().wasChanged();
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

        //clockDateStart.setText(getTimeDDMMYYYY()); // Устанавливаем текущую дату.

        // Манипулируем знаком "двоеточее".
        if (oddTick) clockColon.setStyle("-fx-text-fill: #646464; -fx-font-size: 20.0");
        else
            clockColon.setStyle("-fx-text-fill: #a8a8a8; -fx-font-size: 20.0");

        oddTick = !oddTick;
    }


    private Image entered = new Image("images/reset-date-brown.png");
    private Image exited  = new Image("images/reset-date-grey.png");

    private void setupDatePickers(){

        // Сбросить все даты.
        resetDatePickers();

        // Настройка видимых датапикеров.
        clockDateStart.setOnMouseClicked(event -> {
            //Проверка взаимопересечения дат начала и конца
            Callback<DatePicker, DateCell> dayCellFactory= this.getStartDayCellFactory();
            datepicker_hidden_start.setDayCellFactory(dayCellFactory);
            // Применение даты.
            datepicker_hidden_start.setOnAction(event1 -> {
                clockDateStart.setText(DateTime.getVisualDateConverter().toString(datepicker_hidden_start.getValue()));
                requestAllDatasetReload();
            });
            datepicker_hidden_start.show();
        });
        clockDateStop.setOnMouseClicked(event -> {
            // Проверка взаимопересечения дат начала и конца
            Callback<DatePicker, DateCell> dayCellFactory= this.getStopDayCellFactory();
            datepicker_hidden_stop.setDayCellFactory(dayCellFactory);
            // Применение даты.
           datepicker_hidden_stop.setOnAction(event1 -> {
                clockDateStop.setText(DateTime.getVisualDateConverter().toString(datepicker_hidden_stop.getValue()));
                requestAllDatasetReload();
            });
            datepicker_hidden_stop.show();
        });

        // Кнопка "Сбросить даты и установить везде текущую".
        resetDateButton.setOnMouseEntered(event -> resetDateButton.setImage(entered));// Картинка при наведении курсора.
        resetDateButton.setOnMouseExited (event -> resetDateButton.setImage(exited)); // Картинка при уходе курсора.
        // Слушатель нажатия кнопки
        resetDateButton.setOnMouseClicked(event -> {
            /* Сбросить даты */
            resetDatePickers();
        });
    }

    // Сбросить все даты на текущую.
    private void resetDatePickers() {
        // Устанавливаем текущую дату для видимых датапикеров (Label).
        String reset_value_1 = getTimeDDMMYYYY();
        clockDateStart.setText(reset_value_1);
        clockDateStop.setText(reset_value_1);

        // Устанавливаем текущую дату для скрытых датапикеров (DatePicker).
        LocalDate reset_value_2 = DateTime.getDbDateConverter().fromString(getTimeYYYYMMDD());
        datepicker_hidden_start.setValue(reset_value_2);
        datepicker_hidden_stop.setValue(reset_value_2);
    }

    // Factory to create Cell of DatePicker
    private Callback<DatePicker, DateCell> getStartDayCellFactory() {
        final Callback<DatePicker, DateCell> dayCellFactory = new Callback<DatePicker, DateCell>() {
            @Override
            public DateCell call(final DatePicker datePicker) {
                return new DateCell() {
                    @Override
                    public void updateItem(LocalDate item, boolean empty) {
                        super.updateItem(item, empty);
                        if ( item.compareTo(datepicker_hidden_stop.getValue()) > 0 ){
                            setDisable(true);
                            setStyle("-fx-background-color: #ffc0cb;");
                        }
                    }
                };
            }
        };
        return dayCellFactory;
    }

    // Factory to create Cell of DatePicker
    private Callback<DatePicker, DateCell> getStopDayCellFactory() {
        final Callback<DatePicker, DateCell> dayCellFactory = new Callback<DatePicker, DateCell>() {
            @Override
            public DateCell call(final DatePicker datePicker) {
                return new DateCell() {
                    @Override
                    public void updateItem(LocalDate item, boolean empty) {
                        super.updateItem(item, empty);
                        if ( item.compareTo(datepicker_hidden_start.getValue()) < 0 ){
                            setDisable(true);
                            setStyle("-fx-background-color: #ffc0cb;");
                        }
                    }
                };
            }
        };
        return dayCellFactory;
    }

    DbDateRange getUserSelectedDateRange(){
        DbDateRange dbDateRange = new DbDateRange();
        dbDateRange.setStartDate(datepicker_hidden_start.getValue());
        dbDateRange.setStopDate(datepicker_hidden_stop.getValue());
        return dbDateRange;
    }

    void setMarkDelayView(String value){
        markDelay.setText(value);
    }

    // Реакция на нажатие кнопки "СТАРТ ИНЖЕНЕРНОГО МЕНЮ"
    public void onMasterSetupRequest(ActionEvent event){

        // Если в настоящий момент текущий пользователь Администратор, то просто запускаем инженерное меню
        // без вызова диалогового окна для ввода пароля доступа.
        if (currentUserType == Users.ADMIN){
            // Если этот запрос был вызван не нажатием на кнопку "Инженерное меню", то возвращаем пользователя User.
            if (event == null) {
                changeCurrentUser(Users.USER);
                return;
            }
            // Запускаем окно настроек
            showMasterSetupWindow();
            return;
        }

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
                /////////////////////////////////////////////////////////////////////////
                // Если была нажата "Инженерное меню" -> запускаем окно настроек.
                if (event != null) {
                    showMasterSetupWindow();
                }// В противном случае просто активируем пользователя Admin.
                else {
                    changeCurrentUser(Users.ADMIN);
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

    private void showMasterSetupWindow(){

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
                changeCurrentUser(Users.ADMIN);
            });
            // Событие при исчезновении окна. Наступает после закрытия.
            setupStage.setOnHidden(event23 -> {
                Log.println("Инженерное меню -> закрыто.");
                // Изменяет текущий аккаунт на "USER".
                changeCurrentUser(Users.USER);
            });
            setupStage.show();

        } catch (IOException e) {
            e.printStackTrace();
            Log.println("ОШИБКА! Подробнее:");
            Log.println(e.toString());
        }
    }

    private void changeCurrentUser(Users user){
        if (currentUserType != user) {
            if (Broadcast.getAccountInterface() != null) {
                Broadcast.getAccountInterface().wasChanged(user);
            }
        }
    }

    public void onUserSetupAction (ActionEvent event) {

        //////////////////////////////////////////////
        // Запускаем окно пользовательских настроек //
        //////////////////////////////////////////////

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/frames/user_settings.fxml"));

        Parent root;
        try {
            root = loader.load();
            setupStage = new Stage();
            setupStage.getIcons().add(new Image("/images/user-settings-32.png"));
            setupStage.setTitle("Пользовательские настройки");
            setupStage.setScene(new Scene(root, -1, -1));

            // Событие при открытии окна.
            setupStage.setOnShown(event2 -> {
                /* */
            });
            // Событие при исчезновении окна. Наступает после закрытия.
            setupStage.setOnHidden(event23 -> {
                /* */
            });
            setupStage.show();
            
        } catch (IOException e) {
            e.printStackTrace();
            Log.println("ОШИБКА! Подробнее:");
            Log.println(e.toString());
        }
        /////////////////////////////////////////////////////////////////////////
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
