package forge.util;

//simple class to simulate a StringBuilder but simply output to the console
public class DebugTrace {
    public DebugTrace append(Object output) {
        //System.out.print(output); //Uncomment to show trace output
        return this;
    }
}
