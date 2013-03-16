package forge.gui;

import javax.swing.JOptionPane;

import forge.Card;
import forge.Singletons;
import forge.game.event.FlipCoinEvent;
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
    
    public static boolean confirm(final Card c, String question, final boolean defaultIsYes, final String[] options) {
        CMatchUI.SINGLETON_INSTANCE.setCard(c);
        final StringBuilder title = new StringBuilder();
        if ( c != null)
            title.append(c.getName()).append(" - Ability");
    
        if (!(question.length() > 0)) {
            question = "Activate card's ability?";
        }
    
        int answer;

        String[] opts = options == null ? defaultConfirmOptions : options;
        answer = JOptionPane.showOptionDialog(null, question, title.toString(), JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE, null, opts, opts[defaultIsYes ? 0 : 1]);

        return answer == JOptionPane.YES_OPTION;
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
        JOptionPane.showMessageDialog(null, message);
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
        Singletons.getModel().getGame().getEvents().post(new FlipCoinEvent());
    
        JOptionPane.showMessageDialog(null, source.getName() + " - " + caller + winMsg, source.getName(),
                JOptionPane.PLAIN_MESSAGE);
        return winFlip;
    }

}
