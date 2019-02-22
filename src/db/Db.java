package db;

import bebug.Log;
import broadcast.OnDbConnectionChanged;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import frames.VehicleCapacityItem;
import marks.VehicleItem;
import marks.VehicleMark;
import settings.CachedSettings;
import utils.Auxiliary;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static utils.Encrypt.decode;

public class Db {

    public static String TAG_SQL = "SQL";
    public static String TAG_DB  = "DB";

    public static final int    DB_VERSION_I = 2;
    public static final String DB_VERSION_S = Auxiliary.alignTwo(DB_VERSION_I);

    ////////////////////////////////////////////////////////
    // Метаданные БД.
    ///////////////////////////////////////////////////////
    // Имя базы данных.
    private static final String GENERAL_SCHEMA_NAME = "aura";

    // Таблицы
    private static final String TABLE_MARKS      = "marks";     // Таблица отметок.
    private static final String TABLE_VEHICLES   = "vehicles";  // Таблица со списком ТС.
    private static final String TABLE_VARIABLES  = "variables"; // Таблица системных переменных.
    private static final String TABLE_CAPACITY   = "capacity";  // Таблица вместимости транспортных средств.

    // Таблица TABLE_MARKS_COLUMNS содержит информацию о всех отметках.
    public static final class TABLE_MARKS_COLUMNS {
        public static final String COLUMN_ID         = "id";
        public static final String COLUMN_VEHICLE    = "vehicle";
        public static final String COLUMN_TIMESTAMP  = "time";
        public static final String COLUMN_MAC        = "mac";
        public static final String COLUMN_REQUEST    = "request";
        public static final String COLUMN_DELETED    = "deleted";
        public static final String COLUMN_COMMENT    = "comment";

        public static final int ID_COLUMN_ID         = 0;
        public static final int ID_COLUMN_VEHICLE    = 1;
        public static final int ID_COLUMN_TIMESTAMP  = 2;
        public static final int ID_COLUMN_MAC        = 3;
        public static final int ID_COLUMN_REQUEST    = 4;
        public static final int ID_COLUMN_DELETED    = 5;
        public static final int ID_COLUMN_COMMENT    = 6;
    }

    // Таблица TABLE_VEHICLES_COLUMNS содержит список введенных гос. номеров и их популярность.
    public static final class TABLE_VEHICLES_COLUMNS {
        public static final String COLUMN_ID         = "id";
        public static final String COLUMN_VEHICLE    = "vehicle";
        public static final String COLUMN_POPULARITY = "popularity";
        public static final String COLUMN_BLOCKED    = "blocked";
        public static final String COLUMN_DELETED    = "deleted";
        public static final String COLUMN_CAPACITY   = "capacity";

        public static final int ID_COLUMN_ID          = 0;
        public static final int ID_COLUMN_VEHICLE     = 1;
        public static final int ID_COLUMN_POPULARITY  = 2;
        public static final int ID_COLUMN_BLOCKED     = 3;
        public static final int ID_COLUMN_CAPACITY    = 4;
    }

    // Таблица TABLE_VARIABLES_COLUMNS содержит список системных переменных.
    public static final class TABLE_VARIABLES_COLUMNS {
        public static final String COLUMN_ID    = "id";
        public static final String COLUMN_NAME  = "name";
        public static final String COLUMN_VALUE = "value";

        public static final int ID_COLUMN_ID       = 0;
        public static final int ID_COLUMN_NAME     = 1;
        public static final int ID_COLUMN_VALUE    = 2;
    }

    // Таблица TABLE_VARIABLES_ROWS содержит список системных переменных.
    public static final class TABLE_VARIABLES_ROWS {
        // Системны переменные
        public static final String SYS_VAR_CONFIGURED       = "configured";
        public static final String SYS_VAR_DATASET          = "dataset";
        public static final String SYS_VAR_SW_VERSION       = "sw_version";
        public static final String SYS_VAR_DB_VERSION       = "db_version";
        public static final String SYS_VAR_MARK_DELAY       = "mark_delay";
        public static final String SYS_VAR_GLOBAL_BLOCKED   = "global_blocked";
    }

    // Таблица TABLE_VEHICLES_COLUMNS содержит список введенных гос. номеров и их популярность.
    public static final class TABLE_CAPACITY_COLUMNS {
        public static final String COLUMN_ID         = "id";
        public static final String COLUMN_TYPE       = "type";
        public static final String COLUMN_CAPACITY   = "capacity";
        public static final String COLUMN_COMMENT    = "comment";

        public static final int ID_COLUMN_ID         = 0;
        public static final int ID_COLUMN_TYPE       = 1;
        public static final int ID_COLUMN_CAPACITY   = 2;
        public static final int ID_COLUMN_COMMENT    = 3;
    }

    ////////////////////////////////////////////////////////
    private static final String FLAG_SET   = "1";
    private static final String FLAG_CLEAR = "0";

    private static final String DB_TRUE  = "1";
    private static final String DB_FALSE = "0";
    ////////////////////////////////////////////////////////
    private static final int   DEFAULT_MARK_DELAY_MIN = 15;
    public static final String DEFAULT_MARK_DELAY_MIN_S = Integer.valueOf(DEFAULT_MARK_DELAY_MIN).toString();
    ////////////////////////////////////////////////////////

    private Connection conn = null;

    private static Db instance = null;

    private OnDbConnectionChanged onDbConnectionChanged = null;

    // Хэш код текущего набора данных, загруженного в визуальные компоненты.
    private String currDatasetHash = null;

    public static Db getInstance(){
        if (instance == null) return instance = new Db();
        return instance;
    }

    private Db(){
        try {
            // Создается экземпляр класса драйвера для работы с MySQL сервером через JDBC-драйвер.
            Class.forName ("com.mysql.cj.jdbc.Driver").newInstance ();
        } catch (InstantiationException e) {
            e.printStackTrace();
            Log.printerror(TAG_DB, "CONNECT","Невозможно создать экземпляр класса {com.mysql.cj.jdbc.Driver}.", e.getLocalizedMessage());
            return;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            Log.printerror(TAG_DB, "CONNECT","Невозможно использовать экземпляр класса {com.mysql.cj.jdbc.Driver}", e.getLocalizedMessage());
            return;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            Log.printerror(TAG_DB, "CONNECT","Не найден класс {com.mysql.cj.jdbc.Driver} драйвера работы с базой данных.", e.getLocalizedMessage());
            return;
        }
        // Подключаемся к БД сразу после создания экземпляра класса.
        //if (!connect()) disconnect();
     }

    // Подключиться к предопределенной базе данных.
    public void connect(){
        Log.println("Попытка установить подключение к базе данных ...");
        if (conn != null) {
            Log.println("Соединение с базой данных уже установлено ранее. Переподключение не требуется.");
            return;
        }
        new Thread(() -> {
            try {
                // Данные для утсановки связи с MySQL сервером.
                String serverName = CachedSettings.SERVER_ADDRESS+":"+CachedSettings.SERVER_PORT;
                String userName = decode("595752746157343D");
                String password = decode("62586C7A635778685A47317062673D3D");
                String url = "jdbc:MySQL://" + serverName;
                conn = DriverManager.getConnection(url, userName, password);
                Log.println("Соединение с базой данных успешно установлено.");

                ///////////////////////////////////////////////////////////////////////////////////////////
                // Пытаемся проверить метаданные базы данных {aura}. Метеданные не верны и не могут быть созданы - считаем,
                // что дальнейшая работа с сервером БД невозможна.
                if (!checkMetadata()) return;
                ///////////////////////////////////////////////////////////////////////////////////////////
            }
            catch (Exception ex)
            {
                conn = null;
                Log.printerror(TAG_SQL, "CONNECT","Невозможно установить соединение с сервером бызы данных: "+CachedSettings.SERVER_ADDRESS+":"+CachedSettings.SERVER_PORT, ex.getLocalizedMessage());
                ex.printStackTrace();
                if (onDbConnectionChanged != null) onDbConnectionChanged.onDisconnect(true);
                return;
            }
            if (onDbConnectionChanged != null) onDbConnectionChanged.onConnect();
        }).start();
    }

    public Boolean isConnected() {return conn!=null;}

    public void reConnect(){
        Log.println("Попытка установить новое подключение к базе данных.");
        // Если соединение уже было установленно ранее, закрываем старое соединение.
        if (conn != null) disconnect();
        connect();
    }

    public void OnFailReConnect(){
        Log.println("При обращении к базе данных произошла ошибка. Будет произведена попытка восстановить связь.");
        reConnect();
    }

    // Отключиться от базы данных.
    private void disconnect(){
        Log.println("Происходит разрыв текущего соединения с базой данных ...");
        if (conn != null) {
            try {
                conn.close ();
                conn = null;
                Log.println("Связь с базой данных успешно разорвана.");
            }
            catch (Exception ex) {
                Log.printerror(TAG_SQL, "DISCONNECT","Отключение от базы данных произошло с ошибкой!", ex.getLocalizedMessage());
            }
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        disconnect();
    }

    public void setOnDbConnectionChangedListener(OnDbConnectionChanged listener){
        onDbConnectionChanged = listener;
    }

    // Очистить БД полность.
    public Boolean allDbRemove(){
        Boolean result;
        // Удаляются все ТС.
        result = removeAllVehicles();
        // Удаляются все отметки.
        result &= toggleMarks(null, null, new DbDateRange(true), DataToggleTypes.REAL_DELETE);
        // Удаляются все отметки.
        result &= removeAllVariables();
        // Удаляются все грузовместимости.
        result &= removeAllCapacity();
        return result;
    }

    // Удаляются все грузовместимости.
    private Boolean removeAllCapacity() {
        String query = "DELETE FROM "+GENERAL_SCHEMA_NAME+"."+TABLE_CAPACITY+";";
        try {
            conn.createStatement().executeUpdate(query);
        } catch (SQLException e) {
            e.printStackTrace();
            OnFailReConnect(); // Пытаемся переконнектиться.
            Log.printerror(TAG_SQL, "REMOVE_ALL_CAPACITY",e.getMessage(), query);
            return false;
        }
        return true;
    }

    // Удаляются все переменные.
    private Boolean removeAllVariables() {
        String query = "DELETE FROM "+GENERAL_SCHEMA_NAME+"."+TABLE_VARIABLES+";";
        try {
            conn.createStatement().executeUpdate(query);
        } catch (SQLException e) {
            e.printStackTrace();
            OnFailReConnect(); // Пытаемся переконнектиться.
            Log.printerror(TAG_SQL, "REMOVE_ALL_VARIABLES",e.getMessage(), query);
            return false;
        }
        return true;
    }

    // Очистить список ТС.
    public Boolean removeAllVehicles() {
        String query = "DELETE FROM "+GENERAL_SCHEMA_NAME+"."+TABLE_VEHICLES+";";
        try {
            conn.createStatement().executeUpdate(query);
        } catch (SQLException e) {
            e.printStackTrace();
            OnFailReConnect(); // Пытаемся переконнектиться.
            Log.printerror(TAG_SQL, "REMOVE_ALL_VEHICLES",e.getMessage(), query);
            return false;
        }
        return true;
    }

    enum PopularityActionTypes {INCREMENT, DECREMENT, NOTHING}

    // Увеличить рейтинг на единицу.
    private Boolean incrementPopularity(String vehicle){
        return updatePopularity(vehicle, PopularityActionTypes.INCREMENT);
    }

    // Уменьшит рейтинг на единицу.
    private Boolean decrementPopularity(String vehicle){
        return updatePopularity(vehicle, PopularityActionTypes.DECREMENT);
    }

    // Увеличить рейтинг на единицу.
    private Boolean updatePopularity(String vehicle, PopularityActionTypes actionType){
        if(!isConnected()) return false;

        if (vehicle == null) return false;

        String action;
        switch (actionType){
            case DECREMENT: action = "-"; break;
            case INCREMENT: action = "+"; break;
            case NOTHING  : return true;
            default: action = "+"; break;
        }

        String query = "UPDATE "+GENERAL_SCHEMA_NAME+"."+TABLE_VEHICLES+" SET "+TABLE_VEHICLES_COLUMNS.COLUMN_POPULARITY+"="+TABLE_VEHICLES_COLUMNS.COLUMN_POPULARITY+action+"1 WHERE "+TABLE_VEHICLES_COLUMNS.COLUMN_VEHICLE+"='"+vehicle+"';";
        try {
            conn.createStatement().executeUpdate(query);
        } catch (SQLException e) {
            e.printStackTrace();
            OnFailReConnect(); // Пытаемся переконнектиться.
            Log.printerror(TAG_SQL, "UPDATE_POPULARITY",e.getMessage(), query);
            return false;
        }
        return true;
    }


    // Очистить все рейтинги популярности.
    public Boolean resetPopularity(){
        if(!isConnected()) return false;
        String query = "UPDATE "+GENERAL_SCHEMA_NAME+"."+TABLE_VEHICLES+" SET "+TABLE_VEHICLES_COLUMNS.COLUMN_POPULARITY+"=0;";
        try {
            conn.createStatement().executeUpdate(query);
        } catch (SQLException e) {
            e.printStackTrace();
            OnFailReConnect(); // Пытаемся переконнектиться.
            Log.printerror(TAG_SQL, "RESET_ALL_POPULARITY",e.getMessage(), query);
            return false;
        }
        return true;
    }

    /* Brief: Ручная установка отметки ТС.
     *
     * @vehicle   - госномер ТС для которого будет выполнена отметка.
     * @timestamp - метка времени, которая будет присвоена отметке. Если в качестве этого параметра
     *              передано NULL (или пустая строка), то отметка выполняется с текущей меткой времени.
     */
    Boolean addMark(@NotNull  String vehicle,
                    @Nullable String timestamp,
                    @Nullable String comment){

        if (vehicle==null || vehicle.isEmpty()) return false;
        if (comment == null) comment = "";

        String request_id = Auxiliary.genStrongUidString(20);
        String writer_id = "application";
        String query;

        if (timestamp == null || timestamp.isEmpty())
            query = "INSERT INTO "+GENERAL_SCHEMA_NAME+"."+TABLE_MARKS+" ("+TABLE_MARKS_COLUMNS.COLUMN_VEHICLE+","+TABLE_MARKS_COLUMNS.COLUMN_MAC+","+TABLE_MARKS_COLUMNS.COLUMN_REQUEST+","+TABLE_MARKS_COLUMNS.COLUMN_COMMENT+") " +
                    "VALUES ('"+vehicle+"','"+writer_id+"','"+request_id+"','"+comment+"');";
        else
            query = "INSERT INTO "+GENERAL_SCHEMA_NAME+"."+TABLE_MARKS+" ("+TABLE_MARKS_COLUMNS.COLUMN_VEHICLE+","+TABLE_MARKS_COLUMNS.COLUMN_MAC+","+TABLE_MARKS_COLUMNS.COLUMN_REQUEST+","+TABLE_MARKS_COLUMNS.COLUMN_TIMESTAMP+","+TABLE_MARKS_COLUMNS.COLUMN_COMMENT+") " +
                    "VALUES ('"+vehicle+"','"+writer_id+"','"+request_id+"','"+timestamp+"','"+comment+"');";

        try {
            conn.createStatement().executeUpdate(query);

            // Если запрос выполнен удачно, то обновляем рейтинг популярности.
            incrementPopularity(vehicle);

        } catch (SQLException e) {
            e.printStackTrace();
            Log.printerror(TAG_SQL, "ADD_MARK",e.getMessage(), query);
            OnFailReConnect(); // Пытаемся переконнектиться.
            return false;
        }

        return true;
    }

    /* Brief: Переключает флаг состояния ТС между УДАЛЕН и ВОССТАНОВЛЕН. Также возможно физическое
     *        удаление объекта при указании соответствующего @toggleType.
     *
     */
    public Boolean toggleVehicle(@Nullable String recordId,
                                 @NotNull  String vehicle,
                                 @NotNull  DataToggleTypes toggleType) {

        String query;
        String condition;

        // Защита от непреднамеренной очистки всех данных ТС.
        if ( (recordId == null || recordId.isEmpty()) && (vehicle == null || vehicle.isEmpty()) ) return false;

        if (recordId != null) condition = TABLE_VEHICLES_COLUMNS.COLUMN_ID+"='"+recordId+"'";
        else
                              condition = TABLE_VEHICLES_COLUMNS.COLUMN_VEHICLE+"='"+vehicle+"'";

        //if (!condition.isEmpty()) condition = "WHERE " + condition;
        condition = "WHERE " + condition;

        switch (toggleType){
            //------------------------------------------------------------------------------------------
            // Задание на удаление.
            case MARK_AS_DELETED:
                query = "UPDATE " + GENERAL_SCHEMA_NAME + "." + TABLE_VEHICLES + " SET " + TABLE_VEHICLES_COLUMNS.COLUMN_DELETED + "=1 " + condition + ";";
                break;
            //------------------------------------------------------------------------------------------
            // Задание на восстановление ранее помеченной на удаление записи.
            case MARK_AS_RESTORED:
                query = "UPDATE " + GENERAL_SCHEMA_NAME + "." + TABLE_VEHICLES + " SET " + TABLE_VEHICLES_COLUMNS.COLUMN_DELETED + "=0 " + condition + ";";
                break;
            //------------------------------------------------------------------------------------------
            // Реальное удаление данных выборки без возможности их восстановления.
            case REAL_DELETE:
                query = "DELETE FROM " + GENERAL_SCHEMA_NAME + "." + TABLE_VEHICLES + " " + condition + ";";
                break;
            //------------------------------------------------------------------------------------------
            default:
                query = ";";
                break;
        }
        try {
            conn.createStatement().executeUpdate(query);
        } catch (SQLException e) {
            e.printStackTrace();
            OnFailReConnect();
            Log.printerror(TAG_SQL, "TOGGLE_VEHICLE",e.getMessage(), query);
            return false;
        }
        return true;
    }

    // Установить статус ТС: TRUE - заблокировано, FALSE - разблокировано/норма.
    public Boolean setVehicleBlocked(String vehicle, boolean blocked){
        if (vehicle == null || (vehicle.equals("")) ) return false;
        if(!isConnected()) return false;
        Integer st = (blocked)?1:0;
        String query = "UPDATE "+GENERAL_SCHEMA_NAME+"."+TABLE_VEHICLES+" SET "+TABLE_VEHICLES_COLUMNS.COLUMN_BLOCKED+"="+st+" WHERE "+TABLE_VEHICLES_COLUMNS.COLUMN_VEHICLE+"='"+vehicle+"';";
        try {
            conn.createStatement().executeUpdate(query);
        } catch (SQLException e) {
            e.printStackTrace();
            OnFailReConnect(); // Пытаемся переконнектиться.
            Log.printerror(TAG_SQL, "SET_VEHICLE_STATE",e.getMessage(), query);
            return false;
        }
        return true;
    }

    // Заблокировать ТС.
    public Boolean setVehicleBlocked(String vehicle) {
        return setVehicleBlocked(vehicle, true);
    }

    // Разблокировать ТС.
    public Boolean setVehicleUnblocked(String vehicle) {
        return setVehicleBlocked(vehicle, false);
    }


    private String getDateRangeTimestampCondition(DbDateRange dateRange){
        if (dateRange != null){
            if (dateRange.getAllDatesFlag()){
                return "";
            } else {
                if (dateRange.isSingleDate()) {
                    return "DATE(" + TABLE_MARKS_COLUMNS.COLUMN_TIMESTAMP + ")=DATE('" + dateRange.getSingleDate() + "')";
                } else {
                    return "DATE(" + TABLE_MARKS_COLUMNS.COLUMN_TIMESTAMP + ")>=DATE('" + dateRange.getStartDate() + "') AND DATE(" + TABLE_MARKS_COLUMNS.COLUMN_TIMESTAMP + ")<=DATE('" + dateRange.getStopDate() + "')";
                }
            }
        } else {
            return "DATE("+TABLE_MARKS_COLUMNS.COLUMN_TIMESTAMP+")=DATE(NOW())";
        }
    }

    public enum DataToggleTypes {
        MARK_AS_DELETED, MARK_AS_RESTORED, REAL_DELETE
    }

    /* Brief: Удалить/восстановить все отметки за диапазон дат.
     *
     * Если указан параметр @recordId, все остальные условия отбрасываются.
     * @recordId - уникальный идентификатор записи (строки).
     *
     * Если в качестве параметра @date указан NULL, то удалить только за текущую дату.
     * Если @date - пустая строка, то удалить все данные.
     *
     * Если в качестве параметра @vehicle указан NULL или пустая строка, то выборка будет касаться всех ТС.
     * Если @vehicle - не пустая строка, то выборка будет касаться только указанного ТС.
     */
    public Boolean toggleMarks(@Nullable Integer recordId,
                               @NotNull  String vehicle,
                               @Nullable DbDateRange dateRange,
                               @NotNull  DataToggleTypes toggleType){
        String query;
        String condition;

        if (recordId != null) condition = TABLE_MARKS_COLUMNS.COLUMN_ID+"='"+recordId.toString()+"'";
        else {
            condition = getDateRangeTimestampCondition(dateRange);
        }

        // Дополнительное условие - выборка только для конкретного ТС.
        if (vehicle!=null && !vehicle.isEmpty() && recordId == null) {
            if (!condition.isEmpty()) condition += " AND ";
            condition += TABLE_MARKS_COLUMNS.COLUMN_VEHICLE+"='"+vehicle+"'";
        }

        condition = "WHERE " + condition;

        PopularityActionTypes popAction = PopularityActionTypes.NOTHING;

        switch (toggleType){
            //------------------------------------------------------------------------------------------
            // Задание на удаление.
            case MARK_AS_DELETED:
                query = "UPDATE " + GENERAL_SCHEMA_NAME + "." + TABLE_MARKS + " SET " + TABLE_MARKS_COLUMNS.COLUMN_DELETED + "=1 " + condition + ";";
                popAction = PopularityActionTypes.DECREMENT;
                break;
            //------------------------------------------------------------------------------------------
            // Задание на восстановление ранее помеченной на удаление записи.
            case MARK_AS_RESTORED:
                query = "UPDATE " + GENERAL_SCHEMA_NAME + "." + TABLE_MARKS + " SET " + TABLE_MARKS_COLUMNS.COLUMN_DELETED + "=0 " + condition + ";";
                popAction = PopularityActionTypes.INCREMENT;
                break;
            //------------------------------------------------------------------------------------------
            // Реальное удаление данных выборки без возможности их восстановления.
            case REAL_DELETE:
                query = "DELETE FROM " + GENERAL_SCHEMA_NAME + "." + TABLE_MARKS + " " + condition + ";";
                popAction = PopularityActionTypes.DECREMENT;
                break;
            //------------------------------------------------------------------------------------------
            default:
                query = ";";
                break;
        }

        try {
            conn.createStatement().executeUpdate(query);
            updatePopularity(vehicle, popAction);
        } catch (SQLException e) {
            e.printStackTrace();
            OnFailReConnect(); // Пытаемся переконнектиться.
            Log.printerror(TAG_SQL, "TOGGLE_MARKS",e.getMessage(), query);
            return false;
        }
        return true;
    }

    /* Brief: Возвращает список/лог отметок в диапазоне дат @dateRange.
     *
     * Если параметр @dateRange равен NULL, то выборка за текущую дату.
     * Если параметр @showDeleted равен TRUE, то в списке будут присутствовать удаленные ранее отметки т.е.
     * те, которые отмечены на удаление (столбец deleted в таблице marks).
     */
    public List<VehicleMark> getMarksRawList(DbDateRange dateRange, boolean showDeleted) throws SQLException {

        if(!isConnected()) return null;

        ResultSet rs;

        String condition = getDateRangeTimestampCondition(dateRange);

        // Если не требуется показывать удаленные ранее отметки, создаем дополнительный фильтр.
        if (!showDeleted) {
            if (!condition.isEmpty()) condition += " AND ";
            condition += TABLE_MARKS_COLUMNS.COLUMN_DELETED+"=0";
        }

        if (!condition.isEmpty()) condition = "WHERE "+condition;
        try {
            String query = "SELECT * FROM "+GENERAL_SCHEMA_NAME+"."+TABLE_MARKS+" "+condition+";";
            rs = conn.createStatement().executeQuery(query);
        } catch (SQLException e){
            OnFailReConnect(); // Пытаемся переконнектиться.
            throw e;
        }

        List<VehicleMark> marksList = new ArrayList<>();
        while (rs.next()) {
            //String timestamp = getHHMMFromStringTimestamp(rs.getString(TABLE_MARKS_COLUMNS.COLUMN_TIMESTAMP));
            String timestamp = rs.getString(TABLE_MARKS_COLUMNS.COLUMN_TIMESTAMP);
            String vehicle   = rs.getString(TABLE_MARKS_COLUMNS.COLUMN_VEHICLE);
            String request = rs.getString(TABLE_MARKS_COLUMNS.COLUMN_REQUEST);
            boolean deleted  = rs.getBoolean(TABLE_MARKS_COLUMNS.COLUMN_DELETED);
            String comment   = rs.getString(TABLE_MARKS_COLUMNS.COLUMN_COMMENT);
            int    recordId  = rs.getInt(TABLE_MARKS_COLUMNS.COLUMN_ID);

            VehicleMark mark = new VehicleMark(timestamp, vehicle, request, deleted, recordId, comment);
            marksList.add(mark);
        }
        return marksList;
    }

    /* Получить полный список всех зарегистрированных ТС.
     *
     * Если параметр @show_deleted равен TRUE, то в выбоке будут присутствовать удаленные объекты.
     */
    public List<VehicleItem> getAllVehicles(boolean show_deleted) throws  SQLException{
        if(!isConnected()) return null;

        ResultSet rs;
        String condition = "";

        if (!show_deleted) condition = "WHERE "+TABLE_VEHICLES_COLUMNS.COLUMN_DELETED+"=0";

        try {
            rs = conn.createStatement().executeQuery("SELECT * FROM "+GENERAL_SCHEMA_NAME+"."+TABLE_VEHICLES + " "+condition + ";");
        } catch (SQLException e){
            OnFailReConnect(); // Пытаемся переконнектиться.
            throw e;
        }

        List<VehicleItem> vehiclesList = new ArrayList<>();
        while (rs.next()) {
            vehiclesList.add(new VehicleItem(
                    rs.getString(TABLE_VEHICLES_COLUMNS.COLUMN_VEHICLE),
                    0,
                    rs.getBoolean(TABLE_VEHICLES_COLUMNS.COLUMN_BLOCKED),
                    rs.getBoolean(TABLE_VEHICLES_COLUMNS.COLUMN_DELETED),
                    rs.getInt(TABLE_VEHICLES_COLUMNS.COLUMN_POPULARITY)+1,
                    false));
        }
        return vehiclesList;
    }


    // Получить список заблокированных ТС. В список не попадают помеченные на удаление ТС.
    // Далее, если искомого ТС нет в списке, то считать его НЕ заблокированным.
    private List<String> getBlockedVehicles() throws  SQLException{
        if(!isConnected()) return null;

        ResultSet rs;
        try {
            rs = conn.createStatement().executeQuery("SELECT * FROM "+GENERAL_SCHEMA_NAME+"."+TABLE_VEHICLES+" WHERE "+TABLE_VEHICLES_COLUMNS.COLUMN_BLOCKED+"='1'");
        } catch (SQLException e){
            OnFailReConnect(); // Пытаемся переконнектиться.
            throw e;
        }


        List<String> blackList = new ArrayList<>();
        while (rs.next()) {
            String vehicle = rs.getString(TABLE_VEHICLES_COLUMNS.COLUMN_VEHICLE);
            blackList.add(vehicle);
        }
        return blackList;
    }

    // Получить статистику по каждому ТС: [госномер]-[количество кругов]-[статус блокировки].
    // Может работать с уже имеющимся списком отметок за текущий день. Если список не передан (NULL), тогда
    // список будет сформирован с помощью соответствующего запроса в БД.
    public List<VehicleItem> getVehiclesStatistic(DbDateRange dateRange, @Nullable List<VehicleMark> markList) throws SQLException{

        if(!isConnected()) return null;

        // Получаем (если это необходимо) лог отметок за текущий день.
        // При этом не учитываем объекты, помеченные на удаление.
        if (markList == null) markList = getMarksRawList(dateRange, false);

        // Получаем список заблокированных ТС.
        List<String> blackList;
        try {
            blackList = getBlockedVehicles();
        } catch (SQLException e){
            OnFailReConnect(); // Пытаемся переконнектиться.
            throw e;
        }

        //////////////////////////////////////////////////////////////////////////////////////
        boolean exist;

        // Создаем экземпляр класса списка статистики.
        List<VehicleItem> list = new ArrayList<>();

        for (VehicleMark mark: markList) {
            exist = false;
            for (VehicleItem item: list) {
                // если ТС уже есть в итоговом списке, увеличиваем счетчик кругов и переходим к следующей итерации.
                if (item.getVehicle().equalsIgnoreCase(mark.getVehicle())){
                    item.setLoopsCnt(item.getLoopsCnt()+1);
                    exist = true;
                    break;
                }
            }
            // Если мы попали сюда, занчит этого ТС еще нет в списке. Добавляем.
            if (!exist) {
                boolean isBlocked = false;
                // Ищем ТС в списке заблокированных ТС.
                if (blackList != null)
                for (String blackItem : blackList) {
                    // Если ТС есть в списке заблокированных - делаем отметку об этом.
                    if (mark.getVehicle().equalsIgnoreCase(blackItem)) isBlocked = true;
                }
                // Добавляем новое ТС в новый список.
                list.add( new VehicleItem(mark.getVehicle(), 1, isBlocked, false,0,false) );
            }
        }
        //////////////////////////////////////////////////////////////////////////////////////

        return list;
    }

    /* Установить статус глобальной блокировки отметок: TRUE - все отметки заблокированы.
     *                                                  FALSE - все отметки глобально разрешены.
     */
    public Boolean setGlobalBlock(Boolean block){
        Integer val;
        val = (block)?1:0;
        return setSysVariable(TABLE_VARIABLES_ROWS.SYS_VAR_GLOBAL_BLOCKED, val);
    }

    /* Устнавливает в БД в таблице variables значение value для переменной name. */
    public Boolean setSysVariable(String name, Object value){

        if (name == null || (name.equals("")) ) return false;
        if(!isConnected()) return false;

        String value_final;

        if (value instanceof Integer) value_final = value.toString();
        else
            value_final = "'"+value.toString()+"'";

        String query = "INSERT INTO "+GENERAL_SCHEMA_NAME+"."+TABLE_VARIABLES+" ("+TABLE_VARIABLES_COLUMNS.COLUMN_NAME+","+TABLE_VARIABLES_COLUMNS.COLUMN_VALUE+") VALUES ('"+name+"',"+value_final+") ON DUPLICATE KEY UPDATE "+TABLE_VARIABLES_COLUMNS.COLUMN_VALUE+"="+value_final+";";
        try {
            conn.createStatement().executeUpdate(query);
        } catch (SQLException e) {
            e.printStackTrace();
            Log.printerror(TAG_SQL, "SET_SYS_VARIABLE",e.getMessage(), query);
            OnFailReConnect(); // Пытаемся переконнектиться.
            return false;
        }
        return true;
    }


    /* Возвращает значение системной переменной из БД. */
    public Object getSysVariable(String name){
        if(!isConnected()) return null;
        String query = "SELECT "+TABLE_VARIABLES_COLUMNS.COLUMN_VALUE+" FROM "+GENERAL_SCHEMA_NAME+"."+TABLE_VARIABLES+" WHERE "+TABLE_VARIABLES_COLUMNS.COLUMN_NAME+"='"+name+"';";
        try {
            ResultSet rs = conn.createStatement().executeQuery(query);

            if (rs.next()){
                return rs.getObject(TABLE_VARIABLES_COLUMNS.COLUMN_VALUE);
            } else {
                return null;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            Log.printerror(TAG_SQL, "GET_SYS_VARIABLE",e.getMessage(), query);
            OnFailReConnect(); // Пытаемся переконнектиться.
            return null;
        }
    }

    /* Возвращает TRUE если текущий набор даных на сервере изменился с момента последней выборки. */
    public Boolean isDatasetModified() {
        if(!isConnected()) return false;
        ResultSet rs;

        try {
            rs = conn.createStatement().executeQuery("SELECT * FROM "+GENERAL_SCHEMA_NAME+"."+TABLE_VARIABLES+" WHERE "+TABLE_VARIABLES_COLUMNS.COLUMN_NAME+"='"+TABLE_VARIABLES_ROWS.SYS_VAR_DATASET+"';");
            if (rs.next()){
                String value = rs.getString(TABLE_VARIABLES_COLUMNS.COLUMN_VALUE);

                if (currDatasetHash == null){
                    currDatasetHash = value;
                    return true;
                }
                if (!currDatasetHash.equalsIgnoreCase(value)){
                    currDatasetHash = value;
                    return true;
                }
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            OnFailReConnect(); // Пытаемся переконнектиться.
            return false;
        }
        return false;
    }

    public boolean executeScript(String query){
        try {
            if (isConnected()) {
                conn.createStatement().execute(query);
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Log.printerror(TAG_SQL, "EXECUTE_SCRIPT",e.getMessage(), query);
            return false;
        }
        return false;
    }

    public void executeScriptThrow(String query) throws SQLException{
        if (isConnected()) conn.createStatement().executeQuery(query);
    }

    public boolean updateCapacity(VehicleCapacityItem item){

        if (item == null) return false;

        String query = "UPDATE "+GENERAL_SCHEMA_NAME+"."+TABLE_CAPACITY+
                " SET "+
                TABLE_CAPACITY_COLUMNS.COLUMN_TYPE+"='"+item.getType() + "', "+
                TABLE_CAPACITY_COLUMNS.COLUMN_CAPACITY+"="+item.getCapacity() + " , "+
                TABLE_CAPACITY_COLUMNS.COLUMN_COMMENT+"='"+item.getComment() + "' "+
                " WHERE "+TABLE_CAPACITY_COLUMNS.COLUMN_ID+"="+item.getId() + ";";
        try {
            conn.createStatement().executeUpdate(query);
        } catch (SQLException e) {
            e.printStackTrace();
            Log.printerror(TAG_SQL, "UPDATE_TABLE_CAPACITY",e.getMessage(), query);
            return false;
        }
        return true;
    }

    public List<VehicleCapacityItem> getCapacities(){

        List<VehicleCapacityItem> itemsList = new ArrayList<>();
        ResultSet rs;
        try {
            rs = conn.createStatement().executeQuery("SELECT * FROM "+GENERAL_SCHEMA_NAME+"."+TABLE_CAPACITY+";");

            while (rs.next()){
                VehicleCapacityItem item = new VehicleCapacityItem();

                item.setId(Integer.valueOf(rs.getString(TABLE_CAPACITY_COLUMNS.COLUMN_ID)));
                item.setType(rs.getString(TABLE_CAPACITY_COLUMNS.COLUMN_TYPE));
                item.setCapacity(Integer.valueOf(rs.getString(TABLE_CAPACITY_COLUMNS.COLUMN_CAPACITY)));
                item.setComment(rs.getString(TABLE_CAPACITY_COLUMNS.COLUMN_COMMENT));

                itemsList.add(item);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return  itemsList;
    }

    /* Прверка целостности БД. Создание БД и ее таблиц при необходимости. */
    private Boolean checkMetadata(){
        boolean result;
        Log.println("Проверка целостности базы данных.");
        if (conn == null) {
            Log.println("Проверка прервана из-за отсутствия подключения к БД.");
            return false;
        }
        try {
            dbInitCreate(false);

            // Проверяем наличие в БД переменной @configured. Если эта переменная отсутствует, или ее значение {FLAG_CLEAR},
            // то считаем этот запуск первым и необходимо проинициализировать некоторые переменные.
            Object configured = getSysVariable(TABLE_VARIABLES_ROWS.SYS_VAR_CONFIGURED);
            // И так, в случае первого запуска программы инициализируем переменные.
            if (configured == null || configured.toString().equals(FLAG_CLEAR)){

                Log.println("Первый запуск программы. Настраиваются системные переменные БД.");
                // Инициализация переменных при первой работе с БД.
                result = variablesInitCreate();

                if (!result) {
                    Log.println("Настройка первого запуска завершилась с ошибкой\r\nПодробнее:\r\nНевозможно сохранить в БД одну или несколько системных переменных!");
                    return false;
                }

                // Устанавливаем в {1} флаг-переменную @configured.
                result = setSysVariable(TABLE_VARIABLES_ROWS.SYS_VAR_CONFIGURED, FLAG_SET);

                if (result) Log.println("Настройка первого запуска завершена успешно.");
                else {
                    Log.println("Настройка первого запуска завершилась с ошибкой\r\nПодробнее:\r\nНевозможно сохранить в БД системную переменную {configured}!");
                    return false;
                }
            }

            // Проверка обновлений.
            result = checkForUpdates();

        } catch (Exception e) {
            e.printStackTrace();
            Log.println("Проверка целостности базы данных завершилась с ошибками! Подробнее:");
            Log.println(e.getLocalizedMessage());
            return false;
        }
        Log.println("Проверка целостности базы данных завершена.");
        return result;
    }

    /* Создамне базы данных и всех ее таблиц. */
    // Если параметр remove = TRUE - удалить существующую БД перед сзданием новой.
    private void dbInitCreate(boolean remove) throws Exception{

        int result = 0;

        if (remove) {
            Log.println("Удаление базы данных {aura}.");
            result = conn.createStatement().executeUpdate("DROP DATABASE IF EXISTS "+GENERAL_SCHEMA_NAME+";");
        }

        // Создать базу данных {aura}, если отсутствует.
        Log.println("Проверка существования/создание базы данных {aura}.");
        result = conn.createStatement().executeUpdate("CREATE DATABASE IF NOT EXISTS "+GENERAL_SCHEMA_NAME+";");

        // Создать таблицу {marks}, если отсутствует.
        Log.println("Проверка существования/создание табицы {marks}.");
        result = conn.createStatement().executeUpdate("create table if not exists "+GENERAL_SCHEMA_NAME+"."+TABLE_MARKS+"\n" +
                "(\n" +
                "  "+TABLE_MARKS_COLUMNS.COLUMN_ID+" int(11) unsigned auto_increment primary key,\n" +
                "  "+TABLE_MARKS_COLUMNS.COLUMN_VEHICLE+" varchar(14) not null,\n" +
                "  "+TABLE_MARKS_COLUMNS.COLUMN_TIMESTAMP+" timestamp default CURRENT_TIMESTAMP not null,\n" +
                "  "+TABLE_MARKS_COLUMNS.COLUMN_MAC+" varchar(18) null,\n" +
                "  "+TABLE_MARKS_COLUMNS.COLUMN_REQUEST+" varchar(20) not null,\n" +
                "  "+TABLE_MARKS_COLUMNS.COLUMN_DELETED+" bit default b'0' not null,\n" +
                "  "+TABLE_MARKS_COLUMNS.COLUMN_COMMENT+" varchar(255) null,\n" +
                "  constraint request_unique\n" +
                "  unique ("+TABLE_MARKS_COLUMNS.COLUMN_REQUEST+")\n" +
                ");");

        // Создать таблицу {variables}, если отсутствует.
        Log.println("Проверка существования/создание табицы {variables}.");
        result = conn.createStatement().executeUpdate("create table if not exists "+GENERAL_SCHEMA_NAME+"."+TABLE_VARIABLES+"\n" +
                "(\n" +
                "  "+TABLE_VARIABLES_COLUMNS.COLUMN_ID+" int unsigned auto_increment primary key,\n" +
                "  "+TABLE_VARIABLES_COLUMNS.COLUMN_NAME+" varchar(50) default 'unknown' not null,\n" +
                "  "+TABLE_VARIABLES_COLUMNS.COLUMN_VALUE+" varchar(50) default 'unknown' not null,\n" +
                "  constraint variable_unique\n" +
                "  unique ("+TABLE_VARIABLES_COLUMNS.COLUMN_NAME+")\n" +
                ");");

        // Создать таблицу {vehicles}, если отсутствует.
        Log.println("Проверка существования/создание табицы {vehicles}.");
        result = conn.createStatement().executeUpdate("create table if not exists "+GENERAL_SCHEMA_NAME+"."+TABLE_VEHICLES+"\n" +
                "(\n" +
                "  "+TABLE_VEHICLES_COLUMNS.COLUMN_ID+" int(11) unsigned auto_increment primary key,\n" +
                "  "+TABLE_VEHICLES_COLUMNS.COLUMN_VEHICLE+" varchar(18) not null,\n" +
                "  "+TABLE_VEHICLES_COLUMNS.COLUMN_POPULARITY+" int(11) unsigned default '0' not null,\n" +
                "  "+TABLE_VEHICLES_COLUMNS.COLUMN_BLOCKED+" bit default b'0' not null,\n" +
                "  "+TABLE_VEHICLES_COLUMNS.COLUMN_CAPACITY+" int(11) unsigned default '0' not null,\n" +
                "  constraint vehicle_id_unique\n" +
                "  unique ("+TABLE_VEHICLES_COLUMNS.COLUMN_VEHICLE+")\n" +
                ");");

        // Создать таблицу {capacity}, если отсутствует.
        Log.println("Проверка существования/создание табицы {capacity}.");
        result = conn.createStatement().executeUpdate("create table if not exists "+GENERAL_SCHEMA_NAME+"."+TABLE_CAPACITY+"\n" +
                "(\n" +
                "  "+TABLE_CAPACITY_COLUMNS.COLUMN_ID+" int(11) unsigned auto_increment primary key,\n" +
                "  "+TABLE_CAPACITY_COLUMNS.COLUMN_TYPE+" varchar(50) not null,\n" +
                "  "+TABLE_CAPACITY_COLUMNS.COLUMN_CAPACITY+" int(11) unsigned default '0' not null,\n" +
                "  "+TABLE_CAPACITY_COLUMNS.COLUMN_COMMENT+" varchar(255) null);");
    }

    // Создаем необходимые переменные в соответствующих таблицах БД после первого запуска и инициализации БД.
    private boolean variablesInitCreate(){
        boolean result;
        // Устанавливаем текущую версию БД.
        result = setSysVariable(TABLE_VARIABLES_ROWS.SYS_VAR_DB_VERSION, DB_VERSION_I);
        Log.println("Создание переменной {"+TABLE_VARIABLES_ROWS.SYS_VAR_DB_VERSION+"} ...... ["+((result)?"OK":"FAIL")+"]");
        // Возможность отметок глобально заблокирована.
        result &= setSysVariable(TABLE_VARIABLES_ROWS.SYS_VAR_GLOBAL_BLOCKED, DB_TRUE);
        Log.println("Создание переменной {"+TABLE_VARIABLES_ROWS.SYS_VAR_GLOBAL_BLOCKED+"} ...... ["+((result)?"OK":"FAIL")+"]");
        // Время между отметками по-умолчанию 15 минут.
        result &= setSysVariable(TABLE_VARIABLES_ROWS.SYS_VAR_MARK_DELAY, DEFAULT_MARK_DELAY_MIN_S);
        Log.println("Создание переменной {"+TABLE_VARIABLES_ROWS.SYS_VAR_MARK_DELAY+"} ...... ["+((result)?"OK":"FAIL")+"]");

        return result;
    }

    // Проверяет, необходимо ли установить какое-либо обновление: исправление/дополнение структуры базы данных и ее метаданных.
    // В случае обнаружения такой необходимости, применяем правильное обновление.
    private boolean checkForUpdates(){
        boolean result = true;
        Log.println("Проверка необходимости обновления БД и ее метаданных.");
        Object cur_db_version = getSysVariable(TABLE_VARIABLES_ROWS.SYS_VAR_DB_VERSION);

        // Если версия не определена или она равна версии 1
        if (cur_db_version == null || cur_db_version.toString().equals("1")){
            // Запускаем преобразование до следующей версии.
            result = update_to_v2();
        }
        Log.println("Проверка обновлений завершена.");
        Log.println("Текущая версия базы данных: "+DB_VERSION_S);
        return result;
    }

    /* Обновление до версии v2. */
    private boolean update_to_v2(){
        final String cur_db_version = DB_VERSION_S;

        boolean result;

        Log.println("Старт обновления БД до версии "+cur_db_version+" ...");

        // Создается новая колонка {comment} в таблице {marks}.
        try {
            conn.createStatement().executeUpdate("ALTER TABLE "+GENERAL_SCHEMA_NAME+"."+TABLE_MARKS+" ADD "+TABLE_MARKS_COLUMNS.COLUMN_COMMENT+" varchar(255) AFTER "+TABLE_MARKS_COLUMNS.COLUMN_REQUEST+";");
            result = true;
        } catch (SQLException e) {
            e.printStackTrace();
            // Если такая колонка уже существует, значит это мы ошибочно пытались применить это обновление.
            // Обновляем версию БД до v2. Ошибка не генерируется.
            if (e.getErrorCode() == 1060){
                Log.println("Предупреждение: колонка {comment} уже существует.");
                result = true;
            } else {
                Log.println("Обновление прошло с ошибками! Подробнее:");
                Log.println(e.getLocalizedMessage());
                result =  false;
            }
        }

        if (result){
            // Завершающий этап - обновление переменной {sw_version} в табилце системных переменных.
            setSysVariable(TABLE_VARIABLES_ROWS.SYS_VAR_DB_VERSION, DB_VERSION_I);
            Log.println("Обновление БД до версии "+cur_db_version+" завершено.");
        }
        return result;
    }
}
