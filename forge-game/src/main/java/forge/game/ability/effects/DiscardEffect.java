package forge.game.ability.effects;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import forge.card.CardType;
import forge.game.Game;
import forge.game.GameActionUtil;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
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
        final List<Player> tgtPlayers = getTargetPlayers(sa).filter(PlayerPredicates.canDiscardBy(sa, true));
        final StringBuilder sb = new StringBuilder();

        if (!tgtPlayers.isEmpty()) {
            final String tgtPs = Lang.joinHomogenous(tgtPlayers);
            final String mode = sa.getParam("Mode");
            final boolean revealYouChoose = mode.equals("RevealYouChoose");
            final boolean revealDiscardAll = mode.equals("RevealDiscardAll");
            final Player you = sa.getActivatingPlayer();
            final boolean oneTgtP = tgtPlayers.size() == 1;

            sb.append(tgtPs).append(" ");

            if (revealYouChoose) {
                sb.append(oneTgtP ? "reveals their hand. " : "reveal their hands. ");
                sb.append(you).append(" chooses ");
            } else if (revealDiscardAll) {
                sb.append(oneTgtP ? "reveals their hand. " : "reveal their hands. ");
                sb.append("They discard ");
            } else {
                sb.append(oneTgtP ? "discards " : "discard ");
            }

            int numCards = sa.hasParam("NumCards") ?
                    AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("NumCards"), sa) : 1;
            final boolean oneCard = numCards == 1 && oneTgtP;

            String valid = oneCard ? "card" : "cards";
            if (sa.hasParam("DiscardValid")) {
                String validD = sa.hasParam("DiscardValidDesc") ? sa.getParam("DiscardValidDesc")
                        : sa.getParam("DiscardValid");
                if (validD.equals("Card.nonLand")) {
                    validD = "nonland";
                } else if (CardType.CoreType.isValidEnum(validD)) {
                    validD = validD.toLowerCase();
                }
                valid = validD.contains(" card") ?
                        (oneCard ? validD : validD.replace(" card", " cards")) : validD + " " + valid;
            }

            if (mode.equals("Hand")) {
                sb.append(oneTgtP ? "their hand" : "their hands");
            } else if (revealDiscardAll) {
                sb.append("all");
            } else if (sa.hasParam("AnyNumber")) {
                sb.append("any number");
            } else if (sa.hasParam("NumCards") && sa.getParam("NumCards").equals("X")
                    && sa.getSVar("X").equals("Remembered$Amount")) {
                sb.append("that many");
            } else {
                sb.append(Lang.nounWithNumeralExceptOne(numCards, valid));
            }

            if (revealYouChoose) {
                sb.append(valid.contains(" from ") ? ". " : (oneTgtP ? " from it. " : " from them. ")).append(tgtPs);
                sb.append(oneTgtP ? " discards " : " discard ");
                sb.append(oneCard ? "that card" : "those cards");
            } else if (revealDiscardAll) {
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
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card source = sa.getHostCard();
        final String mode = sa.getParam("Mode");
        final Game game = source.getGame();

        final List<Player> targets = getTargetPlayers(sa),
                discarders;
        Player firstTarget = null;
        if (mode.equals("RevealTgtChoose")) {
            // In this case the target need not be the discarding player
            discarders = getDefinedPlayersOrTargeted(sa);
            firstTarget = Iterables.getFirst(targets, null);
        } else {
            discarders = targets;
        }

        final CardZoneTable table = new CardZoneTable();
        Map<Player, CardCollectionView> discardedMap = Maps.newHashMap();
        for (final Player p : discarders) {
            if (!p.isInGame()) {
                continue;
            }

            CardCollectionView toBeDiscarded = new CardCollection();
            if ((mode.equals("RevealTgtChoose") && firstTarget != null) || !sa.usesTargeting() || p.canBeTargetedBy(sa)) {
                final int numCardsInHand = p.getCardsIn(ZoneType.Hand).size();
                if (mode.equals("Defined")) {
                    if (!p.canDiscardBy(sa, true)) {
                        continue;
                    }

                    boolean runDiscard = !sa.hasParam("Optional")
                            || p.getController().confirmAction(sa, PlayerActionConfirmMode.Random, sa.getParam("DiscardMessage"), null);
                    if (runDiscard) {
                        toBeDiscarded = AbilityUtils.getDefinedCards(source, sa.getParam("DefinedCards"), sa);
                        toBeDiscarded = GameActionUtil.orderCardsByTheirOwners(game, toBeDiscarded, ZoneType.Graveyard, sa);
                    }
                }

                if (mode.equals("Hand")) {
                    toBeDiscarded = p.getCardsIn(ZoneType.Hand);

                    // Empty hand can still be discarded
                    if (!toBeDiscarded.isEmpty() && !p.canDiscardBy(sa, true)) {
                        continue;
                    }

                    toBeDiscarded = GameActionUtil.orderCardsByTheirOwners(game, toBeDiscarded, ZoneType.Graveyard, sa);
                }

                if (mode.equals("NotRemembered")) {
                    if (!p.canDiscardBy(sa, true)) {
                        continue;
                    }
                    toBeDiscarded = CardLists.getValidCards(p.getCardsIn(ZoneType.Hand), "Card.IsNotRemembered", p, source, sa);
                    toBeDiscarded = GameActionUtil.orderCardsByTheirOwners(game, toBeDiscarded, ZoneType.Graveyard, sa);
                }

                int numCards = 1;
                if (sa.hasParam("NumCards")) {
                    numCards = AbilityUtils.calculateAmount(source, sa.getParam("NumCards"), sa);
                    numCards = Math.min(numCards, numCardsInHand);
                }

                if (mode.equals("Random")) {
                    if (!p.canDiscardBy(sa, true)) {
                        continue;
                    }
                    String message = Localizer.getInstance().getMessage("lblWouldYouLikeRandomDiscardTargetCard", String.valueOf(numCards));
                    boolean runDiscard = !sa.hasParam("Optional") || p.getController().confirmAction(sa, PlayerActionConfirmMode.Random, message, null);

                    if (runDiscard) {
                        final String valid = sa.getParamOrDefault("DiscardValid", "Card");
                        List<Card> list = CardLists.getValidCards(p.getCardsIn(ZoneType.Hand), valid, source.getController(), source, sa);

                        toBeDiscarded = new CardCollection(Aggregates.random(list, numCards));
                        toBeDiscarded = GameActionUtil.orderCardsByTheirOwners(game, toBeDiscarded, ZoneType.Graveyard, sa);
                    }
                }
                else if (mode.equals("TgtChoose") && sa.hasParam("UnlessType")) {
                    if (!p.canDiscardBy(sa, true)) {
                        continue;
                    }
                    if (numCardsInHand > 0) {
                        CardCollectionView hand = p.getCardsIn(ZoneType.Hand);
                        toBeDiscarded = p.getController().chooseCardsToDiscardUnlessType(Math.min(numCards, numCardsInHand), hand, sa.getParam("UnlessType"), sa);
                        toBeDiscarded = GameActionUtil.orderCardsByTheirOwners(game,toBeDiscarded, ZoneType.Graveyard, sa);
                    }
                }
                else if (mode.equals("RevealDiscardAll")) {
                    // Reveal
                    final CardCollectionView dPHand = p.getCardsIn(ZoneType.Hand);

                    for (final Player opp : p.getAllOtherPlayers()) {
                        opp.getController().reveal(dPHand, ZoneType.Hand, p, Localizer.getInstance().getMessage("lblReveal") + " ");
                    }

                    if (!p.canDiscardBy(sa, true)) {
                        continue;
                    }

                    String valid = sa.getParamOrDefault("DiscardValid", "Card");

                    if (valid.contains("X")) {
                        valid = TextUtil.fastReplace(valid,
                                "X", Integer.toString(AbilityUtils.calculateAmount(source, "X", sa)));
                    }

                    toBeDiscarded = CardLists.getValidCards(dPHand, valid, source.getController(), source, sa);
                    toBeDiscarded = GameActionUtil.orderCardsByTheirOwners(game, toBeDiscarded, ZoneType.Graveyard, sa);
                } else if (mode.endsWith("YouChoose") || mode.endsWith("TgtChoose")) {
                    CardCollectionView dPHand = p.getCardsIn(ZoneType.Hand);
                    if (dPHand.isEmpty())
                        continue; // for loop over players

                    if (sa.hasParam("RevealNumber")) {
                        int amount = AbilityUtils.calculateAmount(source, sa.getParam("RevealNumber"), sa);
                        dPHand = p.getController().chooseCardsToRevealFromHand(amount, amount, dPHand);
                    }

                    Player chooser = p;
                    if (mode.endsWith("YouChoose")) {
                        chooser = source.getController();
                    } else if (mode.equals("RevealTgtChoose")) {
                        chooser = firstTarget;
                    }

                    if (mode.startsWith("Reveal")) {
                        game.getAction().reveal(dPHand, p);
                    }
                    if (mode.startsWith("Look") && p != chooser) {
                        game.getAction().revealTo(dPHand, chooser);
                    }

                    if (!p.canDiscardBy(sa, true)) {
                        continue;
                    }

                    final String valid = sa.getParamOrDefault("DiscardValid", "Card");
                    CardCollection validCards = CardLists.getValidCards(dPHand, valid, source.getController(), source, sa);

                    int min = sa.hasParam("AnyNumber") || sa.hasParam("Optional") ? 0 : Math.min(validCards.size(), numCards);
                    int max = sa.hasParam("AnyNumber") ? validCards.size() : Math.min(validCards.size(), numCards);

                    toBeDiscarded = max == 0 ? CardCollection.EMPTY : chooser.getController().chooseCardsToDiscardFrom(p, sa, validCards, min, max);

                    toBeDiscarded = GameActionUtil.orderCardsByTheirOwners(game, toBeDiscarded, ZoneType.Graveyard, sa);

                    if (mode.startsWith("Reveal") && p != chooser) {
                        p.getController().reveal(toBeDiscarded, ZoneType.Hand, p, Localizer.getInstance().getMessage("lblPlayerHasChosenCardsFrom", chooser.getName()));
                    }
                }
            }
            discardedMap.put(p, toBeDiscarded);
        }

        Map<AbilityKey, Object> params = AbilityKey.newMap();
        params.put(AbilityKey.LastStateBattlefield, sa.getLastStateBattlefield());
        params.put(AbilityKey.LastStateGraveyard, sa.getLastStateGraveyard());

        discard(sa, table, true, discardedMap, params);

        // run trigger if something got milled
        table.triggerChangesZoneAll(game, sa);
    }
}
