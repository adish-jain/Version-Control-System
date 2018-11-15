package gitlet;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Adish Jain
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) {
        Commands commandInterpreter = new Commands(args);
        commandInterpreter.doCommand();
    }

}
