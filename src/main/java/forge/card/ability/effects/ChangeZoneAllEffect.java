package forge.card.ability.effects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.google.common.collect.Iterables;

import forge.Card;
import forge.CardCharacteristicName;
import forge.CardLists;
import forge.CardPredicates;
import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellAbilityEffect;
import forge.card.spellability.SpellAbility;
import forge.game.Game;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

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
        final ZoneType destination = ZoneType.smartValueOf(sa.getParam("Destination"));
        final List<ZoneType> origin = ZoneType.listValueOf(sa.getParam("Origin"));
        
        if(origin.contains(ZoneType.Library) && sa.hasParam("Search")) {
            sa.getActivatingPlayer().incLibrarySearched();
        }

        List<Card> cards = new ArrayList<Card>();

        List<Player> tgtPlayers = getTargetPlayers(sa);
        final Game game = sa.getActivatingPlayer().getGame();

        if ((tgtPlayers == null) || tgtPlayers.isEmpty() || sa.hasParam("UseAllOriginZones")) {
            cards = game.getCardsIn(origin);
        } else if (origin.contains(ZoneType.Library) && sa.hasParam("Search")) {
            // Search library using changezoneall effect need a param "Search"
            if (sa.getActivatingPlayer().hasKeyword("LimitSearchLibrary")) {
                for (final Player p : tgtPlayers) {
                    cards.addAll(p.getCardsIn(origin));
                    cards.removeAll(p.getCardsIn(ZoneType.Library));
                    int fetchNum = Math.min(p.getCardsIn(ZoneType.Library).size(), 4);
                    cards.addAll(p.getCardsIn(ZoneType.Library, fetchNum));
                }
            }
            if (sa.getActivatingPlayer().hasKeyword("CantSearchLibrary")) {
                // all these cards have "then that player shuffles", mandatory shuffle
                cards.removeAll(game.getCardsIn(ZoneType.Library));
            }
        } else {
            for (final Player p : tgtPlayers) {
                cards.addAll(p.getCardsIn(origin));
            }
        }
        
        if (origin.contains(ZoneType.Library) && sa.hasParam("Search") && !sa.getActivatingPlayer().hasKeyword("CantSearchLibrary")) {
            List<Card> libCards = CardLists.getValidCards(cards, "Card.inZoneLibrary", sa.getActivatingPlayer(), sa.getSourceCard());
            List<Card> libCardsYouOwn = CardLists.filterControlledBy(libCards, sa.getActivatingPlayer());
            if (!libCardsYouOwn.isEmpty()) { // Only searching one's own library would fire Archive Trap's altcost
                sa.getActivatingPlayer().incLibrarySearched();
            }
            sa.getActivatingPlayer().getController().reveal("Looking at the Library", libCards, ZoneType.Library, sa.getActivatingPlayer());
        }
        cards = AbilityUtils.filterListByType(cards, sa.getParam("ChangeType"), sa);

        if (sa.hasParam("ForgetOtherRemembered")) {
            sa.getSourceCard().clearRemembered();
        }

        final String remember = sa.getParam("RememberChanged");
        final String forget = sa.getParam("ForgetChanged");
        final String imprint = sa.getParam("Imprint");
        final boolean random = sa.hasParam("RandomOrder");

        final int libraryPos = sa.hasParam("LibraryPosition") ? Integer.parseInt(sa.getParam("LibraryPosition")) : 0;

        if (destination.equals(ZoneType.Library) && !sa.hasParam("Shuffle") && cards.size() >= 2 && !random) {
            cards = sa.getActivatingPlayer().getController().orderMoveToZoneList(cards, destination);
        }

        if (destination.equals(ZoneType.Library) && random) {
            final Random ran = MyRandom.getRandom();
            Collections.shuffle(cards, ran);
        }

        for (final Card c : cards) {
            if (destination.equals(ZoneType.Battlefield)) {
                // Auras without Candidates stay in their current location
                if (c.isAura()) {
                    final SpellAbility saAura = AttachEffect.getAttachSpellAbility(c);
                    if (!saAura.getTargetRestrictions().hasCandidates(saAura, false)) {
                        continue;
                    }
                }
                if (sa.hasParam("Tapped")) {
                    c.setTapped(true);
                }
            }

            if (sa.hasParam("GainControl")) {
                c.setController(sa.getActivatingPlayer(), game.getNextTimestamp());
                game.getAction().moveToPlay(c, sa.getActivatingPlayer());
            } else {
                final Card movedCard = game.getAction().moveTo(destination, c, libraryPos);
                if (sa.hasParam("ExileFaceDown")) {
                    movedCard.setState(CardCharacteristicName.FaceDown);
                }
                if (sa.hasParam("Tapped")) {
                    movedCard.setTapped(true);
                }
            }

            if (remember != null) {
                game.getCardState(sa.getSourceCard()).addRemembered(c);
            }
            if (forget != null) {
                game.getCardState(sa.getSourceCard()).removeRemembered(c);
            }
            if (imprint != null) {
                game.getCardState(sa.getSourceCard()).addImprinted(c);
            }
        }

        // if Shuffle parameter exists, and any amount of cards were owned by
        // that player, then shuffle that library
        if (sa.hasParam("Shuffle")) {
            for (Player p : game.getPlayers()) {
                if (Iterables.any(cards, CardPredicates.isOwner(p))) {
                    p.shuffle();
                }
            }
        }
    }

}
