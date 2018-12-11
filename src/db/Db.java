package db;

import bebug.Log;
import bebug.LogInterface;
import com.sun.istack.internal.Nullable;
import marks.VehicleInfo;
import marks.VehicleMark;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static utils.DateTime.getHHMMFromStringTimestamp;

public class Db {

    private static Connection conn = null;
    private static Statement statement = null;

    private static String currDataset = null;

    public Db(){
        // Подключаемся к БД сразу после создания экземпляра класса.
        if (conn == null) connect();
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
            statement = conn.createStatement();
            Log.println("Соединение с базой данных установлено.");
        }
        catch (Exception ex)
        {
            conn = null;
            statement = null;
            Log.println("Невозможно установить соединение с сервером бызы данных. Ошибка:");
            Log.println(ex.getLocalizedMessage());
            ex.printStackTrace();
            return false;
        }

        return true;
    }

    // Отключиться от базы данных.
    private void disconnect(){
        if (conn != null) {
            try {
                conn.close ();
                conn = null;
                statement = null;
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

    // Естановить статус ТС: TRUE - заблокировано, FALSE - разблокировано/норма.
    public void setVehicleState(String vehicle, boolean blocked) throws SQLException{
        if (vehicle == null || (vehicle.equals("")) ) return;
        int st = (blocked)?1:0;
        statement.executeQuery("UPDATE vehicles SET blocked='"+st+"' WHERE vehicle='"+vehicle+"';");
    }

    // Заблокировать ТС.
    public void setVehicleBlocked(String vehicle) throws SQLException{
        setVehicleState(vehicle, true);
    }

    // Разблокировать ТС.
    public void setVehicleUnblocked(String vehicle) throws SQLException{
        setVehicleState(vehicle, false);
    }

    /* Возвращает список/лог отметок за текущий день. */
    public List<VehicleMark> getMarksRawList() throws SQLException {

        ResultSet rs = statement.executeQuery("SELECT * FROM marks WHERE DATE(time)=DATE(NOW());");

        List<VehicleMark> marksList = new ArrayList<>();
        while (rs.next()) {
            String timestamp = getHHMMFromStringTimestamp(rs.getString("time"));
            String vehicle   = rs.getString("vehicle_id");

            VehicleMark mark = new VehicleMark(timestamp, vehicle);
            marksList.add(mark);
        }
        return marksList;
    }

    // Получить список заблокированных ТС. Далее, если искомого ТС нет в списке, то считать его НЕ заблокированным.
    public List<String> getBlockedVehicles() throws  SQLException{
        ResultSet rs = statement.executeQuery("SELECT * FROM vehicles WHERE blocked='1'");

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
    public List<VehicleInfo> getVehiclesStatistic(@Nullable List<VehicleMark> markList) throws SQLException{

        // Получаем (если это необходимо) лог отметок за текущий день.
        if (markList == null) markList = getMarksRawList();

        // Получаем список заблокированных ТС.
        List<String> blackList = getBlockedVehicles();

        // Создаем экземпляр класса списка статистики.
        List<VehicleInfo> list = new ArrayList<>();

        //////////////////////////////////////////////////////////////////////////////////////
        boolean exist;

        for (VehicleMark mark: markList) {
            exist = false;
            for (VehicleInfo item: list) {
                // если ТС уже есть в итоговом списке, увеличиваем счетчик кругов и переходим к следующей итерации.
                if (item.getVehicle().equalsIgnoreCase(mark.getVehicle())){
                    item.setLoopsCnt(item.getLoopsCnt()+1);
                    exist = true;
                    break;
                }
            }
            // Если мы попали сюда, занчит этого ТС еще нет в списке. Добавляем.
            if (!exist) list.add( new VehicleInfo(mark.getVehicle(), 1, false, false) );
        }
        //////////////////////////////////////////////////////////////////////////////////////

        return list;
    }

    /* Возвращает TRUE если текущий набор даных на сервере изменился с момента последней выборки. */
    public Boolean isDatasetModifed() throws SQLException {

        ResultSet rs = statement.executeQuery("SELECT * FROM variables WHERE name='dataset';");
        if (rs.next()){
            String value = rs.getString("value");
            if (currDataset == null || !currDataset.equalsIgnoreCase(value)){
                currDataset = value;
                return true;
            }
            return false;
        }
        return true;
    }

}
