package sec;

public class Pair<String,Integer> {

    private String login;
    private Integer  id;

    public Pair(String login,Integer  id ) {
        this.login = login;
        this.id = id;
    }

    public String getLogin() {
        return login;
    }

    public Integer getId() {
        return id;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public void setId(Integer id) {
        this.id = id;
    }
}
