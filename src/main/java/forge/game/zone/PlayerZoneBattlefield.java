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
package forge.game.zone;

import java.util.List;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.Card;
import forge.CardLists;
import forge.CardPredicates;
import forge.CardPredicates.Presets;
import forge.card.ability.AbilityFactory;
import forge.card.mana.ManaCost;
import forge.card.spellability.Ability;
import forge.card.spellability.SpellAbility;
import forge.card.staticability.StaticAbility;
import forge.card.trigger.ZCTrigger;
import forge.game.GameActionUtil;
import forge.game.Game;
import forge.game.player.Player;

/**
 * <p>
 * PlayerZoneComesIntoPlay class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class PlayerZoneBattlefield extends PlayerZone {
    /** Constant <code>serialVersionUID=5750837078903423978L</code>. */
    private static final long serialVersionUID = 5750837078903423978L;

    private boolean trigger = true;
    private boolean leavesTrigger = true;

    public PlayerZoneBattlefield(final ZoneType zone, final Player player) {
        super(zone, player);
    }

    /** {@inheritDoc} */
    @Override
    public final void add(final Card c) {
        if (c == null) {
            throw new RuntimeException("PlayerZoneComesInto Play : add() object is null");
        }

        super.add(c);

        if (this.trigger) {
            if (c.hasKeyword("Hideaway")) {
                // it enters the battlefield this way, and should not fire
                // triggers
                c.setTapped(true);
            } else {
                // ETBTapped static abilities
                for (final Card ca : game.getCardsIn(ZoneType.listValueOf("Battlefield,Command"))) {
                    for (final StaticAbility stAb : ca.getStaticAbilities()) {
                        if (stAb.applyAbility("ETBTapped", c)) {
                            // it enters the battlefield this way, and should
                            // not fire triggers
                            c.setTapped(true);
                        }
                    }
                }
            }
        }

        if (this.trigger) {
            c.setSickness(true); // summoning sickness
            c.executeTrigger(ZCTrigger.ENTERFIELD);
            
            if (c.isLand()) {
                // Tectonic Instability
                final List<Card> tis =
                        CardLists.filter(game.getCardsIn(ZoneType.Battlefield), CardPredicates.nameEquals("Tectonic Instability"));
                final Card tisLand = c;
                for (final Card ti : tis) {
                    final Card source = ti;
                    final SpellAbility ability = new Ability(source, ManaCost.NO_COST) {
                        @Override
                        public void resolve() {
                            List<Card> lands = CardLists.filter(tisLand.getController().getCardsIn(ZoneType.Battlefield), Presets.LANDS);
                            for (final Card land : lands) {
                                land.tap();
                            }
                        }
                    };
                    final StringBuilder sb = new StringBuilder();
                    sb.append(source).append(" - tap all lands ");
                    sb.append(tisLand.getController()).append(" controls.");
                    ability.setStackDescription(sb.toString());
                    game.getStack().addSimultaneousStackEntry(ability);

                }


                for( Player opp : c.getOwner().getOpponents())
                    for( Card le : opp.getCardsIn(ZoneType.Battlefield, "Land Equilibrium") ) {
                        final List<Card> pLands = c.getOwner().getLandsInPlay();
                        final List<Card> oLands = opp.getLandsInPlay();
                        
                        if (oLands.size() <= (pLands.size() - 1)) {
                            SpellAbility abSac = AbilityFactory.getAbility(le.getSVar("SacLand"), le);
                            game.getStack().addSimultaneousStackEntry(abSac);
                        }
                    }
            } // isLand()
        }

        if (game.getStaticEffects().getCardToEffectsList().containsKey(c.getName())) {
            final String[] effects = game.getStaticEffects().getCardToEffectsList().get(c.getName());
            for (final String effect : effects) {
                game.getStaticEffects().addStateBasedEffect(effect);
            }
        }
    } // end add()

    /** {@inheritDoc} */
    @Override
    public final void remove(final Card c) {
        super.remove(c);

        if (this.leavesTrigger) {
            c.executeTrigger(ZCTrigger.LEAVEFIELD);
        }

        if (game.getStaticEffects().getCardToEffectsList().containsKey(c.getName())) {
            final String[] effects = game.getStaticEffects().getCardToEffectsList().get(c.getName());
            String tempEffect = "";
            for (final String effect : effects) {
                tempEffect = effect;
                game.getStaticEffects().removeStateBasedEffect(effect);
                // this is to make sure cards reset correctly
                final Function<Game, ?> comm = GameActionUtil.getCommands().get(tempEffect);
                comm.apply(game);
            }
        }
    }


    public final void setTriggers(final boolean b) {
        this.trigger = b;
        this.leavesTrigger = b;
    }

    private static Predicate<Card> isNotPhased = new Predicate<Card>() {
        @Override
        public boolean apply(Card crd) {
            return !crd.isPhasedOut();
        }
    };


    @Override
    public final List<Card> getCards(final boolean filter) {
        // Battlefield filters out Phased Out cards by default. Needs to call
        // getCards(false) to get Phased Out cards

        if (!filter) {
            return super.getCards(false);
        }
        return Lists.newArrayList(Iterables.filter(roCardList, isNotPhased));

    }
}
