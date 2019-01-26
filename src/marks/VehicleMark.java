package marks;

public class VehicleMark {
    private String vehicle;
    private String timestamp;
    private String requestId;
    private String comment;
    private int    recordId;

    public VehicleMark(String timestamp,
                       String vehicle,
                       String request,
                       int record,
                       String comment){
        this.timestamp  = timestamp;
        this.vehicle    = vehicle;
        this.requestId  = request;
        this.recordId   = record;
        this.comment    = comment;
    }

    public String getComment() {return comment;}
    public void  setComment(String comment) {this.comment = comment;}

    public int  getRecordId() {return recordId;}
    public void setRecordId(int recordId) {this.recordId = recordId;}

    public String getRequestId() {return requestId;}
    public void   setRequestId(String requestId) {this.requestId = requestId;}


    public String getVehicle(){return vehicle;}
    public void   setVehicle(String v) {vehicle=v;}

    public String getTimestamp(){return timestamp;}
    public void   setTimestamp(String t) {timestamp=t;}
}
