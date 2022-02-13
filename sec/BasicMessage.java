package sec;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class BasicMessage implements Serializable
{
    private int type;
    
    private static int id_count = 0;
    private int id;
    
    public static final int EXIT_TYPE = 0;    
    public static final BasicMessage EXIT_MSG = new BasicMessage(EXIT_TYPE);
    
    public BasicMessage(int type)
    {
        this.type = type;
        
        this.id = id_count;        
        BasicMessage.id_count++;
    }
    
    public int getId()
    {
        return id;
    }
    
    public int getType()
    {
        return type;
    }
    
    public static class TextMessage extends BasicMessage implements Serializable
    {   
        private String txt;
        
        public TextMessage(String txt, int type)
        {
            super(type);  
            this.txt = txt;
        }
        
        public String getText()
        {
            return txt;   
        }
    }
    
    public static class DisconnectionMessage extends BasicMessage implements Serializable       //TODO refactore le nom
    {   
        private int userId;
        
        public DisconnectionMessage(int userId, int type)
        {
            super(type);  
            this.userId = userId;
        }
        
        public int getUserId()
        {
            return userId;   
        }
    }
    
    public static class RegisterMessage extends BasicMessage implements Serializable
    {   
        private String login;
        private byte[] psw;
        
        public RegisterMessage(String login, byte[] psw, int type)
        {
            super(type);  
            this.login = login;
            this.psw = psw;
        }

        public String getLogin() {
            return login;
        }
        
        public byte[] getPsw()
        {
            return psw;   
        }
    }
    
    public static class LogginMessage extends BasicMessage implements Serializable
    {   
        private String loggin;
        private byte[] psw;
        
        public LogginMessage(String loggin, byte[] psw, int type)
        {
            super(type);  
            this.loggin = loggin;
            this.psw = psw;
        }
        
        public String getLoggin()
        {
            return loggin;
        }
        
        public byte[] getPsw()
        {
            return psw;   
        }
    }
    
    public static class SetPatrolMessage extends BasicMessage implements Serializable
    {   
        private int idPatrol;
        private int userId;
        
        public SetPatrolMessage(int userId, int idPatrol, int type)
        {
            super(type);  
            this.idPatrol = idPatrol;
            this.userId = userId;
        }
        
        public int getIdPatrol()
        {
            return idPatrol;   
        }

        public int getUserId() {
            return userId;
        }
    }
    
    public static class SetExternalMemberMessage extends BasicMessage implements Serializable
    {   
        private int userId;
        private boolean isExternalMember;
        
        public SetExternalMemberMessage(int userId, boolean isExternalMember, int type)
        {
            super(type);  
            this.userId = userId;
            this.isExternalMember = isExternalMember;
        }
        
        public int getUserId()
        {
            return userId;   
        }

        public boolean isIsExternalMember() {
            return isExternalMember;
        }
    }
    
    public static class SetPatrolExternalMemberMessage extends BasicMessage implements Serializable
    {   
        private int userId;
        private int patrolId;
        private boolean isExternalMember;
        
        public SetPatrolExternalMemberMessage(int userId, int patrolId, boolean isExternalMember, int type)
        {
            super(type);  
            this.userId = userId;
            this.patrolId = patrolId;
            this.isExternalMember = isExternalMember;
        }
        
        public int getUserId()
        {
            return userId;   
        }

        public boolean isIsExternalMember() {
            return isExternalMember;
        }

        public int getPatrolId() {
            return patrolId;
        }
    }
    
    public static class RetrievePatrolsMessage extends BasicMessage implements Serializable
    {   
        private int userId;
        
        public RetrievePatrolsMessage(int userId, int type)
        {
            super(type);  
            this.userId = userId;
        }
        
        public int getUserId() {
            return userId;
        }
    }
    
    public static class SetPatrolDestinationMessage extends BasicMessage implements Serializable
    {   
        private int userId;
        private Coordinate coordinate;
        private int idPatrol;
        
        public SetPatrolDestinationMessage(int userId, Coordinate coordinate, int idPatrol, int type)
        {
            super(type);  
            this.userId = userId;
            this.coordinate = coordinate;
            this.idPatrol = idPatrol;
        }

        public Coordinate getCoordinate() {
            return coordinate;
        }
        
        public int getUserId() {
            return userId;
        }

        public int getIdPatrol() {
            return idPatrol;
        }
    }
    
    public static class SetDestinationMessage extends BasicMessage implements Serializable
    {   
        private int userId;
        private Coordinate coordinate;
        
        public SetDestinationMessage(int userId, Coordinate coordinate, int type)
        {
            super(type);  
            this.userId = userId;
            this.coordinate = coordinate;
        }

        public Coordinate getCoordinate() {
            return coordinate;
        }
        
        public int getUserId() {
            return userId;
        }
    }
    
    public static class AddSuperviseMessage extends BasicMessage implements Serializable
    {   
        private int userId;
        private String cop;
        
        public AddSuperviseMessage(int userId, String cop, int type)
        {
            super(type);  
            this.userId = userId;
            this.cop = cop;
        }

        public String getCop() {
            return cop;
        }
        
        public int getUserId() {
            return userId;
        }
    }
    
    public static class SetPatrolInfoMessage extends BasicMessage implements Serializable
    {   
        private int userId;
        private Patrol patrol;
        
        public SetPatrolInfoMessage(int userId, Patrol patrol, int type)
        {
            super(type);  
            this.userId = userId;
            this.patrol = patrol;
        }
        
        public int getUserId() {
            return userId;
        }

        public Patrol getPatrol() {
            return patrol;
        }
    }
    
    public static class AllowRegistrationsMessage extends BasicMessage implements Serializable
    {   
        private int userId;
        private ArrayList<String> logins;
        
        public AllowRegistrationsMessage(int userId, ArrayList<String> logins, int type)
        {
            super(type);  
            this.userId = userId;
            this.logins = logins;
        }

        public int getUserId() {
            return userId;
        }

        public List<String> getLogins() {
            return logins;
        }
    }
}
