/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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
    @Override
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
    @Override
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
    @Override
    public final boolean isComputer() {
        return false;
    }

    // /////////////
    // /
    // / End transition methods
    // /
    // /////////////

    /** {@inheritDoc} */
    @Override
    public final CardList mayDrawCards(final int n) {
        if (this.canDraw() && GameActionUtil.showYesNoDialog(null, "Draw " + n + " cards?")) {
            return this.drawCards(n);
        } else {
            return new CardList();
        }
    }

    /** {@inheritDoc} */
    @Override
    public final CardList mayDrawCard() {
        return this.mayDrawCards(1);
    }

    /**
     * <p>
     * dredge.
     * </p>
     * 
     * @return a boolean.
     */
    @Override
    public final boolean dredge() {
        boolean dredged = false;
        final String[] choices = { "Yes", "No" };
        final Object o = GuiUtils.getChoice("Do you want to dredge?", choices);
        if (o.equals("Yes")) {
            final Card c = GuiUtils.getChoice("Select card to dredge", this.getDredge().toArray());
            // rule 702.49a
            if (this.getDredgeNumber(c) <= AllZone.getHumanPlayer().getZone(Zone.Library).size()) {

                // might have to make this more sophisticated
                // dredge library, put card in hand
                AllZone.getGameAction().moveToHand(c);

                for (int i = 0; i < this.getDredgeNumber(c); i++) {
                    final Card c2 = AllZone.getHumanPlayer().getZone(Zone.Library).get(0);
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
    @Override
    public final CardList discard(final int num, final SpellAbility sa, final boolean duringResolution) {
        AllZone.getInputControl().setInput(PlayerUtil.inputDiscard(num, sa), duringResolution);

        // why is CardList returned?
        return new CardList();
    }

    /** {@inheritDoc} */
    @Override
    public final void discardUnless(final int num, final String uType, final SpellAbility sa) {
        if (this.getCardsIn(Zone.Hand).size() > 0) {
            AllZone.getInputControl().setInput(PlayerUtil.inputDiscardNumUnless(num, uType, sa));
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.Player#discard_Chains_of_Mephistopheles()
     */
    /**
     * 
     */
    @Override
    protected final void discardChainsOfMephistopheles() {
        AllZone.getInputControl().setInput(PlayerUtil.inputChainsDiscard(), true);
    }

    /** {@inheritDoc} */
    @Override
    public final void handToLibrary(final int numToLibrary, String libPos) {
        if (libPos.equals("Top") || libPos.equals("Bottom")) {
            libPos = libPos.toLowerCase();
        } else {
            String s = "card";
            if (numToLibrary > 1) {
                s += "s";
            }

            final Object o = GuiUtils.getChoice("Do you want to put the " + s
                    + " on the top or bottom of your library?", new Object[] { "top", "bottom" });
            libPos = o.toString();
        }
        AllZone.getInputControl().setInput(PlayerUtil.inputPutFromHandToLibrary(libPos, numToLibrary));
    }

    /** {@inheritDoc} */
    @Override
    protected final void doScry(final CardList topN, final int n) {
        int num = n;
        for (int i = 0; i < num; i++) {
            final Object o = GuiUtils.getChoiceOptional("Put on bottom of library.", topN.toArray());
            if (o != null) {
                final Card c = (Card) o;
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
                final Card c = (Card) o;
                topN.remove(c);
                AllZone.getGameAction().moveToLibrary(c);
            }
            // no else - a card must have been chosen
        }
    }

    /** {@inheritDoc} */
    @Override
    public final void sacrificePermanent(final String prompt, final CardList choices) {
        final Input in = PlayerUtil.inputSacrificePermanent(choices, prompt);
        AllZone.getInputControl().setInput(in);
    }

    /** {@inheritDoc} */
    @Override
    protected final void clashMoveToTopOrBottom(final Card c) {
        String choice = "";
        final String[] choices = { "top", "bottom" };
        AllZone.getDisplay().setCard(c);
        choice = GuiUtils.getChoice(c.getName() + " - Top or bottom of Library", choices);

        if (choice.equals("bottom")) {
            AllZone.getGameAction().moveToBottomOfLibrary(c);
        } else {
            AllZone.getGameAction().moveToLibrary(c);
        }
    }

} // end HumanPlayer class
