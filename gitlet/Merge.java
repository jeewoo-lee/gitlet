package gitlet;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.LinkedList;
import java.util.ArrayList;

/**
 * This class consists helper methods for Merge command.
 * @author Jeewoo Lee
 */
public class Merge {
    /**
     * old implementation.
     */
    private static HashMap<String, Integer> distance = new HashMap<>();
    /**
     * to track if merge was conflicted.
     */
    private static boolean isConflicted = false;
    /**
     * step variable for an old implementation of split.
     */
    private static int normalStep;
    /**
     * step variable for an old implementation of split.
     */
    private static int mergeStep;

    static String findSplit(String headCommitID, String otherCommitID) {
        normalStep = 0;
        mergeStep = 0;
        List<String> headParents = getParents(headCommitID);
        List<String> otherParents = getParents(otherCommitID);
        List<String> headMergeParents = getMergeParents(headCommitID);
        List<String> otherMergeParents = getMergeParents(otherCommitID);

        String normalSplit = splitHelper(headParents, otherParents);
        String mergeSplit = splitMergeHelper(headMergeParents,
                otherMergeParents);

        if (normalStep > mergeStep) {
            return mergeSplit;
        } else {
            return normalSplit;
        }

    }

    static String advancedSplit(String stringBranch) {
        String splitHash = "";
        String commitHash = "";
        Set<String> branches = new TreeSet<String>();
        LinkedList<String> parent = new LinkedList<String>();

        File otherBranchFile = new File(Command.ref()
                + "/" + stringBranch + ".txt");
        String otherCommitHash = Utils.readContentsAsString(otherBranchFile);
        parent.add(otherCommitHash);


        while (!parent.isEmpty()) {
            commitHash = parent.poll();
            branches.add(commitHash);
            File commitFile = new File(Command.objects()
                    + "/" + commitHash + ".txt");
            Commit theCommit = Utils.readObject(commitFile, Commit.class);

            if (!theCommit.getParent().equals("")) {
                parent.add(theCommit.getParent());
                if (theCommit.isMerged()) {
                    parent.add(theCommit.getMergedParent());
                }
            }

        }

        String currentCommitHash = Command.currentCommitHash();
        parent.add(currentCommitHash);

        while (!parent.isEmpty()) {
            commitHash = parent.poll();
            if (branches.contains(commitHash)) {
                splitHash = commitHash;
                break;
            }
            File commitFile = new File(Command.objects()
                    + "/" + commitHash + ".txt");
            Commit theCommit = Utils.readObject(commitFile, Commit.class);
            if (!theCommit.getParent().equals("")) {
                parent.add(theCommit.getParent());
                if (theCommit.isMerged()) {
                    parent.add(theCommit.getMergedParent());
                }
            }
        }

        return splitHash;
    }

    static String splitHelper(List<String> headParents,
                              List<String> otherParents) {
        String latestAncestor = "";
        for (String head: headParents) {
            normalStep = 0;
            for (String other: otherParents) {
                normalStep += 1;
                if (head.equals(other)) {
                    latestAncestor = head;
                    return latestAncestor;
                }
            }

        }
        return latestAncestor;
    }

    static String splitMergeHelper(List<String> headParents,
                                   List<String> otherParents) {
        String latestAncestor = "";
        for (String head: headParents) {
            mergeStep = 0;
            for (String other: otherParents) {
                mergeStep += 1;
                if (head.equals(other)) {
                    latestAncestor = head;
                    return latestAncestor;
                }
            }

        }
        return latestAncestor;
    }

    static void mergeHelp1(HashMap<String, String> result,
                                              HashMap<String, String>
                                                      headFiles,
                                              HashMap<String, String>
                                   splitFiles,
                                              HashMap<String, String>
                                   otherFiles)
                    throws IOException {
        for (String headFileName: headFiles.keySet()) {
            if (splitFiles.containsKey(headFileName)
                    && otherFiles.containsKey(headFileName)) {
                String splitHash = splitFiles.get(headFileName);
                String otherHash = otherFiles.get(headFileName);
                String headHash = headFiles.get(headFileName);
                if (checkModified(splitHash, otherHash)
                        && !checkModified(splitHash, headHash)) {
                    result.put(headFileName, otherHash);
                } else if (!checkModified(splitHash, otherHash)
                        && checkModified(splitHash, headHash)) {
                    result.put(headFileName, headHash);
                } else if (checkModified(splitHash, otherHash)
                        && checkModified(splitHash, headHash)) {
                    result.put(headFileName,
                            mergeConflict(headFileName, otherHash, headHash));
                }
            }
            if (!splitFiles.containsKey(headFileName)
                    && otherFiles.containsKey(headFileName)) {
                String headHash = headFiles.get(headFileName);
                String otherHash = otherFiles.get(headFileName);
                if (!headHash.equals(otherHash)) {
                    result.put(headFileName,
                            mergeConflict(headFileName, otherHash, headHash));
                } else {
                    result.put(headFileName, headHash);
                }
            }
            if (!splitFiles.containsKey(headFileName)
                    && !otherFiles.containsKey(headFileName)) {
                String headHash = headFiles.get(headFileName);
                result.put(headFileName, headHash);
            }
            if (splitFiles.containsKey(headFileName)
                    && !otherFiles.containsKey(headFileName)) {
                String splitHash = splitFiles.get(headFileName);
                String headHash = headFiles.get(headFileName);
                if (!checkModified(splitHash, headHash)) {
                    File removedFile = new File(Command.cwd()
                            + "/" + headFileName);
                    Utils.restrictedDelete(removedFile);
                } else {
                    result.put(headFileName,
                            mergeConflict(headFileName, "", headHash));
                }
            }
        }
    }

    static HashMap<String, String> merge(HashMap<String, String> headFiles,
                                         HashMap<String, String> splitFiles,
                                         HashMap<String, String> otherFiles)
                    throws IOException {
        HashMap<String, String> result = new HashMap<>();
        mergeHelp1(result, headFiles, splitFiles, otherFiles);

        for (String otherFileName: otherFiles.keySet()) {
            if (!(splitFiles.containsKey(otherFileName))
                    && !(headFiles.containsKey(otherFileName))) {
                String otherFile = otherFiles.get(otherFileName);
                result.put(otherFileName, otherFile);
            }
            if (splitFiles.containsKey(otherFileName)
                    && !headFiles.containsKey(otherFileName)) {
                String splitHash = splitFiles.get(otherFileName);
                String otherHash = otherFiles.get(otherFileName);
                if (!checkModified(splitHash, otherHash)) {
                    int n = 0;
                } else {
                    result.put(otherFileName,
                            mergeConflict(otherFileName, otherHash, ""));
                }
            }
        }
        return result;
    }



    static String mergeConflict(String fileName,
                                String otherHash,
                                String headHash) throws IOException {
        if (otherHash.equals(headHash)) {
            return fileName;
        }
        File newFile = new File(Command.cwd() + "/" + fileName);
        String fileHash = "";

        if (!newFile.exists()) {
            newFile.createNewFile();
        }

        if (headHash.equals("")) {
            File otherBlob = new File(Command.blobs()
                    + "/" + otherHash + ".txt");
            String otherBlobContent = Utils.readContentsAsString(otherBlob);
            mergeContents(newFile, "", otherBlobContent);

        } else if (otherHash.equals("")) {
            File headBlob = new File(Command.blobs() + "/" + headHash + ".txt");
            String headBlobContent = Utils.readContentsAsString(headBlob);
            mergeContents(newFile, headBlobContent, "");

        } else {
            File headBlob = new File(Command.blobs() + "/" + headHash + ".txt");
            String headBlobContent = Utils.readContentsAsString(headBlob);
            File otherBlob = new File(Command.blobs()
                    + "/" + otherHash + ".txt");
            String otherBlobContent = Utils.readContentsAsString(otherBlob);
            mergeContents(newFile, headBlobContent, otherBlobContent);

        }

        Blob newBlob = new Blob(Utils.readContents(newFile));
        fileHash = Utils.sha1(Utils.serialize(newBlob));
        File newBlobFile = new File(Command.blobs() + "/" + fileHash + ".txt");
        newBlobFile.createNewFile();
        Utils.writeContents(newBlobFile, Utils.readContentsAsString(newFile));
        isConflicted = true;
        File conflicted = new File(Command.cwd() + "/" + "conflict" + ".txt");
        if (!conflicted.exists()) {
            conflicted.createNewFile();
        }
        Utils.writeContents(conflicted, "Encountered a merge conflict.");
        return fileHash;
    }

    static void mergeContents(File file,
                              String headContents, String otherContents) {
        Utils.writeContents(file, "<<<<<<< HEAD\n"
                + headContents + "=======\n"
                + otherContents + ">>>>>>>\n");

    }

    static boolean checkModified(String splitFileHash, String newFileHash) {
        if (splitFileHash.equals(newFileHash)) {
            return false;
        }
        return true;

    }

    static List<String> getParents(String commitID) {
        List<String> parents = new ArrayList<>();
        Commit commit = Command.getCommit(commitID);
        int n = 1;
        parents.add(commitID);
        while (commit != null) {
            if (commit.getParent().equals("")) {
                break;
            } else {
                parents.add(commit.getParent());
                distance.put(commit.getParent(), n);
                commit = Utils.readObject(new File(Command.objects()
                        + "/" + commit.getParent() + ".txt"), Commit.class);
            }
            n++;
        }
        return parents;
    }

    static List<String> getMergeParents(String commitID) {
        List<String> parents = new ArrayList<>();
        Commit commit = Command.getCommit(commitID);
        int n = 1;
        parents.add(commitID);
        while (commit != null) {
            if (commit.getParent().equals("")) {
                break;
            } else {
                if (commit.isMerged()) {
                    parents.add(commit.getMergedParent());
                    distance.put(commit.getParent(), n);
                    commit = Utils.readObject(new File(Command.objects()
                            + "/" + commit.getMergedParent()
                            + ".txt"), Commit.class);
                } else {
                    parents.add(commit.getParent());
                    distance.put(commit.getParent(), n);
                    commit = Utils.readObject(new File(Command.objects()
                            + "/" + commit.getParent() + ".txt"), Commit.class);
                }
            }
            n++;
        }
        return parents;
    }

}
