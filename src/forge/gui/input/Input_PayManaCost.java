package forge.gui.input;

import forge.*;
import forge.card.mana.ManaCost;
import forge.card.spellability.SpellAbility;

//pays the cost of a card played from the player's hand
//the card is removed from the players hand if the cost is paid
//CANNOT be used for ABILITIES
/**
 * <p>Input_PayManaCost class.</p>
 *
 * @author Forge
 * @version $Id: $
 */
public class Input_PayManaCost extends Input {
    // anything that uses this should be converted to Ability_Cost
    /** Constant <code>serialVersionUID=3467312982164195091L</code> */
    private static final long serialVersionUID = 3467312982164195091L;

    private final String originalManaCost;

    private final Card originalCard;
    public ManaCost manaCost;

    private final SpellAbility spell;

    private boolean skipStack;

    private int phyLifeToLose = 0;

    /**
     * <p>Constructor for Input_PayManaCost.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @param noStack a boolean.
     */
    public Input_PayManaCost(SpellAbility sa, boolean noStack) {
        skipStack = noStack;
        originalManaCost = sa.getManaCost(); // Change
        originalCard = sa.getSourceCard();

        spell = sa;

        if (Phase.getGameBegins() == 1) {
            if (sa.getSourceCard().isCopiedSpell() && sa.isSpell()) {
                if (spell.getAfterPayMana() != null) stopSetNext(spell.getAfterPayMana());
                else {
                    manaCost = new ManaCost("0");
                    AllZone.getStack().add(spell);
                }
            } else {
                manaCost = AllZone.getGameAction().getSpellCostChange(sa, new ManaCost(originalManaCost));
            }
        } else {
            manaCost = new ManaCost(sa.getManaCost());
        }
    }

    /**
     * <p>Constructor for Input_PayManaCost.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     */
    public Input_PayManaCost(SpellAbility sa) {
        originalManaCost = sa.getManaCost(); // Change
        originalCard = sa.getSourceCard();

        spell = sa;

        if (Phase.getGameBegins() == 1) {
            if (sa.getSourceCard().isCopiedSpell() && sa.isSpell()) {
                if (spell.getAfterPayMana() != null) stopSetNext(spell.getAfterPayMana());
                else {
                    manaCost = new ManaCost("0");
                    AllZone.getStack().add(spell);
                }
            } else {
                manaCost = AllZone.getGameAction().getSpellCostChange(sa, new ManaCost(originalManaCost));
            }
        } else {
            manaCost = new ManaCost(sa.getManaCost());
        }
    }

    /**
     * <p>resetManaCost.</p>
     */
    private void resetManaCost() {
        manaCost = new ManaCost(originalManaCost);
        phyLifeToLose = 0;
    }

    /** {@inheritDoc} */
    @Override
    public void selectCard(Card card, PlayerZone zone) {
        //this is a hack, to prevent lands being able to use mana to pay their own abilities from cards like
        //Kher Keep, Pendelhaven, Blinkmoth Nexus, and Mikokoro, Center of the Sea, .... 

        if (originalCard.equals(card) && spell.isTapAbility()) {
            // I'm not sure if this actually prevents anything that wouldn't be handled by canPlay below
            return;
        }

        manaCost = Input_PayManaCostUtil.activateManaAbility(spell, card, manaCost);

        // only show message if this is the active input
        if (AllZone.getInputControl().getInput() == this)
            showMessage();

        if (manaCost.isPaid()) {
            originalCard.setSunburstValue(manaCost.getSunburst());
            done();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void selectPlayer(Player player) {

        if (player.isHuman()) {
            if (manaCost.payPhyrexian()) {
                phyLifeToLose += 2;
            }

            showMessage();
        }

    }

    /**
     * <p>done.</p>
     */
    private void done() {
        if (phyLifeToLose > 0)
            AllZone.getHumanPlayer().payLife(phyLifeToLose, originalCard);
        if (spell.getSourceCard().isCopiedSpell()) {
            if (spell.getAfterPayMana() != null) {
                stopSetNext(spell.getAfterPayMana());
            } else
                AllZone.getInputControl().resetInput();
        } else {
            AllZone.getManaPool().clearPay(spell, false);
            resetManaCost();

            // if tap ability, tap card
            if (spell.isTapAbility())
                originalCard.tap();
            if (spell.isUntapAbility())
                originalCard.untap();

            // if this is a spell, move it to the Stack ZOne

            if (spell.isSpell())    // already checked for if its a copy
                AllZone.getGameAction().moveToStack(originalCard);

            if (spell.getAfterPayMana() != null)
                stopSetNext(spell.getAfterPayMana());
            else {
                if (skipStack) {
                	spell.resolve();
                } else {
                    AllZone.getStack().add(spell);
                }
                AllZone.getInputControl().resetInput();
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void selectButtonCancel() {
        resetManaCost();
        AllZone.getManaPool().unpaid(spell, true);
        AllZone.getHumanBattlefield().updateObservers();//DO NOT REMOVE THIS, otherwise the cards don't always tap

        stop();
    }

    /** {@inheritDoc} */
    @Override
    public void showMessage() {
        ButtonUtil.enableOnlyCancel();

        StringBuilder msg = new StringBuilder("Pay Mana Cost: " + manaCost.toString());
        if (phyLifeToLose > 0) {
            msg.append(" (");
            msg.append(phyLifeToLose);
            msg.append(" life paid for phyrexian mana)");
        }

        if (manaCost.containsPhyrexianMana()) {
            msg.append("\n(Click on your life total to pay life for phyrexian mana.)");
        }

        AllZone.getDisplay().showMessage(msg.toString());
        if (manaCost.isPaid() && !new ManaCost(originalManaCost).isPaid()) {
            originalCard.setSunburstValue(manaCost.getSunburst());
            done();
        }


    }
}
