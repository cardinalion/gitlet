package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;

public class Staged implements Serializable {
    private Map<String, String>_stagedFiles = new HashMap<>();
    private Set<String> _removedFiles = new HashSet<>();
    private Set<String> _deletedFiles = new HashSet<>();
    private Set<String> _untrackedFiles = new HashSet<>();
    private Set<String> _modifiedFiles = new HashSet<>();
    private static transient Staged staged = null;

    public static Staged GetStagedInstance(){
        if (staged == null) staged = new Staged();
        return staged;
    }

    public static Staged GetStagedInstance(File stagedFile) {
        if (staged == null) staged = Utils.readObject(stagedFile, Staged.class);
        return staged;
    }

    public void UpdateStagedLog() {
        Utils.writeObject(new File(MyUtils.gitletDir+ "staged.txt"),
                Utils.serialize(this));
    }

    public Set<String> get_stagedFilesName() {
        return _stagedFiles.keySet();
    }

    public Set<String> get_removedFiles() {
        return _removedFiles;
    }

    public Set<String> get_untrackedFiles() {
        return _untrackedFiles;
    }

    public Set<String> get_modifiedFiles() {
        return _modifiedFiles;
    }

    public Set<String> get_deletedFiles() {
        return _deletedFiles;
    }

    public void RemoveFromStaged(String filename){
        if (_stagedFiles.containsKey(filename)) {
            _stagedFiles.remove(filename);
        }
    }

    public void updateStatus(){
        File[] files = new File(MyUtils.currentDir).listFiles();
        Map<String, String> refMap = Commit.getRefMap(null);
        for (File file: files){
            if (file.isFile()){
                Blob temp = new Blob(file);
                String name = file.getName();
                if (refMap.containsKey(name)){
                    String compare = _stagedFiles.containsKey(name) ? _stagedFiles.get(name) : refMap.get(name);
                    if (Blob.LoadBlob(compare).getContent() != temp.getContent()){
                        _modifiedFiles.add(name);
                    }
                } else {
                    if (!_stagedFiles.containsKey(name)){
                        _untrackedFiles.add(name);
                    }
                }
            }
        }
        for (String filename: refMap.keySet()){
            File f = new File(MyUtils.currentDir + filename);
            if (!f.exists() && !_removedFiles.contains(filename)){
                _deletedFiles.add(filename);
            }
        }
        UpdateStagedLog();
    }

    public boolean untrackedExist(){
        return _untrackedFiles.size() > 0;
    }

    public boolean uncommitedExist() {
        return _removedFiles.size() > 0 || _deletedFiles.size() > 0;
    }

    public void clearStagingArea(){
        _stagedFiles = new HashMap<>();
        _removedFiles = new HashSet<>();
        updateStatus();
    }

    public void removeFile(String filename){
        if (_stagedFiles.containsKey(filename)){
            _stagedFiles.remove(filename);
        } else if (_untrackedFiles.contains(filename)){
            System.err.println("No reason to remove the file.");
            System.exit(-1);
        }
        new File(MyUtils.currentDir + filename).delete();
        _removedFiles.add(filename);
        UpdateStagedLog();
    }

    public void addFile(String filename){
        File fileToAdd = new File(MyUtils.currentDir + filename);
        if (!fileToAdd.exists()) {
            if (_deletedFiles.contains(filename)){
                _deletedFiles.remove(filename);
                _removedFiles.add(filename);
            } else if (!_removedFiles.contains(filename)){
                System.err.println("File does not exist.");
            }
            return;
        }
        Map<String, String> refMap = Commit.LoadCommit(Main.tree.get_headCommitId()).getReferenceMap();
        Blob temp = new Blob(fileToAdd);
        if (refMap.containsKey(filename)
                && Blob.LoadBlob(refMap.get(filename)).getContent().equals(temp.getContent())){
            if (_stagedFiles.containsKey(filename)){
                new File(MyUtils.blobDir + _stagedFiles.get(filename)).delete();
            }
            new File(MyUtils.blobDir + temp.getBlobId()).delete();
        } else {
            _stagedFiles.put(filename, temp.getBlobId());
        }
        UpdateStagedLog();
    }
}