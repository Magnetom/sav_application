package items;

public class VehicleItem {

    private int     recordId;
    private String  vehicle;
    private Integer popularity;
    private Boolean blocked;
    private Boolean deleted;
    private int capacity;

    public VehicleItem(int recordId, String vehicle, Boolean blocked, Boolean deleted, Integer popularity, int capacity){
        this.recordId = recordId;
        this.popularity = popularity;
        this.vehicle    = vehicle;
        this.blocked    = blocked;
        this.deleted    = deleted;
        this.capacity   = capacity;
    }

    public void setVehicle(String vehicle) {
        this.vehicle = vehicle;
    }

    public Boolean isBlocked() { return blocked; }
    public void setBlocked(Boolean blocked) {
        this.blocked = blocked;
    }

    public String getVehicle() {return vehicle;}


    public Boolean isDeleted() { return deleted; }
    public void    setDeleted(Boolean deleted) { this.deleted = deleted; }

    public int  getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public int getId() {return recordId; }
    public void setRecordId(int recordId) { this.recordId = recordId; }

    public Integer getPopularity() { return popularity; }
    public void setPopularity(Integer popularity) { this.popularity = popularity; }
}
