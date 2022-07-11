package gitlet;

import java.io.File;
import java.util.HashMap;

/**
 * Staging class for Gitlet, the tiny stupid version-control system.
 * @author Jeewoo lee
 */
public class Stage {
    /**
     * StageAdd.
     */
    private static HashMap<String, String> stagedAdd;
    /**
     * StageRemove.
     */
    private static HashMap<String, String> stagedRemove;

    static void stageFileAdd(String fileName, String hash) {
        stagedAdd.put(fileName, hash);
    }

    static void stageFileRemove(String fileName, String hash) {
        stagedRemove.put(fileName, hash);
    }

    static void clear() {
        stagedAdd = new HashMap<String, String>();
        stagedRemove = new HashMap<String, String>();
        Utils.writeObject(Command.add(), stagedAdd);
        Utils.writeObject(Command.remove(), stagedRemove);

    }

    static boolean exists(String filename) {
        if (stagedAdd.containsKey(filename)
                || stagedRemove.containsKey(filename)) {
            return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    static boolean existsRemove(String filename) {
        stagedRemove = Utils.readObject(Command.remove(), HashMap.class);
        if (stagedRemove.containsKey(filename)) {
            return true;
        }
        return false;
    }

    void remove(String filename) {
        if (stagedAdd.containsKey(filename)) {
            stagedAdd.remove(filename);
        } else if (stagedRemove.containsKey(filename)) {
            stagedRemove.remove(filename);
        }
    }


    @SuppressWarnings("unchecked")
    static void doAdd(File file) {
        Commit commit = Command.getCurrentCommit();
        Blob newBlob = new Blob(Utils.readContents(file));
        String fileHash = Utils.sha1(Utils.serialize(newBlob));
        String fileName = file.getName();
        File blobFile = new File(Command.blobs() + "/" + fileHash + ".txt");
        stagedAdd = new HashMap<String, String>();
        stagedRemove = new HashMap<String, String>();
        String thisDoNothing = "";

        if (!Utils.readContentsAsString(Command.add()).equals("")) {
            stagedAdd = Utils.readObject(Command.add(), HashMap.class);
        }

        if (!Utils.readContentsAsString(Command.remove()).equals("")) {
            stagedRemove = Utils.readObject(Command.remove(), HashMap.class);
        }

        if (stagedRemove.containsKey(fileName)) {
            stagedRemove.remove(fileName);
        }

        if (commit.getBlob(fileName).equals(fileHash)
                && stagedAdd.containsKey(fileName)) {
            stagedAdd.remove(fileName);
        } else if (commit.getBlob(fileName).equals(fileHash)) {
            thisDoNothing = "";
        } else {
            stagedAdd.put(fileName, fileHash);
        }

        Utils.writeObject(Command.add(), stagedAdd);
        Utils.writeObject(Command.remove(), stagedRemove);
        Utils.writeContents(blobFile, Utils.readContentsAsString(file));
    }
}
