package sec;

public class MsgTypes {

    //LOGIN_LOGOUT
    public static int LOGGIN_TYPE = 1;
    public static int DISCONECTION = 2;

    //COP
    public static int COP_LOGIN_PASSWORD_TYPE = 3;
    public static int CREATE_PATROL = 4;
    public static int SET_CURRENT_POSITION = 5;
    public static int COP_SET_DESTINATION = 6;
    public static int COP_ADD_EXTERNAL_MEMBER = 7;

    public static int JOIN_PATROL = 8;

    //CAPTAIN
    public static int CAPTAIN_LOGIN_PASSWORD_TYPE = 9;
    public static int ADD_SUPERVISE_COP = 10;
    public static int GET_INFOS_PATROL = 11;
    public static int CAPT_SET_DESTINATION = 12;
    public static int CAPT_ADD_EXTERNAL_MEMBER = 13;
    

    //ADMIN
    public static int ADMIN_LOGIN_PASSWORD_TYPE = 14;
    public static int GET_WAITING_REGISTRATIONS = 15;
    public static int ALLOW_USER_REGISTRATIONS = 16;           
    public static int GET_INFOS_ALL_PATROLS = 17;
    public static int SET_INFOS_PATROL = 18;            //send patrol object with modified data inside
    public static int DELETE_SERVER_INFOS = 19;

    //GOUVERNMENT
    public static int GOUVERNEMENT_LOGIN_PASSWORD_TYPE = 20;
    public static int AUTHORISE_ADMIN_ERASE = 21;
}