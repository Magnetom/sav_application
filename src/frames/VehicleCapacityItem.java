package frames;

public class VehicleCapacityItem {
    int id;
    String type;
    int capacity;
    String comment;

    public VehicleCapacityItem() {}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public VehicleCapacityItem(int id, String type, int capacity, String comment) {
        this.id = id;
        this.type = type;
        this.capacity = capacity;
        this.comment = comment;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
