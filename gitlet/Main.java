package gitlet;

import java.io.File;
import java.io.IOException;

import static gitlet.Commit.LoadCommit;

public class Main {
    public static Tree tree;
    public static Staged staged;
    private static void printIncorrectOperandsErrMsg() {
        System.err.println("Incorrect operands.");
        System.exit(-1);
    }
    private static void printNotInitializedErrMsg() {
        System.err.println("Not in an initialized Gitlet directory.");
        System.exit(-1);
    }
    private static boolean initialProgram() {
        if (!new File(MyUtils.gitletDir).isDirectory()) {
            return false;
        }
        // load treeFile
        tree = Tree.GetTreeInstance(new File(MyUtils.gitletDir + "tree.txt"));
        // load staging
        staged = Staged.GetStagedInstance(new File(MyUtils.gitletDir + "staged.txt"));
        staged.updateStatus();
        return true;
    }

    private static void init() throws IOException{
        new File(MyUtils.gitletDir).mkdir();
        new File(MyUtils.commitDir).mkdir();
        new File(MyUtils.blobDir).mkdir();
        //new File(MyUtils.headDir).mkdir();
        // generate initial commit & log files
        new File(MyUtils.gitletDir + "tree.txt").createNewFile();
        tree  = Tree.GetTreeInstance();
        new File(MyUtils.gitletDir + "staged.txt").createNewFile();
        staged = Staged.GetStagedInstance();
    }

    private static void log(){
        Commit curCommit = LoadCommit(tree.get_headCommitId());
        String curId = curCommit.getCommitId();
        while(curId != null) {
            System.out.println("===");
            System.out.println("commit" + curId);
            String parentId = curCommit.getParentId();
            String secondParentId = curCommit.getSecondParentId();
            if (secondParentId != null) {
                System.out.println("Merge:" + parentId.substring(0, 7) + secondParentId.substring(0, 7));
            }
            System.out.println("Date: " + curCommit.getTimestamp());
            System.out.println(curCommit.getLogMsg());
            System.out.println("");
            curCommit = LoadCommit(parentId);
            curId = curCommit.getCommitId();
        }
    }

    private static void globalLog(){
        Commit curCommit = LoadCommit(tree.get_headCommitId());
        File commitDir = new File(MyUtils.gitletDir + "commit" + File.separator);
        for (String curCommitId : commitDir.list()){
            curCommit = LoadCommit(curCommitId);
            System.out.println("===");
            System.out.println("commit" + curCommitId);
            String secondParentId = curCommit.getSecondParentId();
            if (secondParentId != null) {
                System.out.println("Merge:" + curCommit.getParentId().substring(0, 7)
                        + secondParentId.substring(0, 7));
            }
            System.out.println("Date: " + curCommit.getTimestamp());
            System.out.println(curCommit.getLogMsg());
            System.out.println("");
        }
    }

    private static void find(String MsgToFind){
        File commitDir = new File(MyUtils.commitDir);
        boolean found = false;
        for (String curCommitId : commitDir.list()) {
            String Msg = LoadCommit(curCommitId).getLogMsg();
            if (Msg.contains(MsgToFind)) {
                System.out.println(curCommitId);
                found = true;
            }
        }
        if (!found) {
            System.err.println("Found no commit with that message.");
        }
    }

    public static void main(String[] args) throws Exception {
        int argsLength = args.length;
        if (argsLength == 0) {
            System.out.println("Please enter a command.");
            System.exit(-1);
        }
        String commandStr = args[0].toLowerCase();
        boolean initalReturnVal = initialProgram();
        switch(commandStr) {
            case "init":
                if (argsLength > 1) {
                    printIncorrectOperandsErrMsg();
                }
                if (initalReturnVal) {
                    System.err.println("A Gitlet version-control system already exists in the current directory.");
                    return;
                }
                init();
                break;
            // display information about each commit backwards along the commit tree until the initial commit
            // following the first parent commit links
            case "log":
                if (argsLength > 1) {
                    printIncorrectOperandsErrMsg();
                }
                if (!initalReturnVal) {
                    printNotInitializedErrMsg();
                }
                log();
                break;
            // print information about all commits ever made
            case "global-log":
                if (argsLength > 1) {
                    printIncorrectOperandsErrMsg();
                }
                if (!initalReturnVal) {
                    printNotInitializedErrMsg();
                }
                globalLog();
                break;
            // display all existing branches and staged or marked for untracking files
            case "status":
                if (argsLength > 1) {
                    printIncorrectOperandsErrMsg();
                }
                System.out.println("=== Branches ===");
                String currentBranch = tree.get_currentBranch();
                System.out.println("*"+currentBranch);
                for (String branch : tree.get_branchCommitMap().keySet() ){
                    if (!branch.equals(currentBranch)){
                        System.out.println(branch);
                    }
                }
                System.out.println();
                System.out.println("=== Staged Files ===");
                for (String f : staged.get_stagedFilesName()) {
                    System.out.println(f);
                }
                System.out.println();
                System.out.println("=== Removed Files ===");
                for (String f : staged.get_removedFiles()) {
                    System.out.println(f);
                }
                System.out.println();
                System.out.println("=== Modifications Not Staged For Commit ===");
                for (String f: staged.get_deletedFiles()){
                    System.out.println(f+" (deleted)");
                }
                for (String f: staged.get_modifiedFiles()) {
                    System.out.println(f + " (modified)");
                }
                System.out.println();
                System.out.println("=== Untracked Files ===");
                for (String f: staged.get_untrackedFiles()){
                    System.out.println(f);
                }
                break;
            // add a file to the staging area
            case "add":
                if (argsLength > 2) {
                    printIncorrectOperandsErrMsg();
                }
                if (!initalReturnVal) {
                    printNotInitializedErrMsg();
                }
                staged.addFile(args[1]);
                break;
            // save a snapshot of certain files in the staging area
            case "commit":
                if (!initalReturnVal) {
                    printNotInitializedErrMsg();
                }
                if (argsLength == 1 || args[1].isBlank()) {
                    System.err.println("Please enter a commit message.");
                } else if (argsLength > 2) {
                    printIncorrectOperandsErrMsg();
                } else {
                    Commit.makeCommit(args[1], null);
                }
                break;
            // unstage a currently staged file
            case "rm":
                if (argsLength > 2) {
                    printIncorrectOperandsErrMsg();
                }
                if (!initalReturnVal) {
                    printNotInitializedErrMsg();
                }
                staged.removeFile(args[1]);
                break;
            // print out the ids of all commits that have the given commit message
            case "find":
                if (argsLength > 2) {
                    printIncorrectOperandsErrMsg();
                }
                if (!initalReturnVal) {
                    printNotInitializedErrMsg();
                }
                find(args[1]);
                break;
            case "checkout":
                if (!initalReturnVal) {
                    printNotInitializedErrMsg();
                }
                if (argsLength == 2) tree.checkoutBranch(args[1]);
                else if (argsLength == 3 && args[1].equals("--")) tree.checkoutFile(args[2]);
                else if (argsLength == 4 && args[2].equals("--")) tree.checkoutCommitFile(args[1], args[3]);
                else printIncorrectOperandsErrMsg();
                break;
            // create a new branch with the given name, and points it at the current head node.
            case "branch":
                if (argsLength > 2) {
                    printIncorrectOperandsErrMsg();
                }
                if (!initalReturnVal) {
                    printNotInitializedErrMsg();
                }
                if (!tree.CreateNewBranch(args[1])) {
                    System.err.println("A branch with that name already exists.");
                }
                break;
            // delete the branch with the given name
            case "rm-branch":
                if (argsLength > 2) {
                    printIncorrectOperandsErrMsg();
                }
                if (!initalReturnVal) {
                    printNotInitializedErrMsg();
                }
                String removeReturnStr = tree.RemoveBranch(args[1]);
                if (removeReturnStr != null) {
                    System.err.println(removeReturnStr);
                }
                break;
            // check out all the files tracked by the given commit
            case "reset":
                if (argsLength > 2) {
                    printIncorrectOperandsErrMsg();
                }
                if (!initalReturnVal) {
                    printNotInitializedErrMsg();
                }
                tree.reset(args[1], false);
            case "merge":
                if (argsLength > 2) {
                    printIncorrectOperandsErrMsg();
                }
                if (!initalReturnVal) {
                    printNotInitializedErrMsg();
                }
                tree.merge(args[1]);
                break;
            default:
                System.out.println("No command with that name exists.");
                System.exit(-1);

        }
    }
}
