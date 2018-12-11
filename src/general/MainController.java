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
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import marks.Statuses;
import marks.VehicleInfo;
import marks.VehicleMark;

import java.sql.SQLException;
import java.util.ArrayList;

public class MainController {
    public TextArea logAreaId;
    public TableView marksLogId;
    public TableView statisticListId;

    // Содержит список ТС, которые необходимо отображать в логе отметок (список справа).
    private ArrayList<String> filteredVehicles;

    private FilteredList<VehicleMark> filteredData;

    public void sayHelloWorld(ActionEvent actionEvent) {
        //helloWorld.setText("Hello world!");
        //if (logAreaId != null) logAreaId.appendText("This is a text\r\n");
    }

    /* Конструктор класса */
    public  MainController(){

        filteredVehicles = new ArrayList<>();

        // Инициализируем интерфейс для вывода отладочной информации.
        Log.setInterface(new LogInterface() {
            @Override
            public void println(String mess) {
                if (logAreaId != null && (mess != null) ) logAreaId.appendText(mess+"\r\n");
            }

            @Override
            public void print(String mess) {
                if (logAreaId != null && (mess != null)) logAreaId.appendText(mess);
            }
        });
    }

    /* Полная очистка GUI интерфейса. */
    public void clearGUI(){
        // Очищается лог событий.
        if (logAreaId != null) logAreaId.setText("");

        // Настраивается лог отметок.
        if (marksLogId != null) {

            marksLogId.getColumns().clear();

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
            marksLogId.getColumns().addAll(timestampColumn, vehicleColumn);

            // Заполняем список данными.
            //marksLogId.setItems(getUserList());
        }

        // Настраиваем список подробной статистики по каждому госномеру.
        if (statisticListId != null){
            statisticListId.getColumns().clear();

            statisticListId.setEditable(true);

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
                Db db = new Db();
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
            statisticListId.getColumns().addAll(vehicleColumn, loopsColumn, statusColumn, filterColumn);
        }
    }

    @FXML
    public void initialize() {
        clearGUI();
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
        if (marksLogId != null) {
            filteredData = new FilteredList(list);
            SortedList<VehicleMark> sortableData = new SortedList<>(filteredData);
            marksLogId.setItems(sortableData);
            sortableData.comparatorProperty().bind(marksLogId.comparatorProperty());

            // Применить сконфигурированный ранее пользоватем фильтр лога отметок, если он не пуст.
            refreshMarksLogFilter();
        }
    }

    // Заполняется лог статичтики.
    public void fillStatisticList(ObservableList<VehicleInfo> list){
        // Заполняем список данными.
        if (statisticListId != null) {
            // Копируем все отмеченные ранее пользователем чекбоксы "Фильтр" из предыдущего списка.
            copyFilterFlagList( statisticListId.getItems(), list);
            // Заполняем таблицу данными.
            statisticListId.setItems(list);
        }
    }
}
