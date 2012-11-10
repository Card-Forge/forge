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
package forge.card.cardfactory;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Predicate;

import forge.Card;

import forge.CardLists;
import forge.CardPredicates.Presets;
import forge.Singletons;
import forge.card.cost.Cost;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.ComputerUtil;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;

/**
 * <p>
 * CardFactoryInstants class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class CardFactoryInstants {

    /**
     * <p>
     * getCard.
     * </p>
     * 
     * @param card
     *            a {@link forge.Card} object.
     * @param cardName
     *            a {@link java.lang.String} object.
     * @return a {@link forge.Card} object.
     */
    public static void buildCard(final Card card, final String cardName) {

        // *************** START *********** START **************************
        if (cardName.equals("Suffer the Past")) {
            final Cost cost = new Cost(card, "X B", false);
            final Target tgt = new Target(card, "Select a Player", "Player");
            final SpellAbility spell = new Spell(card, cost, tgt) {
                private static final long serialVersionUID = 1168802375190293222L;

                @Override
                public void resolve() {
                    final Player tPlayer = this.getTargetPlayer();
                    final Player player = card.getController();
                    final int max = card.getXManaCostPaid();

                    final List<Card> graveList = new ArrayList<Card>(tPlayer.getCardsIn(ZoneType.Graveyard));
                    final int x = Math.min(max, graveList.size());

                    if (player.isHuman()) {
                        for (int i = 0; i < x; i++) {
                            final Object o = GuiChoose.one("Remove from game", graveList);
                            if (o == null) {
                                break;
                            }
                            final Card c1 = (Card) o;
                            graveList.remove(c1); // remove from the display
                                                  // list
                            Singletons.getModel().getGame().getAction().exile(c1);
                        }
                    } else { // Computer
                        // Random random = MyRandom.random;
                        for (int j = 0; j < x; j++) {
                            // int index = random.nextInt(X-j);
                            Singletons.getModel().getGame().getAction().exile(graveList.get(j));
                        }
                    }

                    tPlayer.loseLife(x);
                    player.gainLife(x, card);
                    card.setXManaCostPaid(0);
                }

                @Override
                public boolean canPlayAI() {
                    final Player ai = getActivatingPlayer();
                    final Player opp = ai.getOpponent();

                    final int maxX = ComputerUtil.getAvailableMana(ai, true).size() - 1;
                    this.getTarget().resetTargets();
                    return ComputerUtil.targetHumanAI(this) && (maxX >= 3) && !opp.getZone(ZoneType.Graveyard).isEmpty();
                }
            };

            card.addSpellAbility(spell);
        } // *************** END ************ END **************************        

        // *************** START *********** START **************************
        else if (cardName.equals("Remove Enchantments")) {
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = -7324132132222075031L;

                @Override
                public boolean canPlayAI() {
                    return false;
                }

                @Override
                public void resolve() {
                    final Player you = card.getController();
                    final List<Card> ens = CardLists.filter(Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield), Presets.ENCHANTMENTS);
                    final List<Card> toReturn = CardLists.filter(ens, new Predicate<Card>() {
                        @Override
                        public boolean apply(final Card c) {
                            final Card enchanting = c.getEnchantingCard();

                            if (enchanting != null) {
                                if ((enchanting.isAttacking() && enchanting.getController().equals(you.getOpponent()))
                                        || enchanting.getController().equals(you)) {
                                    return true;
                                }
                            }

                            return (c.getOwner().equals(you) && c.getController().equals(you));
                        }
                    });
                    for (final Card c : toReturn) {
                        Singletons.getModel().getGame().getAction().moveToHand(c);
                    }

                    for (final Card c : ens) {
                        if (!toReturn.contains(c)) {
                            Singletons.getModel().getGame().getAction().destroy(c);
                        }
                    }
                }
            };

            final StringBuilder sb = new StringBuilder();
            sb.append(card).append(" - destroy/return enchantments.");
            spell.setStackDescription(sb.toString());

            card.addSpellAbility(spell);
        } // *************** END ************ END **************************
    } // getCard

} // end class CardFactory_Instants
