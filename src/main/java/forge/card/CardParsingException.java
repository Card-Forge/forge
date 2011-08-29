package forge.card;

/** 
 * Indicates an error parsing a card txt file.
 */
public class CardParsingException extends Exception {

    private static final long serialVersionUID = -6504223115741449784L;

    /**
     * Constructor with message.
     * 
     * @param txtFile  name of txt file with the problem
     * @param lineNum  indicates the line number containing the problem
     * @param message  describes the nature of the problem in the file
     */
    public CardParsingException(final String txtFile, final int lineNum, final String message) {
        super("in '" + txtFile + "' line " + lineNum + ": " + message);
    }

    /**
     * Constructor with message and cause.
     * 
     * @param txtFile  name of txt file with the problem
     * @param lineNum  indicates the line number containing the problem
     * @param message  describes the nature of the problem in the file
     * @param cause  the original cause for the exception
     */
    public CardParsingException(final String txtFile, final int lineNum, final String message, final Throwable cause)
    {
        super("in '" + txtFile + "' line " + lineNum + ": " + message, cause);
    }
}
