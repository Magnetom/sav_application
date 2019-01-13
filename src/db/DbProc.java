package db;

import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;

import static broadcast.Broadcast.DatasetManualChangedNotification;

public class DbProc {

    public static void clearTodayMarks(String vehicle){
        // Получаем экземпляр класса для работы с БД.
        Db.getInstance().removeMarks(null,null, vehicle);
        // Уведомляем подписчика о том, что набор данных был изменен.
        DatasetManualChangedNotification();
    }

    public static void clearMark(int recordId){
        // Получаем экземпляр класса для работы с БД.
        Db.getInstance().removeMarks(recordId,null, null);
        // Уведомляем подписчика о том, что набор данных был изменен.
        DatasetManualChangedNotification();
    }

    public static void addMark(@NotNull String vehicle, @Nullable String timestamp, @Nullable String comment){
        if (vehicle == null) return;

        // Получаем экземпляр класса для работы с БД.
        Db.getInstance().addMark(vehicle,timestamp);
        // Уведомляем подписчика о том, что набор данных был изменен.
        DatasetManualChangedNotification();
    }
}
