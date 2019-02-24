package frames;

public class VehicleCapacityItem {
    private int     id;
    private String  type;
    private int     capacity;
    private int     cost;
    private String  comment;

    public VehicleCapacityItem() {}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    VehicleCapacityItem(int id, String type, int capacity, int cost, String comment) {
        this.id         = id;
        this.type       = type;
        this.capacity   = capacity;
        this.cost       = cost;
        this.comment    = comment;
    }

    public String getType() {
        return type;
    }
    public void   setType(String type) {
        this.type = type;
    }

    public int  getCapacity() {
        return capacity;
    }
    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public String getComment() {
        return comment;
    }
    public void   setComment(String comment) {
        this.comment = comment;
    }

    public int  getCost() { return cost; }
    public void setCost(int cost) { this.cost = cost; }

    @Override
    public String toString() {
        return this.type;
    }
}
