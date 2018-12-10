package db;

import bebug.Log;
import bebug.LogInterface;
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

    public void setVehicleState(String vehicle, boolean blocked) throws SQLException{
        if (vehicle == null || (vehicle.equals("")) ) return;
        int st = (blocked)?1:0;
        statement.executeQuery("UPDATE vehicles SET blocked='"+st+"' WHERE vehicle='"+vehicle+"';");
    }


    public void setVehicleBlocked(String vehicle) throws SQLException{
        setVehicleState(vehicle, true);
    }

    public void setVehicleUnblocked(String vehicle) throws SQLException{
        setVehicleState(vehicle, false);
    }

    /* Возвращает список отметок за текущий день. */
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
