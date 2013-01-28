package forge.card.abilityfactory.effects;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Iterables;

import forge.Card;
import forge.CardCharacteristicName;
import forge.CardPredicates;
import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.SpellAbility;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;

public class ChangeZoneAllEffect extends SpellEffect {


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

        List<Card> cards = new ArrayList<Card>();

        List<Player> tgtPlayers = getTargetPlayers(sa);

        if ((tgtPlayers == null) || tgtPlayers.isEmpty() || sa.hasParam("UseAllOriginZones")) {
            cards = Singletons.getModel().getGame().getCardsIn(origin);
        } else {
            for (final Player p : tgtPlayers) {
                cards.addAll(p.getCardsIn(origin));
            }
        }

        cards = AbilityFactory.filterListByType(cards, sa.getParam("ChangeType"), sa);

        if (sa.hasParam("ForgetOtherRemembered")) {
            sa.getSourceCard().clearRemembered();
        }

        final String remember = sa.getParam("RememberChanged");

        final int libraryPos = sa.hasParam("LibraryPosition") ? Integer.parseInt(sa.getParam("LibraryPosition")) : 0;

        if (sa.getActivatingPlayer().isHuman() && destination.equals(ZoneType.Library) && !sa.hasParam("Shuffle")
                && cards.size() >= 2) {
            cards = GuiChoose.order("Choose order of cards to put into the library", "Put first", 0, cards, null, null);
        }

        for (final Card c : cards) {
            if (destination.equals(ZoneType.Battlefield)) {
                // Auras without Candidates stay in their current location
                if (c.isAura()) {
                    final SpellAbility saAura = AttachEffect.getAttachSpellAbility(c);
                    if (!saAura.getTarget().hasCandidates(saAura, false)) {
                        continue;
                    }
                }
                if (sa.hasParam("Tapped")) {
                    c.setTapped(true);
                }
            }

            if (sa.hasParam("GainControl")) {
                c.addController(sa.getSourceCard());
                Singletons.getModel().getGame().getAction().moveToPlay(c, sa.getActivatingPlayer());
            } else {
                final Card movedCard = Singletons.getModel().getGame().getAction().moveTo(destination, c, libraryPos);
                if (sa.hasParam("ExileFaceDown")) {
                    movedCard.setState(CardCharacteristicName.FaceDown);
                }
                if (sa.hasParam("Tapped")) {
                    movedCard.setTapped(true);
                }
            }

            if (remember != null) {
                Singletons.getModel().getGame().getCardState(sa.getSourceCard()).addRemembered(c);
            }
        }

        // if Shuffle parameter exists, and any amount of cards were owned by
        // that player, then shuffle that library
        if (sa.hasParam("Shuffle")) {
            for (Player p : Singletons.getModel().getGame().getPlayers()) {
                if (Iterables.any(cards, CardPredicates.isOwner(p))) {
                    p.shuffle();
                }
            }
        }
    }

}
