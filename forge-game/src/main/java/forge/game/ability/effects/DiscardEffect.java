package forge.game.ability.effects;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import forge.game.Game;
import forge.game.GameActionUtil;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates.Presets;
import forge.game.card.CardZoneTable;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.player.PlayerPredicates;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;
import forge.util.Lang;
import forge.util.Localizer;
import forge.util.TextUtil;

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
                sb.append("discards ");
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
                sb.append(numCards == 1 ? "a card" : (Lang.getNumeral(numCards) + " cards"));
            }

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
        Map<Player, CardCollectionView> discardedMap = Maps.newHashMap();
        for (final Player p : discarders) {
            CardCollectionView toBeDiscarded = new CardCollection();
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
                        toBeDiscarded = AbilityUtils.getDefinedCards(source, sa.getParam("DefinedCards"), sa);

                        if (toBeDiscarded.size() > 1) {
                            toBeDiscarded = GameActionUtil.orderCardsByTheirOwners(game, toBeDiscarded, ZoneType.Graveyard, sa);
                        }
                    }
                }

                if (mode.equals("Hand")) {
                    if (!p.canDiscardBy(sa)) {
                        continue;
                    }
                    toBeDiscarded = p.getCardsIn(ZoneType.Hand);

                    if (toBeDiscarded.size() > 1) {
                        toBeDiscarded = GameActionUtil.orderCardsByTheirOwners(game, toBeDiscarded, ZoneType.Graveyard, sa);
                    }
                }

                if (mode.equals("NotRemembered")) {
                    if (!p.canDiscardBy(sa)) {
                        continue;
                    }
                    toBeDiscarded = CardLists.getValidCards(p.getCardsIn(ZoneType.Hand), "Card.IsNotRemembered", p, source, sa);
                    if (toBeDiscarded.size() > 1) {
                        toBeDiscarded = GameActionUtil.orderCardsByTheirOwners(game, toBeDiscarded, ZoneType.Graveyard, sa);
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
                        List<Card> list = CardLists.getValidCards(p.getCardsIn(ZoneType.Hand), valid, source.getController(), source, sa);
                        list = CardLists.filter(list, Presets.NON_TOKEN);
                        CardCollection toDiscard = new CardCollection();
                        for (int i = 0; i < numCards; i++) {
                            if (list.isEmpty())
                                break;

                            final Card disc = Aggregates.random(list);
                            toDiscard.add(disc);
                            list.remove(disc);
                        }

                        toBeDiscarded = toDiscard;
                        if (toBeDiscarded.size() > 1) {
                            toBeDiscarded = GameActionUtil.orderCardsByTheirOwners(game, toBeDiscarded, ZoneType.Graveyard, sa);
                        }
                    }
                }
                else if (mode.equals("TgtChoose") && sa.hasParam("UnlessType")) {
                    if (!p.canDiscardBy(sa)) {
                        continue;
                    }
                    if (numCardsInHand > 0) {
                        CardCollectionView hand = p.getCardsIn(ZoneType.Hand);
                        hand = CardLists.filter(hand, Presets.NON_TOKEN);
                        toBeDiscarded = p.getController().chooseCardsToDiscardUnlessType(Math.min(numCards, numCardsInHand), hand, sa.getParam("UnlessType"), sa);

                        if (toBeDiscarded.size() > 1) {
                            toBeDiscarded = GameActionUtil.orderCardsByTheirOwners(game,toBeDiscarded, ZoneType.Graveyard, sa);
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

                    toBeDiscarded = CardLists.getValidCards(dPHand, valid.split(","), source.getController(), source, sa);
                    toBeDiscarded = CardLists.filter(toBeDiscarded, Presets.NON_TOKEN);
                    if (toBeDiscarded.size() > 1) {
                        toBeDiscarded = GameActionUtil.orderCardsByTheirOwners(game, toBeDiscarded, ZoneType.Graveyard, sa);
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

                    toBeDiscarded = validCards.isEmpty() ? CardCollection.EMPTY : chooser.getController().chooseCardsToDiscardFrom(p, sa, validCards, min, max);

                    if (toBeDiscarded.size() > 1) {
                        toBeDiscarded = GameActionUtil.orderCardsByTheirOwners(game, toBeDiscarded, ZoneType.Graveyard, sa);
                    }

                    if (mode.startsWith("Reveal") ) {
                        p.getController().reveal(toBeDiscarded, ZoneType.Hand, p, Localizer.getInstance().getMessage("lblPlayerHasChosenCardsFrom", chooser.getName()));
                    }
                }
            }
            discardedMap.put(p, toBeDiscarded);
        }

        discard(sa, table, discardedMap);

        // run trigger if something got milled
        table.triggerChangesZoneAll(game, sa);
    } // discardResolve()
}
