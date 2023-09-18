package forge.game.ability.effects;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Iterables;

import forge.game.Game;
import forge.game.GameActionUtil;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardFactoryUtil;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CardUtil;
import forge.game.card.CardZoneTable;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.util.Lang;
import forge.util.Localizer;
import forge.util.TextUtil;

public class ChangeZoneAllEffect extends SpellAbilityEffect {
    @Override
    protected String getStackDescription(SpellAbility sa) {
        // TODO build Stack Description will need expansion as more cards are added

        final String[] desc = sa.getDescription().split(":");

        if (desc.length > 1) {
            return desc[1];
        } else {
            return desc[0];
        }
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card source = sa.getHostCard();

        //if host is not on the battlefield don't apply
        if ("UntilHostLeavesPlay".equals(sa.getParam("Duration")) && !source.isInPlay()) {
            return;
        }

        final ZoneType destination = ZoneType.smartValueOf(sa.getParam("Destination"));
        final List<ZoneType> origin = ZoneType.listValueOf(sa.getParam("Origin"));

        CardCollection cards;
        List<Player> tgtPlayers = getTargetPlayers(sa);
        final Game game = sa.getActivatingPlayer().getGame();
        CardCollectionView lastStateBattlefield = game.copyLastStateBattlefield();
        CardCollectionView lastStateGraveyard = game.copyLastStateGraveyard();

        if ((!sa.usesTargeting() && !sa.hasParam("Defined")) || sa.hasParam("UseAllOriginZones")) {
            cards = new CardCollection(game.getCardsIn(origin));
        } else {
            cards = new CardCollection();
            for (final Player p : tgtPlayers) {
                cards.addAll(p.getCardsIn(origin));

                if (origin.contains(ZoneType.Library) && sa.hasParam("Search") && !sa.getActivatingPlayer().canSearchLibraryWith(sa, p)) {
                    cards.removeAll(p.getCardsIn(ZoneType.Library));
                }
            }
            if (origin.contains(ZoneType.Library) && sa.hasParam("Search")) {
                // Search library using changezoneall effect need a param "Search"
                if (sa.getActivatingPlayer().hasKeyword("LimitSearchLibrary")) {
                    for (final Player p : tgtPlayers) {
                        cards.removeAll(p.getCardsIn(ZoneType.Library));
                        int fetchNum = Math.min(p.getCardsIn(ZoneType.Library).size(), 4);
                        cards.addAll(p.getCardsIn(ZoneType.Library, fetchNum));
                    }
                }
                if (!sa.getActivatingPlayer().canSearchLibraryWith(sa, null)) {
                    // all these cards have "then that player shuffles", mandatory shuffle
                    cards.removeAll(game.getCardsIn(ZoneType.Library));
                }
            }
        }

        if (origin.contains(ZoneType.Library) && sa.hasParam("Search") && sa.getActivatingPlayer().canSearchLibraryWith(sa, null)) {
            CardCollection libCards = CardLists.getValidCards(cards, "Card.inZoneLibrary", sa.getActivatingPlayer(), source, sa);
            CardCollection libCardsYouOwn = CardLists.filterControlledBy(libCards, sa.getActivatingPlayer());
            if (!libCardsYouOwn.isEmpty()) { // Only searching one's own library would fire Archive Trap's altcost
                sa.getActivatingPlayer().incLibrarySearched();
            }
            if (!libCards.isEmpty()) {
                sa.getActivatingPlayer().getController().reveal(libCards, ZoneType.Library, libCards.get(0).getOwner());
            }
            final Map<AbilityKey, Object> runParams = AbilityKey.mapFromPlayer(sa.getActivatingPlayer());
            runParams.put(AbilityKey.Target, tgtPlayers);
            game.getTriggerHandler().runTrigger(TriggerType.SearchedLibrary, runParams, false);
        }
        if (origin.contains(ZoneType.Hand) && sa.hasParam("Search")) {
            CardCollection handCards = CardLists.filterControlledBy(CardLists.getValidCards(cards, "Card.inZoneHand", sa.getActivatingPlayer(), source, sa),
                sa.getActivatingPlayer().getOpponents());
            if (!handCards.isEmpty()) {
                sa.getActivatingPlayer().getController().reveal(handCards, ZoneType.Hand, handCards.get(0).getOwner());
            }
        }

        if (sa.hasParam("Optional")) {
            final String targets = Lang.joinHomogenous(cards);
            final String message;
            if (sa.hasParam("OptionQuestion")) {
                message = TextUtil.fastReplace(sa.getParam("OptionQuestion"), "TARGETS", targets);
            } else {
                message = Localizer.getInstance().getMessage("lblMoveTargetFromOriginToDestination", targets, Lang.joinHomogenous(origin, ZoneType.Accessors.GET_TRANSLATED_NAME), destination.getTranslatedName());
            }

            if (!sa.getActivatingPlayer().getController().confirmAction(sa, null, message, null)) {
                return;
            }
        }

        cards = (CardCollection)AbilityUtils.filterListByType(cards, sa.getParam("ChangeType"), sa);

        if (sa.hasParam("TypeLimit")) {
            cards = new CardCollection(Iterables.limit(cards, AbilityUtils.calculateAmount(source, sa.getParam("TypeLimit"), sa)));
        }

        if (sa.hasParam("ForgetOtherRemembered")) {
            source.clearRemembered();
        }

        final String remember = sa.getParam("RememberChanged");
        final String forget = sa.getParam("ForgetChanged");
        final String imprint = sa.getParam("Imprint");
        final boolean random = sa.hasParam("RandomOrder");
        final boolean remLKI = sa.hasParam("RememberLKI");

        final int libraryPos = sa.hasParam("LibraryPosition") ? Integer.parseInt(sa.getParam("LibraryPosition")) : 0;

        if (!random && !((destination == ZoneType.Library || destination == ZoneType.PlanarDeck) && sa.hasParam("Shuffle"))) {
            if ((destination == ZoneType.Library || destination == ZoneType.PlanarDeck) && cards.size() >= 2) {
                Player p = AbilityUtils.getDefinedPlayers(source, sa.getParam("DefinedPlayer"), sa).get(0);
                cards = (CardCollection) p.getController().orderMoveToZoneList(cards, destination, sa);
                //the last card in this list will be the closest to the top, but we want the first card to be closest.
                //so reverse it here before moving them to the library.
                java.util.Collections.reverse(cards);
            } else {
                cards = (CardCollection) GameActionUtil.orderCardsByTheirOwners(game, cards, destination, sa);
            }
        }

        if (destination.equals(ZoneType.Library) && random) {
            CardLists.shuffle(cards);
        }

        final CardZoneTable triggerList = new CardZoneTable();
        for (final Card c : cards) {
            final Zone originZone = game.getZoneOf(c);

            // Fizzle spells so that they are removed from stack (e.g. Summary Dismissal)
            if (sa.hasParam("Fizzle")) {
                if (originZone.is(ZoneType.Exile) || originZone.is(ZoneType.Hand) || originZone.is(ZoneType.Stack)) {
                    game.getStack().remove(c);
                }
            }

            if (remLKI) {
                source.addRemembered(CardUtil.getLKICopy(c));
            }

            Map<AbilityKey, Object> moveParams = AbilityKey.newMap();
            moveParams.put(AbilityKey.LastStateBattlefield, lastStateBattlefield);
            moveParams.put(AbilityKey.LastStateGraveyard, lastStateGraveyard);

            if (destination == ZoneType.Battlefield) {
                moveParams.put(AbilityKey.SimultaneousETB, cards);
                if (sa.hasAdditionalAbility("AnimateSubAbility")) {
                    // need LKI before Animate does apply
                    moveParams.put(AbilityKey.CardLKI, CardUtil.getLKICopy(c));

                    final SpellAbility animate = sa.getAdditionalAbility("AnimateSubAbility");
                    source.addRemembered(c);
                    AbilityUtils.resolve(animate);
                    source.removeRemembered(c);
                    animate.setSVar("unanimateTimestamp", String.valueOf(game.getTimestamp()));
                }
                if (sa.hasParam("Tapped")) {
                    c.setTapped(true);
                }
                if (sa.hasParam("FaceDown")) {
                    c.turnFaceDown(true);
                    CardFactoryUtil.setFaceDownState(c, sa);
                }
            }
            Card movedCard = null;
            if (sa.hasParam("GainControl")) {
                c.setController(sa.getActivatingPlayer(), game.getNextTimestamp());
                movedCard = game.getAction().moveToPlay(c, sa.getActivatingPlayer(), sa, moveParams);
            } else {
                movedCard = game.getAction().moveTo(destination, c, libraryPos, sa, moveParams);
                if (destination == ZoneType.Exile) {
                    handleExiledWith(movedCard, sa);
                }
                if (sa.hasParam("ExileFaceDown")) {
                    movedCard.turnFaceDown(true);
                }
            }

            if (!movedCard.getZone().equals(originZone)) {
                if (remember != null) {
                    final Card newSource = game.getCardState(source);
                    newSource.addRemembered(movedCard);
                    if (!source.isRemembered(movedCard)) {
                        source.addRemembered(movedCard);
                    }
                    if (c.getMeldedWith() != null) {
                        Card meld = game.getCardState(c.getMeldedWith(), null);
                        if (meld != null) {
                            newSource.addRemembered(meld);
                            if (!source.isRemembered(meld)) {
                                source.addRemembered(meld);
                            }
                        }
                    }
                    if (c.hasMergedCard()) {
                        for (final Card card : c.getMergedCards()) {
                            if (card == c) continue;
                            newSource.addRemembered(card);
                            if (!source.isRemembered(card)) {
                                source.addRemembered(card);
                            }
                        }
                    }
                }
                if (forget != null) {
                    game.getCardState(source).removeRemembered(c);
                }
                if (imprint != null) {
                    game.getCardState(source).addImprintedCard(movedCard);
                }

                triggerList.put(originZone.getZoneType(), movedCard.getZone().getZoneType(), movedCard);

                if (c.getMeldedWith() != null) {
                    Card meld = game.getCardState(c.getMeldedWith(), null);
                    if (meld != null) {
                        triggerList.put(originZone.getZoneType(), movedCard.getZone().getZoneType(), meld);
                    }
                }
                if (c.hasMergedCard()) {
                    for (final Card cm : c.getMergedCards()) {
                        if (cm == c) continue;
                        triggerList.put(originZone.getZoneType(), movedCard.getZone().getZoneType(), cm);
                    }
                }
            }
        }

        triggerList.triggerChangesZoneAll(game, sa);

        if (sa.hasParam("Duration")) {
            addUntilCommand(sa, untilHostLeavesPlayCommand(triggerList, sa));
        }

        // if Shuffle parameter exists, and any amount of cards were owned by
        // that player, then shuffle that library
        if (sa.hasParam("Shuffle")) {
            for (Player p : game.getPlayers()) {
                if (Iterables.any(cards, CardPredicates.isOwner(p))) {
                    p.shuffle(sa);
                }
            }
        }
    }
}
