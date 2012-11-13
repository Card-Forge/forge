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

import com.esotericsoftware.minlog.Log;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import forge.Card;

import forge.CardLists;
import forge.CardPredicates.Presets;
import forge.CardUtil;
import forge.Command;
import forge.Constant;
import forge.Singletons;
import forge.card.cost.Cost;
import forge.card.spellability.Ability;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.SpellPermanent;
import forge.card.spellability.Target;
import forge.control.input.Input;
import forge.control.input.InputPayManaCost;
import forge.control.input.InputSelectManyCards;
import forge.game.player.Player;
import forge.game.zone.PlayerZone;
import forge.game.zone.Zone;
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
                                                        // that already have the
                                                        // type
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
                    if (!c.isLand() || !c.isInZone(ZoneType.Battlefield))
                        return false;
                        
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

        // *************** START *********** START **************************
        else if (cardName.equals("Earthbind")) {
            final Cost cost = new Cost(card, card.getManaCost(), false);
            final Target tgt = new Target(card, "C");
            final SpellAbility spell = new SpellPermanent(card, cost, tgt) {

                private static final long serialVersionUID = 142389375702113977L;

                @Override
                public boolean canPlayAI() {
                    List<Card> list = getActivatingPlayer().getOpponent().getCreaturesInPlay();
                    list = CardLists.getKeyword(list, "Flying");
                    if (list.isEmpty()) {
                        return false;
                    }

                    final Predicate<Card> f = new Predicate<Card>() {
                        @Override
                        public final boolean apply(final Card c) {
                            return (c.getNetDefense() - c.getDamage()) <= 2;
                        }
                    };
                    if (Iterables.any(list, f)) {
                        list = CardLists.filter(list, f);
                    }
                    CardLists.sortAttack(list);

                    for (int i = 0; i < list.size(); i++) {
                        if (list.get(i).canBeTargetedBy(this)) {
                            this.setTargetCard(list.get(i));
                            return super.canPlayAI();
                        }
                    }
                    return false;
                } // canPlayAI()

                @Override
                public void resolve() {
                    Singletons.getModel().getGame().getAction().moveToPlay(card);

                    final Card c = this.getTargetCard();

                    if (c.isInPlay() && c.canBeTargetedBy(this)) {
                        card.enchantEntity(c);
                        Log.debug("Enchanted: " + this.getTargetCard());
                    }
                } // resolve()
            }; // SpellAbility

            card.addSpellAbility(spell);

            final boolean[] badTarget = { true };
            final Command onEnchant = new Command() {

                private static final long serialVersionUID = -5302506578307993978L;

                @Override
                public void execute() {
                    if (card.isEnchanting()) {
                        final Card crd = card.getEnchantingCard();
                        if (crd.hasKeyword("Flying")) {
                            badTarget[0] = false;
                            crd.addDamage(2, card);
                            crd.removeIntrinsicKeyword("Flying");
                            crd.removeAllExtrinsicKeyword("Flying");
                        } else {
                            badTarget[0] = true;
                        }
                    }
                } // execute()
            }; // Command

            final Command onUnEnchant = new Command() {

                private static final long serialVersionUID = -6908757692588823391L;

                @Override
                public void execute() {
                    if (card.isEnchanting() && !badTarget[0]) {
                        final Card crd = card.getEnchantingCard();
                        crd.addIntrinsicKeyword("Flying");
                    }
                } // execute()
            }; // Command

            final Command onLeavesPlay = new Command() {

                private static final long serialVersionUID = -7833240882415702940L;

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
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Guilty Conscience")) {
            final Cost cost = new Cost(card, card.getManaCost(), false);
            final Target tgt = new Target(card, "C");
            final SpellAbility spell = new SpellPermanent(card, cost, tgt) {

                private static final long serialVersionUID = 1169151960692309514L;

                @Override
                public boolean canPlayAI() {

                    final List<Card> stuffy = getActivatingPlayer().getCardsIn(ZoneType.Battlefield, "Stuffy Doll");

                    if (stuffy.size() > 0) {
                        this.setTargetCard(stuffy.get(0));
                        return true;
                    } else {
                        final List<Card> list = getActivatingPlayer().getOpponent().getCreaturesInPlay();

                        if (list.isEmpty()) {
                            return false;
                        }

                        // else
                        CardLists.sortAttack(list);
                        CardLists.sortFlying(list);

                        for (int i = 0; i < list.size(); i++) {
                            if (list.get(i).canBeTargetedBy(this)
                                    && (list.get(i).getNetAttack() >= list.get(i).getNetDefense())
                                    && (list.get(i).getNetAttack() >= 3)) {
                                this.setTargetCard(list.get(i));
                                return super.canPlayAI();
                            }
                        }
                    }
                    return false;

                } // canPlayAI()

                @Override
                public void resolve() {
                    final Card aura = Singletons.getModel().getGame().getAction().moveToPlay(card);

                    final Card c = this.getTargetCard();

                    if (c.isInPlay() && c.canBeTargetedBy(this)) {
                        aura.enchantEntity(c);
                    }
                } // resolve()
            }; // SpellAbility

            card.addSpellAbility(spell);
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Animate Dead") || cardName.equals("Dance of the Dead")) {
            final Card[] targetC = new Card[1];
            // need to override what happens when this is cast.
            final SpellPermanent animate = new SpellPermanent(card) {
                private static final long serialVersionUID = 7126615291288065344L;

                public List<Card> getCreturesInGrave() {
                    // This includes creatures Animate Dead can't enchant once
                    // in play.
                    // The human may try to Animate them, the AI will not.
                    return CardLists.filter(Singletons.getModel().getGame().getCardsIn(ZoneType.Graveyard), Presets.CREATURES);
                }

                @Override
                public boolean canPlay() {
                    return super.canPlay() && (this.getCreturesInGrave().size() != 0);
                }

                @Override
                public boolean canPlayAI() {
                    List<Card> cList = this.getCreturesInGrave();
                    // AI will only target something that will stick in play.
                    cList = CardLists.getTargetableCards(cList, this);
                    if (cList.size() == 0) {
                        return false;
                    }

                    final Card c = CardFactoryUtil.getBestCreatureAI(cList);

                    this.setTargetCard(c);
                    final boolean playable = (2 < c.getNetAttack()) && (2 < c.getNetDefense()) && super.canPlayAI();
                    return playable;
                } // canPlayAI

                @Override
                public void resolve() {
                    targetC[0] = this.getTargetCard();
                    super.resolve();
                }

            }; // addSpellAbility

            // Target AbCost and Restriction are set here to get this working as
            // expected
            final Target tgt = new Target(card, "Select a creature in a graveyard", "Creature".split(","));
            tgt.setZone(ZoneType.Graveyard);
            animate.setTarget(tgt);

            final Cost cost = new Cost(card, "1 B", false);
            animate.setPayCosts(cost);

            animate.getRestrictions().setZone(ZoneType.Hand);

            final Ability attach = new Ability(card, "0") {
                @Override
                public void resolve() {
                    final PlayerZone play = card.getController().getZone(ZoneType.Battlefield);

                    // Animate Dead got destroyed before its ability resolved
                    if (!play.contains(card)) {
                        return;
                    }

                    final Card animated = targetC[0];
                    final Zone grave = Singletons.getModel().getGame().getZoneOf(animated);

                    if (!grave.is(ZoneType.Graveyard)) {
                        // Animated Creature got removed before ability resolved
                        Singletons.getModel().getGame().getAction().sacrifice(card, null);
                        return;
                    }

                    // Bring creature onto the battlefield under your control
                    // (should trigger etb Abilities)
                    animated.addController(card.getController());
                    Singletons.getModel().getGame().getAction().moveToPlay(animated, card.getController());
                    if (cardName.equals("Dance of the Dead")) {
                        animated.tap();
                    }
                    card.enchantEntity(animated); // Attach before Targeting so
                                                  // detach Command will trigger

                    if (CardFactoryUtil.hasProtectionFrom(card, animated)) {
                        // Animated a creature with protection
                        Singletons.getModel().getGame().getAction().sacrifice(card, null);
                        return;
                    }

                    // Everything worked out perfectly.
                }
            }; // Ability

            final Command attachCmd = new Command() {
                private static final long serialVersionUID = 3595188622377350327L;

                @Override
                public void execute() {
                    if (targetC[0] != null) {
                        //too slow - must be done immediately
                        //otherwise before attach is resolved state effect kills aura as it has no target...
//                        AllZone.getStack().addSimultaneousStackEntry(attach);

                        //this seems to work, but I'm not 100% sure of possible side effects (hopefully none)
                        attach.resolve();
                    } else {
                        // note: this should be a state-based action, but it doesn't work currently.
                        // I don't know if that because it's hard-coded or what, but this fixes
                        // these cards being put on the battlefield not attached to anything.
                        Singletons.getModel().getGame().getAction().moveToGraveyard(card);
                    }
                }
            };

            final Ability detach = new Ability(card, "0") {

                @Override
                public void resolve() {
                    final Card c = targetC[0];

                    final PlayerZone play = card.getController().getZone(ZoneType.Battlefield);

                    if (play.contains(c)) {
                        Singletons.getModel().getGame().getAction().sacrifice(c, null);
                    }
                }
            }; // Detach

            final Command detachCmd = new Command() {
                private static final long serialVersionUID = 2425333033834543422L;

                @Override
                public void execute() {
                    final Card c = targetC[0];

                    final PlayerZone play = card.getController().getZone(ZoneType.Battlefield);

                    if (play.contains(c)) {
                        Singletons.getModel().getGame().getStack().addSimultaneousStackEntry(detach);
                    }

                }
            };

            card.addSpellAbility(animate);

            final StringBuilder sbA = new StringBuilder();
            sbA.append("Attaching ").append(cardName).append(" to creature in graveyard.");
            attach.setStackDescription(sbA.toString());
            card.addComesIntoPlayCommand(attachCmd);
            final StringBuilder sbD = new StringBuilder();
            sbD.append(cardName).append(" left play. Sacrificing creature if still around.");
            detach.setStackDescription(sbD.toString());
            card.addLeavesPlayCommand(detachCmd);
            card.addUnEnchantCommand(detachCmd);
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
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
