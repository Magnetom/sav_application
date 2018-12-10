package general;

import bebug.Log;
import bebug.LogInterface;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;
import marks.Statuses;
import marks.VehicleInfo;
import marks.VehicleMark;

public class MainController {
    public TextArea logAreaId;
    public TableView marksLogId;
    public TableView statisticListId;

    public void sayHelloWorld(ActionEvent actionEvent) {
        //helloWorld.setText("Hello world!");
        //if (logAreaId != null) logAreaId.appendText("This is a text\r\n");
    }

    /* Конструктор класса */
    public  MainController(){

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

            vehicleColumn.setMinWidth(75);
            loopsColumn.setMinWidth(75);
            statusColumn.setMinWidth(100);

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

                vehicle.setBlocked(newStatus == Statuses.BLOCKED);

                /* ToDo: вызывать callback и изменять соответствующее поле в БД. */
            });



            /////////////////////////////////////////////////////////////////////////////
            // Делаем чекбокс "Фильтр" редактируемым и вешаем на него слушателя.
            /////////////////////////////////////////////////////////////////////////////
            filterColumn.setCellValueFactory(param -> {

                final VehicleInfo info = param.getValue();
                SimpleBooleanProperty booleanProp = new SimpleBooleanProperty(info.isBlocked());
                // Note: singleCol.setOnEditCommit(): Not work for CheckBoxTableCell.
                // When "Filtered" column change.
                booleanProp.addListener((observable, oldValue, newValue) -> {
                    //info.setBlocked(newValue);
                    /* ToDo: вызывать callback и изменять соответствующее поле в БД. */
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
            // Заполняем список данными.
            statisticListId.setItems(getVehicles());
        }
    }

    @FXML
    public void initialize() {
        clearGUI();
    }

    public void fillMarksLog(ObservableList<VehicleMark> list){
        // Заполняем список данными.
        if (marksLogId != null) marksLogId.setItems(list);
    }


    private ObservableList<VehicleInfo> getVehicles() {

        VehicleInfo info1 = new VehicleInfo("M750AM750", 5,  false, false);
        VehicleInfo info2 = new VehicleInfo("A999VH990", 18, true,  false);
        VehicleInfo info3 = new VehicleInfo("A999VH998", 44, false, false);
        VehicleInfo info4 = new VehicleInfo("B897BA426", 2,  false, false);

        ObservableList<VehicleInfo> list = FXCollections.observableArrayList(info1, info2, info3, info4);
        return list;
    }

}
