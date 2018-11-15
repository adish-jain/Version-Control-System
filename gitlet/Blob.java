package gitlet;

import java.io.Serializable;

/**
 * This class represents a Blob object.
 * @author Adish Jain
 */
public class Blob implements Serializable {

    /**
     * Constructor.
     * @param name String
     * @param fileContent byte[]
     */
    public Blob(String name, byte[] fileContent) {
        content = fileContent;
        _fileName = name;
        _id = Utils.sha1(Utils.serialize(this));
    }

    /**
     * Returns content of file.
     * @return byte[]
     */
    byte[] getContent() {
        return content;
    }

    /**
     * Returns file name.
     * @return String
     */
    String getName() {
        return _fileName;
    }

    /**
     * Returns SHA id of file.
     * @return String
     */
    String getID() {
        return _id;
    }

    /**
     * The content of our file represented as a byte array.
     */
    private byte[] content;

    /**
     * The unique SHA-id of our blob object.
     */
    private String _id;

    /**
     * The file name.
     */
    private String _fileName;
}
