package forge.card.abilityfactory.effects;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Iterables;

import forge.Card;
import forge.CardCharacteristicName;
import forge.CardPredicates;
import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.AbilityFactoryAttach;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class ChangeZoneAllEffect extends SpellEffect {
    
    
    /**
     * <p>
     * changeZoneAllStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    @Override
    protected String getStackDescription(java.util.Map<String,String> params, SpellAbility sa) {
        // TODO build Stack Description will need expansion as more cards are added

        final String[] desc = sa.getDescription().split(":");

        if (desc.length > 1) {
            return desc[1];
        } else {
            return desc[0];
        }
    }

    /**
     * <p>
     * changeZoneAllResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    @Override
    public void resolve(java.util.Map<String,String> params, SpellAbility sa) {
        final ZoneType destination = ZoneType.smartValueOf(params.get("Destination"));
        final List<ZoneType> origin = ZoneType.listValueOf(params.get("Origin"));

        List<Card> cards = null;

        ArrayList<Player> tgtPlayers = null;

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else if (params.containsKey("Defined")) {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        if ((tgtPlayers == null) || tgtPlayers.isEmpty()) {
            cards = Singletons.getModel().getGame().getCardsIn(origin);
        } else {
            cards = tgtPlayers.get(0).getCardsIn(origin);
        }

        cards = AbilityFactory.filterListByType(cards, params.get("ChangeType"), sa);

        if (params.containsKey("ForgetOtherRemembered")) {
            sa.getSourceCard().clearRemembered();
        }

        final String remember = params.get("RememberChanged");

        // I don't know if library position is necessary. It's here if it is,
        // just in case
        final int libraryPos = params.containsKey("LibraryPosition") ? Integer.parseInt(params.get("LibraryPosition"))
                : 0;
        for (final Card c : cards) {
            if (destination.equals(ZoneType.Battlefield)) {
                // Auras without Candidates stay in their current location
                if (c.isAura()) {
                    final SpellAbility saAura = AbilityFactoryAttach.getAttachSpellAbility(c);
                    if (!saAura.getTarget().hasCandidates(saAura, false)) {
                        continue;
                    }
                }
                if (params.containsKey("Tapped")) {
                    c.setTapped(true);
                }
            }

            if (params.containsKey("GainControl")) {
                c.addController(sa.getSourceCard());
                Singletons.getModel().getGame().getAction().moveToPlay(c, sa.getActivatingPlayer());
            } else {
                final Card movedCard = Singletons.getModel().getGame().getAction().moveTo(destination, c, libraryPos);
                if (params.containsKey("ExileFaceDown")) {
                    movedCard.setState(CardCharacteristicName.FaceDown);
                }
                if (params.containsKey("Tapped")) {
                    movedCard.setTapped(true);
                }
            }

            if (remember != null) {
                Singletons.getModel().getGame().getCardState(sa.getSourceCard()).addRemembered(c);
            }
        }

        // if Shuffle parameter exists, and any amount of cards were owned by
        // that player, then shuffle that library
        if (params.containsKey("Shuffle")) {
            for( Player p : Singletons.getModel().getGame().getPlayers()) {
                if (Iterables.any(cards, CardPredicates.isOwner(p))) {
                    p.shuffle();
                }
            }
        }
    }

}