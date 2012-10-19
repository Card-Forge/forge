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
package forge.game.phase;

import java.util.List;

import com.google.common.base.Predicate;

import forge.Card;

import forge.CardLists;
import forge.Counters;
import forge.Singletons;
import forge.card.spellability.Ability;
import forge.card.spellability.SpellAbility;
import forge.game.GameLossReason;
import forge.game.player.Player;
import forge.game.zone.ZoneType;


/**
 * <p>
 * Handles "until end of turn" effects and "at end of turn" triggers.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class EndOfTurn extends Phase implements java.io.Serializable {
    /** Constant <code>serialVersionUID=-3656715295379727275L</code>. */
    private static final long serialVersionUID = -3656715295379727275L;

    /**
     * <p>
     * Handles all the hardcoded events that happen "at end of turn".
     * </p>
     */
    @Override
    public final void executeAt() {

        // TODO - should this freeze the Stack?

        final List<Card> all = Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield);

        //EndOfTurn.endOfTurnWallOfReverence();
        EndOfTurn.endOfTurnLighthouseChronologist();

        // reset mustAttackEntity for me
        Singletons.getModel().getGame().getPhaseHandler().getPlayerTurn().setMustAttackEntity(null);

        Singletons.getModel().getGame().getStaticEffects().rePopulateStateBasedList();

        for (final Card c : all) {
            if (!c.isFaceDown() && c.hasKeyword("At the beginning of the end step, sacrifice CARDNAME.")) {
                final Card card = c;
                final SpellAbility sac = new Ability(card, "0") {
                    @Override
                    public void resolve() {
                        if (card.isInPlay()) {
                            Singletons.getModel().getGame().getAction().sacrifice(card, null);
                        }
                    }
                };
                final StringBuilder sb = new StringBuilder();
                sb.append("Sacrifice ").append(card);
                sac.setStackDescription(sb.toString());
                sac.setDescription(sb.toString());

                Singletons.getModel().getGame().getStack().addSimultaneousStackEntry(sac);

            }
            if (!c.isFaceDown() && c.hasKeyword("At the beginning of the end step, exile CARDNAME.")) {
                final Card card = c;
                final SpellAbility exile = new Ability(card, "0") {
                    @Override
                    public void resolve() {
                        if (card.isInPlay()) {
                            Singletons.getModel().getGame().getAction().exile(card);
                        }
                    }
                };
                final StringBuilder sb = new StringBuilder();
                sb.append("Exile ").append(card);
                exile.setStackDescription(sb.toString());
                exile.setDescription(sb.toString());

                Singletons.getModel().getGame().getStack().addSimultaneousStackEntry(exile);

            }
            if (!c.isFaceDown() && c.hasKeyword("At the beginning of the end step, destroy CARDNAME.")) {
                final Card card = c;
                final SpellAbility destroy = new Ability(card, "0") {
                    @Override
                    public void resolve() {
                        if (card.isInPlay()) {
                            Singletons.getModel().getGame().getAction().destroy(card);
                        }
                    }
                };
                final StringBuilder sb = new StringBuilder();
                sb.append("Destroy ").append(card);
                destroy.setStackDescription(sb.toString());
                destroy.setDescription(sb.toString());

                Singletons.getModel().getGame().getStack().addSimultaneousStackEntry(destroy);

            }
            // Berserk is using this, so don't check isFaceDown()
            if (c.hasKeyword("At the beginning of the next end step, destroy CARDNAME if it attacked this turn.")) {
                if (c.getDamageHistory().getCreatureAttackedThisTurn()) {
                    final Card card = c;
                    final SpellAbility sac = new Ability(card, "0") {
                        @Override
                        public void resolve() {
                            if (card.isInPlay()) {
                                Singletons.getModel().getGame().getAction().destroy(card);
                            }
                        }
                    };
                    final StringBuilder sb = new StringBuilder();
                    sb.append("Destroy ").append(card);
                    sac.setStackDescription(sb.toString());
                    sac.setDescription(sb.toString());

                    Singletons.getModel().getGame().getStack().addSimultaneousStackEntry(sac);

                } else {
                    c.removeAllExtrinsicKeyword("At the beginning of the next end step, "
                            + "destroy CARDNAME if it attacked this turn.");
                }
            }
            if (c.hasKeyword("An opponent gains control of CARDNAME at the beginning of the next end step.")) {
                final Card vale = c;
                final SpellAbility change = new Ability(vale, "0") {
                    @Override
                    public void resolve() {
                        if (vale.isInPlay()) {
                            vale.addController(vale.getController().getOpponent());
                            // Singletons.getModel().getGameAction().changeController(
                            // new ArrayList<Card>(vale), vale.getController(),
                            // vale.getController().getOpponent());

                            vale.removeAllExtrinsicKeyword("An opponent gains control of CARDNAME "
                                    + "at the beginning of the next end step.");
                        }
                    }
                };
                final StringBuilder sb = new StringBuilder();
                sb.append(vale.getName()).append(" changes controllers.");
                change.setStackDescription(sb.toString());
                change.setDescription(sb.toString());

                Singletons.getModel().getGame().getStack().addSimultaneousStackEntry(change);

            }
            if (c.getName().equals("Erg Raiders") && !c.getDamageHistory().getCreatureAttackedThisTurn() && !c.hasSickness()
                    && Singletons.getModel().getGame().getPhaseHandler().isPlayerTurn(c.getController())) {
                final Card raider = c;
                final SpellAbility change = new Ability(raider, "0") {
                    @Override
                    public void resolve() {
                        if (raider.isInPlay()) {
                            raider.getController().addDamage(2, raider);
                        }
                    }
                };
                final StringBuilder sb = new StringBuilder();
                sb.append(raider).append(" deals 2 damage to controller.");
                change.setStackDescription(sb.toString());
                change.setDescription(sb.toString());

                Singletons.getModel().getGame().getStack().addSimultaneousStackEntry(change);

            }
            if (c.hasKeyword("At the beginning of your end step, return CARDNAME to its owner's hand.")
                    && Singletons.getModel().getGame().getPhaseHandler().isPlayerTurn(c.getController())) {
                final Card source = c;
                final SpellAbility change = new Ability(source, "0") {
                    @Override
                    public void resolve() {
                        if (source.isInPlay()) {
                            Singletons.getModel().getGame().getAction().moveToHand(source);
                        }
                    }
                };
                final StringBuilder sb = new StringBuilder();
                sb.append(source).append(" - At the beginning of your end step, return CARDNAME to its owner's hand.");
                change.setStackDescription(sb.toString());
                change.setDescription(sb.toString());

                Singletons.getModel().getGame().getStack().addSimultaneousStackEntry(change);

            }

        }
        Player activePlayer = Singletons.getModel().getGame().getPhaseHandler().getPlayerTurn();
        if (activePlayer.hasKeyword("At the beginning of this turn's end step, you lose the game.")) {
            final Card source = new Card();
            final SpellAbility change = new Ability(source, "0") {
                @Override
                public void resolve() {
                    this.getActivatingPlayer().loseConditionMet(GameLossReason.SpellEffect, "");
                }
            };
            change.setStackDescription("At the beginning of this turn's end step, you lose the game.");
            change.setDescription("At the beginning of this turn's end step, you lose the game.");
            change.setActivatingPlayer(activePlayer);

            Singletons.getModel().getGame().getStack().addSimultaneousStackEntry(change);
        }

        this.execute(this.getAt());

    } // executeAt()

    private static void endOfTurnLighthouseChronologist() {
        final Player player = Singletons.getModel().getGame().getPhaseHandler().getPlayerTurn();
        final Player opponent = player.getOpponent();
        List<Card> list = opponent.getCardsIn(ZoneType.Battlefield);

        list = CardLists.filter(list, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return c.getName().equals("Lighthouse Chronologist") && (c.getCounters(Counters.LEVEL) >= 7);
            }
        });

        Ability ability;
        for (int i = 0; i < list.size(); i++) {
            final Card card = list.get(i);
            ability = new Ability(list.get(i), "0") {
                @Override
                public void resolve() {
                    Singletons.getModel().getGame().getPhaseHandler().addExtraTurn(card.getController());
                }
            };

            final StringBuilder sb = new StringBuilder();
            sb.append(card).append(" - ").append(card.getController()).append(" takes an extra turn.");
            ability.setStackDescription(sb.toString());
            ability.setDescription(sb.toString());

            Singletons.getModel().getGame().getStack().addSimultaneousStackEntry(ability);

        }
    }

} // end class EndOfTurn
