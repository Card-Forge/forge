package java.nio.file;

public class InvalidPathException extends IllegalArgumentException {
    private final String input;
    private final String reason;
    private final int index;

    public InvalidPathException(String input, String reason) {
        this(input, reason, -1);
    }

    public InvalidPathException(String input, String reason, int index) {
        super(reason + ": " + input);
        this.input = input;
        this.reason = reason;
        this.index = index;
    }

    public String getInput() {
        return input;
    }

    public String getReason() {
        return reason;
    }

    public int getIndex() {
        return index;
    }
}
