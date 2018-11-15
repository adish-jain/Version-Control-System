package gitlet;

import java.time.LocalDateTime;
import java.io.Serializable;
import java.util.HashMap;

/**
 * This class represents a Commit object.
 * @author Adish Jain
 */
public class Commit implements Serializable {

    /**
     * Constructor.
     * @param message String
     * @param timestamp LocalDateTie
     * @param parent String
     * @param parent2 String
     * @param blobs HashMap
     */
    public Commit(String message, LocalDateTime timestamp,
                  String parent, String parent2,
                  HashMap<String, String> blobs) {
        if (message.equals("") || message == null) {
            throw new
                    IllegalArgumentException("Please enter a commit message.");
        }
        _message = message;
        _timestamp = timestamp;
        _parent = parent;
        _parent2 = parent2;
        _blobs = blobs;
        _id = Utils.sha1(Utils.serialize(this));
    }

    /**
     * gets the commit message.
     * @return String
     */
    String getMessage() {
        return _message;
    }

    /**
     * gets the date/time stamp of commit.
     * @return LocalDateTime
     */
    LocalDateTime getTimeStamp() {
        return _timestamp;
    }

    /**
     * gets the first parent.
     * @return string
     */
    String getParent() {
        return _parent;
    }

    /**
     * gets the second parent (for merge commits).
     * @return string
     */
    String getParent2() {
        return _parent2;
    }

    /**
     * gets the blobmap.
     * @return HashMap
     */
    HashMap<String, String> getBlobs() {
        return _blobs;
    }

    /**
     * Gets the ID.
     * @return string
     */
    String getID() {
        return _id;
    }


    /**
     * commit message.
     */
    private String _message;

    /**
     * time/date commit was made.
     */
    private LocalDateTime _timestamp;

    /**
     * SHA id of first parent of commit.
     */
    private String _parent;

    /**
     * SHA id of second parent of commit (only for merge commits).
     */
    private String _parent2;

    /**
     * The blobmap of this commit object
     * (name of blob (file name) --> SHA ID of blob).
     */
    private HashMap<String, String> _blobs;

    /**
     * The SHA id of this commit object.
     */
    private String _id;
}
