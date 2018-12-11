package marks;

public class VehicleInfo {
    private String  vehicle;
    private Integer loopsCnt;
    private Boolean blocked;
    private Boolean filtered;

    public VehicleInfo(String vehicle, Integer loops, Boolean blocked, Boolean filtered){ this.vehicle = vehicle; loopsCnt = loops; this.blocked = blocked; this.filtered = filtered;}

    public void setFiltered(Boolean filtered) {this.filtered = filtered; }

    public Boolean isFiltered() {return filtered;}

    public void setVehicle(String vehicle) {
        this.vehicle = vehicle;
    }

    public void setLoopsCnt(Integer loopsCnt) {
        this.loopsCnt = loopsCnt;
    }

    public void setBlocked(Boolean blocked) {
        this.blocked = blocked;
    }

    public String getVehicle() {
        return vehicle;
    }

    public Integer getLoopsCnt() {
        return loopsCnt;
    }

    public Boolean isBlocked() {
        return blocked;
    }
}
