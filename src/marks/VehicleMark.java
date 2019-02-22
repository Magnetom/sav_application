package marks;

public class VehicleMark {

    private int     recordId;
    private String  vehicle;
    private String  timestamp;
    private String  request;
    private boolean deleted;


    private boolean vehicleDeleted;
    private String  comment;

    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }

    public VehicleMark(String timestamp,
                       String vehicle,
                       String request,
                       boolean deleted,
                       int record,
                       String comment){

        this.timestamp  = timestamp;
        this.vehicle    = vehicle;
        this.request    = request;
        this.deleted    = deleted;
        this.recordId   = record;
        this.comment    = comment;
    }

    public String getComment() {return comment;}
    public void  setComment(String comment) {this.comment = comment;}

    public int  getRecordId() {return recordId;}
    public void setRecordId(int recordId) {this.recordId = recordId;}

    public String getRequest() {return request;}
    public void setRequest(String request) {this.request = request;}


    public String getVehicle(){return vehicle;}
    public void   setVehicle(String v) {vehicle=v;}

    public String getTimestamp(){return timestamp;}
    public void   setTimestamp(String t) {timestamp=t;}

    public boolean isVehicleDeleted() {
        return vehicleDeleted;
    }

    public void setVehicleDeleted(boolean vehicleDeleted) {
        this.vehicleDeleted = vehicleDeleted;
    }
}
