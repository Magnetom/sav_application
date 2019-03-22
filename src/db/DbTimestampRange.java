package db;

import utils.time.DateTime;

import java.time.LocalDateTime;

public class DbTimestampRange {

    private LocalDateTime startTimestamp;
    private LocalDateTime stopTimestamp;

    // Флаг, указывающий на то, что даиапазон дат/времен как таковой отсутствует и требуется все возможные даты/времена.
    private boolean allTimestampsFlag;

    public DbTimestampRange(){}
    public DbTimestampRange(boolean allDatesFlag){this.allTimestampsFlag = allDatesFlag;}

    private LocalDateTime convertTimestamp(String timestamp){
        return DateTime.getDbTimestampConverter().fromString(timestamp);
    }

    public void setStartTimestamp (Object startTimestamp){
        if (startTimestamp instanceof String)         this.startTimestamp = convertTimestamp((String) startTimestamp);
        if (startTimestamp instanceof LocalDateTime)  this.startTimestamp = (LocalDateTime) startTimestamp;
    }

    public void setStopTimestamp (Object stopTimestamp){
        if (stopTimestamp instanceof String)         this.stopTimestamp = convertTimestamp((String) stopTimestamp);
        if (stopTimestamp instanceof LocalDateTime)  this.stopTimestamp = (LocalDateTime) stopTimestamp;
    }

    public void     setAllTimestampsFlag(){ allTimestampsFlag = true; }
    public boolean  getAllTimestampsFlag(){ return allTimestampsFlag; }

    public String getStartTimestampFormatted() { return DateTime.getDbTimestampConverter().toString(startTimestamp); }

    public String getStopTimestampFormatted()  { return DateTime.getDbTimestampConverter().toString(stopTimestamp);  }

    public LocalDateTime getStartTimestamp() { return startTimestamp; }

    public LocalDateTime getStopTimestamp()  { return stopTimestamp;  }

    public boolean isSingleTimestamp(){
        return (startTimestamp == null) || (stopTimestamp == null) || (startTimestamp.compareTo(stopTimestamp) == 0);
    }

    public String getSingleTimestamp(){
        if (isSingleTimestamp()){
            if (startTimestamp != null)  return getStartTimestampFormatted();
            else
            if (stopTimestamp  != null)  return getStopTimestampFormatted();
            else
                return "";
        }else
            return "";
    }
}
