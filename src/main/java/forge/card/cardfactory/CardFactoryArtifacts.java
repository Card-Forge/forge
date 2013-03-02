package forge.card.cardfactory;

import java.util.ArrayList;
import java.util.List;

import forge.Card;

import forge.Singletons;
import forge.card.cost.Cost;
import forge.card.spellability.AbilityActivated;
import forge.card.spellability.Target;
import forge.control.input.Input;
import forge.control.input.InputSelectManyCards;
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
                    Player human = getActivatingPlayer().getOpponent();
                    final List<Card> libList = human.getCardsIn(ZoneType.Library);
                    this.getTarget().addTarget(human);
                    return !libList.isEmpty() && canTarget(human);
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
            ab1.setStackDescription(sb.toString());
            card.addSpellAbility(ab1);
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
    }
}
