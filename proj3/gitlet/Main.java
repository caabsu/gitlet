package gitlet;
import java.io.IOException;
import java.util.Objects;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Hee Su Chung
 */
public class Main {
    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) throws IOException {
        Repo repo = new Repo();
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }
        String argFirst = args[0];
        if (Objects.equals(argFirst, "init")) {
            repo.init();
        } else if (Objects.equals(argFirst, "add")) {
            repo.add(args[1]);
        } else if (Objects.equals(argFirst, "commit"))  {
            if (args.length == 1 || args[1].length() == 0) {
                System.out.println("Please enter a commit message");
            } else {
                repo.doCommit(args[1]);
            }
        } else if (Objects.equals(argFirst, "rm")) {
            repo.rm(args[1]);
        } else if (Objects.equals(argFirst, "log")) {
            repo.log();
        } else if (Objects.equals(argFirst, "checkout")) {
            if (args.length == 3 && args[1].equals("--")) {
                repo.checkoutOne(repo.getCurrHead(), args[2]);
            } else if (args.length == 4 && Objects.equals(args[2], "--")) {
                repo.checkoutOne(args[1], args[3]);
            } else if (args.length == 2) {
                repo.checkoutTwo(args[1]);
            } else {
                System.out.println("Incorrect operands.");
            }
        } else if (Objects.equals(argFirst, "find")) {
            repo.find(args[1]);
        } else if (Objects.equals(argFirst, "branch")) {
            repo.branch(args[1]);
        } else if (Objects.equals(argFirst, "global-log")) {
            repo.globalLog();
        } else if (Objects.equals(argFirst, "rm-branch")) {
            repo.removeBranch(args[1]);
        } else if (Objects.equals(argFirst, "reset")) {
            repo.reset(args[1]);
        } else if (Objects.equals(argFirst, "status")) {
            repo.status();
        } else if (Objects.equals(argFirst, "merge")) {
            repo.merge(args[1]);
        } else if (Objects.equals(argFirst, "diff")) {
            if (args.length == 1) {
                repo.diff();
            } else if (args.length == 2) {
                repo.diff2(args[1]);
            } else {
                repo.diff3(args[1], args[2]);
            }
        } else {
            System.out.println("No command with that name exists.");
        }
    }


}

