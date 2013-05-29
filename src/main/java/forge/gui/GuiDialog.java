package forge.gui;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import javax.swing.JOptionPane;
import javax.swing.UIManager;

import org.apache.commons.lang3.StringUtils;

import forge.Card;
import forge.FThreads;
import forge.game.event.GameEventFlipCoin;
import forge.game.player.Player;
import forge.gui.match.CMatchUI;
import forge.util.MyRandom;

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

    public static void message(final String message, String title) {
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.PLAIN_MESSAGE);
    }

    /**
     * <p>
     * flipACoin.
     * </p>
     * 
     * @param caller
     *            a {@link forge.game.player.Player} object.
     * @param source
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public static boolean flipCoin(final Player caller, final Card source) {
        String choice;
        final String[] choices = { "heads", "tails" };
    
        final boolean flip = MyRandom.getRandom().nextBoolean();
        if (caller.isHuman()) {
            choice = GuiChoose.one(source.getName() + " - Call coin flip", choices);
        } else {
            choice = choices[MyRandom.getRandom().nextInt(2)];
        }
    
        final boolean winFlip = flip == choice.equals(choices[0]);
        final String winMsg = winFlip ? " wins flip." : " loses flip.";
    
        // Play the Flip A Coin sound
        caller.getGame().fireEvent(new GameEventFlipCoin());
    
        JOptionPane.showMessageDialog(null, source.getName() + " - " + caller + winMsg, source.getName(),
                JOptionPane.PLAIN_MESSAGE);
        return winFlip;
    }

}
