package forge.game.ability.effects;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardFactoryUtil;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates.Presets;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class DiscardEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final String mode = sa.getParam("Mode");
        final StringBuilder sb = new StringBuilder();

        final List<Player> tgtPlayers = getTargetPlayers(sa);

        if (!tgtPlayers.isEmpty()) {

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
                numCards = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("NumCards"), sa);
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
        final Card source = sa.getHostCard();
        final String mode = sa.getParam("Mode");
        //final boolean anyNumber = sa.hasParam("AnyNumber");

        final TargetRestrictions tgt = sa.getTargetRestrictions();

        final List<Card> discarded = new ArrayList<Card>();
        final List<Player> targets = getTargetPlayers(sa),
                discarders;
        Player firstTarget = null;
        if (mode.equals("RevealTgtChoose")) {
            // In this case the target need not be the discarding player
            discarders = getDefinedPlayersOrTargeted(sa);
            firstTarget = Iterables.getFirst(targets, null);
            if (tgt != null && !firstTarget.canBeTargetedBy(sa)) {
            	firstTarget = null;
            }
        } else {
            discarders = targets;
        }


        for (final Player p : discarders) {
            if ((mode.equals("RevealTgtChoose") && firstTarget != null) || tgt == null || p.canBeTargetedBy(sa)) {
            	if (sa.hasParam("RememberDiscarder")) {
            		source.addRemembered(p);
            	}
                final int numCardsInHand = p.getCardsIn(ZoneType.Hand).size(); 
                if (mode.equals("Defined")) {
                    boolean runDiscard = !sa.hasParam("Optional") 
                    		|| p.getController().confirmAction(sa, PlayerActionConfirmMode.Random, sa.getParam("DiscardMessage"));
                    if (runDiscard) {
                        final List<Card> toDiscard = AbilityUtils.getDefinedCards(source, sa.getParam("DefinedCards"), sa);
                        for (final Card c : toDiscard) {
                            boolean hasDiscarded = p.discard(c, sa) != null;
                            if (hasDiscarded) {
                                discarded.add(c);
                            }
                        }

                        if (sa.hasParam("RememberDiscarded")) {
                            for (final Card c : discarded) {
                                source.addRemembered(c);
                            }
                        }
                    }
                    continue;
                }

                if (mode.equals("Hand")) {
                    boolean shouldRemember = sa.hasParam("RememberDiscarded");
                    for(Card c : Lists.newArrayList(p.getCardsIn(ZoneType.Hand))) { // without copying will get concurrent modification exception
                        boolean hasDiscarded = p.discard(c, sa) != null;
                        if( hasDiscarded && shouldRemember )
                            source.addRemembered(c);
                    }
                    continue;
                }

                if (mode.equals("NotRemembered")) {
                    final List<Card> dPHand = CardLists.getValidCards(p.getCardsIn(ZoneType.Hand), "Card.IsNotRemembered", p, source);
                    for (final Card c : dPHand) {
                        p.discard(c, sa);
                        discarded.add(c);
                    }
                }

                int numCards = 1;
                if (sa.hasParam("NumCards")) {
                    numCards = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("NumCards"), sa);
                    if (!p.getCardsIn(ZoneType.Hand).isEmpty() && p.getCardsIn(ZoneType.Hand).size() < numCards) {
                        // System.out.println("Scale down discard from " + numCards + " to " + p.getCardsIn(ZoneType.Hand).size());
                        numCards = p.getCardsIn(ZoneType.Hand).size();
                    }
                }

                if (mode.equals("Random")) {
                    String message = "Would you like to discard " + numCards + " random card(s)?";
                    boolean runDiscard = !sa.hasParam("Optional") || p.getController().confirmAction(sa, PlayerActionConfirmMode.Random, message);

                    if (runDiscard) {
                        final String valid = sa.hasParam("DiscardValid") ? sa.getParam("DiscardValid") : "Card";
                        List<Card> list = CardLists.getValidCards(p.getCardsIn(ZoneType.Hand), valid, source.getController(), source);
                        list = CardLists.filter(list, Presets.NON_TOKEN);
                        for (int i = 0; i < numCards; i++) {
                            if (list.isEmpty())
                                break;
                                
                            final Card disc = Aggregates.random(list);
                            if (p.discard(disc, sa) != null) 
                                discarded.add(disc);
                            list.remove(disc);
                        }
                    }
                }
                else if (mode.equals("TgtChoose") && sa.hasParam("UnlessType")) {
                    if( numCardsInHand > 0 ) {
                        CardCollectionView hand = p.getCardsIn(ZoneType.Hand);
                        hand = CardLists.filter(hand, Presets.NON_TOKEN);
                        CardCollectionView toDiscard = p.getController().chooseCardsToDiscardUnlessType(Math.min(numCards, numCardsInHand), hand, sa.getParam("UnlessType"), sa);
                        for (Card c : toDiscard) {
                            c.getController().discard(c, sa);
                        }
                    }
                }
                else if (mode.equals("RevealDiscardAll")) {
                    // Reveal
                    final CardCollectionView dPHand = p.getCardsIn(ZoneType.Hand);

                    for (final Player opp : p.getOpponents()) {
                    	opp.getController().reveal(dPHand, ZoneType.Hand, p, "Reveal ");
                    }

                    String valid = sa.hasParam("DiscardValid") ? sa.getParam("DiscardValid") : "Card";

                    if (valid.contains("X")) {
                        valid = valid.replace("X", Integer.toString(AbilityUtils.calculateAmount(source, "X", sa)));
                    }

                    List<Card> dPChHand = CardLists.getValidCards(dPHand, valid.split(","), source.getController(), source, sa);
                    dPChHand = CardLists.filter(dPChHand, Presets.NON_TOKEN);
                    // Reveal cards that will be discarded?
                    for (final Card c : dPChHand) {
                        p.discard(c, sa);
                        discarded.add(c);
                    }
                } else if (mode.equals("RevealYouChoose") || mode.equals("RevealTgtChoose") || mode.equals("TgtChoose")) {
                    CardCollectionView dPHand = p.getCardsIn(ZoneType.Hand);
                    dPHand = CardLists.filter(dPHand, Presets.NON_TOKEN);
                    if (dPHand.isEmpty())
                        continue; // for loop over players

                    if (sa.hasParam("RevealNumber")) {
                        String amountString = sa.getParam("RevealNumber");
                        int amount = StringUtils.isNumeric(amountString) ? Integer.parseInt(amountString) : CardFactoryUtil.xCount(source, source.getSVar(amountString));
                        dPHand = p.getController().chooseCardsToRevealFromHand(amount, amount, dPHand);
                    }
                    final String valid = sa.hasParam("DiscardValid") ? sa.getParam("DiscardValid") : "Card";
                    String[] dValid = valid.split(",");
                    CardCollection validCards = CardLists.getValidCards(dPHand, dValid, source.getController(), source, sa);

                    Player chooser = p;
                    if (mode.equals("RevealYouChoose")) {
                        chooser = source.getController();
                    } else if (mode.equals("RevealTgtChoose")) {
                        chooser = firstTarget;
                    }

                    if (mode.startsWith("Reveal") && p != chooser)
                        chooser.getGame().getAction().reveal(dPHand, p);
                    
                    int min = sa.hasParam("AnyNumber") || sa.hasParam("Optional") ? 0 : Math.min(validCards.size(), numCards);
                    int max = sa.hasParam("AnyNumber") ? validCards.size() : Math.min(validCards.size(), numCards);

                    CardCollectionView toBeDiscarded = validCards.isEmpty() ? null : chooser.getController().chooseCardsToDiscardFrom(p, sa, validCards, min, max);

                    if (toBeDiscarded != null) {
                        if (mode.startsWith("Reveal") ) {
                            p.getController().reveal(toBeDiscarded, ZoneType.Hand, p,
                                    chooser + " has chosen " + (toBeDiscarded.size() == 1 ? "this card" : "these cards")  + " from ");
                        }
                        for (Card card : toBeDiscarded) {
                            if (card == null) { continue; }
                            p.discard(card, sa);
                            discarded.add(card);
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
}
