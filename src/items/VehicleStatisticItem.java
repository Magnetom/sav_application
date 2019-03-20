package items;

public class VehicleStatisticItem {

    private String  vehicle;
    private Integer loopsCnt;
    private Boolean blocked;
    private Boolean filtered;

    private int capacity;   // Грузовместимость (м.куб.)
    private int cost;       // Стоимость одного м.куб. груза (руб.).

    private int totalVolume; // Перевезенный объем (м.куб.).
    private int totalCost;   // Стоимость перевезенного объема (руб.).

    public VehicleStatisticItem(String vehicle,
                                Integer loops,
                                Boolean blocked,
                                Boolean filtered,
                                VehicleCapacityItem capacityItem){
        this.vehicle    = vehicle;
        this.loopsCnt   = loops;
        this.blocked    = blocked;
        this.filtered   = filtered;

        if (capacityItem != null){
            capacity = capacityItem.getCapacity();
            cost     = capacityItem.getCost();
        }
        updateTotalValues();
    }


    public void setFiltered(Boolean filtered) {this.filtered = filtered; }

    public Boolean isFiltered() {return filtered;}

    public void setVehicle(String vehicle) {
        this.vehicle = vehicle;
    }

    public void setLoopsCnt(Integer loopsCnt) {
        this.loopsCnt = loopsCnt;
        updateTotalValues();
    }

    public void incLoopsCnt() {
        this.loopsCnt++;
        updateTotalValues();
    }

    public void decLoopsCnt(Integer loopsCnt) {
        this.loopsCnt--;
        updateTotalValues();
    }
    private void updateTotalValues(){
        totalVolume = capacity * loopsCnt;
        totalCost   = totalVolume * cost;
    }

    public Boolean isBlocked() { return blocked; }
    public void setBlocked(Boolean blocked) {
        this.blocked = blocked;
    }

    public String getVehicle() {return vehicle;}

    public Integer getLoopsCnt() { return loopsCnt; }


    public int getTotalVolume() {
        return totalVolume;
    }

    public int getTotalCost() {
        return totalCost;
    }
}
