package db;

import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;

import static broadcast.Broadcast.DatasetManualChangedNotification;

public class DbProc {


    public static void restoreDateRangeMarks(String vehicle, DbDateRange range){
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

    public static void clearDateRangeMarks(String vehicle, DbDateRange range){
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
}
