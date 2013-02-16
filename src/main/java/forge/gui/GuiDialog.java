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

    /**
     * <p>
     * showYesNoDialog.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @param question
     *            a {@link java.lang.String} object.
     * @return a boolean.
     */
    public static boolean confirm(final Card c, final String question) {
        return GuiDialog.confirm(c, question, true);
    }

    /**
     * <p>
     * showYesNoDialog.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @param question
     *            a {@link java.lang.String} object.
     * @param defaultNo
     *            true if the default option should be "No", false otherwise
     * @return a boolean.
     */
    public static boolean confirm(final Card c, String question, final boolean defaultChoice) {
        CMatchUI.SINGLETON_INSTANCE.setCard(c);
        final StringBuilder title = new StringBuilder();
        if ( c != null)
            title.append(c.getName()).append(" - Ability");
    
        if (!(question.length() > 0)) {
            question = "Activate card's ability?";
        }
    
        int answer;
        if (!defaultChoice) {
            final Object[] options = { "Yes", "No" };
            answer = JOptionPane.showOptionDialog(null, question, title.toString(), JOptionPane.YES_NO_OPTION,
                    JOptionPane.PLAIN_MESSAGE, null, options, options[1]);
        } else {
            answer = JOptionPane.showConfirmDialog(null, question, title.toString(), JOptionPane.YES_NO_OPTION);
        }
    
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
