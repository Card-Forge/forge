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
import java.util.HashMap;
import java.util.List;

import forge.Card;

import forge.CardLists;
import forge.CardUtil;
import forge.Command;
import forge.Constant;
import forge.Singletons;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.control.input.Input;
import forge.control.input.InputPayManaCost;
import forge.control.input.InputSelectManyCards;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;
import forge.gui.match.CMatchUI;


/**
 * <p>
 * CardFactory_Auras class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
class CardFactoryAuras {

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
        if (cardName.equals("Convincing Mirage") || cardName.equals("Phantasmal Terrain")) {

            final String[] newType = new String[1];
            final SpellAbility spell = new Spell(card) {

                private static final long serialVersionUID = 53941812202244498L;

                @Override
                public boolean canPlayAI() {

                    if (!super.canPlayAI()) {
                        return false;
                    }
                    final Player opp = getActivatingPlayer().getOpponent();
                    final String[] landTypes = new String[] { "Plains", "Island", "Swamp", "Mountain", "Forest" };
                    final HashMap<String, Integer> humanLandCount = new HashMap<String, Integer>();
                    final List<Card> humanlands = opp.getLandsInPlay();

                    for (final String landType : landTypes) {
                        humanLandCount.put(landType, 0);
                    }

                    for (final Card c : humanlands) {
                        for (final String singleType : c.getType()) {
                            if (CardUtil.isABasicLandType(singleType)) {
                                humanLandCount.put(singleType, humanLandCount.get(singleType) + 1);
                            }
                        }
                    }

                    int minAt = 0;
                    int minVal = Integer.MAX_VALUE;
                    for (int i = 0; i < landTypes.length; i++) {
                        if (this.getTargetCard().isType(landTypes[i])) {
                            continue;
                        }

                        if (humanLandCount.get(landTypes[i]) < minVal) {
                            minVal = humanLandCount.get(landTypes[i]);
                            minAt = i;
                        }
                    }

                    newType[0] = landTypes[minAt];
                    List<Card> list = opp.getLandsInPlay();
                    list = CardLists.getNotType(list, newType[0]); // Don't enchant lands
                                                        // that already have the type
                    if (list.isEmpty()) {
                        return false;
                    }
                    this.setTargetCard(list.get(0));
                    return true;
                } // canPlayAI()

                @Override
                public void resolve() {
                    // Only query player, AI will have decided already.
                    if (card.getController().isHuman()) {
                        newType[0] = GuiChoose.one("Select land type.", Constant.CardTypes.BASIC_TYPES);
                    }
                    Singletons.getModel().getGame().getAction().moveToPlay(card);

                    final Card c = this.getTargetCard();

                    if (c.isInPlay() && c.canBeTargetedBy(this)) {
                        card.enchantEntity(c);
                    }

                } // resolve()
            }; // SpellAbility

            spell.setDescription("");
            card.addSpellAbility(spell);

            final Command onEnchant = new Command() {

                private static final long serialVersionUID = 3528675112863241126L;

                @Override
                public void execute() {
                    if (card.isEnchanting()) {
                        final Card crd = card.getEnchantingCard();
                        final ArrayList<Card> seas = crd.getEnchantedBy();
                        int count = 0;
                        for (int i = 0; i < seas.size(); i++) {
                            if (seas.get(i).getName().equals(card.getName())) {
                                count = count + 1;
                            }
                        }
                        if (count == 1) {
                            crd.removeType("Swamp");
                            crd.removeType("Forest");
                            crd.removeType("Island");
                            crd.removeType("Plains");
                            crd.removeType("Mountain");
                            crd.removeType("Locus");
                            crd.removeType("Lair");

                            crd.addType(newType[0]);
                        } else {
                            Card otherSeas = null;
                            for (int i = 0; i < seas.size(); i++) {
                                if (seas.get(i) != card) {
                                    otherSeas = seas.get(i);
                                }
                            }
                            final SpellAbility[] abilities = otherSeas.getSpellAbility();
                            for (final SpellAbility abilitie : abilities) {
                                card.addSpellAbility(abilitie);
                            }
                        }
                    }
                } // execute()
            }; // Command

            final Command onUnEnchant = new Command() {
                private static final long serialVersionUID = -202144631191180334L;

                @Override
                public void execute() {
                    if (card.isEnchanting()) {
                        final Card crd = card.getEnchantingCard();
                        final ArrayList<Card> seas = crd.getEnchantedBy();
                        int count = 0;
                        for (int i = 0; i < seas.size(); i++) {
                            if (seas.get(i).getName().equals(card.getName())) {
                                count = count + 1;
                            }
                        }
                        if (count == 1) {
                            crd.removeType(newType[0]);
                            crd.removeType("Land");
                            crd.removeType("Basic");
                            crd.removeType("Snow");
                            crd.removeType("Legendary");
                            final SpellAbility[] cardAbilities = crd.getSpellAbility();
                            for (final SpellAbility cardAbilitie : cardAbilities) {
                                if (cardAbilitie.isIntrinsic()) {
                                    crd.removeSpellAbility(cardAbilitie);
                                }
                            }
                            final Card c = Singletons.getModel().getCardFactory().copyCard(crd);
                            final ArrayList<String> types = c.getType();
                            final SpellAbility[] abilities = card.getSpellAbility();
                            for (int i = 0; i < types.size(); i++) {
                                crd.addType(types.get(i));
                            }
                            for (final SpellAbility abilitie : abilities) {
                                crd.addSpellAbility(abilitie);
                            }
                        }
                    }
                } // execute()
            }; // Command

            final Command onLeavesPlay = new Command() {

                private static final long serialVersionUID = -45433022112460839L;

                @Override
                public void execute() {
                    if (card.isEnchanting()) {
                        final Card crd = card.getEnchantingCard();
                        card.unEnchantEntity(crd);
                    }
                }
            };

            card.addEnchantCommand(onEnchant);
            card.addUnEnchantCommand(onUnEnchant);
            card.addLeavesPlayCommand(onLeavesPlay);

            InputSelectManyCards runtime = new InputSelectManyCards(1, 1) {
                private static final long serialVersionUID = 3306017877120093415L;

                @Override
                protected boolean isValidChoice(Card c) {
                    if (!c.isLand() || !c.isInZone(ZoneType.Battlefield)) {
                        return false;
                    }

                    if (!c.canBeTargetedBy(spell)) {
                        CMatchUI.SINGLETON_INSTANCE.showMessage("Cannot target this card (Shroud? Protection?).");
                        return false;
                    }

                    return true;
                };

                @Override
                protected Input onDone() {
                    spell.setTargetCard(selected.get(0));
                    if (spell.getManaCost().equals("0")) {
                        Singletons.getModel().getGame().getStack().add(spell);
                        return null;
                    } else {
                        return new InputPayManaCost(spell);
                    }
                };
            };
            runtime.setMessage("Select target land");
            spell.setBeforePayMana(runtime);
        } // *************** END ************ END **************************

        else if (CardFactoryUtil.hasKeyword(card, "enchant") != -1) {
            final int n = CardFactoryUtil.hasKeyword(card, "enchant");
            if (n != -1) {
                final String parse = card.getKeyword().get(n).toString();
                final String[] k = parse.split(":");

                final SpellAbility sa = card.getSpellAbility()[0];
                sa.setIsMultiKicker(true);
                sa.setMultiKickerManaCost(k[1]);
            }
        }
    }

}
