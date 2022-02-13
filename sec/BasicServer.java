package sec;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.io.IOException;
import java.io.EOFException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Arrays;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SealedObject;
import javax.crypto.SecretKey;

import sec.BasicMessage;

public class BasicServer {

    private ServerSocket serv_socket;
    private boolean running;
    private Integer connections = 0;

    private ArrayList<BasicMessageHandler> handlers;

    public BasicServer(int listeningPort) throws IOException {
        serv_socket = new ServerSocket(listeningPort);
        running = false;
        this.handlers = new ArrayList<>();
    }

    public boolean registerHandler(BasicMessageHandler handler) {
        return handlers.add(handler);
    }

    public void start() throws IOException {
        System.out.println("Server starting on port "
                + serv_socket.getLocalPort());
        running = true;

        while (running) {
            if (connections <= 1000) {
                Socket socket = serv_socket.accept();
                System.out.println("New connection from client");
                new BasicServerThread(socket).start();
            }
        }
    }

    public void stop() {
        running = false;
    }

    public interface BasicMessageHandler {

        public void handle(BasicMessage m, ObjectInputStream in, ObjectOutputStream out, SecretKey sessionKey);
    }

    private class BasicServerThread extends Thread {

        protected Socket toClientSocket;
        protected ObjectInputStream messageReader;
        protected ObjectOutputStream messageWriter;
        private PrivateKey privateKey;
        private PublicKey clientPublicKey;
        private SecretKey sessionKey;
        private byte[] seed;
        private byte[] hash;
        private byte[] hashClient;
        private SecureRandom random;
        private Challenge challenge;
        private int randomInt;
        private int response;

        private byte[] doHash(byte[] seed, int nbHash) {
            for (int i = 0; i < nbHash; i++) {
                try {
                    //Creating the MessageDigest object
                    MessageDigest md = MessageDigest.getInstance("SHA-256");

                    //Passing data to the created MessageDigest Object
                    md.update(seed);

                    //Compute the message digest
                    seed = md.digest();
                } catch (NoSuchAlgorithmException ex) {
                }
            }
            return seed;
        }

        private void sendCryptedObject(Object object) {
            try {
                Cipher cipher = Cipher.getInstance("AES");
                cipher.init(Cipher.ENCRYPT_MODE, sessionKey);
                SealedObject sealedObject = new SealedObject((Serializable) object, cipher);
                messageWriter.writeObject(sealedObject);
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

        public BasicServerThread(Socket socket) throws IOException {
            try {
                setClientSocket(socket);
                KeyPairGenerator generatorInit = KeyPairGenerator.getInstance("RSA");
                generatorInit.initialize(2048);
                KeyPair pairInit = generatorInit.generateKeyPair();
                messageWriter.writeObject(pairInit.getPublic());
                privateKey = pairInit.getPrivate();
                Cipher sessionCipher = Cipher.getInstance("RSA");
                sessionCipher.init(Cipher.UNWRAP_MODE, privateKey);
                sessionKey = (SecretKey) sessionCipher.unwrap((byte[]) messageReader.readObject(), "AES", Cipher.SECRET_KEY);

                random = SecureRandom.getInstance("SHA1PRNG");
                seed = random.generateSeed(8);
                sendCryptedObject(seed);
                byte[] hashTmp = new byte[seed.length];
                System.arraycopy(seed, 0, hashTmp, 0, seed.length);
                hash = doHash(hashTmp, 42);
            } catch (NoSuchAlgorithmException | NoSuchPaddingException | ClassNotFoundException | InvalidKeyException ex) {
            }
        }

        private void setClientSocket(Socket clientSocket) throws IOException {
            this.toClientSocket = clientSocket;
            this.messageReader = new ObjectInputStream(clientSocket.getInputStream());
            this.messageWriter = new ObjectOutputStream(clientSocket.getOutputStream());
        }

        @Override
        public void run() {
            try {
                boolean exit = false;
                int cmpHash = 1;
                connections++;
                System.out.println(connections);
                try {
                    random = SecureRandom.getInstance("SHA1PRNG");
                } catch (NoSuchAlgorithmException ex) {
                }
                clientPublicKey = (PublicKey) messageReader.readObject();

                //public key signature
                byte[] message = (byte[]) messageReader.readObject();
                byte[] sigBytes = (byte[]) messageReader.readObject();
                Signature signature1 = Signature.getInstance("SHA1withRSA");
                signature1.initVerify(clientPublicKey);
                signature1.update(message);

                boolean result = signature1.verify(sigBytes);
                System.out.println("signature receive");

                //first cipher challenge
                randomInt = random.nextInt();
                Cipher cipherInit = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                cipherInit.init(Cipher.ENCRYPT_MODE, clientPublicKey);
                byte[] toEncryptInit = ByteBuffer.allocate(4).putInt(randomInt).array();
                sendCryptedObject(new Challenge(0, cipherInit.doFinal(toEncryptInit)));
                while (!exit) {
                    try {
                        challenge = (Challenge) decryptData((SealedObject) messageReader.readObject());
                        response = challenge.getResponse();

                        //Lamport OTP
                        hashClient = (byte[]) decryptData((SealedObject) messageReader.readObject());
                        byte[] hashTmp = new byte[seed.length];
                        System.arraycopy(seed, 0, hashTmp, 0, seed.length);
                        if (cmpHash == 42) {
                            cmpHash = 1;
                        }
                        hashTmp = doHash(hashTmp, 42 - cmpHash);
                        cmpHash++;

                        if (result && Arrays.equals(hashClient, hashTmp) && randomInt + 1 == response) {
                            BasicMessage m = (BasicMessage) decryptData((SealedObject) messageReader.readObject());
                            System.out.println("Message of type " + m.getType() + " read");
                            if (m.getType() != 0) //type-0 message = exit messages
                            {
                                for (BasicMessageHandler h : handlers) {
                                    h.handle(m, messageReader, messageWriter, sessionKey);
                                }
                            } else {
                                exit = true;
                            }
                        } else {
                            sendCryptedObject("you can't comunicate with the server bye bye");
                        }
                    } catch (EOFException ex) {
                        System.out.println("Connection brutally interrupted, closing session");
                        exit = true;
                    } catch (IOException | ClassNotFoundException ex) {
                        System.out.println("Unknown connection error, closing session");
                        exit = true;
                    }
                }
                connections--;
            } catch (IOException | ClassNotFoundException | InvalidKeyException
                    | NoSuchAlgorithmException | SignatureException | BadPaddingException
                    | IllegalBlockSizeException | NoSuchPaddingException ex) {
            }
        }
    }
}
