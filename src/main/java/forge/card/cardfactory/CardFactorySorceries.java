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
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.List;
import java.util.Collections;

import javax.swing.JOptionPane;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import forge.Card;

import forge.CardLists;
import forge.CardPredicates;
import forge.CardPredicates.Presets;
import forge.CardUtil;
import forge.Command;
import forge.Constant;
import forge.Singletons;
import forge.card.SpellManaCost;
import forge.card.cost.Cost;
import forge.card.mana.ManaCostParser;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.control.input.Input;
import forge.control.input.InputPayManaCost;
import forge.control.input.InputPayManaCostAbility;
import forge.game.player.ComputerUtil;
import forge.game.player.Player;
import forge.game.player.PlayerUtil;
import forge.game.zone.PlayerZone;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;
import forge.gui.match.CMatchUI;

import forge.view.ButtonUtil;

/**
 * <p>
 * CardFactory_Sorceries class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class CardFactorySorceries {

    private static final SpellAbility getBrilliantUltimatum(final Card card) {
        return new Spell(card) {
            private static final long serialVersionUID = 1481112451519L;

            @Override
            public void resolve() {

                Card choice = null;

                // check for no cards in hand on resolve
                final List<Card> lib = card.getController().getCardsIn(ZoneType.Library);
                final List<Card> cards = new ArrayList<Card>();
                final List<Card> exiled = new ArrayList<Card>();
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
                    Singletons.getModel().getGame().getAction().exile(lib.get(i));
                }
                final List<Card> pile1 = new ArrayList<Card>();
                final List<Card> pile2 = new ArrayList<Card>();
                boolean stop = false;
                int pile1CMC = 0;
                int pile2CMC = 0;

                final StringBuilder msg = new StringBuilder();
                msg.append("Revealing top ").append(count).append(" cards of library: ");
                GuiChoose.one(msg.toString(), cards);
                // Human chooses
                if (card.getController().isComputer()) {
                    for (int i = 0; i < count; i++) {
                        if (!stop) {
                            choice = GuiChoose.oneOrNone("Choose cards to put into the first pile: ",
                                    cards);
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
                            final List<SpellAbility> choices = pile1.get(i).getBasicSpells();

                            for (final SpellAbility sa : choices) {
                                if (sa.canPlayAI()) {
                                    ComputerUtil.playStackFree(sa.getActivatingPlayer(), sa);
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
                            final List<SpellAbility> choices = pile2.get(i).getBasicSpells();

                            for (final SpellAbility sa : choices) {
                                if (sa.canPlayAI()) {
                                    ComputerUtil.playStackFree(sa.getActivatingPlayer(), sa);
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
                        if (biggest.getManaCost().getCMC() < c.getManaCost().getCMC()) {
                            biggest = c;
                        }
                    }

                    pile1.add(biggest);
                    cards.remove(biggest);
                    if (cards.size() > 2) {
                        final Card random = CardUtil.getRandom(cards);
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

                    List<Card> chosen;
                    if (q.equals(0)) {
                        chosen = pile1;
                    } else {
                        chosen = pile2;
                    }

                    final int numChosen = chosen.size();
                    for (int i = 0; i < numChosen; i++) {
                        final Card check = GuiChoose.oneOrNone("Select spells to play in reverse order: ", chosen);
                        if (check == null) {
                            break;
                        }

                        final Card playing = check;
                        if (playing.isLand()) {
                            if (card.getController().canPlayLand()) {
                                card.getController().playLand(playing);
                            } else {
                                JOptionPane.showMessageDialog(null, "You can't play any more lands this turn.", "",
                                        JOptionPane.INFORMATION_MESSAGE);
                            }
                        } else {
                            Singletons.getModel().getGame().getAction().playCardWithoutManaCost(playing, card.getController());
                        }
                        chosen.remove(playing);
                    }

                }
                pile1.clear();
                pile2.clear();
            } // resolve()

            @Override
            public boolean canPlayAI() {
                final List<Card> cards = getActivatingPlayer().getCardsIn(ZoneType.Library);
                return cards.size() >= 8;
            }
        }; // SpellAbility

    }

    private static final SpellAbility getGlobalRuin(final Card card) {
        final List<Card> target = new ArrayList<Card>();
        final List<Card> saveList = new ArrayList<Card>();
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
                List<Card> land = Singletons.getControl().getPlayer().getOpponent().getLandsInPlay();

                for (final String element : Constant.Color.BASIC_LANDS) {
                    final List<Card> cl = CardLists.getType(land, element);
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
                land = CardLists.filter(land, new Predicate<Card>() {
                    @Override
                    public boolean apply(final Card c) {
                        if (c.getName().contains("Dryad Arbor")) {
                            return true;
                        } else {
                            return  (!(c.isType("Forest") || c.isType("Plains") || c.isType("Mountain")
                                || c.isType("Island") || c.isType("Swamp")));
                        }
                    }
                });
                target.addAll(land);

                // when this spell resolves all basic lands which were not
                // selected are sacrificed.
                for (int i = 0; i < target.size(); i++) {
                    if (target.get(i).isInPlay() && !saveList.contains(target.get(i))) {
                        Singletons.getModel().getGame().getAction().sacrifice(target.get(i), this);
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
                    CMatchUI.SINGLETON_INSTANCE.showMessage(sb.toString());
                    ButtonUtil.enableOnlyCancel();
                }
            }

            @Override
            public void selectButtonCancel() {
                this.stop();
            }

            @Override
            public void selectCard(final Card c) {
                Zone zone = Singletons.getModel().getGame().getZoneOf(c);
                if (c.isLand() && zone.is(ZoneType.Battlefield) && c.getController().isHuman()
                /* && c.getName().equals(humanBasic.get(count)) */
                && c.isType(humanBasic.get(this.count))
                /* && !saveList.contains(c) */) {
                    // get all other basic[count] lands human player
                    // controls and add them to target
                    List<Card> land = c.getController().getLandsInPlay();
                    List<Card> cl = CardLists.getType(land, humanBasic.get(this.count));
                    cl = CardLists.filter(cl, new Predicate<Card>() {
                        @Override
                        public boolean apply(final Card crd) {
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
                    land = CardLists.filter(land, new Predicate<Card>() {
                        @Override
                        public boolean apply(final Card c) {
                            if (c.getName().contains("Dryad Arbor")) {
                                return true;
                            } else {
                                return (!(c.isType("Forest") || c.isType("Plains") || c.isType("Mountain")
                                    || c.isType("Island") || c.isType("Swamp")));
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
                // put those in an set to use laters
                final List<Card> land = Singletons.getControl().getPlayer().getCardsIn(ZoneType.Battlefield);

                for (final String element : Constant.Color.BASIC_LANDS) {
                    final List<Card> c = CardLists.getType(land, element);
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
        spell.setBeforePayMana(runtime);
        return spell;
    }

    private static final void balanceLands(Spell card) {

        List<List<Card>> lands = new ArrayList<List<Card>>();
        for (Player p : Singletons.getModel().getGame().getPlayers()) {

            lands.add(p.getLandsInPlay());
        }

        int min = Integer.MAX_VALUE;
        for (List<Card> l : lands) {
            int s = l.size();
            min = Math.min(min, s);
        }
        Iterator<List<Card>> ll = lands.iterator();
        for (Player p : Singletons.getModel().getGame().getPlayers()) {

            List<Card> l = ll.next();
            int sac = l.size() - min;
            if (sac == 0) {
                continue;
            }
            if (p.isComputer()) {
                CardLists.shuffle(l);
                for (int i = 0; i < sac; i++) {
                    Singletons.getModel().getGame().getAction().sacrifice(l.get(i), card);
                }
            } else {
                Singletons.getModel().getMatch().getInput().setInput(PlayerUtil.inputSacrificePermanents(sac, "Land"));
            }
        }
    }

    private static final void balanceHands(Spell card) {

        List<List<Card>> hands = new ArrayList<List<Card>>();
        for (Player p : Singletons.getModel().getGame().getPlayers()) {

            hands.add(p.getCardsIn(ZoneType.Hand));
        }
        int min = Integer.MAX_VALUE;
        for (List<Card> h : hands) {
            int s = h.size();
            min = Math.min(min, s);
        }
        Iterator<List<Card>> hh = hands.iterator();
        for (Player p : Singletons.getModel().getGame().getPlayers()) {

            List<Card> h = hh.next();
            int sac = h.size() - min;
            if (sac == 0) {
                continue;
            }
            p.discard(sac, card);
        }
    }

    private static final void balanceCreatures(Spell card) {
        List<List<Card>> creats = new ArrayList<List<Card>>();
        for (Player p : Singletons.getModel().getGame().getPlayers()) {

            creats.add(p.getCreaturesInPlay());
        }
        int min = Integer.MAX_VALUE;
        for (List<Card> h : creats) {
            int s = h.size();
            min = Math.min(min, s);
        }
        Iterator<List<Card>> cc = creats.iterator();
        for (Player p : Singletons.getModel().getGame().getPlayers()) {

            List<Card> c = cc.next();
            int sac = c.size() - min;
            if (sac == 0) {
                continue;
            }
            if (p.isComputer()) {
                CardLists.sortAttackLowFirst(c);
                CardLists.sortCMC(c);
                Collections.reverse(c);
                for (int i = 0; i < sac; i++) {
                    Singletons.getModel().getGame().getAction().sacrifice(c.get(i), card);
                }
            } else {
                Singletons.getModel().getMatch().getInput().setInput(PlayerUtil.inputSacrificePermanents(sac, "Creature"));
            }
        }
    }

    private static final SpellAbility getBalance(final Card card) {
        return new Spell(card) {
            private static final long serialVersionUID = -5941893280103164961L;

            @Override
            public void resolve() {
                balanceLands(this);
                balanceHands(this);
                balanceCreatures(this);
            }

            @Override
            public boolean canPlayAI() {
                int diff = 0;
                final Player ai = getActivatingPlayer();
                final Player opp = ai.getOpponent();
                final List<Card> humLand = opp.getLandsInPlay();
                final List<Card> compLand = ai.getLandsInPlay();
                diff += humLand.size() - compLand.size();

                final List<Card> humCreats = opp.getCreaturesInPlay();
                List<Card> compCreats = ai.getCreaturesInPlay();
                compCreats = CardLists.filter(compCreats, CardPredicates.Presets.CREATURES);
                diff += 1.5 * (humCreats.size() - compCreats.size());

                final List<Card> humHand = opp.getCardsIn(ZoneType.Hand);
                final List<Card> compHand = ai.getCardsIn(ZoneType.Hand);
                diff += 0.5 * (humHand.size() - compHand.size());

                return diff > 2;
            }
        };
    }

    private static final SpellAbility getWindfall(final Card card) {
        return new Spell(card) {
            private static final long serialVersionUID = -7707012960887790709L;

            @Override
            public boolean canPlayAI() {
                /*
                 * We want compy to have less cards in hand than the human
                 */
                final Player ai = getActivatingPlayer();
                final Player opp = ai.getOpponent();
                final List<Card> humanHand = opp.getCardsIn(ZoneType.Hand);
                final List<Card> computerHand = ai.getCardsIn(ZoneType.Hand);
                return computerHand.size() < humanHand.size();
            }

            @Override
            public void resolve() {
                int max = Integer.MIN_VALUE;
                for (Player p : Singletons.getModel().getGame().getPlayers()) {

                    int s = p.getZone(ZoneType.Hand).size();
                    max = Math.max(max, s);
                }
                for (Player p : Singletons.getModel().getGame().getPlayers()) {

                    this.discardDraw(p, max);
                }
            } // resolve()

            void discardDraw(final Player player, final int num) {
                player.discardHand(this);
                player.drawCards(num);
            }
        };
    }

    private static final SpellAbility getPatriarchsBidding(final Card card) {

        final SpellAbility spell = new Spell(card) {
            private static final long serialVersionUID = -2182173662547136798L;

            @Override
            public void resolve() {
                List<String> types = new ArrayList<String>();
                for (Player p : Singletons.getModel().getGame().getPlayers()) {
                    if (p.isHuman()) {
                         types.add(GuiChoose.one("Which creature type?", Constant.CardTypes.CREATURE_TYPES));
                    } else {
                        final HashMap<String, Integer> countInGraveyard = new HashMap<String, Integer>();
                        final List<Card> aiGrave = p.getCardsIn(ZoneType.Graveyard);
                        for (final Card c : Iterables.filter(aiGrave, CardPredicates.Presets.CREATURES)) {
                            for (final String type : c.getType()) {
                                if (CardUtil.isACreatureType(type)) {
                                    Integer oldVal = countInGraveyard.get(type);
                                    countInGraveyard.put(type, 1 + (oldVal != null ? oldVal : 0));
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
                        types.add(maxKey.equals("") ? "Sliver" : maxKey);
                    }
                }

                List<Card> bidded = CardLists.filter(Singletons.getModel().getGame().getCardsIn(ZoneType.Graveyard), CardPredicates.Presets.CREATURES);
                for (final Card c : bidded) {
                    for (int i = 0; i < types.size(); i++) {
                        if (c.isType(types.get(i))) {
                            Singletons.getModel().getGame().getAction().moveToPlay(c);
                            i = types.size(); // break inner loop
                        }
                    }
                }
            } // resolve()
        }; // SpellAbility
        final StringBuilder sb = new StringBuilder();
        sb.append(card.getName()).append(" - choose a creature type.");
        spell.setStackDescription(sb.toString());
        return spell;
    }

    private static final SpellAbility getLeeches(final Card card) {
        /*
         * Target player loses all poison counters. Leeches deals that much
         * damage to that player.
         */
        final Target tgt = new Target(card, "Select target player", "Player");
        final Cost cost = new Cost(card, "1 W W", false);
        return new Spell(card, cost, tgt) {
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
                final Player ai = getActivatingPlayer();
                final Player opp = ai.getOpponent();
                final int humanPoison = opp.getPoisonCounters();
                final int compPoison = ai.getPoisonCounters();

                if (opp.getLife() <= humanPoison) {
                    tgt.addTarget(opp);
                    return true;
                }

                if ((((2 * (11 - compPoison)) < ai.getLife()) || (compPoison > 7))
                        && (compPoison < (ai.getLife() - 2))) {
                    tgt.addTarget(ai);
                    return true;
                }

                return false;
            }
        };
    }

    private static final SpellAbility getSanityGrinding(final Card card) {
        /*
         * Chroma - Reveal the top ten cards of your library. For each blue
         * mana symbol in the mana costs of the revealed cards, target
         * opponent puts the top card of his or her library into his or her
         * graveyard. Then put the cards you revealed this way on the bottom
         * of your library in any order.
         */
        return new Spell(card) {
            private static final long serialVersionUID = 4475834103787262421L;

            @Override
            public void resolve() {
                final Player player = card.getController();
                final Player opp = player.getOpponent();
                final PlayerZone lib = card.getController().getZone(ZoneType.Library);
                int maxCards = lib.size();
                maxCards = Math.min(maxCards, 10);
                if (maxCards == 0) {
                    return;
                }
                final List<Card> topCards = new ArrayList<Card>();
                // show top n cards:
                for (int j = 0; j < maxCards; j++) {
                    topCards.add(lib.get(j));
                }
                final int num = CardFactoryUtil.getNumberOfManaSymbolsByColor("U", topCards);
                final StringBuilder sb = new StringBuilder();
                sb.append("Revealed cards - ").append(num).append(" U mana symbols");
                GuiChoose.oneOrNone(sb.toString(), topCards);

                // opponent moves this many cards to graveyard
                opp.mill(num);

                // then, move revealed cards to bottom of library
                for (final Card c : topCards) {
                    Singletons.getModel().getGame().getAction().moveToBottomOfLibrary(c);
                }
            } // resolve()

            @Override
            public boolean canPlayAI() {
                return !getActivatingPlayer().getZone(ZoneType.Library).isEmpty();
            }

        };
    }

    private static final SpellAbility getProfaneCommand(final Card card) {
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
                        if (card.getController().getZone(ZoneType.Graveyard).contains(c) && c.canBeTargetedBy(this)) {
                            Singletons.getModel().getGame().getAction().moveToPlay(c);
                        }
                    }
                }

                // "Target creature gets -X/-X until end of turn",
                for (int i = 0; i < card.getChoices().size(); i++) {
                    if (card.getChoice(i).equals(cardChoice[2])) {
                        final Card c = ab2card[0];
                        if (c != null) {
                            if (c.isInPlay() && c.canBeTargetedBy(this)) {
                                final int boost = x[0] * -1;
                                c.addTempAttackBoost(boost);
                                c.addTempDefenseBoost(boost);
                                final Command untilEOT = new Command() {
                                    private static final long serialVersionUID = -6010783402521993651L;

                                    @Override
                                    public void execute() {
                                        if (c.isInPlay()) {
                                            c.addTempAttackBoost(-1 * boost);
                                            c.addTempDefenseBoost(-1 * boost);

                                        }
                                    }
                                };
                                Singletons.getModel().getGame().getEndOfTurn().addUntil(untilEOT);
                            }
                        }
                    }
                } // end ab[2]

                // "Up to X target creatures gain fear until end of turn"
                if (userChoice.contains(cardChoice[3]) || card.getChoices().contains(cardChoice[3])) {
                    final ArrayList<Card> cs = new ArrayList<Card>();
                    cs.addAll(ab3cards);
                    for (final Card c : cs) {
                        if (c.isInPlay() && c.canBeTargetedBy(this)) {
                            c.addExtrinsicKeyword("Fear");
                            final Command untilEOT = new Command() {
                                private static final long serialVersionUID = 986259855862338866L;

                                @Override
                                public void execute() {
                                    if (c.isInPlay()) {
                                        c.removeExtrinsicKeyword("Fear");
                                    }
                                }
                            };
                            Singletons.getModel().getGame().getEndOfTurn().addUntil(untilEOT);
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
                sb.append(card.getName()).append(" - Select a target creature to gain Fear (up to ");
                sb.append(this.stop - this.count).append(" more)");
                CMatchUI.SINGLETON_INSTANCE.showMessage(sb.toString());
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
            public void selectCard(final Card c) {
                Zone zone = Singletons.getModel().getGame().getZoneOf(c);
                if (c.isCreature() && zone.is(ZoneType.Battlefield) && c.canBeTargetedBy(spell)
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
                sb.append(card.getName()).append(" - Select target creature to get -X/-X");
                CMatchUI.SINGLETON_INSTANCE.showMessage(sb.toString());
                ButtonUtil.enableOnlyCancel();
            }

            @Override
            public void selectButtonCancel() {
                this.stop();
            }

            @Override
            public void selectCard(final Card c) {
                Zone zone = Singletons.getModel().getGame().getZoneOf(c);
                if (c.isCreature() && zone.is(ZoneType.Battlefield) && c.canBeTargetedBy(spell)) {
                    ab2card[0] = c;
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
                List<Card> grave = CardLists.filter(card.getController().getCardsIn(ZoneType.Graveyard), Presets.CREATURES);
                grave = CardLists.filter(grave, new Predicate<Card>() {
                    @Override
                    public boolean apply(final Card c) {
                        return c.getCMC() <= x[0];
                    }
                });

                final Card check = GuiChoose.oneOrNone("Select target creature with CMC < X", grave);
                if (check != null) {
                    final Card c = check;
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
                sb.append(card.getName()).append(" - Select target player to lose life");
                CMatchUI.SINGLETON_INSTANCE.showMessage(sb.toString());
                ButtonUtil.enableOnlyCancel();
            }

            @Override
            public void selectButtonCancel() {
                this.stop();
            }

            @Override
            public void selectPlayer(final Player player) {
                if (player.canBeTargetedBy(spell)) {
                    ab0player[0] = player;
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
                    final int max = card.getController().getLife();
                    final Integer[] choices = new Integer[max + 1];
                    for (int i = 0; i <= max; i++) {
                        choices[i] = Integer.valueOf(i);
                    }
                    final Integer answer = GuiChoose.one("Choose X", choices);
                    // everything stops here if user cancelled
                    if (answer == null) {
                        this.stop();
                        return;
                    }
                    x[0] = answer;
                    spell.setManaCost(new SpellManaCost( new ManaCostParser(x[0] + " B B")));
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
                    userChoice.clear();

                    final ArrayList<String> display = new ArrayList<String>();

                    // get all
                    final List<Card> creatures =
                            CardLists.filter(Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield), Presets.CREATURES);
                    List<Card> grave = CardLists.filter(card.getController().getCardsIn(ZoneType.Graveyard), Presets.CREATURES);

                    for (Player p : Singletons.getModel().getGame().getPlayers()) {
                        if (p.canBeTargetedBy(spell)) {
                            display.add("Target player loses X life");
                            break;
                        }
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
                Object o = GuiChoose.oneOrNone("Choose Two", choices);
                if (o == null) {
                    return null;
                }

                out.add((String) o);
                choices.remove(out.get(0));
                o = GuiChoose.oneOrNone("Choose Two", choices);
                if (o == null) {
                    return null;
                }

                out.add((String) o);
                card.setSpellChoice(out);
                return out;
            } // chooseTwo()
        }; // Input chooseTwoInput

        spell.setBeforePayMana(chooseTwoInput);
        card.setSpellWithChoices(true);
        return spell;
    }

    private static final SpellAbility getTransmuteArtifact(final Card card) {
        /*
         * Sacrifice an artifact. If you do, search your library for an
         * artifact card. If that card's converted mana cost is less than or
         * equal to the sacrificed artifact's converted mana cost, put it
         * onto the battlefield. If it's greater, you may pay X, where X is
         * the difference. If you do, put it onto the battlefield. If you
         * don't, put it into its owner's graveyard. Then shuffle your
         * library.
         */

        final Cost abCost = new Cost(card, "U U", false);
        return new Spell(card, abCost, null) {
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
                List<Card> arts = CardLists.filter(p.getCardsIn(ZoneType.Battlefield), Presets.ARTIFACTS);
                final Object toSac = GuiChoose.oneOrNone("Sacrifice an artifact", arts);
                if (toSac != null) {
                    final Card c = (Card) toSac;
                    baseCMC = CardUtil.getConvertedManaCost(c);
                    Singletons.getModel().getGame().getAction().sacrifice(c, this);
                } else {
                    return;
                }

                // Search your library for an artifact
                final List<Card> lib = p.getCardsIn(ZoneType.Library);
                GuiChoose.oneOrNone("Looking at Library", lib);
                final List<Card> libArts = CardLists.filter(lib, Presets.ARTIFACTS);
                final Object o = GuiChoose.oneOrNone("Search for artifact", libArts);
                if (o != null) {
                    newArtifact[0] = (Card) o;
                } else {
                    return;
                }

                final int newCMC = CardUtil.getConvertedManaCost(newArtifact[0]);

                // if <= baseCMC, put it onto the battlefield
                if (newCMC <= baseCMC) {
                    Singletons.getModel().getGame().getAction().moveToPlay(newArtifact[0]);
                } else {
                    final String diffCost = String.valueOf(newCMC - baseCMC);
                    Singletons.getModel().getMatch().getInput().setInput(new InputPayManaCostAbility(diffCost, new Command() {
                        private static final long serialVersionUID = -8729850321341068049L;

                        @Override
                        public void execute() {
                            Singletons.getModel().getGame().getAction().moveToPlay(newArtifact[0]);
                        }
                    }, new Command() {
                        private static final long serialVersionUID = -246036834856971935L;

                        @Override
                        public void execute() {
                            Singletons.getModel().getGame().getAction().moveToGraveyard(newArtifact[0]);
                        }
                    }));
                }

                // finally, shuffle library
                p.shuffle();

            } // resolve()
        }; // SpellAbility
    }

    public static void buildCard(final Card card, final String cardName) {

        if (cardName.equals("Brilliant Ultimatum")) { card.addSpellAbility(getBrilliantUltimatum(card));
        } else if (cardName.equals("Global Ruin")) { card.addSpellAbility(getGlobalRuin(card));
        } else if (cardName.equals("Balance")) { card.addSpellAbility(getBalance(card));
        } else if (cardName.equals("Windfall")) { card.addSpellAbility(getWindfall(card));
        } else if (cardName.equals("Patriarch's Bidding")) { card.addSpellAbility(getPatriarchsBidding(card));
        } else if (cardName.equals("Leeches")) { card.addSpellAbility(getLeeches(card));
        } else if (cardName.equals("Sanity Grinding")) { card.addSpellAbility(getSanityGrinding(card));
        } else if (cardName.equals("Profane Command")) { card.addSpellAbility(getProfaneCommand(card));
        } else if (cardName.equals("Transmute Artifact")) { card.addSpellAbility(getTransmuteArtifact(card));
        }
    } // getCard
}
