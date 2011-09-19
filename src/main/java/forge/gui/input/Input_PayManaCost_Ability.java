package forge.gui.input;

import forge.*;
import forge.card.mana.ManaCost;
import forge.card.spellability.SpellAbility;

//if cost is paid, Command.execute() is called

/**
 * <p>Input_PayManaCost_Ability class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class Input_PayManaCost_Ability extends Input {
    /**
     * Constant <code>serialVersionUID=3836655722696348713L</code>
     */
    private static final long serialVersionUID = 3836655722696348713L;

    private String originalManaCost;
    private String message = "";
    private ManaCost manaCost;
    private SpellAbility fakeAbility;

    private Command paidCommand;
    private Command unpaidCommand;

    //only used for X costs:
    private boolean showOnlyOKButton = false;

    /**
     * <p>Constructor for Input_PayManaCost_Ability.</p>
     *
     * @param manaCost a {@link java.lang.String} object.
     * @param paid     a {@link forge.Command} object.
     */
    public Input_PayManaCost_Ability(String manaCost, Command paid) {
        this(manaCost, paid, Command.Blank);
    }

    /**
     * <p>Constructor for Input_PayManaCost_Ability.</p>
     *
     * @param manaCost_2      a {@link java.lang.String} object.
     * @param paidCommand_2   a {@link forge.Command} object.
     * @param unpaidCommand_2 a {@link forge.Command} object.
     */
    public Input_PayManaCost_Ability(String manaCost_2, Command paidCommand_2, Command unpaidCommand_2) {
        this("", manaCost_2, paidCommand_2, unpaidCommand_2);
    }

    /**
     * <p>Constructor for Input_PayManaCost_Ability.</p>
     *
     * @param m               a {@link java.lang.String} object.
     * @param manaCost_2      a {@link java.lang.String} object.
     * @param paidCommand_2   a {@link forge.Command} object.
     * @param unpaidCommand_2 a {@link forge.Command} object.
     */
    public Input_PayManaCost_Ability(String m, String manaCost_2, Command paidCommand_2, Command unpaidCommand_2) {
        this(m, manaCost_2, paidCommand_2, unpaidCommand_2, false);
    }

    /**
     * <p>Constructor for Input_PayManaCost_Ability.</p>
     *
     * @param m               a {@link java.lang.String} object.
     * @param manaCost_2      a {@link java.lang.String} object.
     * @param paidCommand_2   a {@link forge.Command} object.
     * @param unpaidCommand_2 a {@link forge.Command} object.
     * @param showOKButton    a boolean.
     */
    public Input_PayManaCost_Ability(String m, String manaCost_2, Command paidCommand_2, Command unpaidCommand_2, boolean showOKButton) {
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
     * <p>resetManaCost.</p>
     */
    public void resetManaCost() {
        manaCost = new ManaCost(originalManaCost);
    }

    /** {@inheritDoc} */
    @Override
    public void selectCard(Card card, PlayerZone zone) {
        //only tap card if the mana is needed
        manaCost = Input_PayManaCostUtil.activateManaAbility(fakeAbility, card, manaCost);
        showMessage();

        if (manaCost.isPaid()) {
            resetManaCost();
            AllZone.getHumanPlayer().getManaPool().clearPay(fakeAbility, false);

            paidCommand.execute();

            AllZone.getInputControl().resetInput();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void selectButtonCancel() {
        resetManaCost();
        AllZone.getHumanPlayer().getManaPool().unpaid(fakeAbility, true);
        unpaidCommand.execute();
        AllZone.getInputControl().resetInput();
    }

    /** {@inheritDoc} */
    @Override
    public void selectButtonOK() {
        if (showOnlyOKButton) {
            unpaidCommand.execute();
            AllZone.getInputControl().resetInput();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void showMessage() {
        ButtonUtil.enableOnlyCancel();
        if (showOnlyOKButton)
            ButtonUtil.enableOnlyOK();
        AllZone.getDisplay().showMessage(message + "Pay Mana Cost: \r\n" + manaCost.toString());
    }


}
