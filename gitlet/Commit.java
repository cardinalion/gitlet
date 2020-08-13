package gitlet;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class Commit implements Serializable {
    private String logMsg;
    private String timestamp;
    private String parentId = null;
    private String secondParentId = null;
    private Map<String, String> referenceMap = new HashMap<>();
    private final String commitId;

    private String FormatTimestamp(Date date){
        SimpleDateFormat df = new SimpleDateFormat("EEE MMM dd HH:mm:ss YYYY ZZZZ");
        df.setTimeZone(TimeZone.getTimeZone("PST"));
        return df.format(date);
    }

    private void WriteCommit(byte[] serializedResult) {
        Utils.writeObject(new File(MyUtils.commitDir + commitId), serializedResult);
    }

    public String getCommitId() {
        return commitId;
    }

    public String getLogMsg() {
        return logMsg;
    }

    public Map<String, String> getReferenceMap() {
        return referenceMap;
    }

    public String getParentId() {
        return parentId;
    }

    public String getSecondParentId() {
        return secondParentId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    // first commit
    public Commit(){
        logMsg = "initial commit";
        timestamp = FormatTimestamp(new Date(0));
        byte serializedResult [] = Utils.serialize(this);
        commitId = Utils.sha1("COMMIT", serializedResult);
        WriteCommit(serializedResult);

    }
    public Commit(String logMsg, String parent, Map<String, String> refMap, String secondParentId){
        this.logMsg = logMsg;
        timestamp = FormatTimestamp(new Date());
        parentId = parent;
        this.secondParentId = secondParentId;
        referenceMap = refMap;
        byte serializedResult [] = Utils.serialize(this);
        commitId = Utils.sha1("COMMIT", serializedResult);
        WriteCommit(serializedResult);
    }

    public static Commit LoadCommit(String commitId) {
        File commitToLoad = new File(MyUtils.commitDir + commitId);
        if (commitToLoad.exists()){
            Commit loadedCommit = Utils.readObject(commitToLoad, Commit.class);
            return loadedCommit;
        }
        return null;
    }

    public static void makeCommit(String msg, String secondParentId){
        // load parent commit
        Map<String, String> refMap = getRefMap(null);
        // create new commit
        for (String f: Main.staged.get_removedFiles()){
            refMap.remove(f);
        }
        for (String f: Main.staged.get_stagedFilesName()){
            refMap.put(f, new Blob(f).getBlobId());
        }
        String newCommitId = new Commit(msg,  Main.tree.get_headCommitId(), refMap, secondParentId).getCommitId();
        // update HEAD & logFile
        Main.tree.set_headCommitId(newCommitId);
        Main.tree.get_branchCommitMap().put(Main.tree.get_currentBranch(), newCommitId);
        Main.tree.UpdateTreeFile();
        Main.staged.clearStagingArea();
    }

    public static Map<String, String> getRefMap(String commitId) {
        if (commitId == null) commitId = Main.tree.get_headCommitId();
        return LoadCommit(commitId).referenceMap;
    }

    //TODO
    private static List<String> commitHistory(String commitId) {
        List<String> history = new LinkedList<>();
        Queue<String> queue = new LinkedList<>();
        queue.add(commitId);
        while(queue.size() > 0) {
            String id = queue.poll();
            Commit curCommit = LoadCommit(id);
            String parentId = curCommit.getParentId();
            queue.offer(parentId);
            String second = curCommit.getSecondParentId();
            if (second!= null) queue.offer(second);
            history.add(id);
        }
        return history;
    }

    public static String LatestCommonAncestor(String commitId1, String commitId2){
        List<String> history1 = commitHistory(commitId1);
        List<String> history2 = commitHistory(commitId2);
        // closest to the commitId1
        for (String h: history1){
            for (String s: history2){
                if (h.equals(s)) return h;
            }
        }
        return null;
    }

}
