package gitlet;

import java.io.Serializable;
import java.io.File;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;


public class Commit implements Serializable {

    public Commit() throws IOException {
        _message = "initial commit";
        _date = "Thu Jan 1 00:00:00 1970 -0500";
        _parentID = null;
        _secondID = null;
        _blob = new HashMap<String, Blob>();
        _id = Utils.sha1(Utils.serialize(this));
        _childBranch = new ArrayList<String>();
        _childBranch.add("master");
        saveToDir(this);
    }

    public Commit(String message, String parentID, HashMap<String, Blob> blob,
                  String secondID) throws IOException {
        _message = message;
        _parentID = parentID;
        _secondID = secondID;
        _blob = blob;
        _date = getTimeFormatted(ZonedDateTime.now());
        _id = Utils.sha1(Utils.serialize(this));
        _childBranch = new ArrayList<String>();
        String fileName = Repo.getHeadBranch().getName();
        String[] tokens = fileName.split("\\.(?=[^\\.]+$)");
        _childBranch.add(tokens[0]);
        saveToDir(this);
    }

    public String getTimeFormatted(ZonedDateTime now) {
        DateTimeFormatter format = DateTimeFormatter.
                ofPattern("E LLL d HH:mm:ss uuuu Z");
        return now.format(format);
    }

    public static void saveToDir(Commit save) throws IOException {
        File thisCommit = new File(Repo.COMMITS, save._id + ".txt");
        thisCommit.createNewFile();
        Utils.writeObject(thisCommit, save);
    }

    public void addBranch(String branchName) {
        _childBranch.add(branchName);
    }

    public ArrayList<String> getBranch() {
        return _childBranch;
    }

    public static String getID(Commit commit) {
        return commit._id;
    }

    public static HashMap<String, Blob> getBlob(Commit commit) {
        return commit._blob;
    }

    public static String getParentID(Commit commit) {
        return commit._parentID;
    }

    public static String getParentID2(Commit commit) {
        return commit._secondID;
    }

    public static String getDate(Commit commit) {
        return commit._date;
    }

    public static String getMessage(Commit commit) {
        return commit._message;
    }

    /** The message of the commit. */
    private final String _message;

    /** The first parent ID of this commit. */
    private final String _parentID;

    /** The second parent ID of this commit. */
    private final String _secondID;

    /** The ArrayList of child branches for this commit. */
    private ArrayList<String> _childBranch = null;

    /** The ID of this commit. */
    private final String _id;

    /** The hashmap of the blobs. */
    private final HashMap<String, Blob> _blob;

    /** The date for the commit. */
    private final String _date;


}
