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
import java.util.List;

import com.google.common.collect.Iterables;

import forge.Card;

import forge.CardLists;
import forge.CardPredicates;
import forge.GameAction;
import forge.GameActionUtil;
import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.spellability.SpellAbility;
import forge.game.GameState;
import forge.game.player.ComputerUtil;
import forge.game.player.Player;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.gui.framework.SDisplayUtil;
import forge.gui.match.CMatchUI;
import forge.gui.match.VMatchUI;
import forge.gui.match.nonsingleton.VField;
import forge.gui.match.views.VMessage;
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

    private static final int AI_MULLIGAN_THRESHOLD = 5;

    /** {@inheritDoc} */
    @Override
    public final void showMessage() {
        ButtonUtil.enableAll();
        VMatchUI.SINGLETON_INSTANCE.getBtnOK().setText("No");
        VMatchUI.SINGLETON_INSTANCE.getBtnCancel().setText("Yes");

        final String str =
                (Singletons.getModel().getGame().getPhaseHandler().getPlayerTurn().equals(Singletons.getControl().getPlayer())
                        ? "You're going first. " : "The computer is going first. ");
        CMatchUI.SINGLETON_INSTANCE.showMessage(str + "Do you want to Mulligan?");
    }

    /** {@inheritDoc} */
    @Override
    public final void selectButtonOK() {
        this.end();
    }



    /** {@inheritDoc} */
    @Override
    public final void selectButtonCancel() {
        final Player humanPlayer = Singletons.getControl().getPlayer();


        final int newHand = humanPlayer.doMulligan();

        if (newHand == 0) {
            this.end();
        }
    } // selectButtonOK()

    final void end() {
        GameState game = Singletons.getModel().getGame();

        // Computer mulligan
        for (Player ai : game.getPlayers()) {
            if (ai.isHuman()) {
                continue;
            }

            boolean aiTakesMulligan = true;

            // Computer mulligans if there are no cards with converted mana cost of
            // 0 in its hand
            while (aiTakesMulligan) {

                final List<Card> handList = ai.getCardsIn(ZoneType.Hand);
                final boolean hasLittleCmc0Cards = CardLists.getValidCards(handList, "Card.cmcEQ0", ai, null).size() < 2;
                aiTakesMulligan = (handList.size() > InputMulligan.AI_MULLIGAN_THRESHOLD) && hasLittleCmc0Cards;

                if (aiTakesMulligan) {
                    ai.doMulligan();
                }
            }
        }

        // Human Leylines & Chancellors
        ButtonUtil.reset();
        final AbilityFactory af = new AbilityFactory();

        final GameAction ga = game.getAction();
        for (Player p : game.getPlayers()) {
            final List<Card> openingHand = new ArrayList<Card>(p.getCardsIn(ZoneType.Hand));

            for (final Card c : openingHand) {
                if (p.isHuman()) {
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
                } else { // Computer Leylines & Chancellors
                    if (!c.getName().startsWith("Leyline")) {
                        final ArrayList<String> kws = c.getKeyword();
                        for (int i = 0; i < kws.size(); i++) {
                            final String kw = kws.get(i);

                            if (kw.startsWith("MayEffectFromOpeningHand")) {
                                final String effName = kw.split(":")[1];

                                final SpellAbility effect = af.getAbility(c.getSVar(effName), c);

                                // Is there a better way for the AI to decide this?
                                if (effect.doTrigger(false)) {
                                    GameActionUtil.showInfoDialg("Computer reveals " + c.getName() + "(" + c.getUniqueNumber() + ").");
                                    ComputerUtil.playNoStack(p, effect);
                                }
                            }
                        }
                    }
                    if (c.getName().startsWith("Leyline")
                            && !(c.getName().startsWith("Leyline of Singularity")
                            && (Iterables.any(game.getCardsIn(ZoneType.Battlefield), CardPredicates.nameEquals("Leyline of Singularity"))))) {
                        ga.moveToPlay(c);
                        //ga.checkStateEffects();
                    }
                }
            }
        }

        ga.checkStateEffects();
        Singletons.getModel().getMatch().getInput().clearInput();

        //Set Field shown to current player.
        Player next = game.getPhaseHandler().getPlayerTurn();
        VField nextField = CMatchUI.SINGLETON_INSTANCE.getFieldViewFor(next);
        SDisplayUtil.showTab(nextField);

        game.getPhaseHandler().nextPhase();
    }

    @Override
    public void selectCard(Card c0) {
        Zone z0 = Singletons.getModel().getGame().getZoneOf(c0);
        if (c0.getName().equals("Serum Powder") && z0.is(ZoneType.Hand)) {
            if (GameActionUtil.showYesNoDialog(c0, "Use " + c0.getName() + "'s ability?")) {
                List<Card> hand = new ArrayList<Card>(c0.getController().getCardsIn(ZoneType.Hand));
                for (Card c : hand) {
                    Singletons.getModel().getGame().getAction().exile(c);
                }
                c0.getController().drawCards(hand.size());
            }
        } else {
            SDisplayUtil.remind(VMessage.SINGLETON_INSTANCE);
        }
    }

    @Override
    public void isClassUpdated() {
    }
}
