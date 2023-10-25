package gitlet;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;
import java.util.List;

public class Repo {
    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));

    /** The git directory. */
    public static final File GITDIR = Utils.join(CWD, ".gitlet");

    /** The commit folder. */
    public static final File COMMITS = new File(GITDIR, "COMMITS");

    /** The diff folder. */
    public static final File DIFF = new File(GITDIR, "DIFF");

    /** The staging area folder. */
    public static final File STAGINGAREA =
            new File(GITDIR, "STAGINGAREA");

    /** The staging removal folder. */
    public static final File STAGINGREMOVAL =
            new File(GITDIR, "STAGINGREMOVAL");

    /** The current branch file. */
    public static final File CURRBRANCH =
            new File(GITDIR, "CURRBRANCH.txt");

    /** The current commit branch file. */
    public static final File COMMITBRANCHES = new
            File(GITDIR, "COMMITBRANCHES");


    public static void init() throws IOException {
        if (GITDIR.exists()) {
            System.out.println("A Gitlet version-control system "
                    + "already exists in the current directory.");
        } else {
            GITDIR.mkdir();
            DIFF.mkdir();
            COMMITS.mkdir();
            STAGINGAREA.mkdir();
            STAGINGREMOVAL.mkdir();
            COMMITBRANCHES.mkdir();
            CURRBRANCH.createNewFile();
            File masterBranch = new File(COMMITBRANCHES, "master.txt");
            masterBranch.createNewFile();
            Commit firstCommit = new Commit();
            String firstID = Commit.getID(firstCommit);
            Utils.writeContents(CURRBRANCH, masterBranch.getName());
            Utils.writeContents(masterBranch, firstID);
        }
    }

    public static String getCurrHead() {
        return Utils.readContentsAsString(getHeadBranch());
    }

    public static File getHeadBranch() {
        String current = Utils.readContentsAsString(CURRBRANCH);
        File headBranch = new File(COMMITBRANCHES, current);
        return headBranch;
    }

    public static void add(String fileName) throws IOException {
        File fileToAdd = new File(CWD, fileName);
        File fileInArea = new File(STAGINGAREA, fileName);
        File fileRemoval = new File(STAGINGREMOVAL, fileName);
        fileRemoval.delete();
        if (!fileToAdd.exists()) {
            System.out.println("File does not exist");
            System.exit(0);
        } else {
            Blob fileAdd = new Blob(fileToAdd);
            File curr = new File(COMMITS, getCurrHead() + ".txt");
            Commit fileCurrent = Utils.readObject(curr, Commit.class);
            ArrayList<Blob> check = new ArrayList<Blob>();
            check.addAll(Commit.getBlob(fileCurrent).values());
            boolean exists = false;
            for (Blob blob : check) {
                if (Objects.equals(Blob.getID(blob), Blob.getID(fileAdd))) {
                    exists = true;
                    fileInArea.delete();
                }
            }
            if (!exists) {
                fileInArea.delete();
                fileInArea.createNewFile();
                Utils.writeObject(fileInArea, fileAdd);
            }
        }
    }

    public static void doCommit(String message) throws IOException {
        if (STAGINGAREA.listFiles().length == 0
                && STAGINGREMOVAL.listFiles().length == 0) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }
        File currFile = new File(COMMITS, getCurrHead() + ".txt");
        Commit currCommit = Utils.readObject(currFile, Commit.class);
        String branchName = getHeadBranch().getName();
        String[] tokens = branchName.split("\\.(?=[^\\.]+$)");
        branchName = tokens[0];
        currCommit.addBranch(branchName);
        addToParents(branchName, currCommit);
        Utils.writeObject(currFile, currCommit);
        HashMap<String, Blob> keyNow = Commit.getBlob(currCommit);
        for (File thisFile : STAGINGAREA.listFiles()) {
            Blob thisBlob = Utils.readObject(thisFile, Blob.class);
            keyNow.replace(thisFile.getName(), thisBlob);
            keyNow.putIfAbsent(thisFile.getName(), thisBlob);
        }
        for (File remove : STAGINGREMOVAL.listFiles()) {
            keyNow.remove(remove.getName());
        }
        Commit newCommit = new Commit(message, getCurrHead(), keyNow, null);
        Utils.writeContents(getHeadBranch(), Commit.getID(newCommit));
        for (File subFile : Objects.requireNonNull(STAGINGAREA.listFiles())) {
            subFile.delete();
        }
        for (File subFile : Objects.
                requireNonNull(STAGINGREMOVAL.listFiles())) {
            subFile.delete();
        }
    }
    public static void addToParents(String branchName, Commit curr) {
        if (Commit.getParentID(curr) != null
                && Commit.getParentID2(curr) == null) {
            File currFile = new File(COMMITS,
                    Commit.getParentID(curr) + ".txt");
            Commit currCommit =
                    Utils.readObject(currFile, Commit.class);
            for (String add : curr.getBranch()) {
                currCommit.addBranch(add);
            }
            Utils.writeObject(currFile, currCommit);
            addToParents(branchName, currCommit);
        } else if (Commit.getParentID(curr)
                != null && Commit.getParentID2(curr) != null) {
            File currFile = new File(COMMITS,
                    Commit.getParentID(curr) + ".txt");
            File second = new File(COMMITS,
                    Commit.getParentID2(curr) + ".txt");
            Commit secCommit = Utils.readObject(second, Commit.class);
            Commit currCommit = Utils.readObject(currFile, Commit.class);
            for (String add : curr.getBranch()) {
                currCommit.addBranch(add);
                secCommit.addBranch(add);
            }
            Utils.writeObject(currFile, currCommit);
            Utils.writeObject(second, secCommit);
            addToParents(branchName, currCommit);
            addToParents(branchName, secCommit);
        }
    }

    public static void mergeCommit
    (String message, String secondID, String second) throws IOException {
        if (STAGINGAREA.listFiles().length == 0
                && STAGINGREMOVAL.listFiles().length == 0) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }
        File currFile = new File(COMMITS, getCurrHead() + ".txt");
        Commit currCommit = Utils.readObject(currFile, Commit.class);
        String branchName = getHeadBranch().getName();
        String[] tokens = branchName.split("\\.(?=[^\\.]+$)");
        branchName = tokens[0];
        currCommit.addBranch(branchName);
        addToParents(branchName, currCommit);
        File secondPar = new File(COMMITBRANCHES, second + ".txt");
        File secondCom = new File(COMMITS,
                Utils.readContentsAsString(secondPar) + ".txt");
        Commit secCur = Utils.readObject(secondCom, Commit.class);
        secCur.addBranch(branchName);
        addToParents(branchName, secCur);
        Utils.writeObject(secondCom, secCur);
        Utils.writeObject(currFile, currCommit);
        HashMap<String, Blob> keyNow = Commit.getBlob(currCommit);
        for (File thisFile : STAGINGAREA.listFiles()) {
            Blob thisBlob = Utils.readObject(thisFile, Blob.class);
            keyNow.replace(thisFile.getName(), thisBlob);
            keyNow.putIfAbsent(thisFile.getName(), thisBlob);
        }
        for (File remove : STAGINGREMOVAL.listFiles()) {
            keyNow.remove(remove.getName());
        }
        Commit newCommit = new Commit(message, getCurrHead(), keyNow, secondID);
        newCommit.addBranch(second);
        File newF = new File(COMMITS, Commit.getID(newCommit));
        Utils.writeObject(newF, newCommit);
        Utils.writeContents(getHeadBranch(), Commit.getID(newCommit));
        for (File subFile : Objects.
                requireNonNull(STAGINGAREA.listFiles())) {
            subFile.delete();
        }
        for (File subFile : Objects.
                requireNonNull(STAGINGREMOVAL.listFiles())) {
            subFile.delete();
        }
    }

    public static void rm(String fileName) throws IOException {
        boolean error = true;
        File remove = new File(STAGINGAREA, fileName);
        File cwdFile = new File(CWD, fileName);
        if (remove.exists()) {
            remove.delete();
            error = false;
        }
        File currCommit = new File(COMMITS, getCurrHead() + ".txt");
        Commit commitNow = Utils.readObject(currCommit, Commit.class);
        HashMap<String, Blob> blobNow = Commit.getBlob(commitNow);
        if (blobNow.containsKey(fileName)) {
            File addFile = new File(STAGINGREMOVAL, fileName);
            addFile.createNewFile();
            if (cwdFile.exists()) {
                cwdFile.delete();
            }
            error = false;
        }
        if (error) {
            System.out.println("No reason to remove the file");
        }
    }
    public static void log() {
        File currentFile = new File(COMMITS, getCurrHead() + ".txt");
        Commit currentCommit = Utils.readObject(currentFile, Commit.class);
        boolean last = false;
        while (!last) {
            if (Commit.getParentID(currentCommit) == null) {
                last = true;
                System.out.println("===");
                System.out.println("commit " + Commit.getID(currentCommit));
                System.out.println("Date: " + Commit.getDate(currentCommit));
                System.out.println(Commit.getMessage(currentCommit) + "\r\n");
            } else {
                System.out.println("===");
                System.out.println("commit " + Commit.getID(currentCommit));
                System.out.println("Date: " + Commit.getDate(currentCommit));
                System.out.println(Commit.getMessage(currentCommit) + "\r\n");
                currentFile = new File(COMMITS,
                        Commit.getParentID(currentCommit) + ".txt");
                currentCommit = Utils.readObject(currentFile, Commit.class);
            }
        }
    }

    public static void globalLog() {
        for (File thisFile : Objects.requireNonNull(COMMITS.listFiles())) {
            Commit currentCommit = Utils.readObject(thisFile, Commit.class);
            System.out.println("===");
            System.out.println("commit " + Commit.getID(currentCommit));
            System.out.println("Date: " + Commit.getDate(currentCommit));
            System.out.println(Commit.getMessage(currentCommit) + "\r\n");
        }
    }
    public static void checkoutOne
    (String commitID, String fileName) {
        final int length = 40;
        if (commitID.length() < length) {
            for (File allFiles : COMMITS.listFiles()) {
                String[] tokens = allFiles.getName().split("\\.(?=[^\\.]+$)");
                String name = tokens[0];
                if (name.contains(commitID)) {
                    commitID = name;
                    break;
                }
            }
        }
        File currFile = new File(COMMITS, commitID + ".txt");
        if (!currFile.exists()) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        Commit thisCommit = Utils.readObject(currFile, Commit.class);
        HashMap<String, Blob> thisBlob = Commit.getBlob(thisCommit);
        if (!thisBlob.containsKey(fileName)) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        } else {
            File thisFile = new File(CWD, fileName);
            Blob createBlob = thisBlob.get(fileName);
            Utils.writeContents(thisFile, Blob.getContent(createBlob));
        }
    }

    public static void find(String message) {
        boolean exists = false;
        for (File thisFile : Objects.requireNonNull(COMMITS.listFiles())) {
            Commit thisCommit = Utils.readObject(thisFile, Commit.class);
            if (Commit.getMessage(thisCommit).equals(message)) {
                System.out.println(Commit.getID(thisCommit));
                exists = true;
            }
        }
        if (!exists) {
            System.out.println("Found no commit with that message.");
        }
    }

    public static void branch(String name) throws IOException {
        File newBranch = new File(COMMITBRANCHES, name + ".txt");
        if (newBranch.exists()) {
            System.out.println("A branch with that name already exists");
        } else {
            File currFile = new File(COMMITS, getCurrHead() + ".txt");
            Commit currCommit = Utils.readObject(currFile, Commit.class);
            currCommit.addBranch(name);
            Utils.writeObject(currFile, currCommit);
            newBranch.createNewFile();
            Utils.writeContents(newBranch, getCurrHead());
        }

    }

    public static void checkoutTwo(String branchName) throws IOException {
        File branchFile = new File(COMMITBRANCHES, branchName + ".txt");
        if (!branchFile.exists()) {
            System.out.println("No such branch exists");
        } else if (getHeadBranch().getName().equals(branchName + ".txt")) {
            System.out.println("No need to checkout the current branch");
        } else {
            File commitFile = new File(COMMITS,
                    Utils.readContentsAsString(branchFile) + ".txt");
            Commit headCommit = Utils.readObject(commitFile, Commit.class);

            String name = Utils.readContentsAsString(getHeadBranch());
            File lastFile = new File(COMMITS, name + ".txt");
            Commit lastCommit = Utils.readObject(lastFile, Commit.class);

            checkout(headCommit, lastCommit);
            Utils.writeContents(CURRBRANCH, branchName + ".txt");
            for (File subFile : Objects.
                    requireNonNull(STAGINGAREA.listFiles())) {
                subFile.delete();
            }
        }

    }

    public static void checkout(Commit headCommit,
                                Commit lastCommit) throws IOException {
        HashMap<String, Blob> newBlob = Commit.getBlob(headCommit);
        HashMap<String, Blob> lastBlob = Commit.getBlob(lastCommit);
        for (String key : lastBlob.keySet()) {
            File checkFile = new File(CWD, key);
            if (checkFile.exists()
                    && !Blob.getContent(lastBlob.get(key))
                    .equals(Utils.readContentsAsString(checkFile))) {
                if (!newBlob.containsKey(key)) {
                    System.out.println("There is an untracked file "
                            + "in the way; delete it, "
                            + "or add and commit it first.");
                    System.exit(0);
                } else if (!lastBlob.get(key).equals(newBlob.get(key))) {
                    System.out.println("There is an untracked file "
                            + "in the way; delete it, "
                            + "or add and commit it first.");
                    System.exit(0);
                }
            }
        }
        for (File checkFile : CWD.listFiles()) {
            String name = checkFile.getName();
            if (!lastBlob.containsKey(name) && newBlob.containsKey(name)
                    && !Blob.getContent(newBlob.get(name))
                    .equals(Utils.readContentsAsString(checkFile))) {
                System.out.println("There is an untracked file "
                        + "in the way; delete it, "
                        + "or add and commit it first.");
                System.exit(0);
            } else if (lastBlob.containsKey(name)
                    && !Blob.getContent(lastBlob.get(name))
                    .equals(Utils.readContentsAsString(checkFile))) {
                if (!newBlob.containsKey(name)
                        || !Blob.getContent(newBlob.get(name))
                        .equals(Blob.getContent(lastBlob.get(name)))) {
                    System.out.println("There is an untracked file "
                            + "in the way; delete it, "
                            + "or add and commit it first.");
                    System.exit(0);
                }
            }
        }

        for (String key : lastBlob.keySet()) {
            if (!newBlob.containsKey(key)) {
                File deleteFile = new File(CWD, key);
                deleteFile.delete();
            }
        }
        for (String key : newBlob.keySet()) {
            File checkFile = new File(CWD, key);
            Blob checkBlob = newBlob.get(key);
            if (checkFile.exists()) {
                checkFile.delete();
            }
            checkFile.createNewFile();
            Utils.writeContents(checkFile, Blob.getContent(checkBlob));
        }
    }

    public static void removeBranch(String name) {
        File branchFile = new File(COMMITBRANCHES, name + ".txt");
        if (!branchFile.exists()) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        } else if (getHeadBranch().getName().equals(name + ".txt")) {
            System.out.println("Cannot remove the current branch");
            System.exit(0);
        }
        branchFile.delete();
    }

    public static void reset(String id) throws IOException {
        File commitFile = new File(COMMITS, id + ".txt");
        if (!commitFile.exists()) {
            System.out.println("No commit with that id exists.");
        } else {
            String name = Utils.readContentsAsString(getHeadBranch());
            File lastFile = new File(COMMITS, name + ".txt");
            Commit lastCommit = Utils.readObject(lastFile, Commit.class);
            Commit headCommit = Utils.readObject(commitFile, Commit.class);
            checkout(headCommit, lastCommit);
            Utils.writeContents(getHeadBranch(), Commit.getID(headCommit));
            for (File subFile
                    : Objects.requireNonNull(STAGINGAREA.listFiles())) {
                subFile.delete();
            }
            for (File subFile
                    : Objects.requireNonNull(STAGINGREMOVAL.listFiles())) {
                subFile.delete();
            }
        }
    }

    public static void status() {
        if (!GITDIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
        System.out.println("=== Branches ===");
        for (File branches
                : Objects.requireNonNull(COMMITBRANCHES.listFiles())) {
            String fileName = branches.getName();
            String[] tokens = fileName.split("\\.(?=[^\\.]+$)");
            if (fileName.equals(Utils.readContentsAsString(CURRBRANCH))) {
                System.out.println("*" + tokens[0]);
            } else {
                System.out.println(tokens[0]);
            }
        }
        System.out.println();
        System.out.println("=== Staged Files ===");
        for (File staged
                : Objects.requireNonNull(STAGINGAREA.listFiles())) {
            System.out.println(staged.getName());
        }
        System.out.println();
        System.out.println("=== Removed Files ===");
        for (File removal
                : Objects.requireNonNull(STAGINGREMOVAL.listFiles())) {
            System.out.println(removal.getName());
        }
        System.out.println();
        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println();
        System.out.println("=== Untracked Files ===" + "\r\n");
    }


    public static void merge(String name) throws IOException {
        if (STAGINGAREA.listFiles().length != 0
                || STAGINGREMOVAL.listFiles().length != 0) {
            System.out.println("You have uncommitted changes.");
            System.exit(0);
        }
        findCommon(name);
    }

    public static void findCommon(String name) throws IOException {
        File givenBranch = new File(COMMITBRANCHES, name + ".txt");
        if (!givenBranch.exists()) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        String headNow = Utils.readContentsAsString(getHeadBranch());
        File commitFile = new File(COMMITS, headNow + ".txt");
        File givenFile = new File(COMMITS,
                Utils.readContentsAsString(givenBranch) + ".txt");
        Commit givenCommit = Utils.readObject(givenFile, Commit.class);
        Commit commitNow = Utils.readObject(commitFile, Commit.class);
        if (getHeadBranch().equals(givenBranch)) {
            System.out.println("Cannot merge a branch with itself.");
            System.exit(0);
        }
        checkAncestor(commitNow, Commit.getID(givenCommit));
        Commit split = findAncestor(name);
        checkUntracked(split, commitNow, givenCommit);
        if (Commit.getID(split).equals(Commit.getID(commitNow))) {
            checkoutTwo(name);
            System.out.println("Current branch fast-forwarded");
            System.exit(0);
        }
        helper1(split, commitNow, givenCommit, name);
    }

    public static void helper1(Commit split, Commit commitNow,
                               Commit givenCommit, String name)
            throws IOException {
        boolean conflict = false;
        HashMap<String, Blob> splitBlobs = Commit.getBlob(split);
        HashMap<String, Blob> currBlobs = Commit.getBlob(commitNow);
        HashMap<String, Blob> givenBlobs = Commit.getBlob(givenCommit);
        for (String fileNames : splitBlobs.keySet()) {
            Blob splitB = splitBlobs.get(fileNames);
            Blob currB = currBlobs.get(fileNames);
            Blob givenB = givenBlobs.get(fileNames);
            if (splitB.equals(currB) && !splitB.equals(givenB)) {
                if (givenBlobs.containsKey(fileNames)) {
                    checkoutOne(Commit.getID(givenCommit), fileNames);
                    add(fileNames);
                } else {
                    rm(fileNames);
                }
            } else if (!splitB.equals(currB)
                    && !splitB.equals(givenB)) {
                if ((currB != null && !currB.equals(givenB))
                        || (givenB != null && !givenB.equals(currB))) {
                    String newString;
                    if (currB != null) {
                        newString = "<<<<<<< HEAD" + "\r\n"
                                + Blob.getContent(currBlobs.get(fileNames))
                                + "=======" + "\r\n"
                                + ">>>>>>>" + "\r\n";
                    } else {
                        newString = "<<<<<<< HEAD" + "\r\n" + "=======" + "\r\n"
                                + Blob.getContent(givenBlobs.get(fileNames))
                                + ">>>>>>>" + "\r\n";
                    }
                    File overwrite = new File(CWD, fileNames);
                    Utils.writeContents(overwrite, newString);
                    add(fileNames);
                    conflict = true;
                }
            }
        }
        for (String fileNames : givenBlobs.keySet()) {
            if (currBlobs.get(fileNames) == null
                    && splitBlobs.get(fileNames) == null) {
                checkoutOne(Commit.getID(givenCommit), fileNames);
                add(fileNames);
            } else if (currBlobs.get(fileNames) != null
                    && !(currBlobs.get(fileNames)
                    .equals(givenBlobs.get(fileNames)))
                    && !currBlobs.get(fileNames)
                    .equals(splitBlobs.get(fileNames))) {
                String newString = formString(Blob.getContent(currBlobs.get
                        (fileNames)),
                        Blob.getContent(givenBlobs.get(fileNames)));
                File overwrite = new File(CWD, fileNames);
                Utils.writeContents(overwrite, newString);
                add(fileNames);
                conflict = true;
            }
        }
        checkConflict(name);
        conflict(conflict);
    }

    public static void checkConflict(String name) throws IOException {
        File givenBranch = new File(COMMITBRANCHES, name + ".txt");
        String currBranchName = getHeadBranch().getName();
        String[] tokens = currBranchName.split("\\.(?=[^\\.]+$)");
        currBranchName = tokens[0];
        String message = "Merged " + name + " into " + currBranchName + ".";
        mergeCommit(message, Utils.readContentsAsString(givenBranch), name);
    }

    public static String formString(String one, String two) {
        String newString = "<<<<<<< HEAD" + "\r\n"
                + one
                + "=======" + "\r\n" + two
                + ">>>>>>>" + "\r\n";
        return newString;
    }

    public static void conflict(boolean conflict) {
        if (conflict) {
            System.out.println("Encountered a merge conflict.");
        }
    }

    public static void checkUntracked(Commit split, Commit commitNow,
                                      Commit givenCommit) {
        HashMap<String, Blob> splitBlobs = Commit.getBlob(split);
        HashMap<String, Blob> currBlobs = Commit.getBlob(commitNow);
        HashMap<String, Blob> givenBlobs = Commit.getBlob(givenCommit);
        for (String checkString : currBlobs.keySet()) {
            File checkFile = new File(CWD, checkString);
            String content = Utils.readContentsAsString(checkFile);
            if (checkFile.exists() && !Blob.getContent
                    (currBlobs.get(checkString)).equals(content)) {
                Blob splitB = splitBlobs.get(checkString);
                Blob currB = currBlobs.get(checkString);
                Blob givenB = givenBlobs.get(checkString);
                if (currB.equals(splitB) && !currB.equals(givenB)) {
                    System.out.println("There is an untracked file "
                            + "in the way; delete it, "
                            + "or add and commit it first.");
                    System.exit(0);
                } else if (currB.equals(splitB) && givenB == null) {
                    System.out.println("There is an untracked file "
                            + "in the way; delete it, "
                            + "or add and commit it first.");
                    System.exit(0);
                } else if (!currB.equals(splitB)
                        && !currB.equals(givenB)) {
                    if ((splitB != null && !splitB.equals(givenB))
                            || givenB != null && !givenB.equals(splitB)) {
                        System.out.println("There is an untracked file "
                                + "in the way; delete it, "
                                + "or add and commit it first.");
                        System.exit(0);
                    }

                }
            }
        }
        for (File allFile : CWD.listFiles()) {
            String fileName = allFile.getName();
            if (!splitBlobs.containsKey(allFile)
                    && !currBlobs.containsKey(fileName)
                    && givenBlobs.containsKey(fileName)) {
                System.out.println("There is an untracked file "
                        + "in the way; delete it, "
                        + "or add and commit it first.");
                System.exit(0);
            }
        }
    }

    public static void checkAncestor(Commit checkCommit, String checkID) {
        if (Commit.getID(checkCommit).equals(checkID)) {
            System.out.println("Given branch "
                    + "is an ancestor of the current branch.");
            System.exit(0);
        }
        if (Commit.getParentID(checkCommit) != null) {
            File firstFile = new File(COMMITS,
                    Commit.getParentID(checkCommit) + ".txt");
            Commit firstCommit = Utils.readObject(firstFile, Commit.class);
            checkAncestor(firstCommit, checkID);
            if (Commit.getParentID2(checkCommit) != null) {
                File secondFile = new File(COMMITS,
                        Commit.getParentID2(checkCommit) + ".txt");
                Commit secondCommit = Utils.
                        readObject(secondFile, Commit.class);
                checkAncestor(secondCommit, checkID);
            }
        }
    }

    public static Commit findAncestor(String name) {
        HashMap<Integer, Commit> findMin = new HashMap<Integer, Commit>();
        String currBranchName = getHeadBranch().getName();
        String[] tokens = currBranchName.split("\\.(?=[^\\.]+$)");
        currBranchName = tokens[0];
        File givenBranch = new File(COMMITBRANCHES, name + ".txt");
        File givenFile = new File(COMMITS,
                Utils.readContentsAsString(givenBranch) + ".txt");
        Commit givenCommit = Utils.readObject(givenFile, Commit.class);
        for (File check : COMMITS.listFiles()) {
            Commit thisCommit = Utils.readObject(check, Commit.class);
            if (thisCommit.getBranch() != null
                    && thisCommit.getBranch().contains(name)
                    && thisCommit.getBranch().contains(currBranchName)) {
                int value = findDepth(thisCommit, givenCommit, 0);
                findMin.put(value, thisCommit);
            }
        }
        return findMin.get(Collections.min(findMin.keySet()));
    }

    public static int findDepth(Commit end, Commit start, int counter) {
        if (Commit.getID(start).equals(Commit.getID(end))) {
            return counter;
        }
        if (Commit.getParentID(start) == null) {
            return 100;
        }
        File first = new File(COMMITS, Commit.getParentID(start) + ".txt");
        Commit parent1 = Utils.readObject(first, Commit.class);
        if (Commit.getParentID2(start) != null) {
            File second = new File(COMMITS,
                    Commit.getParentID2(start) + ".txt");
            Commit parent2 = Utils.readObject(second, Commit.class);
            return Math.min(findDepth(end, parent1, counter + 1),
                    findDepth(end, parent2, counter + 1));
        }
        return findDepth(end, parent1, counter + 1);
    }

    public static void diff() throws IOException {
        String currBranchName = getHeadBranch().getName();
        String[] tokens = currBranchName.split("\\.(?=[^\\.]+$)");
        currBranchName = tokens[0];
        firstSecond(currBranchName);
    }
    public static void diff2(String branchName) throws IOException {
        firstSecond(branchName);
    }

    public static HashMap<String, Blob> getHash(String branch) {
        File firstBranch = new File(COMMITBRANCHES, branch + ".txt");
        if (!firstBranch.exists()) {
            System.out.println("At least one branch does not exist.");
            System.exit(0);
        }
        String firstString = Utils.readContentsAsString(firstBranch);
        File firstFile = new File(COMMITS, firstString + ".txt");
        Commit firstCommit = Utils.readObject(firstFile, Commit.class);
        HashMap<String, Blob> firstHash = Commit.getBlob(firstCommit);
        return firstHash;
    }
    public static void diff3(String branch1,
                             String branch2) throws IOException {
        HashMap<String, Blob> firstHash = getHash(branch1);
        HashMap<String, Blob> secondHash = getHash(branch2);
        ArrayList<String> checked = new ArrayList<>();
        ArrayList<String> keys3 = new ArrayList<>();
        for (String fileName : firstHash.keySet()) {
            keys3.add(fileName);
        }
        Collections.reverse(keys3);
        for (String fileName : keys3) {
            checked.add(fileName);
            if (secondHash.containsKey(fileName)
                    && firstHash.get(fileName).
                    equals(secondHash.get(fileName))) {
                continue;
            }
            File blobFile1 = new File(DIFF, fileName);
            blobFile1.createNewFile();
            Utils.writeContents(blobFile1,
                    Blob.getContent(firstHash.get(fileName)));

            File blobFile2 = new File(DIFF, "2" + fileName);
            blobFile2.createNewFile();
            if (secondHash.containsKey(fileName)) {
                Utils.writeContents(blobFile2,
                        Blob.getContent(secondHash.get(fileName)));
            }
            Diff thisDiff = new Diff();
            thisDiff.setSequences(blobFile1, blobFile2);
            int[] edits = thisDiff.diffs();
            List<String> firstList = thisDiff.sequence1();
            List<String> secondList = thisDiff.sequence2();
            anotherHelp(firstList, secondList, fileName);
            int i = 0;
            while (i < edits.length) {
                String printed = printed(edits, i);
                System.out.println(printed);
                for (int j = 0; j < edits[i + 1]; j++) {
                    System.out.println("-" + firstList.get(edits[i] + j));
                }
                for (int k = 0; k < edits[i + 3]; k++) {
                    System.out.println("+" + secondList.get(edits[i + 2] + k));
                }
                i += 4;
            }
        }
        ArrayList<String> keys2 = new ArrayList<>();
        for (String fileName : secondHash.keySet()) {
            keys2.add(fileName);
        }
        Collections.reverse(keys2);
        for (String fileName : keys2) {
            if (!checked.contains(fileName)) {
                thirdHelp(firstHash, secondHash, fileName);
            }
        }
    }

    public static void anotherHelp(List<String> firstList,
                                   List<String> secondList,
                                   String fileName) {
        if (secondList.isEmpty()) {
            System.out.println("diff --git a/" + fileName + " /dev/null");
            System.out.println("--- a/" + fileName);
            System.out.println("+++ dev/null");
        } else if (firstList.isEmpty()) {
            System.out.println("diff --git /dev/null" + " b/" + fileName);
            System.out.println("--- a/" + fileName);
            System.out.println("+++ dev/null");
        } else {
            System.out.println("diff --git a/"
                    + fileName + " b/" + fileName);
            System.out.println("--- a/" + fileName);
            System.out.println("+++ b/" + fileName);
        }
    }

    public static void thirdHelp(HashMap<String, Blob>
                                         firstHash, HashMap<String,
            Blob> secondHash, String fileName) throws IOException {
        File blobFile1 = new File(DIFF, fileName);
        blobFile1.createNewFile();
        if (firstHash.containsKey(fileName)) {
            Utils.writeContents(blobFile1,
                    Blob.getContent(firstHash.get(fileName)));
        }
        File blobFile2 = new File(DIFF, "2" + fileName);
        blobFile2.createNewFile();
        Utils.writeContents(blobFile2,
                Blob.getContent(secondHash.get(fileName)));
        Diff thisDiff = new Diff();
        thisDiff.setSequences(blobFile1, blobFile2);
        int[] edits = thisDiff.diffs();
        List<String> firstList = thisDiff.sequence1();
        List<String> secondList = thisDiff.sequence2();
        if (secondList.isEmpty()) {
            System.out.println("diff --git a/" + fileName + " /dev/null");
            System.out.println("--- a/" + fileName);
            System.out.println("+++ dev/null");
        } else if (firstList.isEmpty()) {
            System.out.println("diff --git /dev/null" + " b/" + fileName);
            System.out.println("--- /dev/null");
            System.out.println("+++ b/" + fileName);
        } else {
            System.out.println("diff --git a/"
                    + fileName + " b/" + fileName);
            System.out.println("--- a/" + fileName);
            System.out.println("+++ b/" + fileName);
        }
        int i = 0;
        while (i < edits.length) {
            String printed = printed(edits, i);
            System.out.println(printed);
            for (int j = 0; j < edits[i + 1]; j++) {
                System.out.println("-" + firstList.get(edits[i] + j));
            }
            for (int k = 0; k < edits[i + 3]; k++) {
                System.out.println("+" + secondList.get(edits[i + 2] + k));
            }
            i += 4;
        }

    }


    public static void firstSecond(String branchName) throws IOException {
        File secondBranch = new File(COMMITBRANCHES, branchName + ".txt");
        if (!secondBranch.exists()) {
            System.out.println("A branch with that name does not exist");
            System.exit(0);
        }
        String firstString = Utils.readContentsAsString(secondBranch);
        File firstFile = new File(COMMITS, firstString + ".txt");
        Commit firstCommit = Utils.readObject(firstFile, Commit.class);
        HashMap<String, Blob> firstHash = Commit.getBlob(firstCommit);
        ArrayList<String> keys = new ArrayList<>();
        for (String fileName : firstHash.keySet()) {
            keys.add(fileName);
        }
        Collections.reverse(keys);
        for (String fileName : keys) {
            File blobFile = new File(DIFF, fileName);
            blobFile.createNewFile();
            Utils.writeContents(blobFile,
                    Blob.getContent(firstHash.get(fileName)));
            File cwdFile = new File(CWD, fileName);
            if (cwdFile.exists()
                    && Utils.readContentsAsString(cwdFile).
                    equals(Utils.readContentsAsString(blobFile))) {
                continue;
            }
            if (cwdFile.exists()) {
                System.out.println("diff --git a/"
                        + fileName + " b/" + fileName);
                System.out.println("--- a/" + fileName);
                System.out.println("+++ b/" + fileName);
            } else {
                System.out.println("diff --git a/"
                        + fileName + " /dev/null");
                System.out.println("--- a/" + fileName);
                System.out.println("+++ /dev/null");
                cwdFile  = new File(DIFF, fileName + "2");
                cwdFile.createNewFile();
            }
            Diff thisDiff = new Diff();
            thisDiff.setSequences(blobFile, cwdFile);
            int[] edits = thisDiff.diffs();
            List<String> firstList = thisDiff.sequence1();
            List<String> secondList = thisDiff.sequence2();
            int i = 0;
            while (i < edits.length) {
                String printed = printed(edits, i);
                System.out.println(printed);
                for (int j = 0; j < edits[i + 1]; j++) {
                    System.out.println("-" + firstList.get(edits[i] + j));
                }
                for (int k = 0; k < edits[i + 3]; k++) {
                    System.out.println("+"
                            + secondList.get(edits[i + 2] + k));
                }
                i += 4;
            }
        }
    }

    public static String printed(int[] edits, int i) {
        String printed = "";
        if (edits[i + 1] == 0) {
            printed += "@@ -" + edits[i];
        } else {
            printed += "@@ -" + (edits[i] + 1);
        }
        if (edits[i + 1] != 1) {
            printed += "," + edits[i + 1];
        }
        if (edits[i + 3] == 0) {
            printed += " +" + edits[i + 2];
        } else {
            printed += " +" + (edits[i + 2] + 1);
        }
        if (edits[i + 3] != 1) {
            printed += "," + edits[i + 3];
        }
        printed += " @@";
        return printed;
    }
}
