package db;

import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import items.VehicleCapacityItem;
import items.VehicleItem;

import java.util.List;

import static broadcast.Broadcast.DatasetManualChangedNotification;

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

        // Получаем экземпляр класса для работы с БД.
        Db.getInstance().addMark(vehicle,timestamp,comment);
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
}
