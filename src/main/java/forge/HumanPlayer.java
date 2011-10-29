package forge;

import forge.Constant.Zone;
import forge.card.spellability.SpellAbility;
import forge.gui.GuiUtils;
import forge.gui.input.Input;

/**
 * <p>
 * HumanPlayer class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class HumanPlayer extends Player {

    /**
     * <p>
     * Constructor for HumanPlayer.
     * </p>
     * 
     * @param myName
     *            a {@link java.lang.String} object.
     */
    public HumanPlayer(final String myName) {
        this(myName, 20, 0);
    }

    /**
     * <p>
     * Constructor for HumanPlayer.
     * </p>
     * 
     * @param myName
     *            a {@link java.lang.String} object.
     * @param myLife
     *            a int.
     * @param myPoisonCounters
     *            a int.
     */
    public HumanPlayer(final String myName, final int myLife, final int myPoisonCounters) {
        super(myName, myLife, myPoisonCounters);
    }

    /**
     * <p>
     * getOpponent.
     * </p>
     * 
     * @return a {@link forge.Player} object.
     */
    public final Player getOpponent() {
        return AllZone.getComputerPlayer();
    }

    // //////////////
    // /
    // / Methods to ease transition to Abstract Player class
    // /
    // /////////////

    /**
     * <p>
     * isHuman.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isHuman() {
        return true;
    }

    /**
     * <p>
     * isComputer.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isComputer() {
        return false;
    }

    // /////////////
    // /
    // / End transition methods
    // /
    // /////////////

    /** {@inheritDoc} */
    public final CardList mayDrawCards(final int n) {
        if (canDraw() && GameActionUtil.showYesNoDialog(null, "Draw " + n + " cards?")) {
            return drawCards(n);
        } else {
            return new CardList();
        }
    }

    /** {@inheritDoc} */
    public final CardList mayDrawCard() {
        return mayDrawCards(1);
    }

    /**
     * <p>
     * dredge.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean dredge() {
        boolean dredged = false;
        String[] choices = { "Yes", "No" };
        Object o = GuiUtils.getChoice("Do you want to dredge?", choices);
        if (o.equals("Yes")) {
            Card c = (Card) GuiUtils.getChoice("Select card to dredge", getDredge().toArray());
            // rule 702.49a
            if (getDredgeNumber(c) <= AllZone.getHumanPlayer().getZone(Zone.Library).size()) {

                // might have to make this more sophisticated
                // dredge library, put card in hand
                AllZone.getGameAction().moveToHand(c);

                for (int i = 0; i < getDredgeNumber(c); i++) {
                    Card c2 = AllZone.getHumanPlayer().getZone(Zone.Library).get(0);
                    AllZone.getGameAction().moveToGraveyard(c2);
                }
                dredged = true;
            } else {
                dredged = false;
            }
        }
        return dredged;
    }

    /** {@inheritDoc} */
    public final CardList discard(final int num, final SpellAbility sa, final boolean duringResolution) {
        AllZone.getInputControl().setInput(PlayerUtil.input_discard(num, sa), duringResolution);

        // why is CardList returned?
        return new CardList();
    }

    /** {@inheritDoc} */
    public final void discardUnless(final int num, final String uType, final SpellAbility sa) {
        AllZone.getInputControl().setInput(PlayerUtil.input_discardNumUnless(num, uType, sa));
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.Player#discard_Chains_of_Mephistopheles()
     */
    /**
     * 
     */
    protected final void discardChainsOfMephistopheles() {
        AllZone.getInputControl().setInput(PlayerUtil.input_chainsDiscard(), true);
    }

    /** {@inheritDoc} */
    public final void handToLibrary(final int numToLibrary, String libPos) {
        if (libPos.equals("Top") || libPos.equals("Bottom")) {
            libPos = libPos.toLowerCase();
        } else {
            String s = "card";
            if (numToLibrary > 1) {
                s += "s";
            }

            Object o = GuiUtils.getChoice("Do you want to put the " + s + " on the top or bottom of your library?",
                    new Object[] { "top", "bottom" });
            libPos = o.toString();
        }
        AllZone.getInputControl().setInput(PlayerUtil.input_putFromHandToLibrary(libPos, numToLibrary));
    }

    /** {@inheritDoc} */
    protected final void doScry(final CardList topN, final int n) {
        int num = n;
        for (int i = 0; i < num; i++) {
            Object o = GuiUtils.getChoiceOptional("Put on bottom of library.", topN.toArray());
            if (o != null) {
                Card c = (Card) o;
                topN.remove(c);
                AllZone.getGameAction().moveToBottomOfLibrary(c);
            } else {
                // no card chosen for the bottom
                break;
            }
        }
        num = topN.size();
        for (int i = 0; i < num; i++) {
            Object o;
            o = GuiUtils.getChoice("Put on top of library.", topN.toArray());
            if (o != null) {
                Card c = (Card) o;
                topN.remove(c);
                AllZone.getGameAction().moveToLibrary(c);
            }
            // no else - a card must have been chosen
        }
    }

    /** {@inheritDoc} */
    public final void sacrificePermanent(final String prompt, final CardList choices) {
        Input in = PlayerUtil.input_sacrificePermanent(choices, prompt);
        AllZone.getInputControl().setInput(in);
    }

    /** {@inheritDoc} */
    protected final void clashMoveToTopOrBottom(final Card c) {
        String choice = "";
        String[] choices = { "top", "bottom" };
        AllZone.getDisplay().setCard(c);
        choice = (String) GuiUtils.getChoice(c.getName() + " - Top or bottom of Library", choices);

        if (choice.equals("bottom")) {
            AllZone.getGameAction().moveToBottomOfLibrary(c);
        } else {
            AllZone.getGameAction().moveToLibrary(c);
        }
    }

} // end HumanPlayer class
