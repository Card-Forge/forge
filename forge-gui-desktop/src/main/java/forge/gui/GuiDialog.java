package forge.gui;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import javax.swing.UIManager;

import org.apache.commons.lang3.StringUtils;

import forge.FThreads;
import forge.GuiBase;
import forge.screens.match.CMatchUI;
import forge.toolbox.FOptionPane;
import forge.view.CardView;

/** 
 * Holds player interactions using standard windows 
 *
 */
public class GuiDialog {
    private static final String[] defaultConfirmOptions = { "Yes", "No" };

    public static boolean confirm(final CardView c, final String question) {
        return GuiDialog.confirm(c, question, true, null);
    }
    public static boolean confirm(final CardView c, final String question, final boolean defaultChoice) {
        return GuiDialog.confirm(c, question, defaultChoice, null);
    }
    public static boolean confirm(final CardView c, final String question, String[] options) {
        return GuiDialog.confirm(c, question, true, options);
    }
    
    public static boolean confirm(final CardView c, final String question, final boolean defaultIsYes, final String[] options) {
        Callable<Boolean> confirmTask = new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                if (c != null) {
                    CMatchUI.SINGLETON_INSTANCE.setCard(c);
                }

                final String title = c == null ? "Question" : c + " - Ability";
                String questionToUse = StringUtils.isBlank(question) ? "Activate card's ability?" : question;
                String[] opts = options == null ? defaultConfirmOptions : options;
                int answer = FOptionPane.showOptionDialog(questionToUse, title, FOptionPane.QUESTION_ICON, opts, defaultIsYes ? 0 : 1);
                return answer == 0;
            }};

        FutureTask<Boolean> future = new FutureTask<Boolean>(confirmTask);
        FThreads.invokeInEdtAndWait(GuiBase.getInterface(), future);
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
        FThreads.invokeInEdtAndWait(GuiBase.getInterface(), new Runnable() {
            @Override
            public void run() {
                FOptionPane.showMessageDialog(message, title, null);
            }
        });
    }
}
