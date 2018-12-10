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
}
