package db;

import utils.DateTime;

import java.time.LocalDate;

public class DbDateRange {

    private LocalDate startDate;
    private LocalDate stopDate;
    private boolean allDatesFlag; // Флаг, указывающий что даиапазон дат как таковой отсутствует и требуется все возможные даты.

    public DbDateRange(){}
    public DbDateRange(boolean allDatesFlag){this.allDatesFlag=allDatesFlag;}

    private LocalDate convertDate(String date){
        return DateTime.getDbDateConverter().fromString(date);
    }

    public void setStartDate (Object date){
        if (date instanceof String)     startDate = convertDate((String) date);
        if (date instanceof LocalDate)  startDate = (LocalDate) date;
    }

    public void setStopDate (Object date){
        if (date instanceof String)     stopDate = convertDate((String) date);
        if (date instanceof LocalDate)  stopDate = (LocalDate) date;
    }

    public void setAllDatesFlag (){allDatesFlag = true;}
    public boolean getAllDatesFlag (){return allDatesFlag;}

    public String getStartDateFormatted() { return DateTime.getDbDateConverter().toString(startDate); }

    public String getStopDateFormatted() {  return DateTime.getDbDateConverter().toString(stopDate); }

    public LocalDate getStartDate() { return startDate; }

    public LocalDate getStopDate()  { return stopDate;  }

    public boolean isSingleDate(){
        return (startDate == null) || (stopDate == null) || (startDate.compareTo(stopDate) == 0);
    }

    public String getSingleDate(){
        if (isSingleDate()){
            if (startDate != null)  return getStartDateFormatted();
            else
            if (stopDate  != null)  return getStopDateFormatted();
            else
                return "";
        }else
            return "";
    }
}
