package forge.card.cardFactory;

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
import forge.card.spellability.Spell_Permanent;
import forge.card.spellability.Target;
import forge.gui.GuiUtils;
import forge.gui.input.Input;
import forge.gui.input.Input_PayManaCost;
import forge.gui.input.Input_PayManaCost_Ability;

/**
 * <p>
 * CardFactory_Sorceries class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class CardFactory_Sorceries {

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

                    Card crd0 = target[0];
                    Card crd1 = target[1];

                    if (crd0 != null && crd1 != null) {
                        Player p0 = crd0.getController();
                        Player p1 = crd1.getController();
                        crd0.addController(p1);
                        crd1.addController(p0);
                        // AllZone.getGameAction().changeController(new
                        // CardList(crd0), p0, p1);
                        // AllZone.getGameAction().changeController(new
                        // CardList(crd1), p1, p0);
                    }

                }// resolve()
            };// SpellAbility

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
                    stop();
                }

                @Override
                public void selectCard(final Card c, final PlayerZone zone) {
                    // must target creature you control
                    if (index[0] == 0 && !c.getController().equals(card.getController())) {
                        return;
                    }

                    // must target creature you don't control
                    if (index[0] == 1 && c.getController().equals(card.getController())) {
                        return;
                    }

                    if (c.isLand() && zone.is(Constant.Zone.Battlefield) && CardFactoryUtil.canTarget(card, c)) {
                        target[index[0]] = c;
                        index[0]++;
                        showMessage();

                        if (index[0] == target.length) {
                            if (this.isFree()) {
                                this.setFree(false);
                                AllZone.getStack().add(spell);
                                stop();
                            } else {
                                stopSetNext(new Input_PayManaCost(spell));
                            }
                        }
                    }
                }// selectCard()
            };// Input

            Input runtime = new Input() {

                private static final long serialVersionUID = 4003351872990899418L;

                @Override
                public void showMessage() {
                    index[0] = 0;
                    stopSetNext(input);
                }
            };// Input

            card.addSpellAbility(spell);
            spell.setBeforePayMana(runtime);
        }// *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Do or Die")) {
            // TODO: Please please please, someone fix this card
            Cost cost = new Cost("1 B", cardName, false);
            Target tgt = new Target(card, "Select a Player", "Player");
            final SpellAbility spell = new Spell(card, cost, tgt) {
                private static final long serialVersionUID = 8241241003478388362L;

                @Override
                public boolean canPlayAI() {
                    return 4 <= CardFactoryUtil.AI_getHumanCreature(card, true).size();
                }

                @Override
                public void resolve() {
                    CardList list = AllZoneUtil.getCreaturesInPlay(getTargetPlayer());

                    list.shuffle();

                    for (int i = 0; i < list.size() / 2; i++) {
                        AllZone.getGameAction().destroyNoRegeneration(list.get(i));
                    }
                }
            };// SpellAbility
            spell.setChooseTargetAI(CardFactoryUtil.AI_targetHuman());

            card.setSVar("PlayMain1", "TRUE");
            card.addSpellAbility(spell);
        }// *************** END ************ END **************************

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

                public void execute() {
                    // int i = 0;
                    for (Card target : targets) {
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
                }// execute()
            };// Command

            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = -532862769235091780L;

                @Override
                public void resolve() {
                    CardList creatures = AllZoneUtil.getCreaturesInPlay();
                    newZone[0] = card.getController().getZone(Constant.Zone.Battlefield);
                    int i = 0;
                    for (Card target : creatures) {
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
                        }// is card in play?
                    }// end for
                    AllZone.getEndOfTurn().addUntil(untilEOT);
                }// resolve()

                @Override
                public boolean canPlayAI() {
                    CardList creatures = AllZoneUtil.getCreaturesInPlay(AllZone.getHumanPlayer());
                    return creatures.size() > 0 && AllZone.getPhase().getPhase().equals(Constant.Phase.Main1);
                }// canPlayAI()

            };// SpellAbility
            card.addSpellAbility(spell);
            card.setSVar("PlayMain1", "TRUE");
        }// *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Mind's Desire")) {
            final Spell PlayCreature = new Spell(card) {
                private static final long serialVersionUID = 53838791023456795L;

                @Override
                public void resolve() {
                    Player player = card.getController();
                    PlayerZone play = player.getZone(Constant.Zone.Battlefield);
                    PlayerZone RFG = player.getZone(Constant.Zone.Exile);
                    Card[] Attached = card.getAttachedCardsByMindsDesire();
                    RFG.remove(Attached[0]);
                    play.add(Attached[0]);
                    card.unattachCardByMindDesire(Attached[0]);
                }// resolve()
            };// SpellAbility

            final Ability freeCast = new Ability(card, "0") {

                @Override
                public void resolve() {
                    Card target = null;
                    Card c = null;
                    Player player = card.getController();
                    if (player.isHuman()) {
                        Card[] Attached = getSourceCard().getAttachedCardsByMindsDesire();
                        Card[] Choices = new Card[Attached.length];
                        boolean SystemsGo = true;
                        if (AllZone.getStack().size() > 0) {
                            CardList Config = new CardList();
                            for (int i = 0; i < Attached.length; i++) {
                                if (Attached[i].isInstant() == true || Attached[i].hasKeyword("Flash") == true) {
                                    Config.add(Attached[i]);
                                }
                            }
                            for (int i = 0; i < Config.size(); i++) {
                                Card crd = Config.get(i);
                                Choices[i] = crd;
                            }
                            if (Config.size() == 0) {
                                SystemsGo = false;
                            }
                        } else {
                            for (int i = 0; i < Attached.length; i++) {
                                Choices[i] = Attached[i];
                            }
                        }
                        Object check = null;
                        if (SystemsGo == true) {
                            check = GuiUtils.getChoiceOptional("Select Card to play for free", Choices);
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
                                } else if (c.isPermanent() == true && c.isAura() == false) {
                                    c.removeIntrinsicKeyword("Flash"); // Stops
                                                                       // the
                                                                       // player
                                                                       // from
                                                                       // re-casting
                                                                       // the
                                                                       // flash
                                                                       // spell.

                                    StringBuilder sb = new StringBuilder();
                                    sb.append(c.getName()).append(" - Copied from Mind's Desire");
                                    PlayCreature.setStackDescription(sb.toString());

                                    Card[] ReAttach = new Card[Attached.length];
                                    ReAttach[0] = c;
                                    int ReAttach_Count = 0;
                                    for (int i = 0; i < Attached.length; i++) {
                                        if (Attached[i] != target) {
                                            ReAttach_Count = ReAttach_Count + 1;
                                            ReAttach[ReAttach_Count] = Attached[i];
                                        }
                                    }
                                    // Clear Attached List
                                    for (int i = 0; i < Attached.length; i++) {
                                        card.unattachCardByMindDesire(Attached[i]);
                                    }
                                    // Re-add
                                    for (int i = 0; i < ReAttach.length; i++) {
                                        if (ReAttach[i] != null) {
                                            card.attachCardByMindsDesire(ReAttach[i]);
                                        }
                                    }
                                    target.addSpellAbility(PlayCreature);
                                    AllZone.getStack().add(PlayCreature);
                                } else {
                                    AllZone.getGameAction().playCardNoCost(c);
                                    card.unattachCardByMindDesire(c);
                                }
                            } else {
                                JOptionPane.showMessageDialog(null,
                                        "Player cancelled or there is no more cards available on Mind's Desire.", "",
                                        JOptionPane.INFORMATION_MESSAGE);
                            }
                        } else {
                            JOptionPane
                                    .showMessageDialog(
                                            null,
                                            "You can only play an instant at this point in time, but none are attached to Mind's Desire.",
                                            "", JOptionPane.INFORMATION_MESSAGE);
                        }
                    }
                }

                public boolean canPlayAI() {
                    return false;
                }

            };
            freeCast.setStackDescription("Mind's Desire - play card without paying its mana cost.");

            Command intoPlay = new Command() {
                private static final long serialVersionUID = 920148510259054021L;

                public void execute() {
                    Player player = AllZone.getPhase().getPlayerTurn();
                    PlayerZone Play = player.getZone(Constant.Zone.Battlefield);
                    Card Minds_D = card;
                    if (player.isHuman()) {
                        card.getController().shuffle();
                    }
                    CardList MindsList = player.getCardsIn(Zone.Battlefield);
                    MindsList = MindsList.getName("Mind's Desire");
                    MindsList.remove(card);
                    if (MindsList.size() > 0) {
                        Play.remove(card);
                        Minds_D = MindsList.get(0);
                    } else {
                        JOptionPane.showMessageDialog(null,
                                "Click Mind's Desire to see the available cards to play without paying its mana cost.",
                                "", JOptionPane.INFORMATION_MESSAGE);
                    }
                    CardList libList = player.getCardsIn(Zone.Library);
                    Card c = null;
                    if (libList.size() > 0) {
                        c = libList.get(0);
                        PlayerZone RFG = player.getZone(Constant.Zone.Exile);
                        AllZone.getGameAction().moveTo(RFG, c);
                        Minds_D.attachCardByMindsDesire(c);
                    }
                    final Card Minds = card;
                    // AllZone.getGameAction().exile(Minds);
                    Minds.setImmutable(true);
                    Command untilEOT = new Command() {
                        private static final long serialVersionUID = -28032591440730370L;

                        public void execute() {
                            Player player = AllZone.getPhase().getPlayerTurn();
                            PlayerZone play = player.getZone(Constant.Zone.Battlefield);
                            play.remove(Minds);
                        }
                    };
                    AllZone.getEndOfTurn().addUntil(untilEOT);
                }

            };
            SpellAbility spell = new Spell_Permanent(card) {
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
                    CardList lib = card.getController().getCardsIn(Zone.Library);
                    CardList cards = new CardList();
                    CardList exiled = new CardList();
                    if (lib.size() == 0) {
                        JOptionPane.showMessageDialog(null, "No more cards in library.", "",
                                JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }
                    int Count = 5;
                    if (lib.size() < 5) {
                        Count = lib.size();
                    }
                    for (int i = 0; i < Count; i++) {
                        cards.add(lib.get(i));
                    }
                    for (int i = 0; i < Count; i++) {
                        exiled.add(lib.get(i));
                        AllZone.getGameAction().exile(lib.get(i));
                    }
                    CardList Pile1 = new CardList();
                    CardList Pile2 = new CardList();
                    boolean stop = false;
                    int Pile1CMC = 0;
                    int Pile2CMC = 0;

                    GuiUtils.getChoice("Revealing top " + Count + " cards of library: ", cards.toArray());
                    // Human chooses
                    if (card.getController().isComputer()) {
                        for (int i = 0; i < Count; i++) {
                            if (stop == false) {
                                choice = GuiUtils.getChoiceOptional("Choose cards to put into the first pile: ",
                                        cards.toArray());
                                if (choice != null) {
                                    Pile1.add(choice);
                                    cards.remove(choice);
                                    Pile1CMC = Pile1CMC + CardUtil.getConvertedManaCost(choice);
                                } else {
                                    stop = true;
                                }
                            }
                        }
                        for (int i = 0; i < Count; i++) {
                            if (!Pile1.contains(exiled.get(i))) {
                                Pile2.add(exiled.get(i));
                                Pile2CMC = Pile2CMC + CardUtil.getConvertedManaCost(exiled.get(i));
                            }
                        }
                        StringBuilder sb = new StringBuilder();
                        sb.append("You have spilt the cards into the following piles" + "\r\n" + "\r\n");
                        sb.append("Pile 1: " + "\r\n");
                        for (int i = 0; i < Pile1.size(); i++) {
                            sb.append(Pile1.get(i).getName() + "\r\n");
                        }
                        sb.append("\r\n" + "Pile 2: " + "\r\n");
                        for (int i = 0; i < Pile2.size(); i++) {
                            sb.append(Pile2.get(i).getName() + "\r\n");
                        }
                        JOptionPane.showMessageDialog(null, sb, "", JOptionPane.INFORMATION_MESSAGE);
                        if (Pile1CMC >= Pile2CMC) {
                            JOptionPane.showMessageDialog(null, "Computer chooses the Pile 1", "",
                                    JOptionPane.INFORMATION_MESSAGE);
                            for (int i = 0; i < Pile1.size(); i++) {
                                ArrayList<SpellAbility> choices = Pile1.get(i).getBasicSpells();

                                for (SpellAbility sa : choices) {
                                    if (sa.canPlayAI()) {
                                        ComputerUtil.playStackFree(sa);
                                        if (Pile1.get(i).isPermanent()) {
                                            exiled.remove(Pile1.get(i));
                                        }
                                        break;
                                    }
                                }
                            }
                        } else {
                            JOptionPane.showMessageDialog(null, "Computer chooses the Pile 2", "",
                                    JOptionPane.INFORMATION_MESSAGE);
                            for (int i = 0; i < Pile2.size(); i++) {
                                ArrayList<SpellAbility> choices = Pile2.get(i).getBasicSpells();

                                for (SpellAbility sa : choices) {
                                    if (sa.canPlayAI()) {
                                        ComputerUtil.playStackFree(sa);
                                        if (Pile2.get(i).isPermanent()) {
                                            exiled.remove(Pile2.get(i));
                                        }
                                        break;
                                    }
                                }
                            }
                        }

                    } else {// Computer chooses (It picks the highest converted
                            // mana cost card and 1 random card.)
                        Card biggest = exiled.get(0);

                        for (Card c : exiled)
                            if (CardUtil.getConvertedManaCost(biggest.getManaCost()) < CardUtil.getConvertedManaCost(c
                                    .getManaCost())) {
                                biggest = c;
                            }

                        Pile1.add(biggest);
                        cards.remove(biggest);
                        if (cards.size() > 2) {
                            Card Random = CardUtil.getRandom(cards.toArray());
                            Pile1.add(Random);
                        }
                        for (int i = 0; i < Count; i++) {
                            if (!Pile1.contains(exiled.get(i)))
                                Pile2.add(exiled.get(i));
                        }
                        StringBuilder sb = new StringBuilder();
                        sb.append("Choose a pile to add to your hand: " + "\r\n" + "\r\n");
                        sb.append("Pile 1: " + "\r\n");
                        for (int i = 0; i < Pile1.size(); i++) {
                            sb.append(Pile1.get(i).getName() + "\r\n");
                        }
                        sb.append("\r\n" + "Pile 2: " + "\r\n");
                        for (int i = 0; i < Pile2.size(); i++) {
                            sb.append(Pile2.get(i).getName() + "\r\n");
                        }
                        Object[] possibleValues = { "Pile 1", "Pile 2" };
                        Object q = JOptionPane.showOptionDialog(null, sb, "Brilliant Ultimatum",
                                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, possibleValues,
                                possibleValues[0]);

                        CardList chosen;
                        if (q.equals(0)) {
                            chosen = Pile1;
                        } else {
                            chosen = Pile2;
                        }

                        int numChosen = chosen.size();
                        for (int i = 0; i < numChosen; i++) {
                            Object check = GuiUtils.getChoiceOptional("Select spells to play in reverse order: ",
                                    chosen.toArray());
                            if (check == null) {
                                break;
                            }

                            Card playing = (Card) check;
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
                    Pile1.clear();
                    Pile2.clear();
                }// resolve()

                @Override
                public boolean canPlayAI() {
                    CardList cards = AllZone.getComputerPlayer().getCardsIn(Zone.Library);
                    return cards.size() >= 8;
                }
            };// SpellAbility
            card.addSpellAbility(spell);
        }// *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Cranial Extraction")) {
            Cost cost = new Cost("3 B", cardName, false);
            Target tgt = new Target(card, "Select a Player", "Player");
            final SpellAbility spell = new Spell(card, cost, tgt) {
                private static final long serialVersionUID = 8127696608769903507L;

                @Override
                public void resolve() {
                    Player target = getTargetPlayer();
                    String choice = null;

                    // human chooses
                    if (card.getController().isHuman()) {
                        choice = JOptionPane.showInputDialog(null, "Name a nonland card", cardName,
                                JOptionPane.QUESTION_MESSAGE);

                        CardList showLibrary = target.getCardsIn(Zone.Library);
                        GuiUtils.getChoiceOptional("Target Player's Library", showLibrary.toArray());

                        CardList showHand = target.getCardsIn(Zone.Hand);
                        GuiUtils.getChoiceOptional("Target Player's Hand", showHand.toArray());
                    }// if
                    else // computer chooses
                    {
                        // the computer cheats by choosing a creature in the
                        // human players library or hand
                        CardList all = target.getCardsIn(Zone.Hand);
                        all.addAll(target.getCardsIn(Zone.Library));

                        CardList four = all.filter(new CardListFilter() {
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

                    }// else
                    remove(choice, target);
                    target.shuffle();
                }// resolve()

                void remove(final String name, final Player player) {
                    CardList all = player.getCardsIn(Zone.Hand);
                    all.addAll(player.getCardsIn(Zone.Graveyard));
                    all.addAll(player.getCardsIn(Zone.Library));

                    for (int i = 0; i < all.size(); i++) {
                        if (all.get(i).getName().equals(name)) {
                            if (!all.get(i).isLand()) {
                                AllZone.getGameAction().exile(all.get(i));
                            }
                        }
                    }
                }// remove()

                @Override
                public boolean canPlayAI() {
                    CardList c = AllZone.getHumanPlayer().getCardsIn(Zone.Library);
                    c = c.filter(CardListFilter.nonlands);
                    return c.size() > 0;
                }
            };// SpellAbility spell
            spell.setChooseTargetAI(CardFactoryUtil.AI_targetHuman());

            card.addSpellAbility(spell);
        }// *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Maelstrom Pulse")) {
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = -4050843868789582138L;

                @Override
                public boolean canPlayAI() {
                    CardList c = getCreature();
                    if (c.isEmpty()) {
                        return false;
                    } else {
                        setTargetCard(c.get(0));
                        return true;
                    }
                }// canPlayAI()

                CardList getCreature() {
                    CardList out = new CardList();
                    CardList list = CardFactoryUtil.AI_getHumanCreature("Flying", card, true);
                    list.shuffle();

                    for (int i = 0; i < list.size(); i++) {
                        if ((list.get(i).getNetAttack() >= 2) && (list.get(i).getNetDefense() <= 2))
                            out.add(list.get(i));
                    }

                    // in case human player only has a few creatures in play,
                    // target anything
                    if (out.isEmpty() && 0 < CardFactoryUtil.AI_getHumanCreature(2, card, true).size()
                            && 3 > CardFactoryUtil.AI_getHumanCreature(card, true).size()) {
                        out.addAll(CardFactoryUtil.AI_getHumanCreature(2, card, true));
                        CardListUtil.sortFlying(out);
                    }
                    return out;
                }// getCreature()

                @Override
                public void resolve() {
                    if (AllZoneUtil.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card, getTargetCard())) {

                        AllZone.getGameAction().destroy(getTargetCard());

                        if (!getTargetCard().isFaceDown()) {
                            // get all creatures
                            CardList list = AllZoneUtil.getCardsIn(Zone.Battlefield);

                            list = list.getName(getTargetCard().getName());
                            list.remove(getTargetCard());

                            if (!getTargetCard().isFaceDown()) {
                                for (int i = 0; i < list.size(); i++)
                                    AllZone.getGameAction().destroy(list.get(i));
                            }
                        }// is token?
                    }// in play?
                }// resolve()
            };// SpellAbility

            card.addSpellAbility(spell);

            Input target = new Input() {
                private static final long serialVersionUID = -4947592326270275532L;

                @Override
                public void showMessage() {
                    AllZone.getDisplay().showMessage("Select target nonland permanent for " + spell.getSourceCard());
                    ButtonUtil.enableOnlyCancel();
                }

                @Override
                public void selectButtonCancel() {
                    stop();
                }

                @Override
                public void selectCard(final Card card, final PlayerZone zone) {
                    if (zone.is(Constant.Zone.Battlefield) && !card.isLand()) {
                        spell.setTargetCard(card);
                        if (this.isFree()) {
                            this.setFree(false);
                            AllZone.getStack().add(spell);
                            stop();
                        } else {
                            stopSetNext(new Input_PayManaCost(spell));
                        }
                    }
                }
            };// Input

            spell.setBeforePayMana(target);
        }// *************** END ************ END ***************************

        // *************** START *********** START **************************
        else if (cardName.equals("Erratic Explosion")) {
            Cost cost = new Cost(card.getManaCost(), cardName, false);
            Target tgt = new Target(card, "CP");
            final SpellAbility spell = new Spell(card, cost, tgt) {
                private static final long serialVersionUID = -6003403347798646257L;

                int damage = 3;
                Card check;

                @Override
                public boolean canPlayAI() {
                    if (AllZone.getHumanPlayer().getLife() <= damage) {
                        return true;
                    }

                    check = getFlying();
                    return check != null;
                }

                @Override
                public void chooseTargetAI() {
                    if (AllZone.getHumanPlayer().getLife() <= damage) {
                        setTargetPlayer(AllZone.getHumanPlayer());
                        return;
                    }

                    Card c = getFlying();
                    if ((c == null) || (!check.equals(c))) {
                        throw new RuntimeException(card + " error in chooseTargetAI() - Card c is " + c
                                + ",  Card check is " + check);
                    }

                    setTargetCard(c);
                }// chooseTargetAI()

                // uses "damage" variable
                Card getFlying() {
                    CardList flying = CardFactoryUtil.AI_getHumanCreature("Flying", card, true);
                    for (int i = 0; i < flying.size(); i++) {
                        if (flying.get(i).getNetDefense() <= damage)
                            return flying.get(i);
                    }

                    return null;
                }

                @Override
                public void resolve() {
                    int damage = getDamage();

                    if (getTargetCard() != null) {
                        if (AllZoneUtil.isCardInPlay(getTargetCard())
                                && CardFactoryUtil.canTarget(card, getTargetCard())) {
                            javax.swing.JOptionPane.showMessageDialog(null, "Erratic Explosion causes " + damage
                                    + " to " + getTargetCard());

                            Card c = getTargetCard();
                            c.addDamage(damage, card);
                        }
                    } else {
                        javax.swing.JOptionPane.showMessageDialog(null, "Erratic Explosion causes " + damage + " to "
                                + getTargetPlayer());
                        getTargetPlayer().addDamage(damage, card);
                    }
                }

                // randomly choose a nonland card
                int getDamage() {
                    CardList notLand = card.getController().getCardsIn(Zone.Library);
                    notLand = notLand.filter(CardListFilter.nonlands);
                    notLand.shuffle();

                    if (notLand.isEmpty()) {
                        return 0;
                    }

                    Card card = notLand.get(0);
                    return CardUtil.getConvertedManaCost(card.getSpellAbility()[0]);
                }
            };// SpellAbility
            card.addSpellAbility(spell);

            card.setSVar("PlayMain1", "TRUE");
        }// *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Martial Coup")) {

            Cost cost = new Cost(card.getManaCost(), cardName, false);
            SpellAbility spell = new Spell(card, cost, null) {

                private static final long serialVersionUID = -29101524966207L;

                @Override
                public void resolve() {
                    CardList all = AllZoneUtil.getCardsIn(Zone.Battlefield);
                    int Soldiers = card.getXManaCostPaid();
                    for (int i = 0; i < Soldiers; i++) {
                        CardFactoryUtil.makeToken("Soldier", "W 1 1 Soldier", card.getController(), "W", new String[] {
                                "Creature", "Soldier" }, 1, 1, new String[] { "" });
                    }
                    if (Soldiers >= 5) {
                        for (int i = 0; i < all.size(); i++) {
                            Card c = all.get(i);
                            if (c.isCreature()) {
                                AllZone.getGameAction().destroy(c);
                            }
                        }
                    }
                }// resolve()

                @Override
                public boolean canPlayAI() {
                    CardList human = AllZoneUtil.getCreaturesInPlay(AllZone.getHumanPlayer());
                    CardList computer = AllZoneUtil.getCreaturesInPlay(AllZone.getComputerPlayer());

                    // the computer will at least destroy 2 more human creatures
                    return (computer.size() < human.size() - 1 || (AllZone.getComputerPlayer().getLife() < 7 && !human
                            .isEmpty())) && ComputerUtil.getAvailableMana().size() >= 7;
                }
            };// SpellAbility

            card.addSpellAbility(spell);
        }// *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Parallel Evolution")) {
            SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = 3456160935845779623L;

                @Override
                public boolean canPlayAI() {
                    CardList humTokenCreats = AllZoneUtil.getCreaturesInPlay(AllZone.getHumanPlayer());
                    humTokenCreats = humTokenCreats.filter(CardListFilter.token);

                    CardList compTokenCreats = AllZoneUtil.getCreaturesInPlay(AllZone.getComputerPlayer());
                    compTokenCreats = compTokenCreats.filter(CardListFilter.token);

                    return compTokenCreats.size() > humTokenCreats.size();
                }// canPlayAI()

                @Override
                public void resolve() {
                    CardList tokens = AllZoneUtil.getCreaturesInPlay();
                    tokens = tokens.filter(CardListFilter.token);

                    CardFactoryUtil.copyTokens(tokens);

                }// resolve()
            };// SpellAbility

            spell.setDescription("For each creature token on the battlefield, its controller puts a token that's a copy of that creature onto the battlefield.");
            spell.setStackDescription("Parallel Evolution - For each creature token on the battlefield, its controller puts a token that's a copy of that creature onto the battlefield.");

            card.setFlashback(true);
            card.addSpellAbility(spell);
            card.addSpellAbility(CardFactoryUtil.ability_Flashback(card, "4 G G G"));
        }// *************** END ************ END **************************

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
                    String[] basic = { "Forest", "Plains", "Mountain", "Island", "Swamp" };

                    for (int i = 0; i < basic.length; i++) {
                        CardList cl = land.getType(basic[i]);
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
                        if (AllZoneUtil.isCardInPlay(target.get(i)) && !saveList.contains(target.get(i)))
                            AllZone.getGameAction().sacrifice(target.get(i));
                    }
                }// resolve()
            };// SpellAbility

            final Input input = new Input() {
                private static final long serialVersionUID = 1739423591445361917L;
                private int count;

                @Override
                public void showMessage() { // count is the current index we are
                                            // on.
                    // countBase[0] is the total number of basic land types the
                    // human has
                    // index[0] is the number to offset the index by
                    count = countBase[0] - index[0] - 1; // subtract by one
                                                         // since humanBasic is
                                                         // 0 indexed.
                    if (count < 0) {
                        // need to reset the variables in case they cancel this
                        // spell and it stays in hand.
                        humanBasic.clear();
                        countBase[0] = 0;
                        index[0] = 0;
                        stop();
                    } else {
                        AllZone.getDisplay().showMessage(
                                "Select target " + humanBasic.get(count) + " land to not sacrifice");
                        ButtonUtil.enableOnlyCancel();
                    }
                }

                @Override
                public void selectButtonCancel() {
                    stop();
                }

                @Override
                public void selectCard(final Card c, final PlayerZone zone) {
                    if (c.isLand() && zone.is(Constant.Zone.Battlefield) && c.getController().isHuman()
                    /* && c.getName().equals(humanBasic.get(count)) */
                    && c.isType(humanBasic.get(count))
                    /* && !saveList.contains(c) */) {
                        // get all other basic[count] lands human player
                        // controls and add them to target
                        CardList land = AllZoneUtil.getPlayerLandsInPlay(AllZone.getHumanPlayer());
                        CardList cl = land.getType(humanBasic.get(count));
                        cl = cl.filter(new CardListFilter() {
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
                        showMessage();

                        if (index[0] >= humanBasic.size()) {
                            stopSetNext(new Input_PayManaCost(spell));
                        }

                        // need to sacrifice the other non-basic land types
                        land = land.filter(new CardListFilter() {
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
                }// selectCard()
            };// Input

            Input runtime = new Input() {
                private static final long serialVersionUID = -122635387376995855L;

                @Override
                public void showMessage() {
                    countBase[0] = 0;
                    // figure out which basic land types the human has
                    // put those in an set to use later
                    CardList land = AllZone.getHumanPlayer().getCardsIn(Zone.Battlefield);
                    String[] basic = { "Forest", "Plains", "Mountain", "Island", "Swamp" };

                    for (int i = 0; i < basic.length; i++) {
                        CardList c = land.getType(basic[i]);
                        if (!c.isEmpty()) {
                            humanBasic.add(basic[i]);
                            countBase[0]++;
                        }
                    }
                    if (countBase[0] == 0) {
                        // human has no basic land, so don't prompt to select
                        // one.
                        stop();
                    } else {
                        index[0] = 0;
                        target.clear();
                        stopSetNext(input);
                    }
                }
            };// Input
            card.addSpellAbility(spell);
            spell.setBeforePayMana(runtime);
        }// *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Gerrard's Verdict")) {
            SpellAbility spell = new Spell(card) {

                private static final long serialVersionUID = 4734024742326763385L;

                @Override
                public boolean canPlayAI() {
                    PlayerZone humanHand = AllZone.getHumanPlayer().getZone(Constant.Zone.Hand);
                    if (humanHand.size() >= 2) {
                        return true;
                    } else {
                        return false;
                    }
                }

                @Override
                public void resolve() {
                    Player player = card.getController();
                    if (player.isHuman()) {
                        humanResolve();
                    } else {
                        computerResolve();
                    }
                }

                public void humanResolve() {
                    CardList list = AllZone.getComputerPlayer().getCardsIn(Zone.Hand);
                    list.shuffle();

                    if (list.size() == 0) {
                        return;
                    }

                    Card c1 = list.get(0);
                    list.remove(c1);
                    c1.getController().discard(c1, null);

                    if (list.size() == 0) {
                        return;
                    }

                    Card c2 = list.get(0);
                    list.remove(c2);

                    c2.getController().discard(c2, null);

                    if (c1.isLand()) {
                        AllZone.getHumanPlayer().gainLife(3, card);
                    }

                    if (c2.isLand()) {
                        AllZone.getHumanPlayer().gainLife(3, card);
                    }

                }// resolve()

                public void computerResolve() {
                    CardList list = AllZone.getHumanPlayer().getCardsIn(Zone.Hand);

                    if (list.size() > 0) {

                        Object o = GuiUtils.getChoiceOptional("First card to discard", list.toArray());

                        Card c = (Card) o;
                        list.remove(c);

                        c.getController().discard(c, null);

                        if (c.isLand()) {
                            AllZone.getComputerPlayer().gainLife(3, card);
                        }

                        if (list.size() > 0) {
                            Object o2 = GuiUtils.getChoiceOptional("Second card to discard", list.toArray());

                            Card c2 = (Card) o2;
                            list.remove(c2);

                            c2.getController().discard(c2, null);

                            if (c2.isLand()) {
                                AllZone.getComputerPlayer().gainLife(3, card);
                            }
                        }
                    }
                }
            };
            card.addSpellAbility(spell);
        }// *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Mind Funeral")) {
            Cost cost = new Cost("1 U B", cardName, false);
            Target tgt = new Target(card, "Select a Player", "Player");
            final SpellAbility spell = new Spell(card, cost, tgt) {
                private static final long serialVersionUID = 42470566751344693L;

                @Override
                public boolean canPlayAI() {
                    setTargetPlayer(AllZone.getHumanPlayer());
                    CardList libList = AllZone.getHumanPlayer().getCardsIn(Zone.Library);
                    return libList.size() > 0;
                }

                @Override
                public void resolve() {
                    Player player = getTargetPlayer();

                    CardList libList = player.getCardsIn(Zone.Library);

                    int numLands = libList.getType("Land").size();

                    int total = 0;
                    if (numLands > 3) { // if only 3 or less lands in the deck
                                        // everything is going
                        int landCount = 0;

                        for (Card c : libList) {
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
            };// SpellAbility
            card.addSpellAbility(spell);
        }// *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Haunting Echoes")) {
            Cost cost = new Cost("3 B B", cardName, false);
            Target tgt = new Target(card, "Select a Player", "Player");
            final SpellAbility spell = new Spell(card, cost, tgt) {
                private static final long serialVersionUID = 42470566751344693L;

                @Override
                public boolean canPlayAI() {
                    // Haunting Echoes shouldn't be cast if only basic land in
                    // graveyard or library is empty
                    CardList graveyard = AllZone.getHumanPlayer().getCardsIn(Zone.Graveyard);
                    CardList library = AllZone.getHumanPlayer().getCardsIn(Zone.Library);
                    int graveCount = graveyard.size();
                    graveyard = graveyard.filter(new CardListFilter() {
                        public boolean addCard(final Card c) {
                            return c.isBasicLand();
                        }
                    });

                    setTargetPlayer(AllZone.getHumanPlayer());

                    return ((graveCount - graveyard.size() > 0) && library.size() > 0);
                }

                @Override
                public void resolve() {
                    Player player = getTargetPlayer();

                    CardList grave = player.getCardsIn(Zone.Graveyard);
                    grave = grave.getNotType("Basic");

                    CardList lib = player.getCardsIn(Zone.Library);

                    for (Card c : grave) {
                        CardList remLib = lib.getName(c.getName());
                        for (Card rem : remLib) {
                            AllZone.getGameAction().exile(rem);
                            lib.remove(rem);
                        }
                        AllZone.getGameAction().exile(c);
                    }
                }
            };// SpellAbility
            card.addSpellAbility(spell);
        }// *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Donate")) {
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = 782912579034503349L;

                @Override
                public void resolve() {
                    Card c = getTargetCard();

                    if (c != null && AllZoneUtil.isCardInPlay(c) && CardFactoryUtil.canTarget(card, c)) {
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
                    CardList list = AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield, "Illusions of Grandeur");

                    if (list.size() > 0) {
                        setTargetCard(list.get(0));
                        return true;
                    }
                    return false;
                }
            };

            Input runtime = new Input() {
                private static final long serialVersionUID = -7823269301012427007L;

                @Override
                public void showMessage() {
                    CardList perms = AllZone.getHumanPlayer().getCardsIn(Zone.Battlefield);
                    perms = perms.filter(new CardListFilter() {
                        public boolean addCard(final Card c) {
                            return c.isPermanent() && !c.getName().equals("Mana Pool");
                        }
                    });

                    boolean free = false;
                    if (this.isFree()) {
                        free = true;
                    }

                    stopSetNext(CardFactoryUtil.input_targetSpecific(spell, perms, "Select a permanent you control",
                            true, free));

                }// showMessage()
            };// Input

            spell.setBeforePayMana(runtime);
            card.addSpellAbility(spell);
        }// *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Balance")) {
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = -5941893280103164961L;

                public void resolve() {
                    // Lands:
                    CardList humLand = AllZoneUtil.getPlayerLandsInPlay(AllZone.getHumanPlayer());
                    CardList compLand = AllZoneUtil.getPlayerLandsInPlay(AllZone.getComputerPlayer());

                    if (compLand.size() > humLand.size()) {
                        compLand.shuffle();
                        for (int i = 0; i < compLand.size() - humLand.size(); i++) {
                            AllZone.getGameAction().sacrifice(compLand.get(i));
                        }
                    } else if (humLand.size() > compLand.size()) {
                        int diff = humLand.size() - compLand.size();
                        AllZone.getInputControl().setInput(PlayerUtil.input_sacrificePermanents(diff, "Land"));
                    }

                    // Hand
                    CardList humHand = AllZone.getHumanPlayer().getCardsIn(Zone.Hand);
                    CardList compHand = AllZone.getComputerPlayer().getCardsIn(Zone.Hand);
                    int handDiff = Math.abs(humHand.size() - compHand.size());

                    if (compHand.size() > humHand.size()) {
                        AllZone.getComputerPlayer().discard(handDiff, this, false);
                    } else if (humHand.size() > compHand.size()) {
                        AllZone.getHumanPlayer().discard(handDiff, this, false);
                    }

                    // Creatures:
                    CardList humCreats = AllZoneUtil.getCreaturesInPlay(AllZone.getHumanPlayer());
                    CardList compCreats = AllZoneUtil.getCreaturesInPlay(AllZone.getComputerPlayer());

                    if (compCreats.size() > humCreats.size()) {
                        CardListUtil.sortAttackLowFirst(compCreats);
                        CardListUtil.sortCMC(compCreats);
                        compCreats.reverse();
                        for (int i = 0; i < compCreats.size() - humCreats.size(); i++) {
                            AllZone.getGameAction().sacrifice(compCreats.get(i));
                        }
                    } else if (humCreats.size() > compCreats.size()) {
                        int diff = humCreats.size() - compCreats.size();
                        AllZone.getInputControl().setInput(PlayerUtil.input_sacrificePermanents(diff, "Creature"));
                    }
                }

                public boolean canPlayAI() {
                    int diff = 0;
                    CardList humLand = AllZoneUtil.getPlayerLandsInPlay(AllZone.getHumanPlayer());
                    CardList compLand = AllZoneUtil.getPlayerLandsInPlay(AllZone.getComputerPlayer());
                    diff += humLand.size() - compLand.size();

                    CardList humCreats = AllZoneUtil.getCreaturesInPlay(AllZone.getHumanPlayer());
                    CardList compCreats = AllZoneUtil.getCreaturesInPlay(AllZone.getComputerPlayer());
                    compCreats = compCreats.getType("Creature");
                    diff += 1.5 * (humCreats.size() - compCreats.size());

                    CardList humHand = AllZone.getHumanPlayer().getCardsIn(Zone.Hand);
                    CardList compHand = AllZone.getComputerPlayer().getCardsIn(Zone.Hand);
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

                public void resolve() {
                    final Player thePlayer = card.getController();
                    thePlayer.addMaxLandsToPlay(3);

                    Command untilEOT = new Command() {
                        private static final long serialVersionUID = 1665720009691293263L;

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

                public void resolve() {
                    final Player thePlayer = card.getController();
                    thePlayer.addMaxLandsToPlay(1);

                    Command untilEOT = new Command() {
                        private static final long serialVersionUID = -2618916698575607634L;

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
        else if (cardName.equals("Brood Birthing")) {
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = -8303724057068847270L;

                public void resolve() {
                    int times = 1;
                    CardList cl;
                    if (card.getController().getCardsIn(Zone.Battlefield, "Eldrazi Spawn").size() > 0) {
                        times = 3;
                    }
                    for (int i = 0; i < times; i++) {
                        cl = CardFactoryUtil.makeToken("Eldrazi Spawn", "C 0 1 Eldrazi Spawn", card.getController(),
                                "", new String[] { "Creature", "Eldrazi", "Spawn" }, 0, 1, new String[] {});
                        for (Card crd : cl)
                            crd.addSpellAbility(CardFactoryUtil.getEldraziSpawnAbility(crd));
                    }
                }
            };
            StringBuilder sb = new StringBuilder();
            sb.append(cardName).append(" - ").append(card.getController());
            sb.append(" puts one or three 0/1 Eldrazi Spawn creature tokens onto the battlefield.");
            spell.setStackDescription(sb.toString());

            card.addSpellAbility(spell);
        }// *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Explosive Revelation")) {
            /*
             * Choose target creature or player. Reveal cards from the top of
             * your library until you reveal a nonland card. Explosive
             * Revelation deals damage equal to that card's converted mana cost
             * to that creature or player. Put the nonland card into your hand
             * and the rest on the bottom of your library in any order.
             */
            Cost cost = new Cost(card.getManaCost(), cardName, false);
            Target tgt = new Target(card, "CP");
            final SpellAbility spell = new Spell(card, cost, tgt) {
                private static final long serialVersionUID = -3234630801871872940L;

                int damage = 3;
                Card check;

                @Override
                public boolean canPlayAI() {
                    if (AllZone.getHumanPlayer().getLife() <= damage) {
                        return true;
                    }

                    check = getFlying();
                    return check != null;
                }

                @Override
                public void chooseTargetAI() {
                    if (AllZone.getHumanPlayer().getLife() <= damage) {
                        setTargetPlayer(AllZone.getHumanPlayer());
                        return;
                    }

                    Card c = getFlying();
                    if ((c == null) || (!check.equals(c))) {
                        throw new RuntimeException(card + " error in chooseTargetAI() - Card c is " + c
                                + ",  Card check is " + check);
                    }

                    setTargetCard(c);
                }// chooseTargetAI()

                // uses "damage" variable
                Card getFlying() {
                    CardList flying = CardFactoryUtil.AI_getHumanCreature("Flying", card, true);
                    for (int i = 0; i < flying.size(); i++) {
                        if (flying.get(i).getNetDefense() <= damage)
                            return flying.get(i);
                    }

                    return null;
                }

                @Override
                public void resolve() {

                    int damage = getDamage();

                    if (getTargetCard() != null) {
                        if (AllZoneUtil.isCardInPlay(getTargetCard())
                                && CardFactoryUtil.canTarget(card, getTargetCard())) {
                            javax.swing.JOptionPane.showMessageDialog(null, cardName + " causes " + damage + " to "
                                    + getTargetCard());

                            Card c = getTargetCard();
                            c.addDamage(damage, card);
                        }
                    } else {
                        javax.swing.JOptionPane.showMessageDialog(null, cardName + " causes " + damage + " to "
                                + getTargetPlayer());
                        getTargetPlayer().addDamage(damage, card);
                    }
                    // System.out.println("Library after: "+card.getController()).getCardsIn(Zone.Library);
                }

                int getDamage() {
                    /*
                     * Reveal cards from the top of your library until you
                     * reveal a nonland card.
                     */
                    CardList lib = card.getController().getCardsIn(Zone.Library);
                    Log.debug("Explosive Revelation", "Library before: " + lib);
                    CardList revealed = new CardList();
                    if (lib.size() > 0) {
                        int index = 0;
                        Card top;
                        do {
                            top = lib.get(index);
                            // System.out.println("Got from top of library:"+top);
                            index += 1;
                            revealed.add(top);
                        } while (index < lib.size() && top.isLand());
                        // Display the revealed cards
                        GuiUtils.getChoice("Revealed cards:", revealed.toArray());
                        // non-land card into hand
                        AllZone.getGameAction().moveToHand(revealed.get(revealed.size() - 1));
                        // put the rest of the cards on the bottom of library
                        for (int j = 0; j < revealed.size() - 1; j++) {
                            AllZone.getGameAction().moveToBottomOfLibrary(revealed.get(j));
                        }
                        // return the damage

                        // System.out.println("Explosive Revelation does "+CardUtil.getConvertedManaCost(top)+" from: "+top);
                        return CardUtil.getConvertedManaCost(top);
                    }
                    return 0;
                }
            };// SpellAbility
            card.addSpellAbility(spell);
        }// *************** END ************ END **************************

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
                    int humanLife = AllZone.getHumanPlayer().getLife();
                    if (maxX >= humanLife) {
                        targetPlayers.add(AllZone.getHumanPlayer());
                        return true;
                    }
                    return false;
                }

                @Override
                public void resolve() {
                    int damage = (card.getXManaCostPaid() - getNumTargets() + 1) / getNumTargets();
                    // add that much damage to each creature
                    // DEBUG
                    Log.debug("Fireball", "Fireball - damage to each target: " + damage);
                    Log.debug("Fireball", "Fireball - card targets: ");
                    printCardTargets();
                    Log.debug("Fireball", "Fireball - player targets: ");
                    printPlayerTargets();
                    if (card.getController().isComputer()) {
                        StringBuilder sb = new StringBuilder();
                        sb.append(cardName + " - Computer causes " + damage + " to:\n\n");
                        for (int i = 0; i < targets.size(); i++) {
                            Card target = targets.get(i);
                            if (AllZoneUtil.isCardInPlay(target) && CardFactoryUtil.canTarget(card, target)) {
                                sb.append(target + "\n");
                            }
                        }
                        for (int i = 0; i < targetPlayers.size(); i++) {
                            Player p = targetPlayers.get(i);
                            if (p.canTarget(this)) {
                                sb.append(p + "\n");
                            }
                        }
                        javax.swing.JOptionPane.showMessageDialog(null, sb.toString());
                    }
                    for (int i = 0; i < targets.size(); i++) {
                        Card target = targets.get(i);
                        if (AllZoneUtil.isCardInPlay(target) && CardFactoryUtil.canTarget(card, target)) {
                            // DEBUG
                            Log.debug("Fireball", "Fireball does " + damage + " to: " + target);
                            target.addDamage(damage, card);
                        }
                    }
                    for (int i = 0; i < targetPlayers.size(); i++) {
                        Player p = targetPlayers.get(i);
                        if (p.canTarget(this)) {
                            // DEBUG
                            Log.debug("Fireball", "Fireball does " + damage + " to: " + p);
                            p.addDamage(damage, card);
                        }
                    }
                }// resolve()

                // DEBUG
                private void printCardTargets() {
                    StringBuilder sb = new StringBuilder("[");
                    for (Card target : targets) {
                        sb.append(target).append(",");
                    }
                    sb.append("]");
                    Log.debug("Fireball", sb.toString());
                }

                // DEBUG
                private void printPlayerTargets() {
                    StringBuilder sb = new StringBuilder("[");
                    for (Player p : targetPlayers) {
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

            };// SpellAbility

            final Input input = new Input() {
                private static final long serialVersionUID = 1099272655273322957L;

                @Override
                public void showMessage() {
                    AllZone.getDisplay().showMessage(
                            "Select target creatures and/or players.  Currently, " + getNumTargets()
                                    + " targets.  Click OK when done.");
                }

                private int getNumTargets() {
                    int numTargets = 0;
                    numTargets += targets.size();
                    numTargets += targetPlayers.size();
                    // DEBUG
                    Log.debug("Fireball", "Fireball - numTargets = " + numTargets);
                    return numTargets;
                }

                @Override
                public void selectButtonCancel() {
                    targets.clear();
                    targetPlayers.clear();
                    stop();
                }

                @Override
                public void selectButtonOK() {
                    spell.setStackDescription(cardName + " deals X damage to " + getNumTargets() + " target(s).");
                    stopSetNext(new Input_PayManaCost(spell));
                }

                @Override
                public void selectCard(final Card c, final PlayerZone zone) {
                    if (!CardFactoryUtil.canTarget(card, c)) {
                        AllZone.getDisplay().showMessage("Cannot target this card.");
                        return; // cannot target
                    }
                    if (targets.contains(c)) {
                        AllZone.getDisplay().showMessage("You have already selected this target.");
                        return; // cannot target the same creature twice.
                    }

                    if (c.isCreature() && zone.is(Constant.Zone.Battlefield)) {
                        targets.add(c);
                        showMessage();
                    }
                }// selectCard()

                @Override
                public void selectPlayer(final Player player) {
                    if (!player.canTarget(spell)) {
                        AllZone.getDisplay().showMessage("Cannot target this player.");
                        return; // cannot target
                    }
                    if (targetPlayers.contains(player)) {
                        AllZone.getDisplay().showMessage("You have already selected this player.");
                        return; // cannot target the same player twice.
                    }
                    targetPlayers.add(player);
                    showMessage();
                }
            };// Input

            card.addSpellAbility(spell);
            spell.setBeforePayMana(input);
        }// *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Recall")) {
            /*
             * Discard X cards, then return a card from your graveyard to your
             * hand for each card discarded this way. Exile Recall.
             */
            Cost cost = new Cost(card.getManaCost(), cardName, false);
            final SpellAbility spell = new Spell(card, cost, null) {
                private static final long serialVersionUID = -3935814273439962834L;

                @Override
                public boolean canPlayAI() {
                    // for compy to play this wisely, it should check hand, and
                    // if there
                    // are no spells that canPlayAI(), then use recall. maybe.
                    return false;
                }

                @Override
                public void resolve() {
                    int numCards = card.getXManaCostPaid();
                    final Player player = card.getController();
                    int maxCards = player.getCardsIn(Zone.Hand).size();
                    if (numCards != 0) {
                        numCards = Math.min(numCards, maxCards);
                        if (player.isHuman()) {
                            AllZone.getInputControl().setInput(
                                    CardFactoryUtil.input_discardRecall(numCards, card, this));
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
                }// resolve()
            };// SpellAbility

            StringBuilder sb = new StringBuilder();
            sb.append(card.getName()).append(" - discard X cards and return X cards to your hand.");
            spell.setStackDescription(sb.toString());

            card.addSpellAbility(spell);
        }// *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Windfall")) {
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = -7707012960887790709L;

                @Override
                public boolean canPlayAI() {
                    /*
                     * We want compy to have less cards in hand than the human
                     */
                    CardList Hhand = AllZone.getHumanPlayer().getCardsIn(Zone.Hand);
                    CardList Chand = AllZone.getComputerPlayer().getCardsIn(Zone.Hand);
                    return Chand.size() < Hhand.size();
                }

                @Override
                public void resolve() {
                    CardList Hhand = AllZone.getHumanPlayer().getCardsIn(Zone.Hand);
                    CardList Chand = AllZone.getComputerPlayer().getCardsIn(Zone.Hand);

                    int num = Math.max(Hhand.size(), Chand.size());

                    discardDraw(AllZone.getHumanPlayer(), num);
                    discardDraw(AllZone.getComputerPlayer(), num);
                }// resolve()

                void discardDraw(final Player player, final int num) {
                    player.discardHand(this);
                    player.drawCards(num);
                }
            };// SpellAbility
            card.addSpellAbility(spell);
        }// *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Stitch Together")) {
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = -57996914115026814L;

                @Override
                public void resolve() {
                    CardList threshold = card.getController().getCardsIn(Zone.Graveyard);
                    Card c = getTargetCard();

                    if (threshold.size() >= 7) {
                        if (card.getController().getZone(Zone.Graveyard).contains(c)) {
                            PlayerZone play = card.getController().getZone(Constant.Zone.Battlefield);
                            AllZone.getGameAction().moveTo(play, c);
                        }
                    } else {
                        if (card.getController().getZone(Zone.Graveyard).contains(c)) {
                            PlayerZone hand = card.getController().getZone(Constant.Zone.Hand);
                            AllZone.getGameAction().moveTo(hand, c);
                        }
                    }
                }// resolve()

                @Override
                public boolean canPlay() {
                    return getCreatures().length != 0;
                }

                public boolean canPlayAI() {
                    CardList check = card.getController().getCardsIn(Zone.Graveyard);
                    return getCreaturesAI().length != 0 || check.size() >= 7;
                }

                public Card[] getCreatures() {
                    CardList creature = card.getController().getCardsIn(Zone.Graveyard).getType("Creature");
                    return creature.toArray();
                }

                public Card[] getCreaturesAI() {
                    CardList creature = card.getController().getCardsIn(Zone.Graveyard).getType("Creature");
                    creature = creature.filter(new CardListFilter() {
                        public boolean addCard(final Card c) {
                            return c.getNetAttack() > 4;
                        }
                    });
                    return creature.toArray();
                }

                @Override
                public void chooseTargetAI() {
                    Card[] c = getCreatures();
                    Card biggest = c[0];
                    for (int i = 0; i < c.length; i++) {
                        if (biggest.getNetAttack() < c[i].getNetAttack())
                            biggest = c[i];
                    }

                    setTargetCard(biggest);
                }
            };// SpellAbility
            card.addSpellAbility(spell);

            Input target = new Input() {
                private static final long serialVersionUID = -3717723884199321767L;

                @Override
                public void showMessage() {
                    CardList creature = card.getController().getCardsIn(Zone.Graveyard).getType("Creature");
                    Object check = GuiUtils.getChoiceOptional("Select creature", creature);
                    if (check != null) {
                        spell.setTargetCard((Card) check);
                        stopSetNext(new Input_PayManaCost(spell));
                    } else {
                        stop();
                    }
                }// showMessage()
            };// Input
            spell.setBeforePayMana(target);
        }// *************** END ************ END **************************

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
                            // TODO: some more input validation,
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

                    HashMap<String, Integer> countInGraveyard = new HashMap<String, Integer>();
                    CardList allGrave = AllZone.getComputerPlayer().getCardsIn(Constant.Zone.Graveyard);
                    allGrave.getType("Creature");
                    for (Card c : allGrave) {
                        for (String type : c.getType()) {
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
                    for (Entry<String, Integer> entry : countInGraveyard.entrySet()) {
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
                    for (Card c : bidded) {
                        if (c.isType(input[1]) || (!input[0].equals("") && c.isType(input[0]))) {
                            AllZone.getGameAction().moveToPlay(c);
                        }
                    }
                }// resolve()
            };// SpellAbility
            card.addSpellAbility(spell);

            StringBuilder sb = new StringBuilder();
            sb.append(card.getName()).append(" - choose a creature type.");
            spell.setStackDescription(sb.toString());
        }// *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Leeches")) {
            /*
             * Target player loses all poison counters. Leeches deals that much
             * damage to that player.
             */
            final Target tgt = new Target(card, "Select target player", "Player");
            Cost cost = new Cost("1 W W", cardName, false);
            SpellAbility spell = new Spell(card, cost, tgt) {
                private static final long serialVersionUID = 8555498267738686288L;

                @Override
                public void resolve() {
                    Player p = tgt.getTargetPlayers().get(0);
                    int counters = p.getPoisonCounters();
                    p.addDamage(counters, card);
                    p.subtractPoisonCounters(counters);
                }// resolve()

                @Override
                public boolean canPlayAI() {
                    int humanPoison = AllZone.getHumanPlayer().getPoisonCounters();
                    int compPoison = AllZone.getComputerPlayer().getPoisonCounters();

                    if (AllZone.getHumanPlayer().getLife() <= humanPoison) {
                        tgt.addTarget(AllZone.getHumanPlayer());
                        return true;
                    }

                    if ((2 * (11 - compPoison) < AllZone.getComputerPlayer().getLife() || compPoison > 7)
                            && compPoison < AllZone.getComputerPlayer().getLife() - 2) {
                        tgt.addTarget(AllZone.getComputerPlayer());
                        return true;
                    }

                    return false;
                }
            };// SpellAbility
            card.addSpellAbility(spell);
        }// *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Sanity Grinding")) {
            /*
             * Chroma - Reveal the top ten cards of your library. For each blue
             * mana symbol in the mana costs of the revealed cards, target
             * opponent puts the top card of his or her library into his or her
             * graveyard. Then put the cards you revealed this way on the bottom
             * of your library in any order.
             */
            SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = 4475834103787262421L;

                @Override
                public void resolve() {
                    Player player = card.getController();
                    Player opp = player.getOpponent();
                    PlayerZone lib = card.getController().getZone(Constant.Zone.Library);
                    int maxCards = lib.size();
                    maxCards = Math.min(maxCards, 10);
                    if (maxCards == 0) {
                        return;
                    }
                    CardList topCards = new CardList();
                    // show top n cards:
                    for (int j = 0; j < maxCards; j++) {
                        topCards.add(lib.get(j));
                    }
                    final int num = CardFactoryUtil.getNumberOfManaSymbolsByColor("U", topCards);
                    GuiUtils.getChoiceOptional("Revealed cards - " + num + " U mana symbols", topCards.toArray());

                    // opponent moves this many cards to graveyard
                    opp.mill(num);

                    // then, move revealed cards to bottom of library
                    for (Card c : topCards) {
                        AllZone.getGameAction().moveToBottomOfLibrary(c);
                    }
                }// resolve()

                @Override
                public boolean canPlayAI() {
                    return !AllZone.getComputerPlayer().getZone(Zone.Library).isEmpty();
                }

            };// SpellAbility
            card.addSpellAbility(spell);
        }// *************** END ************ END **************************

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
                    discardDrawX(AllZone.getHumanPlayer());
                    discardDrawX(AllZone.getComputerPlayer());
                }// resolve()

                void discardDrawX(final Player player) {
                    CardList hand = player.getCardsIn(Zone.Hand);

                    for (Card c : hand)
                        AllZone.getGameAction().moveToLibrary(c);

                    // Shuffle library
                    player.shuffle();

                    player.drawCards(hand.size());
                }

                // Simple, If computer has two or less playable cards remaining
                // in hand play Winds of Change
                @Override
                public boolean canPlayAI() {
                    CardList c = AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield);
                    c = c.filter(CardListFilter.nonlands);
                    return 2 >= c.size();
                }

            };// SpellAbility
            card.addSpellAbility(spell);
        }// *************** END ************ END **************************

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
                    Player player = card.getController();
                    Player opp = player.getOpponent();
                    discardDraw(AllZone.getHumanPlayer());
                    discardDraw(AllZone.getComputerPlayer());

                    if (player.hasMetalcraft()) {
                        opp.addDamage(opp.getNumDrawnThisTurn(), card);
                    }
                }// resolve()

                void discardDraw(final Player player) {
                    CardList hand = player.getCardsIn(Zone.Hand);
                    int numDraw = hand.size();

                    // move hand to library
                    for (Card c : hand) {
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
                    c = c.filter(CardListFilter.nonlands);
                    return 2 >= c.size()
                            || (AllZone.getComputerPlayer().hasMetalcraft() && AllZone.getHumanPlayer().getLife() <= 3);
                }

            };// SpellAbility
            card.addSpellAbility(spell);
        }// *************** END ************ END **************************

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
                    Player player = card.getController();
                    for (Card c : player.getCardsIn(Zone.Graveyard))
                        AllZone.getGameAction().moveToHand(c);

                    AllZone.getGameAction().exile(card);

                    card.setSVar("HSStamp", "" + Player.getHandSizeStamp());
                    player.addHandSizeOperation(new HandSizeOp("=", -1, Integer.parseInt(card.getSVar("HSStamp"))));
                }
            };// SpellAbility
            card.addSpellAbility(spell);
        }// *************** END ************ END **************************

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
                    "Return target creature card with converted mana cost X or less from your graveyard to the battlefield",
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
                                setTargetPlayer(ab0player[0]);
                                if (getTargetPlayer().canTarget(this)) {
                                    getTargetPlayer().addDamage(x[0], card);
                                }
                            }
                        }
                    }

                    // "Return target creature card with converted mana cost X or less from your graveyard to the battlefield",
                    if (userChoice.contains(cardChoice[1]) || card.getChoices().contains(cardChoice[1])) {
                        Card c = ab1card[0];
                        if (c != null) {
                            if (card.getController().getZone(Zone.Graveyard).contains(c)
                                    && CardFactoryUtil.canTarget(card, c)) {
                                AllZone.getGameAction().moveToPlay(c);
                            }
                        }
                    }

                    // "Target creature gets -X/-X until end of turn",
                    for (int i = 0; i < card.getChoices().size(); i++) {
                        if (card.getChoice(i).equals(cardChoice[2])) {
                            final Card c = ab2card[0];
                            if (c != null) {
                                if (AllZoneUtil.isCardInPlay(c) && CardFactoryUtil.canTarget(card, c)) {
                                    final int boost = x[0] * -1;
                                    c.addTempAttackBoost(boost);
                                    c.addTempDefenseBoost(boost);
                                    final Command untilEOT = new Command() {
                                        private static final long serialVersionUID = -6010783402521993651L;

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
                    }// end ab[2]

                    // "Up to X target creatures gain fear until end of turn"
                    if (userChoice.contains(cardChoice[3]) || card.getChoices().contains(cardChoice[3])) {
                        ArrayList<Card> cs = new ArrayList<Card>();
                        cs.addAll(ab3cards);
                        for (final Card c : cs) {
                            if (AllZoneUtil.isCardInPlay(c) && CardFactoryUtil.canTarget(card, c)) {
                                c.addExtrinsicKeyword("Fear");
                                final Command untilEOT = new Command() {
                                    private static final long serialVersionUID = 986259855862338866L;

                                    public void execute() {
                                        if (AllZoneUtil.isCardInPlay(c)) {
                                            c.removeExtrinsicKeyword("Fear");
                                        }
                                    }
                                };
                                AllZone.getEndOfTurn().addUntil(untilEOT);
                            }
                        }
                    }// end ab[3]
                }// resolve()

                @Override
                public boolean canPlayAI() {
                    return false;
                }
            };// SpellAbility

            final Command setStackDescription = new Command() {
                private static final long serialVersionUID = 5840471361149632482L;

                public void execute() {
                    ArrayList<String> a = new ArrayList<String>();
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

                    String s = a.get(0) + ", " + a.get(1);
                    spell.setStackDescription(card.getName() + " - " + s);
                }
            };// Command

            // for ab[3] - X creatures gain fear until EOT
            final Input targetXCreatures = new Input() {
                private static final long serialVersionUID = 2584765431286321048L;

                int stop = 0;
                int count = 0;

                @Override
                public void showMessage() {
                    if (count == 0) {
                        stop = x[0];
                    }
                    AllZone.getDisplay().showMessage(
                            cardName + " - Select a target creature to gain Fear (up to " + (stop - count) + " more)");
                    ButtonUtil.enableAll();
                }

                @Override
                public void selectButtonCancel() {
                    stop();
                }

                @Override
                public void selectButtonOK() {
                    done();
                }

                @Override
                public void selectCard(final Card c, final PlayerZone zone) {
                    if (c.isCreature() && zone.is(Constant.Zone.Battlefield) && CardFactoryUtil.canTarget(card, c)
                            && !ab3cards.contains(c)) {
                        ab3cards.add(c);
                        count++;
                        if (count == stop) {
                            done();
                        } else {
                            showMessage();
                        }
                    }
                }// selectCard()

                private void done() {
                    setStackDescription.execute();
                    stopSetNext(new Input_PayManaCost(spell));
                }
            };

            // for ab[2] target creature gets -X/-X
            final Input targetCreature = new Input() {
                private static final long serialVersionUID = -6879692803780014943L;

                @Override
                public void showMessage() {
                    AllZone.getDisplay().showMessage(cardName + " - Select target creature to get -X/-X");
                    ButtonUtil.enableOnlyCancel();
                }

                @Override
                public void selectButtonCancel() {
                    stop();
                }

                @Override
                public void selectCard(final Card c, final PlayerZone zone) {
                    if (c.isCreature() && zone.is(Constant.Zone.Battlefield) && CardFactoryUtil.canTarget(card, c)) {
                        if (card.isCopiedSpell()) {
                            card.getChoiceTargets().remove(0);
                        }
                        ab2card[0] = c;
                        // spell.setTargetCard(c);
                        card.setSpellChoiceTarget(String.valueOf(c.getUniqueNumber()));
                        setStackDescription.execute();

                        if (userChoice.contains(cardChoice[3]) || card.getChoices().contains(cardChoice[3])) {
                            stopSetNext(targetXCreatures);
                        } else {
                            System.out.println("Input_PayManaCost for spell is getting: " + spell.getManaCost());
                            stopSetNext(new Input_PayManaCost(spell));
                        }
                    }// if
                }// selectCard()
            };// Input targetCreature

            // for ab[1] - return creature from grave to the battlefield
            final Input targetGraveCreature = new Input() {
                private static final long serialVersionUID = -7558252187229252725L;

                @Override
                public void showMessage() {
                    CardList grave = card.getController().getCardsIn(Constant.Zone.Graveyard);
                    grave = grave.filter(CardListFilter.creatures);
                    grave = grave.filter(new CardListFilter() {
                        public boolean addCard(final Card c) {
                            return c.getCMC() <= x[0];
                        }
                    });

                    Object check = GuiUtils.getChoiceOptional("Select target creature with CMC < X", grave.toArray());
                    if (check != null) {
                        Card c = (Card) check;
                        if (CardFactoryUtil.canTarget(card, c)) {
                            ab1card[0] = c;
                        }
                    } else {
                        stop();
                    }

                    done();
                }// showMessage()

                public void done() {
                    if (userChoice.contains(cardChoice[2]) || card.getChoices().contains(cardChoice[2])) {
                        stopSetNext(targetCreature);
                    } else if (userChoice.contains(cardChoice[3]) || card.getChoices().contains(cardChoice[3])) {
                        stopSetNext(targetXCreatures);
                    } else {
                        stopSetNext(new Input_PayManaCost(spell));
                    }
                }
            };// Input

            // for ab[0] - target player loses X life
            final Input targetPlayer = new Input() {
                private static final long serialVersionUID = 9101387253945650303L;

                @Override
                public void showMessage() {
                    AllZone.getDisplay().showMessage(cardName + " - Select target player to lose life");
                    ButtonUtil.enableOnlyCancel();
                }

                @Override
                public void selectButtonCancel() {
                    stop();
                }

                @Override
                public void selectPlayer(final Player player) {
                    if (player.canTarget(spell)) {
                        if (card.isCopiedSpell()) {
                            card.getChoiceTargets().remove(0);
                        }
                        ab0player[0] = player;
                        // spell.setTargetPlayer(player);
                        card.setSpellChoiceTarget(player.toString());
                        setStackDescription.execute();

                        if (userChoice.contains(cardChoice[1]) || card.getChoices().contains(cardChoice[1])) {
                            stopSetNext(targetGraveCreature);
                        } else if (userChoice.contains(cardChoice[2]) || card.getChoices().contains(cardChoice[2])) {
                            stopSetNext(targetCreature);
                        } else if (userChoice.contains(cardChoice[3]) || card.getChoices().contains(cardChoice[3])) {
                            stopSetNext(targetXCreatures);
                        } else {
                            stopSetNext(new Input_PayManaCost(spell));
                        }
                    }
                }// selectPlayer()
            };// Input targetPlayer

            final Input chooseX = new Input() {
                private static final long serialVersionUID = 5625588008756700226L;

                @Override
                public void showMessage() {
                    if (card.isCopiedSpell()) {
                        x[0] = 0;
                        if (userChoice.contains(cardChoice[0])) {
                            stopSetNext(targetPlayer);
                        } else if (userChoice.contains(cardChoice[1])) {
                            stopSetNext(targetGraveCreature);
                        } else if (userChoice.contains(cardChoice[2])) {
                            stopSetNext(targetCreature);
                        } else if (userChoice.contains(cardChoice[3])) {
                            stopSetNext(targetXCreatures);
                        } else {
                            throw new RuntimeException(
                                    "Something in if(isCopiedSpell()) in Profane Command selection is FUBAR.");
                        }
                    } else {
                        ArrayList<String> choices = new ArrayList<String>();
                        for (int i = 0; i <= card.getController().getLife(); i++) {
                            choices.add("" + i);
                        }
                        Object o = GuiUtils.getChoice("Choose X", choices.toArray());
                        // everything stops here if user cancelled
                        if (o == null) {
                            stop();
                            return;
                        }

                        String answer = (String) o;

                        x[0] = Integer.parseInt(answer);
                        spell.setManaCost(x[0] + " B B");
                        spell.setIsXCost(false);

                        if (userChoice.contains(cardChoice[0])) {
                            stopSetNext(targetPlayer);
                        } else if (userChoice.contains(cardChoice[1])) {
                            stopSetNext(targetGraveCreature);
                        } else if (userChoice.contains(cardChoice[2])) {
                            stopSetNext(targetCreature);
                        } else if (userChoice.contains(cardChoice[3])) {
                            stopSetNext(targetXCreatures);
                        } else {
                            throw new RuntimeException("Something in Profane Command selection is FUBAR.");
                        }
                    }
                }// showMessage()
            };// Input chooseX

            Input chooseTwoInput = new Input() {
                private static final long serialVersionUID = 5625588008756700226L;

                @Override
                public void showMessage() {
                    if (card.isCopiedSpell()) {
                        if (userChoice.contains(cardChoice[0])) {
                            stopSetNext(targetPlayer);
                        } else if (userChoice.contains(cardChoice[1])) {
                            stopSetNext(targetGraveCreature);
                        } else if (userChoice.contains(cardChoice[2])) {
                            stopSetNext(targetCreature);
                        } else if (userChoice.contains(cardChoice[3])) {
                            stopSetNext(targetXCreatures);
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

                        ArrayList<String> display = new ArrayList<String>();

                        // get all
                        CardList creatures = AllZoneUtil.getCreaturesInPlay();
                        CardList grave = card.getController().getCardsIn(Zone.Graveyard);
                        grave = grave.filter(CardListFilter.creatures);

                        if (AllZone.getHumanPlayer().canTarget(spell) || AllZone.getComputerPlayer().canTarget(spell)) {
                            display.add("Target player loses X life");
                        }
                        if (grave.size() > 0) {
                            display.add("Return target creature card with converted mana cost X or less from your graveyard to the battlefield");
                        }
                        if (creatures.size() > 0) {
                            display.add("Target creature gets -X/-X until end of turn");
                        }
                        display.add("Up to X target creatures gain fear until end of turn");

                        ArrayList<String> a = chooseTwo(display);
                        // everything stops here if user cancelled
                        if (a == null) {
                            stop();
                            return;
                        }
                        userChoice.addAll(a);

                        stopSetNext(chooseX);
                    }
                }// showMessage()

                private ArrayList<String> chooseTwo(final ArrayList<String> choices) {
                    ArrayList<String> out = new ArrayList<String>();
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
                }// chooseTwo()
            };// Input chooseTwoInput
            card.addSpellAbility(spell);

            card.setSpellWithChoices(true);
            spell.setBeforePayMana(chooseTwoInput);
        }// *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Turn to Slag")) {
            Cost abCost = new Cost("3 R R", cardName, false);
            Target target = new Target(card, "Select target creature", "Creature".split(","));
            final SpellAbility spell = new Spell(card, abCost, target) {
                private static final long serialVersionUID = 3848014348910653252L;

                @Override
                public boolean canPlayAI() {
                    return false;
                }

                @Override
                public void resolve() {
                    Card tgt = getTargetCard();
                    if (AllZoneUtil.isCardInPlay(tgt) && CardFactoryUtil.canTarget(card, tgt)) {
                        tgt.addDamage(5, card);
                        CardList equipment = new CardList(tgt.getEquippedBy());
                        for (Card eq : equipment)
                            AllZone.getGameAction().destroy(eq);
                    }
                }// resolve()
            };// SpellAbility

            spell.setDescription(cardName
                    + " deals 5 damage to target creature. Destroy all Equipment attached to that creature.");

            card.addSpellAbility(spell);
        }// *************** END ************ END **************************

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

            Cost abCost = new Cost("U U", cardName, false);
            final SpellAbility spell = new Spell(card, abCost, null) {
                private static final long serialVersionUID = -8497142072380944393L;

                @Override
                public boolean canPlayAI() {
                    return false;
                }

                @Override
                public void resolve() {
                    Player p = card.getController();
                    int baseCMC = -1;
                    final Card[] newArtifact = new Card[1];

                    // Sacrifice an artifact
                    CardList arts = p.getCardsIn(Constant.Zone.Battlefield);
                    arts = arts.filter(CardListFilter.artifacts);
                    Object toSac = GuiUtils.getChoiceOptional("Sacrifice an artifact", arts.toArray());
                    if (toSac != null) {
                        Card c = (Card) toSac;
                        baseCMC = CardUtil.getConvertedManaCost(c);
                        AllZone.getGameAction().sacrifice(c);
                    } else {
                        return;
                    }

                    // Search your library for an artifact
                    CardList lib = p.getCardsIn(Zone.Library);
                    GuiUtils.getChoiceOptional("Looking at Library", lib.toArray());
                    CardList libArts = lib.filter(CardListFilter.artifacts);
                    Object o = GuiUtils.getChoiceOptional("Search for artifact", libArts.toArray());
                    if (o != null) {
                        newArtifact[0] = (Card) o;
                    } else {
                        return;
                    }

                    int newCMC = CardUtil.getConvertedManaCost(newArtifact[0]);

                    // if <= baseCMC, put it onto the battlefield
                    if (newCMC <= baseCMC) {
                        AllZone.getGameAction().moveToPlay(newArtifact[0]);
                    } else {
                        String diffCost = String.valueOf(newCMC - baseCMC);
                        AllZone.getInputControl().setInput(new Input_PayManaCost_Ability(diffCost, new Command() {
                            private static final long serialVersionUID = -8729850321341068049L;

                            public void execute() {
                                AllZone.getGameAction().moveToPlay(newArtifact[0]);
                            }
                        }, new Command() {
                            private static final long serialVersionUID = -246036834856971935L;

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
    }// getCard
}