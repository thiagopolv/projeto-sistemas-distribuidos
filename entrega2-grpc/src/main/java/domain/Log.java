package domain;

public class Log {

    private LogFunctions function;
    private LogData logData;

    public Log() {
    }

    public Log(LogFunctions function, LogData logData) {
        this.function = function;
        this.logData = logData;
    }

    public LogFunctions getFunction() {
        return function;
    }

    public void setFunction(LogFunctions function) {
        this.function = function;
    }

    public LogData getLogData() {
        return logData;
    }

    public void setLogData(LogData logData) {
        this.logData = logData;
    }
}
