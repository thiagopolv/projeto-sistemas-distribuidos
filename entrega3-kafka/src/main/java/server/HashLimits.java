package server;

import java.util.Objects;

public class HashLimits {
    private String init;
    private String end;

    public HashLimits(String init, String end) {
        this.init = init;
        this.end = end;
    }

    public String getInit() {
        return init;
    }

    public String getEnd() {
        return end;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HashLimits that = (HashLimits) o;
        return Objects.equals(init, that.init) &&
                Objects.equals(end, that.end);
    }

    @Override
    public int hashCode() {
        return Objects.hash(init, end);
    }

    @Override
    public String toString() {
        return "HashLimits{" +
                "init='" + init + '\'' +
                ", end='" + end + '\'' +
                '}';
    }
}
