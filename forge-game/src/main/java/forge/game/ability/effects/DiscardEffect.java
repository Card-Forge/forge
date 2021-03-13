package forge.game.ability.effects;

import forge.game.Game;
import forge.game.GameActionUtil;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.*;
import forge.game.card.CardPredicates.Presets;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.player.PlayerPredicates;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;

import forge.util.Lang;
import forge.util.Aggregates;
import forge.util.TextUtil;
import forge.util.Localizer;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.Map;

public class DiscardEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final String mode = sa.getParam("Mode");
        final StringBuilder sb = new StringBuilder();

        final Iterable<Player> tgtPlayers = Iterables.filter(getTargetPlayers(sa), PlayerPredicates.canDiscardBy(sa));

        if (!Iterables.isEmpty(tgtPlayers)) {
            sb.append(Lang.joinHomogenous(tgtPlayers)).append(" ");

            if (mode.equals("RevealYouChoose")) {
                sb.append("reveals their hand.").append("  You choose (");
            } else if (mode.equals("RevealDiscardAll")) {
                sb.append("reveals their hand. Discard (");
            } else {
                sb.append("discards (");
            }

            int numCards = 1;
            if (sa.hasParam("NumCards")) {
                numCards = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("NumCards"), sa);
            }

            if (mode.equals("Hand")) {
                sb.append("their hand");
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

                if (sa.getHostCard() != null) {
                    final List<Card> toDiscard = AbilityUtils.getDefinedCards(sa.getHostCard(), sa.getParam("DefinedCards"), sa);
                    if (!toDiscard.isEmpty()) {
                        sb.append(": ");

                        List<String> definedNames = Lists.newArrayList();
                        for (Card discarded : toDiscard) {
                            definedNames.add(discarded.toString());
                        }

                        sb.append(TextUtil.join(definedNames, ","));
                    }
                }
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
        final Game game = source.getGame();
        //final boolean anyNumber = sa.hasParam("AnyNumber");

        final List<Card> discarded = Lists.newArrayList();
        final List<Player> targets = getTargetPlayers(sa),
                discarders;
        Player firstTarget = null;
        if (mode.equals("RevealTgtChoose")) {
            // In this case the target need not be the discarding player
            discarders = getDefinedPlayersOrTargeted(sa);
            firstTarget = Iterables.getFirst(targets, null);
            if (sa.usesTargeting() && !firstTarget.canBeTargetedBy(sa)) {
                firstTarget = null;
            }
        } else {
            discarders = targets;
        }


        final CardZoneTable table = new CardZoneTable();
        for (final Player p : discarders) {
            boolean firstDiscard = p.getNumDiscardedThisTurn() == 0;
            final CardCollection discardedByPlayer = new CardCollection();
            if ((mode.equals("RevealTgtChoose") && firstTarget != null) || !sa.usesTargeting() || p.canBeTargetedBy(sa)) {
                if (sa.hasParam("RememberDiscarder") && p.canDiscardBy(sa)) {
                    source.addRemembered(p);
                }
                final int numCardsInHand = p.getCardsIn(ZoneType.Hand).size();
                if (mode.equals("Defined")) {
                    if (!p.canDiscardBy(sa)) {
                        continue;
                    }

                    boolean runDiscard = !sa.hasParam("Optional")
                            || p.getController().confirmAction(sa, PlayerActionConfirmMode.Random, sa.getParam("DiscardMessage"));
                    if (runDiscard) {
                        CardCollectionView toDiscard = AbilityUtils.getDefinedCards(source, sa.getParam("DefinedCards"), sa);

                        if (toDiscard.size() > 1) {
                            toDiscard = GameActionUtil.orderCardsByTheirOwners(game, toDiscard, ZoneType.Graveyard, sa);
                        }

                        for (final Card c : toDiscard) {
                            if (p.discard(c, sa, table) != null) {
                                discarded.add(c);
                                discardedByPlayer.add(c);
                            }
                        }
                    }
                }

                if (mode.equals("Hand")) {
                    if (!p.canDiscardBy(sa)) {
                        continue;
                    }
                    CardCollectionView toDiscard = p.getCardsIn(ZoneType.Hand);

                    if (toDiscard.size() > 1) {
                        toDiscard = GameActionUtil.orderCardsByTheirOwners(game, toDiscard, ZoneType.Graveyard, sa);
                    }

                    for(Card c : Lists.newArrayList(toDiscard)) { // without copying will get concurrent modification exception
                        if (p.discard(c, sa, table) != null) {
                            discarded.add(c);
                            discardedByPlayer.add(c);
                        }
                    }
                }

                if (mode.equals("NotRemembered")) {
                    if (!p.canDiscardBy(sa)) {
                        continue;
                    }
                    CardCollectionView dPHand = CardLists.getValidCards(p.getCardsIn(ZoneType.Hand), "Card.IsNotRemembered", p, source);
                    if (dPHand.size() > 1) {
                        dPHand = GameActionUtil.orderCardsByTheirOwners(game, dPHand, ZoneType.Graveyard, sa);
                    }

                    for (final Card c : dPHand) {
                        if (p.discard(c, sa, table) != null) {
                            discarded.add(c);
                            discardedByPlayer.add(c);
                        }
                    }
                }

                int numCards = 1;
                if (sa.hasParam("NumCards")) {
                    numCards = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("NumCards"), sa);
                    numCards = Math.min(numCards, numCardsInHand);
                }

                if (mode.equals("Random")) {
                    if (!p.canDiscardBy(sa)) {
                        continue;
                    }
                    String message = Localizer.getInstance().getMessage("lblWouldYouLikeRandomDiscardTargetCard", String.valueOf(numCards));
                    boolean runDiscard = !sa.hasParam("Optional") || p.getController().confirmAction(sa, PlayerActionConfirmMode.Random, message);

                    if (runDiscard) {
                        final String valid = sa.hasParam("DiscardValid") ? sa.getParam("DiscardValid") : "Card";
                        List<Card> list = CardLists.getValidCards(p.getCardsIn(ZoneType.Hand), valid, source.getController(), source);
                        list = CardLists.filter(list, Presets.NON_TOKEN);
                        CardCollection toDiscard = new CardCollection();
                        for (int i = 0; i < numCards; i++) {
                            if (list.isEmpty())
                                break;

                            final Card disc = Aggregates.random(list);
                            toDiscard.add(disc);
                            list.remove(disc);
                        }

                        CardCollectionView toDiscardView = toDiscard;
                        if (toDiscard.size() > 1) {
                            toDiscardView = GameActionUtil.orderCardsByTheirOwners(game, toDiscard, ZoneType.Graveyard, sa);
                        }

                        for (Card c : toDiscardView) {
                            if (p.discard(c, sa, table) != null) {
                                discarded.add(c);
                                discardedByPlayer.add(c);
                            }
                        }
                    }
                }
                else if (mode.equals("TgtChoose") && sa.hasParam("UnlessType")) {
                    if (!p.canDiscardBy(sa)) {
                        continue;
                    }
                    if( numCardsInHand > 0 ) {
                        CardCollectionView hand = p.getCardsIn(ZoneType.Hand);
                        hand = CardLists.filter(hand, Presets.NON_TOKEN);
                        CardCollectionView toDiscard = p.getController().chooseCardsToDiscardUnlessType(Math.min(numCards, numCardsInHand), hand, sa.getParam("UnlessType"), sa);

                        if (toDiscard.size() > 1) {
                            toDiscard = GameActionUtil.orderCardsByTheirOwners(game, toDiscard, ZoneType.Graveyard, sa);
                        }

                        for (Card c : toDiscard) {
                            if (c.getController().discard(c, sa, table) != null) {
                                discarded.add(c);
                                discardedByPlayer.add(c);
                            }
                        }
                    }
                }
                else if (mode.equals("RevealDiscardAll")) {
                    // Reveal
                    final CardCollectionView dPHand = p.getCardsIn(ZoneType.Hand);

                    for (final Player opp : p.getAllOtherPlayers()) {
                        opp.getController().reveal(dPHand, ZoneType.Hand, p, Localizer.getInstance().getMessage("lblReveal") + " ");
                    }

                    if (!p.canDiscardBy(sa)) {
                        continue;
                    }

                    String valid = sa.hasParam("DiscardValid") ? sa.getParam("DiscardValid") : "Card";

                    if (valid.contains("X")) {
                        valid = TextUtil.fastReplace(valid,
                                "X", Integer.toString(AbilityUtils.calculateAmount(source, "X", sa)));
                    }

                    CardCollectionView dPChHand = CardLists.getValidCards(dPHand, valid.split(","), source.getController(), source, sa);
                    dPChHand = CardLists.filter(dPChHand, Presets.NON_TOKEN);
                    if (dPChHand.size() > 1) {
                        dPChHand = GameActionUtil.orderCardsByTheirOwners(game, dPChHand, ZoneType.Graveyard, sa);
                    }

                    // Reveal cards that will be discarded?
                    for (final Card c : dPChHand) {
                        if (p.discard(c, sa, table) != null) {
                            discarded.add(c);
                            discardedByPlayer.add(c);
                        }
                    }
                } else if (mode.equals("RevealYouChoose") || mode.equals("RevealTgtChoose") || mode.equals("TgtChoose")) {
                    CardCollectionView dPHand = p.getCardsIn(ZoneType.Hand);
                    dPHand = CardLists.filter(dPHand, Presets.NON_TOKEN);
                    if (dPHand.isEmpty())
                        continue; // for loop over players

                    if (sa.hasParam("RevealNumber")) {
                        int amount = AbilityUtils.calculateAmount(source, sa.getParam("RevealNumber"), sa);
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

                    if (mode.startsWith("Reveal") && p != chooser) {
                        game.getAction().reveal(dPHand, p);
                    }

                    if (!p.canDiscardBy(sa)) {
                        continue;
                    }

                    int min = sa.hasParam("AnyNumber") || sa.hasParam("Optional") ? 0 : Math.min(validCards.size(), numCards);
                    int max = sa.hasParam("AnyNumber") ? validCards.size() : Math.min(validCards.size(), numCards);

                    CardCollectionView toBeDiscarded = validCards.isEmpty() ? null : chooser.getController().chooseCardsToDiscardFrom(p, sa, validCards, min, max);

                    if (toBeDiscarded != null) {
                        if (toBeDiscarded.size() > 1) {
                            toBeDiscarded = GameActionUtil.orderCardsByTheirOwners(game, toBeDiscarded, ZoneType.Graveyard, sa);
                        }

                        if (mode.startsWith("Reveal") ) {
                            p.getController().reveal(toBeDiscarded, ZoneType.Hand, p, Localizer.getInstance().getMessage("lblPlayerHasChosenCardsFrom", chooser.getName()));
                        }
                        for (Card card : toBeDiscarded) {
                            if (card == null) { continue; }
                            if (p.discard(card, sa, table) != null) {
                                discarded.add(card);
                                discardedByPlayer.add(card);
                            }
                        }
                    }
                }
            }

            if (!discardedByPlayer.isEmpty()) {
                final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
                runParams.put(AbilityKey.Player, p);
                runParams.put(AbilityKey.Cards, discardedByPlayer);
                runParams.put(AbilityKey.Cause, sa);
                runParams.put(AbilityKey.FirstTime, firstDiscard);
                game.getTriggerHandler().runTrigger(TriggerType.DiscardedAll, runParams, false);
            }
        }

        if (sa.hasParam("RememberDiscarded")) {
            for (final Card c : discarded) {
                source.addRemembered(c);
            }
        }

        // run trigger if something got milled
        table.triggerChangesZoneAll(source.getGame());
    } // discardResolve()
}
