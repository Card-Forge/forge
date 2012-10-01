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
package forge.control.input;

import java.util.ArrayList;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.CardList;
import forge.CardListUtil;
import forge.GameAction;
import forge.GameActionUtil;
import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.spellability.SpellAbility;
import forge.game.GamePlayerRating;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseUtil;
import forge.game.player.ComputerUtil;
import forge.game.player.Player;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.gui.framework.SDisplayUtil;
import forge.gui.match.CMatchUI;
import forge.gui.match.VMatchUI;
import forge.gui.match.views.VMessage;
import forge.quest.QuestController;
import forge.quest.bazaar.QuestItemType;
import forge.view.ButtonUtil;
 /**
  * <p>
  * InputMulligan class.
  * </p>
  * 
  * @author Forge
  * @version $Id$
  */
public class InputMulligan extends Input {
    /** Constant <code>serialVersionUID=-8112954303001155622L</code>. */
    private static final long serialVersionUID = -8112954303001155622L;

    private static final int MAGIC_NUMBER_OF_SHUFFLES = 100;
    private static final int AI_MULLIGAN_THRESHOLD = 5;

    /** {@inheritDoc} */
    @Override
    public final void showMessage() {
        ButtonUtil.enableAll();
        VMatchUI.SINGLETON_INSTANCE.getBtnOK().setText("No");
        VMatchUI.SINGLETON_INSTANCE.getBtnCancel().setText("Yes");

        final String str =
                (Singletons.getModel().getGameState().getPhaseHandler().getPlayerTurn().equals(AllZone.getHumanPlayer())
                        ? "You're going first. " : "The computer is going first. ");
        CMatchUI.SINGLETON_INSTANCE.showMessage(str + "Do you want to Mulligan?");
    }

    /** {@inheritDoc} */
    @Override
    public final void selectButtonOK() {
        this.end();
    }

    /**
     * 
     * TODO Write javadoc for this method.
     * 
     * @param player
     *            a Player object
     * @param playerRating
     *            a GamePlayerRating object
     * @return an int
     */
    public final int doMulligan(final Player player, final GamePlayerRating playerRating) {
        final CardList hand = player.getCardsIn(ZoneType.Hand);
        for (final Card c : hand) {
            Singletons.getModel().getGameAction().moveToLibrary(c);
        }
        for (int i = 0; i < InputMulligan.MAGIC_NUMBER_OF_SHUFFLES; i++) {
            player.shuffle();
        }
        final int newHand = hand.size() - 1;
        for (int i = 0; i < newHand; i++) {
            player.drawCard();
        }
        AllZone.getGameLog().add("Mulligan", player + " has mulliganed down to " + newHand + " cards.", 0);
        playerRating.notifyHasMulliganed();
        playerRating.notifyOpeningHandSize(newHand);
        return newHand;
    }

    /** {@inheritDoc} */
    @Override
    public final void selectButtonCancel() {
        final Player humanPlayer = AllZone.getHumanPlayer();
        final GamePlayerRating humanRating = Singletons.getModel().getGameSummary().getPlayerRating(humanPlayer.getName());

        final int newHand = this.doMulligan(humanPlayer, humanRating);

        final QuestController quest = AllZone.getQuest();
        if (quest.isLoaded() && quest.getAssets().hasItem(QuestItemType.SLEIGHT) && (humanRating.getMulliganCount() == 1)) {
            AllZone.getHumanPlayer().drawCard();
            humanRating.notifyOpeningHandSize(newHand + 1);
        }

        if (newHand == 0) {
            this.end();
        }
    } // selectButtonOK()

    /**
     * <p>
     * end.
     * </p>
     */
    final void end() {
        // Computer mulligan
        final GameAction ga = Singletons.getModel().getGameAction();
        final Player aiPlayer = AllZone.getComputerPlayer();
        final GamePlayerRating aiRating = Singletons.getModel().getGameSummary().getPlayerRating(aiPlayer.getName());
        boolean aiTakesMulligan = true;

        // Computer mulligans if there are no cards with converted mana cost of
        // 0 in its hand
        while (aiTakesMulligan) {

            final CardList handList = aiPlayer.getCardsIn(ZoneType.Hand);
            final boolean hasLittleCmc0Cards = CardListUtil.getValidCards(handList, "Card.cmcEQ0", aiPlayer, null).size() < 2;
            aiTakesMulligan = (handList.size() > InputMulligan.AI_MULLIGAN_THRESHOLD) && hasLittleCmc0Cards;

            if (aiTakesMulligan) {
                this.doMulligan(aiPlayer, aiRating);
            }
        }

        // Human Leylines & Chancellors
        ButtonUtil.reset();
        final AbilityFactory af = new AbilityFactory();
        final CardList humanOpeningHand = AllZone.getHumanPlayer().getCardsIn(ZoneType.Hand);

        for (final Card c : humanOpeningHand) {
            final ArrayList<String> kws = c.getKeyword();
            for (int i = 0; i < kws.size(); i++) {
                final String kw = kws.get(i);

                if (kw.startsWith("MayEffectFromOpeningHand")) {
                    final String effName = kw.split(":")[1];

                    final SpellAbility effect = af.getAbility(c.getSVar(effName), c);
                    if (GameActionUtil.showYesNoDialog(c, "Use this card's ability?")) {
                        // If we ever let the AI memorize cards in the players
                        // hand, this would be a place to do so.
                        ga.playSpellAbilityNoStack(effect, false);
                    }
                }
            }
            if (c.getName().startsWith("Leyline")) {
                if (GameActionUtil.showYesNoDialog(c, "Use this card's ability?")) {
                    ga.moveToPlay(c);
                }
            }
        }

        // Computer Leylines & Chancellors
        final CardList aiOpeningHand = AllZone.getComputerPlayer().getCardsIn(ZoneType.Hand);
        for (final Card c : aiOpeningHand) {
            if (!c.getName().startsWith("Leyline")) {
                final ArrayList<String> kws = c.getKeyword();
                for (int i = 0; i < kws.size(); i++) {
                    final String kw = kws.get(i);

                    if (kw.startsWith("MayEffectFromOpeningHand")) {
                        final String effName = kw.split(":")[1];

                        final SpellAbility effect = af.getAbility(c.getSVar(effName), c);

                        // Is there a better way for the AI to decide this?
                        if (effect.doTrigger(false)) {
                            GameActionUtil.showInfoDialg("Computer reveals " + c.getName() + "(" + c.getUniqueNumber()
                                    + ").");
                            ComputerUtil.playNoStack(effect);
                        }
                    }
                }
            }
            if (c.getName().startsWith("Leyline")
                    && !(c.getName().startsWith("Leyline of Singularity") && (AllZoneUtil.getCardsIn(ZoneType.Battlefield,
                            "Leyline of Singularity").size() > 0))) {
                ga.moveToPlay(c);
                //ga.checkStateEffects();
            }
        }
        //ga.checkStateEffects();

        if (ga.isStartCut() && !(humanOpeningHand.contains(ga.getHumanCut())
                        || aiOpeningHand.contains(ga.getComputerCut()))) {
            ga.moveTo(AllZone.getHumanPlayer().getZone(ZoneType.Library), ga.getHumanCut());
            ga.moveTo(AllZone.getComputerPlayer().getZone(ZoneType.Library), ga.getComputerCut());
        }

        ga.checkStateEffects();
        PhaseHandler.setGameBegins(1);
        AllZone.getGameLog().add("Turn",
                "Turn " + Singletons.getModel().getGameState().getPhaseHandler().getTurn()
                    + " (" + Singletons.getModel().getGameState().getPhaseHandler().getPlayerTurn() + ")",
                0);
        Singletons.getModel().getGameState().getPhaseHandler().setNeedToNextPhase(false);
        PhaseUtil.visuallyActivatePhase(Singletons.getModel().getGameState().getPhaseHandler().getPhase());

        this.stop();
    }

    @Override
    public void selectCard(Card c0, PlayerZone z0) {

        if (c0.getName().equals("Serum Powder") && z0.is(ZoneType.Hand)) {
            if (GameActionUtil.showYesNoDialog(c0, "Use " + c0.getName() + "'s ability?")) {
                CardList hand = c0.getController().getCardsIn(ZoneType.Hand);
                for (Card c : hand) {
                    Singletons.getModel().getGameAction().exile(c);
                }
                c0.getController().drawCards(hand.size());
            }
        } else {
            SDisplayUtil.remind(VMessage.SINGLETON_INSTANCE);
        }
    }
}
