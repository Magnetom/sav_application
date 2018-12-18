package general;

import bebug.Log;
import bebug.LogInterface;
import broadcast.Broadcast;
import db.Db;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import marks.Statuses;
import marks.VehicleItem;
import marks.VehicleMark;

import java.io.IOException;
import java.util.ArrayList;

import static utils.DateTime.*;
import static utils.hash.*;

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

    /* Полная очистка GUI интерфейса и реинициализация. */
    public void initAllGUIs(){

        // Очищается лог событий.
        if (debugLogArea != null) debugLogArea.setText("");

        // Настраивается лог отметок.
        if (todayVehiclesMarksLog != null) {

            todayVehiclesMarksLog.getColumns().clear();

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
            timestampColumn.setSortable(true);

            // Добавляем новые колонки.
            todayVehiclesMarksLog.getColumns().addAll(timestampColumn, vehicleColumn);
        }

        // Настраиваем список подробной статистики по каждому госномеру.
        if (todayVehiclesStatistic != null){
            todayVehiclesStatistic.getColumns().clear();

            todayVehiclesStatistic.setEditable(true);

            TableColumn <VehicleItem, String>   vehicleColumn  = new TableColumn<>("Госномер");
            TableColumn <VehicleItem, Integer>  loopsColumn    = new TableColumn<>("Кругов");
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

            // Добавляем новые колонки.
            todayVehiclesStatistic.getColumns().addAll(vehicleColumn, loopsColumn, statusColumn, filterColumn);
        }

        // Настраиваем отображение списока всех госномеров.
        if (allDbVehiclesList != null){
            allDbVehiclesList.getColumns().clear();
            allDbVehiclesList.setEditable(true);

            TableColumn <VehicleItem, String>   vehicleColumn    = new TableColumn<>("Госномер");
            TableColumn <VehicleItem, Integer>  popularityColumn = new TableColumn<>("Рейтинг");
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


            // Добавляем новые колонки.
            allDbVehiclesList.getColumns().addAll(vehicleColumn, statusColumn, popularityColumn);
        }
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

        OnOffImage.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
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
    public void printMarksLog(ObservableList<VehicleMark> list){
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
    public void printStatisticList(ObservableList<VehicleItem> list){
        // Заполняем список данными.
        if (todayVehiclesStatistic != null) {
            // Копируем все отмеченные ранее пользователем чекбоксы "Фильтр" из предыдущего списка.
            copyFilterFlagList( todayVehiclesStatistic.getItems(), list);
            // Заполняем таблицу данными.
            todayVehiclesStatistic.setItems(list);
        }
    }

    public void setImageOffError(){
        OnOffImage.setImage(new Image("images/switch-off-red-48.png"));
        OnOffImage.setDisable(true);
        OnState = false;
    }

    public void setImageOff(){
        OnOffImage.setImage(new Image("images/switch-off-gray-48.png"));
        OnOffImage.setDisable(false);
        OnState = false;
    }

    public void setImageOn(){
        OnOffImage.setImage(new Image("images/switch-on-green-48.png"));
        OnOffImage.setDisable(false);
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

    public void setMarkDelayView(String value){
        markDelay.setText(value);
    }

    public void onSetupAction (ActionEvent event){

        // Настраиваем поле для ввода пароля.
        final PasswordField passwordPasswordField = new PasswordField();
        GridPane grid = new GridPane();
        grid.add(passwordPasswordField, 0, 0);
        grid.setStyle("-fx-alignment: center");
        passwordPasswordField.setFocusTraversable(true);

        // Настраиваем диалоговое окно.
        Dialog<String> dialog = new Dialog<>();
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK,  ButtonType.CLOSE);

        // Вешаем слушателя на кнопку ОК.
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setOnAction(event1 -> {
            // Сравниваем хэш-суммы паролей.
            String str = MD5(passwordPasswordField.getText());
            // Если верный пароль, даем доступ к меню настроек.
            if ( str.equalsIgnoreCase("eb0a191797624dd3a48fa681d3061212")){
                // ToDo: открыть окно настроек
                /////////////////////////////////////////////////////////////////////////
                FXMLLoader loader = new FXMLLoader(getClass().getResource("settings.fxml"));
                Parent root = null;
                try {
                    root = loader.load();
                    //SettingsController settingsController = loader.getController();
                    Stage stage = new Stage();
                    stage.getIcons().add(new Image("/images/services-32.png"));
                    stage.setTitle("Настройки сервера базы данных");
                    stage.setScene(new Scene(root, -1, -1));
                    stage.show();

                } catch (IOException e) {
                    e.printStackTrace();
                }
                /////////////////////////////////////////////////////////////////////////
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

    public void printAllVehicles(ObservableList<VehicleItem> list){
        if (allDbVehiclesList != null){
            // Заполняем таблицу данными.
            allDbVehiclesList.setItems(list);
        }
    }
}
