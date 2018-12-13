package general;

import bebug.Log;
import bebug.LogInterface;
import db.Db;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import marks.Statuses;
import marks.VehicleInfo;
import marks.VehicleMark;

import java.util.ArrayList;

import static utils.DateTime.*;

public class MainController {
    public TextArea  debugLogArea;
    public TableView markLogArea;
    public TableView vehiclesArea;
    public ImageView OnOffImage;

    // Цифровые часы
    public Label clockHour;
    public Label clockColon;
    public Label clockMinutes;
    public Label clockDate;

    // Содержит список ТС, которые необходимо отображать в логе отметок (список справа).
    private ArrayList<String> filteredVehicles;

    private FilteredList<VehicleMark> filteredData;

    private Boolean OnState;

    /* Конструктор класса */
    public  MainController(){
        OnState = false;

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

    /* Полная очистка GUI интерфейса. */
    public void clearGUI(){
        // Очищается лог событий.
        if (debugLogArea != null) debugLogArea.setText("");

        // Настраивается лог отметок.
        if (markLogArea != null) {

            markLogArea.getColumns().clear();

            TableColumn <VehicleMark, String> timestampColumn = new TableColumn<>("Время");
            TableColumn <VehicleMark, String> vehicleColumn   = new TableColumn<>("Госномер");

            // Defines how to fill data for each cell.
            timestampColumn.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
            vehicleColumn.setCellValueFactory(new PropertyValueFactory<>("vehicle"));

            timestampColumn.setMinWidth(40);
            vehicleColumn.setMinWidth(90);

            timestampColumn.setStyle("-fx-alignment: CENTER;");
            //vehicleColumn.setStyle("-fx-alignment: CENTER;");

            // Set Sort type for userName column
            timestampColumn.setSortType(TableColumn.SortType.DESCENDING);
            //timestampColumn.setSortable(false);
            timestampColumn.setSortable(true);

            // Добавляем новые колонки.
            markLogArea.getColumns().addAll(timestampColumn, vehicleColumn);

            // Заполняем список данными.
            //markLogArea.setItems(getUserList());
        }

        // Настраиваем список подробной статистики по каждому госномеру.
        if (vehiclesArea != null){
            vehiclesArea.getColumns().clear();

            vehiclesArea.setEditable(true);

            TableColumn <VehicleInfo, String>   vehicleColumn  = new TableColumn<>("Госномер");
            TableColumn <VehicleInfo, Integer>  loopsColumn    = new TableColumn<>("Количество кругов");
            TableColumn <VehicleInfo, Statuses> statusColumn   = new TableColumn<>("Статус");
            TableColumn <VehicleInfo, Boolean>  filterColumn   = new TableColumn<>("Фильтр");

            vehicleColumn.setMinWidth(90);
            loopsColumn.setMinWidth(125);
            statusColumn.setMinWidth(100);

            loopsColumn.setStyle("-fx-alignment: CENTER;");
            statusColumn.setStyle("-fx-alignment: CENTER;");

            // Defines how to fill data for each cell.
            vehicleColumn.setCellValueFactory(new PropertyValueFactory<>("vehicle"));
            loopsColumn.setCellValueFactory(new PropertyValueFactory<>("loopsCnt"));

            // Set Sort type for userName column
            loopsColumn.setSortType(TableColumn.SortType.DESCENDING);
            loopsColumn.setSortable(true);

            /////////////////////////////////////////////////////////////////////////////
            // Делаем комбо-бокс "Статус" редактируемым и вешаем на него слушателя.
            /////////////////////////////////////////////////////////////////////////////
            ObservableList<Statuses> statusList = FXCollections.observableArrayList(Statuses.values());

            statusColumn.setCellValueFactory(param -> {
                VehicleInfo vehicle = param.getValue();
                return new SimpleObjectProperty<>( vehicle.isBlocked() ? Statuses.BLOCKED : Statuses.NORMAL );
            });

            statusColumn.setCellFactory(ComboBoxTableCell.forTableColumn(statusList));

            statusColumn.setOnEditCommit((TableColumn.CellEditEvent<VehicleInfo, Statuses> event) -> {
                TablePosition<VehicleInfo, Statuses> pos = event.getTablePosition();

                Statuses newStatus  = event.getNewValue();
                VehicleInfo vehicle = event.getTableView().getItems().get(pos.getRow());

                // Создаем новый экземпляр для работы с БД.
                Db db = Db.getInstance();
                // Подключаемся к БД на случай, если подключение не было выполнено ранее.
                if (!db.isConnected()) db.connect();
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
            });

            /////////////////////////////////////////////////////////////////////////////
            // Делаем чекбокс "Фильтр" редактируемым и вешаем на него слушателя.
            /////////////////////////////////////////////////////////////////////////////
            filterColumn.setCellValueFactory(param -> {

                final VehicleInfo info = param.getValue();
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
                CheckBoxTableCell<VehicleInfo, Boolean> cell = new CheckBoxTableCell<>();
                cell.setAlignment(Pos.CENTER);
                return cell;
            });

            /////////////////////////////////////////////////////////////////////////////
            // Финальные действия.
            /////////////////////////////////////////////////////////////////////////////
            // Добавляем новые колонки.
            vehiclesArea.getColumns().addAll(vehicleColumn, loopsColumn, statusColumn, filterColumn);
        }
    }

    @FXML
    public void initialize() {
        refreshClocks();

        clearGUI();

        setImageOff();

        OnOffImage.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {

            @Override
            public void handle(MouseEvent event) {
                boolean result = false;

                OnState = !OnState;

                // Создаем новый экземпляр для работы с БД.
                Db db = Db.getInstance();
                if (OnState){
                    if (db.isConnected()) result = db.setGlobalBlock(false);
                    if (result) {
                        Log.println("Установлено глобальное РАЗРЕШЕНИЕ на выполнение отметок.");
                        setImageOn();
                    } else {
                        Log.println("Не удалось осуществить глобальное разрешение на выполнение отметок из-за непредвиденной ошибки.");
                        setImageOff();
                    }
                } else {
                    if (db.isConnected()) result = db.setGlobalBlock(true);
                    if (result) {
                        Log.println("Установлено глобальное ЗАПРЕЩЕНИЕ на выполнение отметок!");
                        setImageOff();
                    } else {
                        Log.println("Не удалось осуществить глобальное запрещение на выполнение отметок из-за непредвиденной ошибки.");
                        setImageOn();
                    }
                }
            }
        });

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

    // Применить фильтр отметок с списку отметок.
    private void refreshMarksLogFilter(){
        // Если фильтр не пуст - применяем его, в противном случае - показываем все данные.
        if (!filteredVehicles.isEmpty()){
            if (filteredVehicles != null) filteredData.setPredicate(x -> filteredVehicles.contains(x.getVehicle()));
        } else {
            if (filteredVehicles != null) filteredData.setPredicate(x -> true);// разрешаем все данные.
        }
    }

    // Копирует состояние всех чекбоксов "Фильтр" из старого списка в новый.
    private void copyFilterFlagList(ObservableList<VehicleInfo> oldList, ObservableList<VehicleInfo> newList){
        for (VehicleInfo oldInfo:oldList) {
            if (oldInfo.isFiltered()){
                for (VehicleInfo newInfo:newList) {
                    if (newInfo.getVehicle().equalsIgnoreCase(oldInfo.getVehicle())) newInfo.setFiltered(true);
                }
            }
        }
    }


    // Заполняется лог отметок.
    public void fillMarksLog(ObservableList<VehicleMark> list){
        // Заполняем список данными.
        if (markLogArea != null) {
            filteredData = new FilteredList(list);
            SortedList<VehicleMark> sortableData = new SortedList<>(filteredData);
            markLogArea.setItems(sortableData);
            sortableData.comparatorProperty().bind(markLogArea.comparatorProperty());

            // Применить сконфигурированный ранее пользоватем фильтр лога отметок, если он не пуст.
            refreshMarksLogFilter();
        }
    }

    // Заполняется лог статиcтики.
    public void fillStatisticList(ObservableList<VehicleInfo> list){
        // Заполняем список данными.
        if (vehiclesArea != null) {
            // Копируем все отмеченные ранее пользователем чекбоксы "Фильтр" из предыдущего списка.
            copyFilterFlagList( vehiclesArea.getItems(), list);
            // Заполняем таблицу данными.
            vehiclesArea.setItems(list);
        }
    }

    public void setImageOff(){
        OnOffImage.setImage(new Image("images/switch-off-gray-48.png"));
        OnState = false;
    }

    public void setImageOn(){
        OnOffImage.setImage(new Image("images/switch-on-green-48.png"));
        OnState = true;
    }

    static boolean oddTick = false;
    public void refreshClocks(){

        clockHour.setText(getTimeKK());   // Устанавливаем часы.
        clockMinutes.setText(getTimeMM());// Устанавливаем минуты.

        clockDate.setText(getTimeDDMMYYYY()); // Устанавливаем текущую дату.

        // Манипулируем знаком "двоеточее".
        if (oddTick) clockColon.setStyle("-fx-text-fill: #646464; -fx-font-size: 20.0");
        else
            clockColon.setStyle("-fx-text-fill: #a8a8a8; -fx-font-size: 20.0");

        oddTick = !oddTick;
    }
}
