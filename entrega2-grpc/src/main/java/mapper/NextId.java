package mapper;

public class NextId {

    private Integer id;

    public NextId() {
    }

    public NextId(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "NextId{" +
                "id=" + id +
                '}';
    }
}
