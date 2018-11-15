package gitlet;
import java.io.Serializable;

/**
 * This class represents a Branch object.
 * @author Adish Jain
 */
public class Branch implements Serializable {

    /**
     * Constructor.
     * @param name String
     * @param node Commit
     */
    public Branch(String name, Commit node) {
        _name = name;
        _node = node.getID();
    }

    /**
     * Returns name of branch.
     * @return String
     */
    String getName() {
        return _name;
    }

    /**
     * Returns SHA id of commit object
     * which branch points to.
     * @return String
     */
    String getNode() {
        return _node;
    }

    /**
     * Name of branch.
     */
    private String _name;

    /**
     * SHA id of commit object
     * to which branch points.
     */
    private String _node;
}
