package forge.toolbox;

import forge.FThreads;
import forge.game.card.Card;
import forge.util.Callback;

import org.apache.commons.lang3.StringUtils;

/** 
 * Holds player interactions using standard windows 
 *
 */
public class GuiDialog {
    private static final String[] defaultConfirmOptions = { "Yes", "No" };

    public static void confirm(final Card c, final String question, final Callback<Boolean> callback) {
        GuiDialog.confirm(c, question, true, null, callback);
    }
    public static void confirm(final Card c, final String question, final boolean defaultChoice, final Callback<Boolean> callback) {
        GuiDialog.confirm(c, question, defaultChoice, null, callback);
    }
    public static void confirm(final Card c, final String question, String[] options, final Callback<Boolean> callback) {
        GuiDialog.confirm(c, question, true, options, callback);
    }
    
    public static void confirm(final Card c, final String question, final boolean defaultIsYes, final String[] options, final Callback<Boolean> callback) {
        FThreads.invokeInEdtAndWait(new Runnable() {
            @Override
            public void run() {
                final String title = c == null ? "Question" : c.getName() + " - Ability";
                String questionToUse = StringUtils.isBlank(question) ? "Activate card's ability?" : question;
                String[] opts = options == null ? defaultConfirmOptions : options;
                FOptionPane.showOptionDialog(questionToUse, title, FOptionPane.QUESTION_ICON, opts, defaultIsYes ? 0 : 1, new Callback<Integer>() {
                    @Override
                    public void run(Integer result) {
                        callback.run(result == 0);
                    }
                });
            }
        });
    }

    /**
     * <p>
     * showInfoDialg.
     * </p>
     * 
     * @param message
     *            a {@link java.lang.String} object.
     */
    public static void message(final String message) {
        message(message, "Forge");
    }

    public static void message(final String message, final String title) {
        FThreads.invokeInEdtAndWait(new Runnable() {
            @Override
            public void run() {
                FOptionPane.showMessageDialog(message, title, null);
            }
        });
    }
}
