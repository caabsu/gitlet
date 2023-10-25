package gitlet;

import java.io.Serializable;

public class Tree implements Serializable {

    /** The head commit of tree. */
    private Commit _headCommit;

    /** The current commit of tree. */
    private Commit _currCommit;

    /** The name of branch. */
    private String _branchName;

    public Tree(Commit commit) {
        _headCommit = commit;
        _currCommit = commit;
        _branchName = "master";
    }
}
