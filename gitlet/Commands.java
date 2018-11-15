package gitlet;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * The class which reads and interprets commands given by user.
 * @author Adish Jain
 */
public class Commands {

    /**
     * Constructor.
     * @param args String[]
     */
    public Commands(String[] args) {
        if (args.length == 0) {
            _command = "nothing entered";
        } else {
            _command = args[0];
            if ((Arrays.copyOfRange(args, 1, args.length)).length > 0) {
                _operands = Arrays.copyOfRange(args, 1, args.length);
            }
        }
    }

    /**
     * Identifies which command has been given.
     */
    void doCommand() {
        switch (_command) {
        case "init":
            initCommand();
            break;
        case "add":
            addCommand(_operands);
            break;
        case "commit":
            commitCommand(_operands);
            break;
        case "rm":
            removeCommand(_operands);
            break;
        case "log":
            logCommand();
            break;
        case "global-log":
            globalLogCommand();
            break;
        case "find":
            findCommand(_operands);
            break;
        case "status":
            statusCommand();
            break;
        case "checkout":
            checkoutCommand(_operands);
            break;
        case "branch":
            branchCommand(_operands);
            break;
        case "rm-branch":
            removeBranchCommand(_operands);
            break;
        case "reset":
            resetCommand(_operands);
            break;
        case "merge":
            mergeCommand(_operands);
            break;
        case "nothing entered":
            System.out.println("Please enter a command.");
            System.exit(0);
            break;
        default:
            System.out.println("No command with that name exists.");
            System.exit(0);
        }
    }

    /**
     * Initializes a Gitlet repository in current directory.
     */
    void initCommand() {
        if (!hidden.exists()) {
            hidden.mkdir();
            stage.mkdir();
            blobs.mkdir();
            LocalDateTime firstTimeStamp =
                    LocalDateTime.ofEpochSecond(0L, 0, ZoneOffset.UTC);
            initial = new Commit("initial commit",
                    firstTimeStamp, "", "",
                    new HashMap<String, String>());
            Branch head = new Branch("master", initial);
            branchTracker.put(head.getName(), head.getNode());
            Utils.writeObject(branchTrack, branchTracker);
            Utils.writeObject(unstage, blobsToUnstage);
            Utils.writeObject(initialCommit, initial);
            commits.mkdir();
            headCommit.mkdir();
            Utils.writeObject(new File(headCommit
                    + java.io.File.separator
                    + initial.getID()), initial);
            branches.mkdir();
            headBranch.mkdir();
            Utils.writeObject(new File(headBranch
                    + java.io.File.separator
                    + Utils.sha1(Utils.serialize(head))), head);
        } else {
            System.out.println("A Gitlet version-control "
                    + "system already exists in the current directory.");
            System.exit(0);
        }
    }

    /**
     * Stages a file (a blob).
     * @param operands String[]
     */
    void addCommand(String[] operands) {
        if (!hidden.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        } else if (operands.length != 1) {
            System.out.println(operands.length);
            System.out.println("Incorrect operands.");
            System.exit(0);
        } else if (!new File(operands[0]).exists()) {
            System.out.println("File does not exist.");
            System.exit(0);
        } else {
            Blob file = new Blob(operands[0],
                    Utils.readContents(
                            new File(operands[0])));
            Commit head = Utils.readObject(
                    headCommit.listFiles()[0], Commit.class);
            if (!head.getBlobs().containsValue(file.getID())
                    || !head.getBlobs().containsKey(file.getName())) {
                Utils.writeObject(new File(stage
                        + java.io.File.separator + file.getID()), file);
                Utils.writeObject(new File(blobs
                        + java.io.File.separator + file.getID()), file);
            } else if (inStaged(operands[0])) {
                for (File f : stage.listFiles()) {
                    Blob newBlob = Utils.readObject(f, Blob.class);
                    if (newBlob.getName().equals(operands[0])) {
                        f.delete();
                    }
                }
            }
            blobsToUnstage = Utils.readObject(unstage, HashSet.class);
            if (!blobsToUnstage.isEmpty()) {
                if (blobsToUnstage.contains(file.getName())) {
                    blobsToUnstage.remove(file.getName());
                }
                Utils.writeObject(unstage, blobsToUnstage);
            }
        }
    }

    /**
     * Creates a new commit object
     * and commits blobs in stage.
     * @param operands String[]
     */
    void commitCommand(String[] operands) {
        if (!hidden.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        } else if (operands.length != 1) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        } else if (operands[0].isEmpty()
                || operands[0].equals("")) {
            System.out.println("Please enter a commit message.");
            System.exit(0);
        } else if (stage.listFiles().length == 0
                && Utils.readObject(unstage, HashSet.class).isEmpty()) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        } else {
            Commit parent = Utils.readObject(
                    headCommit.listFiles()[0], Commit.class);
            HashMap<String, String> parentBlobs = parent.getBlobs();
            blobsToUnstage = Utils.readObject(unstage, HashSet.class);
            parentBlobs.keySet().removeAll(blobsToUnstage);
            Commit newNode = new Commit(operands[0],
                    LocalDateTime.now(), parent.getID(), "", parentBlobs);

            branchTracker = Utils.readObject(
                    branchTrack, HashMap.class);
            Branch newBranch = new Branch(
                    Utils.readObject(headBranch.listFiles()[0],
                            Branch.class).getName(), newNode);
            branchTracker.put(
                    newBranch.getName(), newBranch.getNode());

            for (File f : stage.listFiles()) {
                Blob newBlob = Utils.readObject(f, Blob.class);
                if (!blobsToUnstage.contains(newBlob.getName())) {
                    newNode.getBlobs().put(newBlob.getName(), newBlob.getID());
                }
                f.delete();
            }
            blobsToUnstage.clear();

            File tempCommit = headCommit.listFiles()[0];
            tempCommit.renameTo(new File(commits
                    + File.separator + tempCommit.getName()));

            headBranch.listFiles()[0].delete();
            Utils.writeObject(new File(headCommit
                    + java.io.File.separator + newNode.getID()), newNode);
            Utils.writeObject(new File(headBranch
                    + java.io.File.separator
                            + Utils.sha1(Utils.serialize(newBranch))),
                    newBranch);
            Utils.writeObject(unstage, blobsToUnstage);
            Utils.writeObject(branchTrack, branchTracker);
        }
    }

    /**
     * Removes a blob from WD and marks it
     * to be untracked by next commit.
     * @param operands String[]
     */
    void removeCommand(String[] operands) {
        if (!hidden.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        } else if (operands.length != 1) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        } else if (!Utils.readObject(headCommit.listFiles()[0],
                Commit.class).getBlobs().containsKey(operands[0])
                    && !inStaged(operands[0])) {
            System.out.println("No reason to remove the file.");
            System.exit(0);
        } else {
            HashMap<String, String> headBlobs =
                    Utils.readObject(headCommit.listFiles()[0],
                            Commit.class).getBlobs();
            if (headBlobs.containsKey(operands[0])) {
                blobsToUnstage = Utils.readObject(unstage, HashSet.class);
                new File(operands[0]).delete();
                blobsToUnstage.add(operands[0]);
                Utils.writeObject(unstage, blobsToUnstage);
            }
            if (inStaged(operands[0])) {
                for (File f : stage.listFiles()) {
                    Blob newBlob = Utils.readObject(f, Blob.class);
                    if (newBlob.getName().equals(operands[0])) {
                        f.delete();
                    }
                }
            }
        }
    }

    /**
     * Outputs the commit history.
     */
    void logCommand() {
        if (!hidden.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        } else {
            Commit curr = Utils.readObject(
                    headCommit.listFiles()[0], Commit.class);
            while (true) {
                System.out.println("===");
                System.out.println("commit " +  curr.getID());
                if (!curr.getParent2().equals("")) {
                    System.out.println("Merge: "
                            + curr.getParent().substring(0, 7)
                            + " " + curr.getParent2().substring(0, 7));
                }
                LocalDateTime date = curr.getTimeStamp();
                DateTimeFormatter formatter =
                        DateTimeFormatter.ofPattern("E MMM d HH:mm:ss YYYY");
                System.out.println("Date: "
                        + date.format(formatter) + " -0800");
                System.out.println(curr.getMessage());
                System.out.println();
                if (findCommitWithParent(curr.getParent()) == null) {
                    return;
                }
                curr = findCommitWithParent(curr.getParent());
            }
        }
    }

    /**
     * Outputs commit history of all
     * commits ever made.
     */
    void globalLogCommand() {
        if (!hidden.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
        for (int i = 0; i < commits.listFiles().length; i++) {
            if (commits.listFiles()[i].isDirectory()) {
                Commit curr = Utils.readObject(
                        headCommit.listFiles()[0], Commit.class);
                System.out.println("===");
                System.out.println("commit " + curr.getID());
                if (!curr.getParent2().equals("")) {
                    System.out.println("Merge: "
                            + curr.getParent().substring(0, 7)
                            + " " + curr.getParent2().substring(0, 7));
                }
                LocalDateTime date = curr.getTimeStamp();
                DateTimeFormatter formatter =
                        DateTimeFormatter.ofPattern("E MMM d HH:mm:ss YYYY");
                System.out.println("Date: "
                        + date.format(formatter) + " -0800");
                System.out.println(curr.getMessage());
                System.out.println();
            } else {
                Commit curr = Utils.readObject(
                        commits.listFiles()[i], Commit.class);
                System.out.println("===");
                System.out.println("commit " + curr.getID());
                if (!curr.getParent2().equals("")) {
                    System.out.println("Merge: "
                            + curr.getParent().substring(0, 8)
                            + curr.getParent2().substring(0, 8));
                }
                LocalDateTime date = curr.getTimeStamp();
                DateTimeFormatter formatter =
                        DateTimeFormatter.ofPattern("E MMM d HH:mm:ss YYYY");
                System.out.println("Date: "
                        + date.format(formatter) + " -0800");
                System.out.println(curr.getMessage());
                System.out.println();
            }
        }
    }

    /**
     * Finds the commit(s) with given
     * commit message.
     * @param operands String[]
     */
    void findCommand(String[] operands) {
        if (!hidden.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        } else if (operands.length != 1) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        } else {
            boolean noneExist = true;

            for (int i = 0; i < commits.listFiles().length; i++) {
                if (commits.listFiles()[i].isDirectory()) {
                    if (Utils.readObject(headCommit.listFiles()[0],
                            Commit.class).getMessage().equals(operands[0])) {
                        System.out.println(Utils.readObject(
                                headCommit.listFiles()[0],
                                Commit.class).getID());
                        noneExist = false;
                    }
                } else {
                    if (Utils.readObject(commits.listFiles()[i],
                            Commit.class).getMessage().equals(operands[0])) {
                        System.out.println(Utils.readObject(
                                commits.listFiles()[i], Commit.class).getID());
                        noneExist = false;
                    }
                }
            }
            if (noneExist) {
                System.out.println("Found no commit with that message.");
                System.exit(0);
            }
        }
    }

    /**
     * Outputs the status of our Gitlet repository.
     */
    void statusCommand() {
        if (!hidden.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
        branchTracker =
                Utils.readObject(branchTrack, HashMap.class);
        System.out.println("=== Branches ===");
        System.out.println("*" + Utils.readObject(
                headBranch.listFiles()[0], Branch.class).getName());
        for (String str : branchTracker.keySet()) {
            if (!str.equals(Utils.readObject(
                    headBranch.listFiles()[0], Branch.class).getName())) {
                System.out.println(str);
            }
        }
        System.out.println();
        System.out.println("=== Staged Files ===");
        for (File f : stage.listFiles()) {
            Blob newBlob = Utils.readObject(f, Blob.class);
            System.out.println(newBlob.getName());
        }
        System.out.println();

        System.out.println("=== Removed Files ===");
        blobsToUnstage = Utils.readObject(unstage, HashSet.class);
        if (!blobsToUnstage.isEmpty()) {
            for (String str : blobsToUnstage) {
                System.out.println(str);
            }
        }
        System.out.println();

        System.out.println("=== Modifications "
                + "Not Staged For Commit ===");
        System.out.println();

        System.out.println("=== Untracked Files ===");
        System.out.println();

    }

    /**
     * This command has three cases.
     * (1) Checks out the given file from
     * most recent commit.
     * (2) Checks out the given file from
     * the given commit.
     * (3) Checks out all files from the
     * given branch.
     * @param operands String[]
     */
    void checkoutCommand(String[] operands) {
        if (!hidden.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        } else if (operands.length > 3) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        } else {
            if (operands.length == 2) {
                checkoutCase1(operands);
            } else if (operands.length == 3) {
                checkoutCase2(operands);
            } else if (operands.length == 1) {
                checkoutCase3(operands);
            }
        }
    }

    /**
     * First case of checkout.
     * @param operands String[]
     */
    void checkoutCase1(String[] operands) {
        if (!operands[0].equals("--")) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        Commit head = Utils.readObject(
                headCommit.listFiles()[0], Commit.class);
        if (!head.getBlobs().containsKey(operands[1])) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        } else {
            Blob headBlob =
                    Utils.readObject(new File(blobs
                                    + java.io.File.separator
                                    + head.getBlobs().get(operands[1])),
                            Blob.class);
            Utils.writeContents(new File("."
                            + java.io.File.separator + operands[1]),
                    headBlob.getContent());
        }
    }

    /**
     * Second case of checkout.
     * @param operands String[]
     */
    void checkoutCase2(String[] operands) {
        if (!operands[1].equals("--")) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        Commit given = findCommitWithID(operands[0]);
        if (given == null) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        } else if (!given.getBlobs().containsKey(operands[2])) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        } else {
            Blob givenBlob = Utils.readObject(new File(
                            blobs + java.io.File.separator
                                    + given.getBlobs().get(operands[2])),
                    Blob.class);
            Utils.writeContents(new File("."
                            + java.io.File.separator + operands[2]),
                    givenBlob.getContent());
        }
    }

    /**
     * Third case of checkout.
     * @param operands String[]
     */
    void checkoutCase3(String[] operands) {
        Commit given = null;
        branchTracker =
                Utils.readObject(branchTrack, HashMap.class);
        if (!branchTracker.containsKey(operands[0])) {
            System.out.println("No such branch exists.");
            System.exit(0);
        } else if ((Utils.readObject(headBranch.listFiles()[0],
                Branch.class).getName().equals(operands[0]))) {
            System.out.println("No need to "
                    + "checkout the current branch.");
            System.exit(0);
        } else {
            initial = Utils.readObject(
                    initialCommit, Commit.class);
            if (branchTracker.get(
                    operands[0]).equals(initial.getID())) {
                given = initial;
            } else {
                if (!isTrackedGivenBranch(operands[0])) {
                    System.out.println("There is an untracked file "
                            + "in the way; delete it or add it first.");
                    System.exit(0);
                }
                given = Utils.readObject(new File(commits
                                + java.io.File.separator
                                + branchTracker.get(operands[0])),
                        Commit.class);
            }
        }

        HashMap<String, String> allBlobs = given.getBlobs();
        for (String blobName : allBlobs.keySet()) {
            Blob aBlob = Utils.readObject(new File(blobs
                    + java.io.File.separator
                    + allBlobs.get(blobName)), Blob.class);
            Utils.writeContents(new File("."
                            + java.io.File.separator + blobName),
                    aBlob.getContent());
        }
        headBranch.listFiles()[0].delete();
        Branch theBranch = new Branch(operands[0], given);
        Utils.writeObject(new File(headBranch
                + java.io.File.separator
                + Utils.sha1(Utils.serialize(theBranch))), theBranch);
        for (File f : stage.listFiles()) {
            f.delete();
        }

        removeFromWDGivenBranch(operands[0], given);

        File tempCommit = headCommit.listFiles()[0];
        tempCommit.renameTo(new File(commits
                + java.io.File.separator + tempCommit.getName()));
        Utils.writeObject(new File(headCommit
                + java.io.File.separator + given.getID()), given);
        new File(commits
                + java.io.File.separator + given.getID()).delete();
    }

    /**
     * Creates a new branch object.
     * @param operands String[]
     */
    void branchCommand(String[] operands) {
        if (!hidden.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        } else if (operands.length != 1) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        } else {
            branchTracker =
                    Utils.readObject(branchTrack, HashMap.class);
            if (branchTracker.containsKey(operands[0])) {
                System.out.println("A branch with that name already exists.");
                System.exit(0);
            } else {
                Branch br = new Branch(operands[0],
                        Utils.readObject(headCommit.listFiles()[0],
                                Commit.class));
                branchTracker.put(br.getName(), br.getNode());
                Utils.writeObject(branchTrack, branchTracker);
            }
        }
    }

    /**
     * Removes branch object.
     * @param operands String[]
     */
    void removeBranchCommand(String[] operands) {
        if (!hidden.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        } else if (operands.length != 1) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        } else {
            branchTracker =
                    Utils.readObject(branchTrack, HashMap.class);
            if (!branchTracker.containsKey(operands[0])) {
                System.out.println("A branch with "
                        + "that name does not exist.");
                System.exit(0);
            } else if (Utils.readObject(headBranch.listFiles()[0],
                    Branch.class).getName().equals(operands[0])) {
                System.out.println("Cannot remove the current branch.");
                System.exit(0);
            } else {
                branchTracker.remove(operands[0]);
                Utils.writeObject(branchTrack, branchTracker);
            }
        }
    }

    /**
     * Checks out all the files
     * tracked by the given commit.
     * @param operands String[]
     */
    void resetCommand(String[] operands) {
        if (!hidden.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        } else if (operands.length != 1) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        Commit given = findCommitWithID(operands[0]);
        if (given == null) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        } else if (!isTrackedGivenCommit(operands[0])) {
            System.out.println("There is an untracked "
                    + "file in the way; "
                    + "delete it or add it first.");
            System.exit(0);
        } else {
            HashMap<String, String> allBlobs = given.getBlobs();
            for (String blobName : allBlobs.keySet()) {
                Blob aBlob = Utils.readObject(
                        new File(blobs
                                + java.io.File.separator
                                + allBlobs.get(blobName)),
                        Blob.class);
                Utils.writeContents(new File(
                        "." + java.io.File.separator
                                + aBlob.getName()),
                        aBlob.getContent());
            }
            branchTracker =
                    Utils.readObject(branchTrack, HashMap.class);
            removeFromWDGivenCommit(operands[0]);
            Branch currHeadBranch =
                    Utils.readObject(
                            headBranch.listFiles()[0], Branch.class);
            headBranch.listFiles()[0].delete();
            Branch newHeadBranch =
                    new Branch(currHeadBranch.getName(), given);
            branchTracker.put(newHeadBranch.getName(),
                    newHeadBranch.getNode());
            Utils.writeObject(new File(headBranch
                    + java.io.File.separator
                    + Utils.sha1(Utils.serialize(newHeadBranch))),
                    newHeadBranch);
            Utils.writeObject(branchTrack, branchTracker);

            File tempCommit = headCommit.listFiles()[0];
            tempCommit.renameTo(new File(commits
                    + java.io.File.separator
                    + tempCommit.getName()));
            Utils.writeObject(new File(headCommit
                    + java.io.File.separator + given.getID()), given);
            new File(commits
                    + java.io.File.separator + given.getID()).delete();
            for (File f : stage.listFiles()) {
                f.delete();
            }
        }
    }

    /**
     * Merges files from given branch
     * into the current branch.
     * @param operands String[]
     */
    void mergeCommand(String[] operands) {
        if (!hidden.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        } else if (operands.length != 1) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        } else if (stage.listFiles().length > 0
                || !Utils.readObject(unstage, HashSet.class).isEmpty()) {
            System.out.println("You have uncommitted changes.");
            System.exit(0);
        }
        if (!Utils.readObject(branchTrack,
                HashMap.class).containsKey(operands[0])) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        } else if (operands[0].equals(Utils.readObject(
                headBranch.listFiles()[0], Branch.class).getName())) {
            System.out.println("Cannot merge a branch with itself.");
            System.exit(0);
        } else if (!isTrackedGivenBranch(operands[0])) {
            System.out.println("There is an untracked file in "
                    + "the way; delete it or add it first.");
            System.exit(0);
        } else {
            Commit splitPoint = findSplitPoint(operands);
            Commit given = Utils.readObject(
                    new File(commits + java.io.File.separator
                            + Utils.readObject(branchTrack,
                            HashMap.class).get(operands[0])), Commit.class);
            Commit head = (Utils.readObject(
                    headCommit.listFiles()[0], Commit.class));

            mergeCase1(operands, splitPoint, given, head);

            headFiles = (HashMap) head.getBlobs().clone();
            givenFiles = (HashMap) given.getBlobs().clone();
            splitPointFiles = (HashMap) splitPoint.getBlobs().clone();
            splitPointFiles.keySet().removeAll(
                        headFiles.keySet());
            givenFiles.values().retainAll(
                        splitPointFiles.values());
            HashMap<String, String>
                    notModifiedInGivenSinceSplitAndNotInHead = givenFiles;
            noConflict.addAll(
                    notModifiedInGivenSinceSplitAndNotInHead.keySet());

            mergeConflictCase(operands, splitPoint, given, head);
        }
    }

    /**
     * Creates a new merge commit.
     * @param operands String[] where first arg is
     *                 commit message and second arg is
     *                 second parent commit's ID
     * @param conflicts HashSet
     */
    void mergeCommitCommand(String[] operands, HashSet<String> conflicts) {
        if (stage.listFiles().length == 0
                && Utils.readObject(unstage,
                HashSet.class).isEmpty()) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        } else {
            Commit parent = Utils.readObject(
                    headCommit.listFiles()[0], Commit.class);
            Commit secondParent = Utils.readObject(new File(
                    commits + java.io.File.separator
                            + operands[1]), Commit.class);
            HashMap<String, String> parentBlobs = parent.getBlobs();
            blobsToUnstage = Utils.readObject(unstage, HashSet.class);
            parentBlobs.keySet().removeAll(blobsToUnstage);
            Commit newNode = new Commit(operands[0],
                    LocalDateTime.now(), parent.getID(),
                    secondParent.getID(), parentBlobs);

            branchTracker = Utils.readObject(branchTrack, HashMap.class);
            Branch newBranch = new Branch(Utils.readObject(
                    headBranch.listFiles()[0],
                    Branch.class).getName(), newNode);
            branchTracker.put(newBranch.getName(), newBranch.getNode());

            for (File f : stage.listFiles()) {
                Blob newBlob = Utils.readObject(f, Blob.class);
                if (!blobsToUnstage.contains(newBlob.getName())) {
                    newNode.getBlobs().put(
                            newBlob.getName(), newBlob.getID());
                }
                f.delete();
            }

            blobsToUnstage.clear();

            File tempCommit = headCommit.listFiles()[0];
            tempCommit.renameTo(new File(commits
                    + File.separator + tempCommit.getName()));

            headBranch.listFiles()[0].delete();
            Utils.writeObject(new File(headCommit
                    + java.io.File.separator + newNode.getID()), newNode);
            Utils.writeObject(new File(headBranch
                    + java.io.File.separator
                    + Utils.sha1(Utils.serialize(newBranch))), newBranch);
            Utils.writeObject(unstage, blobsToUnstage);
            Utils.writeObject(branchTrack, branchTracker);
            if (!conflicts.isEmpty()) {
                System.out.println("Encountered a merge conflict.");
            }
        }
    }

    /**
     * Finds the split point of a commit tree.
     * @param operands String[]
     * @return Commit
     */
    Commit findSplitPoint(String[] operands) {
        Commit splitPoint = null;
        Commit given = Utils.readObject(
                new File(commits + java.io.File.separator
                        + Utils.readObject(branchTrack,
                        HashMap.class).get(operands[0])), Commit.class);
        Commit head = (Utils.readObject(
                headCommit.listFiles()[0], Commit.class));
        HashSet<String> ancestorsOfHead = findAncestors(head);
        HashSet<String> ancestorsOfGiven = findAncestors(given);
        ancestorsOfHead.retainAll(ancestorsOfGiven);
        HashSet<String> ancestorsOfBoth = ancestorsOfHead;

        for (String id : ancestorsOfBoth) {
            Commit ancestralCommitWithID;
            if (!new File(commits
                    + java.io.File.separator + id).exists()) {
                ancestralCommitWithID = Utils.readObject(new File(
                        headCommit + java.io.File.separator
                                + id), Commit.class);
            } else {
                ancestralCommitWithID = Utils.readObject(new File(
                        commits + java.io.File.separator
                                + id), Commit.class);
            }
            if (splitPoint == null) {
                splitPoint = ancestralCommitWithID;
            } else if (splitPoint.getTimeStamp().isBefore(
                    ancestralCommitWithID.getTimeStamp())) {
                splitPoint = ancestralCommitWithID;
            }
        }
        return splitPoint;
    }

    /**
     * First case of merge.
     * @param operands String[]
     * @param splitPoint Commit
     * @param given Commit
     * @param head Commit
     */
    void mergeCase1(String[] operands, Commit splitPoint,
                    Commit given, Commit head) {
        if (splitPoint.getID().equals(given.getID())) {
            System.out.println("Given branch is an "
                    + "ancestor of the current branch.");
            System.exit(0);
        } else if (splitPoint.getID().equals(head.getID())) {
            headBranch.listFiles()[0].delete();
            Branch theBranch = new Branch(operands[0], given);
            Utils.writeObject(new File(headBranch
                    + java.io.File.separator
                    + Utils.sha1(Utils.serialize(theBranch))), theBranch);
            File tempCommit = headCommit.listFiles()[0];
            tempCommit.renameTo(new File(commits
                    + java.io.File.separator + tempCommit.getName()));
            Utils.writeObject(new File(headCommit
                    + java.io.File.separator + given.getID()), given);
            new File(commits + java.io.File.separator
                    + given.getID()).delete();
            System.out.println("Current branch fast-forwarded");
            System.exit(0);
        } else {
            mergeCase2(operands, splitPoint, given, head);
        }
    }

    /**
     * Second case of merge.
     * @param operands String[]
     * @param splitPoint Commit
     * @param given Commit
     * @param head Commit
     */
    void mergeCase2(String[] operands, Commit splitPoint,
                    Commit given, Commit head) {
        splitPointFiles = (HashMap) splitPoint.getBlobs().clone();
        headFiles = (HashMap) head.getBlobs().clone();
        givenFiles = (HashMap) given.getBlobs().clone();

        headFiles.values().retainAll(splitPointFiles.values());
        notModifiedInHeadSinceSplit = headFiles;

        givenFiles.keySet().retainAll(splitPointFiles.keySet());
        splitPointFiles.values().retainAll(givenFiles.values());
        givenFiles.values().removeAll(splitPointFiles.values());
        modifiedInGivenSinceSplit = givenFiles;

        notModifiedInHeadSinceSplit.values().retainAll(
                modifiedInGivenSinceSplit.values());
        HashMap<String, String>
                notModifiedInHeadButModifiedInGiven =
                notModifiedInHeadSinceSplit;

        for (String blobName
                : notModifiedInHeadButModifiedInGiven.keySet()) {
            noConflict.add(blobName);
            Blob aBlob = Utils.readObject(new File(
                            blobs + java.io.File.separator
                                    + notModifiedInHeadButModifiedInGiven.get(
                                    blobName)),
                    Blob.class);
            Utils.writeContents(new File("."
                            + java.io.File.separator + aBlob.getName()),
                    aBlob.getContent());
            Utils.writeObject(new File(stage
                    + java.io.File.separator + aBlob.getID()), aBlob);
        }
        mergeCase3(operands, splitPoint, given, head);
    }

    /**
     * Third case of merge.
     * @param operands String[]
     * @param splitPoint Commit
     * @param given Commit
     * @param head Commit
     */
    void mergeCase3(String[] operands, Commit splitPoint,
                    Commit given, Commit head) {
        headFiles = (HashMap) head.getBlobs().clone();
        HashMap<String, String> tempHeadFiles =
                (HashMap) head.getBlobs().clone();
        givenFiles = (HashMap) given.getBlobs().clone();
        splitPointFiles = (HashMap) splitPoint.getBlobs().clone();

        givenFiles.values().retainAll(splitPointFiles.values());
        notModifiedInGivenSinceSplit = givenFiles;

        splitPointFiles = (HashMap) splitPoint.getBlobs().clone();

        tempHeadFiles.values().retainAll(
                splitPointFiles.values());
        headFiles.keySet().removeAll(
                tempHeadFiles.keySet());
        modifiedInHeadSinceSplit = headFiles;

        modifiedInHeadSinceSplit.keySet().retainAll(
                notModifiedInGivenSinceSplit.keySet());
        noConflict.addAll(modifiedInHeadSinceSplit.keySet());

        mergeCase4thru7(operands, splitPoint, given, head);
    }

    /**
     * Cases 4 through 7 of merge.
     * @param operands String[]
     * @param splitPoint Commit
     * @param given Commit
     * @param head Commit
     */
    void mergeCase4thru7(String[] operands, Commit splitPoint,
                         Commit given, Commit head) {
        modifiedInHeadSinceSplit.values().retainAll(
                modifiedInGivenSinceSplit.values());
        noConflict.addAll(
                modifiedInHeadSinceSplit.keySet());

        headFiles = (HashMap) head.getBlobs().clone();
        givenFiles = (HashMap) given.getBlobs().clone();
        splitPointFiles = (HashMap) splitPoint.getBlobs().clone();
        headFiles.keySet().removeAll(splitPointFiles.keySet());
        headFiles.keySet().removeAll(givenFiles.keySet());
        noConflict.addAll(headFiles.keySet());

        headFiles = (HashMap) head.getBlobs().clone();
        givenFiles = (HashMap) given.getBlobs().clone();
        splitPointFiles = (HashMap) splitPoint.getBlobs().clone();
        givenFiles.keySet().removeAll(splitPointFiles.keySet());
        givenFiles.keySet().removeAll(headFiles.keySet());
        for (String blobName : givenFiles.keySet()) {
            noConflict.add(blobName);
            Blob aBlob = Utils.readObject(new File(blobs
                    + java.io.File.separator
                    + givenFiles.get(blobName)), Blob.class);
            Utils.writeContents(new File("."
                    + java.io.File.separator
                    + aBlob.getName()), aBlob.getContent());
            Utils.writeObject(new File(stage
                    + java.io.File.separator + aBlob.getID()), aBlob);
        }

        headFiles = (HashMap) head.getBlobs().clone();
        givenFiles = (HashMap) given.getBlobs().clone();
        splitPointFiles =
                (HashMap) splitPoint.getBlobs().clone();
        splitPointFiles.keySet().removeAll(
                givenFiles.keySet());
        headFiles.values().retainAll(
                splitPointFiles.values());
        notModifiedInHeadSinceSplit = headFiles;
        splitPointFiles.values().retainAll(
                notModifiedInHeadSinceSplit.values());
        for (String blobName : splitPointFiles.keySet()) {
            noConflict.add(blobName);
            Blob aBlob = Utils.readObject(new File(
                            blobs + java.io.File.separator
                                    + splitPointFiles.get(blobName)),
                    Blob.class);
            new File("." + java.io.File.separator
                    + aBlob.getName()).delete();
            blobsToUnstage.add(aBlob.getName());
        }
    }

    /**
     * Conflict case for merge.
     * @param operands String[]
     * @param splitPoint Commit
     * @param given Commit
     * @param head Commit
     */
    void mergeConflictCase(String[] operands, Commit splitPoint,
                           Commit given, Commit head) {
        headFiles = (HashMap) head.getBlobs().clone();
        givenFiles = (HashMap) given.getBlobs().clone();

        HashSet<String> conflictFiles = new HashSet<String>();
        conflictFiles.addAll(headFiles.keySet());
        conflictFiles.addAll(givenFiles.keySet());
        conflictFiles.removeAll(noConflict);

        for (String blobName : conflictFiles) {
            Blob headBlob = Utils.readObject(new File(
                    blobs + java.io.File.separator
                            + headFiles.get(blobName)), Blob.class);
            String strGivenBlobContent;
            if (givenFiles.get(blobName) != null) {
                Blob givenBlob = Utils.readObject(new File(blobs
                        + java.io.File.separator
                        + givenFiles.get(blobName)), Blob.class);
                byte[] givenBlobContent = Utils.readObject(
                        new File(blobs + java.io.File.separator
                                + givenFiles.get(blobName)),
                        Blob.class).getContent();
                strGivenBlobContent = new String(
                        givenBlobContent, StandardCharsets.UTF_8);
            } else {
                strGivenBlobContent = "";
            }
            byte[] headBlobContent = Utils.readObject(new File(
                            blobs + java.io.File.separator
                                    + headFiles.get(blobName)),
                    Blob.class).getContent();
            String strHeadBlobContent = new String(
                    headBlobContent, StandardCharsets.UTF_8);
            Utils.writeContents(new File("."
                            + java.io.File.separator + blobName),
                    "<<<<<<< HEAD\n" + strHeadBlobContent
                            + "=======\n" +  strGivenBlobContent
                            + ">>>>>>>\n");

            Blob aBlob = new Blob(blobName, Utils.serialize(
                    "<<<<<<< HEAD\n" + headBlobContent
                            + "=======\n" +  strGivenBlobContent
                            + ">>>>>>>\n"));

            Utils.writeObject(new File(blobs
                    + java.io.File.separator + aBlob.getID()), aBlob);
            Utils.writeObject(new File(stage
                    + java.io.File.separator + aBlob.getID()), aBlob);
        }

        Utils.writeObject(unstage, blobsToUnstage);
        mergeCommitCommand(new String[]{"Merged "
                + operands[0] + " into "
                + Utils.readObject(headBranch.listFiles()[0],
                Branch.class).getName()
                + ".", given.getID()}, conflictFiles);
    }

    /**
     * Checks whether given file
     * is in stageing area.
     * @param fileName String
     * @return boolean
     */
    boolean inStaged(String fileName) {
        for (File f : stage.listFiles()) {
            Blob newBlob = Utils.readObject(f, Blob.class);
            if (newBlob.getName().equals(fileName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Finds commit with given parent.
     * @param parentID String
     * @return Commit
     */
    Commit findCommitWithParent(String parentID) {
        for (int i = 0; i < commits.listFiles().length; i++) {
            if (commits.listFiles()[i].isDirectory()) {
                if (Utils.readObject(headCommit.listFiles()[0],
                        Commit.class).getID().equals(parentID)) {
                    return Utils.readObject(
                            headCommit.listFiles()[0], Commit.class);
                }
            } else {
                if (Utils.readObject(commits.listFiles()[i],
                        Commit.class).getID().equals(parentID)) {
                    return Utils.readObject(
                            commits.listFiles()[i], Commit.class);
                }
            }
        }
        return null;
    }

    /**
     * Finds commit with given SHA id.
     * @param commitID String
     * @return Commit
     */
    Commit findCommitWithID(String commitID) {
        for (int i = 0; i < commits.listFiles().length; i++) {
            if (commits.listFiles()[i].isDirectory()) {
                if (Utils.readObject(headCommit.listFiles()[0],
                        Commit.class).getID().substring(0,
                        commitID.length()).equals(commitID)) {
                    return (Utils.readObject(
                            headCommit.listFiles()[0], Commit.class));
                }
            } else {
                if (Utils.readObject(commits.listFiles()[i],
                        Commit.class).getID().substring(0,
                        commitID.length()).equals(commitID)) {
                    return Utils.readObject(
                            commits.listFiles()[i], Commit.class);
                }
            }
        }
        return null;
    }

    /**
     * Checks whether all files
     * are tracked in a given branch.
     * returns true if all blob map files
     * of commit pointed to by branch are
     * in head commit already, else checks
     * that those files aren't in the WD.
     * @param branchName String
     * @return boolean
     */
    boolean isTrackedGivenBranch(String branchName) {
        Commit head = Utils.readObject(
                headCommit.listFiles()[0], Commit.class);
        HashMap<String, String> headBlobs = head.getBlobs();
        branchTracker = Utils.readObject(branchTrack, HashMap.class);
        Commit given = Utils.readObject(new File(commits
                + java.io.File.separator
                + branchTracker.get(branchName)), Commit.class);
        HashMap<String, String> givenBlobs = given.getBlobs();
        if (headBlobs.keySet().containsAll(givenBlobs.keySet())) {
            return true;
        } else {
            givenBlobs.keySet().removeAll(headBlobs.keySet());
            List<String> filesInWD =
                    Utils.plainFilenamesIn(new File("."
                            + java.io.File.separator));
            givenBlobs.keySet().retainAll(filesInWD);
            if (givenBlobs.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Removes files that are in WD
     * but not in the blob map of
     * the commit pointed to by the
     * given branch.
     * @param branchName String
     * @param comm Commit
     */
    void removeFromWDGivenBranch(String branchName, Commit comm) {
        Commit head = Utils.readObject(
                headCommit.listFiles()[0], Commit.class);
        HashMap<String, String> headBlobs = head.getBlobs();
        branchTracker = Utils.readObject(branchTrack, HashMap.class);
        Commit given = comm;
        HashMap<String, String> givenBlobs = given.getBlobs();

        headBlobs.keySet().removeAll(givenBlobs.keySet());

        for (String str : headBlobs.keySet()) {
            new File("."
                    + java.io.File.separator  + str).delete();
        }
    }

    /**
     * Checks whether all files are
     * tracked in a given commit. Returns
     * true if all blob map files of commit
     * are in head commit already, else
     * checks that those files aren't in WD.
     * @param commitID String
     * @return boolean
     */
    boolean isTrackedGivenCommit(String commitID) {
        Commit head = Utils.readObject(
                headCommit.listFiles()[0], Commit.class);
        HashMap<String, String> headBlobs = head.getBlobs();

        Commit given = findCommitWithID(commitID);
        HashMap<String, String> givenBlobs = given.getBlobs();
        if (headBlobs.keySet().containsAll(givenBlobs.keySet())) {
            return true;
        } else {
            givenBlobs.keySet().removeAll(headBlobs.keySet());
            List<String> filesInWD = Utils.plainFilenamesIn(
                    new File("." + java.io.File.separator));
            givenBlobs.keySet().retainAll(filesInWD);
            if (givenBlobs.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Removes files that are in WD
     * but not in the blob map of the
     * given commit.
     * @param commitID String
     */
    void removeFromWDGivenCommit(String commitID) {
        Commit head = Utils.readObject(
                headCommit.listFiles()[0], Commit.class);
        HashMap<String, String> headBlobs = head.getBlobs();

        Commit given = findCommitWithID(commitID);
        HashMap<String, String> givenBlobs = given.getBlobs();

        headBlobs.keySet().removeAll(givenBlobs.keySet());

        ArrayList<String> temp = new ArrayList<>();

        for (String str : headBlobs.keySet()) {
            new File("."
                    + java.io.File.separator  + str).delete();
        }
    }

    /**
     * Finds all the ancestors of the
     * given commit.
     * @param comm Commit
     * @return HashSet (set of SHA id's of ancestor commits)
     */
    HashSet<String> findAncestors(Commit comm) {
        Commit curr = comm;
        HashSet<String> ancestors = new HashSet<>();
        while (true) {
            ancestors.add(curr.getID());
            if (findCommitWithParent(curr.getParent()) == null) {
                return ancestors;
            }
            curr = findCommitWithParent(curr.getParent());
        }
    }

    /**
     * A given command.
     */
    private String _command;

    /**
     * Operands.
     */
    private String[] _operands;

    /**
     * Path of Gitlet repository.
     */
    private File hidden = new File(".gitlet"
            + java.io.File.separator);

    /**
     * Path of commits subdirectory
     * in Gitlet repository.
     */
    private File commits = new File(hidden
            + java.io.File.separator + "commits"
            + java.io.File.separator);

    /**
     * Path of headCommit subdirectory
     * in Gitlet repository.
     */
    private File headCommit = new File(commits
            + java.io.File.separator + "head"
            + java.io.File.separator);

    /**
     * Path of branches subdirectory
     * in Gitlet repository.
     */
    private File branches = new File(hidden
            + java.io.File.separator + "branches"
            + java.io.File.separator);

    /**
     * Path of headBranch subdirectory
     * in Gitlet repository.
     */
    private File headBranch = new File(branches
            + java.io.File.separator + "head"
            + java.io.File.separator);

    /**
     * Path of blobs directory
     * in Gitlet repository.
     */
    private File blobs = new File(hidden
            + java.io.File.separator + "blobs"
            + java.io.File.separator);

    /**
     * Path of stage directory
     * in Gitlet repository.
     */
    private File stage = new File(hidden
            + java.io.File.separator + "stage"
            + java.io.File.separator);

    /**
     * Path of unstage file
     * in Gitlet repository.
     */
    private File unstage = new File(hidden
            + java.io.File.separator + "unstage");

    /**
     * HashSet which contains names
     * of blobs which need to be unstaged
     * is hashed here.
     */
    private HashSet<String> blobsToUnstage =
            new HashSet<String>();

    /**
     * Path of branchTracker file
     * in Gitlet repository.
     */
    private File branchTrack = new File(hidden
            + java.io.File.separator + "branchTracker");

    /**
     * HashMap containing
     * name of branch --> SHA id of
     * commit object branch points to.
     */
    private HashMap<String, String>
            branchTracker = new HashMap<>();

    /**
     * Path of initial file
     * in Gitlet repository.
     */
    private File initialCommit = new File(hidden
            + java.io.File.separator + "initial");

    /**
     * The initial Commit.
     */
    private Commit initial;

    /**
     * Set of files that don't
     * have any conflict.
     */
    private ArrayList<String> noConflict = new ArrayList<String>();

    /**
     * Blobs of split point.
     */
    private HashMap<String, String> splitPointFiles = new HashMap<>();

    /**
     * Blobs of current head.
     */
    private HashMap<String, String> headFiles = new HashMap<>();

    /**
     * Blobs of given branch's commit.
     */
    private HashMap<String, String> givenFiles = new HashMap<>();

    /**
     * Set of files that haven't been
     * modified in given commit since split point.
     */
    private HashMap<String, String>
            notModifiedInGivenSinceSplit = new HashMap<>();

    /**
     * Set of files that have been
     * modified in given commit since split point.
     */
    private HashMap<String, String>
            modifiedInGivenSinceSplit = new HashMap<>();

    /**
     * Set of files that haven't been
     * modified in head commit since split.
     */
    private HashMap<String, String>
            notModifiedInHeadSinceSplit = new HashMap<>();

    /**
     * Set of files that have been
     * modified in head commit since split.
     */
    private HashMap<String, String>
            modifiedInHeadSinceSplit = new HashMap<>();

}
