package bebug;

public final class Log {

    private static LogInterface logInterface;

    public static void setInterface(LogInterface i){
        logInterface = i;
    }

    public static LogInterface getInterface(){
        return logInterface;
    }

    public static void print (String mess){
        if (logInterface != null) logInterface.print(mess);
    }

    public static void println (String mess){
        if (logInterface != null) logInterface.println(mess);
    }

    public static void printerror (String tag, String block, String mess, String extra){
        if (logInterface != null) {
            logInterface.println("Error in ["+tag+"]["+block+"]:");
            logInterface.println(mess);
            if (extra != null && !extra.isEmpty()) {
                logInterface.println("Extra:");
                logInterface.println(extra);
            }
        }
    }
}
