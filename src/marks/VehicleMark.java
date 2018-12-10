package marks;

public class VehicleMark {
    private String vehicle;
    private String timestamp;

    public VehicleMark(String t, String v){ timestamp = t; vehicle = v;}

    public String getVehicle(){return vehicle;}
    public void setVehicle(String v) {vehicle=v;}

    public String getTimestamp(){return timestamp;}
    public void setTimestamp(String t) {timestamp=t;}
}
