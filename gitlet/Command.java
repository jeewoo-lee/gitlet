package gitlet;

import java.io.File;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** Command class for Gitlet, the tiny stupid version-control system.
 *  @author Jeewoo lee
 */

public class Command {
    /**
     * File.
     */
    private static File cwd = new File(System.getProperty("user.dir"));
    /**
     * File.
     */
    private static File gitlet = new File(cwd + "/.gitlet");
    /**
     * File.
     */
    private static File head = new File(gitlet + "/HEAD.txt");
    /**
     * File.
     */
    private static File index = new File(gitlet + "/index");
    /**
     * File.
     */
    private static File objects = new File(gitlet + "/objects");
    /**
     * File.
     */
    private static File ref = new File(gitlet + "/ref");
    /**
     * File.
     */
    private static File master = new File(ref + "/master.txt");
    /**
     * File.
     */
    private static Stage stage;
    /**
     * File.
     */
    private static File add = new File(Command.index + "/add.txt");
    /**
     * File.
     */
    private static File remove = new File(Command.index + "/remove.txt");
    /**
     * File.
     */
    private static File logs = new File(gitlet + "/logs");
    /**
     * File.
     */
    private static File blobs = new File(gitlet + "/blobs");
    /**
     * File.
     */
    private static File branch = new File(gitlet + "/branch.txt");

    static File cwd() {
        return cwd;
    }

    static File gitlet() {
        return gitlet;
    }

    static File head() {
        return head;
    }

    static File index() {
        return index;
    }

    static File objects() {
        return objects;
    }

    static File ref() {
        return ref;
    }

    static File add() {
        return add;
    }

    static File remove() {
        return remove;
    }

    static File logs() {
        return logs;
    }

    static File blobs() {
        return blobs;
    }

    static File branch() {
        return branch;
    }

    static void init() throws IOException {
        if (gitlet.exists()) {
            System.out.println("A Gitlet version-control "
                    + "system already exists in the current directory.");
        } else {
            gitlet.mkdir();
            index.mkdir();
            objects.mkdir();
            ref.mkdir();
            logs.mkdir();
            head.createNewFile();
            master.createNewFile();
            branch.createNewFile();
            blobs.mkdir();
            stage = new Stage();
            Utils.writeContents(head, "ref/master.txt");
            Utils.writeContents(branch, "master");

            Commit first = new Commit("initial commit", 0, "");
            String hash = Utils.sha1(Utils.serialize(first));
            File firstCommit = new File(objects + "/" + hash + ".txt");
            firstCommit.createNewFile();
            Utils.writeObject(firstCommit, first);

            add.createNewFile();
            remove.createNewFile();

            Utils.writeContents(master, hash);
            Utils.writeObject(add, new HashMap<String, String>());
            Utils.writeObject(remove, new HashMap<String, String>());




        }

    }

    static void diff1() throws IOException {

    }

    static void add(String fileName) {
        File file = new File(cwd + "/" + fileName);
        if (!file.exists() & !checkRemoved(file)) {
            System.out.println("File does not exist.");
        } else {
            Stage.doAdd(file);
        }

    }

    @SuppressWarnings("unchecked")
    static void merge(String otherBranchName) throws IOException {
        HashMap<String, String> added = Utils.readObject(add, HashMap.class);
        HashMap<String, String> removed =
                Utils.readObject(remove, HashMap.class);
        String headBranchName = Utils.readContentsAsString(branch);
        Commit headCommit = getCurrentCommit();
        HashMap<String, String> headFiles = headCommit.getBlobs();
        File otherBranchFile = new File(ref + "/" + otherBranchName + ".txt");
        List<String> workingFiles = Utils.plainFilenamesIn(cwd);
        if (!added.isEmpty() || !removed.isEmpty()) {
            System.out.println("You have uncommitted changes."); return;
        }
        if (headBranchName.equals(otherBranchName)) {
            System.out.println("Cannot merge a branch with itself."); return;
        }
        if (!otherBranchFile.exists()) {
            System.out.println("A branch with that name does not exist.");
            return;
        }
        for (int i = 0; i < workingFiles.size(); i++) {
            if (!headFiles.containsKey(workingFiles.get(i))
                    && !added.containsKey(workingFiles.get(i))
                    && !workingFiles.get(i).equals(".DS_Store")) {
                System.out.println("There is an untracked "
                        + "file in the way; "
                        + "delete it, or add and commit it first.");
                return;
            }
        }
        String otherCommitID = Utils.readContentsAsString(otherBranchFile);
        String splitCommitID = Merge.advancedSplit(otherBranchName);
        if (splitCommitID.equals(otherCommitID)) {
            System.out.println("Given branch is an ancestor "
                    + "of the current branch.");
            return;
        } else if (currentCommitHash().equals(splitCommitID)) {
            checkoutBranch(otherBranchName);
            System.out.println("Current branch fast-forwarded.");
            return;
        }
        Commit splitCommit = getCommit(splitCommitID);
        Commit otherCommit = getCommit(otherCommitID);
        HashMap<String, String> splitFiles = splitCommit.getBlobs();
        HashMap<String, String> otherFiles = otherCommit.getBlobs();

        HashMap<String, String> newBlobs = Merge.merge(headFiles,
                splitFiles, otherFiles);
        String msg = "Merged " + otherBranchName
                + " into "  + headBranchName + ".";
        mergeCommit(msg, currentCommitHash(), otherCommitID, newBlobs);
        createNewBlobs(newBlobs);
        conflicted(new File(Command.cwd + "/" + "conflict" + ".txt"));

    }

    static void createNewBlobs(HashMap<String, String> newBlobs)
            throws IOException {
        for (String fileName: newBlobs.keySet()) {
            File file = new File(cwd + "/" + fileName);
            if (!file.exists()) {
                file.createNewFile();
            }
            String fileHash = newBlobs.get(fileName);
            File blobFile = new File(blobs + "/" + fileHash + ".txt");
            if (!blobFile.exists()) {
                blobFile.createNewFile();
            }
            String fileContents = Utils.readContentsAsString(blobFile);
            Utils.writeContents(file, fileContents);
        }

    }

    static void conflicted(File conflicted) {
        if (conflicted.exists()) {
            System.out.println(Utils.readContentsAsString(conflicted));
            conflicted.delete();
        }
    }

    static void mergeCommit(String msg, String headParentHash,
                            String mergeParentHash,
                            HashMap<String, String> theblobs)
            throws IOException {
        if (theblobs.isEmpty()) {
            System.out.println("No changes added to the commit.");
            return;
        }
        Commit thisCommit = new Commit(msg,
                ZonedDateTime.now().toInstant().toEpochMilli(),
                headParentHash, mergeParentHash);
        String hash = Utils.sha1(Utils.serialize(thisCommit));
        thisCommit.setBlobs(theblobs);
        File commitFile = new File(objects + "/" + hash + ".txt");
        commitFile.createNewFile();
        Utils.writeObject(commitFile, thisCommit);

        String headPath = Utils.readContentsAsString(head);
        File currentBranch = new File(gitlet + "/" + headPath);
        Utils.writeContents(currentBranch, hash);

    }

    @SuppressWarnings("unchecked")
    static void checkoutBranch(String branchName)
            throws IOException {
        List<String> branchPaths = Utils.plainFilenamesIn(ref);
        List<String> branches = new ArrayList<>();
        Commit currentCommit = getCurrentCommit();
        HashMap<String, String> stageAdd = Utils.readObject(add, HashMap.class);
        String currentBranch = Utils.readContentsAsString(branch);
        List<String> workingFiles = Utils.plainFilenamesIn(cwd);

        for (String path: branchPaths) {
            branches.add(path.replace(".txt", ""));
        }

        if (!branches.contains(branchName)) {
            System.out.println("No such branch exists.");
            return;
        } else if (currentBranch.equals(branchName)) {
            System.out.println("No need to checkout the current branch.");
            return;
        }
        File theBranch = new File(ref + "/" + branchName + ".txt");
        String commitHash = Utils.readContentsAsString(theBranch);
        Commit commit = getCommit(commitHash);

        for (String fileName: workingFiles) {
            if (!currentCommit.getBlobs().containsKey(fileName)
                    && !fileName.equals(".DS_Store")) {
                System.out.println("There is an "
                        + "untracked file in the way; delete it, "
                        + "or add and commit it first.");
                return;
            }
        }

        for (String toDelete: currentCommit.getBlobs().keySet()) {
            if (!commit.blobExist(toDelete)) {
                File delete = new File(cwd + "/" + toDelete);
                Utils.restrictedDelete(delete);
            }
        }

        for (String toAdd: commit.getBlobs().keySet()) {
            File newFile = new File(cwd + "/" + toAdd);
            String fileHash = commit.getBlob(toAdd);
            File blobFile = new File(blobs + "/" + fileHash + ".txt");
            Utils.writeContents(newFile, Utils.readContentsAsString(blobFile));
        }

        Stage.clear();
        Utils.writeContents(head, "ref" + "/" + branchName + ".txt");
        Utils.writeContents(branch, branchName);

    }

    static void checkoutFile(String fileName) throws IOException {
        Commit currentCommit = getCurrentCommit();
        checkoutHelper(currentCommit, fileName);
    }

    static void checkoutCommit(String commitID,
                               String fileName) throws IOException {
        File expectedCommitFile = new File(objects + "/" + commitID + ".txt");
        if (5 < commitID.length()) {
            File[] commits = objects.listFiles();
            for (File commit: commits) {
                if (commit.getName().contains(commitID)) {
                    commitID = commit.getName();
                    expectedCommitFile = new File(objects + "/" + commitID);
                }
            }
        }


        if (!expectedCommitFile.exists()) {
            System.out.println("No commit with that id exists.");
            return;
        }
        Commit commit = Utils.readObject(expectedCommitFile, Commit.class);
        checkoutHelper(commit, fileName);
    }

    static void checkoutHelper(Commit currentCommit,
                               String fileName)
            throws IOException {
        if (!currentCommit.blobExist(fileName)) {
            System.out.println("File does not exist in that commit.");
        } else {
            File newFile = new File(cwd + "/" + fileName);
            String fileHash = currentCommit.getBlob(fileName);
            File blobFile = new File(blobs + "/" + fileHash + ".txt");
            if (!newFile.exists()) {
                newFile.createNewFile();
            }
            Utils.writeContents(newFile, Utils.readContentsAsString(blobFile));
        }
    }

    @SuppressWarnings("unchecked")
    static void commit(String msg) throws IOException {
        if (Utils.readObject(Command.add, HashMap.class).equals("")) {
            return;
        }
        HashMap<String, String> stagedForAdd =
                Utils.readObject(Command.add, HashMap.class);
        HashMap<String, String> stagedForRemove =
                Utils.readObject(Command.remove, HashMap.class);
        if (stagedForAdd.isEmpty() && stagedForRemove.isEmpty()) {
            System.out.println("No changes added to the commit.");
            return;
        }

        Commit parent = getCurrentCommit();
        Commit thisCommit = new Commit(msg,
                ZonedDateTime.now().toInstant().toEpochMilli(),
                currentCommitHash());
        thisCommit.setBlobs(parent.getBlobs());
        HashMap<String, String> thisCommitBlobs = thisCommit.getBlobs();

        for (String k : stagedForAdd.keySet()) {
            if (thisCommitBlobs.containsKey(k)) {
                thisCommitBlobs.replace(k, stagedForAdd.get(k));
            } else {
                thisCommitBlobs.put(k, stagedForAdd.get(k));
            }
        }

        for (String k : stagedForRemove.keySet()) {
            if (!thisCommitBlobs.containsKey(k)) {
                System.out.println("This shouldn't happen");
            } else {
                thisCommitBlobs.remove(k);
            }
        }

        thisCommit.setBlobs(thisCommitBlobs);
        String hash = Utils.sha1(Utils.serialize(thisCommit));
        File commitFile = new File(objects + "/" + hash + ".txt");
        commitFile.createNewFile();
        Utils.writeObject(commitFile, thisCommit);

        String headPath = Utils.readContentsAsString(head);
        File currentBranch = new File(gitlet + "/" + headPath);
        Utils.writeContents(currentBranch, hash);
        Stage.clear();

    }

    static String currentCommitHash() {
        String headPath = Utils.readContentsAsString(head);
        return Utils.readContentsAsString(new File(gitlet + "/" + headPath));
    }

    static Commit getCommit(String commitID) {
        File commitFile = new File(objects + "/" + commitID + ".txt");
        return Utils.readObject(commitFile, Commit.class);
    }

    static Commit getCurrentCommit() {
        if (!gitlet.exists()) {
            System.out.println("Gitlet isn't initialized");
            System.exit(-1);
            return null;
        } else {
            return getCommit(currentCommitHash());
        }
    }

    static boolean checkRemoved(File file) {
        if (!file.exists()
                && getCurrentCommit().getBlobs()
                .containsKey(file.getName())) {
            return true;
        }
        return false;
    }

    static void log() {
        Commit commit = getCurrentCommit();
        while (commit != null) {
            System.out.println(commit.getLog());
            if (commit.getParent() == "") {
                break;
            } else {
                commit = Utils.readObject(new File(objects
                        + "/" + commit.getParent() + ".txt"), Commit.class);
            }
        }
    }

    static void globalLog() {
        List<String> stringCommits = Utils.plainFilenamesIn(objects);
        for (String commitFilePath: stringCommits) {
            String commitID = commitFilePath.replace(".txt", "");
            System.out.println(getCommit(commitID).getLog());
        }

    }

    static void find(String msg) {
        List<String> commitPaths = Utils.plainFilenamesIn(objects);
        List<String> commitIDs = new ArrayList<>();
        boolean found = false;

        for (String path: commitPaths) {
            commitIDs.add(path.replace(".txt", ""));
        }

        for (String id: commitIDs) {
            if (getCommit(id).getMessage().equals(msg)) {
                System.out.println(id);
                found = true;
            }
        }

        if (!found) {
            System.out.println("Found no commit with that message.");
        }


    }

    @SuppressWarnings("unchecked")
    static void rm(String fileName) {
        Commit currentCommit = getCurrentCommit();
        HashMap<String, String> stagedForRemove =
                Utils.readObject(Command.remove, HashMap.class);
        HashMap<String, String> stagedForAdd =
                Utils.readObject(Command.add, HashMap.class);

        if (!(currentCommit.blobExist(fileName))
                && !(stagedForAdd.containsKey(fileName))) {
            System.out.println("No reason to remove the file.");
            return;
        }

        if (stagedForAdd.containsKey(fileName)) {
            stagedForAdd.remove(fileName);
        }

        if (currentCommit.blobExist(fileName)) {
            stagedForRemove.put(fileName, currentCommit.getBlob(fileName));
            File fileToDelete = new File(cwd + "/" + fileName);
            if (fileToDelete.exists()) {
                Utils.restrictedDelete(cwd + "/" + fileName);
            }
        }

        Utils.writeContents(Command.remove, Utils.serialize(stagedForRemove));
        Utils.writeContents(Command.add, Utils.serialize(stagedForAdd));

    }

    static void branch(String branchName) throws IOException {
        File newBranch = new File(ref + "/" + branchName + ".txt");
        if (newBranch.exists()) {
            System.out.println("A branch with that name already exists.");
            return;
        }
        newBranch.createNewFile();
        String currentCommit = currentCommitHash();
        Utils.writeContents(newBranch, currentCommit);


    }

    static void rmbranch(String branchName) throws IOException {
        List<String> branchPaths = Utils.plainFilenamesIn(ref);
        List<String> branches = new ArrayList<>();
        String currentBranch = Utils.readContentsAsString(branch);

        for (String path: branchPaths) {
            branches.add(path.replace(".txt", ""));
        }

        if (!branches.contains(branchName)) {
            System.out.println("A branch with that name does not exist.");
        } else if (currentBranch.equals(branchName)) {
            System.out.println("Cannot remove the current branch.");
        } else {
            File theBranch = new File(ref + "/" + branchName + ".txt");
            theBranch.delete();
        }
    }

    static void statusPrintBranches(List<String> branchPaths,
                                    String currBranch) {
        for (String path: branchPaths) {
            String name = path.replace(".txt", "");
            if (name.equals(currBranch)) {
                System.out.println("*" + name);
            } else {
                System.out.println(name);
            }
        }
    }

    @SuppressWarnings("unchecked")
    static void status() {
        if (!gitlet.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        }
        List<String> branchPaths = Utils.plainFilenamesIn(ref);
        Collections.sort(branchPaths);
        String currBranch = Utils.readContentsAsString(branch);
        System.out.println("=== Branches ===");
        statusPrintBranches(branchPaths, currBranch);
        HashMap<String, String> stagedAdd = new HashMap<>();
        HashMap<String, String> stagedRemove = new HashMap<>();
        if (!Utils.readContentsAsString(Command.add).equals("")) {
            stagedAdd = Utils.readObject(Command.add, HashMap.class);
        }
        if (!Utils.readContentsAsString(Command.remove).equals("")) {
            stagedRemove = Utils.readObject(Command.remove, HashMap.class);
        }
        Set<String> stagedAddSet = stagedAdd.keySet();
        Set<String> stagedRemoveSet = stagedRemove.keySet();
        Set<String> stagedSet = new TreeSet<>();
        stagedSet.addAll(stagedAddSet);
        System.out.println("\n=== Staged Files ===");
        for (String file: stagedSet) {
            System.out.println(file);
        }
        Commit currCommit = getCurrentCommit();
        HashMap<String, String> currCommitBlobs = currCommit.getBlobs();
        List<String> workingFiles = Utils.plainFilenamesIn(cwd);
        List<String> removed = new ArrayList<>();
        for (String rmFile: stagedRemoveSet) {
            if (!removed.contains(rmFile)) {
                removed.add(rmFile);
            }
        }
        System.out.println("\n=== Removed Files ===");
        if (removed.size() > 0) {
            Collections.sort(removed);
            for (String r: removed) {
                System.out.println(r);
            }
        }
        List<String> tracked = new ArrayList<>();
        tracked.addAll(stagedSet);
        tracked.addAll(removed);
        tracked.addAll(currCommitBlobs.keySet());
        Collections.sort(tracked);
        HashMap<String, String> modified = new HashMap<>();
        statusHelper(modified, stagedAdd, workingFiles);
        for (String fileName: currCommitBlobs.keySet()) {
            if (!workingFiles.contains(fileName)
                    && !removed.contains(fileName)) {
                modified.put(fileName, "(deleted)");
            }
        }
        statusModify(modified);
        statusUntracked(tracked, workingFiles);
    }

    static void statusHelper(HashMap<String, String> modified,
                             HashMap<String, String> stagedAdd,
                             List<String> workingFiles) {
        for (String fileName: workingFiles) {
            File theFile = new File(cwd + "/" + fileName);
            Blob newBlob = new Blob(Utils.readContents(theFile));
            String fileHash = Utils.sha1(Utils.serialize(newBlob));
            if (getCurrentCommit().getBlobs().containsKey(fileName)
                    && !fileHash.equals(
                    getCurrentCommit().getBlobs().get(fileName))) {
                if (!stagedAdd.containsKey(fileName)) {
                    modified.put(fileName, "(modified)");
                }
            }
        }
    }

    static void statusModify(HashMap<String, String> modified) {
        System.out.println("\n=== Modifications Not Staged For Commit ===");
        if (modified.size() > 0) {
            for (String fileName: modified.keySet()) {
                System.out.println(fileName + " " + modified.get(fileName));
            }
        }
    }

    static void statusUntracked(List<String> tracked,
                                List<String> workingFiles) {
        System.out.println("\n=== Untracked Files ===");
        List<String> untracked = new ArrayList<>();
        for (String fileName: workingFiles) {
            if (!tracked.contains(fileName)) {
                untracked.add(fileName);
            }
        }
        if (untracked.size() > 0) {
            for (String fileName: untracked) {
                System.out.println(fileName);
            }
        }
    }

    @SuppressWarnings("unchecked")
    static void reset(String commitID) throws IOException {
        if (!new File(objects
                + "/" + commitID + ".txt").exists()) {
            System.out.println("No commit with that id exists.");
            return;
        }

        Commit c = Utils.readObject(new File(objects
                + "/" + commitID + ".txt"), Commit.class);
        Commit currCommit = getCurrentCommit();
        HashMap<String, String> cFiles = c.getBlobs();
        HashMap<String, String> currFiles = currCommit.getBlobs();
        HashMap<String, String> added = Utils.readObject(add, HashMap.class);
        File currBranch = new File(gitlet
                + "/" + Utils.readContentsAsString(head));
        Utils.writeContents(currBranch, commitID);
        List<String> workingFiles = Utils.plainFilenamesIn(cwd);



        for (int i = 0; i < workingFiles.size(); i++) {
            if (!currFiles.containsKey(workingFiles.get(i))
                    && !added.containsKey(workingFiles.get(i))
                    && !workingFiles.get(i).equals(".DS_Store")) {
                System.out.println("There is an untracked "
                        + "file in the way; delete it, or "
                        + "add and commit it first.");
                return;
            }
        }

        for (String fileName: workingFiles) {
            File file = new File(cwd + "/" + fileName);
            file.delete();
        }

        for (String s: cFiles.keySet()) {
            File thisFile = new File(cwd + "/" + s);
            thisFile.createNewFile();
            Utils.writeContents(thisFile,
                    Utils.readContentsAsString(new File(blobs
                    + "/" + c.getBlobs().get(s) + ".txt")));
        }

        Utils.writeObject(add, new HashMap<String, String>());

    }

}
