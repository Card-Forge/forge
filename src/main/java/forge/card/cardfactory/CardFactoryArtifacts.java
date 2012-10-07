package forge.card.cardfactory;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;

import forge.Command;
import forge.Counters;
import forge.Singletons;
import forge.card.cost.Cost;
import forge.card.spellability.Ability;
import forge.card.spellability.AbilityActivated;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.control.input.Input;
import forge.game.player.Player;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;
import forge.gui.match.CMatchUI;

import forge.view.ButtonUtil;

/** 
 * TODO: Write javadoc for this type.
 *
 */
class CardFactoryArtifacts {

    /**
     * TODO: Write javadoc for this method.
     * @param card
     * @param cardName
     * @return
     */
    public static void buildCard(final Card card, final String cardName) {

        if (cardName.equals("Sarpadian Empires, Vol. VII")) {

            final String[] choices = { "Citizen", "Camarid", "Thrull", "Goblin", "Saproling" };
            final Player player = card.getController();
            class SarpadianEmpiresVolVIIETB extends Ability {
                public SarpadianEmpiresVolVIIETB(final Card c, final String s) {
                    super(c, s);
                }
                @Override
                public void resolve() {
                    String type = "";
                    String imageName = "";
                    String color = "";

                    if (player.isComputer()) {
                        type = "Thrull";
                        imageName = "B 1 1 Thrull";
                        color = "B";
                    } else if (player.isHuman()) {
                        final Object q = GuiChoose.oneOrNone("Select type of creature", choices);
                        if (q != null) {
                            if (q.equals("Citizen")) {
                                type = "Citizen";
                                imageName = "W 1 1 Citizen";
                                color = "W";
                            } else if (q.equals("Camarid")) {
                                type = "Camarid";
                                imageName = "U 1 1 Camarid";
                                color = "U";
                            } else if (q.equals("Thrull")) {
                                type = "Thrull";
                                imageName = "B 1 1 Thrull";
                                color = "B";
                            } else if (q.equals("Goblin")) {
                                type = "Goblin";
                                imageName = "R 1 1 Goblin";
                                color = "R";
                            } else if (q.equals("Saproling")) {
                                type = "Saproling";
                                imageName = "G 1 1 Saproling";
                                color = "G";
                            }
                        }
                    }
                    getSourceCard().setChosenType(type);

                    final String t = type;
                    final String in = imageName;
                    final String col = color;
                    // card.setChosenType(input[0]);

                    class AbilitySarpadianEmpiresVolVII extends AbilityActivated {

                        private static final long serialVersionUID = -2114111483117171609L;

                        public AbilitySarpadianEmpiresVolVII(Card ca, Cost co, Target t) {
                            super(ca, co, t);
                        }

                        @Override
                        public AbilityActivated getCopy() {
                            AbilityActivated res = new AbilitySarpadianEmpiresVolVII(getSourceCard(),
                                    getPayCosts(), getTarget() == null ? null : new Target(getTarget()));
                            CardFactoryUtil.copySpellAbility(this, res);
                            return res;
                        }

                        @Override
                        public void resolve() {
                            CardFactoryUtil.makeToken(t, in, getSourceCard().getController(), col, new String[] { "Creature", t },
                                    1, 1, new String[] { "" });
                        }
                    }
                    final Cost a1Cost = new Cost(getSourceCard(), "3 T", true);
                    final AbilityActivated a1 = new AbilitySarpadianEmpiresVolVII(card, a1Cost, null);
                    final StringBuilder sb = new StringBuilder();
                    sb.append(card.getName()).append(" - ").append(card.getController());
                    sb.append(" puts a 1/1 ").append(t).append(" token onto the battlefield");
                    a1.setStackDescription(sb.toString());

                    card.addSpellAbility(a1);
                }
            } // ability
            final SpellAbility ability = new SarpadianEmpiresVolVIIETB(card, "0");
            final Command intoPlay = new Command() {
                private static final long serialVersionUID = 7202704600935499188L;

                @Override
                public void execute() {
                    final StringBuilder sb = new StringBuilder();
                    sb.append("As Sarpadian Empires, Vol. VII enters the battlefield, ");
                    sb.append("choose white Citizen, blue Camarid, black Thrull, red Goblin, or green Saproling.");
                    ability.setStackDescription(sb.toString());

                    AllZone.getStack().addSimultaneousStackEntry(ability);

                }
            };
            final StringBuilder sb = new StringBuilder();
            sb.append("As Sarpadian Empires, Vol. VII enters the battlefield, ");
            sb.append("choose white Citizen, blue Camarid, black Thrull, red Goblin, or green Saproling.\r\n");
            sb.append("3, Tap: Put a 1/1 creature token of the chosen color and type onto the battlefield.\r\n");
            sb.append(card.getText()); // In the slight chance that there may be
                                       // a need to add a note to this card.
            card.setText(sb.toString());

            card.addComesIntoPlayCommand(intoPlay);

        } // *************** END ************ END **************************


        // *************** START *********** START **************************
        else if (cardName.equals("Goblin Charbelcher")) {
            final Cost abCost = new Cost(card, "3 T", true);
            class AbilityGoblinCharbelcher extends AbilityActivated {
                public AbilityGoblinCharbelcher(final Card ca, final Cost co, final Target t) {
                    super(ca, co, t);
                }

                @Override
                public AbilityActivated getCopy() {
                    AbilityActivated res = new AbilityGoblinCharbelcher(getSourceCard(),
                            getPayCosts(), getTarget() == null ? null : new Target(getTarget()));
                    CardFactoryUtil.copySpellAbility(this, res);
                    return res;
                }

                private static final long serialVersionUID = -840041589720758423L;

                @Override
                public void resolve() {
                    final List<Card> topOfLibrary = card.getController().getCardsIn(ZoneType.Library);
                    final List<Card> revealed = new ArrayList<Card>();

                    if (topOfLibrary.size() == 0) {
                        return;
                    }

                    int damage = 0;
                    int count = 0;
                    Card c = null;
                    Card crd;
                    while (c == null) {
                        revealed.add(topOfLibrary.get(count));
                        crd = topOfLibrary.get(count++);
                        if (crd.isLand() || (count == topOfLibrary.size())) {
                            c = crd;
                            damage = count;
                            if (crd.isLand()) {
                                damage--;
                            }

                            if (crd.isType("Mountain")) {
                                damage *= 2;
                            }
                        }
                    } // while
                    GuiChoose.oneOrNone("Revealed cards:", revealed);
                    for (final Card revealedCard : revealed) {
                        Singletons.getModel().getGameAction().moveToBottomOfLibrary(revealedCard);
                    }

                    if (this.getTargetCard() != null) {
                        if (AllZoneUtil.isCardInPlay(this.getTargetCard())
                                && this.getTargetCard().canBeTargetedBy(this)) {
                            this.getTargetCard().addDamage(damage, card);
                        }
                    } else {
                        this.getTargetPlayer().addDamage(damage, card);
                    }
                }
            }

            final AbilityActivated ability = new AbilityGoblinCharbelcher(card, abCost, new Target(card, "TgtCP"));

            final StringBuilder sb = new StringBuilder();
            sb.append(abCost);
            sb.append("Reveal cards from the top of your library until you reveal a land card. ");
            sb.append("Goblin Charbelcher deals damage equal to the number of nonland cards revealed ");
            sb.append("this way to target creature or player. If the revealed land card was a ");
            sb.append("Mountain, Goblin Charbelcher deals double that damage instead. Put the ");
            sb.append("revealed cards on the bottom of your library in any order.");
            ability.setDescription(sb.toString());

            ability.setChooseTargetAI(CardFactoryUtil.targetHumanAI);
            card.addSpellAbility(ability);
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        /*else if (cardName.equals("Lodestone Bauble")) {
            /*
             * 1, Tap, Sacrifice Lodestone Bauble: Put up to four target basic
             * land cards from a player's graveyard on top of his or her library
             * in any order. That player draws a card at the beginning of the
             * next turn's upkeep.
             */

            /*class AbilityLodestoneBauble extends AbilityActivated {
                public AbilityLodestoneBauble(final Card ca, final Cost co, final Target t) {
                    super(ca, co, t);
                }

                @Override
                public AbilityActivated getCopy() {
                    AbilityActivated res = new AbilityLodestoneBauble(getSourceCard(),
                            getPayCosts(), getTarget() == null ? null : new Target(getTarget()));
                    CardFactoryUtil.copySpellAbility(this, res);
                    return res;
                }

                private static final long serialVersionUID = -6711849408085138636L;

                @Override
                public boolean canPlayAI() {
                    return this.getComputerLands(getActivatingPlayer()).size() >= 4;
                }

                @Override
                public void chooseTargetAI() {
                    this.setTargetPlayer(getActivatingPlayer());
                } // chooseTargetAI()

                @Override
                public void resolve() {
                    final int limit = 4; // at most, this can target 4 cards
                    final Player player = this.getTargetPlayer();

                    List<Card> lands = player.getCardsIn(ZoneType.Graveyard);
                    lands = CardLists.filter(lands, Presets.BASIC_LANDS);
                    if (card.getController().isHuman()) {
                        // now, select up to four lands
                        int end = -1;
                        end = Math.min(lands.size(), limit);
                        // TODO - maybe pop a message box here that no basic
                        // lands found (if necessary)
                        for (int i = 1; i <= end; i++) {
                            String title = "Put on top of library: ";
                            if (i == 2) {
                                title = "Put second from top of library: ";
                            }
                            if (i == 3) {
                                title = "Put third from top of library: ";
                            }
                            if (i == 4) {
                                title = "Put fourth from top of library: ";
                            }
                            final Object o = GuiChoose.oneOrNone(title, lands);
                            if (o == null) {
                                break;
                            }
                            final Card c1 = (Card) o;
                            lands.remove(c1); // remove from the display list
                            Singletons.getModel().getGameAction().moveToLibrary(c1, i - 1);
                        }
                    } else { // Computer
                        // based on current AI, computer should always target
                        // himself.
                        final List<Card> list = this.getComputerLands(card.getController());
                        int max = list.size();
                        if (max > limit) {
                            max = limit;
                        }

                        for (int i = 0; i < max; i++) {
                            Singletons.getModel().getGameAction().moveToLibrary(list.get(i));
                        }
                    }

                    player.addSlowtripList(card);
                }

                private List<Card> getComputerLands(final Player ai) {
                    return CardLists.filter(ai.getCardsIn(ZoneType.Graveyard), CardPredicates.Presets.BASIC_LANDS);
                }
            }
            final Cost abCost = new Cost(card, "1 T Sac<1/CARDNAME>", true);
            final Target target = new Target(card, "Select target player", new String[] { "Player" });
            final AbilityActivated ability = new AbilityLodestoneBauble(card, abCost, target);

            final StringBuilder sb = new StringBuilder();
            sb.append(abCost);
            sb.append("Put up to four target basic land cards from a player's graveyard on top ");
            sb.append("of his or her library in any order. That player draws a card at the ");
            sb.append("beginning of the next turn's upkeep.");
            ability.setDescription(sb.toString());
            card.addSpellAbility(ability);
        }*/ // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Grindstone")) {

            class AbilityGrindstone extends AbilityActivated {
                public AbilityGrindstone(final Card ca, final Cost co, final Target t) {
                    super(ca, co, t);
                }

                @Override
                public AbilityActivated getCopy() {
                    AbilityActivated res = new AbilityGrindstone(getSourceCard(),
                            getPayCosts(), getTarget() == null ? null : new Target(getTarget()));
                    CardFactoryUtil.copySpellAbility(this, res);
                    res.setChooseTargetAI(CardFactoryUtil.targetHumanAI);
                    return res;
                }

                private static final long serialVersionUID = -6281219446216L;

                @Override
                public boolean canPlayAI() {
                    final List<Card> libList = getActivatingPlayer().getOpponent().getCardsIn(ZoneType.Library);
                    // List<Card> list =
                    // AllZoneUtil.getCardsInPlay("Painter's Servant");
                    return !libList.isEmpty(); // && list.size() > 0;
                }

                @Override
                public void resolve() {
                    final Player target = this.getTargetPlayer();
                    final List<Card> library = this.getTargetPlayer().getCardsIn(ZoneType.Library);

                    boolean loop = true;
                    final List<Card> grinding = new ArrayList<Card>();
                    do {
                        grinding.clear();

                        for (int i = 0; i < 2; i++) {
                            // Move current grinding to a different list
                            if (library.size() > 0) {
                                final Card c = library.get(0);
                                grinding.add(c);
                                library.remove(c);
                            } else {
                                loop = false;
                                break;
                            }
                        }

                        // if current grinding dont share a color, stop grinding
                        if (loop) {
                            loop = grinding.get(0).sharesColorWith(grinding.get(1));
                        }
                        target.mill(grinding.size());
                    } while (loop);
                }
            }

            final Target target = new Target(card, "Select target player", new String[] { "Player" });
            final Cost abCost = new Cost(card, "3 T", true);
            final AbilityActivated ab1 = new AbilityGrindstone(card, abCost, target);

            ab1.setChooseTargetAI(CardFactoryUtil.targetHumanAI);
            final StringBuilder sb = new StringBuilder();
            sb.append(abCost);
            sb.append("Put the top two cards of target player's library into that player's graveyard. ");
            sb.append("If both cards share a color, repeat this process.");
            ab1.setDescription(sb.toString());
            card.addSpellAbility(ab1);
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Everflowing Chalice")) {
            final Command comesIntoPlay = new Command() {
                private static final long serialVersionUID = 4245563898487609274L;

                @Override
                public void execute() {
                    card.addCounter(Counters.CHARGE, card.getMultiKickerMagnitude());
                    card.setMultiKickerMagnitude(0);
                }
            };
            card.addComesIntoPlayCommand(comesIntoPlay);
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Phyrexian Processor")) {
            final Command intoPlay = new Command() {
                private static final long serialVersionUID = 5634360316643996274L;

                @Override
                public void execute() {

                    final Player player = card.getController();
                    int lifeToPay = 0;
                    if (player.isHuman()) {
                        final int num = card.getController().getLife();
                        final String[] choices = new String[num + 1];
                        for (int j = 0; j <= num; j++) {
                            choices[j] = "" + j;
                        }
                        final String answer = (GuiChoose.oneOrNone("Life to pay:", choices));
                        lifeToPay = Integer.parseInt(answer);
                    } else {
                        // not implemented for Compy
                    }

                    if (player.payLife(lifeToPay, card)) {
                        card.setXLifePaid(lifeToPay);
                    }

                }
            };
            card.addComesIntoPlayCommand(intoPlay);
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Scroll Rack")) {
            class AbilityScrollRack extends AbilityActivated {
                public AbilityScrollRack(final Card ca, final Cost co, final Target t) {
                    super(ca, co, t);
                }

                @Override
                public AbilityActivated getCopy() {
                    AbilityActivated res = new AbilityScrollRack(getSourceCard(),
                            getPayCosts(), getTarget() == null ? null : new Target(getTarget()));
                    CardFactoryUtil.copySpellAbility(this, res);
                    return res;
                }

                private static final long serialVersionUID = -5588587187720068547L;

                @Override
                public void resolve() {
                    // not implemented for compy
                    if (card.getController().isHuman()) {
                        AllZone.getInputControl().setInput(new Input() {
                            private static final long serialVersionUID = -2305549394512889450L;
                            private final List<Card> exiled = new ArrayList<Card>();

                            @Override
                            public void showMessage() {
                                final StringBuilder sb = new StringBuilder();
                                sb.append(card.getName()).append(" - Exile cards from hand.  Currently, ");
                                sb.append(this.exiled.size()).append(" selected.  (Press OK when done.)");
                                CMatchUI.SINGLETON_INSTANCE.showMessage(sb.toString());
                                ButtonUtil.enableOnlyOK();
                            }

                            @Override
                            public void selectButtonOK() {
                                this.done();
                            }

                            @Override
                            public void selectCard(final Card c, final PlayerZone zone) {
                                if (zone.is(ZoneType.Hand, card.getController()) && !this.exiled.contains(c)) {
                                    this.exiled.add(c);
                                    this.showMessage();
                                }
                            }

                            public void done() {
                                // exile those cards
                                for (final Card c : this.exiled) {
                                    Singletons.getModel().getGameAction().exile(c);
                                }

                                // Put that many cards from the top of your
                                // library into your hand.
                                // Ruling: This is not a draw...
                                final PlayerZone lib = card.getController().getZone(ZoneType.Library);
                                int numCards = 0;
                                while ((lib.size() > 0) && (numCards < this.exiled.size())) {
                                    Singletons.getModel().getGameAction().moveToHand(lib.get(0));
                                    numCards++;
                                }

                                final StringBuilder sb = new StringBuilder();
                                sb.append(card.getName()).append(" - Returning cards to top of library.");
                                CMatchUI.SINGLETON_INSTANCE.showMessage(sb.toString());

                                // Then look at the exiled cards and put them on
                                // top of your library in any order.
                                while (this.exiled.size() > 0) {
                                    final Card c1 = GuiChoose.one("Put a card on top of your library.", this.exiled);
                                    Singletons.getModel().getGameAction().moveToLibrary(c1);
                                    this.exiled.remove(c1);
                                }

                                this.stop();
                            }
                        });
                    }
                }

                @Override
                public boolean canPlayAI() {
                    return false;
                }
            }
            final Cost abCost = new Cost(card, "1 T", true);
            final AbilityActivated ability = new AbilityScrollRack(card, abCost, null);

            final StringBuilder sbDesc = new StringBuilder();
            sbDesc.append(abCost);
            sbDesc.append("Exile any number of cards from your hand face down. Put that many cards ");
            sbDesc.append("from the top of your library into your hand. Then look at the exiled cards ");
            sbDesc.append("and put them on top of your library in any order.");
            ability.setDescription(sbDesc.toString());

            final StringBuilder sbStack = new StringBuilder();
            sbStack.append(cardName).append(" - exile any number of cards from your hand.");
            ability.setStackDescription(sbStack.toString());
            card.addSpellAbility(ability);
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Temporal Aperture")) {
            /*
             * 5, Tap: Shuffle your library, then reveal the top card. Until end
             * of turn, for as long as that card remains on top of your library,
             * play with the top card of your library revealed and you may play
             * that card without paying its mana cost. (If it has X in its mana
             * cost, X is 0.)
             */
            final Card[] topCard = new Card[1];

            final Ability freeCast = new Ability(card, "0") {

                @Override
                public boolean canPlay() {
                    final PlayerZone lib = card.getController().getZone(ZoneType.Library);
                    return super.canPlay() && ((lib.size() > 0) && lib.get(0).equals(topCard[0]));
                }

                @Override
                public void resolve() {
                    final Card freeCard = topCard[0];
                    final Player player = card.getController();
                    if (freeCard != null) {
                        if (freeCard.isLand()) {
                            if (player.canPlayLand()) {
                                player.playLand(freeCard);
                            } else {
                                JOptionPane.showMessageDialog(null, "You can't play any more lands this turn.", "",
                                        JOptionPane.INFORMATION_MESSAGE);
                            }
                        } else {
                            Singletons.getModel().getGameAction().playCardWithoutManaCost(freeCard);
                        }
                    } else {
                        final StringBuilder sb = new StringBuilder();
                        sb.append("Error in ").append(cardName).append(".  freeCard is null");
                        JOptionPane.showMessageDialog(null, sb.toString(), "", JOptionPane.INFORMATION_MESSAGE);
                    }

                }

                @Override
                public boolean canPlayAI() {
                    return false;
                }

            };
            freeCast.setDescription("Play the previously revealed top card of your library for free.");
            final StringBuilder sb = new StringBuilder();
            sb.append(cardName).append(" - play card without paying its mana cost.");
            freeCast.setStackDescription(sb.toString());

            class AbilityTemporalAperture extends AbilityActivated {
                public AbilityTemporalAperture(final Card ca, final Cost co, final Target t) {
                    super(ca, co, t);
                }

                @Override
                public AbilityActivated getCopy() {
                    AbilityActivated res = new AbilityTemporalAperture(getSourceCard(),
                            getPayCosts(), getTarget() == null ? null : new Target(getTarget()));
                    CardFactoryUtil.copySpellAbility(this, res);
                    return res;
                }

                private static final long serialVersionUID = -7328518969488588777L;

                @Override
                public void resolve() {
                    final PlayerZone lib = card.getController().getZone(ZoneType.Library);
                    if (lib.size() > 0) {

                        // shuffle your library
                        card.getController().shuffle();

                        // reveal the top card
                        topCard[0] = lib.get(0);
                        final StringBuilder sb = new StringBuilder();
                        sb.append("Revealed card:\n").append(topCard[0].getName());
                        JOptionPane.showMessageDialog(null, sb.toString(), card.getName(), JOptionPane.PLAIN_MESSAGE);

                        card.addSpellAbility(freeCast);
                        card.addExtrinsicKeyword("Play with the top card of your library revealed.");
                        AllZone.getEndOfTurn().addUntil(new Command() {
                            private static final long serialVersionUID = -2860753262177388046L;

                            @Override
                            public void execute() {
                                card.removeSpellAbility(freeCast);
                                card.removeExtrinsicKeyword("Play with the top card of your library revealed.");
                            }
                        });
                    }
                } // resolve

                @Override
                public boolean canPlayAI() {
                    return false;
                }
            }

            final Cost abCost = new Cost(card, "5 T", true);
            final AbilityActivated ability = new AbilityTemporalAperture(card, abCost, null);

            final StringBuilder sbStack = new StringBuilder();
            sbStack.append(card).append(" - Shuffle your library, then reveal the top card.");
            ability.setStackDescription(sbStack.toString());

            final StringBuilder sbDesc = new StringBuilder();
            sbDesc.append(abCost).append("Shuffle your library, then reveal the top card. ");
            sbDesc.append("Until end of turn, for as long as that card remains on top of your ");
            sbDesc.append("library, play with the top card of your library revealed ");
            sbDesc.append("and you may play that card without paying its mana cost. ");
            sbDesc.append("(If it has X in its mana cost, X is 0.)");
            ability.setDescription(sbDesc.toString());

            card.addSpellAbility(ability);
        } // *************** END ************ END **************************
    }

}
