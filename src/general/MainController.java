package general;

import bebug.Log;
import bebug.LogInterface;
import broadcast.Broadcast;
import db.Db;
import db.DbProc;
import db.DbTimestampRange;
import dialogs.newmark.addNewMarkDialog;
import dialogs.time.TimeDialog;
import enums.Users;
import frames.SettingsController;
import items.*;
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
import javafx.scene.Node;
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
import javafx.scene.text.*;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Callback;
import utils.time.DateTime;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static utils.Hash.MD5;
import static utils.time.DateTime.*;

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
    public Label clockTimeStart;
    public Label clockTimeStop;
    public ImageView resetDateButton;
    public DatePicker datepicker_hidden_start;
    public DatePicker datepicker_hidden_stop;

    // Отображение текущей настройки времянного интервала.
    public Label markDelay;
    public AnchorPane headerPane;
    public TextFlow statisticInTotalTextFlow;

    // Лог событий
    private int unseenCount; // Количество непросмотренных сообщений.
    public TitledPane logTitledPane; // Панель, на которой располагается лог событий.

    // Содержит список ТС, которые необходимо отображать в логе отметок (список справа).
    private ArrayList<String> filteredVehicles;

    private FilteredList<VehicleMarkItem> filteredData;

    private boolean controlButton;

    private Users currentUserType;

    private ContextMenu contextMenu;

    private Stage setupStage;

    private static SettingsController settingsController;

    TableColumn <VehicleItem, VehicleCapacityItem>  capacityColumn;

    // Текст, который будет отображен в случае пустого списка.
    private static final String emptyText = "Нет данных для отображения.";

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

        setupTimePickers();

        initListeners();
    }

    private Stage getStage(){
        if (this.debugLogArea != null) {
            if (this.debugLogArea.getScene() != null){
                return (Stage)this.debugLogArea.getScene().getWindow();
            }
        }
        return null;
    }

    public Users getCurrentUser(){
        return currentUserType;
    }

    // Закрывает (если они все-еще открыты) все дочерние окна, поражденные этим контроллером.
    void closeAllChildStages(){
        // Закрываем инженерное меню, если оно все-еще открыто.
        if (setupStage!=null)setupStage.close();
    }

    private Node getEmptyNode(){
        Label emptyLabel = new Label(emptyText);
        emptyLabel.setStyle("-fx-text-alignment: center");
        return emptyLabel;
    }

    /* Полная очистка GUI интерфейса и реинициализация. */
    private void initAllGUIs(){

        // Очищается лог событий.
        if (debugLogArea != null) debugLogArea.setText("");


        /////////////////////////////////////////////////////////////////////////////////
        // Настраиваем список подробной СТАТИСТИКИ ПО КАЖДОМУ ГОСНОМЕРУ.
        /////////////////////////////////////////////////////////////////////////////////
        if (todayVehiclesStatistic != null){
            todayVehiclesStatistic.getColumns().clear();

            todayVehiclesStatistic.setEditable(true);
            todayVehiclesStatistic.setPlaceholder(getEmptyNode());

            TableColumn <VehicleStatisticItem, String>   vehicleColumn  = new TableColumn<>("Госномер");
            TableColumn <VehicleStatisticItem, Integer>  loopsColumn    = new TableColumn<>("Рейсов");
            TableColumn <Object, Statuses>               statusColumn   = new TableColumn<>("Статус блокировки");
            TableColumn <VehicleStatisticItem, Boolean>  filterColumn   = new TableColumn<>("Фильтр");
            TableColumn <VehicleStatisticItem, Integer>  volumeColumn   = new TableColumn<>("Объем перевезен, м.куб.");
            TableColumn <VehicleStatisticItem, Integer>  costColumn     = new TableColumn<>("Стоимость итого, руб.");

            vehicleColumn.setMinWidth(90);
            loopsColumn.setMinWidth(80);
            statusColumn.setMinWidth(125);
            volumeColumn.setMinWidth(165);
            costColumn.setMinWidth(150);

            String textAlignStyle = "-fx-alignment: CENTER;";
            loopsColumn.setStyle(textAlignStyle);
            statusColumn.setStyle(textAlignStyle);
            volumeColumn.setStyle(textAlignStyle);
            costColumn.setStyle(textAlignStyle);

            // Defines how to fill data for each cell.
            vehicleColumn.setCellValueFactory(new PropertyValueFactory<>("vehicle"));
            loopsColumn.setCellValueFactory(new PropertyValueFactory<>("loopsCnt"));
            volumeColumn.setCellValueFactory(new PropertyValueFactory<>("totalVolume"));
            costColumn.setCellValueFactory(new PropertyValueFactory<>("totalCost"));

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
            // Определяем, как получаются значения true/false для чекбоксов.
            filterColumn.setCellValueFactory(param -> {

                final VehicleStatisticItem info = param.getValue();

                SimpleBooleanProperty booleanProp = new SimpleBooleanProperty(info.isFiltered());
                // Note: singleCol.setOnEditCommit(): Not work for CheckBoxTableCell.
                // When "Filtered" column change.
                booleanProp.addListener((observable, oldValue, newValue) -> {
                    updateMarksLogFilter(newValue, info.getVehicle());
                    info.setFiltered(newValue);
                });
                return booleanProp;
            });

            // Установка чекбоксов в качестве элементов ячеек этого столбца.
            filterColumn.setCellFactory(p -> {
                CheckBoxTableCell<VehicleStatisticItem, Boolean> cell = new CheckBoxTableCell<>();
                cell.setAlignment(Pos.CENTER);
                return cell;
            });

            // Настраиваем контекстное меню.
            setupStatisticContextMenu();

            // Добавляем новые колонки.
            todayVehiclesStatistic.getColumns().addAll(vehicleColumn, loopsColumn, statusColumn, filterColumn,volumeColumn, costColumn);
        }

        /////////////////////////////////////////////////////////////////////////////////
        // Настраивается ЛОГ ОТМЕТОК.
        /////////////////////////////////////////////////////////////////////////////////
        if (todayVehiclesMarksLog != null) {

            todayVehiclesMarksLog.getColumns().clear();
            todayVehiclesMarksLog.setEditable(true);

            todayVehiclesMarksLog.setPlaceholder(getEmptyNode());

            TableColumn <VehicleMarkItem, String> timestampColumn = new TableColumn<>("Дата/Время");
            TableColumn <VehicleMarkItem, String> vehicleColumn   = new TableColumn<>("Госномер");
            TableColumn <VehicleMarkItem, String> deviceColumn    = new TableColumn<>("Устройство");
            TableColumn <VehicleMarkItem, String> commentColumn   = new TableColumn<>("Комментарий");

            // Defines how to fill data for each cell.
            timestampColumn.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
            vehicleColumn.setCellValueFactory(new PropertyValueFactory<>("vehicle"));
            commentColumn.setCellValueFactory(new PropertyValueFactory<>("comment"));
            deviceColumn.setCellValueFactory(new PropertyValueFactory<>("device"));

            timestampColumn.setMinWidth(150);
            vehicleColumn.setMinWidth(90);
            commentColumn.setMinWidth(120);
            deviceColumn.setMinWidth(120);

            timestampColumn.setStyle("-fx-alignment: CENTER;");
            deviceColumn.setStyle("-fx-alignment: CENTER;");

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
            commentColumn.setOnEditCommit((TableColumn.CellEditEvent<VehicleMarkItem, String> event) -> {
                VehicleMarkItem item = event.getTableView().getItems().get(event.getTablePosition().getRow());
                item.setComment(event.getNewValue());
                //Запись нового значения в БД.
                Db.getInstance().updateMarkComment(String.valueOf(item.getRecordId()), event.getNewValue());
            });


            // Настраиваем контекстное меню.
            setupTodayVehiclesMarksLogContextMenu();

            // Добавляем новые колонки.
            todayVehiclesMarksLog.getColumns().addAll(timestampColumn, vehicleColumn, deviceColumn, commentColumn);
        }

        /////////////////////////////////////////////////////////////////////////////////
        // Настраиваем отображение СПИСОКА ВСЕХ ГОСНОМЕРОВ.
        /////////////////////////////////////////////////////////////////////////////////
        if (allDbVehiclesList != null){
            allDbVehiclesList.getColumns().clear();
            allDbVehiclesList.setEditable(true);
            allDbVehiclesList.setPlaceholder(getEmptyNode());

            TableColumn <VehicleItem, Object>  vehicleColumn    = new TableColumn<>("Госномер");
            TableColumn <Object, Statuses>     statusColumn     = new TableColumn<>("Статус блокировки");
            TableColumn <VehicleItem, Object>  popularityColumn = new TableColumn<>("Всего рейсов");
                                               capacityColumn   = new TableColumn<>("Тип");

            vehicleColumn.setMinWidth(90);
            statusColumn.setMinWidth(125);
            popularityColumn.setMinWidth(90);
            capacityColumn.setMinWidth(90);

            popularityColumn.setStyle("-fx-alignment: CENTER;");
            statusColumn.setStyle("-fx-alignment: CENTER;");
            capacityColumn.setStyle("-fx-alignment: CENTER;");

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

            /////////////////////////////////////////////////////////////////////////////
            // Делаем комбо-бокс "Грузовместимость" редактируемым и вешаем на него слушателя.
            /////////////////////////////////////////////////////////////////////////////
            //setupCapacityComboBox(capacityColumn);

            /////////////////////////////////////////////////////////////////////////
            // Настройка ячеек: отметки, помеченные как "удаленные" выделяются красным шрифтом во всех столбцах.
            vehicleColumn.setCellFactory(column -> setupTableCellFactoryVEHICLES());
            popularityColumn.setCellFactory(column -> setupTableCellFactoryVEHICLES());
            /////////////////////////////////////////////////////////////////////////

            // Настройка выпадающего меню.
            setupVehiclesListContextMenu();

            // Добавляем новые колонки.
            //allDbVehiclesList.getColumns().addAll(vehicleColumn, statusColumn, popularityColumn, capacityColumn);
            allDbVehiclesList.getColumns().addAll(vehicleColumn, statusColumn, capacityColumn);
        }
    }

    // Настройка ячеек для таблицы "СПИСОК ТЕКУЩИХ ОТМЕТОК".
    private TableCell<VehicleMarkItem, String> setupTableCellFactoryMARKS(){
        return new TableCell<VehicleMarkItem, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) setText("");
                else {
                    setText(item);
                    TableRow<VehicleMarkItem> currentRow = getTableRow();
                    if (currentRow != null) {
                        VehicleMarkItem markItem = currentRow.getItem();
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

        todayVehiclesMarksLog.setRowFactory((Callback<TableView<VehicleMarkItem>, TableRow<VehicleMarkItem>>) tableView -> {
            final ContextMenu rowMenu = new ContextMenu();
            final SeparatorMenuItem separatorMenuItem = new SeparatorMenuItem();
            final TableRow<VehicleMarkItem> row = new TableRow<>();

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
                        DbProc.clearDateRangeMarks(row.getItem().getVehicle(), getUserSelectedTimestampRange());
                    });

                    // Элемент выпадающего меню для администратора - "УДАЛИТЬ РЕЙСЫ - ТЕКУЩАЯ ДАТА".
                    final MenuItem removeItemCurrDate = new MenuItem("Удалить рейсы (текущая дата)");
                    removeItemCurrDate.setOnAction(event -> {
                        DbProc.clearTodayMarks(row.getItem().getVehicle());
                    });

                    // Элемент выпадающего меню для администратора - "ВОССТАНОВИТЬ РЕЙСЫ - ВЫБРАННЫЙ ДИАПАЗОН ДАТ".
                    final MenuItem restoreDateRangeMarks = new MenuItem("Восстановить рейсы (диапазон дат)");
                    restoreDateRangeMarks.setOnAction(event -> {
                        DbProc.restoreDateRangeMarks(row.getItem().getVehicle(), getUserSelectedTimestampRange());
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


    /////////////////////////////////////////////////////////////////////////////
    // Делаем комбо-бокс "Грузовместимость" редактируемым и вешаем на него слушателя.
    /////////////////////////////////////////////////////////////////////////////
    private void setupCapacityComboBox( TableColumn <VehicleItem, VehicleCapacityItem>  column){
        if (column == null) return;

        List<VehicleCapacityItem> capList = Db.getInstance().getCapacity(null);
        if (capList == null) capList = new ArrayList<>();
        // Получаем из БД полный актуальный список грузовместимостей.
        ObservableList<VehicleCapacityItem> capacityItemsList = FXCollections.observableArrayList(capList);

        column.setCellValueFactory(param -> {
            VehicleItem vehicle = param.getValue();
            VehicleCapacityItem capacity = DbProc.getCapacity(capacityItemsList, vehicle);
            return new SimpleObjectProperty<>(capacity);
        });

        column.setCellFactory(ComboBoxTableCell.forTableColumn(capacityItemsList));

        column.setOnEditCommit((TableColumn.CellEditEvent<VehicleItem, VehicleCapacityItem> event) -> {
            TablePosition<VehicleItem, VehicleCapacityItem> pos = event.getTablePosition();

            VehicleCapacityItem newCapItem  = event.getNewValue();
            VehicleItem vehicle = event.getTableView().getItems().get(pos.getRow());

            // Сохраняем новое значение грузовместимости в БД.
            DbProc.updateVehicleCapacity(vehicle, newCapItem);

            // Сообщаем верхнему уровню об изменении набора данных или изменении параметров выборки.
            requestAllDatasetReload();
        });
    }


    /////////////////////////////////////////////////////////////////////////////
    // Делаем комбо-бокс "Статус" редактируемым и вешаем на него слушателя.
    /////////////////////////////////////////////////////////////////////////////
    /* ToDo: адаптировать для всех типов VehicleItem и VehicleStatisticItem. */
    private void setupStatusComboBox(TableColumn <Object, Statuses> column){

        ObservableList<Statuses> statusList = FXCollections.observableArrayList(Statuses.values());

        column.setCellValueFactory(param -> {

            boolean blockState = false;

            Object vehicleObject = param.getValue();

            if (vehicleObject instanceof VehicleItem)           blockState = ((VehicleItem)vehicleObject).isBlocked();
            if (vehicleObject instanceof VehicleStatisticItem)  blockState = ((VehicleStatisticItem)vehicleObject).isBlocked();

            return new SimpleObjectProperty<>( blockState ? Statuses.BLOCKED : Statuses.NORMAL );
        });

        //column.setCellFactory(ComboBoxTableCell.forTableColumn(statusList));
        //////////////////////////////////////////////////////////////////////////////////////////////////////
        column.setCellFactory(p -> new ComboBoxTableCell<Object, Statuses>(statusList) {
            @Override
            public void updateItem(Statuses item, boolean empty) {
                super.updateItem(item, empty);

                if (!empty) {
                    //VehicleStatisticItem statisticItem = (VehicleStatisticItem) getTableRow().getItem();
                    
                    // В ячейке с именем "Итого" скрываем чекбокс.
                    //if (statisticItem != null && statisticItem.getVehicle().equalsIgnoreCase(MARKER_IN_TOTAL)) {
                    /*
                    if (getTableRow().getId().equals(MARKER_IN_TOTAL)) {
                        //setGraphic(null);
                        //setText(null);
                        setEditable(false);
                    }
                    */
                }
            }
        });
        ////////////////////////////////////////////////////////////////////////////////////////////////////////


        column.setOnEditCommit((TableColumn.CellEditEvent<Object, Statuses> event) -> {
            TablePosition<Object, Statuses> pos = event.getTablePosition();

            Statuses newStatus  = event.getNewValue();
            Object vehicleObject = event.getTableView().getItems().get(pos.getRow());

            String vehicle = "";
            if (vehicleObject instanceof VehicleItem)           vehicle = ((VehicleItem)vehicleObject).getVehicle();
            if (vehicleObject instanceof VehicleStatisticItem)  vehicle = ((VehicleStatisticItem)vehicleObject).getVehicle();

            // Создаем новый экземпляр для работы с БД.
            Db db = Db.getInstance();
            // Проверяем наличие подключения еще раз.
            if (db.isConnected()){
                // Применяем новый статус к ТС.
                boolean result = db.setVehicleBlocked(vehicle, newStatus == Statuses.BLOCKED);

                // Если SQL-запрос выполнен с ошибкой - возвращаем предыдущий статус.
                if (!result) {newStatus = event.getOldValue();}
                else
                    Log.println("Госномер "+vehicle+" был " + ((newStatus==Statuses.BLOCKED)?"заблокирован":"разблокирован")+".");

            } else {
                // Если мы так и не смогли подключиться к БД, то возвращаем предыдущий статус.
                newStatus = event.getOldValue();
            }

            // Применяем новый статус к единице данных.
            if (vehicleObject instanceof VehicleItem)           ((VehicleItem)vehicleObject).setBlocked(newStatus == Statuses.BLOCKED);
            if (vehicleObject instanceof VehicleStatisticItem)  ((VehicleStatisticItem)vehicleObject).setBlocked(newStatus == Statuses.BLOCKED);

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
    private void copyFilterFlagList(ObservableList<VehicleStatisticItem> oldList, ObservableList<VehicleStatisticItem> newList){
        for (VehicleStatisticItem oldInfo:oldList) {
            if (oldInfo.isFiltered()){
                for (VehicleStatisticItem newInfo:newList) {
                    if (newInfo.getVehicle().equalsIgnoreCase(oldInfo.getVehicle())) newInfo.setFiltered(true);
                }
            }
        }
    }


    // Заполняется лог отметок.
    void printMarksLog(ObservableList<VehicleMarkItem> list){
        // Заполняем список данными.
        if (todayVehiclesMarksLog != null) {
            filteredData = new FilteredList(list);
            SortedList<VehicleMarkItem> sortableData = new SortedList<>(filteredData);
            todayVehiclesMarksLog.setItems(sortableData);
            sortableData.comparatorProperty().bind(todayVehiclesMarksLog.comparatorProperty());

            // Применить сконфигурированный ранее пользоватем фильтр лога отметок, если он не пуст.
            refreshMarksLogFilter();
        }
    }

    // Заполняется лог статиcтики.
    void printStatisticList(ObservableList<VehicleStatisticItem> list){
        // Заполняем список данными.
        if (todayVehiclesStatistic != null) {
            // Копируем все отмеченные ранее пользователем чекбоксы "Фильтр" из предыдущего списка.
            copyFilterFlagList( todayVehiclesStatistic.getItems(), list);
            // Заполняем таблицу данными.
            todayVehiclesStatistic.setItems(list);

            // Заполняем поле "Итого".
            updateInTotal(getInTotal(list));
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

    private void setupTimePickers() {

        clockTimeStart.setText("00:01");
        clockTimeStop.setText("23:59");

        setupTimePicker(clockTimeStart);
        setupTimePicker(clockTimeStop);
    }

    private void setupTimePicker(Node node){

        node.setOnMouseClicked(event -> {

            // Берем любой графический элемент и через него выходим на окно.
            Window window = node.getParent().getScene().getWindow();

            // Запускаем диалоговое окно выбора времени.
            TimeDialog timeDialog = new TimeDialog(window, node, ((Label)node).getText());

            // Назначаем слушателя на изменение значения времени.
            timeDialog.setInterface(timestamp -> {
                ((Label)node).setText(timestamp);
                requestAllDatasetReload();
            });
            timeDialog.showAndWait();
        });
    }

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

        clockTimeStart.setText("00:01");
        clockTimeStop.setText("23:59");

        // Устанавливаем текущую дату для скрытых датапикеров (DatePicker).
        LocalDate reset_value_2 = DateTime.getDbDateConverter().fromString(getTimeYYYYMMDD());
        datepicker_hidden_start.setValue(reset_value_2);
        datepicker_hidden_stop.setValue(reset_value_2);

        requestAllDatasetReload();
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

    DbTimestampRange getUserSelectedTimestampRange(){

        DbTimestampRange dbDateRange = new DbTimestampRange();

        String start = datepicker_hidden_start.getValue().toString() + " " + clockTimeStart.getText()+":00";
        String stop  = datepicker_hidden_stop.getValue().toString() + " " + clockTimeStop.getText()+":00";

        dbDateRange.setStartTimestamp(start);
        dbDateRange.setStopTimestamp(stop);

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
        passwordPasswordField.setFocusTraversable(true);
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
        dialog.initOwner(getStage());

        ///////////////////////////////////////////////////////
        // Попытка установить фокус на текстовое поле ввода пароля.
        // Комментарий:
        // Platform.runLater will run at the end, after the main method start(),
        // which ensures the call of requestFocus will be after scene graph construction.
        Platform.runLater(passwordPasswordField::requestFocus);
        ///////////////////////////////////////////////////////

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
            setupStage.initOwner(getStage());

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

            setupCapacityComboBox(capacityColumn);

            // Заполняем таблицу данными.
            allDbVehiclesList.setItems(list);
        }
    }


    private void addMarkManually(String vehicle){

        // Настраиваем диалоговое окно для получения времянной метки новой отметки.
        addNewMarkDialog dialog = new addNewMarkDialog();
        dialog.setInterface((timestamp, comment) -> DbProc.addMark(vehicle, timestamp, comment));
        dialog.showAndWait();
    }

    private void updateInTotal(InTotal inTotal){

        Integer loops   = 0;
        Integer volume  = 0;
        Integer cost    = 0;

        if (inTotal != null){
            loops   = inTotal.getLoops();
            volume  = inTotal.getVolume();
            cost    = inTotal.getCost();
        }

        addInTotalHeader("Итого");
        addInTotalParamValue("кругов", loops.toString(), "шт.",true);
        addInTotalParamValue("объем", volume.toString(), "м.куб",true);
        addInTotalParamValue("стоимость", cost.toString(), "руб.",false);

    }

    private InTotal getInTotal (ObservableList<VehicleStatisticItem> list){

        if (list == null || list.isEmpty()) return null;

        InTotal inTotal = new InTotal();

        int loops   = 0;
        int volume  = 0;
        int cost    = 0;

        for (VehicleStatisticItem item: list) {
            loops   += item.getLoopsCnt();
            volume  += item.getTotalVolume();
            cost    += item.getTotalCost();
        }

        inTotal.setLoops(loops);
        inTotal.setVolume(volume);
        inTotal.setCost(cost);

        return inTotal;
    }

    private void addInTotalHeader(String text){

        statisticInTotalTextFlow.getChildren().clear();
        // Setting the line spacing between the text objects
        statisticInTotalTextFlow.setTextAlignment(TextAlignment.JUSTIFY);
        // Setting the line spacing
        statisticInTotalTextFlow.setLineSpacing(5.0);

        // Retrieving the observable list of the TextFlow Pane
        ObservableList list = statisticInTotalTextFlow.getChildren();

        Font  headerFont  = Font.font("Helvetica", FontWeight.NORMAL, 12);
        Color headerColor = Color.GRAY;

        Text headerText = new Text(text+": ");
        headerText.setFont(headerFont);
        headerText.setFill(headerColor);
        list.addAll(headerText);
    }

    private void addInTotalParamValue(String param, String value, String units, boolean with_delimiter){

        Font  paramFont  = Font.font("Helvetica", FontWeight.BOLD, 12);
        Color paramColor = Color.GRAY;

        Font  valueFont  = Font.font("Helvetica", FontWeight.NORMAL, 12);
        Color valueColor = Color.BROWN;

        Font  unitsFont  = Font.font("Helvetica", FontWeight.NORMAL, 12);
        Color unitsColor = Color.GRAY;

        Font  delimiterFont  = Font.font("Helvetica", FontWeight.NORMAL, 12);
        Color delimiterColor = Color.LIGHTGRAY;

        Text paramText = new Text(param+": ");
        paramText.setFont(paramFont);
        paramText.setFill(paramColor);

        Text valueText = new Text(value);
        valueText.setFont(valueFont);
        valueText.setFill(valueColor);

        Text unitsText = new Text((units!=null)?" "+units:"");
        unitsText.setFont(unitsFont);
        unitsText.setFill(unitsColor);

        Text delimiter = new Text(with_delimiter?" | ":"");
        delimiter.setFont(delimiterFont);
        delimiter.setFill(delimiterColor);

        // Retrieving the observable list of the TextFlow Pane
        ObservableList list = statisticInTotalTextFlow.getChildren();
        // Adding cylinder to the pane
        list.addAll(paramText, valueText, unitsText, delimiter);
    }

}
