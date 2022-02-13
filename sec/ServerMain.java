package sec;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SealedObject;
import javax.crypto.SecretKey;

public class ServerMain {

    private static int userIdCount = 1;
    private static int patrolCount = 1;

    private static String timeStamp = new SimpleDateFormat("[yyyy-MM-dd_HH:mm:ss] :").format(Calendar.getInstance().getTime());

    private static final List<Pair<String, Integer>> cops = new ArrayList<Pair<String, Integer>>();
    private static final List<Pair<String, Integer>> capts = new ArrayList<Pair<String, Integer>>();
    private static List<Captain> captains = new ArrayList<Captain>();

    private static List<String> engWaitingList = new ArrayList<String>();

    private static List<Patrol> patroList = new ArrayList<Patrol>();

    private static Integer idAdmin;
    private static Integer idGouvern;

    private static boolean deleteLogs = false;

    private static void replaceLine(String oldLine, String newLine) throws IOException {
        //replace oldline by newline in patrol file
        Path path = Paths.get("patrol.txt");
        Charset charset = StandardCharsets.UTF_8;
        String content = new String(Files.readAllBytes(path), charset);
        content = Pattern.compile(oldLine, Pattern.LITERAL).matcher(content).replaceAll(newLine);
        Files.write(path, content.getBytes(charset));
    }

    private static void sendCryptedObject(ObjectOutputStream out, SecretKey sessionKey, Object object) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, sessionKey);
            SealedObject sealedObject = new SealedObject((Serializable) object, cipher);
            out.writeObject(sealedObject);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException
                | IOException | IllegalBlockSizeException ex) {
        }
    }

    private static String parsePassword(String password) {
        return password.replaceAll("\\r\\n|[\\n\\x0B\\x0C\\r\\u0085\\u2028\\u2029]", "");
    }

    private static void createfiles() throws IOException {
        File cop = new File("cop.txt");
        File captain = new File("captain.txt");
        File admin = new File("admin.txt");
        File gouvernment = new File("government.txt");
        File logs = new File("logs.txt");
        if (!cop.exists()) {
            cop.createNewFile();
        }
        if (!captain.exists()) {
            captain.createNewFile();
        }
        if (!admin.exists()) {
            admin.createNewFile();
        }
        if (!gouvernment.exists()) {
            gouvernment.createNewFile();
        }
        if (!logs.exists()) {
            logs.createNewFile();
        }
    }
    
    private static void createPatrols() {
        FileReader fr = null;
        try {
            // Le fichier d'entrée
            File file = new File("patrol.txt");
            // Créer l'objet File Reader
            fr = new FileReader(file);
            // Créer l'objet BufferedReader
            BufferedReader br = new BufferedReader(fr);
            String line;
            String[] tabLine;
            int counter=1;
            int id = 0;
            ArrayList<String> cops = new ArrayList<>();
            String captain = null;
            Coordinate currentPosition = null;
            Coordinate destination = null;
            boolean externalMember = false;
            while ((line = br.readLine()) != null) {
                try {
                    tabLine = line.split(" ");
                    id = Integer.parseInt(tabLine[0]);
                    while (!tabLine[counter].equals("|")) {
                        cops.add(tabLine[counter]);
                        counter++;
                    }
                    counter++;      //skip |
                    if (tabLine[counter].charAt(0) == '!') {
                        captain = tabLine[counter++].substring(1);
                    }
                    if (!(tabLine[counter].equals("true") || tabLine[counter].equals("false"))) {
                        currentPosition = new Coordinate(Integer.parseInt(tabLine[counter++]),
                                Integer.parseInt(tabLine[counter++]));
                        destination = new Coordinate(Integer.parseInt(tabLine[counter++]),
                                Integer.parseInt(tabLine[counter++]));
                    }
                    externalMember = Boolean.parseBoolean(tabLine[counter]);
                } catch (ArrayIndexOutOfBoundsException e) {
                }
                patroList.add(new Patrol(cops, captain, destination, currentPosition, externalMember, id));
            }
            fr.close();
        } catch (FileNotFoundException ex) {
        } catch (IOException ex) {
        }
    }
    
    private static boolean testUniqueLogin(BasicMessage.RegisterMessage msg, boolean loginOk) throws IOException {
        // Le fichier d'entrée
        File file = new File("cop.txt");
        // Créer l'objet BufferedReader
        FileReader fr = new FileReader(file);
        // Créer l'objet BufferedReader
        BufferedReader br = new BufferedReader(fr);
        String line;
        List<String> logins = new ArrayList<>();
        String[] logs;
        while ((line = br.readLine()) != null) {
            logins.add(line);
        }
        for (String login : logins) {
            logs = login.split(" ");
            for (String log : logs) {
                if (log.equals(msg.getLogin())) {
                    loginOk = false;
                }
            }
        }
        // Le fichier d'entrée
        file = new File("captain.txt");
        // Créer l'objet BufferedReader
        fr = new FileReader(file);
        // Créer l'objet BufferedReader
        br = new BufferedReader(fr);
        String lineCapt;
        logins = new ArrayList<>();
        String[] logsCapt;
        while ((lineCapt = br.readLine()) != null) {
            logins.add(lineCapt);
        }
        for (String login : logins) {
            logsCapt = login.split(" ");
            for (String log : logsCapt) {
                if (log.equals(msg.getLogin())) {
                    loginOk = false;
                }
            }
        }
        return loginOk;
    }

    public static void main(String[] args) {
        try {
            BasicServer server;
            server = new BasicServer(42000);
            createfiles();
            createPatrols();

            //COP
            //handler 1 loggin as user
            server.registerHandler(new BasicServer.BasicMessageHandler() {
                @Override
                public void handle(BasicMessage m, ObjectInputStream in, ObjectOutputStream out, SecretKey sessionKey) {
                    if (m.getType() == MsgTypes.LOGGIN_TYPE) {
                        try {
                            BasicMessage.LogginMessage msg = (BasicMessage.LogginMessage) m;
                            Cipher uncipher = Cipher.getInstance("AES");
                            uncipher.init(Cipher.DECRYPT_MODE, sessionKey);
                            byte[] decryptedBytes = uncipher.doFinal(msg.getPsw());
                            int type = 0;
                            boolean ok = false;
                            // Le fichier d'entrée
                            File file = new File("cop.txt");
                            // Créer l'objet File Reader
                            FileReader fr = new FileReader(file);
                            // Créer l'objet BufferedReader        
                            BufferedReader br = new BufferedReader(fr);
                            String line;
                            List<String> logs = new ArrayList<>();
                            String[] infos = null;
                            while ((line = br.readLine()) != null) {
                                logs.add(line);
                            }
                            for (String log : logs) {
                                infos = log.split(" ");
                                ok = infos[0].equals(msg.getLoggin());
                                if (ok) {
                                    type = 1;
                                    break;
                                }
                            }
                            fr.close();

                            if (!ok) {
                                // Le fichier d'entrée
                                file = new File("captain.txt");
                                // Créer l'objet File Reader
                                fr = new FileReader(file);
                                // Créer l'objet BufferedReader        
                                br = new BufferedReader(fr);
                                logs = new ArrayList<>();
                                infos = null;
                                while ((line = br.readLine()) != null) {
                                    logs.add(line);
                                }
                                for (String log : logs) {
                                    infos = log.split(" ");
                                    ok = infos[0].equals(msg.getLoggin());
                                    if (ok) {
                                        type = 2;
                                        break;
                                    }
                                }
                                fr.close();
                            }
                            if (!ok) {
                                // Le fichier d'entrée
                                file = new File("admin.txt");
                                // Créer l'objet File Reader
                                fr = new FileReader(file);
                                // Créer l'objet BufferedReader        
                                br = new BufferedReader(fr);
                                logs = new ArrayList<>();
                                infos = null;
                                while ((line = br.readLine()) != null) {
                                    logs.add(line);
                                }
                                for (String log : logs) {
                                    infos = log.split(" ");
                                    ok = infos[0].equals(msg.getLoggin());
                                    if (ok) {
                                        type = 3;
                                        break;
                                    }
                                }
                                fr.close();
                            }
                            if (!ok) {
                                // Le fichier d'entrée
                                file = new File("government.txt");
                                // Créer l'objet File Reader
                                fr = new FileReader(file);
                                // Créer l'objet BufferedReader        
                                br = new BufferedReader(fr);
                                logs = new ArrayList<>();
                                infos = null;
                                while ((line = br.readLine()) != null) {
                                    logs.add(line);
                                }
                                for (String log : logs) {
                                    infos = log.split(" ");
                                    ok = infos[0].equals(msg.getLoggin());
                                    if (ok) {
                                        type = 4;
                                        break;
                                    }
                                }
                                fr.close();
                            }

                            if (ok) {   //ok for unique Loggin
                                byte[] salt = infos[2].getBytes("ISO-8859-1");
                                byte[] digest = new byte[decryptedBytes.length + salt.length];
                                System.arraycopy(decryptedBytes, 0, digest, 0, decryptedBytes.length);
                                System.arraycopy(salt, 0, digest, decryptedBytes.length, salt.length);

                                for (int j = 0; j < 100; j++) {
                                    try {
                                        //Creating the MessageDigest object
                                        MessageDigest md = MessageDigest.getInstance("SHA-256");

                                        //Passing data to the created MessageDigest Object
                                        md.update(digest);

                                        //Compute the message digest
                                        digest = md.digest();
                                    } catch (NoSuchAlgorithmException ex) {
                                    }
                                }

                                String digStr = new String(digest, "ISO-8859-1");
                                digStr = parsePassword(digStr);
                                if (digStr.equals(infos[1])) {      //ok for password
                                    switch (type) {
                                        case 1:
                                            cops.add(new Pair(msg.getLoggin(), userIdCount));
                                            sendCryptedObject(out, sessionKey, type);
                                            break;
                                        case 2:
                                            capts.add(new Pair(msg.getLoggin(), userIdCount));
                                            captains.add(new Captain(userIdCount));
                                            sendCryptedObject(out, sessionKey, type);
                                            break;
                                        case 3:
                                            idAdmin = userIdCount;
                                            sendCryptedObject(out, sessionKey, type);
                                            break;
                                        case 4:
                                            idGouvern = userIdCount;
                                            sendCryptedObject(out, sessionKey, type);
                                            break;
                                        default:
                                            break;
                                    }
                                    sendCryptedObject(out, sessionKey, "Loggin success, add this id before your next messages : " + userIdCount);
                                    userIdCount++;
                                    String filename = "logs.txt";
                                    try (FileWriter fw = new FileWriter(filename, true) //the true will append the new data
                                            ) {
                                        String register = timeStamp + "User login";
                                        fw.write(register + "\n");
                                        fw.close();
                                    }
                                } else {
                                    sendCryptedObject(out, sessionKey, 0);
                                    sendCryptedObject(out, sessionKey, "Wrong password");
                                }
                            } else {
                                sendCryptedObject(out, sessionKey, 0);
                                sendCryptedObject(out, sessionKey, "There is an error in your loggin");
                            }
                        } catch (IOException | InvalidKeyException | NoSuchAlgorithmException
                                | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException ex) {
                        }
                    }
                }
            });

            //handler  2 DECONNECTION
            server.registerHandler(new BasicServer.BasicMessageHandler() {
                @Override
                public void handle(BasicMessage m, ObjectInputStream in, ObjectOutputStream out, SecretKey sessionKe) {
                    if (m.getType() == MsgTypes.DISCONECTION) {
                        BasicMessage.DisconnectionMessage msg = (BasicMessage.DisconnectionMessage) m;
                        Integer id = msg.getUserId();
                        boolean ok = false;
                        while (!ok) {
                            for (int i = 0; i < cops.size(); i++) {
                                if (Objects.equals(id, cops.get(i).getId())) {
                                    cops.remove(cops.get(i));
                                    ok = true;
                                    break;
                                }
                            }
                            for (int i = 0; i < captains.size(); i++) {
                                if (Objects.equals(id, captains.get(i).getId())) {
                                    captains.remove(captains.get(i));
                                    ok = true;
                                    break;
                                }
                            }
                            if (Objects.equals(id, idAdmin)) {
                                idAdmin = 0;
                                ok = true;
                                break;
                            }
                            if (Objects.equals(id, idGouvern)) {
                                idGouvern = 0;
                                ok = true;
                                break;
                            }
                        }
                        String filename = "logs.txt";
                        try ( FileWriter fw = new FileWriter(filename, true)) {
                            String register = timeStamp + "User logout";
                            fw.write(register + "\n");
                            fw.close();

                        } catch (IOException ex) {
                            Logger.getLogger(ServerMain.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        System.out.println(idGouvern);
                        if (ok)
                            sendCryptedObject(out, sessionKe, "You are now disconnected");
                        else sendCryptedObject(out, sessionKe, "You are not connected");
                    }
                }
            });

            //handler 3 add COP login
            server.registerHandler(new BasicServer.BasicMessageHandler() {
                @Override
                public void handle(BasicMessage m, ObjectInputStream in, ObjectOutputStream out, SecretKey sessionKe) {
                    if (m.getType() == MsgTypes.COP_LOGIN_PASSWORD_TYPE) {
                        BasicMessage.RegisterMessage msg = (BasicMessage.RegisterMessage) m;
                        boolean loginOk = true;
                        boolean passwordOk = true;
                        String filename = "cop.txt";
                        String logCop = "1 " + msg.getLogin();
                        //check login
                        try {
                            
                            loginOk = testUniqueLogin(msg, loginOk);
                            
                            for (String string : engWaitingList) {
                                if (string.split(" ")[1].equals(msg.getLogin())) {
                                    loginOk = false;
                                }
                            }
                            //check password
                            if (loginOk) {
                                Cipher uncipher = Cipher.getInstance("AES");
                                uncipher.init(Cipher.DECRYPT_MODE, sessionKe);
                                byte[] decryptedBytes = uncipher.doFinal(msg.getPsw());
                                Pattern p = Pattern.compile("[0-9]");
                                Matcher matcher;
                                ByteCharSequence sequence = new ByteCharSequence(decryptedBytes);
                                matcher = p.matcher(sequence);
                                passwordOk = (matcher.find()) && (sequence.length() > 7);
                                //erase data
                                if (passwordOk) {
                                    String str = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                                            + "abcdefghijklmnopqrstuvxyz";
                                    StringBuilder s = new StringBuilder(8);
                                    for (int i = 0; i < 8; i++) {
                                        int index = (int) (str.length() * Math.random());
                                        s.append(str.charAt(index));
                                    }
                                    byte[] salt = s.toString().getBytes();
                                    byte[] digest = new byte[decryptedBytes.length + salt.length];
                                    System.arraycopy(decryptedBytes, 0, digest, 0, decryptedBytes.length);
                                    System.arraycopy(salt, 0, digest, decryptedBytes.length, salt.length);

                                    for (int i = 0; i < 100; i++) {
                                        try {
                                            //Creating the MessageDigest object
                                            MessageDigest md = MessageDigest.getInstance("SHA-256");
                                            //Passing data to the created MessageDigest Object
                                            md.update(digest);
                                            //Compute the message digest
                                            digest = md.digest();
                                        } catch (NoSuchAlgorithmException ex) {
                                        }
                                    }

                                    String digStr = new String(digest, "ISO-8859-1");
                                    digStr = parsePassword(digStr);
                                    String saltStr = new String(salt, "ISO-8859-1");
                                    String infoLog = logCop + " " + digStr + " " + saltStr;
                                    engWaitingList.add(infoLog);
                                    sendCryptedObject(out, sessionKe, "To log in waiting for admin allow");
                                    filename = "logs.txt";
                                    try (FileWriter fw = new FileWriter(filename, true) //the true will append the new data
                                            ) {
                                        String register = timeStamp + "Request Cop login";
                                        fw.write(register + "\n");
                                        fw.close();
                                    }
                                } else {
                                    sendCryptedObject(out, sessionKe, "password not match the rules, at least 8 char with at least 1 number");
                                }
                            } else {
                                sendCryptedObject(out, sessionKe, "user name already take, chose an other one");
                            }
                        } catch (IOException e) {
                            System.out.println("the file daesn't exist");
                        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException ex) {
                            Logger.getLogger(ServerMain.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            });

            //handler 5 CREATE_PATROL (COP)
            server.registerHandler(new BasicServer.BasicMessageHandler() {
                @Override
                public void handle(BasicMessage m, ObjectInputStream in, ObjectOutputStream out, SecretKey sessionKe) {
                    if (m.getType() == MsgTypes.CREATE_PATROL) {
                        BasicMessage.SetPatrolMessage msg = (BasicMessage.SetPatrolMessage) m;
                        Integer id = msg.getUserId();
                        String login = "";
                        boolean logOk = false;
                        for (int i = 0; i < cops.size(); i++) {
                            if (Objects.equals(id, cops.get(i).getId())) {
                                login = cops.get(i).getLogin();
                                logOk = true;
                                break;
                            }
                        }
                        try {
                            if (logOk) {
                                Patrol patrol = new Patrol(login, patrolCount);
                                patroList.add(patrol);
                                String filename = "patrol.txt";
                                try (FileWriter fw = new FileWriter(filename, true)) {
                                    fw.write(patrol.toString() + "\n");
                                }
                                sendCryptedObject(out, sessionKe, "add patrol success, your patrol id is : " + patrolCount);
                                filename = "logs.txt";
                                try (FileWriter fw2 = new FileWriter(filename, true) //the true will append the new data
                                        ) {
                                    String register = timeStamp + "New patrol create";
                                    fw2.write(register + "\n");
                                    fw2.close();
                                }
                                patrolCount++;
                            } else {
                                sendCryptedObject(out, sessionKe, "you are not loged in");
                            }
                        } catch (IOException e) {
                        }
                    }
                }
            });

            //handler 6 SET_CURRENT_POSITION
            server.registerHandler(new BasicServer.BasicMessageHandler() {
                @Override
                public void handle(BasicMessage m, ObjectInputStream in, ObjectOutputStream out, SecretKey sessionKe) {
                    if (m.getType() == MsgTypes.SET_CURRENT_POSITION) {
                        BasicMessage.SetDestinationMessage msg = (BasicMessage.SetDestinationMessage) m;
                        Integer id = msg.getUserId();
                        String login = "";
                        String newLine = "";
                        String oldLine = "";
                        boolean logOk = false;
                        boolean set = false;
                        for (int i = 0; i < cops.size(); i++) {
                            if (Objects.equals(id, cops.get(i).getId())) {
                                login = cops.get(i).getLogin();
                                logOk = true;
                                break;
                            }
                        }
                        if (!logOk) {
                            for (int i = 0; i < capts.size(); i++) {
                                if (Objects.equals(id, capts.get(i).getId())) {
                                    login = capts.get(i).getLogin();
                                    logOk = true;
                                    break;
                                }
                            }
                        }
                        try {
                            if (logOk) {
                                for (int i = 0; i < patroList.size(); i++) {
                                    for (int j = 0; j < patroList.get(i).getCops().size(); j++) {
                                        if (login.equals(patroList.get(i).getCops().get(j))) {
                                            oldLine = patroList.get(i).toString();
                                            patroList.get(i).setCurrPosition(msg.getCoordinate());
                                            newLine = patroList.get(i).toString();
                                            set = true;
                                            break;
                                        }
                                    }
                                    if (!set) {
                                        if (login.equals(patroList.get(i).getCaptain())) {
                                            oldLine = patroList.get(i).toString();
                                            patroList.get(i).setCurrPosition(msg.getCoordinate());
                                            newLine = patroList.get(i).toString();
                                            break;
                                        }
                                    }
                                }
                                replaceLine(oldLine, newLine);
                                sendCryptedObject(out, sessionKe, "Current position correct add to patrol");
                                String filename = "logs.txt";
                                try (FileWriter fw = new FileWriter(filename, true) //the true will append the new data
                                        ) {
                                    String register = timeStamp + "Patrol current position set";
                                    fw.write(register + "\n");
                                    fw.close();
                                }
                            } else {
                                sendCryptedObject(out, sessionKe, "you are not loged in");
                            }
                        } catch (IOException e) {
                        }
                    }
                }
            });

            //handler 7 COP_SET_DESTINATION
            server.registerHandler(new BasicServer.BasicMessageHandler() {
                @Override
                public void handle(BasicMessage m, ObjectInputStream in, ObjectOutputStream out, SecretKey sessionKe) {
                    if (m.getType() == MsgTypes.COP_SET_DESTINATION) {
                        BasicMessage.SetDestinationMessage msg = (BasicMessage.SetDestinationMessage) m;
                        Integer id = msg.getUserId();
                        String login = "";
                        String newLine = "";
                        String oldLine = "";
                        boolean logOk = false;
                        boolean set = false;
                        for (int i = 0; i < cops.size(); i++) {
                            if (Objects.equals(id, cops.get(i).getId())) {
                                login = cops.get(i).getLogin();
                                logOk = true;
                                break;
                            }
                        }
                        try {
                            if (logOk) {
                                for (int i = 0; i < patroList.size(); i++) {
                                    for (int j = 0; j < patroList.get(i).getCops().size(); j++) {
                                        if (login.equals(patroList.get(i).getCops().get(j))) {
                                            oldLine = patroList.get(i).toString();
                                            patroList.get(i).setDestination(msg.getCoordinate());
                                            newLine = patroList.get(i).toString();
                                            set = true;
                                            break;
                                        }
                                    }
                                    if (set) {
                                        break;
                                    }
                                }
                                replaceLine(oldLine, newLine);
                                sendCryptedObject(out, sessionKe, "Patrol destination set correctly");
                            } else {
                                sendCryptedObject(out, sessionKe, "you are not loged in");
                            }
                        } catch (IOException e) {
                        }
                    }
                }
            });

            //handler 8 COP_ADD_EXTERNAL_MEMBER      
            server.registerHandler(new BasicServer.BasicMessageHandler() {
                @Override
                public void handle(BasicMessage m, ObjectInputStream in, ObjectOutputStream out, SecretKey sessionKe) {
                    if (m.getType() == MsgTypes.COP_ADD_EXTERNAL_MEMBER) {
                        BasicMessage.SetExternalMemberMessage msg = (BasicMessage.SetExternalMemberMessage) m;
                        Integer id = msg.getUserId();
                        String login = "";
                        String newLine = "";
                        String oldLine = "";
                        boolean logOk = false;
                        boolean set = false;
                        for (int i = 0; i < cops.size(); i++) {
                            if (Objects.equals(id, cops.get(i).getId())) {
                                login = cops.get(i).getLogin();
                                logOk = true;
                                break;
                            }
                        }
                        try {
                            if (logOk) {
                                for (int i = 0; i < patroList.size(); i++) {
                                    for (int j = 0; j < patroList.get(i).getCops().size(); j++) {
                                        if (login.equals(patroList.get(i).getCops().get(j))) {
                                            oldLine = patroList.get(i).toString();
                                            patroList.get(i).setIsExternalMember(msg.isIsExternalMember());
                                            newLine = patroList.get(i).toString();
                                            set = true;
                                            break;
                                        }
                                    }
                                    if (set) {
                                        break;
                                    }
                                }
                                replaceLine(oldLine, newLine);
                                sendCryptedObject(out, sessionKe, " Externel member correctly set");
                                String filename = "logs.txt";
                                try (FileWriter fw = new FileWriter(filename, true) //the true will append the new data
                                        ) {
                                    String register = timeStamp + "Patrol external member set";
                                    fw.write(register + "\n");
                                    fw.close();
                                }
                            } else {
                                sendCryptedObject(out, sessionKe, "you are not loged in");
                            }
                        } catch (IOException e) {
                        }
                    }
                }
            });

            //handler 9 JOIN_PATROL (COP - CAPTAIN)                                               
            server.registerHandler(new BasicServer.BasicMessageHandler() {
                @Override
                public void handle(BasicMessage m, ObjectInputStream in, ObjectOutputStream out, SecretKey sessionKe) {
                    if (m.getType() == MsgTypes.JOIN_PATROL) {
                        BasicMessage.SetPatrolMessage msg = (BasicMessage.SetPatrolMessage) m;
                        Integer id = msg.getUserId();
                        int type = 0;
                        boolean logOk = false;
                        for (int i = 0; i < cops.size(); i++) {
                            if (Objects.equals(id, cops.get(i).getId())) {
                                logOk = true;
                                type = 1;
                                break;
                            }
                        }
                        if (!logOk) {
                            for (int i = 0; i < capts.size(); i++) {
                                if (Objects.equals(id, capts.get(i).getId())) {
                                    logOk = true;
                                    type = 2;
                                    break;
                                }
                            }
                        }
                        try {
                            if (logOk) {
                                //add cop or captain to patrol object
                                boolean isAdded=false;
                                String log = "";
                                String oldLine = "";
                                String newLine = "";
                                for (Patrol patrol : patroList) {
                                    if (type == 1) {
                                        for (Pair<String, Integer> pair : cops) {
                                            if (pair.getId() == msg.getUserId()) {
                                                log = pair.getLogin();
                                            }
                                        }
                                        if (patrol.getId() == msg.getIdPatrol()) {
                                            oldLine = patrol.toString();
                                            isAdded = patrol.addCopToPatrol(log);
                                            newLine = patrol.toString();
                                            break;
                                        }
                                    }
                                    if (type == 2) {
                                        for (Pair<String, Integer> capt : capts) {
                                            if (capt.getId() == msg.getUserId()) {
                                                log = capt.getLogin();
                                            }
                                        }
                                        if (patrol.getId() == msg.getIdPatrol()) {
                                            oldLine = patrol.toString();
                                            isAdded = patrol.setCaptain(log);
                                            newLine = patrol.toString();
                                        }
                                    }
                                }
                                if(isAdded){
                                    replaceLine(oldLine, newLine);
                                    sendCryptedObject(out, sessionKe, "You join the patrol " + msg.getIdPatrol());
                                }
                                else sendCryptedObject(out, sessionKe, "You are already register on this patrol");
                                String filename = "logs.txt";
                                try (FileWriter fw = new FileWriter(filename, true) //the true will append the new data
                                        ) {
                                    String register = timeStamp + "Patrol members modify";
                                    fw.write(register + "\n");
                                    fw.close();
                                }
                            } else {
                                sendCryptedObject(out, sessionKe, "you are not loged in");
                            }
                        } catch (IOException e) {
                        }
                    }
                }
            });
            //CAPTAIN

            //handler 10 add CAPTAIN login
            server.registerHandler(new BasicServer.BasicMessageHandler() {
                @Override
                public void handle(BasicMessage m, ObjectInputStream in, ObjectOutputStream out, SecretKey sessionKe) {
                    if (m.getType() == MsgTypes.CAPTAIN_LOGIN_PASSWORD_TYPE) {
                        BasicMessage.RegisterMessage msg = (BasicMessage.RegisterMessage) m;
                        boolean loginOk = true;
                        boolean passwordOk = true;
                        String filename = "captain.txt";
                        String logCap = "2 " + msg.getLogin();
                        try {
                            loginOk = testUniqueLogin(msg, loginOk);
                            for (String string : engWaitingList) {
                                if (string.split(" ")[1].equals(msg.getLogin())) {
                                    loginOk = false;
                                }
                            }
                            if (loginOk) {
                                Cipher uncipher = Cipher.getInstance("AES");
                                uncipher.init(Cipher.DECRYPT_MODE, sessionKe);
                                byte[] decryptedBytes = uncipher.doFinal(msg.getPsw());
                                Pattern p = Pattern.compile("[0-9]");
                                Matcher matcher;
                                ByteCharSequence sequence = new ByteCharSequence(decryptedBytes);
                                matcher = p.matcher(sequence);
                                passwordOk = (matcher.find()) && (sequence.length() > 7);

                                if (passwordOk) {
                                    String str = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                                            + "abcdefghijklmnopqrstuvxyz";
                                    StringBuilder s = new StringBuilder(8);
                                    for (int i = 0; i < 8; i++) {
                                        int index = (int) (str.length() * Math.random());
                                        s.append(str.charAt(index));
                                    }
                                    byte[] salt = s.toString().getBytes();
                                    byte[] digest = new byte[decryptedBytes.length + salt.length];
                                    System.arraycopy(decryptedBytes, 0, digest, 0, decryptedBytes.length);
                                    System.arraycopy(salt, 0, digest, decryptedBytes.length, salt.length);

                                    for (int i = 0; i < 100; i++) {
                                        try {
                                            //Creating the MessageDigest object
                                            MessageDigest md = MessageDigest.getInstance("SHA-256");
                                            //Passing data to the created MessageDigest Object
                                            md.update(digest);
                                            //Compute the message digest
                                            digest = md.digest();
                                        } catch (NoSuchAlgorithmException ex) {
                                        }
                                    }

                                    String digStr = new String(digest, "ISO-8859-1");
                                    digStr = parsePassword(digStr);
                                    String saltStr = new String(salt, "ISO-8859-1");
                                    String infoLog = logCap + " " + digStr + " " + saltStr;
                                    engWaitingList.add(infoLog);
                                    sendCryptedObject(out, sessionKe, "To log in waiting for admin allow");
                                    filename = "logs.txt";
                                    try (FileWriter fw = new FileWriter(filename, true) //the true will append the new data
                                            ) {
                                        String register = timeStamp + "Request captain login ";
                                        fw.write(register + "\n");
                                        fw.close();
                                    }
                                } else {
                                    sendCryptedObject(out, sessionKe, "password not match the rules, at least 8 char with at least 1 number");
                                }
                            } else {
                                sendCryptedObject(out, sessionKe, "user name already take, chose an other one");
                            }
                        } catch (IOException e) {
                            System.out.println("the file daesn't exist");
                        } catch (NoSuchAlgorithmException | NoSuchPaddingException
                                | InvalidKeyException | IllegalBlockSizeException | BadPaddingException ex) {
                        }
                    }
                }
            });
                

            //handler 12 ADD_SUPERVISE_COP ;
            server.registerHandler(new BasicServer.BasicMessageHandler() {
                @Override
                public void handle(BasicMessage m, ObjectInputStream in, ObjectOutputStream out, SecretKey sessionKe) {
                    if (m.getType() == MsgTypes.ADD_SUPERVISE_COP) {
                        BasicMessage.AddSuperviseMessage msg = (BasicMessage.AddSuperviseMessage) m;
                        Integer id = msg.getUserId();
                        String logCopToAdd = msg.getCop();
                        boolean logOk = false;
                        try {
                            int i = 0;
                            while (!logOk) {
                                if (Objects.equals(id, captains.get(i).getId())) {
                                    logOk = true;
                                }
                            }
                            if (logOk) {
                                captains.get(i).addCop(logCopToAdd);
                                sendCryptedObject(out, sessionKe, "you are the supervisor of the cop " + logCopToAdd);
                                String filename = "logs.txt";
                                try (FileWriter fw = new FileWriter(filename, true) //the true will append the new data
                                        ) {
                                    String register = timeStamp + "Add supervise cop";
                                    fw.write(register + "\n");
                                    fw.close();
                                }
                            } else {
                                sendCryptedObject(out, sessionKe, "You are not login");
                            }
                        } catch (IOException e) {
                        }
                    }
                }
            });

            //handler 13 GET_INFOS_PATROL ;
            server.registerHandler(new BasicServer.BasicMessageHandler() {
                @Override
                public void handle(BasicMessage m, ObjectInputStream in, ObjectOutputStream out, SecretKey sessionKe) {
                    if (m.getType() == MsgTypes.GET_INFOS_PATROL) {
                        BasicMessage.DisconnectionMessage msg = (BasicMessage.DisconnectionMessage) m;
                        Integer id = msg.getUserId();
                        boolean logOk = false;
                        ArrayList<String> patrolInfo = new ArrayList<>();
                        try {
                            int i = 0;
                            while (!logOk) {
                                if (Objects.equals(id, captains.get(i).getId())) {
                                    logOk = true;
                                }
                            }
                            if (logOk) {
                                // Le fichier d'entrée
                                File file = new File("patrol.txt");
                                // Créer l'objet BufferedReader
                                try (FileReader fr = new FileReader(file)) {
                                    // Créer l'objet BufferedReader
                                    BufferedReader br = new BufferedReader(fr);
                                    String line;
                                    List<String> logins = new ArrayList<>();
                                    String[] logs;
                                    for (int j = 0; j < captains.size(); j++) {
                                        String logCop = captains.get(j).getCops().get(j);
                                        while ((line = br.readLine()) != null) {
                                            logins.add(line);
                                            for (String login : logins) {
                                                logs = login.split(" ");
                                                for (String log : logs) {
                                                    if (log.equals(logCop)) {
                                                        patrolInfo.add(line);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                sendCryptedObject(out, sessionKe, patrolInfo);
                                sendCryptedObject(out, sessionKe, "Now you can see the information about your patrol");
                                String filename = "logs.txt";
                                try (FileWriter fw = new FileWriter(filename, true) //the true will append the new data
                                        ) {
                                    String register = timeStamp + "Admin get patrol infos";
                                    fw.write(register + "\n");
                                    fw.close();
                                }
                            } else {
                                sendCryptedObject(out, sessionKe, "You are not login");
                            }
                        } catch (IOException e) {
                        }
                    }
                }
            });

            //handler 14 SET_DESTINATION_PATROL 
            server.registerHandler(new BasicServer.BasicMessageHandler() {
                @Override
                public void handle(BasicMessage m, ObjectInputStream in, ObjectOutputStream out, SecretKey sessionKe) {
                    if (m.getType() == MsgTypes.CAPT_SET_DESTINATION) {
                        BasicMessage.SetPatrolDestinationMessage msg = (BasicMessage.SetPatrolDestinationMessage) m;
                        Integer id = msg.getUserId();
                        String logCp = "";
                        String newLine = "";
                        String oldLine = "";
                        Integer idPat = msg.getIdPatrol();
                        boolean logOk = false;
                        try {
                            int i = 0;
                            while (!logOk) {
                                if (Objects.equals(id, capts.get(i).getId())) {
                                    logCp = capts.get(i).getLogin();
                                    logOk = true;
                                }
                            }
                            if (logOk) {
                                for (int j = 0; j < patroList.size(); j++) {
                                    if (Objects.equals(idPat, patroList.get(j).getId())) {
                                        oldLine = patroList.get(i).toString();
                                        patroList.get(j).setDestination(msg.getCoordinate());
                                        newLine = patroList.get(i).toString();
                                        break;
                                    }
                                }
                            }
                            replaceLine(oldLine, newLine);
                            sendCryptedObject(out, sessionKe, "The patrol destination is correctly set");
                            String filename = "logs.txt";
                            try (FileWriter fw = new FileWriter(filename, true) //the true will append the new data
                                    ) {
                                String register = timeStamp + "Captain set patrol destination ";
                                fw.write(register + "\n");
                                fw.close();
                            }
                        } catch (IOException e) {
                        }
                    }
                }
            });

            //handler 15 CAPT_ADD_EXTERNAL_MEMBER
            server.registerHandler(new BasicServer.BasicMessageHandler() {
                @Override
                @SuppressWarnings("UnusedAssignment")
                public void handle(BasicMessage m, ObjectInputStream in, ObjectOutputStream out, SecretKey sessionKe) {
                    if (m.getType() == MsgTypes.CAPT_ADD_EXTERNAL_MEMBER) {
                        BasicMessage.SetPatrolExternalMemberMessage msg = (BasicMessage.SetPatrolExternalMemberMessage) m;
                        Integer id = msg.getUserId();
                        Integer idPat = msg.getPatrolId();
                        String newLine = "";
                        String oldLine = "";
                        boolean logOk = false;
                        int i = 0;
                        while (!logOk) {
                            if (Objects.equals(id, capts.get(i).getId())) {
                                logOk = true;
                            }
                        }
                        if (logOk) {
                            for (int j = 0; j < patroList.size(); j++) {
                                try {
                                    if (Objects.equals(idPat, patroList.get(j).getId())) {
                                        oldLine = patroList.get(i).toString();
                                        patroList.get(j).setIsExternalMember(msg.isIsExternalMember());
                                        newLine = patroList.get(i).toString();
                                        break;
                                    }
                                    replaceLine(oldLine, newLine);

                                    String filename = "logs.txt";
                                    try (FileWriter fw = new FileWriter(filename, true)) {
                                        String register = timeStamp + "Captain set petrol external member";
                                        fw.write(register + "\n");
                                        fw.close();
                                    }
                                } catch (IOException ex) {
                                    Logger.getLogger(ServerMain.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            }
                        } else {
                            sendCryptedObject(out, sessionKe, "you are not login");
                        }
                        sendCryptedObject(out, sessionKe, "The external memeber of the patrol is correctly set");
                    }
                }
            });

//ADMIN
            //handler 16 add ADMIN login
            server.registerHandler(new BasicServer.BasicMessageHandler() {
                @Override
                public void handle(BasicMessage m, ObjectInputStream in, ObjectOutputStream out, SecretKey sessionKe) {
                    if (m.getType() == MsgTypes.ADMIN_LOGIN_PASSWORD_TYPE) {
                        BasicMessage.RegisterMessage msg = (BasicMessage.RegisterMessage) m;
                        boolean loginOk = true;
                        boolean passwordOk = true;
                        try {
                            File file = new File("admin.txt");
                            if (file.length() != 0) {
                                loginOk = false;
                            }
                        } catch (Exception e) {
                            System.out.println("the file doesn't exist");
                        }
                        if (loginOk) {
                            try {
                                Cipher uncipher = Cipher.getInstance("AES");
                                uncipher.init(Cipher.DECRYPT_MODE, sessionKe);
                                byte[] decryptedBytes = uncipher.doFinal(msg.getPsw());
                                Pattern p = Pattern.compile("[0-9]");
                                Matcher matcher;
                                ByteCharSequence sequence = new ByteCharSequence(decryptedBytes);
                                matcher = p.matcher(sequence);
                                passwordOk = (matcher.find()) && (sequence.length() > 7);

                                if (passwordOk) {
                                    String str = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                                            + "abcdefghijklmnopqrstuvxyz";
                                    StringBuilder s = new StringBuilder(8);
                                    for (int i = 0; i < 8; i++) {
                                        int index = (int) (str.length() * Math.random());
                                        s.append(str.charAt(index));
                                    }
                                    byte[] salt = s.toString().getBytes();
                                    byte[] digest = new byte[decryptedBytes.length + salt.length];
                                    System.arraycopy(decryptedBytes, 0, digest, 0, decryptedBytes.length);
                                    System.arraycopy(salt, 0, digest, decryptedBytes.length, salt.length);
                                    for (int i = 0; i < 100; i++) {
                                        try {
                                            //Creating the MessageDigest object
                                            MessageDigest md = MessageDigest.getInstance("SHA-256");
                                            //Passing data to the created MessageDigest Object
                                            md.update(digest);
                                            //Compute the message digest
                                            digest = md.digest();
                                        } catch (NoSuchAlgorithmException ex) {
                                        }
                                    }
                                    try {
                                        String digStr = new String(digest, "ISO-8859-1");
                                        digStr = parsePassword(digStr);
                                        String saltStr = new String(salt, "ISO-8859-1");
                                        String filename = "admin.txt";
                                        try (FileWriter fw = new FileWriter(filename, true)) {
                                            fw.write(msg.getLogin() + " " + digStr + " " + saltStr + "\n");
                                        }
                                        sendCryptedObject(out, sessionKe, "welcome admin!");
                                        filename = "logs.txt";
                                        try (FileWriter fw2 = new FileWriter(filename, true)) {
                                            String register = timeStamp + "Captain set petrol external member";
                                            fw2.write(register + "\n");
                                            fw2.close();
                                        }
                                    } catch (IOException e) {
                                        System.out.println("the file doesn't exist");
                                    }
                                } else {
                                    sendCryptedObject(out, sessionKe, "password not match the rules, at least 8 char with at least 1 number");
                                }
                            } catch (NoSuchAlgorithmException | NoSuchPaddingException
                                    | IllegalBlockSizeException | BadPaddingException | InvalidKeyException ex) {
                            }
                        } else {
                            sendCryptedObject(out, sessionKe, "the admin already exist");
                        }
                    }
                }
            });

            //handler 18 GET_WAITING_REGISTRATIONS ;
            server.registerHandler(new BasicServer.BasicMessageHandler() {
                @Override
                public void handle(BasicMessage m, ObjectInputStream in, ObjectOutputStream out, SecretKey sessionKey) {
                    if (m.getType() == MsgTypes.GET_WAITING_REGISTRATIONS) {
                        BasicMessage.DisconnectionMessage msg = (BasicMessage.DisconnectionMessage) m;
                        Integer id = msg.getUserId();
                        if (Objects.equals(id, idAdmin)) {
                            sendCryptedObject(out, sessionKey, engWaitingList);
                            try {
                                if (Objects.equals(id, idAdmin)) {
                                    String filename = "logs.txt";
                                    try (FileWriter fw = new FileWriter(filename, true) //the true will append the new data
                                            ) {
                                        String register = timeStamp + "Admin get the list of not allow users";
                                        fw.write(register + "\n");
                                        fw.close();
                                    }
                                    sendCryptedObject(out, sessionKey, "Select the users you allow to register");
                                } else {
                                    sendCryptedObject(out, sessionKey, "you are not login");
                                }

                            } catch (IOException e) {
                            }
                        }
                    }
                }
            });

            //handler 19 ALLOW_USER_REGISTRATIONS ;
            server.registerHandler(new BasicServer.BasicMessageHandler() {
                @Override
                public void handle(BasicMessage m, ObjectInputStream in, ObjectOutputStream out, SecretKey sessionKey) {
                    if (m.getType() == MsgTypes.ALLOW_USER_REGISTRATIONS) {
                        BasicMessage.AllowRegistrationsMessage msg = (BasicMessage.AllowRegistrationsMessage) m;
                        List<String> logs = msg.getLogins();
                        boolean okAllow = false;
                        if (msg.getUserId() == idAdmin && !logs.isEmpty()) {
                            for (String log : logs) {
                                for (int i = 0; i < engWaitingList.size(); i++) {
                                    String[] infos = engWaitingList.get(i).split(" ");
                                    if (log.equals(infos[1])) {
                                        try {
                                            if ("1".equals(infos[0])) {
                                                String filename = "cop.txt";
                                                try (FileWriter fw = new FileWriter(filename, true)) {
                                                    String[] line = engWaitingList.get(i).split(" ");
                                                    engWaitingList.remove(i);
                                                    i--;
                                                    String register = "";
                                                    for (int j = 1; j < line.length; j++) {
                                                        register += line[j] + " ";
                                                    }
                                                    fw.write(register + "\n");
                                                    okAllow = true;
                                                }
                                            } else if ("2".equals(infos[0])) {
                                                String filename = "captain.txt";
                                                try (FileWriter fw = new FileWriter(filename, true)) {
                                                    String[] line = engWaitingList.get(i).split(" ");
                                                    engWaitingList.remove(i);
                                                    i--;
                                                    String register = "";
                                                    for (int j = 1; j < line.length; j++) {
                                                        register += line[j] + " ";
                                                    }
                                                    fw.write(register + "\n");
                                                    okAllow = true;
                                                }
                                            }
                                        } catch (IOException e) {
                                        }
                                    }
                                }
                            }
                            String filename = "logs.txt";
                            try (FileWriter fw = new FileWriter(filename, true) //the true will append the new data
                                    ) {
                                String register = timeStamp + "admin allow users ";
                                fw.write(register + "\n");
                                fw.close();
                            } catch (IOException ex) {
                                Logger.getLogger(ServerMain.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            if(okAllow) sendCryptedObject(out, sessionKey, "The login/s are correctly allow!");
                            else sendCryptedObject(out, sessionKey, "Incorrect username.");
                        } else {
                            sendCryptedObject(out, sessionKey, "The list of logins "
                                    + "to allow is empty, plese get the list and write the correct login to allow.");
                        }
                    }
                }
            });

            //handler 20 GET_INFOS_ALL_PATROLS
            server.registerHandler(new BasicServer.BasicMessageHandler() {
                @Override
                public void handle(BasicMessage m, ObjectInputStream in, ObjectOutputStream out, SecretKey sessionKe) {
                    if (m.getType() == MsgTypes.GET_INFOS_ALL_PATROLS) {
                        BasicMessage.DisconnectionMessage msg = (BasicMessage.DisconnectionMessage) m;
                        Integer id = msg.getUserId();
                        ArrayList<String> patrolInfo = new ArrayList<>();
                        try {
                            if (Objects.equals(id, idAdmin)) {
                                File file = new File("logs.txt");
                                // Créer l'objet File Reader
                                FileReader fr = new FileReader(file);
                                // Créer l'objet BufferedReader
                                BufferedReader br = new BufferedReader(fr);
                                String line;
                                while ((line = br.readLine()) != null) {
                                    patrolInfo.add(line);
                                }
                                String filename = "logs.txt";
                                try (FileWriter fw = new FileWriter(filename, true)) {
                                    String register = timeStamp + "Admin get all patrols infos";
                                    fw.write(register + "\n");
                                    fw.close();
                                }
                                out.writeObject(patrolInfo);
                                out.writeObject("now you can see the list of logs");
                            } else {
                                out.writeObject("You are not loggin");
                            }
                        } catch (IOException e) {
                        }
                        sendCryptedObject(out, sessionKe, "The patrol destination is correctly set");
                    }
                }
            });

            //handler 21 SET_INFOS_PATROL;                                                           A TERMINE
            server.registerHandler(new BasicServer.BasicMessageHandler() {
                @Override
                public void handle(BasicMessage m, ObjectInputStream in, ObjectOutputStream out, SecretKey sessionKe) {
                    if (m.getType() == MsgTypes.SET_INFOS_PATROL) {
                        BasicMessage.AllowRegistrationsMessage msg = (BasicMessage.AllowRegistrationsMessage) m;
                        Integer id = msg.getUserId();

                        ArrayList<String> patrolInfo = new ArrayList<>();
                        if (Objects.equals(id, idAdmin)) {

                        }
                        sendCryptedObject(out, sessionKe, "The patrol destination is correctly set");
                    }
                }
            });

            //handler 22 DELETE_SERVER_INFOS
            server.registerHandler(new BasicServer.BasicMessageHandler() {
                @Override
                public void handle(BasicMessage m, ObjectInputStream in, ObjectOutputStream out, SecretKey sessionKe) {
                    if (m.getType() == MsgTypes.DELETE_SERVER_INFOS) {
                        BasicMessage.DisconnectionMessage msg = (BasicMessage.DisconnectionMessage) m;
                        Integer id = msg.getUserId();
                        try {
                            if (Objects.equals(id, idAdmin)) {
                                deleteLogs = true;
                                String filename = "logs.txt";
                                try (FileWriter fw = new FileWriter(filename, true)) {
                                    String register = timeStamp + "Admin delete logs request ";
                                    fw.write(register + "\n");
                                    fw.close();
                                }
                                sendCryptedObject(out, sessionKe, "Waiting for the gouvernment allow to delete logs");
                            }
                            sendCryptedObject(out, sessionKe, "You are not login");
                        } catch (IOException e) {
                        }
                    }
                }
            });

            //GOVERNMENT
            //handler 23 GOUVERNEMENT_LOGIN_TYPE
            server.registerHandler(new BasicServer.BasicMessageHandler() {
                @Override
                public void handle(BasicMessage m, ObjectInputStream in, ObjectOutputStream out, SecretKey sessionKe) {
                    if (m.getType() == MsgTypes.GOUVERNEMENT_LOGIN_PASSWORD_TYPE) {
                        BasicMessage.RegisterMessage msg = (BasicMessage.RegisterMessage) m;
                        boolean loginOk = true;
                        boolean passwordOk = true;
                        try {
                            File file = new File("government.txt");
                            if (file.length() != 0) {
                                loginOk = false;
                            }
                        } catch (Exception e) {
                            System.out.println("the file doesn't exist");
                        }
                        if (loginOk) {
                            try {
                                Cipher uncipher = Cipher.getInstance("AES");
                                uncipher.init(Cipher.DECRYPT_MODE, sessionKe);
                                byte[] decryptedBytes = uncipher.doFinal(msg.getPsw());
                                Pattern p = Pattern.compile("[0-9]");
                                Matcher matcher;
                                ByteCharSequence sequence = new ByteCharSequence(decryptedBytes);
                                matcher = p.matcher(sequence);
                                passwordOk = (matcher.find()) && (sequence.length() > 7);

                                if (passwordOk) {
                                    String str = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                                            + "abcdefghijklmnopqrstuvxyz";
                                    StringBuilder s = new StringBuilder(8);
                                    for (int i = 0; i < 8; i++) {
                                        int index = (int) (str.length() * Math.random());
                                        s.append(str.charAt(index));
                                    }
                                    byte[] salt = s.toString().getBytes();
                                    byte[] digest = new byte[decryptedBytes.length + salt.length];
                                    System.arraycopy(decryptedBytes, 0, digest, 0, decryptedBytes.length);
                                    System.arraycopy(salt, 0, digest, decryptedBytes.length, salt.length);
                                    for (int i = 0; i < 100; i++) {
                                        try {
                                            //Creating the MessageDigest object
                                            MessageDigest md = MessageDigest.getInstance("SHA-256");
                                            //Passing data to the created MessageDigest Object
                                            md.update(digest);
                                            //Compute the message digest
                                            digest = md.digest();
                                        } catch (NoSuchAlgorithmException ex) {
                                        }
                                    }
                                    try {
                                        String digStr = new String(digest, "ISO-8859-1");
                                        digStr = parsePassword(digStr);
                                        String saltStr = new String(salt, "ISO-8859-1");
                                        String filename = "government.txt";
                                        try (FileWriter fw = new FileWriter(filename, true)) {
                                            fw.write(msg.getLogin() + " " + digStr + " " + saltStr + "\n");
                                        }
                                        sendCryptedObject(out, sessionKe, "welcome gouvernment!");
                                        filename = "logs.txt";
                                        try (FileWriter fw2 = new FileWriter(filename, true) //the true will append the new data
                                                ) {
                                            String register = timeStamp + "Add Gouvernment login ";
                                            fw2.write(register + "\n");
                                            fw2.close();
                                        }
                                    } catch (IOException e) {
                                        System.out.println("the file doesn't exist");
                                    }
                                } else {
                                    sendCryptedObject(out, sessionKe, "password not match the rules, at least 8 char with at least 1 number");
                                }
                            } catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException
                                    | BadPaddingException | InvalidKeyException ex) {
                            }
                        } else {
                            sendCryptedObject(out, sessionKe, "the gouvernment already exist");
                        }
                    }
                }
            });

            //handler 25 AUTHORISE_ADMIN_ERASE
            server.registerHandler(new BasicServer.BasicMessageHandler() {
                @Override
                public void handle(BasicMessage m, ObjectInputStream in, ObjectOutputStream out, SecretKey sessionKe) {
                    if (m.getType() == MsgTypes.AUTHORISE_ADMIN_ERASE) {
                        BasicMessage.DisconnectionMessage msg = (BasicMessage.DisconnectionMessage) m;
                        Integer id = msg.getUserId();
                        try {
                            if (Objects.equals(id, idGouvern)) {
                                if (deleteLogs) {
                                    File file = new File("logs.txt");
                                    if (file.delete()) {
                                        System.out.println(file.getName() + " est supprimé.");
                                    } else {
                                        System.out.println("Opération de suppression echouée");
                                    }
                                    String filename = "logs.txt";
                                    try (FileWriter fw = new FileWriter(filename, true) //the true will append the new data
                                            ) {
                                        String register = timeStamp + "Gouvernment authorise delete logs ";
                                        fw.write(register + "\n");
                                        fw.close();
                                    }
                                    sendCryptedObject(out, sessionKe, "Logs are delete correctly");
                                }
                            } else {
                                sendCryptedObject(out, sessionKe, "Your are not loggin");
                            }
                        } catch (IOException e) {
                        }
                        sendCryptedObject(out, sessionKe, "The patrol destination is correctly set");
                    }
                }
            });

            System.out.println("Handlers registered");
            server.start();
        } catch (IOException ex) {
        }
    }
}
