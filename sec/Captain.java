package sec;

import java.util.ArrayList;
import java.util.List;

public class Captain {

    Integer id;
    ArrayList<String> cops;

    public Captain(Integer id) {
        this.id = id;
        this.cops = new ArrayList<String>();
    }

    public Integer getId() {
        return id;
    }

    public List<String> getCops() {
        return cops;
    }

    public void setId(Integer id) {
        this.id = id;
    }
    
    public void addCop (String cop) {
        cops.add(cop);
    }
}
