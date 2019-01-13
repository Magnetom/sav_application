package marks;

public class VehicleMark {
    private String vehicle;
    private String timestamp;
    private String requestId;
    private int    recordId;

    public int getRecordId() {return recordId;}

    public void setRecordId(int recordId) {this.recordId = recordId;}

    public String getRequestId() {return requestId;}
    public void setRequestId(String requestId) {this.requestId = requestId;}

    public VehicleMark(String t, String v, String request, int record){ timestamp = t; vehicle = v; requestId = request; recordId = record;}

    public String getVehicle(){return vehicle;}
    public void setVehicle(String v) {vehicle=v;}

    public String getTimestamp(){return timestamp;}
    public void setTimestamp(String t) {timestamp=t;}
}
