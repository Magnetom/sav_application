package db;

import bebug.Log;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import marks.VehicleItem;
import marks.VehicleMark;
import utils.Utils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static utils.DateTime.getHHMMFromStringTimestamp;

public class Db {

    public static String TAG_SQL = "SQL";
    public static String TAG_DB  = "DB";

    private Connection conn = null;

    private static Db instance = null;

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
        connect();
     }

    // Подключиться к предопределенной базе данных.
    private boolean connect(){
        // Если соединение уже было установленно ранее, закрываем старое соединение.
        if (conn != null) disconnect();
        try
        {
            ///////////////////////////////////////////////
            // Пытаемся проверить метаданны базы данных {sav}. Метеданные не верный и не могут быть созданы - считаем,
            // что дальнейшая работа с сервером БД невозможна.
            if (!checkMetadata()) return false;
            ///////////////////////////////////////////////

            // Данные для утсановки связи с MySQL сервером.
            String serverName = "localhost";
            String dbName     = "sav";
            //String userName   = "user";
            //String password   = "mysqluser";
            String userName   = "admin";
            String password   = "mysqladmin";
            String url = "jdbc:MySQL://"+serverName+"/"+dbName;
            conn = DriverManager.getConnection (url, userName, password);
            Log.println("Соединение с базой данных установлено.");
        }
        catch (Exception ex)
        {
            conn = null;
            Log.printerror(TAG_SQL, "CONNECT","Невозможно установить соединение с сервером бызы данных!", ex.getLocalizedMessage());
            ex.printStackTrace();
            return false;
        }
        return true;
    }

    public Boolean isConnected() {return conn!=null;}

    public Boolean reConnect(){
        Log.println("Попытка заново подключиться к базе данных.");
        return connect();
    }

    // Отключиться от базы данных.
    private void disconnect(){
        if (conn != null) {
            try {
                conn.close ();
                conn = null;
                Log.println("Связь с базой данных разорвана.");
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

    // Очистить БД полность.
    public Boolean allDbRemove(){
        Boolean result;
        // Удаляются все ТС.
        result = removeAllVehicles();
        // Удаляются все отметки.
        result &= removeMarks(null,"",null);
        // Удаляются все отметки.
        result &= removeAllVariables();
        return result;
    }

    // Удаляются все переменные.
    private Boolean removeAllVariables() {
        String query = "DELETE FROM variables;";
        try {
            conn.createStatement().executeUpdate(query);
        } catch (SQLException e) {
            e.printStackTrace();
            reConnect(); // Пытаемся переконнектиться.
            Log.printerror(TAG_SQL, "REMOVE_ALL_VARIABLES",e.getMessage(), query);
            return false;
        }
        return true;
    }

    // Очистить список ТС.
    public Boolean removeAllVehicles() {
        String query = "DELETE FROM vehicles;";
        try {
            conn.createStatement().executeUpdate(query);
        } catch (SQLException e) {
            e.printStackTrace();
            reConnect(); // Пытаемся переконнектиться.
            Log.printerror(TAG_SQL, "REMOVE_ALL_VEHICLES",e.getMessage(), query);
            return false;
        }
        return true;
    }

    // Очистить все рейтинги популярности.
    public Boolean resetPopularity(){
        if(!isConnected()) return false;
        String query = "UPDATE vehicles SET popularity=0;";
        try {
            conn.createStatement().executeUpdate(query);
        } catch (SQLException e) {
            e.printStackTrace();
            reConnect(); // Пытаемся переконнектиться.
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
    public Boolean addMark(@NotNull  String vehicle,
                           @Nullable String timestamp){

        if (vehicle==null || vehicle.isEmpty()) return false;

        String request_id = Utils.genStrongUidString(20);
        String writer_id = "application";
        String query;

        if (timestamp == null || timestamp.isEmpty())
            query = "INSERT INTO marks (vehicle_id,mac,request_id) " +
                    "VALUES ('"+vehicle+"','"+writer_id+"','"+request_id+"');";
        else
            query = "INSERT INTO marks (vehicle_id,mac,request_id,time) " +
                    "VALUES ('"+vehicle+"','"+writer_id+"','"+request_id+"','"+timestamp+"');";

        try {
            conn.createStatement().executeUpdate(query);
        } catch (SQLException e) {
            e.printStackTrace();
            Log.printerror(TAG_SQL, "ADD_MARK",e.getMessage(), query);
            reConnect(); // Пытаемся переконнектиться.
            return false;
        }

        return true;
    }

    /* Brief: Удалить все отметки за все сегодняшнюю дату (частичная очистка БД).
     *
     * Если указан параметр @recordId, все остальные условия обрасываются.
     * @recordId - уникальный идентификатор записи (строки).
     *
     * Если в качестве параметра @date указан NULL, то удалить только за текущую дату.
     * Если @date - пустая строка, то удалить все данные.
     *
     * Если в качестве параметра @vehicle указан NULL или пустая строка, то выборка будет касаться всех ТС.
     * Если @vehicle - не пустая строка, то выборка будет касаться только указанного ТС.
    */
    public Boolean removeMarks(@Nullable Integer recordId,
                               @Nullable String date,
                               @Nullable String vehicle){
        String condition = "";
        if (recordId != null) condition = " WHERE id='"+recordId.toString()+"'";
        else
        if (date == null) condition = "WHERE DATE(time)=DATE(NOW())";
        else
        if (!date.equals("")) condition = "WHERE DATE(time)=DATE('"+date+"')";

        // Дополнительное условие - выборка только для конкретного ТС.
        if (vehicle!=null && !vehicle.isEmpty() && recordId == null) condition += " AND vehicle_id='"+vehicle+"'";
        
        
        String query = "DELETE FROM marks "+ condition + ";";
        try {
            conn.createStatement().executeUpdate(query);
        } catch (SQLException e) {
            e.printStackTrace();
            reConnect(); // Пытаемся переконнектиться.
            Log.printerror(TAG_SQL, "REMOVE_MARKS",e.getMessage(), query);
            return false;
        }
        return true;
    }

    // Установить статус ТС: TRUE - заблокировано, FALSE - разблокировано/норма.
    public Boolean setVehicleState(String vehicle, boolean blocked){
        if (vehicle == null || (vehicle.equals("")) ) return false;
        if(!isConnected()) return false;
        Integer st = (blocked)?1:0;
        String query = "UPDATE vehicles SET blocked="+st+" WHERE vehicle='"+vehicle+"';";
        try {
            conn.createStatement().executeUpdate(query);
        } catch (SQLException e) {
            e.printStackTrace();
            reConnect(); // Пытаемся переконнектиться.
            Log.printerror(TAG_SQL, "SET_VEHICLE_STATE",e.getMessage(), query);
            return false;
        }
        return true;
    }

    // Заблокировать ТС.
    public Boolean setVehicleBlocked(String vehicle) throws SQLException{
        return setVehicleState(vehicle, true);
    }

    // Разблокировать ТС.
    public Boolean setVehicleUnblocked(String vehicle) throws SQLException{
        return setVehicleState(vehicle, false);
    }

    // Возвращает список/лог отметок за текущий день.
    public List<VehicleMark> getMarksRawList() throws SQLException {
        if(!isConnected()) return null;

        ResultSet rs;
        try {
            rs = conn.createStatement().executeQuery("SELECT * FROM marks WHERE DATE(time)=DATE(NOW());");
        } catch (SQLException e){
            reConnect(); // Пытаемся переконнектиться.
            throw e;
        }

        List<VehicleMark> marksList = new ArrayList<>();
        while (rs.next()) {
            String timestamp = getHHMMFromStringTimestamp(rs.getString("time"));
            String vehicle   = rs.getString("vehicle_id");
            String requestId = rs.getString("request_id");
            int    recordId  = rs.getInt("id");

            VehicleMark mark = new VehicleMark(timestamp, vehicle, requestId, recordId);
            marksList.add(mark);
        }
        return marksList;
    }

    // Получить полный список всех зарегистрированных ТС.
    public List<VehicleItem> getAllVehicles() throws  SQLException{
        if(!isConnected()) return null;

        ResultSet rs;
        try {
            rs = conn.createStatement().executeQuery("SELECT * FROM vehicles");
        } catch (SQLException e){
            reConnect(); // Пытаемся переконнектиться.
            throw e;
        }

        List<VehicleItem> vehiclesList = new ArrayList<>();
        while (rs.next()) {
            vehiclesList.add(new VehicleItem(
                    rs.getString("vehicle"),
                    0,
                    rs.getBoolean("blocked"),
                    rs.getInt("popularity"),
                    false));
        }
        return vehiclesList;
    }


    // Получить список заблокированных ТС. Далее, если искомого ТС нет в списке, то считать его НЕ заблокированным.
    public List<String> getBlockedVehicles() throws  SQLException{
        if(!isConnected()) return null;

        ResultSet rs;
        try {
            rs = conn.createStatement().executeQuery("SELECT * FROM vehicles WHERE blocked='1'");
        } catch (SQLException e){
            reConnect(); // Пытаемся переконнектиться.
            throw e;
        }


        List<String> blackList = new ArrayList<>();
        while (rs.next()) {
            String vehicle = rs.getString("vehicle");
            blackList.add(vehicle);
        }
        return blackList;
    }

    // Получить статистику по каждому ТС: [госномер]-[количество кругов]-[статус блокировки].
    // Может работать с уже имеющимся списком отметок за текущий день. Если список не передан (NULL), тогда
    // список будет сформирован с помощью соответствующего запроса в БД.
    public List<VehicleItem> getVehiclesStatistic(@Nullable List<VehicleMark> markList) throws SQLException{

        if(!isConnected()) return null;
        // Получаем (если это необходимо) лог отметок за текущий день.
        if (markList == null) markList = getMarksRawList();

        // Получаем список заблокированных ТС.
        List<String> blackList;
        try {
            blackList = getBlockedVehicles();
        } catch (SQLException e){
            reConnect(); // Пытаемся переконнектиться.
            throw e;
        }

        // Создаем экземпляр класса списка статистики.
        List<VehicleItem> list = new ArrayList<>();

        //////////////////////////////////////////////////////////////////////////////////////
        boolean exist;

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
                for (String blackItem : blackList) {
                    // Если ТС есть в списке заблокированных - делаем отметку об этом.
                    if (mark.getVehicle().equalsIgnoreCase(blackItem)) isBlocked = true;
                }
                // Добавляем новое ТС в новый список.
                list.add( new VehicleItem(mark.getVehicle(), 1, isBlocked, 0,false) );
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
        return setSysVariable("global_blocked", val);
    }

    /* Устнавливает в БД в таблице variables значение value для переменной name. */
    public Boolean setSysVariable(String name, Object value){

        if (name == null || (name.equals("")) ) return false;
        if(!isConnected()) return false;

        String value_final;

        if (value instanceof Integer) value_final = value.toString();
        else
            value_final = "'"+value.toString()+"'";

        String query = "INSERT INTO variables (name,value) VALUES ('"+name+"',"+value_final+") ON DUPLICATE KEY UPDATE value="+value_final+";";
        try {
            conn.createStatement().executeUpdate(query);
        } catch (SQLException e) {
            e.printStackTrace();
            Log.printerror(TAG_SQL, "SET_SYS_VARIABLE",e.getMessage(), query);
            reConnect(); // Пытаемся переконнектиться.
            return false;
        }
        return true;
    }


    /* Возвращает значение системной переменной из БД. */
    public Object getSysVariable(String name){
        if(!isConnected()) return null;
        String query = "SELECT value FROM variables WHERE name='"+name+"';";
        try {
            ResultSet rs = conn.createStatement().executeQuery(query);

            if (rs.next()){
                return rs.getObject("value");
            } else {
                return null;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            Log.printerror(TAG_SQL, "GET_SYS_VARIABLE",e.getMessage(), query);
            reConnect(); // Пытаемся переконнектиться.
            return null;
        }
    }

    /* Возвращает TRUE если текущий набор даных на сервере изменился с момента последней выборки. */
    public Boolean isDatasetModified() {
        if(!isConnected()) return false;
        ResultSet rs;

        try {
            rs = conn.createStatement().executeQuery("SELECT * FROM variables WHERE name='dataset';");
            if (rs.next()){
                String value = rs.getString("value");

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
            reConnect(); // Пытаемся переконнектиться.
            return false;
        }
        return false;
    }

    /* Прверка целостности БД. Создание БД и ее таблиц при необходимости. */
    public Boolean checkMetadata(){
        Log.println("Проверка целостности базы данных.");

        try {
            dbCreate(false);
        } catch (Exception e) {
            e.printStackTrace();
            Log.println("Проверка целостности базы данных завершилась с ошибками! Подробнее:");
            Log.println(e.getLocalizedMessage());
            return false;
        }
        Log.println("Проверка целостности базы данных завершена.");
        return true;
    }

    /* Создамне базы данных и всех ее таблиц. */
    // Если параметр remove = TRUE - удалить существующую БД перед сзданием новой.
    public void dbCreate(boolean remove) throws Exception{
        int result = 0;

        // Данные для установки связи с MySQL сервером.
        Connection cn = DriverManager.getConnection ("jdbc:MySQL://localhost", "admin", "mysqladmin");
        Log.println("Соединение с MySQL-сервером установлено.");

        if (remove) {
            Log.println("Удаление базы данных {sav}.");
            result = cn.createStatement().executeUpdate("DROP DATABASE IF EXISTS sav;");
        }

        // Создать базу данных {sav}, если отсутствует.
        Log.println("Проверка существования/создание базы данных {sav}.");
        result = cn.createStatement().executeUpdate("CREATE DATABASE IF NOT EXISTS sav;");

        // Создать таблицу {marks}, если отсутствует.
        Log.println("Проверка существования/создание табицы {marks}.");
        result = cn.createStatement().executeUpdate("create table if not exists sav.marks\n" +
                "(\n" +
                "  id         int(11) unsigned auto_increment\n" +
                "    primary key,\n" +
                "  vehicle_id varchar(14)                         not null,\n" +
                "  time       timestamp default CURRENT_TIMESTAMP not null,\n" +
                "  mac        varchar(18)                         null,\n" +
                "  request_id varchar(20)                         not null,\n" +
                "  constraint request_id\n" +
                "  unique (request_id)\n" +
                ");");

        // Создать таблицу {variables}, если отсутствует.
        Log.println("Проверка существования/создание табицы {variables}.");
        result = cn.createStatement().executeUpdate("create table if not exists sav.variables\n" +
                "(\n" +
                "  id    int unsigned auto_increment\n" +
                "    primary key,\n" +
                "  name  varchar(50) default 'unknown' not null,\n" +
                "  value varchar(50) default 'unknown' not null,\n" +
                "  constraint variable_unique\n" +
                "  unique (name)\n" +
                ");");

        // Создать таблицу {vehicles}, если отсутствует.
        Log.println("Проверка существования/создание табицы {vehicles}.");
        result = cn.createStatement().executeUpdate("create table if not exists sav.vehicles\n" +
                "(\n" +
                "  id         int(11) unsigned auto_increment\n" +
                "    primary key,\n" +
                "  vehicle    varchar(18)                  not null,\n" +
                "  popularity int(11) unsigned default '0' not null,\n" +
                "  blocked    bit default b'0'             not null,\n" +
                "  constraint vehicle_id\n" +
                "  unique (vehicle)\n" +
                ");");
        // Закрываем соединение.
        cn.close();
    }
}
