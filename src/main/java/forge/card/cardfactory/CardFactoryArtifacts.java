package forge.card.cardfactory;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import forge.Card;

import forge.Command;
import forge.CounterType;
import forge.Singletons;
import forge.card.cost.Cost;
import forge.card.spellability.Ability;
import forge.card.spellability.AbilityActivated;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.control.input.Input;
import forge.control.input.InputSelectManyCards;
import forge.game.player.ComputerUtil;
import forge.game.player.Player;
import forge.game.zone.PlayerZone;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;
import forge.gui.match.CMatchUI;

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

        // *************** START *********** START **************************
        if (cardName.equals("Grindstone")) {

            class AbilityGrindstone extends AbilityActivated {
                public AbilityGrindstone(final Card ca, final Cost co, final Target t) {
                    super(ca, co, t);
                }

                @Override
                public AbilityActivated getCopy() {
                    AbilityActivated res = new AbilityGrindstone(getSourceCard(),
                            getPayCosts(), getTarget() == null ? null : new Target(getTarget()));
                    CardFactoryUtil.copySpellAbility(this, res);
                    return res;
                }

                private static final long serialVersionUID = -6281219446216L;

                @Override
                public boolean canPlayAI() {
                    this.getTarget().resetTargets();
                    final List<Card> libList = getActivatingPlayer().getOpponent().getCardsIn(ZoneType.Library);
                    return !libList.isEmpty() && ComputerUtil.targetHumanAI(this);
                }

                @Override
                public void resolve() {
                    final Player target = this.getTargetPlayer();
                    final List<Card> library = new ArrayList<Card>(this.getTargetPlayer().getCardsIn(ZoneType.Library));

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
                    card.addCounter(CounterType.CHARGE, card.getMultiKickerMagnitude(), true);
                    card.setMultiKickerMagnitude(0);
                }
            };
            card.addComesIntoPlayCommand(comesIntoPlay);
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

                        InputSelectManyCards inp = new InputSelectManyCards(0, Integer.MAX_VALUE) {
                            private static final long serialVersionUID = 806464726820739922L;

                            @Override
                            protected boolean isValidChoice(Card c) {
                                Zone zone = Singletons.getModel().getGame().getZoneOf(c);
                                return zone.is(ZoneType.Hand) && c.getController() == card.getController();
                            }

                            /* (non-Javadoc)
                             * @see forge.control.input.InputSelectManyCards#onDone()
                             */
                            @Override
                            protected Input onDone() {
                                for (final Card c : selected) {
                                    Singletons.getModel().getGame().getAction().exile(c);
                                }

                                // Put that many cards from the top of your
                                // library into your hand.
                                // Ruling: This is not a draw...
                                final PlayerZone lib = card.getController().getZone(ZoneType.Library);
                                int numCards = 0;
                                while ((lib.size() > 0) && (numCards < selected.size())) {
                                    Singletons.getModel().getGame().getAction().moveToHand(lib.get(0));
                                    numCards++;
                                }

                                final StringBuilder sb = new StringBuilder();
                                sb.append(card.getName()).append(" - Returning cards to top of library.");
                                CMatchUI.SINGLETON_INSTANCE.showMessage(sb.toString());

                                // Then look at the exiled cards and put them on
                                // top of your library in any order.
                                while (selected.size() > 0) {
                                    final Card c1 = GuiChoose.one("Put a card on top of your library.", selected);
                                    Singletons.getModel().getGame().getAction().moveToLibrary(c1);
                                    selected.remove(c1);
                                }
                                return null;                            }
                        };
                        inp.setMessage(card.getName() + " - Exile cards from hand.  Currently, %d selected.  (Press OK when done.)");

                        Singletons.getModel().getMatch().getInput().setInput(inp);

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
                            Singletons.getModel().getGame().getAction().playCardWithoutManaCost(freeCard);
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
                        Singletons.getModel().getGame().getEndOfTurn().addUntil(new Command() {
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
