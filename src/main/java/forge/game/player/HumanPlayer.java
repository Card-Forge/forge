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
package forge.game.player;

import java.util.List;

import forge.Card;

import forge.Singletons;
import forge.card.spellability.SpellAbility;
import forge.control.input.Input;
import forge.game.GameType;
import forge.game.GameState;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;
import forge.gui.GuiDialog;
import forge.gui.match.CMatchUI;
import forge.quest.QuestController;
import forge.quest.bazaar.QuestItemType;

/**
 * <p>
 * HumanPlayer class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class HumanPlayer extends Player {
    private PlayerControllerHuman controller;
    
    /**
     * <p>
     * Constructor for HumanPlayer.
     * </p>
     * 
     * @param myName
     *            a {@link java.lang.String} object.
     */
    public HumanPlayer(final LobbyPlayer player, GameState game) {
        super(player, game);
        
        controller = new PlayerControllerHuman(game, this);
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
        final boolean wantDredge = GuiDialog.confirm(null, "Do you want to dredge?");
        if (wantDredge) {
            final Card c = GuiChoose.one("Select card to dredge", this.getDredge());
            // rule 702.49a
            if (this.getDredgeNumber(c) <= getZone(ZoneType.Library).size()) {

                // might have to make this more sophisticated
                // dredge library, put card in hand
                game.getAction().moveToHand(c);

                for (int i = 0; i < this.getDredgeNumber(c); i++) {
                    final Card c2 = getZone(ZoneType.Library).get(0);
                    game.getAction().moveToGraveyard(c2);
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
    public final void discard(final int num, final SpellAbility sa) {
        Singletons.getModel().getMatch().getInput().setInput(PlayerUtil.inputDiscard(num, sa));
    }

    /** {@inheritDoc} */
    @Override
    public final void discardUnless(final int num, final String uType, final SpellAbility sa) {
        if (this.getCardsIn(ZoneType.Hand).size() > 0) {
            Singletons.getModel().getMatch().getInput().setInput(PlayerUtil.inputDiscardNumUnless(num, uType, sa));
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
        Singletons.getModel().getMatch().getInput().setInputInterrupt(PlayerUtil.inputChainsDiscard());
    }

    /** {@inheritDoc} */
    @Override
    protected final void doScry(final List<Card> topN, final int n) {
        int num = n;
        for (int i = 0; i < num; i++) {
            final Card c = GuiChoose.oneOrNone("Put on bottom of library.", topN);
            if (c != null) {
                topN.remove(c);
                game.getAction().moveToBottomOfLibrary(c);
            } else {
                // no card chosen for the bottom
                break;
            }
        }
        num = topN.size();
        for (int i = 0; i < num; i++) {
            final Card c = GuiChoose.one("Put on top of library.", topN);
            if (c != null) {
                topN.remove(c);
                game.getAction().moveToLibrary(c);
            }
            // no else - a card must have been chosen
        }
    }

    /** {@inheritDoc} */
    @Override
    public final void sacrificePermanent(final String prompt, final List<Card> choices) {
        final Input in = PlayerUtil.inputSacrificePermanent(choices, prompt);
        Singletons.getModel().getMatch().getInput().setInput(in);
    }

    /** {@inheritDoc} */
    @Override
    protected final void clashMoveToTopOrBottom(final Card c) {
        String choice = "";
        final String[] choices = { "top", "bottom" };
        CMatchUI.SINGLETON_INSTANCE.setCard(c);
        choice = GuiChoose.one(c.getName() + " - Top or bottom of Library", choices);

        if (choice.equals("bottom")) {
            game.getAction().moveToBottomOfLibrary(c);
        } else {
            game.getAction().moveToLibrary(c);
        }
    }

    /* (non-Javadoc)
     * @see forge.game.player.Player#getType()
     */
    @Override
    public PlayerType getType() {
        return PlayerType.HUMAN;
    }

    @Override
    public final int doMulligan() {
        int newHand = super.doMulligan();
        final QuestController quest = Singletons.getModel().getQuest();
        final boolean isQuest = Singletons.getModel().getMatch().getGameType().equals(GameType.Quest);
        if (isQuest && quest.getAssets().hasItem(QuestItemType.SLEIGHT) && (getStats().getMulliganCount() == 1)) {
            drawCard();
            newHand++;
            getStats().notifyOpeningHandSize(newHand);
        }
        return newHand;
    }

    /* (non-Javadoc)
     * @see forge.game.player.Player#getController()
     */
    @Override
    public PlayerController getController() {
        return controller;
    }
} // end HumanPlayer class
