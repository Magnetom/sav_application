package items;

public enum Statuses {

    NORMAL("норма"), BLOCKED("заблокирован");

    String status;
    Statuses(String s){status=s;}

    @Override
    public String toString() {
        return this.status;
    }
}
