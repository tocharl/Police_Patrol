package sec;

import java.util.ArrayList;
import java.util.Iterator;

public class Patrol {

    Integer id;
    ArrayList<String> cops;
    String captain;
    Coordinate destination;
    Coordinate currPosition;
    boolean isExternalMember;

    public Patrol(ArrayList<String> cops, String captain, Coordinate destination, 
            Coordinate currPosition, boolean isExternalMember, Integer id) {
        this.cops = cops;
        this.captain = captain;
        this.destination = destination;
        this.currPosition = currPosition;
        this.isExternalMember = isExternalMember;
        this.id = id;
    }
    
    public Patrol(String login, Integer id) {
        this.id = id;
        this.cops = new ArrayList<String>();
        cops.add(login);
        this.captain = null;
        this.destination = null;
        this.currPosition = null;
        this.isExternalMember = false;
    }

    public Integer getId() {
        return id;
    }

    public ArrayList<String> getCops() {
        return cops;
    }

    public String getCaptain() {
        return captain;
    }

    public Coordinate getDestination() {
        return destination;
    }

    public Coordinate getCurrPosition() {
        return currPosition;
    }

    public boolean isExternalMember() {
        return isExternalMember;
    }

    public boolean addCopToPatrol(String login) {
        if(!cops.contains(login)){
            cops.add(login);
            return true;
        } else return false;
    }

    public boolean setCaptain(String captain) {
        if(!captain.equals(this.captain)){
            this.captain = captain;
            return true;
        } else return false;
    }

    public void setDestination(Coordinate destination) {
        this.destination = destination;
    }

    public void setCurrPosition(Coordinate currPosition) {
        this.currPosition = currPosition;
    }

    public void setIsExternalMember(boolean isExternalMember) {
        this.isExternalMember = isExternalMember;
    }

    @Override
    public String toString() {
        String toString = "";
        toString += id;
        for (Iterator<String> it = cops.iterator(); it.hasNext();) {
            String cop = it.next();
            if (cop != null) {
                toString += " ";
                toString += cop;
            }
            if(!it.hasNext()) toString += " |";
        }
        if (captain != null) {
            toString += " !" + captain;
        }
        if (currPosition != null) {
            toString += " " + currPosition;
        }
        if (destination != null) {
            toString += " " + destination;
        }
        toString += " " + isExternalMember;
        return toString;
    }
}
