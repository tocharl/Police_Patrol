package sec;

import java.io.IOException;
import java.util.Scanner;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import javax.crypto.Cipher;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.util.ArrayList;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SealedObject;
import javax.crypto.SecretKey;

public class BasicTextClient {

    Socket socket;
    ObjectOutputStream out;
    ObjectInputStream in;

    String ip;
    int port;

    byte[] seed;
    byte[] hash;
    PublicKey publicKey;
    PrivateKey privateKey;
    SecretKey sessionKey;
    SecureRandom random;
    Challenge challenge;
    int randomInt;
    int n;
    int userType=0;

    public BasicTextClient(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public void start() {
        try {
            int cmpHash = 1;
            socket = new Socket(ip, port);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            publicKey = (PublicKey) in.readObject();
            
            //session key
            KeyGenerator sessionGenerator = KeyGenerator.getInstance("AES");
            sessionKey = sessionGenerator.generateKey();
            Cipher sessionChipher = Cipher.getInstance("RSA");
            sessionChipher.init(Cipher.WRAP_MODE, publicKey);
            byte[] wrappedSessionKey = sessionChipher.wrap(sessionKey);
            out.writeObject(wrappedSessionKey);
            
            seed = (byte[]) decryptData((SealedObject) in.readObject());
            random = SecureRandom.getInstance("SHA1PRNG");
            String line;
            
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();
            privateKey = keyPair.getPrivate();
            out.writeObject(keyPair.getPublic());
            
            //signature
            String messageString = "text";
            Signature signature = Signature.getInstance("SHA1withRSA");
            signature.initSign(privateKey, new SecureRandom());
            byte[] message = messageString.getBytes();
            signature.update(message);
            byte[] sigBytes = signature.sign();
            out.writeObject(message);
            out.writeObject(sigBytes);
            System.out.println("signature send");
              
            //first cipher challenge
            challenge = (Challenge) decryptData((SealedObject) in.readObject());
            Cipher uncipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            uncipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] decryptedBytes = uncipher.doFinal(challenge.getDigest());
            n = ByteBuffer.wrap(decryptedBytes).getInt();

            do {
                Scanner scanner = new Scanner(System.in);
                switch (userType) {
                    case 0:
                        System.out.println("Loggin :                        1 <username> <password>");
                        System.out.println("Register cop account :          3 <username> <password>");
                        System.out.println("Register captain account :      9 <username> <password>");
                        System.out.println("Register admin account :        14 <username> <password>");
                        System.out.println("Register government account :   20 <username> <password>");
                        break;
                    case 1:
                        System.out.println("Disconnection :         2 <id>");
                        System.out.println("Create patrol :         4 <id>");
                        System.out.println("Set current position :  5 <id> <x> <y>");
                        System.out.println("Set destination :       6 <id> <x> <y>");
                        System.out.println("Set external member :   7 <id> <true/false>");
                        System.out.println("Join existing patrol :  8 <id> <idPatrol>");
                        break;
                    case 2:
                        System.out.println("Disconnection :                     2 <id>");
                        System.out.println("Join existing patrol :              8 <id> <idPatrol>");
                        System.out.println("Add a cop to your supervision :     10 <id> <copLog>");
                        System.out.println("Get your patrols informations :     11 <id>");
                        System.out.println("Set destination of a patrol :       12 <id> <idPatrol> <x> <y>");
                        System.out.println("Set external member of a patrol :   13 <id> <idPatrol> <true/false>");
                        break;
                    case 3:
                        System.out.println("Disconnection :                                  2 <id>");
                        System.out.println("Get waiting list of registrations :              15 <id>");
                        System.out.println("Validate registration of one or multiple users : 16 <id> <userLog ... userLog ...>");
                        System.out.println("Ask erase logs file :                            19 <id>");
                        break;
                    case 4:
                        System.out.println("Disconnection :                                  2 <id>");
                        System.out.println("Allow to erase logs file :                      21 <id>");
                        break;
                    default:
                        break;
                }
                
                System.out.print("> ");
                line = scanner.nextLine();
                boolean known = true;
                if (line.isEmpty()) {
                    continue;
                }

                try {   //line.substring(0,1) -> command type
                    Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                    cipher.init(Cipher.ENCRYPT_MODE, publicKey);
                    byte[] toEncrypt = ByteBuffer.allocate(4).putInt(randomInt).array();
                    sendCryptedObject(new Challenge(n + 1, cipher.doFinal(toEncrypt)));

                    //Lamport OTP
                    hash = new byte[seed.length];
                    System.arraycopy(seed, 0, hash, 0, seed.length);

                    if (cmpHash == 42) {
                        cmpHash = 2;
                    }
                    hash = doHash(hash, 42 - cmpHash);
                    cmpHash++;

                    sendCryptedObject(hash);

                    String[] infos = line.split(" ");
                    if (Integer.parseInt(infos[0]) == MsgTypes.LOGGIN_TYPE) {       //1
                        //encrypte password
                        
                        byte[] messageToBytes = infos[2].getBytes();
                        Cipher cipherLog = Cipher.getInstance("AES");
                        cipherLog.init(Cipher.ENCRYPT_MODE, sessionKey);
                        byte[] encryptedBytes = cipherLog.doFinal(messageToBytes);

                        sendCryptedObject(new BasicMessage.LogginMessage(infos[1], encryptedBytes, MsgTypes.LOGGIN_TYPE));
                        
                        userType = (int) decryptData((SealedObject) in.readObject());
                    } else if (Integer.parseInt(infos[0]) == MsgTypes.DISCONECTION) {       //2
                       sendCryptedObject(new BasicMessage.DisconnectionMessage(Integer.parseInt(line.substring(2)), 
                                MsgTypes.DISCONECTION));
                    } else if (Integer.parseInt(infos[0]) == MsgTypes.COP_LOGIN_PASSWORD_TYPE) {      //3
                        //encrypte password
                        
                        byte[] messageToBytes = infos[2].getBytes();
                        Cipher cipherPwd = Cipher.getInstance("AES");
                        cipherPwd.init(Cipher.ENCRYPT_MODE, sessionKey);
                        byte[] encryptedBytes = cipherPwd.doFinal(messageToBytes);

                        sendCryptedObject(new BasicMessage.RegisterMessage(infos[1], encryptedBytes, MsgTypes.COP_LOGIN_PASSWORD_TYPE));     
                    } else if (Integer.parseInt(infos[0]) == MsgTypes.CREATE_PATROL) {      //4
                        sendCryptedObject(new BasicMessage.SetPatrolMessage(Integer.parseInt(line.substring(2)), -1, 
                                MsgTypes.CREATE_PATROL));
                    } else if (Integer.parseInt(infos[0]) == MsgTypes.SET_CURRENT_POSITION) {       //5
                        sendCryptedObject(new BasicMessage.SetDestinationMessage(Integer.parseInt(infos[1]), 
                                new Coordinate(Integer.parseInt(infos[2]), Integer.parseInt(infos[3])), MsgTypes.SET_CURRENT_POSITION));
                    } else if (Integer.parseInt(infos[0]) == MsgTypes.COP_SET_DESTINATION) {        //6
                        sendCryptedObject(new BasicMessage.SetDestinationMessage(Integer.parseInt(infos[1]), 
                                new Coordinate(Integer.parseInt(infos[2]), Integer.parseInt(infos[3])), MsgTypes.COP_SET_DESTINATION));
                    } else if (Integer.parseInt(infos[0]) == MsgTypes.COP_ADD_EXTERNAL_MEMBER) {        //7
                        sendCryptedObject(new BasicMessage.SetExternalMemberMessage(Integer.parseInt(infos[1]), 
                                Boolean.parseBoolean(infos[2]), MsgTypes.COP_ADD_EXTERNAL_MEMBER));
                    } else if (Integer.parseInt(infos[0]) == MsgTypes.JOIN_PATROL) {        //8
                        sendCryptedObject(new BasicMessage.SetPatrolMessage(Integer.parseInt(infos[1]), 
                                Integer.parseInt(infos[2]), MsgTypes.JOIN_PATROL));
                    } else if (Integer.parseInt(infos[0]) == MsgTypes.CAPTAIN_LOGIN_PASSWORD_TYPE) {      //9
                        //encrypte password
                        
                        byte[] messageToBytes = infos[2].getBytes();
                        Cipher cipherPwd = Cipher.getInstance("AES");
                        cipherPwd.init(Cipher.ENCRYPT_MODE, sessionKey);
                        byte[] encryptedBytes = cipherPwd.doFinal(messageToBytes);

                        sendCryptedObject(new BasicMessage.RegisterMessage(infos[1], encryptedBytes, MsgTypes.CAPTAIN_LOGIN_PASSWORD_TYPE));      
                    } else if (Integer.parseInt(infos[0]) == MsgTypes.ADD_SUPERVISE_COP) {      //10
                        sendCryptedObject(new BasicMessage.AddSuperviseMessage(Integer.parseInt(infos[1]), 
                                infos[2], MsgTypes.ADD_SUPERVISE_COP));
                    } else if (Integer.parseInt(infos[0]) == MsgTypes.GET_INFOS_PATROL) {       //11
                        sendCryptedObject(new BasicMessage.DisconnectionMessage(Integer.parseInt(line.substring(3)), 
                                MsgTypes.GET_INFOS_PATROL));
                        ArrayList<String> patrols = (ArrayList) decryptData((SealedObject) in.readObject());
                        for (String patrol : patrols) {
                            System.out.println(patrol);
                        }
                    } else if (Integer.parseInt(infos[0]) == MsgTypes.CAPT_SET_DESTINATION) {       //12
                        sendCryptedObject(new BasicMessage.SetPatrolDestinationMessage(Integer.parseInt(infos[1]), 
                                new Coordinate(Integer.parseInt(infos[2]), Integer.parseInt(infos[3])), 
                                Integer.parseInt(infos[4]), MsgTypes.CAPT_SET_DESTINATION));
                    } else if (Integer.parseInt(infos[0]) == MsgTypes.CAPT_ADD_EXTERNAL_MEMBER) {       //13
                        sendCryptedObject(new BasicMessage.SetPatrolExternalMemberMessage(Integer.parseInt(infos[1]), 
                                Integer.parseInt(infos[2]), Boolean.parseBoolean(infos[3]), 
                                MsgTypes.CAPT_ADD_EXTERNAL_MEMBER));
                    } else if (Integer.parseInt(infos[0]) == MsgTypes.ADMIN_LOGIN_PASSWORD_TYPE) {        //14
                        //encrypte password
                        
                        byte[] messageToBytes = infos[2].getBytes();
                        Cipher cipherPwd = Cipher.getInstance("AES");
                        cipherPwd.init(Cipher.ENCRYPT_MODE, sessionKey);
                        byte[] encryptedBytes = cipherPwd.doFinal(messageToBytes);

                        sendCryptedObject(new BasicMessage.RegisterMessage(infos[1], encryptedBytes, MsgTypes.ADMIN_LOGIN_PASSWORD_TYPE));  
                    } else if (Integer.parseInt(infos[0]) == MsgTypes.GET_WAITING_REGISTRATIONS) {      //15
                        sendCryptedObject(new BasicMessage.DisconnectionMessage(Integer.parseInt(line.substring(3)), 
                                MsgTypes.GET_WAITING_REGISTRATIONS));
                        ArrayList<String> registrations;
                        registrations = (ArrayList) decryptData((SealedObject) in.readObject());
                        System.out.println("Registrations waiting list. \nSend a message to allow registrations. \n16 <id> <userLog ... userLog ...>");
                        for (String registration : registrations) {
                            System.out.println(registration);
                        }
                    } else if (Integer.parseInt(infos[0]) == MsgTypes.ALLOW_USER_REGISTRATIONS) {       //16
                        ArrayList<String> registrations = new ArrayList<>();
                        for(int i = 2; i < infos.length; i++){
                            registrations.add(infos[i]);
                        }
                        sendCryptedObject(new BasicMessage.AllowRegistrationsMessage(Integer.parseInt(infos[1]),    
                                registrations, MsgTypes.ALLOW_USER_REGISTRATIONS));
                    } else if (Integer.parseInt(infos[0]) == MsgTypes.GET_INFOS_ALL_PATROLS) {
                        sendCryptedObject(new BasicMessage.TextMessage(line.substring(2), MsgTypes.GET_INFOS_ALL_PATROLS));
                    } else if (Integer.parseInt(infos[0]) == MsgTypes.SET_INFOS_PATROL) {
                        sendCryptedObject(new BasicMessage.TextMessage(line.substring(2), MsgTypes.SET_INFOS_PATROL));
                    } else if (Integer.parseInt(infos[0]) == MsgTypes.DELETE_SERVER_INFOS) {
                        sendCryptedObject(new BasicMessage.DisconnectionMessage(Integer.parseInt(line.substring(3)), MsgTypes.DELETE_SERVER_INFOS));
                    } else if (Integer.parseInt(infos[0]) == MsgTypes.GOUVERNEMENT_LOGIN_PASSWORD_TYPE) {
                        //encrypte password
                        
                        byte[] messageToBytes = infos[2].getBytes();
                        Cipher cipherPwd = Cipher.getInstance("AES");
                        cipherPwd.init(Cipher.ENCRYPT_MODE, sessionKey);
                        byte[] encryptedBytes = cipherPwd.doFinal(messageToBytes);

                        sendCryptedObject(new BasicMessage.RegisterMessage(infos[1], encryptedBytes, MsgTypes.GOUVERNEMENT_LOGIN_PASSWORD_TYPE));  
                    } else if (Integer.parseInt(infos[0]) == MsgTypes.AUTHORISE_ADMIN_ERASE) {
                        sendCryptedObject(new BasicMessage.DisconnectionMessage(Integer.parseInt(line.substring(3)), MsgTypes.AUTHORISE_ADMIN_ERASE));
                    } else {
                        known = false;
                    }
                } catch (NumberFormatException ex) {
                    known = false;
                }

                if (known) {
                    System.out.println((String) decryptData((SealedObject) in.readObject()));
                } else {
                    System.out.println("Unkown command");
                }
            } while (!line.equals("exit"));

            sendCryptedObject(BasicMessage.EXIT_MSG);
        } catch (IOException | ClassNotFoundException | InvalidKeyException | NoSuchAlgorithmException 
                | SignatureException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException ex) {
        }
    }

    private byte[] doHash(byte[] hash, int nb) {
        for (int i = 0; i < nb; i++) {
            try {
                //Creating the MessageDigest object
                MessageDigest md = MessageDigest.getInstance("SHA-256");

                //Passing data to the created MessageDigest Object
                md.update(hash);

                //Compute the message digest
                hash = md.digest();
            } catch (NoSuchAlgorithmException ex) {
            }
        }
        return hash;
    }
    
    private void sendCryptedObject(Object object){
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, sessionKey);
            SealedObject sealedObject = new SealedObject((Serializable) object, cipher);
            out.writeObject(sealedObject);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException
                | IOException | IllegalBlockSizeException ex) {
        }
    }
    
    private Object decryptData(SealedObject sealedObject) {
        try {
                Cipher cipher = Cipher.getInstance("AES");
                cipher.init(Cipher.DECRYPT_MODE, sessionKey);
                return sealedObject.getObject(cipher);
            } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException 
                    | IOException | ClassNotFoundException | IllegalBlockSizeException | BadPaddingException ex) {
            }
            return null;
    }
}
