package broadcast;

public interface OnDbConnectionChanged {
    void onConnect();
    void onDisconnect(boolean failFlag);
}
