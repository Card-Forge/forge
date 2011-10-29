package forge.gui.input;

import forge.AllZone;
import forge.ButtonUtil;
import forge.Card;
import forge.Command;
import forge.PlayerZone;
import forge.card.mana.ManaCost;
import forge.card.spellability.SpellAbility;

//if cost is paid, Command.execute() is called

/**
 * <p>
 * Input_PayManaCost_Ability class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class Input_PayManaCost_Ability extends Input {
    /**
     * Constant <code>serialVersionUID=3836655722696348713L</code>.
     */
    private static final long serialVersionUID = 3836655722696348713L;

    private String originalManaCost;
    private String message = "";
    private ManaCost manaCost;
    private SpellAbility fakeAbility;

    private Command paidCommand;
    private Command unpaidCommand;

    // only used for X costs:
    private boolean showOnlyOKButton = false;

    /**
     * <p>
     * Constructor for Input_PayManaCost_Ability.
     * </p>
     * 
     * @param manaCost
     *            a {@link java.lang.String} object.
     * @param paid
     *            a {@link forge.Command} object.
     */
    public Input_PayManaCost_Ability(final String manaCost, final Command paid) {
        this(manaCost, paid, Command.BLANK);
    }

    /**
     * <p>
     * Constructor for Input_PayManaCost_Ability.
     * </p>
     * 
     * @param manaCost_2
     *            a {@link java.lang.String} object.
     * @param paidCommand_2
     *            a {@link forge.Command} object.
     * @param unpaidCommand_2
     *            a {@link forge.Command} object.
     */
    public Input_PayManaCost_Ability(final String manaCost_2,
            final Command paidCommand_2, final Command unpaidCommand_2) {
        this("", manaCost_2, paidCommand_2, unpaidCommand_2);
    }

    /**
     * <p>
     * Constructor for Input_PayManaCost_Ability.
     * </p>
     * 
     * @param m
     *            a {@link java.lang.String} object.
     * @param manaCost_2
     *            a {@link java.lang.String} object.
     * @param paidCommand_2
     *            a {@link forge.Command} object.
     * @param unpaidCommand_2
     *            a {@link forge.Command} object.
     */
    public Input_PayManaCost_Ability(final String m, final String manaCost_2,
            final Command paidCommand_2, final Command unpaidCommand_2) {
        this(m, manaCost_2, paidCommand_2, unpaidCommand_2, false);
    }

    /**
     * <p>
     * Constructor for Input_PayManaCost_Ability.
     * </p>
     * 
     * @param m
     *            a {@link java.lang.String} object.
     * @param manaCost_2
     *            a {@link java.lang.String} object.
     * @param paidCommand_2
     *            a {@link forge.Command} object.
     * @param unpaidCommand_2
     *            a {@link forge.Command} object.
     * @param showOKButton
     *            a boolean.
     */
    public Input_PayManaCost_Ability(final String m, final String manaCost_2,
            final Command paidCommand_2, final Command unpaidCommand_2,
            final boolean showOKButton) {
        fakeAbility = new SpellAbility(SpellAbility.Ability, null) {
            @Override
            public void resolve() {
            }

            @Override
            public boolean canPlay() {
                return false;
            }
        };
        originalManaCost = manaCost_2;
        message = m;

        manaCost = new ManaCost(originalManaCost);
        paidCommand = paidCommand_2;
        unpaidCommand = unpaidCommand_2;
        showOnlyOKButton = showOKButton;
    }

    /**
     * <p>
     * resetManaCost.
     * </p>
     */
    public final void resetManaCost() {
        manaCost = new ManaCost(originalManaCost);
    }

    /** {@inheritDoc} */
    @Override
    public final void selectCard(final Card card, final PlayerZone zone) {
        // only tap card if the mana is needed
        manaCost = Input_PayManaCostUtil.activateManaAbility(fakeAbility, card, manaCost);

        if (manaCost.isPaid()) {
            resetManaCost();
            AllZone.getHumanPlayer().getManaPool().clearPay(fakeAbility, false);
            stop();
            paidCommand.execute();
        } else {
            showMessage();
        }
    }

    /** {@inheritDoc} */
    @Override
    public final void selectButtonCancel() {
        resetManaCost();
        AllZone.getHumanPlayer().getManaPool().unpaid(fakeAbility, true);
        stop();
        unpaidCommand.execute();
    }

    /** {@inheritDoc} */
    @Override
    public final void selectButtonOK() {
        if (showOnlyOKButton) {
            stop();
            unpaidCommand.execute();
        }
    }

    /** {@inheritDoc} */
    @Override
    public final void showMessage() {
        ButtonUtil.enableOnlyCancel();
        if (showOnlyOKButton) {
            ButtonUtil.enableOnlyOK();
        }
        AllZone.getDisplay().showMessage(message + "Pay Mana Cost: \r\n" + manaCost.toString());
    }

}
