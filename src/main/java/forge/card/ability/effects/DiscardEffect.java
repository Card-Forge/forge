package forge.card.ability.effects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;

import com.google.common.base.Predicate;

import forge.Card;
import forge.CardLists;
import forge.card.ability.AbilityUtils;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.AIPlayer;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;
import forge.gui.GuiDialog;
import forge.util.Aggregates;

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
                numCards = AbilityUtils.calculateAmount(sa.getSourceCard(), sa.getParam("NumCards"), sa);
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

    private List<Card> discardComputerChooses(SpellAbility sa, Player victim, Player chooser, int numCards, String[] dValid, boolean isReveal) {
        final Card source = sa.getSourceCard();
        List<Card> dPChHand = new ArrayList<Card>(victim.getCardsIn(ZoneType.Hand));
        dPChHand = CardLists.getValidCards(dPChHand, dValid, source.getController(), source);
        final List<Card> toDiscard = new ArrayList<Card>();

        int max = Math.min(dPChHand.size(), numCards);
        List<Card> list = new ArrayList<Card>();
        
        if (!victim.isOpponentOf(chooser) && victim instanceof AIPlayer) { // discard AI cards
            list = ((AIPlayer) victim).getAi().getCardsToDiscard(max, dValid, sa);
        } else {
            // discard hostile or human opponent
            for (int i = 0; i < max; i++) {
                Card dC = chooseCardToDiscardFromOpponent(sa, dPChHand);
                dPChHand.remove(dC);
                list.add(dC);
            }
        }
        
        if (isReveal) {
            GuiChoose.oneOrNone("Computer has chosen", list);
        }
        
        return toDiscard;

    }

    /**
     * TODO: Write javadoc for this method.
     * @param sa
     * @param opponentHand
     * @param list
     */
    private Card chooseCardToDiscardFromOpponent(SpellAbility sa, List<Card> opponentHand) {
        List<Card> goodChoices = CardLists.filter(opponentHand, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                if (!c.getSVar("DiscardMeByOpp").equals("") || !c.getSVar("DiscardMe").equals("")) {
                    return false;
                }
                return true;
            }
        });
        if (goodChoices.isEmpty()) {
            goodChoices = opponentHand;
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

        return Aggregates.random(goodChoices);
    }

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
                    final List<Card> toDiscard = AbilityUtils.getDefinedCards(source, sa.getParam("DefinedCards"), sa);
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
                    numCards = AbilityUtils.calculateAmount(sa.getSourceCard(), sa.getParam("NumCards"), sa);
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
                           runDiscard = GuiDialog.confirm(source, sb.toString());
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

                    String valid = sa.hasParam("DiscardValid") ? sa.getParam("DiscardValid") : "Card";

                    if (valid.contains("X")) {
                        valid = valid.replace("X", Integer.toString(AbilityUtils.calculateAmount(source, "X", sa)));
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
                        final String valid = sa.hasParam("DiscardValid") ? sa.getParam("DiscardValid") : "Card";
                        String[] dValid = ArrayUtils.EMPTY_STRING_ARRAY;
                        dValid = valid.split(",");
                        dPChHand = CardLists.getValidCards(dPHand, dValid, source.getController(), source);

                        Player chooser = p;
                        if (mode.equals("RevealYouChoose")) {
                            chooser = source.getController();
                        } else if (mode.equals("RevealOppChoose")) {
                            chooser = source.getController().getOpponent();
                        }

                        List<Card> toBeDiscarded = new ArrayList<Card>(); 
                        if (chooser.isComputer()) {
                            toBeDiscarded = discardComputerChooses(sa, p, chooser, numCards, dValid, mode.startsWith("Reveal"));
                        } else {
                            // human
                            if (mode.startsWith("Reveal")) {
                                GuiChoose.oneOrNone("Revealed " + p + "  hand", dPHand);
                            }

                            if (sa.hasParam("AnyNumber")) {
                                List<Card> chosen = getDiscardedList(p, dPChHand, dPChHand.size(), true);

                                for (Card c : chosen) {
                                    dPChHand.remove(chosen);
                                    toBeDiscarded.add(c);
                                }
                            } else 
                                for (int i = 0; i < numCards; i++) {
                                if (dPChHand.isEmpty()) {
                                    break;
                                }
                                Card dC = null;
                                if (sa.hasParam("Optional")) {
                                    dC = GuiChoose.oneOrNone("Choose a card to be discarded", dPChHand);
                                } else {
                                    dC = GuiChoose.one("Choose a card to be discarded", dPChHand);
                                } 
                                
                                if (dC != null) {
                                    dPChHand.remove(dC);
                                    toBeDiscarded.add(dC);
                                }
                                else break;
                            }
                        }

                        if (toBeDiscarded != null) {
                            for (Card card : toBeDiscarded) {
                                if ( null == card ) continue;
                                p.discard(card, sa);
                                discarded.add(card);
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
            final List<Card> selection = GuiChoose.order("Choose Which Cards to Discard", "Discarded", -1, valid, null, null);
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
