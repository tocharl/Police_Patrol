package sec;

import java.io.Serializable;

public class Challenge implements Serializable{
    
    private final int response;
    private final byte[] digest;

    public Challenge(int response, byte[] digest) {
        this.response = response;
        this.digest = digest;
    }

    public int getResponse() {
        return response;
    }

    public byte[] getDigest() {
        return digest;
    }
}
