package gitlet;

import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;


public class Commit implements Serializable {
    /**
     * Variables needed for each commit instance.
     */
    private String _message;
    /**
     * Variables needed for each commit instance.
     */
    private String _parentCommit;
    /**
     * Variables needed for each commit instance.
     */
    private String _mergedParentCommit;
    /**
     * Variables needed for each commit instance.
     */
    private Date _time;
    /**
     * Variables needed for each commit instance.
     */
    private HashMap<String, String> _blobs;
    /**
     * Variables needed for each commit instance.
     */
    private boolean _isMerged;
    /**
     * file path.
     */
    private static File cwd = new File(".");
    /**
     * file path.
     */
    private static File gitlet = new File(cwd + "/.gitlet");

    public Commit(String msg, long t, String parent) {
        _message = msg;
        _time = new Date(t);
        _parentCommit = parent;
        _blobs = new HashMap<>();
        _isMerged = false;
    }

    public Commit(String msg, long t, String parent, String mergedParent) {
        _message = msg;
        _time = new Date(t);
        _parentCommit = parent;
        _mergedParentCommit = mergedParent;
        _blobs = new HashMap<>();
        _isMerged = true;
    }

    boolean isMerged() {
        return _isMerged;
    }

    String getParent() {
        return _parentCommit;
    }

    String getMergedParent() {
        return _mergedParentCommit;
    }

    HashMap<String, String> getBlobs() {
        return _blobs;
    }

    String getBlob(String fileName) {
        if (_blobs.containsKey(fileName)) {
            return _blobs.get(fileName);
        } else {
            return "";
        }
    }

    void setBlobs(HashMap<String, String> b) {
        _blobs = b;
    }

    String getMessage() {
        return _message;
    }

    boolean blobExist(String fileName) {
        if (_blobs.containsKey(fileName)) {
            return true;
        }
        return false;
    }

    /**
     * Getting commit information for log for this commit obj.
     * @return String
     */
    String getLog() {
        SimpleDateFormat simpleDateFormat
                = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy");
        String divider = "=== \n";
        String commitIdInfo = "commit "
                + Utils.sha1(Utils.serialize(this)) + "\n";
        String date = "Date: " + simpleDateFormat.format(this._time)
                + " -0800" + "\n";
        String message = this.getMessage();
        return divider + commitIdInfo + date + message + "\n";
    }







}
