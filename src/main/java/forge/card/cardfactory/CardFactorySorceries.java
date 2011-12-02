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
import java.util.Map.Entry;
import java.util.Vector;

import javax.swing.JOptionPane;

import com.esotericsoftware.minlog.Log;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.ButtonUtil;
import forge.Card;
import forge.CardList;
import forge.CardListFilter;
import forge.CardListUtil;
import forge.CardUtil;
import forge.Command;
import forge.ComputerUtil;
import forge.Constant;
import forge.Constant.Zone;
import forge.HandSizeOp;
import forge.Player;
import forge.PlayerUtil;
import forge.PlayerZone;
import forge.card.cost.Cost;
import forge.card.spellability.Ability;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.SpellPermanent;
import forge.card.spellability.Target;
import forge.gui.GuiUtils;
import forge.gui.input.Input;
import forge.gui.input.InputPayManaCost;
import forge.gui.input.InputPayManaCostAbility;

/**
 * <p>
 * CardFactory_Sorceries class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class CardFactorySorceries {

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
    public static Card getCard(final Card card, final String cardName) {

        // *************** START *********** START **************************
        if (cardName.equals("Political Trickery")) {
            final Card[] target = new Card[2];
            final int[] index = new int[1];

            final SpellAbility spell = new Spell(card) {

                private static final long serialVersionUID = -3075569295823682336L;

                @Override
                public boolean canPlayAI() {
                    return false;
                }

                @Override
                public void resolve() {

                    final Card crd0 = target[0];
                    final Card crd1 = target[1];

                    if ((crd0 != null) && (crd1 != null)) {
                        final Player p0 = crd0.getController();
                        final Player p1 = crd1.getController();
                        crd0.addController(p1);
                        crd1.addController(p0);
                        // AllZone.getGameAction().changeController(new
                        // CardList(crd0), p0, p1);
                        // AllZone.getGameAction().changeController(new
                        // CardList(crd1), p1, p0);
                    }

                } // resolve()
            }; // SpellAbility

            final Input input = new Input() {

                private static final long serialVersionUID = -1017253686774265770L;

                @Override
                public void showMessage() {
                    if (index[0] == 0) {
                        AllZone.getDisplay().showMessage("Select target land you control.");
                    } else {
                        AllZone.getDisplay().showMessage("Select target land opponent controls.");
                    }

                    ButtonUtil.enableOnlyCancel();
                }

                @Override
                public void selectButtonCancel() {
                    this.stop();
                }

                @Override
                public void selectCard(final Card c, final PlayerZone zone) {
                    // must target creature you control
                    if ((index[0] == 0) && !c.getController().equals(card.getController())) {
                        return;
                    }

                    // must target creature you don't control
                    if ((index[0] == 1) && c.getController().equals(card.getController())) {
                        return;
                    }

                    if (c.isLand() && zone.is(Constant.Zone.Battlefield) && c.canBeTargetedBy(spell)) {
                        target[index[0]] = c;
                        index[0]++;
                        this.showMessage();

                        if (index[0] == target.length) {
                            if (this.isFree()) {
                                this.setFree(false);
                                AllZone.getStack().add(spell);
                                this.stop();
                            } else {
                                this.stopSetNext(new InputPayManaCost(spell));
                            }
                        }
                    }
                } // selectCard()
            }; // Input

            final Input runtime = new Input() {

                private static final long serialVersionUID = 4003351872990899418L;

                @Override
                public void showMessage() {
                    index[0] = 0;
                    this.stopSetNext(input);
                }
            }; // Input

            card.addSpellAbility(spell);
            spell.setBeforePayMana(runtime);
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Insurrection")) {
            /*
             * Untap all creatures and gain control of them until end of turn.
             * They gain haste until end of turn.
             */
            final ArrayList<PlayerZone> orig = new ArrayList<PlayerZone>();
            final PlayerZone[] newZone = new PlayerZone[1];
            final ArrayList<Player> controllerEOT = new ArrayList<Player>();
            final ArrayList<Card> targets = new ArrayList<Card>();

            final Command untilEOT = new Command() {
                private static final long serialVersionUID = -5809548350739536763L;

                @Override
                public void execute() {
                    // int i = 0;
                    for (final Card target : targets) {
                        // if card isn't in play, do nothing
                        if (!AllZoneUtil.isCardInPlay(target)) {
                            continue;
                        }

                        target.removeController(card);
                        // AllZone.getGameAction().changeController(new
                        // CardList(target), card.getController(),
                        // controllerEOT.get(i));

                        target.removeExtrinsicKeyword("Haste");

                        // i++;
                    }
                } // execute()
            }; // Command

            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = -532862769235091780L;

                @Override
                public void resolve() {
                    final CardList creatures = AllZoneUtil.getCreaturesInPlay();
                    newZone[0] = card.getController().getZone(Constant.Zone.Battlefield);
                    final int i = 0;
                    for (final Card target : creatures) {
                        if (AllZoneUtil.isCardInPlay(target)) {
                            orig.add(i, AllZone.getZoneOf(target));
                            controllerEOT.add(i, target.getController());
                            targets.add(i, target);

                            target.addController(card);
                            // AllZone.getGameAction().changeController(new
                            // CardList(target), target.getController(),
                            // card.getController());

                            target.untap();
                            target.addExtrinsicKeyword("Haste");
                        } // is card in play?
                    } // end for
                    AllZone.getEndOfTurn().addUntil(untilEOT);
                } // resolve()

                @Override
                public boolean canPlayAI() {
                    final CardList creatures = AllZoneUtil.getCreaturesInPlay(AllZone.getHumanPlayer());
                    return (creatures.size() > 0) && AllZone.getPhase().getPhase().equals(Constant.Phase.MAIN1);
                } // canPlayAI()

            }; // SpellAbility
            card.addSpellAbility(spell);
            card.setSVar("PlayMain1", "TRUE");
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Mind's Desire")) {
            final Spell playCreature = new Spell(card) {
                private static final long serialVersionUID = 53838791023456795L;

                @Override
                public void resolve() {
                    final Player player = card.getController();
                    final PlayerZone play = player.getZone(Constant.Zone.Battlefield);
                    final PlayerZone rfg = player.getZone(Constant.Zone.Exile);
                    final Card[] attached = card.getAttachedCardsByMindsDesire();
                    rfg.remove(attached[0]);
                    play.add(attached[0]);
                    card.unattachCardByMindDesire(attached[0]);
                } // resolve()
            }; // SpellAbility

            final Ability freeCast = new Ability(card, "0") {

                @Override
                public void resolve() {
                    Card target = null;
                    Card c = null;
                    final Player player = card.getController();
                    if (player.isHuman()) {
                        final Card[] attached = this.getSourceCard().getAttachedCardsByMindsDesire();
                        final Card[] choices = new Card[attached.length];
                        boolean systemsGo = true;
                        if (AllZone.getStack().size() > 0) {
                            final CardList config = new CardList();
                            for (final Card element : attached) {
                                if (element.isInstant() || element.hasKeyword("Flash")) {
                                    config.add(element);
                                }
                            }
                            for (int i = 0; i < config.size(); i++) {
                                final Card crd = config.get(i);
                                choices[i] = crd;
                            }
                            if (config.size() == 0) {
                                systemsGo = false;
                            }
                        } else {
                            for (int i = 0; i < attached.length; i++) {
                                choices[i] = attached[i];
                            }
                        }
                        Object check = null;
                        if (systemsGo) {
                            check = GuiUtils.getChoiceOptional("Select Card to play for free", choices);
                            if (check != null) {
                                target = ((Card) check);
                            }
                            if (target != null) {
                                c = AllZone.getCardFactory().copyCard(target);
                            }

                            if (c != null) {
                                if (c.isLand()) {
                                    if (player.canPlayLand()) {
                                        player.playLand(c);
                                    } else {
                                        JOptionPane.showMessageDialog(null, "You can't play any more lands this turn.",
                                                "", JOptionPane.INFORMATION_MESSAGE);
                                    }
                                } else if (c.isPermanent() && c.isAura()) {
                                    c.removeIntrinsicKeyword("Flash"); // Stops
                                                                       // the
                                                                       // player
                                                                       // from
                                                                       // re-casting
                                                                       // the
                                                                       // flash
                                                                       // spell.

                                    final StringBuilder sb = new StringBuilder();
                                    sb.append(c.getName()).append(" - Copied from Mind's Desire");
                                    playCreature.setStackDescription(sb.toString());

                                    final Card[] reAttach = new Card[attached.length];
                                    reAttach[0] = c;
                                    int reAttachCount = 0;
                                    for (final Card element : attached) {
                                        if (element != target) {
                                            reAttachCount = reAttachCount + 1;
                                            reAttach[reAttachCount] = element;
                                        }
                                    }
                                    // Clear Attached List
                                    for (final Card element : attached) {
                                        card.unattachCardByMindDesire(element);
                                    }
                                    // Re-add
                                    for (final Card element : reAttach) {
                                        if (element != null) {
                                            card.attachCardByMindsDesire(element);
                                        }
                                    }
                                    target.addSpellAbility(playCreature);
                                    AllZone.getStack().add(playCreature);
                                } else {
                                    AllZone.getGameAction().playCardNoCost(c);
                                    card.unattachCardByMindDesire(c);
                                }
                            } else {
                                final StringBuilder sb = new StringBuilder();
                                sb.append("Player cancelled or there is no more ");
                                sb.append("cards available on Mind's Desire.");
                                JOptionPane.showMessageDialog(null, sb.toString(), "", JOptionPane.INFORMATION_MESSAGE);
                            }
                        } else {
                            final StringBuilder sb = new StringBuilder();
                            sb.append("You can only play an instant at this point in time, ");
                            sb.append("but none are attached to Mind's Desire.");
                            JOptionPane.showMessageDialog(null, sb.toString(), "", JOptionPane.INFORMATION_MESSAGE);
                        }
                    }
                }

                @Override
                public boolean canPlayAI() {
                    return false;
                }

            };
            freeCast.setStackDescription("Mind's Desire - play card without paying its mana cost.");

            final Command intoPlay = new Command() {
                private static final long serialVersionUID = 920148510259054021L;

                @Override
                public void execute() {
                    final Player player = AllZone.getPhase().getPlayerTurn();
                    final PlayerZone play = player.getZone(Constant.Zone.Battlefield);
                    Card mindsD = card;
                    if (player.isHuman()) {
                        card.getController().shuffle();
                    }
                    CardList mindsList = player.getCardsIn(Zone.Battlefield);
                    mindsList = mindsList.getName("Mind's Desire");
                    mindsList.remove(card);
                    if (mindsList.size() > 0) {
                        play.remove(card);
                        mindsD = mindsList.get(0);
                    } else {
                        final StringBuilder sb = new StringBuilder();
                        sb.append("Click Mind's Desire to see the available cards ");
                        sb.append("to play without paying its mana cost.");
                        JOptionPane.showMessageDialog(null, sb.toString(), "", JOptionPane.INFORMATION_MESSAGE);
                    }
                    final CardList libList = player.getCardsIn(Zone.Library);
                    Card c = null;
                    if (libList.size() > 0) {
                        c = libList.get(0);
                        final PlayerZone rfg = player.getZone(Constant.Zone.Exile);
                        AllZone.getGameAction().moveTo(rfg, c);
                        mindsD.attachCardByMindsDesire(c);
                    }
                    final Card minds = card;
                    // AllZone.getGameAction().exile(Minds);
                    minds.setImmutable(true);
                    final Command untilEOT = new Command() {
                        private static final long serialVersionUID = -28032591440730370L;

                        @Override
                        public void execute() {
                            final Player player = AllZone.getPhase().getPlayerTurn();
                            final PlayerZone play = player.getZone(Constant.Zone.Battlefield);
                            play.remove(minds);
                        }
                    };
                    AllZone.getEndOfTurn().addUntil(untilEOT);
                }

            };
            final SpellAbility spell = new SpellPermanent(card) {
                private static final long serialVersionUID = -2940969025405788931L;

                @Override
                public boolean canPlayAI() {
                    return false;
                }
            };

            card.addComesIntoPlayCommand(intoPlay);

            card.addSpellAbility(spell);
            card.addSpellAbility(freeCast);
            spell.setDescription("");
        }
        // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Brilliant Ultimatum")) {
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = 1481112451519L;

                @Override
                public void resolve() {

                    Card choice = null;

                    // check for no cards in hand on resolve
                    final CardList lib = card.getController().getCardsIn(Zone.Library);
                    final CardList cards = new CardList();
                    final CardList exiled = new CardList();
                    if (lib.size() == 0) {
                        JOptionPane.showMessageDialog(null, "No more cards in library.", "",
                                JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }
                    int count = 5;
                    if (lib.size() < 5) {
                        count = lib.size();
                    }
                    for (int i = 0; i < count; i++) {
                        cards.add(lib.get(i));
                    }
                    for (int i = 0; i < count; i++) {
                        exiled.add(lib.get(i));
                        AllZone.getGameAction().exile(lib.get(i));
                    }
                    final CardList pile1 = new CardList();
                    final CardList pile2 = new CardList();
                    boolean stop = false;
                    int pile1CMC = 0;
                    int pile2CMC = 0;

                    final StringBuilder msg = new StringBuilder();
                    msg.append("Revealing top ").append(count).append(" cards of library: ");
                    GuiUtils.getChoice(msg.toString(), cards.toArray());
                    // Human chooses
                    if (card.getController().isComputer()) {
                        for (int i = 0; i < count; i++) {
                            if (!stop) {
                                choice = GuiUtils.getChoiceOptional("Choose cards to put into the first pile: ",
                                        cards.toArray());
                                if (choice != null) {
                                    pile1.add(choice);
                                    cards.remove(choice);
                                    pile1CMC = pile1CMC + CardUtil.getConvertedManaCost(choice);
                                } else {
                                    stop = true;
                                }
                            }
                        }
                        for (int i = 0; i < count; i++) {
                            if (!pile1.contains(exiled.get(i))) {
                                pile2.add(exiled.get(i));
                                pile2CMC = pile2CMC + CardUtil.getConvertedManaCost(exiled.get(i));
                            }
                        }
                        final StringBuilder sb = new StringBuilder();
                        sb.append("You have spilt the cards into the following piles");
                        sb.append("\r\n").append("\r\n");
                        sb.append("Pile 1: ").append("\r\n");
                        for (int i = 0; i < pile1.size(); i++) {
                            sb.append(pile1.get(i).getName()).append("\r\n");
                        }
                        sb.append("\r\n").append("Pile 2: ").append("\r\n");
                        for (int i = 0; i < pile2.size(); i++) {
                            sb.append(pile2.get(i).getName()).append("\r\n");
                        }
                        JOptionPane.showMessageDialog(null, sb, "", JOptionPane.INFORMATION_MESSAGE);
                        if (pile1CMC >= pile2CMC) {
                            JOptionPane.showMessageDialog(null, "Computer chooses the Pile 1", "",
                                    JOptionPane.INFORMATION_MESSAGE);
                            for (int i = 0; i < pile1.size(); i++) {
                                final ArrayList<SpellAbility> choices = pile1.get(i).getBasicSpells();

                                for (final SpellAbility sa : choices) {
                                    if (sa.canPlayAI()) {
                                        ComputerUtil.playStackFree(sa);
                                        if (pile1.get(i).isPermanent()) {
                                            exiled.remove(pile1.get(i));
                                        }
                                        break;
                                    }
                                }
                            }
                        } else {
                            JOptionPane.showMessageDialog(null, "Computer chooses the Pile 2", "",
                                    JOptionPane.INFORMATION_MESSAGE);
                            for (int i = 0; i < pile2.size(); i++) {
                                final ArrayList<SpellAbility> choices = pile2.get(i).getBasicSpells();

                                for (final SpellAbility sa : choices) {
                                    if (sa.canPlayAI()) {
                                        ComputerUtil.playStackFree(sa);
                                        if (pile2.get(i).isPermanent()) {
                                            exiled.remove(pile2.get(i));
                                        }
                                        break;
                                    }
                                }
                            }
                        }

                    } else { // Computer chooses (It picks the highest converted
                             // mana cost card and 1 random card.)
                        Card biggest = exiled.get(0);

                        for (final Card c : exiled) {
                            if (CardUtil.getConvertedManaCost(biggest.getManaCost()) < CardUtil.getConvertedManaCost(c
                                    .getManaCost())) {
                                biggest = c;
                            }
                        }

                        pile1.add(biggest);
                        cards.remove(biggest);
                        if (cards.size() > 2) {
                            final Card random = CardUtil.getRandom(cards.toArray());
                            pile1.add(random);
                        }
                        for (int i = 0; i < count; i++) {
                            if (!pile1.contains(exiled.get(i))) {
                                pile2.add(exiled.get(i));
                            }
                        }
                        final StringBuilder sb = new StringBuilder();
                        sb.append("Choose a pile to add to your hand: ");
                        sb.append("\r\n").append("\r\n");
                        sb.append("Pile 1: ").append("\r\n");
                        for (int i = 0; i < pile1.size(); i++) {
                            sb.append(pile1.get(i).getName()).append("\r\n");
                        }
                        sb.append("\r\n").append("Pile 2: ").append("\r\n");
                        for (int i = 0; i < pile2.size(); i++) {
                            sb.append(pile2.get(i).getName()).append("\r\n");
                        }
                        final Object[] possibleValues = { "Pile 1", "Pile 2" };
                        final Object q = JOptionPane.showOptionDialog(null, sb, "Brilliant Ultimatum",
                                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, possibleValues,
                                possibleValues[0]);

                        CardList chosen;
                        if (q.equals(0)) {
                            chosen = pile1;
                        } else {
                            chosen = pile2;
                        }

                        final int numChosen = chosen.size();
                        for (int i = 0; i < numChosen; i++) {
                            final Object check = GuiUtils.getChoiceOptional("Select spells to play in reverse order: ",
                                    chosen.toArray());
                            if (check == null) {
                                break;
                            }

                            final Card playing = (Card) check;
                            if (playing.isLand()) {
                                if (card.getController().canPlayLand()) {
                                    card.getController().playLand(playing);
                                } else {
                                    JOptionPane.showMessageDialog(null, "You can't play any more lands this turn.", "",
                                            JOptionPane.INFORMATION_MESSAGE);
                                }
                            } else {
                                AllZone.getGameAction().playCardNoCost(playing);
                            }
                            chosen.remove(playing);
                        }

                    }
                    pile1.clear();
                    pile2.clear();
                } // resolve()

                @Override
                public boolean canPlayAI() {
                    final CardList cards = AllZone.getComputerPlayer().getCardsIn(Zone.Library);
                    return cards.size() >= 8;
                }
            }; // SpellAbility
            card.addSpellAbility(spell);
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Cranial Extraction")) {
            final Cost cost = new Cost("3 B", cardName, false);
            final Target tgt = new Target(card, "Select a Player", "Player");
            final SpellAbility spell = new Spell(card, cost, tgt) {
                private static final long serialVersionUID = 8127696608769903507L;

                @Override
                public void resolve() {
                    final Player target = this.getTargetPlayer();
                    String choice = null;

                    // human chooses
                    if (card.getController().isHuman()) {
                        choice = JOptionPane.showInputDialog(null, "Name a nonland card", cardName,
                                JOptionPane.QUESTION_MESSAGE);

                        final CardList showLibrary = target.getCardsIn(Zone.Library);
                        GuiUtils.getChoiceOptional("Target Player's Library", showLibrary.toArray());

                        final CardList showHand = target.getCardsIn(Zone.Hand);
                        GuiUtils.getChoiceOptional("Target Player's Hand", showHand.toArray());
                    } // if
                    else {
                        // computer chooses
                        // the computer cheats by choosing a creature in the
                        // human players library or hand
                        final CardList all = target.getCardsIn(Zone.Hand);
                        all.addAll(target.getCardsIn(Zone.Library));

                        final CardList four = all.filter(new CardListFilter() {
                            @Override
                            public boolean addCard(final Card c) {
                                if (c.isLand()) {
                                    return false;
                                }

                                return 3 < CardUtil.getConvertedManaCost(c.getManaCost());
                            }
                        });
                        if (!four.isEmpty()) {
                            choice = CardUtil.getRandom(four.toArray()).getName();
                        } else {
                            choice = CardUtil.getRandom(all.toArray()).getName();
                        }

                    } // else
                    this.remove(choice, target);
                    target.shuffle();
                } // resolve()

                void remove(final String name, final Player player) {
                    final CardList all = player.getCardsIn(Zone.Hand);
                    all.addAll(player.getCardsIn(Zone.Graveyard));
                    all.addAll(player.getCardsIn(Zone.Library));

                    for (int i = 0; i < all.size(); i++) {
                        if (all.get(i).getName().equals(name)) {
                            if (!all.get(i).isLand()) {
                                AllZone.getGameAction().exile(all.get(i));
                            }
                        }
                    }
                } // remove()

                @Override
                public boolean canPlayAI() {
                    CardList c = AllZone.getHumanPlayer().getCardsIn(Zone.Library);
                    c = c.filter(CardListFilter.NON_LANDS);
                    return c.size() > 0;
                }
            }; // SpellAbility spell
            spell.setChooseTargetAI(CardFactoryUtil.targetHumanAI());

            card.addSpellAbility(spell);
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Maelstrom Pulse")) {
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = -4050843868789582138L;

                @Override
                public boolean canPlayAI() {
                    final CardList c = this.getCreature();
                    if (c.isEmpty()) {
                        return false;
                    } else {
                        this.setTargetCard(c.get(0));
                        return true;
                    }
                } // canPlayAI()

                CardList getCreature() {
                    final CardList out = new CardList();
                    final CardList list = CardFactoryUtil.getHumanCreatureAI("Flying", this, true);
                    list.shuffle();

                    for (int i = 0; i < list.size(); i++) {
                        if ((list.get(i).getNetAttack() >= 2) && (list.get(i).getNetDefense() <= 2)) {
                            out.add(list.get(i));
                        }
                    }

                    // in case human player only has a few creatures in play,
                    // target anything
                    if (out.isEmpty() && (0 < CardFactoryUtil.getHumanCreatureAI(2, this, true).size())
                            && (3 > CardFactoryUtil.getHumanCreatureAI(this, true).size())) {
                        out.addAll(CardFactoryUtil.getHumanCreatureAI(2, this, true));
                        CardListUtil.sortFlying(out);
                    }
                    return out;
                } // getCreature()

                @Override
                public void resolve() {
                    if (AllZoneUtil.isCardInPlay(this.getTargetCard()) && this.getTargetCard().canBeTargetedBy(this)) {

                        AllZone.getGameAction().destroy(this.getTargetCard());

                        if (!this.getTargetCard().isFaceDown()) {
                            // get all creatures
                            CardList list = AllZoneUtil.getCardsIn(Zone.Battlefield);

                            list = list.getName(this.getTargetCard().getName());
                            list.remove(this.getTargetCard());

                            if (!this.getTargetCard().isFaceDown()) {
                                for (int i = 0; i < list.size(); i++) {
                                    AllZone.getGameAction().destroy(list.get(i));
                                }
                            }
                        } // is token?
                    } // in play?
                } // resolve()
            }; // SpellAbility

            card.addSpellAbility(spell);

            final Input target = new Input() {
                private static final long serialVersionUID = -4947592326270275532L;

                @Override
                public void showMessage() {
                    final StringBuilder sb = new StringBuilder();
                    sb.append("Select target nonland permanent for ");
                    sb.append(spell.getSourceCard());
                    AllZone.getDisplay().showMessage(sb.toString());
                    ButtonUtil.enableOnlyCancel();
                }

                @Override
                public void selectButtonCancel() {
                    this.stop();
                }

                @Override
                public void selectCard(final Card card, final PlayerZone zone) {
                    if (zone.is(Constant.Zone.Battlefield) && !card.isLand()) {
                        spell.setTargetCard(card);
                        if (this.isFree()) {
                            this.setFree(false);
                            AllZone.getStack().add(spell);
                            this.stop();
                        } else {
                            this.stopSetNext(new InputPayManaCost(spell));
                        }
                    }
                }
            }; // Input

            spell.setBeforePayMana(target);
        } // *************** END ************ END ***************************

        // *************** START *********** START **************************
        else if (cardName.equals("Erratic Explosion")) {
            final Cost cost = new Cost(card.getManaCost(), cardName, false);
            final Target tgt = new Target(card, "CP");
            final SpellAbility spell = new Spell(card, cost, tgt) {
                private static final long serialVersionUID = -6003403347798646257L;

                private final int damage = 3;
                private Card check;

                @Override
                public boolean canPlayAI() {
                    if (AllZone.getHumanPlayer().getLife() <= this.damage) {
                        return true;
                    }

                    this.check = this.getFlying();
                    return this.check != null;
                }

                @Override
                public void chooseTargetAI() {
                    if (AllZone.getHumanPlayer().getLife() <= this.damage) {
                        this.setTargetPlayer(AllZone.getHumanPlayer());
                        return;
                    }

                    final Card c = this.getFlying();
                    if ((c == null) || (!this.check.equals(c))) {
                        final StringBuilder sb = new StringBuilder();
                        sb.append(card).append(" error in chooseTargetAI() - Card c is ");
                        sb.append(c).append(",  Card check is ").append(this.check);
                        throw new RuntimeException(sb.toString());
                    }

                    this.setTargetCard(c);
                } // chooseTargetAI()

                // uses "damage" variable
                Card getFlying() {
                    final CardList flying = CardFactoryUtil.getHumanCreatureAI("Flying", this, true);
                    for (int i = 0; i < flying.size(); i++) {
                        if (flying.get(i).getNetDefense() <= this.damage) {
                            return flying.get(i);
                        }
                    }

                    return null;
                }

                @Override
                public void resolve() {
                    final int damage = this.getDamage();

                    if (this.getTargetCard() != null) {
                        if (AllZoneUtil.isCardInPlay(this.getTargetCard())
                                && this.getTargetCard().canBeTargetedBy(this)) {
                            final StringBuilder sb = new StringBuilder();
                            sb.append("Erratic Explosion causes ").append(damage);
                            sb.append(" to ").append(this.getTargetCard());
                            javax.swing.JOptionPane.showMessageDialog(null, sb.toString());

                            final Card c = this.getTargetCard();
                            c.addDamage(damage, card);
                        }
                    } else {
                        final StringBuilder sb = new StringBuilder();
                        sb.append("Erratic Explosion causes ").append(damage);
                        sb.append(" to ").append(this.getTargetPlayer());
                        javax.swing.JOptionPane.showMessageDialog(null, sb.toString());
                        this.getTargetPlayer().addDamage(damage, card);
                    }
                }

                // randomly choose a nonland card
                int getDamage() {
                    CardList notLand = card.getController().getCardsIn(Zone.Library);
                    notLand = notLand.filter(CardListFilter.NON_LANDS);
                    notLand.shuffle();

                    if (notLand.isEmpty()) {
                        return 0;
                    }

                    final Card card = notLand.get(0);
                    return CardUtil.getConvertedManaCost(card.getSpellAbility()[0]);
                }
            }; // SpellAbility
            card.addSpellAbility(spell);

            card.setSVar("PlayMain1", "TRUE");
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Martial Coup")) {

            final Cost cost = new Cost(card.getManaCost(), cardName, false);
            final SpellAbility spell = new Spell(card, cost, null) {

                private static final long serialVersionUID = -29101524966207L;

                @Override
                public void resolve() {
                    final CardList all = AllZoneUtil.getCardsIn(Zone.Battlefield);
                    final int soldiers = card.getXManaCostPaid();
                    for (int i = 0; i < soldiers; i++) {
                        CardFactoryUtil.makeToken("Soldier", "W 1 1 Soldier", card.getController(), "W", new String[] {
                                "Creature", "Soldier" }, 1, 1, new String[] { "" });
                    }
                    if (soldiers >= 5) {
                        for (int i = 0; i < all.size(); i++) {
                            final Card c = all.get(i);
                            if (c.isCreature()) {
                                AllZone.getGameAction().destroy(c);
                            }
                        }
                    }
                } // resolve()

                @Override
                public boolean canPlayAI() {
                    final CardList human = AllZoneUtil.getCreaturesInPlay(AllZone.getHumanPlayer());
                    final CardList computer = AllZoneUtil.getCreaturesInPlay(AllZone.getComputerPlayer());

                    // the computer will at least destroy 2 more human creatures
                    return ((computer.size() < (human.size() - 1)) || ((AllZone.getComputerPlayer().getLife() < 7) && !human
                            .isEmpty())) && (ComputerUtil.getAvailableMana().size() >= 7);
                }
            }; // SpellAbility

            card.addSpellAbility(spell);
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Parallel Evolution")) {
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = 3456160935845779623L;

                @Override
                public boolean canPlayAI() {
                    CardList humTokenCreats = AllZoneUtil.getCreaturesInPlay(AllZone.getHumanPlayer());
                    humTokenCreats = humTokenCreats.filter(CardListFilter.TOKEN);

                    CardList compTokenCreats = AllZoneUtil.getCreaturesInPlay(AllZone.getComputerPlayer());
                    compTokenCreats = compTokenCreats.filter(CardListFilter.TOKEN);

                    return compTokenCreats.size() > humTokenCreats.size();
                } // canPlayAI()

                @Override
                public void resolve() {
                    CardList tokens = AllZoneUtil.getCreaturesInPlay();
                    tokens = tokens.filter(CardListFilter.TOKEN);

                    CardFactoryUtil.copyTokens(tokens);

                } // resolve()
            }; // SpellAbility

            final StringBuilder sbDesc = new StringBuilder();
            sbDesc.append("For each creature token on the battlefield, ");
            sbDesc.append("its controller puts a token that's a copy of that creature onto the battlefield.");
            spell.setDescription(sbDesc.toString());

            final StringBuilder sbStack = new StringBuilder();
            sbStack.append("Parallel Evolution - For each creature ");
            sbStack.append("token on the battlefield, its controller puts a token that's a copy of ");
            sbStack.append("that creature onto the battlefield.");
            spell.setStackDescription(sbStack.toString());

            card.addSpellAbility(spell);
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Global Ruin")) {
            final CardList target = new CardList();
            final CardList saveList = new CardList();
            // need to use arrays so we can declare them final and still set the
            // values in the input and runtime classes. This is a hack.
            final int[] index = new int[1];
            final int[] countBase = new int[1];
            final Vector<String> humanBasic = new Vector<String>();

            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = 5739127258598357186L;

                @Override
                public boolean canPlayAI() {
                    return false;
                    // should check if computer has land in hand, or if computer
                    // has more basic land types than human.
                }

                @Override
                public void resolve() {
                    // add computer's lands to target

                    // int computerCountBase = 0;
                    // Vector<?> computerBasic = new Vector();

                    // figure out which basic land types the computer has
                    CardList land = AllZoneUtil.getPlayerLandsInPlay(AllZone.getComputerPlayer());
                    final String[] basic = { "Forest", "Plains", "Mountain", "Island", "Swamp" };

                    for (final String element : basic) {
                        final CardList cl = land.getType(element);
                        if (!cl.isEmpty()) {
                            // remove one land of this basic type from this list
                            // the computer AI should really jump in here and
                            // select the land which is the best.
                            // to determine the best look at which lands have
                            // enchantments, which lands are tapped
                            cl.remove(cl.get(0));
                            // add the rest of the lands of this basic type to
                            // the target list, this is the list which will be
                            // sacrificed.
                            target.addAll(cl);
                        }
                    }

                    // need to sacrifice the other non-basic land types
                    land = land.filter(new CardListFilter() {
                        @Override
                        public boolean addCard(final Card c) {
                            if (c.getName().contains("Dryad Arbor")) {
                                return true;
                            } else if (!(c.isType("Forest") || c.isType("Plains") || c.isType("Mountain")
                                    || c.isType("Island") || c.isType("Swamp"))) {
                                return true;
                            } else {
                                return false;
                            }
                        }
                    });
                    target.addAll(land);

                    // when this spell resolves all basic lands which were not
                    // selected are sacrificed.
                    for (int i = 0; i < target.size(); i++) {
                        if (AllZoneUtil.isCardInPlay(target.get(i)) && !saveList.contains(target.get(i))) {
                            AllZone.getGameAction().sacrifice(target.get(i));
                        }
                    }
                } // resolve()
            }; // SpellAbility

            final Input input = new Input() {
                private static final long serialVersionUID = 1739423591445361917L;
                private int count;

                @Override
                public void showMessage() { // count is the current index we are
                                            // on.
                    // countBase[0] is the total number of basic land types the
                    // human has
                    // index[0] is the number to offset the index by
                    this.count = countBase[0] - index[0] - 1; // subtract by one
                    // since humanBasic is
                    // 0 indexed.
                    if (this.count < 0) {
                        // need to reset the variables in case they cancel this
                        // spell and it stays in hand.
                        humanBasic.clear();
                        countBase[0] = 0;
                        index[0] = 0;
                        this.stop();
                    } else {
                        final StringBuilder sb = new StringBuilder();
                        sb.append("Select target ").append(humanBasic.get(this.count));
                        sb.append(" land to not sacrifice");
                        AllZone.getDisplay().showMessage(sb.toString());
                        ButtonUtil.enableOnlyCancel();
                    }
                }

                @Override
                public void selectButtonCancel() {
                    this.stop();
                }

                @Override
                public void selectCard(final Card c, final PlayerZone zone) {
                    if (c.isLand() && zone.is(Constant.Zone.Battlefield) && c.getController().isHuman()
                    /* && c.getName().equals(humanBasic.get(count)) */
                    && c.isType(humanBasic.get(this.count))
                    /* && !saveList.contains(c) */) {
                        // get all other basic[count] lands human player
                        // controls and add them to target
                        CardList land = AllZoneUtil.getPlayerLandsInPlay(AllZone.getHumanPlayer());
                        CardList cl = land.getType(humanBasic.get(this.count));
                        cl = cl.filter(new CardListFilter() {
                            @Override
                            public boolean addCard(final Card crd) {
                                return !saveList.contains(crd);
                            }
                        });

                        if (!c.getName().contains("Dryad Arbor")) {
                            cl.remove(c);
                            saveList.add(c);
                        }
                        target.addAll(cl);

                        index[0]++;
                        this.showMessage();

                        if (index[0] >= humanBasic.size()) {
                            this.stopSetNext(new InputPayManaCost(spell));
                        }

                        // need to sacrifice the other non-basic land types
                        land = land.filter(new CardListFilter() {
                            @Override
                            public boolean addCard(final Card c) {
                                if (c.getName().contains("Dryad Arbor")) {
                                    return true;
                                } else if (!(c.isType("Forest") || c.isType("Plains") || c.isType("Mountain")
                                        || c.isType("Island") || c.isType("Swamp"))) {
                                    return true;
                                } else {
                                    return false;
                                }
                            }
                        });
                        target.addAll(land);

                    }
                } // selectCard()
            }; // Input

            final Input runtime = new Input() {
                private static final long serialVersionUID = -122635387376995855L;

                @Override
                public void showMessage() {
                    countBase[0] = 0;
                    // figure out which basic land types the human has
                    // put those in an set to use later
                    final CardList land = AllZone.getHumanPlayer().getCardsIn(Zone.Battlefield);
                    final String[] basic = { "Forest", "Plains", "Mountain", "Island", "Swamp" };

                    for (final String element : basic) {
                        final CardList c = land.getType(element);
                        if (!c.isEmpty()) {
                            humanBasic.add(element);
                            countBase[0]++;
                        }
                    }
                    if (countBase[0] == 0) {
                        // human has no basic land, so don't prompt to select
                        // one.
                        this.stop();
                    } else {
                        index[0] = 0;
                        target.clear();
                        this.stopSetNext(input);
                    }
                }
            }; // Input
            card.addSpellAbility(spell);
            spell.setBeforePayMana(runtime);
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Mind Funeral")) {
            final Cost cost = new Cost("1 U B", cardName, false);
            final Target tgt = new Target(card, "Select a Player", "Player");
            final SpellAbility spell = new Spell(card, cost, tgt) {
                private static final long serialVersionUID = 42470566751344693L;

                @Override
                public boolean canPlayAI() {
                    this.setTargetPlayer(AllZone.getHumanPlayer());
                    final CardList libList = AllZone.getHumanPlayer().getCardsIn(Zone.Library);
                    return libList.size() > 0;
                }

                @Override
                public void resolve() {
                    final Player player = this.getTargetPlayer();

                    final CardList libList = player.getCardsIn(Zone.Library);

                    final int numLands = libList.getType("Land").size();

                    int total = 0;
                    if (numLands > 3) { // if only 3 or less lands in the deck
                                        // everything is going
                        int landCount = 0;

                        for (final Card c : libList) {
                            total++;
                            if (c.isLand()) {
                                landCount++;
                                if (landCount == 4) {
                                    break;
                                }
                            }
                        }
                    } else {
                        total = libList.size();
                    }
                    player.mill(total);
                }
            }; // SpellAbility
            card.addSpellAbility(spell);
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Haunting Echoes")) {
            final Cost cost = new Cost("3 B B", cardName, false);
            final Target tgt = new Target(card, "Select a Player", "Player");
            final SpellAbility spell = new Spell(card, cost, tgt) {
                private static final long serialVersionUID = 42470566751344693L;

                @Override
                public boolean canPlayAI() {
                    // Haunting Echoes shouldn't be cast if only basic land in
                    // graveyard or library is empty
                    CardList graveyard = AllZone.getHumanPlayer().getCardsIn(Zone.Graveyard);
                    final CardList library = AllZone.getHumanPlayer().getCardsIn(Zone.Library);
                    final int graveCount = graveyard.size();
                    graveyard = graveyard.filter(new CardListFilter() {
                        @Override
                        public boolean addCard(final Card c) {
                            return c.isBasicLand();
                        }
                    });

                    this.setTargetPlayer(AllZone.getHumanPlayer());

                    return (((graveCount - graveyard.size()) > 0) && (library.size() > 0));
                }

                @Override
                public void resolve() {
                    final Player player = this.getTargetPlayer();

                    CardList grave = player.getCardsIn(Zone.Graveyard);
                    grave = grave.getNotType("Basic");

                    final CardList lib = player.getCardsIn(Zone.Library);

                    for (final Card c : grave) {
                        final CardList remLib = lib.getName(c.getName());
                        for (final Card rem : remLib) {
                            AllZone.getGameAction().exile(rem);
                            lib.remove(rem);
                        }
                        AllZone.getGameAction().exile(c);
                    }
                }
            }; // SpellAbility
            card.addSpellAbility(spell);
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Donate")) {
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = 782912579034503349L;

                @Override
                public void resolve() {
                    final Card c = this.getTargetCard();

                    if ((c != null) && AllZoneUtil.isCardInPlay(c) && c.canBeTargetedBy(this)) {
                        // Donate should target both the player and the creature
                        c.addController(card.getController().getOpponent());
                        /*
                         * if (!c.isAura()) {
                         * 
                         * //AllZone.getGameAction().changeController(new
                         * CardList(c), c.getController(),
                         * c.getController().getOpponent());
                         * 
                         * } else //Aura {
                         * c.setController(card.getController().getOpponent());
                         * }
                         */
                    }
                }

                @Override
                public boolean canPlayAI() {
                    final CardList list = AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield,
                            "Illusions of Grandeur");

                    if (list.size() > 0) {
                        this.setTargetCard(list.get(0));
                        return true;
                    }
                    return false;
                }
            };

            final Input runtime = new Input() {
                private static final long serialVersionUID = -7823269301012427007L;

                @Override
                public void showMessage() {
                    CardList perms = AllZone.getHumanPlayer().getCardsIn(Zone.Battlefield);
                    perms = perms.filter(new CardListFilter() {
                        @Override
                        public boolean addCard(final Card c) {
                            return c.isPermanent() && !c.getName().equals("Mana Pool");
                        }
                    });

                    boolean free = false;
                    if (this.isFree()) {
                        free = true;
                    }

                    this.stopSetNext(CardFactoryUtil.inputTargetSpecific(spell, perms,
                            "Select a permanent you control", true, free));

                } // showMessage()
            }; // Input

            spell.setBeforePayMana(runtime);
            card.addSpellAbility(spell);
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Balance")) {
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = -5941893280103164961L;

                @Override
                public void resolve() {
                    // Lands:
                    final CardList humLand = AllZoneUtil.getPlayerLandsInPlay(AllZone.getHumanPlayer());
                    final CardList compLand = AllZoneUtil.getPlayerLandsInPlay(AllZone.getComputerPlayer());

                    if (compLand.size() > humLand.size()) {
                        compLand.shuffle();
                        for (int i = 0; i < (compLand.size() - humLand.size()); i++) {
                            AllZone.getGameAction().sacrifice(compLand.get(i));
                        }
                    } else if (humLand.size() > compLand.size()) {
                        final int diff = humLand.size() - compLand.size();
                        AllZone.getInputControl().setInput(PlayerUtil.inputSacrificePermanents(diff, "Land"));
                    }

                    // Hand
                    final CardList humHand = AllZone.getHumanPlayer().getCardsIn(Zone.Hand);
                    final CardList compHand = AllZone.getComputerPlayer().getCardsIn(Zone.Hand);
                    final int handDiff = Math.abs(humHand.size() - compHand.size());

                    if (compHand.size() > humHand.size()) {
                        AllZone.getComputerPlayer().discard(handDiff, this, false);
                    } else if (humHand.size() > compHand.size()) {
                        AllZone.getHumanPlayer().discard(handDiff, this, false);
                    }

                    // Creatures:
                    final CardList humCreats = AllZoneUtil.getCreaturesInPlay(AllZone.getHumanPlayer());
                    final CardList compCreats = AllZoneUtil.getCreaturesInPlay(AllZone.getComputerPlayer());

                    if (compCreats.size() > humCreats.size()) {
                        CardListUtil.sortAttackLowFirst(compCreats);
                        CardListUtil.sortCMC(compCreats);
                        compCreats.reverse();
                        for (int i = 0; i < (compCreats.size() - humCreats.size()); i++) {
                            AllZone.getGameAction().sacrifice(compCreats.get(i));
                        }
                    } else if (humCreats.size() > compCreats.size()) {
                        final int diff = humCreats.size() - compCreats.size();
                        AllZone.getInputControl().setInput(PlayerUtil.inputSacrificePermanents(diff, "Creature"));
                    }
                }

                @Override
                public boolean canPlayAI() {
                    int diff = 0;
                    final CardList humLand = AllZoneUtil.getPlayerLandsInPlay(AllZone.getHumanPlayer());
                    final CardList compLand = AllZoneUtil.getPlayerLandsInPlay(AllZone.getComputerPlayer());
                    diff += humLand.size() - compLand.size();

                    final CardList humCreats = AllZoneUtil.getCreaturesInPlay(AllZone.getHumanPlayer());
                    CardList compCreats = AllZoneUtil.getCreaturesInPlay(AllZone.getComputerPlayer());
                    compCreats = compCreats.getType("Creature");
                    diff += 1.5 * (humCreats.size() - compCreats.size());

                    final CardList humHand = AllZone.getHumanPlayer().getCardsIn(Zone.Hand);
                    final CardList compHand = AllZone.getComputerPlayer().getCardsIn(Zone.Hand);
                    diff += 0.5 * (humHand.size() - compHand.size());

                    return diff > 2;
                }
            };
            card.addSpellAbility(spell);
        }
        // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Summer Bloom")) {
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = 5559004016728325736L;

                @Override
                public boolean canPlayAI() {
                    // The computer should only play this card if it has at
                    // least
                    // one land in its hand. Because of the way the computer
                    // turn
                    // is structured, it will already have played land to it's
                    // limit

                    CardList hand = AllZone.getComputerPlayer().getCardsIn(Zone.Hand);
                    hand = hand.getType("Land");
                    return hand.size() > 0;
                }

                @Override
                public void resolve() {
                    final Player thePlayer = card.getController();
                    thePlayer.addMaxLandsToPlay(3);

                    final Command untilEOT = new Command() {
                        private static final long serialVersionUID = 1665720009691293263L;

                        @Override
                        public void execute() {
                            thePlayer.addMaxLandsToPlay(-3);
                        }
                    };
                    AllZone.getEndOfTurn().addUntil(untilEOT);
                }
            };
            card.addSpellAbility(spell);

            card.setSVar("PlayMain1", "TRUE");
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Explore")) {
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = 8377957584738695517L;

                @Override
                public boolean canPlayAI() {
                    // The computer should only play this card if it has at
                    // least
                    // one land in its hand. Because of the way the computer
                    // turn
                    // is structured, it will already have played its first
                    // land.
                    CardList list = AllZone.getComputerPlayer().getCardsIn(Zone.Hand);

                    list = list.getType("Land");
                    if (list.size() > 0) {
                        return true;
                    } else {
                        return false;
                    }
                }

                @Override
                public void resolve() {
                    final Player thePlayer = card.getController();
                    thePlayer.addMaxLandsToPlay(1);

                    final Command untilEOT = new Command() {
                        private static final long serialVersionUID = -2618916698575607634L;

                        @Override
                        public void execute() {
                            thePlayer.addMaxLandsToPlay(-1);
                        }
                    };
                    AllZone.getEndOfTurn().addUntil(untilEOT);

                    thePlayer.drawCard();
                }
            };
            card.addSpellAbility(spell);

            card.setSVar("PlayMain1", "TRUE");
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Explosive Revelation")) {
            /*
             * Choose target creature or player. Reveal cards from the top of
             * your library until you reveal a nonland card. Explosive
             * Revelation deals damage equal to that card's converted mana cost
             * to that creature or player. Put the nonland card into your hand
             * and the rest on the bottom of your library in any order.
             */
            final Cost cost = new Cost(card.getManaCost(), cardName, false);
            final Target tgt = new Target(card, "CP");
            final SpellAbility spell = new Spell(card, cost, tgt) {
                private static final long serialVersionUID = -3234630801871872940L;

                private final int damage = 3;
                private Card check;

                @Override
                public boolean canPlayAI() {
                    if (AllZone.getHumanPlayer().getLife() <= this.damage) {
                        return true;
                    }

                    this.check = this.getFlying();
                    return this.check != null;
                }

                @Override
                public void chooseTargetAI() {
                    if (AllZone.getHumanPlayer().getLife() <= this.damage) {
                        this.setTargetPlayer(AllZone.getHumanPlayer());
                        return;
                    }

                    final Card c = this.getFlying();
                    if ((c == null) || (!this.check.equals(c))) {
                        final StringBuilder sb = new StringBuilder();
                        sb.append(card).append(" error in chooseTargetAI() - Card c is ");
                        sb.append(c).append(",  Card check is ").append(this.check);
                        throw new RuntimeException(sb.toString());
                    }

                    this.setTargetCard(c);
                } // chooseTargetAI()

                // uses "damage" variable
                Card getFlying() {
                    final CardList flying = CardFactoryUtil.getHumanCreatureAI("Flying", this, true);
                    for (int i = 0; i < flying.size(); i++) {
                        if (flying.get(i).getNetDefense() <= this.damage) {
                            return flying.get(i);
                        }
                    }

                    return null;
                }

                @Override
                public void resolve() {

                    final int damage = this.getDamage();

                    if (this.getTargetCard() != null) {
                        if (AllZoneUtil.isCardInPlay(this.getTargetCard())
                                && this.getTargetCard().canBeTargetedBy(this)) {
                            final StringBuilder sb = new StringBuilder();
                            sb.append(cardName).append(" causes ").append(damage);
                            sb.append(" to ").append(this.getTargetCard());
                            javax.swing.JOptionPane.showMessageDialog(null, sb.toString());

                            final Card c = this.getTargetCard();
                            c.addDamage(damage, card);
                        }
                    } else {
                        final StringBuilder sb = new StringBuilder();
                        sb.append(cardName).append(" causes ").append(damage);
                        sb.append(" to ").append(this.getTargetPlayer());
                        javax.swing.JOptionPane.showMessageDialog(null, sb.toString());

                        this.getTargetPlayer().addDamage(damage, card);
                    }
                    // System.out.println("Library after: "+card.getController()).getCardsIn(Zone.Library);
                }

                int getDamage() {
                    /*
                     * Reveal cards from the top of your library until you
                     * reveal a nonland card.
                     */
                    final CardList lib = card.getController().getCardsIn(Zone.Library);
                    final StringBuilder sb = new StringBuilder();
                    sb.append("Library before: ").append(lib);
                    Log.debug("Explosive Revelation", sb.toString());

                    final CardList revealed = new CardList();
                    if (lib.size() > 0) {
                        int index = 0;
                        Card top;
                        do {
                            top = lib.get(index);
                            // System.out.println("Got from top of library:"+top);
                            index += 1;
                            revealed.add(top);
                        } while ((index < lib.size()) && top.isLand());
                        // Display the revealed cards
                        GuiUtils.getChoice("Revealed cards:", revealed.toArray());
                        // non-land card into hand
                        AllZone.getGameAction().moveToHand(revealed.get(revealed.size() - 1));
                        // put the rest of the cards on the bottom of library
                        for (int j = 0; j < (revealed.size() - 1); j++) {
                            AllZone.getGameAction().moveToBottomOfLibrary(revealed.get(j));
                        }
                        // return the damage

                        // System.out.println("Explosive Revelation does "
                        // +CardUtil.getConvertedManaCost(top)+" from: "+top);
                        return CardUtil.getConvertedManaCost(top);
                    }
                    return 0;
                }
            }; // SpellAbility
            card.addSpellAbility(spell);
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Fireball")) {
            /*
             * Fireball deals X damage divided evenly, rounded down, among any
             * number of target creatures and/or players. Fireball costs 1 more
             * to cast for each target beyond the first.
             */
            final CardList targets = new CardList();
            final ArrayList<Player> targetPlayers = new ArrayList<Player>();

            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = -6293612568525319357L;

                @Override
                public boolean canPlayAI() {
                    final int maxX = ComputerUtil.getAvailableMana().size() - 1;
                    final int humanLife = AllZone.getHumanPlayer().getLife();
                    if (maxX >= humanLife) {
                        targetPlayers.add(AllZone.getHumanPlayer());
                        return true;
                    }
                    return false;
                }

                @Override
                public void resolve() {
                    final int damage = ((card.getXManaCostPaid() - this.getNumTargets()) + 1) / this.getNumTargets();
                    // add that much damage to each creature
                    // DEBUG
                    final StringBuilder sbDmg = new StringBuilder();
                    sbDmg.append("Fireball - damage to each target: ").append(damage);
                    Log.debug("Fireball", sbDmg.toString());

                    Log.debug("Fireball", "Fireball - card targets: ");
                    this.printCardTargets();
                    Log.debug("Fireball", "Fireball - player targets: ");
                    this.printPlayerTargets();
                    if (card.getController().isComputer()) {
                        final StringBuilder sb = new StringBuilder();
                        sb.append(cardName).append(" - Computer causes ");
                        sb.append(damage).append(" to:\n\n");
                        for (int i = 0; i < targets.size(); i++) {
                            final Card target = targets.get(i);
                            if (AllZoneUtil.isCardInPlay(target) && target.canBeTargetedBy(this)) {
                                sb.append(target).append("\n");
                            }
                        }
                        for (int i = 0; i < targetPlayers.size(); i++) {
                            final Player p = targetPlayers.get(i);
                            if (p.canBeTargetedBy(this)) {
                                sb.append(p).append("\n");
                            }
                        }
                        javax.swing.JOptionPane.showMessageDialog(null, sb.toString());
                    }
                    for (int i = 0; i < targets.size(); i++) {
                        final Card target = targets.get(i);
                        if (AllZoneUtil.isCardInPlay(target) && target.canBeTargetedBy(this)) {
                            // DEBUG
                            final StringBuilder sb = new StringBuilder();
                            sb.append("Fireball does ").append(damage);
                            sb.append(" to: ").append(target);
                            Log.debug("Fireball", sb.toString());

                            target.addDamage(damage, card);
                        }
                    }
                    for (int i = 0; i < targetPlayers.size(); i++) {
                        final Player p = targetPlayers.get(i);
                        if (p.canBeTargetedBy(this)) {
                            // DEBUG
                            final StringBuilder sb = new StringBuilder();
                            sb.append("Fireball does ").append(damage);
                            sb.append(" to: ").append(p);
                            Log.debug("Fireball", sb.toString());

                            p.addDamage(damage, card);
                        }
                    }
                } // resolve()

                // DEBUG
                private void printCardTargets() {
                    final StringBuilder sb = new StringBuilder("[");
                    for (final Card target : targets) {
                        sb.append(target).append(",");
                    }
                    sb.append("]");
                    Log.debug("Fireball", sb.toString());
                }

                // DEBUG
                private void printPlayerTargets() {
                    final StringBuilder sb = new StringBuilder("[");
                    for (final Player p : targetPlayers) {
                        sb.append(p).append(",");
                    }
                    sb.append("]");
                    Log.debug("Fireball", sb.toString());
                }

                private int getNumTargets() {
                    int numTargets = 0;
                    numTargets += targets.size();
                    numTargets += targetPlayers.size();
                    return numTargets;
                }

            }; // SpellAbility

            final Input input = new Input() {
                private static final long serialVersionUID = 1099272655273322957L;

                @Override
                public void showMessage() {
                    final StringBuilder sb = new StringBuilder();
                    sb.append("Select target creatures and/or players.  Currently, ");
                    sb.append(this.getNumTargets()).append(" targets.  Click OK when done.");
                    AllZone.getDisplay().showMessage(sb.toString());
                }

                private int getNumTargets() {
                    int numTargets = 0;
                    numTargets += targets.size();
                    numTargets += targetPlayers.size();
                    // DEBUG
                    final StringBuilder sb = new StringBuilder();
                    sb.append("Fireball - numTargets = ").append(numTargets);
                    Log.debug("Fireball", sb.toString());

                    return numTargets;
                }

                @Override
                public void selectButtonCancel() {
                    targets.clear();
                    targetPlayers.clear();
                    this.stop();
                }

                @Override
                public void selectButtonOK() {
                    final StringBuilder sb = new StringBuilder();
                    sb.append(cardName).append(" deals X damage to ");
                    sb.append(this.getNumTargets()).append(" target(s).");
                    spell.setStackDescription(sb.toString());
                    this.stopSetNext(new InputPayManaCost(spell));
                }

                @Override
                public void selectCard(final Card c, final PlayerZone zone) {
                    if (!c.canBeTargetedBy(spell)) {
                        AllZone.getDisplay().showMessage("Cannot target this card.");
                        return; // cannot target
                    }
                    if (targets.contains(c)) {
                        AllZone.getDisplay().showMessage("You have already selected this target.");
                        return; // cannot target the same creature twice.
                    }

                    if (c.isCreature() && zone.is(Constant.Zone.Battlefield)) {
                        targets.add(c);
                        this.showMessage();
                    }
                } // selectCard()

                @Override
                public void selectPlayer(final Player player) {
                    if (!player.canBeTargetedBy(spell)) {
                        AllZone.getDisplay().showMessage("Cannot target this player.");
                        return; // cannot target
                    }
                    if (targetPlayers.contains(player)) {
                        AllZone.getDisplay().showMessage("You have already selected this player.");
                        return; // cannot target the same player twice.
                    }
                    targetPlayers.add(player);
                    this.showMessage();
                }
            }; // Input

            card.addSpellAbility(spell);
            spell.setBeforePayMana(input);
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Recall")) {
            /*
             * Discard X cards, then return a card from your graveyard to your
             * hand for each card discarded this way. Exile Recall.
             */
            final Cost cost = new Cost(card.getManaCost(), cardName, false);
            final SpellAbility spell = new Spell(card, cost, null) {
                private static final long serialVersionUID = -3935814273439962834L;

                @Override
                public boolean canPlayAI() {
                    // for compy to play this wisely, it should check hand, and if there
                    // are no spells that canPlayAI(), then use recall. maybe.
                    return false;
                }

                @Override
                public void resolve() {
                    int numCards = card.getXManaCostPaid();
                    final Player player = card.getController();
                    final int maxCards = player.getCardsIn(Zone.Hand).size();
                    if (numCards != 0) {
                        numCards = Math.min(numCards, maxCards);
                        if (player.isHuman()) {
                            AllZone.getInputControl()
                                    .setInput(CardFactoryUtil.inputDiscardRecall(numCards, card, this));
                        }
                    }
                    /*
                     * else { //computer
                     * card.getControler().discardRandom(numCards);
                     * AllZone.getGameAction().exile(card); CardList grave =
                     * AllZoneUtil.getPlayerGraveyard(card.getController());
                     * for(int i = 1; i <= numCards; i ++) { Card t1 =
                     * CardFactoryUtil.AI_getBestCreature(grave); if(null != t1)
                     * { t1 = grave.get(0); grave.remove(t1);
                     * AllZone.getGameAction().moveToHand(t1); } } }
                     */
                } // resolve()
            }; // SpellAbility

            final StringBuilder sb = new StringBuilder();
            sb.append(card).append(" - discard X cards and return X cards to your hand.");
            spell.setStackDescription(sb.toString());

            card.addSpellAbility(spell);
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Windfall")) {
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = -7707012960887790709L;

                @Override
                public boolean canPlayAI() {
                    /*
                     * We want compy to have less cards in hand than the human
                     */
                    final CardList humanHand = AllZone.getHumanPlayer().getCardsIn(Zone.Hand);
                    final CardList computerHand = AllZone.getComputerPlayer().getCardsIn(Zone.Hand);
                    return computerHand.size() < humanHand.size();
                }

                @Override
                public void resolve() {
                    final CardList humanHand = AllZone.getHumanPlayer().getCardsIn(Zone.Hand);
                    final CardList computerHand = AllZone.getComputerPlayer().getCardsIn(Zone.Hand);

                    final int num = Math.max(humanHand.size(), computerHand.size());

                    this.discardDraw(AllZone.getHumanPlayer(), num);
                    this.discardDraw(AllZone.getComputerPlayer(), num);
                } // resolve()

                void discardDraw(final Player player, final int num) {
                    player.discardHand(this);
                    player.drawCards(num);
                }
            }; // SpellAbility
            card.addSpellAbility(spell);
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Patriarch's Bidding")) {
            final String[] input = new String[2];

            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = -2182173662547136798L;

                @Override
                public void resolve() {
                    input[0] = "";
                    while (input[0] == "") {
                        input[0] = JOptionPane.showInputDialog(null, "Which creature type?", "Pick type",
                                JOptionPane.QUESTION_MESSAGE);
                        if (input[0] == null) {
                            break;
                        }
                        if (!CardUtil.isACreatureType(input[0])) {
                            input[0] = "";
                            // TODO some more input validation,
                            // case-sensitivity,
                            // etc.
                        }

                        input[0] = input[0].trim(); // this is to prevent
                                                    // "cheating", and selecting
                                                    // multiple creature
                                                    // types,eg "Goblin Soldier"
                    }

                    if (input[0] == null) {
                        input[0] = "";
                    }

                    final HashMap<String, Integer> countInGraveyard = new HashMap<String, Integer>();
                    final CardList allGrave = AllZone.getComputerPlayer().getCardsIn(Constant.Zone.Graveyard);
                    allGrave.getType("Creature");
                    for (final Card c : allGrave) {
                        for (final String type : c.getType()) {
                            if (CardUtil.isACreatureType(type)) {
                                if (countInGraveyard.containsKey(type)) {
                                    countInGraveyard.put(type, countInGraveyard.get(type) + 1);
                                } else {
                                    countInGraveyard.put(type, 1);
                                }
                            }
                        }
                    }
                    String maxKey = "";
                    int maxCount = -1;
                    for (final Entry<String, Integer> entry : countInGraveyard.entrySet()) {
                        if (entry.getValue() > maxCount) {
                            maxKey = entry.getKey();
                            maxCount = entry.getValue();
                        }
                    }
                    if (!maxKey.equals("")) {
                        input[1] = maxKey;
                    } else {
                        input[1] = "Sliver";
                    }

                    // Actually put everything on the battlefield
                    CardList bidded = AllZoneUtil.getCardsIn(Constant.Zone.Graveyard);
                    bidded = bidded.getType("Creature");
                    for (final Card c : bidded) {
                        if (c.isType(input[1]) || (!input[0].equals("") && c.isType(input[0]))) {
                            AllZone.getGameAction().moveToPlay(c);
                        }
                    }
                } // resolve()
            }; // SpellAbility
            card.addSpellAbility(spell);

            final StringBuilder sb = new StringBuilder();
            sb.append(card.getName()).append(" - choose a creature type.");
            spell.setStackDescription(sb.toString());
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Leeches")) {
            /*
             * Target player loses all poison counters. Leeches deals that much
             * damage to that player.
             */
            final Target tgt = new Target(card, "Select target player", "Player");
            final Cost cost = new Cost("1 W W", cardName, false);
            final SpellAbility spell = new Spell(card, cost, tgt) {
                private static final long serialVersionUID = 8555498267738686288L;

                @Override
                public void resolve() {
                    final Player p = tgt.getTargetPlayers().get(0);
                    final int counters = p.getPoisonCounters();
                    p.addDamage(counters, card);
                    p.subtractPoisonCounters(counters);
                } // resolve()

                @Override
                public boolean canPlayAI() {
                    final int humanPoison = AllZone.getHumanPlayer().getPoisonCounters();
                    final int compPoison = AllZone.getComputerPlayer().getPoisonCounters();

                    if (AllZone.getHumanPlayer().getLife() <= humanPoison) {
                        tgt.addTarget(AllZone.getHumanPlayer());
                        return true;
                    }

                    if ((((2 * (11 - compPoison)) < AllZone.getComputerPlayer().getLife()) || (compPoison > 7))
                            && (compPoison < (AllZone.getComputerPlayer().getLife() - 2))) {
                        tgt.addTarget(AllZone.getComputerPlayer());
                        return true;
                    }

                    return false;
                }
            }; // SpellAbility
            card.addSpellAbility(spell);
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Sanity Grinding")) {
            /*
             * Chroma - Reveal the top ten cards of your library. For each blue
             * mana symbol in the mana costs of the revealed cards, target
             * opponent puts the top card of his or her library into his or her
             * graveyard. Then put the cards you revealed this way on the bottom
             * of your library in any order.
             */
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = 4475834103787262421L;

                @Override
                public void resolve() {
                    final Player player = card.getController();
                    final Player opp = player.getOpponent();
                    final PlayerZone lib = card.getController().getZone(Constant.Zone.Library);
                    int maxCards = lib.size();
                    maxCards = Math.min(maxCards, 10);
                    if (maxCards == 0) {
                        return;
                    }
                    final CardList topCards = new CardList();
                    // show top n cards:
                    for (int j = 0; j < maxCards; j++) {
                        topCards.add(lib.get(j));
                    }
                    final int num = CardFactoryUtil.getNumberOfManaSymbolsByColor("U", topCards);
                    final StringBuilder sb = new StringBuilder();
                    sb.append("Revealed cards - ").append(num).append(" U mana symbols");
                    GuiUtils.getChoiceOptional(sb.toString(), topCards.toArray());

                    // opponent moves this many cards to graveyard
                    opp.mill(num);

                    // then, move revealed cards to bottom of library
                    for (final Card c : topCards) {
                        AllZone.getGameAction().moveToBottomOfLibrary(c);
                    }
                } // resolve()

                @Override
                public boolean canPlayAI() {
                    return !AllZone.getComputerPlayer().getZone(Zone.Library).isEmpty();
                }

            }; // SpellAbility
            card.addSpellAbility(spell);
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Winds of Change")) {
            /*
             * Each player shuffles the cards from his or her hand into his or
             * her library, then draws that many cards.
             */
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = 1137557863607126794L;

                @Override
                public void resolve() {
                    this.discardDrawX(AllZone.getHumanPlayer());
                    this.discardDrawX(AllZone.getComputerPlayer());
                } // resolve()

                void discardDrawX(final Player player) {
                    final CardList hand = player.getCardsIn(Zone.Hand);

                    for (final Card c : hand) {
                        AllZone.getGameAction().moveToLibrary(c);
                    }

                    // Shuffle library
                    player.shuffle();

                    player.drawCards(hand.size());
                }

                // Simple, If computer has two or less playable cards remaining
                // in hand play Winds of Change
                @Override
                public boolean canPlayAI() {
                    CardList c = AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield);
                    c = c.filter(CardListFilter.NON_LANDS);
                    return 2 >= c.size();
                }

            }; // SpellAbility
            card.addSpellAbility(spell);
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Molten Psyche")) {
            /*
             * Each player shuffles the cards from his or her hand into his or
             * her library, then draws that many cards. Metalcraft - If you
             * control three or more artifacts, Molten Psyche deals damage to
             * each opponent equal to the number of cards that player has drawn
             * this turn.
             */
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = -1276674329039279896L;

                @Override
                public void resolve() {
                    final Player player = card.getController();
                    final Player opp = player.getOpponent();
                    this.discardDraw(AllZone.getHumanPlayer());
                    this.discardDraw(AllZone.getComputerPlayer());

                    if (player.hasMetalcraft()) {
                        opp.addDamage(opp.getNumDrawnThisTurn(), card);
                    }
                } // resolve()

                void discardDraw(final Player player) {
                    final CardList hand = player.getCardsIn(Zone.Hand);
                    final int numDraw = hand.size();

                    // move hand to library
                    for (final Card c : hand) {
                        AllZone.getGameAction().moveToLibrary(c);
                    }

                    // Shuffle library
                    player.shuffle();

                    // Draw X cards
                    player.drawCards(numDraw);
                }

                // Simple, If computer has two or less playable cards remaining
                // in hand play CARDNAME
                @Override
                public boolean canPlayAI() {
                    CardList c = AllZone.getComputerPlayer().getCardsIn(Zone.Hand);
                    c = c.filter(CardListFilter.NON_LANDS);
                    return (2 >= c.size())
                            || (AllZone.getComputerPlayer().hasMetalcraft() && (AllZone.getHumanPlayer().getLife() <= 3));
                }

            }; // SpellAbility
            card.addSpellAbility(spell);
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Praetor's Counsel")) {
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = 2208683667850222369L;

                @Override
                public boolean canPlayAI() {
                    return false;
                }

                @Override
                public void resolve() {
                    final Player player = card.getController();
                    for (final Card c : player.getCardsIn(Zone.Graveyard)) {
                        AllZone.getGameAction().moveToHand(c);
                    }

                    AllZone.getGameAction().exile(card);

                    card.setSVar("HSStamp", "" + Player.getHandSizeStamp());
                    player.addHandSizeOperation(new HandSizeOp("=", -1, Integer.parseInt(card.getSVar("HSStamp"))));
                }
            }; // SpellAbility
            card.addSpellAbility(spell);
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Profane Command")) {
            // not sure what to call variables, so I just made up something
            final Player[] ab0player = new Player[1];
            final Card[] ab1card = new Card[1];
            final Card[] ab2card = new Card[1];
            final ArrayList<Card> ab3cards = new ArrayList<Card>();
            final int[] x = new int[1];

            final ArrayList<String> userChoice = new ArrayList<String>();

            final String[] cardChoice = {
                    "Target player loses X life",
                    "Return target creature card with converted mana cost X "
                            + "or less from your graveyard to the battlefield",
                    "Target creature gets -X/-X until end of turn",
                    "Up to X target creatures gain fear until end of turn" };

            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = -2924301460675657126L;

                @Override
                public void resolve() {
                    // System.out.println(userChoice);
                    // System.out.println("0: "+ab0player[0]);
                    // System.out.println("1: "+ab1card[0]);
                    // System.out.println("2: "+ab2card[0]);
                    // System.out.println("3: "+ab3cards);

                    // "Target player loses X life",
                    for (int i = 0; i < card.getChoices().size(); i++) {
                        if (card.getChoice(i).equals(cardChoice[0])) {
                            if (ab0player[0] != null) {
                                this.setTargetPlayer(ab0player[0]);
                                if (this.getTargetPlayer().canBeTargetedBy(this)) {
                                    this.getTargetPlayer().addDamage(x[0], card);
                                }
                            }
                        }
                    }

                    // "Return target creature card with converted mana cost
                    // X or less from your graveyard to the battlefield",
                    if (userChoice.contains(cardChoice[1]) || card.getChoices().contains(cardChoice[1])) {
                        final Card c = ab1card[0];
                        if (c != null) {
                            if (card.getController().getZone(Zone.Graveyard).contains(c) && c.canBeTargetedBy(this)) {
                                AllZone.getGameAction().moveToPlay(c);
                            }
                        }
                    }

                    // "Target creature gets -X/-X until end of turn",
                    for (int i = 0; i < card.getChoices().size(); i++) {
                        if (card.getChoice(i).equals(cardChoice[2])) {
                            final Card c = ab2card[0];
                            if (c != null) {
                                if (AllZoneUtil.isCardInPlay(c) && c.canBeTargetedBy(this)) {
                                    final int boost = x[0] * -1;
                                    c.addTempAttackBoost(boost);
                                    c.addTempDefenseBoost(boost);
                                    final Command untilEOT = new Command() {
                                        private static final long serialVersionUID = -6010783402521993651L;

                                        @Override
                                        public void execute() {
                                            if (AllZoneUtil.isCardInPlay(c)) {
                                                c.addTempAttackBoost(-1 * boost);
                                                c.addTempDefenseBoost(-1 * boost);

                                            }
                                        }
                                    };
                                    AllZone.getEndOfTurn().addUntil(untilEOT);
                                }
                            }
                        }
                    } // end ab[2]

                    // "Up to X target creatures gain fear until end of turn"
                    if (userChoice.contains(cardChoice[3]) || card.getChoices().contains(cardChoice[3])) {
                        final ArrayList<Card> cs = new ArrayList<Card>();
                        cs.addAll(ab3cards);
                        for (final Card c : cs) {
                            if (AllZoneUtil.isCardInPlay(c) && c.canBeTargetedBy(this)) {
                                c.addExtrinsicKeyword("Fear");
                                final Command untilEOT = new Command() {
                                    private static final long serialVersionUID = 986259855862338866L;

                                    @Override
                                    public void execute() {
                                        if (AllZoneUtil.isCardInPlay(c)) {
                                            c.removeExtrinsicKeyword("Fear");
                                        }
                                    }
                                };
                                AllZone.getEndOfTurn().addUntil(untilEOT);
                            }
                        }
                    } // end ab[3]
                } // resolve()

                @Override
                public boolean canPlayAI() {
                    return false;
                }
            }; // SpellAbility

            final Command setStackDescription = new Command() {
                private static final long serialVersionUID = 5840471361149632482L;

                @Override
                public void execute() {
                    final ArrayList<String> a = new ArrayList<String>();
                    if (userChoice.contains(cardChoice[0]) || card.getChoices().contains(cardChoice[0])) {
                        a.add(ab0player[0] + " loses X life");
                    }
                    if (userChoice.contains(cardChoice[1]) || card.getChoices().contains(cardChoice[1])) {
                        a.add("return " + ab1card[0] + " from graveyard to play");
                    }
                    if (userChoice.contains(cardChoice[2]) || card.getChoices().contains(cardChoice[2])) {
                        a.add(ab2card[0] + " gets -X/-X until end of turn");
                    }
                    if (userChoice.contains(cardChoice[3]) || card.getChoices().contains(cardChoice[3])) {
                        a.add("up to X target creatures gain Fear until end of turn");
                    }

                    final String s = a.get(0) + ", " + a.get(1);
                    spell.setStackDescription(card.getName() + " - " + s);
                }
            }; // Command

            // for ab[3] - X creatures gain fear until EOT
            final Input targetXCreatures = new Input() {
                private static final long serialVersionUID = 2584765431286321048L;

                private int stop = 0;
                private int count = 0;

                @Override
                public void showMessage() {
                    if (this.count == 0) {
                        this.stop = x[0];
                    }
                    final StringBuilder sb = new StringBuilder();
                    sb.append(cardName).append(" - Select a target creature to gain Fear (up to ");
                    sb.append(this.stop - this.count).append(" more)");
                    AllZone.getDisplay().showMessage(sb.toString());
                    ButtonUtil.enableAll();
                }

                @Override
                public void selectButtonCancel() {
                    this.stop();
                }

                @Override
                public void selectButtonOK() {
                    this.done();
                }

                @Override
                public void selectCard(final Card c, final PlayerZone zone) {
                    if (c.isCreature() && zone.is(Constant.Zone.Battlefield) && c.canBeTargetedBy(spell)
                            && !ab3cards.contains(c)) {
                        ab3cards.add(c);
                        this.count++;
                        if (this.count == this.stop) {
                            this.done();
                        } else {
                            this.showMessage();
                        }
                    }
                } // selectCard()

                private void done() {
                    setStackDescription.execute();
                    this.stopSetNext(new InputPayManaCost(spell));
                }
            };

            // for ab[2] target creature gets -X/-X
            final Input targetCreature = new Input() {
                private static final long serialVersionUID = -6879692803780014943L;

                @Override
                public void showMessage() {
                    final StringBuilder sb = new StringBuilder();
                    sb.append(cardName).append(" - Select target creature to get -X/-X");
                    AllZone.getDisplay().showMessage(sb.toString());
                    ButtonUtil.enableOnlyCancel();
                }

                @Override
                public void selectButtonCancel() {
                    this.stop();
                }

                @Override
                public void selectCard(final Card c, final PlayerZone zone) {
                    if (c.isCreature() && zone.is(Constant.Zone.Battlefield) && c.canBeTargetedBy(spell)) {
                        if (card.isCopiedSpell()) {
                            card.getChoiceTargets().remove(0);
                        }
                        ab2card[0] = c;
                        // spell.setTargetCard(c);
                        card.setSpellChoiceTarget(String.valueOf(c.getUniqueNumber()));
                        setStackDescription.execute();

                        if (userChoice.contains(cardChoice[3]) || card.getChoices().contains(cardChoice[3])) {
                            this.stopSetNext(targetXCreatures);
                        } else {
                            final StringBuilder sb = new StringBuilder();
                            sb.append("Input_PayManaCost for spell is getting: ");
                            sb.append(spell.getManaCost());
                            System.out.println(sb.toString());
                            this.stopSetNext(new InputPayManaCost(spell));
                        }
                    } // if
                } // selectCard()
            }; // Input targetCreature

            // for ab[1] - return creature from grave to the battlefield
            final Input targetGraveCreature = new Input() {
                private static final long serialVersionUID = -7558252187229252725L;

                @Override
                public void showMessage() {
                    CardList grave = card.getController().getCardsIn(Constant.Zone.Graveyard);
                    grave = grave.filter(CardListFilter.CREATURES);
                    grave = grave.filter(new CardListFilter() {
                        @Override
                        public boolean addCard(final Card c) {
                            return c.getCMC() <= x[0];
                        }
                    });

                    final Object check = GuiUtils.getChoiceOptional("Select target creature with CMC < X",
                            grave.toArray());
                    if (check != null) {
                        final Card c = (Card) check;
                        if (c.canBeTargetedBy(spell)) {
                            ab1card[0] = c;
                        }
                    } else {
                        this.stop();
                    }

                    this.done();
                } // showMessage()

                public void done() {
                    if (userChoice.contains(cardChoice[2]) || card.getChoices().contains(cardChoice[2])) {
                        this.stopSetNext(targetCreature);
                    } else if (userChoice.contains(cardChoice[3]) || card.getChoices().contains(cardChoice[3])) {
                        this.stopSetNext(targetXCreatures);
                    } else {
                        this.stopSetNext(new InputPayManaCost(spell));
                    }
                }
            }; // Input

            // for ab[0] - target player loses X life
            final Input targetPlayer = new Input() {
                private static final long serialVersionUID = 9101387253945650303L;

                @Override
                public void showMessage() {
                    final StringBuilder sb = new StringBuilder();
                    sb.append(cardName).append(" - Select target player to lose life");
                    AllZone.getDisplay().showMessage(sb.toString());
                    ButtonUtil.enableOnlyCancel();
                }

                @Override
                public void selectButtonCancel() {
                    this.stop();
                }

                @Override
                public void selectPlayer(final Player player) {
                    if (player.canBeTargetedBy(spell)) {
                        if (card.isCopiedSpell()) {
                            card.getChoiceTargets().remove(0);
                        }
                        ab0player[0] = player;
                        // spell.setTargetPlayer(player);
                        card.setSpellChoiceTarget(player.toString());
                        setStackDescription.execute();

                        if (userChoice.contains(cardChoice[1]) || card.getChoices().contains(cardChoice[1])) {
                            this.stopSetNext(targetGraveCreature);
                        } else if (userChoice.contains(cardChoice[2]) || card.getChoices().contains(cardChoice[2])) {
                            this.stopSetNext(targetCreature);
                        } else if (userChoice.contains(cardChoice[3]) || card.getChoices().contains(cardChoice[3])) {
                            this.stopSetNext(targetXCreatures);
                        } else {
                            this.stopSetNext(new InputPayManaCost(spell));
                        }
                    }
                } // selectPlayer()
            }; // Input targetPlayer

            final Input chooseX = new Input() {
                private static final long serialVersionUID = 5625588008756700226L;

                @Override
                public void showMessage() {
                    if (card.isCopiedSpell()) {
                        x[0] = 0;
                        if (userChoice.contains(cardChoice[0])) {
                            this.stopSetNext(targetPlayer);
                        } else if (userChoice.contains(cardChoice[1])) {
                            this.stopSetNext(targetGraveCreature);
                        } else if (userChoice.contains(cardChoice[2])) {
                            this.stopSetNext(targetCreature);
                        } else if (userChoice.contains(cardChoice[3])) {
                            this.stopSetNext(targetXCreatures);
                        } else {
                            throw new RuntimeException(
                                    "Something in if(isCopiedSpell()) in Profane Command selection is FUBAR.");
                        }
                    } else {
                        final ArrayList<String> choices = new ArrayList<String>();
                        for (int i = 0; i <= card.getController().getLife(); i++) {
                            choices.add("" + i);
                        }
                        final Object o = GuiUtils.getChoice("Choose X", choices.toArray());
                        // everything stops here if user cancelled
                        if (o == null) {
                            this.stop();
                            return;
                        }

                        final String answer = (String) o;

                        x[0] = Integer.parseInt(answer);
                        spell.setManaCost(x[0] + " B B");
                        spell.setIsXCost(false);

                        if (userChoice.contains(cardChoice[0])) {
                            this.stopSetNext(targetPlayer);
                        } else if (userChoice.contains(cardChoice[1])) {
                            this.stopSetNext(targetGraveCreature);
                        } else if (userChoice.contains(cardChoice[2])) {
                            this.stopSetNext(targetCreature);
                        } else if (userChoice.contains(cardChoice[3])) {
                            this.stopSetNext(targetXCreatures);
                        } else {
                            throw new RuntimeException("Something in Profane Command selection is FUBAR.");
                        }
                    }
                } // showMessage()
            }; // Input chooseX

            final Input chooseTwoInput = new Input() {
                private static final long serialVersionUID = 5625588008756700226L;

                @Override
                public void showMessage() {
                    if (card.isCopiedSpell()) {
                        if (userChoice.contains(cardChoice[0])) {
                            this.stopSetNext(targetPlayer);
                        } else if (userChoice.contains(cardChoice[1])) {
                            this.stopSetNext(targetGraveCreature);
                        } else if (userChoice.contains(cardChoice[2])) {
                            this.stopSetNext(targetCreature);
                        } else if (userChoice.contains(cardChoice[3])) {
                            this.stopSetNext(targetXCreatures);
                        } else {
                            throw new RuntimeException(
                                    "Something in if(isCopiedSpell()) in Profane Command selection is FUBAR.");
                        }
                    } else {
                        // reset variables
                        ab0player[0] = null;
                        ab1card[0] = null;
                        ab2card[0] = null;
                        ab3cards.clear();
                        card.getChoices().clear();
                        card.getChoiceTargets().clear();
                        userChoice.clear();

                        final ArrayList<String> display = new ArrayList<String>();

                        // get all
                        final CardList creatures = AllZoneUtil.getCreaturesInPlay();
                        CardList grave = card.getController().getCardsIn(Zone.Graveyard);
                        grave = grave.filter(CardListFilter.CREATURES);

                        if (AllZone.getHumanPlayer().canBeTargetedBy(spell)
                                || AllZone.getComputerPlayer().canBeTargetedBy(spell)) {
                            display.add("Target player loses X life");
                        }
                        if (grave.size() > 0) {
                            display.add("Return target creature card with converted mana "
                                    + "cost X or less from your graveyard to the battlefield");
                        }
                        if (creatures.size() > 0) {
                            display.add("Target creature gets -X/-X until end of turn");
                        }
                        display.add("Up to X target creatures gain fear until end of turn");

                        final ArrayList<String> a = this.chooseTwo(display);
                        // everything stops here if user cancelled
                        if (a == null) {
                            this.stop();
                            return;
                        }
                        userChoice.addAll(a);

                        this.stopSetNext(chooseX);
                    }
                } // showMessage()

                private ArrayList<String> chooseTwo(final ArrayList<String> choices) {
                    final ArrayList<String> out = new ArrayList<String>();
                    Object o = GuiUtils.getChoiceOptional("Choose Two", choices.toArray());
                    if (o == null) {
                        return null;
                    }

                    out.add((String) o);
                    card.addSpellChoice((String) o);
                    choices.remove(out.get(0));
                    o = GuiUtils.getChoiceOptional("Choose Two", choices.toArray());
                    if (o == null) {
                        return null;
                    }

                    out.add((String) o);
                    card.addSpellChoice((String) o);
                    return out;
                } // chooseTwo()
            }; // Input chooseTwoInput
            card.addSpellAbility(spell);

            card.setSpellWithChoices(true);
            spell.setBeforePayMana(chooseTwoInput);
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Turn to Slag")) {
            final Cost abCost = new Cost("3 R R", cardName, false);
            final Target target = new Target(card, "Select target creature", "Creature".split(","));
            final SpellAbility spell = new Spell(card, abCost, target) {
                private static final long serialVersionUID = 3848014348910653252L;

                @Override
                public boolean canPlayAI() {
                    return false;
                }

                @Override
                public void resolve() {
                    final Card tgt = this.getTargetCard();
                    if (AllZoneUtil.isCardInPlay(tgt) && tgt.canBeTargetedBy(this)) {
                        tgt.addDamage(5, card);
                        final CardList equipment = new CardList(tgt.getEquippedBy());
                        for (final Card eq : equipment) {
                            AllZone.getGameAction().destroy(eq);
                        }
                    }
                } // resolve()
            }; // SpellAbility

            final StringBuilder sb = new StringBuilder();
            sb.append(cardName).append(" deals 5 damage to target creature. ");
            sb.append("Destroy all Equipment attached to that creature.");
            spell.setDescription(sb.toString());

            card.addSpellAbility(spell);
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Transmute Artifact")) {
            /*
             * Sacrifice an artifact. If you do, search your library for an
             * artifact card. If that card's converted mana cost is less than or
             * equal to the sacrificed artifact's converted mana cost, put it
             * onto the battlefield. If it's greater, you may pay X, where X is
             * the difference. If you do, put it onto the battlefield. If you
             * don't, put it into its owner's graveyard. Then shuffle your
             * library.
             */

            final Cost abCost = new Cost("U U", cardName, false);
            final SpellAbility spell = new Spell(card, abCost, null) {
                private static final long serialVersionUID = -8497142072380944393L;

                @Override
                public boolean canPlayAI() {
                    return false;
                }

                @Override
                public void resolve() {
                    final Player p = card.getController();
                    int baseCMC = -1;
                    final Card[] newArtifact = new Card[1];

                    // Sacrifice an artifact
                    CardList arts = p.getCardsIn(Constant.Zone.Battlefield);
                    arts = arts.filter(CardListFilter.ARTIFACTS);
                    final Object toSac = GuiUtils.getChoiceOptional("Sacrifice an artifact", arts.toArray());
                    if (toSac != null) {
                        final Card c = (Card) toSac;
                        baseCMC = CardUtil.getConvertedManaCost(c);
                        AllZone.getGameAction().sacrifice(c);
                    } else {
                        return;
                    }

                    // Search your library for an artifact
                    final CardList lib = p.getCardsIn(Zone.Library);
                    GuiUtils.getChoiceOptional("Looking at Library", lib.toArray());
                    final CardList libArts = lib.filter(CardListFilter.ARTIFACTS);
                    final Object o = GuiUtils.getChoiceOptional("Search for artifact", libArts.toArray());
                    if (o != null) {
                        newArtifact[0] = (Card) o;
                    } else {
                        return;
                    }

                    final int newCMC = CardUtil.getConvertedManaCost(newArtifact[0]);

                    // if <= baseCMC, put it onto the battlefield
                    if (newCMC <= baseCMC) {
                        AllZone.getGameAction().moveToPlay(newArtifact[0]);
                    } else {
                        final String diffCost = String.valueOf(newCMC - baseCMC);
                        AllZone.getInputControl().setInput(new InputPayManaCostAbility(diffCost, new Command() {
                            private static final long serialVersionUID = -8729850321341068049L;

                            @Override
                            public void execute() {
                                AllZone.getGameAction().moveToPlay(newArtifact[0]);
                            }
                        }, new Command() {
                            private static final long serialVersionUID = -246036834856971935L;

                            @Override
                            public void execute() {
                                AllZone.getGameAction().moveToGraveyard(newArtifact[0]);
                            }
                        }));
                    }

                    // finally, shuffle library
                    p.shuffle();

                } // resolve()
            }; // SpellAbility

            card.addSpellAbility(spell);
        } // *************** END ************ END **************************

        return card;
    } // getCard
}
