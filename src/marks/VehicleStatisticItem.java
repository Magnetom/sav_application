package marks;

public class VehicleStatisticItem {

    private int     recordId;
    private String  vehicle;
    private Integer loopsCnt;
    private Integer popularity;
    private Boolean blocked;
    private Boolean deleted;
    private Boolean filtered;
    private int capacity;

    public VehicleStatisticItem(int recordId, String vehicle, Integer loops, Boolean blocked, Boolean deleted, Integer popularity, int capacity, Boolean filtered){
        this.recordId = recordId;
        this.popularity = popularity;
        this.vehicle    = vehicle;
        this.loopsCnt   = loops;
        this.blocked    = blocked;
        this.deleted    = deleted;
        this.filtered   = filtered;
        this.capacity   = capacity;
    }

    public void setFiltered(Boolean filtered) {this.filtered = filtered; }

    public Boolean isFiltered() {return filtered;}

    public void setVehicle(String vehicle) {
        this.vehicle = vehicle;
    }

    public void setLoopsCnt(Integer loopsCnt) {
        this.loopsCnt = loopsCnt;
    }

    public Boolean isBlocked() { return blocked; }
    public void setBlocked(Boolean blocked) {
        this.blocked = blocked;
    }

    public String getVehicle() {return vehicle;}

    public Integer getLoopsCnt() {
        return loopsCnt;
    }

    public Integer getPopularity() {return popularity; }
    public void    setPopularity(Integer popularity) { this.popularity = popularity; }

    public Boolean isDeleted() { return deleted; }
    public void    setDeleted(Boolean deleted) { this.deleted = deleted; }

    public int  getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public int getId() {return recordId; }
    public void setRecordId(int recordId) { this.recordId = recordId; }

}
