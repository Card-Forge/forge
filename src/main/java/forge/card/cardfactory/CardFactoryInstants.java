package forge.card.cardfactory;

import javax.swing.JOptionPane;

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
import forge.MyRandom;
import forge.PhaseUtil;
import forge.Player;
import forge.PlayerZone;
import forge.card.cost.Cost;
import forge.card.spellability.Ability;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.gui.GuiUtils;
import forge.gui.input.Input;
import forge.gui.input.InputPayManaCost;

/**
 * <p>
 * CardFactory_Instants class.
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
    public static Card getCard(final Card card, final String cardName) {

        // *************** START *********** START **************************
        if (cardName.equals("Fact or Fiction")) {
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = 1481112451519L;

                @Override
                public void resolve() {

                    Card choice = null;

                    // check for no cards in hand on resolve
                    final PlayerZone library = card.getController().getZone(Constant.Zone.Library);
                    final PlayerZone hand = card.getController().getZone(Constant.Zone.Hand);
                    // PlayerZone Grave =
                    // card.getController().getZone(Constant.Zone.Graveyard);
                    final CardList cards = new CardList();

                    if (library.size() == 0) {
                        JOptionPane.showMessageDialog(null, "No more cards in library.", "",
                                JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }
                    int count = 5;
                    if (library.size() < 5) {
                        count = library.size();
                    }
                    for (int i = 0; i < count; i++) {
                        cards.add(library.get(i));
                    }
                    final CardList pile1 = new CardList();
                    final CardList pile2 = new CardList();
                    boolean stop = false;
                    int pile1CMC = 0;
                    int pile2CMC = 0;

                    GuiUtils.getChoice("Revealing top " + count + " cards of library: ", cards.toArray());
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
                            if (!pile1.contains(library.get(i))) {
                                pile2.add(library.get(i));
                                pile2CMC = pile2CMC + CardUtil.getConvertedManaCost(library.get(i));
                            }
                        }
                        final StringBuilder sb = new StringBuilder();
                        sb.append("You have spilt the cards into the following piles" + "\r\n" + "\r\n");
                        sb.append("Pile 1: " + "\r\n");
                        for (int i = 0; i < pile1.size(); i++) {
                            sb.append(pile1.get(i).getName() + "\r\n");
                        }
                        sb.append("\r\n" + "Pile 2: " + "\r\n");
                        for (int i = 0; i < pile2.size(); i++) {
                            sb.append(pile2.get(i).getName() + "\r\n");
                        }
                        JOptionPane.showMessageDialog(null, sb, "", JOptionPane.INFORMATION_MESSAGE);
                        if (pile1CMC >= pile2CMC) {
                            JOptionPane.showMessageDialog(null, "Computer adds the first pile to its hand "
                                    + "and puts the second pile into the graveyard", "",
                                    JOptionPane.INFORMATION_MESSAGE);
                            for (int i = 0; i < pile1.size(); i++) {
                                AllZone.getGameAction().moveTo(hand, pile1.get(i));
                            }
                            for (int i = 0; i < pile2.size(); i++) {
                                AllZone.getGameAction().moveToGraveyard(pile2.get(i));
                            }
                        } else {
                            JOptionPane.showMessageDialog(null, "Computer adds the second pile to its hand and "
                                    + "puts the first pile into the graveyard", "", JOptionPane.INFORMATION_MESSAGE);
                            for (int i = 0; i < pile2.size(); i++) {
                                AllZone.getGameAction().moveTo(hand, pile2.get(i));
                            }
                            for (int i = 0; i < pile1.size(); i++) {
                                AllZone.getGameAction().moveToGraveyard(pile1.get(i));
                            }
                        }

                    } else {
                        // Computer chooses (It picks the highest converted
                        // mana cost card and 1 random card.)
                        Card biggest = null;
                        biggest = library.get(0);

                        for (int i = 0; i < count; i++) {
                            if (CardUtil.getConvertedManaCost(biggest.getManaCost()) >= CardUtil
                                    .getConvertedManaCost(biggest.getManaCost())) {
                                biggest = cards.get(i);
                            }
                        }
                        pile1.add(biggest);
                        cards.remove(biggest);
                        if (cards.size() > 0) {
                            final Card random = CardUtil.getRandom(cards.toArray());
                            pile1.add(random);
                        }
                        for (int i = 0; i < count; i++) {
                            if (!pile1.contains(library.get(i))) {
                                pile2.add(library.get(i));
                            }
                        }
                        final StringBuilder sb = new StringBuilder();
                        sb.append("Choose a pile to add to your hand: " + "\r\n" + "\r\n");
                        sb.append("Pile 1: " + "\r\n");
                        for (int i = 0; i < pile1.size(); i++) {
                            sb.append(pile1.get(i).getName() + "\r\n");
                        }
                        sb.append("\r\n" + "Pile 2: " + "\r\n");
                        for (int i = 0; i < pile2.size(); i++) {
                            sb.append(pile2.get(i).getName() + "\r\n");
                        }
                        final Object[] possibleValues = { "Pile 1", "Pile 2" };
                        final Object q = JOptionPane.showOptionDialog(null, sb, "Fact or Fiction",
                                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, possibleValues,
                                possibleValues[0]);
                        if (q.equals(0)) {
                            for (int i = 0; i < pile1.size(); i++) {
                                AllZone.getGameAction().moveTo(hand, pile1.get(i));
                            }
                            for (int i = 0; i < pile2.size(); i++) {
                                AllZone.getGameAction().moveToGraveyard(pile2.get(i));
                            }
                        } else {
                            for (int i = 0; i < pile2.size(); i++) {
                                AllZone.getGameAction().moveTo(hand, pile2.get(i));
                            }
                            for (int i = 0; i < pile1.size(); i++) {
                                AllZone.getGameAction().moveToGraveyard(pile1.get(i));
                            }
                        }
                    }
                    pile1.clear();
                    pile2.clear();
                } // resolve()

                @Override
                public boolean canPlayAI() {
                    final CardList cards = AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield);
                    return cards.size() >= 10;
                }
            }; // SpellAbility

            card.addSpellAbility(spell);
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Echoing Decay")) {
            final Cost cost = new Cost(card.getManaCost(), cardName, false);
            final Target tgt = new Target(card, "C");
            final SpellAbility spell = new Spell(card, cost, tgt) {
                private static final long serialVersionUID = 3154935854257358023L;

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
                    final CardList list = CardFactoryUtil.getHumanCreatureAI("Flying", card, true);
                    list.shuffle();

                    for (int i = 0; i < list.size(); i++) {
                        if ((list.get(i).getNetAttack() >= 2) && (list.get(i).getNetDefense() <= 2)) {
                            out.add(list.get(i));
                        }
                    }

                    // in case human player only has a few creatures in play,
                    // target anything
                    if (out.isEmpty() && (0 < CardFactoryUtil.getHumanCreatureAI(2, card, true).size())
                            && (3 > CardFactoryUtil.getHumanCreatureAI(card, true).size())) {
                        out.addAll(CardFactoryUtil.getHumanCreatureAI(2, card, true));
                        CardListUtil.sortFlying(out);
                    }
                    return out;
                } // getCreature()

                @Override
                public void resolve() {
                    if (AllZoneUtil.isCardInPlay(this.getTargetCard())
                            && CardFactoryUtil.canTarget(card, this.getTargetCard())) {
                        final Card c = this.getTargetCard();

                        c.addTempAttackBoost(-2);
                        c.addTempDefenseBoost(-2);

                        AllZone.getEndOfTurn().addUntil(new Command() {
                            private static final long serialVersionUID = 1327455269456577020L;

                            @Override
                            public void execute() {
                                c.addTempAttackBoost(2);
                                c.addTempDefenseBoost(2);
                            }
                        });

                        // get all creatures
                        CardList list = AllZoneUtil.getCardsIn(Zone.Battlefield);

                        list = list.getName(this.getTargetCard().getName());
                        list.remove(this.getTargetCard());

                        if (!this.getTargetCard().isFaceDown()) {
                            for (int i = 0; i < list.size(); i++) {
                                final Card crd = list.get(i);

                                crd.addTempAttackBoost(-2);
                                crd.addTempDefenseBoost(-2);

                                AllZone.getEndOfTurn().addUntil(new Command() {
                                    private static final long serialVersionUID = 5151337777143949221L;

                                    @Override
                                    public void execute() {
                                        crd.addTempAttackBoost(2);
                                        crd.addTempDefenseBoost(2);
                                    }
                                });
                            }
                        }

                    } // in play?
                } // resolve()
            }; // SpellAbility

            card.addSpellAbility(spell);

            card.setSVar("PlayMain1", "TRUE");
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Hidetsugu's Second Rite")) {
            final Target t = new Target(card, "Select target player", "Player");
            final Cost cost = new Cost("3 R", cardName, false);
            final SpellAbility spell = new Spell(card, cost, t) {
                private static final long serialVersionUID = 176857775451818523L;

                @Override
                public void resolve() {
                    if (this.getTargetPlayer().getLife() == 10) {
                        this.getTargetPlayer().addDamage(10, card);
                    }
                }

                @Override
                public boolean canPlayAI() {
                    return AllZone.getHumanPlayer().getLife() == 10;
                }

            };
            spell.setChooseTargetAI(CardFactoryUtil.targetHumanAI());

            card.addSpellAbility(spell);

            card.setSVar("PlayMain1", "TRUE");
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Echoing Truth")) {
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = 563933533543239220L;

                @Override
                public boolean canPlayAI() {
                    final CardList human = CardFactoryUtil.getHumanCreatureAI(card, true);
                    return (4 < AllZone.getPhase().getTurn()) && (0 < human.size());
                }

                @Override
                public void chooseTargetAI() {
                    final CardList human = CardFactoryUtil.getHumanCreatureAI(card, true);
                    this.setTargetCard(CardFactoryUtil.getBestCreatureAI(human));
                }

                @Override
                public void resolve() {
                    // if target card is not in play, just quit
                    if (!AllZoneUtil.isCardInPlay(this.getTargetCard())
                            || !CardFactoryUtil.canTarget(card, this.getTargetCard())) {
                        return;
                    }

                    // get all permanents
                    final CardList all = AllZoneUtil.getCardsIn(Zone.Battlefield);

                    CardList sameName = all.getName(this.getTargetCard().getName());
                    sameName = sameName.filter(new CardListFilter() {
                        @Override
                        public boolean addCard(final Card c) {
                            return !c.isFaceDown();
                        }
                    });

                    if (!this.getTargetCard().isFaceDown()) {
                        // bounce all permanents with the same name
                        for (int i = 0; i < sameName.size(); i++) {
                            if (sameName.get(i).isToken()) {
                                AllZone.getGameAction().exile(sameName.get(i));
                            } else {
                                final PlayerZone hand = sameName.get(i).getOwner().getZone(Constant.Zone.Hand);
                                AllZone.getGameAction().moveTo(hand, sameName.get(i));
                            }
                        } // for
                    } // if (!isFaceDown())
                    else {
                        final PlayerZone hand = this.getTargetCard().getOwner().getZone(Constant.Zone.Hand);
                        AllZone.getGameAction().moveTo(hand, this.getTargetCard());
                    }
                } // resolve()
            }; // SpellAbility
            final Input target = new Input() {
                private static final long serialVersionUID = -3978705328511825933L;

                @Override
                public void showMessage() {
                    AllZone.getDisplay().showMessage("Select target nonland permanent for " + spell.getSourceCard());
                    ButtonUtil.enableOnlyCancel();
                }

                @Override
                public void selectButtonCancel() {
                    this.stop();
                }

                @Override
                public void selectCard(final Card card, final PlayerZone zone) {
                    if (!card.isLand() && zone.is(Constant.Zone.Battlefield) && card.canTarget(spell)) {
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

            card.setSVar("PlayMain1", "TRUE");

            spell.setBeforePayMana(target);

            card.addSpellAbility(spell);
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Intuition")) {
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = 8282597086298330698L;

                @Override
                public void resolve() {
                    final Player player = card.getController();
                    if (player.isHuman()) {
                        this.humanResolve();
                    } else {
                        this.computerResolve();
                    }
                    player.shuffle();
                }

                public void humanResolve() {
                    final CardList libraryList = AllZone.getHumanPlayer().getCardsIn(Zone.Library);
                    final CardList selectedCards = new CardList();

                    Object o = GuiUtils.getChoiceOptional("Select first card", libraryList.toArray());
                    if (o != null) {
                        final Card c1 = (Card) o;
                        libraryList.remove(c1);
                        selectedCards.add(c1);
                    } else {
                        return;
                    }
                    o = GuiUtils.getChoiceOptional("Select second card", libraryList.toArray());
                    if (o != null) {
                        final Card c2 = (Card) o;
                        libraryList.remove(c2);
                        selectedCards.add(c2);
                    } else {
                        return;
                    }
                    o = GuiUtils.getChoiceOptional("Select third card", libraryList.toArray());
                    if (o != null) {
                        final Card c3 = (Card) o;
                        libraryList.remove(c3);
                        selectedCards.add(c3);
                    } else {
                        return;
                    }

                    // comp randomly selects one of the three cards
                    final Card choice = selectedCards.get(MyRandom.getRandom().nextInt(2));

                    selectedCards.remove(choice);
                    AllZone.getGameAction().moveToHand(choice);

                    for (final Card trash : selectedCards) {
                        AllZone.getGameAction().moveToGraveyard(trash);
                    }
                }

                public void computerResolve() {
                    final CardList list = AllZone.getComputerPlayer().getCardsIn(Zone.Library);
                    final CardList selectedCards = new CardList();

                    // pick best creature
                    Card c = CardFactoryUtil.getBestCreatureAI(list);
                    if (c == null) {
                        c = list.get(0);
                    }
                    list.remove(c);
                    selectedCards.add(c);

                    c = CardFactoryUtil.getBestCreatureAI(list);
                    if (c == null) {
                        c = list.get(0);
                    }
                    list.remove(c);
                    selectedCards.add(c);

                    c = CardFactoryUtil.getBestCreatureAI(list);
                    if (c == null) {
                        c = list.get(0);
                    }
                    list.remove(c);
                    selectedCards.add(c);

                    // NOTE: Using getChoiceOptional() results in a null error
                    // when you click on Cancel.
                    final Object o = GuiUtils.getChoice("Select card to give to computer", selectedCards.toArray());

                    final Card choice = (Card) o;

                    selectedCards.remove(choice);
                    AllZone.getGameAction().moveToHand(choice);

                    for (final Card trash : selectedCards) {
                        AllZone.getGameAction().moveToGraveyard(trash);
                    }
                }

                @Override
                public boolean canPlay() {
                    final CardList library = card.getController().getCardsIn(Zone.Library);
                    return library.size() >= 3;
                }

                @Override
                public boolean canPlayAI() {
                    CardList creature = AllZone.getComputerPlayer().getCardsIn(Zone.Library);
                    creature = creature.getType("Creature");
                    return creature.size() >= 3;
                }
            }; // SpellAbility

            card.addSpellAbility(spell);
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        /*
         * else if (cardName.equals("Echoing Courage")) { Cost cost = new
         * Cost(card.getManaCost(), cardName, false); Target tgt = new
         * Target(card, "C"); final SpellAbility spell = new Spell(card, cost,
         * tgt) { private static final long serialVersionUID =
         * -8649611733196156346L;
         * 
         * public boolean canPlayAI() { CardList list =
         * AllZoneUtil.getCreaturesInPlay(AllZone.getComputerPlayer()); if
         * (list.isEmpty()) { return false; } else {
         * setTargetCard(CardFactoryUtil.AI_getBestCreature(list)); return true;
         * } } //canPlayAI()
         * 
         * public void resolve() { if (AllZoneUtil.isCardInPlay(getTargetCard())
         * && CardFactoryUtil.canTarget(card, getTargetCard())) { final Card c =
         * getTargetCard();
         * 
         * c.addTempAttackBoost(2); c.addTempDefenseBoost(2);
         * 
         * AllZone.getEndOfTurn().addUntil(new Command() { private static final
         * long serialVersionUID = 1327455269456577020L;
         * 
         * public void execute() { c.addTempAttackBoost(-2);
         * c.addTempDefenseBoost(-2); } });
         * 
         * //get all creatures CardList list =
         * AllZoneUtil.getCardsIn(Zone.Battlefield, getTargetCard().getName());
         * list.remove(getTargetCard());
         * 
         * if (!getTargetCard().isFaceDown()) { for (int i = 0; i < list.size();
         * i++) { final Card crd = list.get(i);
         * 
         * crd.addTempAttackBoost(2); crd.addTempDefenseBoost(2);
         * 
         * AllZone.getEndOfTurn().addUntil(new Command() { private static final
         * long serialVersionUID = 5151337777143949221L;
         * 
         * public void execute() { crd.addTempAttackBoost(-2);
         * crd.addTempDefenseBoost(-2); } }); } }
         * 
         * } //in play? } //resolve() }; //SpellAbility
         * 
         * card.addSpellAbility(spell); }
         */// *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Hurkyl's Recall")) {
            /*
             * Return all artifacts target player owns to his or her hand.
             */
            final Target t = new Target(card, "Select target player", "Player");
            final Cost cost = new Cost("1 U", cardName, false);

            final SpellAbility spell = new Spell(card, cost, t) {
                private static final long serialVersionUID = -4098702062413878046L;

                @Override
                public boolean canPlayAI() {
                    CardList humanArts = AllZone.getHumanPlayer().getCardsIn(Zone.Battlefield);
                    humanArts = humanArts.getType("Artifact");
                    return humanArts.size() > 0;
                } // canPlayAI

                @Override
                public void chooseTargetAI() {
                    this.setTargetPlayer(AllZone.getHumanPlayer());
                } // chooseTargetAI()

                @Override
                public void resolve() {
                    final Player player = this.getTargetPlayer();
                    CardList artifacts = AllZoneUtil.getCardsIn(Zone.Battlefield);
                    artifacts = artifacts.getType("Artifact");

                    for (int i = 0; i < artifacts.size(); i++) {
                        final Card thisArtifact = artifacts.get(i);
                        if (thisArtifact.getOwner().equals(player)) {
                            // moveToHand handles tokens
                            AllZone.getGameAction().moveToHand(thisArtifact);
                        }
                    }
                } // resolve()
            };

            card.addSpellAbility(spell);
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Suffer the Past")) {
            final Cost cost = new Cost("X B", cardName, false);
            final Target tgt = new Target(card, "Select a Player", "Player");
            final SpellAbility spell = new Spell(card, cost, tgt) {
                private static final long serialVersionUID = 1168802375190293222L;

                @Override
                public void resolve() {
                    final Player tPlayer = this.getTargetPlayer();
                    final Player player = card.getController();
                    final int max = card.getXManaCostPaid();

                    final CardList graveList = tPlayer.getCardsIn(Zone.Graveyard);
                    final int x = Math.min(max, graveList.size());

                    if (player.isHuman()) {
                        for (int i = 0; i < x; i++) {
                            final Object o = GuiUtils.getChoice("Remove from game", graveList.toArray());
                            if (o == null) {
                                break;
                            }
                            final Card c1 = (Card) o;
                            graveList.remove(c1); // remove from the display
                                                  // list
                            AllZone.getGameAction().exile(c1);
                        }
                    } else { // Computer
                        // Random random = MyRandom.random;
                        for (int j = 0; j < x; j++) {
                            // int index = random.nextInt(X-j);
                            AllZone.getGameAction().exile(graveList.get(j));
                        }
                    }

                    tPlayer.loseLife(x, card);
                    player.gainLife(x, card);
                    card.setXManaCostPaid(0);
                }

                @Override
                public void chooseTargetAI() {
                    this.setTargetPlayer(AllZone.getHumanPlayer());
                } // chooseTargetAI()

                @Override
                public boolean canPlayAI() {
                    final CardList graveList = AllZone.getHumanPlayer().getCardsIn(Zone.Graveyard);

                    final int maxX = ComputerUtil.getAvailableMana().size() - 1;
                    return (maxX >= 3) && (graveList.size() > 0);
                }
            };

            card.addSpellAbility(spell);
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Demonic Consultation")) {
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = 1481101852928051519L;

                @Override
                public void resolve() {
                    final Player player = card.getController();
                    final CardList libList = player.getCardsIn(Zone.Library);
                    final String[] input = new String[1];
                    input[0] = JOptionPane.showInputDialog(null, "Which card?", "Pick card",
                            JOptionPane.QUESTION_MESSAGE);

                    for (int i = 0; i < 7; i++) {
                        final Card c = libList.get(i);
                        AllZone.getGameAction().exile(c);
                    }

                    final int max = libList.size();
                    int stop = 0;
                    for (int i = 0; i < max; i++) {
                        final Card c = libList.get(i);
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
                    final CardList libList = card.getController().getCardsIn(Zone.Library);
                    return (libList.size() > 6) && super.canPlay();
                }

                @Override
                public boolean canPlayAI() {
                    return false;
                }
            }; // SpellAbility

            spell.setStackDescription("Name a card. Exile the top six cards of your library, "
                    + "then reveal cards from the top of your library until you reveal the named card. "
                    + "Put that card into your hand and exile all other cards revealed this way");
            card.addSpellAbility(spell);
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Siren's Call")) {
            /*
             * Creatures the active player controls attack this turn if able.
             * 
             * At the beginning of the next end step, destroy all non-Wall
             * creatures that player controls that didn't attack this turn.
             * Ignore this effect for each creature the player didn't control
             * continuously since the beginning of the turn.
             */
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = -5746330758531799264L;

                @Override
                public boolean canPlay() {
                    return PhaseUtil.isBeforeAttackersAreDeclared()
                            && AllZone.getPhase().isPlayerTurn(card.getController().getOpponent());
                } // canPlay

                @Override
                public boolean canPlayAI() {
                    return false;
                } // canPlayAI

                @Override
                public void resolve() {
                    // this needs to get a list of opponents creatures and set
                    // the siren flag
                    final Player player = card.getController();
                    final Player opponent = player.getOpponent();
                    final CardList creatures = AllZoneUtil.getCreaturesInPlay(opponent);
                    for (final Card creature : creatures) {
                        // skip walls, skip creatures with summoning sickness
                        // also skip creatures with haste if they came onto the
                        // battlefield this turn
                        if ((!creature.isWall() && !creature.hasSickness())
                                || (creature.hasKeyword("Haste") && (creature.getTurnInZone() != 1))) {
                            creature.setSirenAttackOrDestroy(true);
                            // System.out.println("Siren's Call - setting flag for "+creature.getName());
                        }
                    }
                    final SpellAbility destroy = new Ability(card, "0") {
                        @Override
                        public void resolve() {
                            final Player player = card.getController();
                            final Player opponent = player.getOpponent();
                            final CardList creatures = AllZoneUtil.getCreaturesInPlay(opponent);

                            for (final Card creature : creatures) {
                                // System.out.println("Siren's Call - EOT - "+creature.getName()
                                // +" flag: "+creature.getSirenAttackOrDestroy());
                                // System.out.println("Siren's Call - EOT - "+creature.getName()
                                // +" attacked?: "+creature.getCreatureAttackedThisCombat());
                                if (creature.getSirenAttackOrDestroy() && !creature.getCreatureAttackedThisTurn()) {
                                    if (AllZoneUtil.isCardInPlay(creature)) {
                                        // System.out.println("Siren's Call - destroying "+creature.getName());
                                        // this should probably go on the stack
                                        AllZone.getGameAction().destroy(creature);
                                    }
                                }
                                creature.setSirenAttackOrDestroy(false);
                            }
                        }
                    };
                    final Command atEOT = new Command() {
                        private static final long serialVersionUID = 5369528776959445848L;

                        @Override
                        public void execute() {
                            final StringBuilder sb = new StringBuilder();
                            sb.append(card).append(" - At the beginning of the next end step, ");
                            sb.append("destroy all non-Wall creatures that player controls that didn't ");
                            sb.append("attack this turn. Ignore this effect for each creature the player ");
                            sb.append("didn't control continuously since the beginning of the turn.");
                            destroy.setDescription(sb.toString());
                            destroy.setStackDescription(sb.toString());

                            AllZone.getStack().addSimultaneousStackEntry(destroy);
                        } // execute
                    }; // Command
                    AllZone.getEndOfTurn().addAt(atEOT);
                } // resolve
            }; // SpellAbility

            final StringBuilder sb = new StringBuilder();
            sb.append(card.getName()).append(" - All creatures that can attack must do so or be destroyed.");
            spell.setStackDescription(sb.toString());

            card.addSpellAbility(spell);
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Telling Time")) {
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = 2626878556107707854L;
                private final String[] prompt = new String[] { "Put a card into your hand",
                        "Put a card on top of library", "Put a card on bottom of library" };

                @Override
                public boolean canPlayAI() {
                    return false;
                }

                @Override
                public void resolve() {
                    final PlayerZone lib = card.getController().getZone(Constant.Zone.Library);
                    final CardList choices = new CardList();
                    for (int i = 0; (i < 3) && (lib.size() > 0); i++) {
                        choices.add(lib.get(i));
                    }

                    for (int i = 0; (i < 3) && !choices.isEmpty(); i++) {
                        final Object o = GuiUtils.getChoice(this.prompt[i], choices.toArray());
                        final Card c1 = (Card) o;
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
                    final CardList ens = AllZoneUtil.getCardsIn(Zone.Battlefield).filter(CardListFilter.ENCHANTMENTS);
                    final CardList toReturn = ens.filter(new CardListFilter() {
                        @Override
                        public boolean addCard(final Card c) {
                            final Card enchanting = c.getEnchantingCard();

                            if (enchanting != null) {
                                if ((enchanting.isAttacking() && enchanting.getController().isPlayer(you.getOpponent()))
                                        || enchanting.getController().isPlayer(you)) {
                                    return true;
                                }
                            }

                            return (c.getOwner().isPlayer(you) && c.getController().isPlayer(you));
                        }
                    });
                    for (final Card c : toReturn) {
                        AllZone.getGameAction().moveToHand(c);
                    }

                    for (final Card c : ens) {
                        if (!toReturn.contains(c)) {
                            AllZone.getGameAction().destroy(c);
                        }
                    }
                }
            };

            spell.setStackDescription(card + " - destroy/return enchantments.");

            card.addSpellAbility(spell);
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Burn the Impure")) {
            /*
             * Burn the Impure deals 3 damage to target creature. If that
             * creature has infect, Burn the Impure deals 3 damage to that
             * creature's controller.
             */
            final Cost abCost = new Cost("1 R", cardName, false);
            final SpellAbility spell = new Spell(card, abCost, new Target(card, "TgtC")) {
                private static final long serialVersionUID = -3069135027502686218L;
                private final int damage = 3;

                @Override
                public void chooseTargetAI() {

                    CardList creatures = AllZoneUtil.getCreaturesInPlay(AllZone.getHumanPlayer());
                    creatures = creatures.filter(new CardListFilter() {
                        @Override
                        public boolean addCard(final Card c) {
                            return (c.getNetAttack() <= damage) && !c.hasKeyword("Indestructible");
                        }
                    });
                    final CardList infect = creatures.getKeyword("Infect");
                    if (infect.size() > 0) {
                        final Card c = CardFactoryUtil.getBestCreatureAI(infect);
                        this.setTargetCard(c);
                    } else {
                        final Card c = CardFactoryUtil.getBestCreatureAI(creatures);
                        this.setTargetCard(c);
                    }

                } // chooseTargetAI()

                @Override
                public boolean canPlayAI() {
                    CardList creatures = AllZoneUtil.getCreaturesInPlay(AllZone.getHumanPlayer());
                    creatures = creatures.filter(new CardListFilter() {
                        @Override
                        public boolean addCard(final Card c) {
                            return (c.getNetAttack() <= damage) && !c.hasKeyword("Indestructible");
                        }
                    });
                    return creatures.size() > 0;
                }

                @Override
                public void resolve() {
                    if (AllZoneUtil.isCardInPlay(this.getTargetCard())
                            && CardFactoryUtil.canTarget(card, this.getTargetCard())) {
                        final Card c = this.getTargetCard();
                        c.addDamage(this.damage, card);
                        if (c.hasKeyword("Infect")) {
                            c.getController().addDamage(3, card);
                        }
                    }
                }
            }; // SpellAbility

            final StringBuilder sb = new StringBuilder();
            sb.append(cardName);
            sb.append(" deals 3 damage to target creature. If that creature has infect, ");
            sb.append(cardName);
            sb.append(" deals 3 damage to that creature's controller.");
            spell.setDescription(sb.toString());

            card.addSpellAbility(spell);
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Turnabout")) {
            /*
             * Choose artifact, creature, or land. Tap all untapped permanents
             * of the chosen type target player controls, or untap all tapped
             * permanents of that type that player controls.
             */
            final Cost abCost = new Cost("2 U U", cardName, false);
            final Target target = new Target(card, "Select target player", "Player".split(","));
            final SpellAbility spell = new Spell(card, abCost, target) {
                private static final long serialVersionUID = -2175586347805121896L;

                @Override
                public boolean canPlayAI() {
                    return false;
                }

                @Override
                public void resolve() {
                    final String[] choices = new String[] { "Artifact", "Creature", "Land" };
                    final Object o = GuiUtils.getChoice("Select permanent type", choices);
                    final String cardType = (String) o;
                    final CardList list = this.getTargetPlayer().getCardsIn(Zone.Battlefield).getType(cardType);

                    final String[] tapOrUntap = new String[] { "Tap", "Untap" };
                    final Object z = GuiUtils.getChoice("Tap or Untap?", tapOrUntap);
                    final boolean tap = (z.equals("Tap")) ? true : false;

                    for (final Card c : list) {
                        if (tap) {
                            c.tap();
                        } else {
                            c.untap();
                        }
                    }
                } // resolve()
            }; // SpellAbility
            card.addSpellAbility(spell);
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Wing Puncture")) {

            final Target t2 = new Target(card, "Select target creature with flying", "Creature.withFlying".split(","));
            final AbilitySub sub = new AbilitySub(card, t2) {
                private static final long serialVersionUID = 4618047889975691050L;

                @Override
                public boolean chkAIDrawback() {
                    return false;
                }

                @Override
                public void resolve() {
                    final Card myc = this.getParent().getTargetCard();
                    final Card tgt = this.getTargetCard();
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

            final Cost abCost = new Cost("G", cardName, false);
            final Target t1 = new Target(card, "Select target creature you control", "Creature.YouCtrl".split(","));
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
            spell.setDescription("Target creature you control deals damage "
                    + "equal to its power to target creature with flying.");
            spell.setStackDescription(card
                    + " - Creature you control deals damage equal to its power to creature with flying.");

            card.addSpellAbility(spell);
        } // *************** END ************ END **************************

        return card;
    } // getCard

} // end class CardFactory_Instants
