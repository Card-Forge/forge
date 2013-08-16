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
import forge.card.ability.AbilityFactory;
import forge.card.mana.ManaCost;
import forge.card.spellability.Ability;
import forge.card.spellability.SpellAbility;
import forge.game.Game;
import forge.game.player.GameLossReason;
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

    protected final Game game;
    public EndOfTurn(final Game game) { 
        super(PhaseType.END_OF_TURN);
        this.game = game;
    }
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
                String sb = "Sacrifice CARDNAME.";
                String effect = "AB$ Sacrifice | Cost$ 0 | SacValid$ Self";

                SpellAbility ability = AbilityFactory.getAbility(effect, card);
                ability.setActivatingPlayer(card.getController());
                ability.setDescription(sb);
                ability.setStackDescription(sb);
                ability.setTrigger(true);
                final int amount = card.getKeywordAmount("At the beginning of the end step, sacrifice CARDNAME.");
                for (int i = 0; i < amount; i++) {
                    game.getStack().addSimultaneousStackEntry(ability);
                }
                // Trigger once: sacrifice at the beginning of the next end step 
                c.removeAllExtrinsicKeyword("At the beginning of the end step, sacrifice CARDNAME.");
                c.removeAllExtrinsicKeyword("HIDDEN At the beginning of the end step, sacrifice CARDNAME.");
            }
            if (!c.isFaceDown() && c.hasKeyword("At the beginning of the end step, exile CARDNAME.")) {
                final Card card = c;
                final SpellAbility exile = new Ability(card, ManaCost.ZERO) {
                    @Override
                    public void resolve() {
                        final Card current = game.getCardState(card);
                        if (current.isInPlay()) {
                            game.getAction().exile(current);
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
                final SpellAbility destroy = new Ability(card, ManaCost.ZERO) {
                    @Override
                    public void resolve() {
                        final Card current = game.getCardState(card);
                        if (current.isInPlay()) {
                            game.getAction().destroy(current, this);
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
                    final SpellAbility sac = new Ability(card, ManaCost.ZERO) {
                        @Override
                        public void resolve() {
                            final Card current = game.getCardState(card);
                            if (current.isInPlay()) {
                                game.getAction().destroy(current, this);
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

        }
        Player activePlayer = game.getPhaseHandler().getPlayerTurn();
        if (activePlayer.hasKeyword("At the beginning of this turn's end step, you lose the game.")) {
            final Card source = new Card(game.nextCardId());
            final SpellAbility change = new Ability(source, ManaCost.ZERO) {
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

        this.execute(this.at);

    } // executeAt()

} // end class EndOfTurn
