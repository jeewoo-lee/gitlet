package gitlet;

import java.io.IOException;

/**
 * Driver class for Gitlet, the tiny stupid version-control system.
 * Collaborator: Nathaniel Macasaet.
 * @author Jeewoo lee
 */
public class Main {

    /**
     * Usage: java gitlet.Main ARGS, where ARGS contains
     * <COMMAND> <OPERAND> ....
     * java git.Main [command] [file]
     */
    public static void main(String... args)
            throws IOException {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            return;
        }

        if (!args[0].equals("init")) {
            if (!Command.gitlet().exists()) {
                System.out.println("Not in an initialized Gitlet directory.");
                return;
            }
        }
        int numOperand = args.length - 1;
        switch (args[0]) {
        case "checkout":
            checkoutHelper(numOperand, args);
            break;
        case "add":
            helper(numOperand, args);
            break;
        case "init":
            helper(numOperand, args);
            break;
        case "log":
            helper(numOperand, args);
            break;
        case "global-log":
            helper(numOperand, args);
            break;
        case "commit":
            helper(numOperand, args);
            break;
        case "rm":
            helper(numOperand, args);
            break;
        case "find":
            helper(numOperand, args);
            break;
        case "branch":
            helper(numOperand, args);
            break;
        case "rm-branch":
            helper(numOperand, args);
            break;
        case "status":
            helper(numOperand, args);
            break;
        case "reset":
            helper(numOperand, args);
            break;
        case "merge":
            helper(numOperand, args);
            break;
        default:
            System.out.println("No command with that name exists.");
        }
    }

    static void checkoutHelper(int numOperand, String... args)
            throws IOException {
        if (numOperand == 1) {
            Command.checkoutBranch(args[1]);
        } else if (numOperand == 2) {
            if (args[1].equals("--")) {
                Command.checkoutFile(args[2]);
            } else {
                System.out.println("Incorrect Operands.");
            }
        } else if (numOperand == 3) {
            if (args[2].equals("--")) {
                Command.checkoutCommit(args[1], args[3]);
            } else {
                System.out.println("Incorrect Operands.");
            }
        }
    }

    static void helper(int numOperand, String... args) throws IOException {
        switch (args[0]) {
        case "add":
            if (checkOperands(numOperand, 1)) {
                Command.add(args[1]);
            }
            break;
        case "init":
            if (checkOperands(numOperand, 0)) {
                Command.init();
            }
            break;
        case "log":
            if (checkOperands(numOperand, 0)) {
                Command.log();
            }
            break;
        case "global-log":
            if (checkOperands(numOperand, 0)) {
                Command.globalLog();
            }
            break;
        case "commit":
            helper1(numOperand, args);
            break;
        case "rm":
            if (checkOperands(numOperand, 1)) {
                Command.rm(args[1]);
            }
            break;
        case "find":
            if (checkOperands(numOperand, 1)) {
                Command.find(args[1]);
            }
            break;
        case "branch":
            if (checkOperands(numOperand, 1)) {
                Command.branch(args[1]);
            }
            break;
        case "rm-branch":
            helper1(numOperand, args);
            break;
        case "status":
            helper1(numOperand, args);
            break;
        case "reset":
            helper1(numOperand, args);
            break;
        case "merge":
            helper1(numOperand, args);
            break;
        default:
            System.out.println("No command with that name exists.");
        }
    }

    static void helper1(int numOperand, String... args) throws IOException {
        switch (args[0]) {
        case "commit":
            if (checkOperands(numOperand, 1)) {
                if (args[1].equals("")) {
                    System.out.println("Please enter a commit message.");
                } else {
                    Command.commit(args[1]);
                }
            }
            break;
        case "rm-branch":
            if (checkOperands(numOperand, 1)) {
                Command.rmbranch(args[1]);
            }
            break;
        case "status":
            if (checkOperands(numOperand, 0)) {
                Command.status();
            }
            break;
        case "reset":
            if (checkOperands(numOperand, 1)) {
                Command.reset(args[1]);
            }
            break;
        case "merge":
            if (checkOperands(numOperand, 1)) {
                Command.merge(args[1]);
            }
            break;
        default:
            System.out.println("No command with that name exists.");
        }

    }


    /**
     * Check number of operands.
     * @param num
     * @param expected
     * @return boolean.
     */
    public static boolean checkOperands(int num, int expected) {
        if (num == expected) {
            return true;
        }
        System.out.println("Incorrect Operands.");
        return false;

    }
}
