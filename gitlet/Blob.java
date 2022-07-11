package gitlet;

import java.io.Serializable;

public class Blob implements Serializable {
    /**
     * File Date.
     */
    private byte[] _fileData;

    public Blob(byte[] fileData) {
        _fileData = fileData;
    }
}
