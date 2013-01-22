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

import forge.Card;

import forge.Singletons;
import forge.card.SpellManaCost;
import forge.card.spellability.Ability;
import forge.card.spellability.SpellAbility;
import forge.game.GameLossReason;
import forge.game.GameState;
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
public class EndOfTurn extends Phase {
    /** Constant <code>serialVersionUID=-3656715295379727275L</code>. */
    private static final long serialVersionUID = -3656715295379727275L;

    public EndOfTurn(final GameState game) { super(game); }
    /**
     * <p>
     * Handles all the hardcoded events that happen "at end of turn".
     * </p>
     */
    @Override
    public final void executeAt() {
        // reset mustAttackEntity for me
        game.getPhaseHandler().getPlayerTurn().setMustAttackEntity(null);

        game.getStaticEffects().rePopulateStateBasedList();

        for (final Card c : game.getCardsIn(ZoneType.Battlefield)) {
            if (!c.isFaceDown() && c.hasKeyword("At the beginning of the end step, sacrifice CARDNAME.")) {
                final Card card = c;
                final SpellAbility sac = new Ability(card, SpellManaCost.ZERO) {
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

                game.getStack().addSimultaneousStackEntry(sac);

            }
            if (!c.isFaceDown() && c.hasKeyword("At the beginning of the end step, exile CARDNAME.")) {
                final Card card = c;
                final SpellAbility exile = new Ability(card, SpellManaCost.ZERO) {
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

                game.getStack().addSimultaneousStackEntry(exile);

            }
            if (!c.isFaceDown() && c.hasKeyword("At the beginning of the end step, destroy CARDNAME.")) {
                final Card card = c;
                final SpellAbility destroy = new Ability(card, SpellManaCost.ZERO) {
                    @Override
                    public void resolve() {
                        if (card.isInPlay()) {
                            game.getAction().destroy(card);
                        }
                    }
                };
                final StringBuilder sb = new StringBuilder();
                sb.append("Destroy ").append(card);
                destroy.setStackDescription(sb.toString());
                destroy.setDescription(sb.toString());

                game.getStack().addSimultaneousStackEntry(destroy);

            }
            // Berserk is using this, so don't check isFaceDown()
            if (c.hasKeyword("At the beginning of the next end step, destroy CARDNAME if it attacked this turn.")) {
                if (c.getDamageHistory().getCreatureAttackedThisTurn()) {
                    final Card card = c;
                    final SpellAbility sac = new Ability(card, SpellManaCost.ZERO) {
                        @Override
                        public void resolve() {
                            if (card.isInPlay()) {
                                game.getAction().destroy(card);
                            }
                        }
                    };
                    final StringBuilder sb = new StringBuilder();
                    sb.append("Destroy ").append(card);
                    sac.setStackDescription(sb.toString());
                    sac.setDescription(sb.toString());

                    game.getStack().addSimultaneousStackEntry(sac);

                } else {
                    c.removeAllExtrinsicKeyword("At the beginning of the next end step, "
                            + "destroy CARDNAME if it attacked this turn.");
                }
            }

            if (c.hasKeyword("At the beginning of your end step, return CARDNAME to its owner's hand.")
                    && game.getPhaseHandler().isPlayerTurn(c.getController())) {
                final Card source = c;
                final SpellAbility change = new Ability(source, SpellManaCost.ZERO) {
                    @Override
                    public void resolve() {
                        if (source.isInPlay()) {
                            game.getAction().moveToHand(source);
                        }
                    }
                };
                final StringBuilder sb = new StringBuilder();
                sb.append(source).append(" - At the beginning of your end step, return CARDNAME to its owner's hand.");
                change.setStackDescription(sb.toString());
                change.setDescription(sb.toString());

                game.getStack().addSimultaneousStackEntry(change);

            }

        }
        Player activePlayer = game.getPhaseHandler().getPlayerTurn();
        if (activePlayer.hasKeyword("At the beginning of this turn's end step, you lose the game.")) {
            final Card source = new Card();
            final SpellAbility change = new Ability(source, SpellManaCost.ZERO) {
                @Override
                public void resolve() {
                    this.getActivatingPlayer().loseConditionMet(GameLossReason.SpellEffect, "");
                }
            };
            change.setStackDescription("At the beginning of this turn's end step, you lose the game.");
            change.setDescription("At the beginning of this turn's end step, you lose the game.");
            change.setActivatingPlayer(activePlayer);

            game.getStack().addSimultaneousStackEntry(change);
        }

        this.execute(this.getAt());

    } // executeAt()

} // end class EndOfTurn
