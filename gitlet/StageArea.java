package gitlet;

import java.io.Serializable;
import java.util.HashMap;
/**
 * StageArea class for Gitlet, the tiny stupid version-control system.
 * @author Jeewoo lee
 */
public class StageArea implements Serializable {
    /**
     * StageAdd.
     */
    private static HashMap<String, String> stagedAdd;
    /**
     * StageRemove.
     */
    private static HashMap<String, String> stagedRemove;

    public StageArea(HashMap<String, String> add,
                     HashMap<String, String> remove) {
        stagedAdd = add;
        stagedRemove = remove;

    }

    HashMap<String, String> getStagedAdd() {
        return stagedAdd;
    }

    HashMap<String, String> getStagedRemove() {
        return stagedRemove;
    }

}
