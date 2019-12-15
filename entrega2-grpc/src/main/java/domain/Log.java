package domain;

public class Log {

    private LogFunction function;
    private LogData logData;

    public Log() {
    }

    public Log(LogFunction function, LogData logData) {
        this.function = function;
        this.logData = logData;
    }

    public LogFunction getFunction() {
        return function;
    }

    public void setFunction(LogFunction function) {
        this.function = function;
    }

    public LogData getLogData() {
        return logData;
    }

    public void setLogData(LogData logData) {
        this.logData = logData;
    }
}
