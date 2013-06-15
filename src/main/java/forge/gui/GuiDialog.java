package forge.gui;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import javax.swing.JOptionPane;
import javax.swing.UIManager;

import org.apache.commons.lang3.StringUtils;

import forge.Card;
import forge.FThreads;
import forge.gui.match.CMatchUI;

/** 
 * Holds player interactions using standard windows 
 *
 */
public class GuiDialog {

    private static final String[] defaultConfirmOptions = { "Yes", "No" };
    public static boolean confirm(final Card c, final String question) {
        return GuiDialog.confirm(c, question, true, null);
    }
    public static boolean confirm(final Card c, final String question, final boolean defaultChoice) {
        return GuiDialog.confirm(c, question, defaultChoice, null);
    }
    public static boolean confirm(final Card c, final String question, String[] options) {
        return GuiDialog.confirm(c, question, true, options);
    }
    
    public static boolean confirm(final Card c, final String question, final boolean defaultIsYes, final String[] options) {
        Callable<Boolean> confirmTask = new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                if ( null != c )
                    CMatchUI.SINGLETON_INSTANCE.setCard(c);

                final String title = c == null ? "Question" : c.getName() + " - Ability";
                String questionToUse = StringUtils.isBlank(question) ? "Activate card's ability?" : question;
                String[] opts = options == null ? defaultConfirmOptions : options;
                int answer = JOptionPane.showOptionDialog(null, questionToUse, title, 
                        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, 
                        opts, opts[defaultIsYes ? 0 : 1]);
                return answer == JOptionPane.YES_OPTION;
            }};

        FutureTask<Boolean> future = new FutureTask<Boolean>(confirmTask);
        FThreads.invokeInEdtAndWait(future);
        try { 
            return future.get().booleanValue();
        } catch (Exception e) { // should be no exception here
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
                JOptionPane.showMessageDialog(null, message, title, JOptionPane.PLAIN_MESSAGE);
            }
        });
    }

}
