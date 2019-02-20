package frames;

import db.Db;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;
import javafx.util.StringConverter;

import java.util.List;

public class UserSettings {

    public TableView capacityTable;

    @FXML
    public void initialize() {
        initMainTab();
    }


    private void initMainTab(){

        // Настраиваем таблицу с типами вместимостей.
        capacityTable.getColumns().clear();
        capacityTable.setEditable(true);

        TableColumn<VehicleCapacityItem,  String>  capacityTypeColumn    = new TableColumn<>("Тип");
        TableColumn <VehicleCapacityItem, Integer> vehicleCapacityColumn = new TableColumn<>("Объем (куб.м.)");
        TableColumn <VehicleCapacityItem, String>  commentColumn         = new TableColumn<>("Комментарий");

        capacityTypeColumn.setCellValueFactory   (new PropertyValueFactory<>("type"));
        vehicleCapacityColumn.setCellValueFactory(new PropertyValueFactory<>("capacity"));
        commentColumn.setCellValueFactory        (new PropertyValueFactory<>("comment"));

        capacityTypeColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        vehicleCapacityColumn.setCellFactory(TextFieldTableCell.forTableColumn(new StringConverter<Integer>() {
            @Override
            public String toString(Integer object) {
                if (object == null) return "0";
                return object.toString();
            }
            @Override
            public Integer fromString(String string) {
                if (string == null || string.isEmpty()) return 0;
                Integer val;
                try {
                    val = Integer.valueOf(string);
                } catch (NumberFormatException e){
                    return 0;
                }
                return val;
            }
        }));
        commentColumn.setCellFactory(TextFieldTableCell.forTableColumn());

        vehicleCapacityColumn.setStyle("-fx-alignment: CENTER;");

        capacityTypeColumn.setMinWidth(100);
        vehicleCapacityColumn.setMinWidth(110);
        commentColumn.setMinWidth(180);

        // Текст, который будет отображен в случае пустого списка.
        Label emptyLabel = new Label("Нет данных для отображения.\r\n" +
                "Для добавления первого элемента выполните двойной щелчок по свободному пространству.");
        emptyLabel.setStyle("-fx-text-alignment: center");
        capacityTable.setPlaceholder(emptyLabel);

        // Вешаем слушателя на изменение содержимого ячейки "ТИП".
        capacityTypeColumn.setOnEditCommit((TableColumn.CellEditEvent<VehicleCapacityItem, String> event) -> {
            VehicleCapacityItem item = event.getTableView().getItems().get(event.getTablePosition().getRow());
            item.setType(event.getNewValue());
            //Запись нового значения в БД.
            Db.getInstance().updateCapacity(item);
        });

        // Вешаем слушателя на изменение содержимого ячейки "ГРУЗОВМЕСТИМОСТЬ".
        vehicleCapacityColumn.setOnEditCommit((TableColumn.CellEditEvent<VehicleCapacityItem, Integer> event) -> {
            VehicleCapacityItem item = event.getTableView().getItems().get(event.getTablePosition().getRow());
            item.setCapacity(event.getNewValue());
            //Запись нового значения в БД.
            Db.getInstance().updateCapacity(item);
        });

        // Вешаем слушателя на изменение содержимого ячейки "КОММЕНТАРИЙ".
        commentColumn.setOnEditCommit((TableColumn.CellEditEvent<VehicleCapacityItem, String> event) -> {
            VehicleCapacityItem item = event.getTableView().getItems().get(event.getTablePosition().getRow());
            item.setComment(event.getNewValue());
            //Запись нового значения в БД.
            Db.getInstance().updateCapacity(item);
        });

        // Добавляем новые колонки.
        capacityTable.getColumns().addAll(capacityTypeColumn, vehicleCapacityColumn, commentColumn);

        // Этот код убирает выделение с выделенных колонок при щелчке на пустое место тела таблицы.
        capacityTable.addEventFilter(MouseEvent.MOUSE_CLICKED, evt -> {
            Node source = evt.getPickResult().getIntersectedNode();
            // move up through the node hierarchy until a TableRow or scene root is found
            while (source != null && !(source instanceof TableRow)) {
                source = source.getParent();
            }
            // clear selection on click anywhere but on a filled row
            if (source == null || (source instanceof TableRow && ((TableRow) source).isEmpty())) {
                capacityTable.getSelectionModel().clearSelection();
            }
        });

        // Настраиваем контекстное меню.
        capacityTable.setRowFactory((Callback<TableView<VehicleCapacityItem>, TableRow<VehicleCapacityItem>>) tableView -> {
            final TableRow<VehicleCapacityItem> row = new TableRow<>();
            final ContextMenu rowMenu = new ContextMenu();
            final SeparatorMenuItem separatorMenuItem = new SeparatorMenuItem();

            // Элемент выпадающего меню для администратора - "УДАЛИТЬ ЭЛЕМЕНТ".
            final MenuItem removeItem = new MenuItem("Удалить элемент!");
            removeItem.setOnAction(event -> {
                //DbProc.clearMark(row.getItem().getRecordId());
            });

            // Элемент выпадающего меню для администратора - "ДОБАВИТЬ НОВЫЙ ЭЛЕМЕНТ".
            final MenuItem addItem = new MenuItem("Добавить новый элемент");
            addItem.setOnAction(event -> {
                //addMarkManually(row.getItem().getVehicle());
            });

            rowMenu.getItems().addAll(addItem,separatorMenuItem,removeItem);

            // only display context menu for non-null items:
            row.contextMenuProperty().bind(
                    Bindings.when(Bindings.isNotNull(row.itemProperty()))
                            .then(rowMenu)
                            .otherwise((ContextMenu)null));
            return row;
        });

        // Регистрируем слушателя на двойной щелчек мышью по таблице
        capacityTable.setOnMouseClicked(event -> {
            if(event.getButton().equals(MouseButton.PRIMARY)){
                // Если был зарегистрирован двойной щелчок - добавляем новую сроку.
                if(event.getClickCount() == 2){
                    // Получаем элемент таблицы, на котором был совершен двойной щелчок.
                    VehicleCapacityItem itemSelected = (VehicleCapacityItem)capacityTable.getSelectionModel().getSelectedItem();
                    // Только если двойной щелчок был совершен на пустом месте, то добавляем новую строку
                    if (itemSelected == null) {
                        capacityTable.getItems().add(new VehicleCapacityItem(-1,"SCANIA XT", 0, ""));
                        capacityTable.getSelectionModel().selectLast();
                        System.out.println("Added new empty row...");
                    }
                }
            }
        });

        // Загрузка и отображение всех элементов таблицы БД - CAPACITY.
        List<VehicleCapacityItem> capList = Db.getInstance().getCapacities();
        if (capList != null) {
            ObservableList<VehicleCapacityItem> capacityListObservable = FXCollections.observableArrayList(capList);
            capacityTable.setItems(capacityListObservable);
        }
    }


}
