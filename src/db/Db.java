package db;

import bebug.Log;
import com.sun.istack.internal.Nullable;
import marks.VehicleItem;
import marks.VehicleMark;

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
        // Подключаемся к БД сразу после создания экземпляра класса.
        connect();
     }

    // Подключиться к предопределенной базе данных.
    public boolean connect(){
        // Если соединение уже было установленно ранее, закрываем старое соединение.
        if (conn != null) disconnect();
        try
        {
            // Создается экземпляр класса драйвера длс яработы с MySQL сервером через JDBC-драйвер.
            Class.forName ("com.mysql.cj.jdbc.Driver").newInstance ();

            // Данные для утсановки связи с MySQL сервером.
            String serverName = "localhost";
            String dbName     = "sav";
            String userName   = "root";
            String password   = "";

            String url = "jdbc:MySQL://"+serverName+"/"+dbName;
            conn = DriverManager.getConnection (url, userName, password);
            Log.println("Соединение с базой данных установлено.");
        }
        catch (Exception ex)
        {
            conn = null;
            Log.println("Невозможно установить соединение с сервером бызы данных. Ошибка:");
            Log.println(ex.getLocalizedMessage());
            ex.printStackTrace();
            return false;
        }

        return true;
    }

    public Boolean isConnected() {return conn!=null;}

    // Отключиться от базы данных.
    private void disconnect(){
        if (conn != null) {
            try {
                conn.close ();
                conn = null;
                Log.println("Связь с базой данных разорвана.");
            }
            catch (Exception ex) {
                Log.println("Отключение от базы данных произошло с ошибкой:");
                Log.println(ex.getLocalizedMessage());
            }
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        disconnect();
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
        ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM marks WHERE DATE(time)=DATE(NOW());");

        List<VehicleMark> marksList = new ArrayList<>();
        while (rs.next()) {
            String timestamp = getHHMMFromStringTimestamp(rs.getString("time"));
            String vehicle   = rs.getString("vehicle_id");

            VehicleMark mark = new VehicleMark(timestamp, vehicle);
            marksList.add(mark);
        }
        return marksList;
    }

    // Получить полный список всех зарегистрированных ТС.
    public List<VehicleItem> getAllVehicles() throws  SQLException{
        if(!isConnected()) return null;

        ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM vehicles");

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

        ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM vehicles WHERE blocked='1'");

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
        List<String> blackList = getBlockedVehicles();

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
            return null;
        }
    }

    /* Возвращает TRUE если текущий набор даных на сервере изменился с момента последней выборки. */
    public Boolean isDatasetModifed() throws SQLException {
        if(!isConnected()) return false;
        ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM variables WHERE name='dataset';");
        if (rs.next()){
            String value = rs.getString("value");
            if (currDatasetHash == null || !currDatasetHash.equalsIgnoreCase(value)){
                currDatasetHash = value;
                return true;
            }
            return false;
        }
        return true;
    }

}
