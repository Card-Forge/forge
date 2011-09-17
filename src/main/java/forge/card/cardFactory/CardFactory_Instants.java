package forge.card.cardFactory;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.ButtonUtil;
import forge.Card;
import forge.CardUtil;
import forge.CardList;
import forge.CardListFilter;
import forge.CardListUtil;
import forge.Combat;
import forge.Command;
import forge.ComputerUtil;
import forge.Constant;
import forge.MyRandom;
import forge.PhaseUtil;
import forge.Player;
import forge.PlayerZone;
import forge.card.cost.Cost;
import forge.card.spellability.*;
import forge.gui.GuiUtils;
import forge.gui.input.Input;
import forge.gui.input.Input_PayManaCost;

import javax.swing.*;
import java.util.ArrayList;


/**
 * <p>CardFactory_Instants class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class CardFactory_Instants {

    /**
     * <p>getCard.</p>
     *
     * @param card a {@link forge.Card} object.
     * @param cardName a {@link java.lang.String} object.
     * @param owner a {@link forge.Player} object.
     * @return a {@link forge.Card} object.
     */
    public static Card getCard(final Card card, final String cardName) {


        //*************** START *********** START **************************
        if (cardName.equals("Brave the Elements")) {
            /**
             *  This card now works slightly better than it did before the spAllPump
             *  keyword was created. The AI is too simple and needs some work.
             */
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = -7998437920995642451L;

                @Override
                public boolean canPlayAI() {
                    return getAttacker() != null;
                }

                public Card getAttacker() {
                    // target creatures that is going to attack
                    Combat c = ComputerUtil.getAttackers();
                    Card[] att = c.getAttackers();

                    // Effect best used on at least a couple creatures
                    if (att.length > 1) {
                        return att[0];
                    } else {
                        return null;
                    }
                } //getAttacker()

                String getKeywordBoost() {
                    String theColor = getChosenColor();
                    return "Protection from " + theColor;
                } //getKeywordBoost()

                String getChosenColor() {
                    // Choose color for protection in Brave the Elements
                    String color = "Black";
                    if (card.getController().isHuman()) {

                        // String[] colors = Constant.Color.Colors;
                        // colors[colors.length-1] = null;

                        // You can no longer choose to gain "protection from null".
                        String[] colors = Constant.Color.onlyColors;

                        Object o = GuiUtils.getChoice("Choose color", colors);
                        color = (String) o;
                    } else {
                        if (getAttacker() != null) {
                        color = getAttacker().getColor().get(0).toString();
                        }
                    }
                    return color;
                } // getChosenColor

                @Override
                public void resolve() {
                    final String kboost = getKeywordBoost();

                    CardList list = AllZoneUtil.getPlayerCardsInPlay(card.getController());
                    list = list.filter(AllZoneUtil.white);

                    for (int i = 0; i < list.size(); i++) {
                        final Card[] target = new Card[1];
                        target[0] = list.get(i);

                        final Command untilEOT = new Command() {
                            private static final long serialVersionUID = 6308754740309909072L;

                            public void execute() {
                                if (AllZoneUtil.isCardInPlay(target[0])) {
                                    target[0].removeExtrinsicKeyword(kboost);
                                }
                            }
                        }; //Command

                        if (AllZoneUtil.isCardInPlay(target[0]) && !target[0].hasKeyword(kboost)) {
                            target[0].addExtrinsicKeyword(kboost);

                            AllZone.getEndOfTurn().addUntil(untilEOT);
                        } //if
                    } //for
                } //resolve
            }; //SpellAbility

            card.setSVar("PlayMain1", "TRUE");

            card.addSpellAbility(spell);
        } //*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Sprout Swarm")) {
            final SpellAbility spell_one = new Spell(card) {
                private static final long serialVersionUID = -609007714604161377L;

                @Override
                public boolean canPlayAI() {
                    return false;
                }

                @Override
                public void resolve() {
                    CardFactoryUtil.makeTokenSaproling(card.getController());
                }
            }; //SpellAbility

            final SpellAbility spell_two = new Spell(card) {
                private static final long serialVersionUID = -1387385820860395676L;

                @Override
                public void resolve() {
                    CardFactoryUtil.makeTokenSaproling(card.getController());
                    //return card to the hand
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
                    AllZone.getGameAction().moveTo(hand, card);
                }
            }; //SpellAbility

            spell_one.setManaCost("1 G");
            spell_two.setManaCost("4 G");
            spell_two.setAdditionalManaCost("3");

            spell_one.setDescription("Put a 1/1 green Saproling token onto the battlefield.");
            spell_two.setDescription("Buyback 3 (You may pay an additional 3 as you cast this spell. If you do, put this card into your hand as it resolves.)");

            spell_one.setStackDescription("Sprout Swarm - Put a 1/1 green Saproling token onto the battlefield");
            spell_two.setStackDescription("Sprout Swarm - Buyback, Put a 1/1 green Saproling token onto the battlefield");

            spell_two.setIsBuyBackAbility(true);

            card.addSpellAbility(spell_one);
            card.addSpellAbility(spell_two);
        } //*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Fact or Fiction")) {
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = 1481112451519L;

                @Override
                public void resolve() {

                    Card choice = null;

                    //check for no cards in hand on resolve
                    PlayerZone library = AllZone.getZone(Constant.Zone.Library, card.getController());
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
                    //PlayerZone Grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
                    CardList cards = new CardList();

                    if (library.size() == 0) {
                        JOptionPane.showMessageDialog(null, "No more cards in library.", "", JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }
                    int Count = 5;
                    if (library.size() < 5) {
                        Count = library.size();
                    }
                    for (int i = 0; i < Count; i++) {
                        cards.add(library.get(i));
                    }
                    CardList Pile1 = new CardList();
                    CardList Pile2 = new CardList();
                    boolean stop = false;
                    int Pile1CMC = 0;
                    int Pile2CMC = 0;


                    GuiUtils.getChoice("Revealing top " + Count + " cards of library: ", cards.toArray());
                    //Human chooses
                    if (card.getController().isComputer()) {
                        for (int i = 0; i < Count; i++) {
                            if (stop == false) {
                                choice = GuiUtils.getChoiceOptional("Choose cards to put into the first pile: ", cards.toArray());
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
                            if (!Pile1.contains(library.get(i))) {
                                Pile2.add(library.get(i));
                                Pile2CMC = Pile2CMC + CardUtil.getConvertedManaCost(library.get(i));
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
                            JOptionPane.showMessageDialog(null, "Computer adds the first pile to its hand and puts the second pile into the graveyard", "", JOptionPane.INFORMATION_MESSAGE);
                            for (int i = 0; i < Pile1.size(); i++) {
                                AllZone.getGameAction().moveTo(hand, Pile1.get(i));
                            }
                            for (int i = 0; i < Pile2.size(); i++) {
                                AllZone.getGameAction().moveToGraveyard(Pile2.get(i));
                            }
                        } else {
                            JOptionPane.showMessageDialog(null, "Computer adds the second pile to its hand and puts the first pile into the graveyard", "", JOptionPane.INFORMATION_MESSAGE);
                            for (int i = 0; i < Pile2.size(); i++) {
                                AllZone.getGameAction().moveTo(hand, Pile2.get(i));
                            }
                            for (int i = 0; i < Pile1.size(); i++) {
                                AllZone.getGameAction().moveToGraveyard(Pile1.get(i));
                            }
                        }

                    } else//Computer chooses (It picks the highest converted mana cost card and 1 random card.)
                    {
                        Card biggest = null;
                        biggest = library.get(0);

                        for (int i = 0; i < Count; i++) {
                            if (CardUtil.getConvertedManaCost(biggest.getManaCost()) >= CardUtil.getConvertedManaCost(biggest.getManaCost())) {
                                biggest = cards.get(i);
                            }
                        }
                        Pile1.add(biggest);
                        cards.remove(biggest);
                        if (cards.size() > 0) {
                            Card Random = CardUtil.getRandom(cards.toArray());
                            Pile1.add(Random);
                        }
                        for (int i = 0; i < Count; i++) if (!Pile1.contains(library.get(i))) Pile2.add(library.get(i));
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
                        Object[] possibleValues = {"Pile 1", "Pile 2"};
                        Object q = JOptionPane.showOptionDialog(null, sb, "Fact or Fiction",
                                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,
                                null, possibleValues, possibleValues[0]);
                        if (q.equals(0)) {
                            for (int i = 0; i < Pile1.size(); i++) {
                                AllZone.getGameAction().moveTo(hand, Pile1.get(i));
                            }
                            for (int i = 0; i < Pile2.size(); i++) {
                                AllZone.getGameAction().moveToGraveyard(Pile2.get(i));
                            }
                        } else {
                            for (int i = 0; i < Pile2.size(); i++) {
                                AllZone.getGameAction().moveTo(hand, Pile2.get(i));
                            }
                            for (int i = 0; i < Pile1.size(); i++) {
                                AllZone.getGameAction().moveToGraveyard(Pile1.get(i));
                            }
                        }
                    }
                    Pile1.clear();
                    Pile2.clear();
                } //resolve()

                @Override
                public boolean canPlayAI() {
                    CardList cards = AllZoneUtil.getPlayerCardsInPlay(AllZone.getComputerPlayer());
                    return cards.size() >= 10;
                }
            }; //SpellAbility

            card.addSpellAbility(spell);
        } //*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Echoing Decay")) {
            Cost cost = new Cost(card.getManaCost(), cardName, false);
            Target tgt = new Target(card, "C");
            final SpellAbility spell = new Spell(card, cost, tgt) {
                private static final long serialVersionUID = 3154935854257358023L;

                @Override
                public boolean canPlayAI() {
                    CardList c = getCreature();
                    if (c.isEmpty()) {
                        return false;
                    }
                    else {
                        setTargetCard(c.get(0));
                        return true;
                    }
                } //canPlayAI()

                CardList getCreature() {
                    CardList out = new CardList();
                    CardList list = CardFactoryUtil.AI_getHumanCreature("Flying", card, true);
                    list.shuffle();

                    for (int i = 0; i < list.size(); i++) {
                        if ((list.get(i).getNetAttack() >= 2) && (list.get(i).getNetDefense() <= 2)) {
                            out.add(list.get(i));
                        }
                    }

                    //in case human player only has a few creatures in play, target anything
                    if (out.isEmpty() && 0 < CardFactoryUtil.AI_getHumanCreature(2, card, true).size()
                            && 3 > CardFactoryUtil.AI_getHumanCreature(card, true).size())
                    {
                        out.addAll(CardFactoryUtil.AI_getHumanCreature(2, card, true));
                        CardListUtil.sortFlying(out);
                    }
                    return out;
                } //getCreature()


                @Override
                public void resolve() {
                    if (AllZoneUtil.isCardInPlay(getTargetCard())
                            && CardFactoryUtil.canTarget(card, getTargetCard())) {
                        final Card c = getTargetCard();

                        c.addTempAttackBoost(-2);
                        c.addTempDefenseBoost(-2);

                        AllZone.getEndOfTurn().addUntil(new Command() {
                            private static final long serialVersionUID = 1327455269456577020L;

                            public void execute() {
                                c.addTempAttackBoost(2);
                                c.addTempDefenseBoost(2);
                            }
                        });

                        //get all creatures
                        CardList list = AllZoneUtil.getCardsInPlay();

                        list = list.getName(getTargetCard().getName());
                        list.remove(getTargetCard());

                        if (!getTargetCard().isFaceDown()) for (int i = 0; i < list.size(); i++) {
                            final Card crd = list.get(i);

                            crd.addTempAttackBoost(-2);
                            crd.addTempDefenseBoost(-2);

                            AllZone.getEndOfTurn().addUntil(new Command() {
                                private static final long serialVersionUID = 5151337777143949221L;

                                public void execute() {
                                    crd.addTempAttackBoost(2);
                                    crd.addTempDefenseBoost(2);
                                }
                            });
                        }

                    } //in play?
                } //resolve()
            }; //SpellAbility

            card.addSpellAbility(spell);

            card.setSVar("PlayMain1", "TRUE");
        } //*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Hidetsugu's Second Rite")) {
            Target t = new Target(card, "Select target player", "Player");
            Cost cost = new Cost("3 R", cardName, false);
            final SpellAbility spell = new Spell(card, cost, t) {
                private static final long serialVersionUID = 176857775451818523L;

                @Override
                public void resolve() {
                    if (getTargetPlayer().getLife() == 10) {
                        getTargetPlayer().addDamage(10, card);
                    }
                }

                @Override
                public boolean canPlayAI() {
                    return AllZone.getHumanPlayer().getLife() == 10;
                }

            };
            spell.setChooseTargetAI(CardFactoryUtil.AI_targetHuman());

            card.addSpellAbility(spell);

            card.setSVar("PlayMain1", "TRUE");
        } //*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Echoing Truth")) {
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = 563933533543239220L;

                @Override
                public boolean canPlayAI() {
                    CardList human = CardFactoryUtil.AI_getHumanCreature(card, true);
                    return 4 < AllZone.getPhase().getTurn() && 0 < human.size();
                }

                @Override
                public void chooseTargetAI() {
                    CardList human = CardFactoryUtil.AI_getHumanCreature(card, true);
                    setTargetCard(CardFactoryUtil.AI_getBestCreature(human));
                }

                @Override
                public void resolve() {
                    //if target card is not in play, just quit
                    if (!AllZoneUtil.isCardInPlay(getTargetCard())
                            || !CardFactoryUtil.canTarget(card, getTargetCard()))
                    {
                        return;
                    }

                    //get all permanents
                    CardList all = AllZoneUtil.getCardsInPlay();

                    CardList sameName = all.getName(getTargetCard().getName());
                    sameName = sameName.filter(new CardListFilter() {
                        public boolean addCard(final Card c) {
                            return !c.isFaceDown();
                        }
                    });

                    if (!getTargetCard().isFaceDown()) {
                        //bounce all permanents with the same name
                        for (int i = 0; i < sameName.size(); i++) {
                            if (sameName.get(i).isToken()) {
                                AllZone.getGameAction().exile(sameName.get(i));
                            } else {
                                PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, sameName.get(i).getOwner());
                                AllZone.getGameAction().moveTo(hand, sameName.get(i));
                            }
                        } //for
                    } //if (!isFaceDown())
                    else {
                        PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, getTargetCard().getOwner());
                        AllZone.getGameAction().moveTo(hand, getTargetCard());
                    }
                } //resolve()
            }; //SpellAbility
            Input target = new Input() {
                private static final long serialVersionUID = -3978705328511825933L;

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
                    if (!card.isLand() && zone.is(Constant.Zone.Battlefield) && CardFactoryUtil.canTarget(spell, card)) {
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
            }; //Input

            card.setSVar("PlayMain1", "TRUE");

            spell.setBeforePayMana(target);

            card.addSpellAbility(spell);
        } //*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Intuition")) {
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = 8282597086298330698L;

                @Override
                public void resolve() {
                    Player player = card.getController();
                    if (player.isHuman()) {
                        humanResolve();
                    } else {
                        computerResolve();
                    }
                    player.shuffle();
                }

                public void humanResolve() {
                    CardList libraryList = AllZoneUtil.getPlayerCardsInLibrary(AllZone.getHumanPlayer());
                    CardList selectedCards = new CardList();

                    Object o = GuiUtils.getChoiceOptional("Select first card", libraryList.toArray());
                    if (o != null) {
                        Card c1 = (Card) o;
                        libraryList.remove(c1);
                        selectedCards.add(c1);
                    } else {
                        return;
                    }
                    o = GuiUtils.getChoiceOptional("Select second card", libraryList.toArray());
                    if (o != null) {
                        Card c2 = (Card) o;
                        libraryList.remove(c2);
                        selectedCards.add(c2);
                    } else {
                        return;
                    }
                    o = GuiUtils.getChoiceOptional("Select third card", libraryList.toArray());
                    if (o != null) {
                        Card c3 = (Card) o;
                        libraryList.remove(c3);
                        selectedCards.add(c3);
                    } else {
                        return;
                    }

                    //comp randomly selects one of the three cards
                    Card choice = selectedCards.get(MyRandom.random.nextInt(2));

                    selectedCards.remove(choice);
                    AllZone.getGameAction().moveToHand(choice);

                    for (Card trash : selectedCards) {
                        AllZone.getGameAction().moveToGraveyard(trash);
                    }
                }

                public void computerResolve() {
                    CardList list = AllZoneUtil.getPlayerCardsInLibrary(AllZone.getComputerPlayer());
                    CardList selectedCards = new CardList();

                    //pick best creature
                    Card c = CardFactoryUtil.AI_getBestCreature(list);
                    if (c == null) {
                        c = list.get(0);
                    }
                    list.remove(c);
                    selectedCards.add(c);

                    c = CardFactoryUtil.AI_getBestCreature(list);
                    if (c == null) {
                        c = list.get(0);
                    }
                    list.remove(c);
                    selectedCards.add(c);

                    c = CardFactoryUtil.AI_getBestCreature(list);
                    if (c == null) {
                        c = list.get(0);
                    }
                    list.remove(c);
                    selectedCards.add(c);

                    // NOTE: Using getChoiceOptional() results in a null error when you click on Cancel.
                    Object o = GuiUtils.getChoice("Select card to give to computer", selectedCards.toArray());

                    Card choice = (Card) o;

                    selectedCards.remove(choice);
                    AllZone.getGameAction().moveToHand(choice);

                    for (Card trash : selectedCards) {
                        AllZone.getGameAction().moveToGraveyard(trash);
                    }
                }

                @Override
                public boolean canPlay() {
                    CardList library = AllZoneUtil.getPlayerCardsInLibrary(card.getController());
                    return library.size() >= 3;
                }

                @Override
                public boolean canPlayAI() {
                    CardList creature = AllZoneUtil.getPlayerCardsInLibrary(AllZone.getComputerPlayer());
                    creature = creature.getType("Creature");
                    return creature.size() >= 3;
                }
            }; //SpellAbility

            card.addSpellAbility(spell);
        } //*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Echoing Courage")) {
            Cost cost = new Cost(card.getManaCost(), cardName, false);
            Target tgt = new Target(card, "C");
            final SpellAbility spell = new Spell(card, cost, tgt) {
                private static final long serialVersionUID = -8649611733196156346L;

                public boolean canPlayAI() {
                    CardList list = AllZoneUtil.getCreaturesInPlay(AllZone.getComputerPlayer());
                    if (list.isEmpty()) {
                        return false;
                    } else {
                        setTargetCard(CardFactoryUtil.AI_getBestCreature(list));
                        return true;
                    }
                } //canPlayAI()

                public void resolve() {
                    if (AllZoneUtil.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card, getTargetCard())) {
                        final Card c = getTargetCard();

                        c.addTempAttackBoost(2);
                        c.addTempDefenseBoost(2);

                        AllZone.getEndOfTurn().addUntil(new Command() {
                            private static final long serialVersionUID = 1327455269456577020L;

                            public void execute() {
                                c.addTempAttackBoost(-2);
                                c.addTempDefenseBoost(-2);
                            }
                        });

                        //get all creatures
                        CardList list = AllZoneUtil.getCardsInPlay(getTargetCard().getName());
                        list.remove(getTargetCard());

                        if (!getTargetCard().isFaceDown()) {
                            for (int i = 0; i < list.size(); i++) {
                                final Card crd = list.get(i);

                                crd.addTempAttackBoost(2);
                                crd.addTempDefenseBoost(2);

                                AllZone.getEndOfTurn().addUntil(new Command() {
                                    private static final long serialVersionUID = 5151337777143949221L;

                                    public void execute() {
                                        crd.addTempAttackBoost(-2);
                                        crd.addTempDefenseBoost(-2);
                                    }
                                });
                            }
                        }

                    } //in play?
                } //resolve()
            }; //SpellAbility

            card.addSpellAbility(spell);
        } //*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Hurkyl's Recall")) {
        	/*
        	 * Return all artifacts target player owns to his or her hand.
        	 */
            Target t = new Target(card, "Select target player", "Player");
            Cost cost = new Cost("1 U", cardName, false);

            SpellAbility spell = new Spell(card, cost, t) {
                private static final long serialVersionUID = -4098702062413878046L;

                @Override
                public boolean canPlayAI() {
                    CardList humanArts = AllZoneUtil.getPlayerCardsInPlay(AllZone.getHumanPlayer());
                    humanArts = humanArts.getType("Artifact");
                    return humanArts.size() > 0;
                } //canPlayAI

                @Override
                public void chooseTargetAI() {
                    setTargetPlayer(AllZone.getHumanPlayer());
                } //chooseTargetAI()

                @Override
                public void resolve() {
                    Player player = getTargetPlayer();
                    CardList artifacts = AllZoneUtil.getCardsInPlay();
                    artifacts = artifacts.getType("Artifact");

                    for (int i = 0; i < artifacts.size(); i++) {
                        Card thisArtifact = artifacts.get(i);
                        if (thisArtifact.getOwner().equals(player)) {
                            //moveToHand handles tokens
                            AllZone.getGameAction().moveToHand(thisArtifact);
                        }
                    }
                } //resolve()
            };

            card.addSpellAbility(spell);
        }//*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Suffer the Past")) {
            Cost cost = new Cost("X B", cardName, false);
            Target tgt = new Target(card, "Select a Player", "Player");
            final SpellAbility spell = new Spell(card, cost, tgt) {
                private static final long serialVersionUID = 1168802375190293222L;

                @Override
                public void resolve() {
                    Player tPlayer = getTargetPlayer();
                    Player player = card.getController();
                    final int max = card.getXManaCostPaid();

                    CardList graveList = AllZoneUtil.getPlayerGraveyard(tPlayer);
                    int X = Math.min(max, graveList.size());

                    if (player.isHuman()) {
                        for (int i = 0; i < X; i++) {
                            Object o = GuiUtils.getChoice("Remove from game", graveList.toArray());
                            if (o == null) {
                                break;
                            }
                            Card c1 = (Card) o;
                            graveList.remove(c1); //remove from the display list
                            AllZone.getGameAction().exile(c1);
                        }
                    } else { //Computer
                        //Random random = MyRandom.random;
                        for (int j = 0; j < X; j++) {
                            //int index = random.nextInt(X-j);
                            AllZone.getGameAction().exile(graveList.get(j));
                        }
                    }

                    tPlayer.loseLife(X, card);
                    player.gainLife(X, card);
                    card.setXManaCostPaid(0);
                }

                @Override
                public void chooseTargetAI() {
                    setTargetPlayer(AllZone.getHumanPlayer());
                } //chooseTargetAI()

                @Override
                public boolean canPlayAI() {
                    CardList graveList = AllZoneUtil.getPlayerGraveyard(AllZone.getHumanPlayer());

                    final int maxX = ComputerUtil.getAvailableMana().size() - 1;
                    return (maxX >= 3) && (graveList.size() > 0);
                }
            };

            card.addSpellAbility(spell);
        } //*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Demonic Consultation")) {
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = 1481101852928051519L;

                @Override
                public void resolve() {
                    Player player = card.getController();
                    CardList libList = AllZoneUtil.getPlayerCardsInLibrary(player);
                    final String[] input = new String[1];
                    input[0] = JOptionPane.showInputDialog(null, "Which card?", "Pick card", JOptionPane.QUESTION_MESSAGE);

                    for (int i = 0; i < 7; i++) {
                        Card c = libList.get(i);
                        AllZone.getGameAction().exile(c);
                    }

                    int max = libList.size();
                    int stop = 0;
                    for (int i = 0; i < max; i++) {
                        Card c = libList.get(i);
                        if (c.getName().equals(input[0])) {
                            if (stop == 0) {
                                AllZone.getGameAction().moveToHand(c);
                                stop = 1;
                            }

                        } else if (stop == 0) {
                            AllZone.getGameAction().exile(c);
                        }
                    }
                }

                @Override
                public boolean canPlay() {
                    CardList libList = AllZoneUtil.getPlayerCardsInLibrary(card.getController());
                    return libList.size() > 6 && super.canPlay();
                }

                @Override
                public boolean canPlayAI() {
                    return false;
                }
            }; //SpellAbility


            spell.setStackDescription("Name a card. Exile the top six cards of your library, then reveal cards from the top of your library until you reveal the named card. Put that card into your hand and exile all other cards revealed this way");
            card.addSpellAbility(spell);
        } //*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Siren's Call")) {
            /*
             *  Creatures the active player controls attack this turn if able.
             *
             *  At the beginning of the next end step, destroy all non-Wall creatures
             *  that player controls that didn't attack this turn. Ignore this effect
             *  for each creature the player didn't control continuously since the
             *  beginning of the turn.
             */
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = -5746330758531799264L;

                @Override
                public boolean canPlay() {
                    return PhaseUtil.isBeforeAttackersAreDeclared()
                        && AllZone.getPhase().isPlayerTurn(card.getController().getOpponent());
                } //canPlay

                @Override
                public boolean canPlayAI() {
                    return false;
                } //canPlayAI

                @Override
                public void resolve() {
                    //this needs to get a list of opponents creatures and set the siren flag
                    Player player = card.getController();
                    Player opponent = player.getOpponent();
                    CardList creatures = AllZoneUtil.getCreaturesInPlay(opponent);
                    for (Card creature : creatures) {
                        //skip walls, skip creatures with summoning sickness
                        //also skip creatures with haste if they came onto the battlefield this turn
                        if ((!creature.isWall()
                                && !creature.hasSickness())
                                || (creature.hasKeyword("Haste") && creature.getTurnInZone() != 1))
                        {
                            creature.setSirenAttackOrDestroy(true);
                            //System.out.println("Siren's Call - setting flag for "+creature.getName());
                        }
                    }
                    final SpellAbility destroy = new Ability(card, "0") {
                    	@Override
                    	public void resolve() {
                    		Player player = card.getController();
                            Player opponent = player.getOpponent();
                            CardList creatures = AllZoneUtil.getCreaturesInPlay(opponent);

                            for (Card creature : creatures) {
                                //System.out.println("Siren's Call - EOT - "+creature.getName() +" flag: "+creature.getSirenAttackOrDestroy());
                                //System.out.println("Siren's Call - EOT - "+creature.getName() +" attacked?: "+creature.getCreatureAttackedThisCombat());
                                if (creature.getSirenAttackOrDestroy() && !creature.getCreatureAttackedThisTurn()) {
                                    if (AllZoneUtil.isCardInPlay(creature)) {
                                        //System.out.println("Siren's Call - destroying "+creature.getName());
                                        //this should probably go on the stack
                                        AllZone.getGameAction().destroy(creature);
                                    }
                                }
                                creature.setSirenAttackOrDestroy(false);
                            }
                    	}
                    };
                    Command atEOT = new Command() {
                        private static final long serialVersionUID = 5369528776959445848L;

                        public void execute() {
                        	StringBuilder sb = new StringBuilder();
                        	sb.append(card).append(" - At the beginning of the next end step, destroy all non-Wall creatures that player controls that didn't attack this turn. ");
                        	sb.append("Ignore this effect for each creature the player didn't control continuously since the beginning of the turn.");
                            destroy.setDescription(sb.toString());
                        	destroy.setStackDescription(sb.toString());
                        	
                        	AllZone.getStack().addSimultaneousStackEntry(destroy);
                        } //execute
                    }; //Command
                    AllZone.getEndOfTurn().addAt(atEOT);
                } //resolve
            }; //SpellAbility

            StringBuilder sb = new StringBuilder();
            sb.append(card.getName()).append(" - All creatures that can attack must do so or be destroyed.");
            spell.setStackDescription(sb.toString());

            card.addSpellAbility(spell);
        } //*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Telling Time")) {
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = 2626878556107707854L;
                String[] prompt = new String[] {
                        "Put a card into your hand",
                        "Put a card on top of library",
                        "Put a card on bottom of library"
                };

                @Override
                public boolean canPlayAI() {
                    return false;
                }

                @Override
                public void resolve() {
                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
                    CardList choices = new CardList();
                    for (int i = 0; i < 3 && lib.size() > 0; i++) {
                        choices.add(lib.get(i));
                    }

                    for (int i = 0; i < 3 && !choices.isEmpty(); i++) {
                        Object o = GuiUtils.getChoice(prompt[i], choices.toArray());
                        Card c1 = (Card) o;
                        if (i == 0) {
                            AllZone.getGameAction().moveToHand(c1);
                        } else if (i == 1) {
                            AllZone.getGameAction().moveToLibrary(c1);
                        } else if (i == 2) {
                            AllZone.getGameAction().moveToBottomOfLibrary(c1);
                        }

                        choices.remove(c1);
                    }
                }
            };

            
            
            card.addSpellAbility(spell);
        }//*************** END ************ END ************************** 


        //*************** START *********** START **************************
        /*
        else if (cardName.equals("Vengeful Dreams")) {
            final CardList targets = new CardList();
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = 1593405082929818055L;

                @Override
                public boolean canPlayAI() {
                    return false;
                }

                @Override
                public void resolve() {
                    for (Card c : targets) {
                        if (AllZoneUtil.isCardInPlay(c) && CardFactoryUtil.canTarget(card, c)) {
                            AllZone.getGameAction().exile(c);
                        }//if isCardInPlay
                    }
                    targets.clear();
                }
            };

            Input runtime = new Input() {
                private static final long serialVersionUID = 4656252051002867111L;
                int max = 0;

                @Override
                public void showMessage() {
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, AllZone.getHumanPlayer());
                    AllZone.getGameAction().moveToStack(card);
                    hand.updateObservers();
                    max = AllZoneUtil.getPlayerHand(card.getController()).size();
                    if (max == targets.size()) done();
                    StringBuilder sb = new StringBuilder();
                    sb.append(card.getName()).append(" - Select target attacking creatures.  Currently, (");
                    sb.append(targets.size()).append(") selected.");
                    sb.append(" Press OK when done.");
                    AllZone.getDisplay().showMessage(sb.toString());
                    ButtonUtil.enableAll();
                }

                @Override
                public void selectButtonCancel() {
                    targets.clear();
                    AllZone.getGameAction().moveToHand(card);
                    stop();
                }

                @Override
                public void selectCard(Card c, PlayerZone zone) {
                    if (zone.is(Constant.Zone.Battlefield) && !targets.contains(c)
                            && CardFactoryUtil.canTarget(card, c) && c.isAttacking()) {
                        targets.add(c);
                        showMessage();
                    }
                }

                @Override
                public void selectButtonOK() {
                    done();
                }

                private void done() {
                    if (targets.size() > AllZoneUtil.getPlayerHand(card.getController()).size()) stop();
                    else {
                        card.getController().discard(targets.size(), spell, false);
                        stopSetNext(new Input_PayManaCost(spell));
                    }

                }
            };
            spell.setStackDescription(cardName + " - exile X attacking creatures.");
            spell.setBeforePayMana(runtime);

            
            
            card.addSpellAbility(spell);
        }//*************** END ************ END **************************
*/
/*
        //*************** START *********** START **************************
        else if (cardName.equals("Firestorm")) {
            final ArrayList<Object> targets = new ArrayList<Object>();
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = -3763504534745192451L;

                @Override
                public boolean canPlayAI() {
                    return false;
                }

                @Override
                public void resolve() {
                    int dmg = targets.size();
                    for (Object o : targets) {
                        if (o instanceof Player) {
                            if (((Player) o).canTarget(this)) {
                                ((Player) o).addDamage(dmg, card);
                            }
                        } else if (o instanceof Card) {
                            if (AllZoneUtil.isCardInPlay((Card) o) && CardFactoryUtil.canTarget(card, (Card) o)) {
                                ((Card) o).addDamage(dmg, card);
                            }//if isCardInPlay
                        }
                    }
                    targets.clear();
                }
            };

            Input runtime = new Input() {
                private static final long serialVersionUID = 5261183989797221059L;

                @Override
                public void showMessage() {
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, AllZone.getHumanPlayer());
                    AllZone.getGameAction().moveToStack(card);
                    hand.updateObservers();
                    StringBuilder sb = new StringBuilder();
                    sb.append(card.getName()).append(" - Select target creatures, players, and/or planeswalkers.  Currently, (");
                    sb.append(targets.size()).append(") selected.");
                    sb.append(" Press OK when done.");
                    AllZone.getDisplay().showMessage(sb.toString());
                    ButtonUtil.enableAll();
                }

                @Override
                public void selectButtonCancel() {
                    targets.clear();
                    AllZone.getGameAction().moveToHand(card);
                    stop();
                }

                @Override
                public void selectCard(Card c, PlayerZone zone) {
                    if (zone.is(Constant.Zone.Battlefield) && !targets.contains(c)
                            && CardFactoryUtil.canTarget(card, c) &&
                            (c.isCreature() || c.isPlaneswalker())) {
                        targets.add(c);
                        showMessage();
                    }
                }

                @Override
                public void selectPlayer(Player p) {
                    if (p.canTarget(spell) && !targets.contains(p)) {
                        targets.add(p);
                        showMessage();
                    }
                }

                @Override
                public void selectButtonOK() {
                    done();
                }

                private void done() {
                    if (targets.size() > AllZoneUtil.getPlayerHand(card.getController()).size()) stop();
                    else {
                        card.getController().discard(targets.size(), spell, true);
                        stopSetNext(new Input_PayManaCost(spell));
                    }

                }
            };
            spell.setStackDescription(cardName + " - deals X damage to each of X target creatures and/or players.");
            spell.setBeforePayMana(runtime);

            
            
            card.addSpellAbility(spell);
        }//*************** END ************ END **************************
*/

        //*************** START *********** START **************************
        else if (cardName.equals("Cryptic Command")) {
            final SpellAbility[] m_spell = new SpellAbility[1];
            final Card[] m_perm = new Card[1];

            final ArrayList<String> userChoice = new ArrayList<String>();

            final String[] cardChoice = {
                    "Counter target spell",
                    "Return target permanent to its owner's hand",
                    "Tap all creatures your opponents control",
                    "Draw a card"};

            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = 9178547049760990376L;

                @Override
                public void resolve() {

                    //"Counter target spell",
                    for (int i = 0; i < card.getChoices().size(); i++) {
                        if (card.getChoice(i).equals(cardChoice[0])) {
                            if (AllZone.getStack().size() > 0) {
                                SpellAbility sa = AllZone.getStack().peekAbility();
                                if (sa.isSpell()) {
                                    AllZone.getStack().pop();
                                    AllZone.getGameAction().moveToGraveyard(sa.getSourceCard());
                                }
                            }
                        }
                    }

                    //"Return target Permanent to its owner's hand",
                    if (userChoice.contains(cardChoice[1]) || card.getChoices().contains(cardChoice[1])) {
                        if (AllZoneUtil.isCardInPlay(getTargetCard())
                                && CardFactoryUtil.canTarget(card, getTargetCard()))
                        {
                            AllZone.getGameAction().moveToHand(getTargetCard());
                        }
                    }

                    //"Tap all creatures your opponents control",
                    for (int i = 0; i < card.getChoices().size(); i++) {
                        if (card.getChoice(i).equals(cardChoice[2])) {
                            CardList creatures = AllZoneUtil.getCreaturesInPlay(card.getController().getOpponent());
                            for (Card c : creatures) {
                                c.tap();
                            }
                        }
                    }

                    //"Draw a card"
                    if (userChoice.contains(cardChoice[3]) || card.getChoices().contains(cardChoice[3])) {
                        card.getController().drawCard();
                    }
                } //resolve()

                @Override
                public boolean canPlayAI() {
                    return false;
                }
            }; //SpellAbility

            final Command setStackDescription = new Command() {

                private static final long serialVersionUID = -4833850318955216009L;

                public void execute() {
                    ArrayList<String> a = new ArrayList<String>();
                    if (userChoice.contains(cardChoice[0]) || card.getChoices().contains(cardChoice[0])) {
                        a.add("counter target spell");
                    }
                    if (userChoice.contains(cardChoice[1]) || card.getChoices().contains(cardChoice[1])) {
                        a.add("return target permanent to its owner's hand");
                    }
                    if (userChoice.contains(cardChoice[2]) || card.getChoices().contains(cardChoice[2])) {
                        a.add("tap all creatures your opponents control");
                    }
                    if (userChoice.contains(cardChoice[3]) || card.getChoices().contains(cardChoice[3])) {
                        a.add(" Draw a card.");
                    }

                    String s = a.get(0) + ", " + a.get(1);
                    spell.setStackDescription(card.getName() + " - " + s);
                }
            };//Command


            final Input returnTarget = new Input() {
                private static final long serialVersionUID = 2736368243448655071L;

                @Override
                public void showMessage() {
                    AllZone.getDisplay().showMessage("Select target permanent");
                    ButtonUtil.enableOnlyCancel();
                }

                @Override
                public void selectButtonCancel() {
                    stop();
                }

                @Override
                public void selectCard(final Card c, final PlayerZone zone) {
                    if (c.isPermanent() && zone.is(Constant.Zone.Battlefield) && CardFactoryUtil.canTarget(card, c)) {
                        if (card.isCopiedSpell()) {
                            card.getChoiceTargets().remove(0);
                        }
                        m_perm[0] = c;
                        spell.setTargetCard(c);
                        card.setSpellChoiceTarget(String.valueOf(c.getUniqueNumber()));
                        setStackDescription.execute();
                        stopSetNext(new Input_PayManaCost(spell));
                    } //if
                } //selectCard()
            }; //Input targetLand

            Input chooseTwoInput = new Input() {
                private static final long serialVersionUID = -4200213000203960667L;

                @Override
                public void showMessage() {
                    if (card.isCopiedSpell()) {
                        if (card.getChoices().contains(cardChoice[1])) {
                            stopSetNext(returnTarget);
                        }
                        else {
                            setStackDescription.execute();

                            stopSetNext(new Input_PayManaCost(spell));
                        }
                    } else {
                        //reset variables
                        m_spell[0] = null;
                        m_perm[0] = null;
                        card.getChoices().clear();
                        card.getChoiceTargets().clear();
                        userChoice.clear();

                        ArrayList<String> display = new ArrayList<String>();

                        //get all
                        CardList list = AllZoneUtil.getCardsInPlay();

                        if (AllZone.getStack().size() > 0) {
                            display.add("Counter target spell");
                        }
                        if (list.size() > 0) {
                            display.add("Return target permanent to its owner's hand");
                        }
                        display.add("Tap all creatures your opponents control");
                        display.add("Draw a card");

                        ArrayList<String> a = chooseTwo(display);
                        //everything stops here if user cancelled
                        if (a == null) {
                            stop();
                            return;
                        }

                        userChoice.addAll(a);

                        if (userChoice.contains(cardChoice[1])) {
                            stopSetNext(returnTarget);
                        }
                        else {
                            setStackDescription.execute();
                            stopSetNext(new Input_PayManaCost(spell));
                        }
                    }
                } //showMessage()

                ArrayList<String> chooseTwo(final ArrayList<String> choices) {
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
                } //chooseTwo()
            }; //Input chooseTwoInput

            card.addSpellAbility(spell);

            card.setSpellWithChoices(true);
            spell.setBeforePayMana(chooseTwoInput);
        } //*************** END ************ END **************************


        //*************** START *********** START **************************
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
                    CardList ens = AllZoneUtil.getTypeInPlay("Enchantment");
                    CardList toReturn = ens.filter(new CardListFilter() {
                        public boolean addCard(final Card c) {
                            Card enchanting = c.getEnchantingCard();
                            
                            if (enchanting != null){
                                if ((enchanting.isAttacking() && enchanting.getController().isPlayer(you.getOpponent())) || 
                                        enchanting.getController().isPlayer(you)){
                                    return true;
                                }
                            }
                            
                            return (c.getOwner().isPlayer(you) && c.getController().isPlayer(you));
                        }
                    });
                    for (Card c : toReturn) {
                        AllZone.getGameAction().moveToHand(c);
                    }

                    for (Card c : ens) {
                        if (!toReturn.contains(c)) {
                            AllZone.getGameAction().destroy(c);
                        }
                    }
                }
            };

            spell.setStackDescription(card + " - destroy/return enchantments.");

            card.addSpellAbility(spell);
        } //*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Burn the Impure")) {
        	/*
        	 * Burn the Impure deals 3 damage to target creature. If that
        	 * creature has infect, Burn the Impure deals 3 damage to that
        	 * creature's controller.
        	 */
            Cost abCost = new Cost("1 R", cardName, false);
            final SpellAbility spell = new Spell(card, abCost, new Target(card, "TgtC")) {
                private static final long serialVersionUID = -3069135027502686218L;
                int damage = 3;

                @Override
                public void chooseTargetAI() {

                    CardList creatures = AllZoneUtil.getCreaturesInPlay(AllZone.getHumanPlayer());
                    creatures = creatures.filter(new CardListFilter() {
                        public boolean addCard(final Card c) {
                            return c.getNetAttack() <= damage
                                    && !c.hasKeyword("Indestructible");
                        }
                    });
                    CardList infect = creatures.filter(AllZoneUtil.getKeywordFilter("Infect"));
                    if (infect.size() > 0) {
                        Card c = CardFactoryUtil.AI_getBestCreature(infect);
                        setTargetCard(c);
                    } else {
                        Card c = CardFactoryUtil.AI_getBestCreature(creatures);
                        setTargetCard(c);
                    }

                } //chooseTargetAI()

                @Override
                public boolean canPlayAI() {
                    CardList creatures = AllZoneUtil.getCreaturesInPlay(AllZone.getHumanPlayer());
                    creatures = creatures.filter(new CardListFilter() {
                        public boolean addCard(final Card c) {
                            return c.getNetAttack() <= damage
                                    && !c.hasKeyword("Indestructible");
                        }
                    });
                    return creatures.size() > 0;
                }

                @Override
                public void resolve() {
                    if (AllZoneUtil.isCardInPlay(getTargetCard())
                            && CardFactoryUtil.canTarget(card, getTargetCard()))
                    {
                        Card c = getTargetCard();
                        c.addDamage(damage, card);
                        if (c.hasKeyword("Infect")) {
                            c.getController().addDamage(3, card);
                        }
                    }
                }
            }; //SpellAbility

            StringBuilder sb = new StringBuilder();
            sb.append(cardName);
            sb.append(" deals 3 damage to target creature. If that creature has infect, ");
            sb.append(cardName);
            sb.append(" deals 3 damage to that creature's controller.");
            spell.setDescription(sb.toString());

            card.addSpellAbility(spell);
        } //*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Turnabout")) {
            /*
             * Choose artifact, creature, or land. Tap all untapped permanents of the chosen
             * type target player controls, or untap all tapped permanents of that type that
             * player controls.
             */
            Cost abCost = new Cost("2 U U", cardName, false);
            Target target = new Target(card, "Select target player", "Player".split(","));
            final SpellAbility spell = new Spell(card, abCost, target) {
                private static final long serialVersionUID = -2175586347805121896L;

                @Override
                public boolean canPlayAI() {
                    return false;
                }

                @Override
                public void resolve() {
                    String[] choices = new String[]{"Artifact", "Creature", "Land"};
                    Object o = GuiUtils.getChoice("Select permanent type", choices);
                    String cardType = (String) o;
                    CardList list = AllZoneUtil.getPlayerTypeInPlay(getTargetPlayer(), cardType);

                    String[] tapOrUntap = new String[]{"Tap", "Untap"};
                    Object z = GuiUtils.getChoice("Tap or Untap?", tapOrUntap);
                    boolean tap = (z.equals("Tap")) ? true : false;

                    for (Card c : list) {
                        if (tap) {
                            c.tap();
                        }
                        else {
                            c.untap();
                        }
                    }
                } //resolve()
            }; //SpellAbility



            card.addSpellAbility(spell);
        } //*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Wing Puncture")) {

            Target t2 = new Target(card, "Select target creature with flying", "Creature.withFlying".split(","));
            final Ability_Sub sub = new Ability_Sub(card, t2) {
                private static final long serialVersionUID = 4618047889975691050L;

                @Override
                public boolean chkAI_Drawback() {
                    return false;
                }

                @Override
                public void resolve() {
                    Card myc = this.getParent().getTargetCard();
                    Card tgt = getTargetCard();
                    if (AllZoneUtil.isCardInPlay(myc) && AllZoneUtil.isCardInPlay(tgt)) {
                        if (CardFactoryUtil.canTarget(card, myc) && CardFactoryUtil.canTarget(card, tgt)) {
                            tgt.addDamage(myc.getNetAttack(), myc);
                        }
                    }
                }

                @Override
                public boolean doTrigger(final boolean b) {
                    return false;
                }
            };

            Cost abCost = new Cost("G", cardName, false);
            Target t1 = new Target(card, "Select target creature you control", "Creature.YouCtrl".split(","));
            final SpellAbility spell = new Spell(card, abCost, t1) {
                private static final long serialVersionUID = 8964235807056739219L;

                @Override
                public boolean canPlayAI() {
                    return false;
                }

                @Override
                public void resolve() {
                    sub.resolve();
                }
            };
            spell.setSubAbility(sub);
            spell.setDescription("Target creature you control deals damage equal to its power to target creature with flying.");
            spell.setStackDescription(card + " - Creature you control deals damage equal to its power to creature with flying.");


            card.addSpellAbility(spell);
        } //*************** END ************ END **************************

        return card;
    } //getCard

} //end class CardFactory_Instants
