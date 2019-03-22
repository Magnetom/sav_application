package db;

import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import items.VehicleCapacityItem;
import items.VehicleItem;
import utils.time.DateTime;

import java.time.LocalDateTime;
import java.util.List;

import static broadcast.Broadcast.DatasetManualChangedNotification;
import static servcmd.Parser.parseIntPair;

public class DbProc {


    public static void restoreDateRangeMarks(String vehicle, DbTimestampRange range){
        // Получаем экземпляр класса для работы с БД.
        Db.getInstance().toggleMarks(null,vehicle, range, Db.DataToggleTypes.MARK_AS_RESTORED);
        // Уведомляем подписчика о том, что набор данных был изменен.
        DatasetManualChangedNotification();
    }

    public static void restoreTodayMarks(String vehicle){
        // Получаем экземпляр класса для работы с БД.
        Db.getInstance().toggleMarks(null, vehicle, null, Db.DataToggleTypes.MARK_AS_RESTORED);
        // Уведомляем подписчика о том, что набор данных был изменен.
        DatasetManualChangedNotification();
    }

    public static void restoreMark(int recordId, String vehicle){
        // Получаем экземпляр класса для работы с БД.
        Db.getInstance().toggleMarks(recordId,vehicle, null, Db.DataToggleTypes.MARK_AS_RESTORED);
        // Уведомляем подписчика о том, что набор данных был изменен.
        DatasetManualChangedNotification();
    }

    public static void clearDateRangeMarks(String vehicle, DbTimestampRange range){
        // Получаем экземпляр класса для работы с БД.
        Db.getInstance().toggleMarks(null, vehicle, range, Db.DataToggleTypes.MARK_AS_DELETED);
        // Уведомляем подписчика о том, что набор данных был изменен.
        DatasetManualChangedNotification();
    }

    public static void clearTodayMarks(String vehicle){
        // Получаем экземпляр класса для работы с БД.
        Db.getInstance().toggleMarks(null, vehicle,null,  Db.DataToggleTypes.MARK_AS_DELETED);
        // Уведомляем подписчика о том, что набор данных был изменен.
        DatasetManualChangedNotification();
    }

    public static void clearMark(int recordId, String vehicle){
        // Получаем экземпляр класса для работы с БД.
        Db.getInstance().toggleMarks(recordId,vehicle, null, Db.DataToggleTypes.MARK_AS_DELETED);
        // Уведомляем подписчика о том, что набор данных был изменен.
        DatasetManualChangedNotification();
    }

    public static void addMark(@NotNull String vehicle, @Nullable String timestamp, @Nullable String comment){
        if (vehicle == null) return;


        // Реализуется скрытая возможность: если комментарий указан в формате {Число1:Число2}, то добавляется
        // количество идентичных отметок равным Число1 с шагом в Число2 минут. Служебная команда из комментария удаляется.
        // При этом комментарий может быть заполнен любой другой информацией - вся она будет сохранна вместе с отметкой.

        int repeat = 1; // Количество повторений отметок.
        int step_minutes = 1; // Шаг повторений в минутах.

        if (comment != null && !comment.isEmpty()) {

            StringBuffer bufferComment = new StringBuffer(comment);

            List<Integer[]> list = parseIntPair (bufferComment, true);

            if (list!= null && !list.isEmpty()){
                repeat       = list.get(0)[0];
                step_minutes = list.get(0)[1];

                // Получаем новый комментарий, очищенный от служебных сомволов.
                comment = bufferComment.toString();
            }
        }

        // Получаем стартовую дату и время.
        LocalDateTime currTimestamp = DateTime.getDbTimestampConverter().fromString(timestamp);

        Db db = Db.getInstance();
        if (db == null) return;

        // Выполняем требуемое количество отметок с нужным шагом.
        for (int ii = 0; ii < repeat; ii++){

            // Получаем экземпляр класса для работы с БД и добавляем отметку.
            db.addMark(vehicle,currTimestamp,comment);
            // Инкрементируем шаг в минутах.
            currTimestamp = currTimestamp.plusMinutes(step_minutes);
        }

        // Уведомляем подписчика о том, что набор данных был изменен.
        DatasetManualChangedNotification();
    }

    public static void deleteVehicle(@Nullable String vehicle){
        if (vehicle == null) return;

        // Получаем экземпляр класса для работы с БД.
        Db.getInstance().toggleVehicle(null, vehicle, Db.DataToggleTypes.MARK_AS_DELETED);
        // Уведомляем подписчика о том, что набор данных был изменен.
        DatasetManualChangedNotification();
    }

    public static void restoreVehicle(@Nullable String vehicle){
        if (vehicle == null) return;

        // Получаем экземпляр класса для работы с БД.
        Db.getInstance().toggleVehicle(null, vehicle, Db.DataToggleTypes.MARK_AS_RESTORED);
        // Уведомляем подписчика о том, что набор данных был изменен.
        DatasetManualChangedNotification();
    }

    public static VehicleCapacityItem getCapacity(List<VehicleCapacityItem> list, VehicleItem vehicle){
        if (list == null || vehicle == null) return null;
        for (VehicleCapacityItem item: list) {
            if (vehicle.getCapacity() == item.getId()) return item;
        }
        return null;
    }

    public static void updateVehicleCapacity(VehicleItem vehicleItem, VehicleCapacityItem capacity) {
        if (vehicleItem == null || capacity == null) return;
        Db.getInstance().updateVehicleCapacity(String.valueOf(vehicleItem.getId()), String.valueOf(capacity.getId()));
        // Уведомляем подписчика о том, что набор данных был изменен.
        DatasetManualChangedNotification();
    }

    // Возвращает количество повторов и шаг повторов для автоматической вставки множества отметок.
    /*
    private DecPair getNewMarkRepetitions(){

    }
    */
}
