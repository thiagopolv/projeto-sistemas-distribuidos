package domain;

import java.util.Map;

public class Log {

    private LogFunctions function;
    private Object data;

    public Log() {
    }

    public Log(LogFunctions function, Object data) {
        this.function = function;
        this.data = data;
    }

    public LogFunctions getFunction() {
        return function;
    }

    public void setFunction(LogFunctions function) {
        this.function = function;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
