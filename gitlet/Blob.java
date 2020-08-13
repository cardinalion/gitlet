package gitlet;

import java.io.*;

import static gitlet.MyUtils.blobDir;

public class Blob implements Serializable {
    private final String fileName;
    private final String blobId;
    private String content;

    public Blob(String fileName){
        this.fileName = fileName;
        content = Utils.readContentsAsString(new File(MyUtils.currentDir + this.fileName));
        byte serializedResult [] = Utils.serialize(this);
        blobId = Utils.sha1("BLOB", serializedResult);
        File blobFile = new File(blobDir + blobId);
        if (!blobFile.exists()) {
            Utils.writeObject(blobFile, serializedResult);
        }
    }

    public Blob(File file){
        fileName = file.getName();
        content = Utils.readContentsAsString(file);
        byte serializedResult [] = Utils.serialize(this);
        blobId = Utils.sha1("BLOB", serializedResult);
    }

    public String getFileName(){
        return fileName;
    }

    public String getContent(){
        return content;
    }

    public String getBlobId(){
        return blobId;
    }

    public static Blob LoadBlob(String blobId) {
        File blobToLoad = new File(blobDir  + blobId);
        Blob loadedBlob = Utils.readObject(blobToLoad, Blob.class);
        return loadedBlob;
    }

    public static boolean compareContent(String blobId1, String blobId2){
        if (blobId1 == null || blobId1 == null) return false;
        return LoadBlob(blobId1).getContent().equals(LoadBlob(blobId2).getContent());
    }
}
