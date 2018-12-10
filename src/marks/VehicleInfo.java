package marks;

public class VehicleInfo {
    private String vehicle;
    private Integer loopsCnt;
    private Boolean blocked;
    private Boolean filtered;

    public void setFiltered(Boolean filtered) {this.filtered = filtered; }

    public Boolean isFiltered() {return filtered;}

    public VehicleInfo(String v, Integer m, Boolean b, Boolean f){ vehicle = v; loopsCnt = m; blocked = b; filtered = f;}

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
