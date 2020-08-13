package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Tree implements Serializable {
    private HashMap<String, String> _branchCommitMap = new HashMap<>();
    private String _currentBranch;
    private String _headCommitId;
    private static transient Tree tree = null;

    // init
    private Tree() throws IOException {
        String firstCommitId = new Commit().getCommitId();
        _branchCommitMap.put("master", firstCommitId);
        _currentBranch = "master";
        _headCommitId = firstCommitId;
        UpdateTreeFile();
    }

    public static Tree GetTreeInstance() throws IOException {
        if (tree == null) tree = new Tree();
        return tree;
    }

    public static Tree GetTreeInstance(File treeFile) {
        if (tree == null) tree = Utils.readObject(treeFile, Tree.class);
        return tree;
    }

    public String get_currentBranch() {
        return _currentBranch;
    }

    public String get_headCommitId() {
        return _headCommitId;
    }

    public void set_headCommitId(String head){
        _headCommitId = head;
    }

    public HashMap<String, String> get_branchCommitMap() {
        return _branchCommitMap;
    }

    public void UpdateTreeFile() {
        Utils.writeObject(new File(MyUtils.gitletDir+ "tree.txt"),
                Utils.serialize(this));
    }

    public boolean CreateNewBranch(String name) {
        if (_branchCommitMap.containsKey(name)) return false;
        _branchCommitMap.put(name, _headCommitId);
        UpdateTreeFile();
        return true;
    }

    public String RemoveBranch(String name) {
        if (!_branchCommitMap.containsKey(name)) return "A branch with that name does not exist.";
        if (name.equals(_currentBranch)) return "Cannot remove the current branch.";
        _branchCommitMap.remove(name);
        return null;
    }

    public void checkoutFile(String filename){
        checkoutCommitFile(get_headCommitId(), filename);
    }

    public void checkoutCommitFile(String commitId, String filename){
        // load commit
        Commit commit = Commit.LoadCommit(commitId);
        if (commit == null){
            System.err.println("No commit with that id exists.");
            return;
        }
        String blobId = commit.getReferenceMap().get(filename);
        MyUtils.checkoutBlob(blobId, filename);
    }

    public void reset(String commitId, boolean isCheckoutBranch){
        if(Main.staged.untrackedExist()){
            System.err.println("There is an untracked file in the way; delete it or add it first.");
            return;
        }
        Commit commit = Commit.LoadCommit(commitId);
        if (commit == null){
            System.err.println("No commit with that id exists.");
            return;
        }
        // delete all files in current directory
        for (File fileInCurDir : new File(MyUtils.currentDir).listFiles()){
            if (fileInCurDir.isFile()) fileInCurDir.delete();
        }
        // fetch all files from the branch
        Map<String, String> referenceMap = commit.getReferenceMap();
        for (String blobId : referenceMap.values()){
            MyUtils.BlobToFile(blobId);
        }
        _headCommitId = commitId;
        Main.staged.clearStagingArea();
        if (isCheckoutBranch) return;
        _branchCommitMap.put(_currentBranch, _headCommitId);
        UpdateTreeFile();
    }

    public void checkoutBranch (String name) {
        if (name.equals(_currentBranch)) {
            System.err.println("No need to checkout the current branch.");
            return;
        }
        if (_branchCommitMap.containsKey(name)) {
            reset(_branchCommitMap.get(name), true);
            // change head from commit to branch
            _currentBranch = name;
            UpdateTreeFile();
        } else {
            System.err.println("No such branch exists.");
        }
    }

    public void merge(String branch){
        if (Main.staged.uncommitedExist()){
            System.err.println("You have uncommitted changes.");
            System.exit(-1);
        }
        if (!_branchCommitMap.containsKey(branch)){
            System.err.println("No such branch exists.");
            System.exit(-1);
        }
        if (branch.equals(_currentBranch)){
            System.err.println("Cannot merge a branch with itself.");
            System.exit(-1);
        }
        if (Main.staged.untrackedExist()){
            System.err.println("There is an untracked file in the way; delete it or add it first");
            System.exit(-1);
        }
        // also check commit error if nothing changed
        String branchCommitId = _branchCommitMap.get(branch);
        String LCA = Commit.LatestCommonAncestor(_headCommitId, branchCommitId);

        if (branchCommitId.equals(LCA)){
            System.out.println("Given branch is an ancestor of the current branch.");
            System.exit(0);
        }
        if (_headCommitId.equals(LCA)){
            _branchCommitMap.put(_currentBranch, branchCommitId);
            System.out.println("Current branch fast-forwarded.");
            System.exit(0);
        }

        Map<String, String> headMap = Commit.getRefMap(_headCommitId);
        Map<String, String> mergeRefMap = Commit.getRefMap(branchCommitId);
        Map<String, String> LCARefMap = Commit.getRefMap(LCA);
        Set<String> fileSet = new HashSet<>();
        fileSet.addAll(headMap.keySet());
        fileSet.addAll(mergeRefMap.keySet());
        fileSet.addAll(LCARefMap.keySet());
        boolean conflict = false;
        for (String f: fileSet){
            boolean inHead = headMap.containsKey(f), inBranch = mergeRefMap.containsKey(f),
                    inLCA = LCARefMap.containsKey(f);
            String headBlobId = headMap.get(f), branchBlobId = mergeRefMap.get(f), LCABlobId = LCARefMap.get(f);
            if (headBlobId.equals(LCABlobId) && !branchBlobId.equals(LCABlobId)){
                // stage branchBlobId
                MyUtils.checkoutBlobAndStage(branchBlobId, f);
            } else if (headBlobId.equals(branchBlobId) && !headBlobId.equals(LCABlobId)) {
                // do nothing
                // If a file is removed in both, but a file of that name is present in the working directory
                // that file is not removed from the working directory
                // (but it continues to be absent—not staged—in the merge).
            } else if (!inHead && inBranch && !inLCA){
                // stage branchBlobId
                MyUtils.checkoutBlobAndStage(branchBlobId, f);
            } else if (inLCA && LCABlobId.equals(headBlobId) && !inBranch){
                // remove
                Main.staged.removeFile(f);
            } else if (!headBlobId.equals(LCABlobId) && !branchBlobId.equals(LCABlobId)
                    && !headBlobId.equals(branchBlobId)) {
                // in conflict
                conflict = true;
                String conflictStr = "<<<<<<< HEAD" + System.lineSeparator();
                conflictStr += inHead ? Blob.LoadBlob(headBlobId).getContent() : ""
                        + "=======" + System.lineSeparator();
                conflictStr += inBranch ? Blob.LoadBlob(branchBlobId).getContent() : ""
                        + ">>>>>>>" + System.lineSeparator();
                File file = new File(MyUtils.currentDir + f);
                try (PrintWriter out = new PrintWriter(file)) {
                    out.print(conflictStr);
                } catch (Exception e) {
                }
                Main.staged.addFile(f);
            }
        }
        String msg = "Merged " + branch + " into " + _currentBranch + ".";
        Commit.makeCommit(msg, branchCommitId);
        if (conflict) {
            System.out.println("Encountered a merge conflict.");
        }
    }
}
