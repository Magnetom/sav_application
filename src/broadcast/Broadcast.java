package broadcast;

public class Broadcast {

    private static SettingsChanged settingsInterface;

    public static DatasetManualChanged getDatasetInterface() { return datasetInterface; }

    public static void setDatasetInterface(DatasetManualChanged datasetInterface) { Broadcast.datasetInterface = datasetInterface; }

    private static DatasetManualChanged datasetInterface;

    public static void setSettingsChangedInterface(SettingsChanged i){
        settingsInterface = i;
    }

    public static SettingsChanged getSettingsChangedInterface(){
        return settingsInterface;
    }

    public static void SettingsChangedNotification(){
        if (settingsInterface != null) settingsInterface.wasChanged();
    }
}
