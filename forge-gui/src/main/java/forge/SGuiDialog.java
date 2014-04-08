package forge;

import forge.FThreads;
import forge.game.card.Card;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/** 
 * Holds player interactions using standard windows 
 *
 */
public class SGuiDialog {
    private static final String[] defaultConfirmOptions = { "Yes", "No" };

    public static boolean confirm(final Card c, final String question) {
        return SGuiDialog.confirm(c, question, true, null);
    }
    public static boolean confirm(final Card c, final String question, final boolean defaultChoice) {
        return SGuiDialog.confirm(c, question, defaultChoice, null);
    }
    public static boolean confirm(final Card c, final String question, String[] options) {
        return SGuiDialog.confirm(c, question, true, options);
    }
    
    public static boolean confirm(final Card c, final String question, final boolean defaultIsYes, final String[] options) {
        Callable<Boolean> confirmTask = new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                if (c != null) {
                    GuiBase.getInterface().setCard(c);
                }

                final String title = c == null ? "Question" : c.getName() + " - Ability";
                String questionToUse = StringUtils.isBlank(question) ? "Activate card's ability?" : question;
                String[] opts = options == null ? defaultConfirmOptions : options;
                int answer = SOptionPane.showOptionDialog(questionToUse, title, SOptionPane.QUESTION_ICON, opts, defaultIsYes ? 0 : 1);
                return answer == 0;
            }};

        FutureTask<Boolean> future = new FutureTask<Boolean>(confirmTask);
        FThreads.invokeInEdtAndWait(future);
        try { 
            return future.get().booleanValue();
        }
        catch (Exception e) { // should be no exception here
            e.printStackTrace();
        }
        return false;
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
        message(message, UIManager.getString("OptionPane.messageDialogTitle"));
    }

    public static void message(final String message, final String title) {
        FThreads.invokeInEdtAndWait(new Runnable() {
            @Override
            public void run() {
                SOptionPane.showMessageDialog(message, title, null);
            }
        });
    }
}
