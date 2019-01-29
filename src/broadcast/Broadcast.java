package broadcast;

public class Broadcast {

    private static SettingsChanged      settingsInterface;
    private static DatasetManualChanged datasetInterface;
    private static AccountChanged       accountInterface;
    private static DbReconectionRequest dbReconectionRequest;


    public static SettingsChanged getSettingsInterface() {
        return settingsInterface;
    }


    public static void setDatasetInterface          (DatasetManualChanged datasetInterface)     { Broadcast.datasetInterface = datasetInterface; }
    public static void setSettingsChangedInterface  (SettingsChanged i)                         { settingsInterface = i; }
    public static void setAccountInterface          (AccountChanged accountInterface)           { Broadcast.accountInterface = accountInterface; }
    public static void setDbReconnectionRequest     (DbReconectionRequest dbReconectionRequest) { Broadcast.dbReconectionRequest = dbReconectionRequest;}

    public static SettingsChanged       getSettingsChangedInterface()   { return settingsInterface; }
    public static DatasetManualChanged  getDatasetInterface()           { return datasetInterface; }
    public static AccountChanged        getAccountInterface()           { return accountInterface; }
    public static DbReconectionRequest  getDbReconectionRequest()       { return dbReconectionRequest; }

    public static void SettingsChangedNotification(){
        if (settingsInterface != null) settingsInterface.wasChanged();
    }

    public static void DatasetManualChangedNotification(){ if (datasetInterface != null) datasetInterface.wasChanged(); }
}
