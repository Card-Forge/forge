package forge.card.abilityfactory.effects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.base.Predicate;

import forge.Card;
import forge.CardLists;
import forge.CardUtil;
import forge.GameActionUtil;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.ComputerUtil;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;

public class DiscardEffect extends RevealEffectBase {
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final String mode = sa.getParam("Mode");
        final StringBuilder sb = new StringBuilder();

        final List<Player> tgtPlayers = getTargetPlayers(sa);

        if (tgtPlayers.size() > 0) {

            for (final Player p : tgtPlayers) {
                sb.append(p.toString()).append(" ");
            }

            if (mode.equals("RevealYouChoose")) {
                sb.append("reveals his or her hand.").append("  You choose (");
            } else if (mode.equals("RevealDiscardAll")) {
                sb.append("reveals his or her hand. Discard (");
            } else {
                sb.append("discards (");
            }

            int numCards = 1;
            if (sa.hasParam("NumCards")) {
                numCards = AbilityFactory.calculateAmount(sa.getSourceCard(), sa.getParam("NumCards"), sa);
            }

            if (mode.equals("Hand")) {
                sb.append("his or her hand");
            } else if (mode.equals("RevealDiscardAll")) {
                sb.append("All");
            } else if (sa.hasParam("AnyNumber")) {
                sb.append("any number");
            } else if (sa.hasParam("NumCards") && sa.getParam("NumCards").equals("X")
                    && sa.getSVar("X").equals("Remembered$Amount")) {
                sb.append("that many");
            } else {
                sb.append(numCards);
            }

            sb.append(")");

            if (mode.equals("RevealYouChoose")) {
                sb.append(" to discard");
            } else if (mode.equals("RevealDiscardAll")) {
                String valid = sa.getParam("DiscardValid");
                if (valid == null) {
                    valid = "Card";
                }
                sb.append(" of type: ").append(valid);
            }

            if (mode.equals("Defined")) {
                sb.append(" defined cards");
            }

            if (mode.equals("Random")) {
                sb.append(" at random.");
            } else {
                sb.append(".");
            }
        }
        return sb.toString();
    } // discardStackDescription()

    @Override
    public void resolve(SpellAbility sa) {
        final Card source = sa.getSourceCard();
        final String mode = sa.getParam("Mode");
        //final boolean anyNumber = sa.hasParam("AnyNumber");

        final Target tgt = sa.getTarget();

        final List<Card> discarded = new ArrayList<Card>();

        for (final Player p : getTargetPlayers(sa)) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                if (mode.equals("Defined")) {
                    final ArrayList<Card> toDiscard = AbilityFactory.getDefinedCards(source, sa.getParam("DefinedCards"),
                            sa);
                    for (final Card c : toDiscard) {
                        discarded.addAll(p.discard(c, sa));
                    }
                    if (sa.hasParam("RememberDiscarded")) {
                        for (final Card c : discarded) {
                            source.addRemembered(c);
                        }
                    }
                    continue;
                }

                if (mode.equals("Hand")) {
                    final List<Card> list = p.discardHand(sa);
                    if (sa.hasParam("RememberDiscarded")) {
                        for (final Card c : list) {
                            source.addRemembered(c);
                        }
                    }
                    continue;
                }

                if (mode.equals("NotRemembered")) {
                    final List<Card> dPHand =
                            CardLists.getValidCards(p.getCardsIn(ZoneType.Hand), "Card.IsNotRemembered", source.getController(), source);
                    for (final Card c : dPHand) {
                        p.discard(c, sa);
                        discarded.add(c);
                    }
                }

                int numCards = 1;
                if (sa.hasParam("NumCards")) {
                    numCards = AbilityFactory.calculateAmount(sa.getSourceCard(), sa.getParam("NumCards"), sa);
                    if (p.getCardsIn(ZoneType.Hand).size() > 0
                            && p.getCardsIn(ZoneType.Hand).size() < numCards) {
                        // System.out.println("Scale down discard from " + numCards + " to " + p.getCardsIn(ZoneType.Hand).size());
                        numCards = p.getCardsIn(ZoneType.Hand).size();
                    }
                }

                if (mode.equals("Random")) {
                    boolean runDiscard = true;
                    if (sa.hasParam("Optional")) {
                       if (p.isHuman()) {
                           // TODO Ask if Human would like to discard a card at Random
                           StringBuilder sb = new StringBuilder("Would you like to discard ");
                           sb.append(numCards).append(" random card(s)?");
                           runDiscard = GameActionUtil.showYesNoDialog(source, sb.toString());
                       }
                       else {
                           // TODO For now AI will always discard Random used currently with:
                           // Balduvian Horde and similar cards
                       }
                    }

                    if (runDiscard) {
                        final String valid = sa.hasParam("DiscardValid") ? sa.getParam("DiscardValid") : "Card";
                        discarded.addAll(p.discardRandom(numCards, sa, valid));
                    }
                } else if (mode.equals("TgtChoose") && sa.hasParam("UnlessType")) {
                    p.discardUnless(numCards, sa.getParam("UnlessType"), sa);
                } else if (mode.equals("RevealDiscardAll")) {
                    // Reveal
                    final List<Card> dPHand = p.getCardsIn(ZoneType.Hand);

                    if (p.isHuman()) {
                        // "reveal to computer" for information gathering
                    } else {
                        GuiChoose.oneOrNone("Revealed computer hand", dPHand);
                    }

                    String valid = sa.getParam("DiscardValid");
                    if (valid == null) {
                        valid = "Card";
                    }

                    if (valid.contains("X")) {
                        valid = valid.replace("X", Integer.toString(AbilityFactory.calculateAmount(source, "X", sa)));
                    }

                    final List<Card> dPChHand = CardLists.getValidCards(dPHand, valid.split(","), source.getController(), source);
                    // Reveal cards that will be discarded?
                    for (final Card c : dPChHand) {
                        p.discard(c, sa);
                        discarded.add(c);
                    }
                } else if (mode.equals("RevealYouChoose") || mode.equals("RevealOppChoose") || mode.equals("TgtChoose")) {
                    // Is Reveal you choose right? I think the wrong player is
                    // being used?
                    List<Card> dPHand = new ArrayList<Card>(p.getCardsIn(ZoneType.Hand));
                    if (dPHand.size() != 0) {
                        if (sa.hasParam("RevealNumber")) {
                            String amountString = sa.getParam("RevealNumber");
                            int amount = amountString.matches("[0-9][0-9]?") ? Integer.parseInt(amountString)
                                    : CardFactoryUtil.xCount(source, source.getSVar(amountString));
                            dPHand = getRevealedList(p, dPHand, amount, false);
                        }
                        List<Card> dPChHand = new ArrayList<Card>(dPHand);
                        String[] dValid = null;
                        if (sa.hasParam("DiscardValid")) { // Restrict card choices
                            dValid = sa.getParam("DiscardValid").split(",");
                            dPChHand = CardLists.getValidCards(dPHand, dValid, source.getController(), source);
                        }
                        Player chooser = p;
                        if (mode.equals("RevealYouChoose")) {
                            chooser = source.getController();
                        } else if (mode.equals("RevealOppChoose")) {
                            chooser = source.getController().getOpponent();
                        }

                        if (chooser.isComputer()) {
                            // AI
                            if (p.isComputer()) { // discard AI cards
                                int max = chooser.getCardsIn(ZoneType.Hand).size();
                                max = Math.min(max, numCards);
                                List<Card> list = ComputerUtil.discardNumTypeAI(p, max, dValid, sa);
                                if (mode.startsWith("Reveal")) {
                                    GuiChoose.oneOrNone("Computer has chosen", list);
                                }
                                if (list != null) {
                                    discarded.addAll(list);
                                    for (Card card : list) {
                                        p.discard(card, sa);
                                    }
                                }
                                continue;
                            }
                            // discard human cards
                            for (int i = 0; i < numCards; i++) {
                                if (dPChHand.size() > 0) {
                                    List<Card> goodChoices = CardLists.filter(dPChHand, new Predicate<Card>() {
                                        @Override
                                        public boolean apply(final Card c) {
                                            if (c.hasKeyword("If a spell or ability an opponent controls causes you to discard CARDNAME,"
                                                    + " put it onto the battlefield instead of putting it into your graveyard.")
                                                    || !c.getSVar("DiscardMe").equals("")) {
                                                return false;
                                            }
                                            return true;
                                        }
                                    });
                                    if (goodChoices.isEmpty()) {
                                        goodChoices = dPChHand;
                                    }
                                    final List<Card> dChoices = new ArrayList<Card>();
                                    if (sa.hasParam("DiscardValid")) {
                                        final String validString = sa.getParam("DiscardValid");
                                        if (validString.contains("Creature") && !validString.contains("nonCreature")) {
                                            final Card c = CardFactoryUtil.getBestCreatureAI(goodChoices);
                                            if (c != null) {
                                                dChoices.add(CardFactoryUtil.getBestCreatureAI(goodChoices));
                                            }
                                        }
                                    }

                                    Collections.sort(goodChoices, CardLists.TextLenReverseComparator);

                                    CardLists.sortCMC(goodChoices);
                                    dChoices.add(goodChoices.get(0));

                                    final Card dC = CardUtil.getRandom(goodChoices);
                                    dPChHand.remove(dC);

                                    if (mode.startsWith("Reveal")) {
                                        final List<Card> dCs = new ArrayList<Card>();
                                        dCs.add(dC);
                                        GuiChoose.oneOrNone("Computer has chosen", dCs);
                                    }
                                    discarded.add(dC);
                                    p.discard(dC, sa);
                                }
                            }
                        } else {
                            // human
                            if (mode.startsWith("Reveal")) {
                                GuiChoose.oneOrNone("Revealed " + p + "  hand", dPHand);
                            }

                            for (int i = 0; i < numCards; i++) {
                                if (dPChHand.size() > 0) {
                                    Card dC = null;
                                    if (sa.hasParam("AnyNumber")) {
                                        dPChHand = getDiscardedList(p, dPChHand, dPChHand.size(), true);
                                        for (Card c : dPChHand) {
                                            discarded.add(c);
                                            p.discard(c, sa);
                                        }
                                    }
                                    else if (sa.hasParam("Optional")) {
                                        dC = GuiChoose.oneOrNone("Choose a card to be discarded", dPChHand);
                                    } else {
                                        dC = GuiChoose.one("Choose a card to be discarded", dPChHand);
                                    } if (dC != null) {
                                        dPChHand.remove(dC);
                                        discarded.add(dC);
                                        p.discard(dC, sa);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (sa.hasParam("RememberDiscarded")) {
            for (final Card c : discarded) {
                source.addRemembered(c);
            }
        }

    } // discardResolve()

    public static List<Card> getDiscardedList(final Player player, final List<Card> valid, final int max, boolean anyNumber) {
        final List<Card> chosen = new ArrayList<Card>();
        final int validamount = Math.min(valid.size(), max);

        if (anyNumber && player.isHuman() && validamount > 0) {
            final List<Card> selection = GuiChoose.getOrderChoices("Choose Which Cards to Discard", "Discarded", -1, valid, null, null);
            for (final Object o : selection) {
                if (o != null && o instanceof Card) {
                    chosen.add((Card) o);
                }
            }
        } else {
            for (int i = 0; i < validamount; i++) {
                if (player.isHuman()) {
                    final Card o = GuiChoose.one("Choose card(s) to discard", valid);
                    if (o != null) {
                        chosen.add(o);
                        valid.remove(o);
                    } else {
                        break;
                    }
                } else { // Computer
                    chosen.add(valid.get(0));
                    valid.remove(valid.get(0));
                }
            }
        }
        return chosen;
    }
}
