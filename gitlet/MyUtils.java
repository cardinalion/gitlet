package gitlet;

import java.io.File;
import java.io.PrintWriter;

public class MyUtils {

    public static String currentDir = "." + File.separator;
    public static String gitletDir = currentDir + ".gitlet" + File.separator;
    public static String commitDir = gitletDir + "commit" + File.separator;
    public static String blobDir = gitletDir + "blob" + File.separator;
    //public static String headDir = gitletDir + "head" + File.separator;

    public static void BlobToFile(String blobId){
        Blob blob = Blob.LoadBlob(blobId);
        File file = new File(currentDir + blob.getFileName());
        try (PrintWriter out = new PrintWriter(file)){
            out.print(blob.getContent());
        } catch (Exception e){}
    }

    public static void checkoutBlob(String blobId, String filename){
        if (blobId != null){
            BlobToFile(blobId);
            // unstage file
            Main.staged.RemoveFromStaged(filename);
        } else {
            System.err.println("File does not exist in that commit.");
        }
    }

    public static void checkoutBlobAndStage(String blobId, String filename){
        checkoutBlob(blobId, filename);
        Main.staged.addFile(filename);
    }
}
