package forge.game.ability.effects;

import java.util.Collections;
import java.util.Map;

import com.google.common.collect.Maps;

import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardZoneTable;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.util.Localizer;

public class DigMultipleEffect extends SpellAbilityEffect {

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Player player = sa.getActivatingPlayer();
        final Game game = player.getGame();
        int digNum = AbilityUtils.calculateAmount(host, sa.getParam("DigNum"), sa);

        final ZoneType srcZone = sa.hasParam("SourceZone") ? ZoneType.smartValueOf(sa.getParam("SourceZone")) : ZoneType.Library;

        final ZoneType destZone1 = sa.hasParam("DestinationZone") ? ZoneType.smartValueOf(sa.getParam("DestinationZone")) : ZoneType.Hand;
        final ZoneType destZone2 = sa.hasParam("DestinationZone2") ? ZoneType.smartValueOf(sa.getParam("DestinationZone2")) : ZoneType.Library;
        int libraryPosition = sa.hasParam("LibraryPosition") ? Integer.parseInt(sa.getParam("LibraryPosition")) : -1;
        final int libraryPosition2 = sa.hasParam("LibraryPosition2") ? Integer.parseInt(sa.getParam("LibraryPosition2")) : -1;

        String changeValid = sa.getParamOrDefault("ChangeValid", "");
        boolean chooseOptional = sa.hasParam("Optional");

        CardZoneTable table = new CardZoneTable();
        for (final Player chooser : getDefinedPlayersOrTargeted(sa)) {
            if (!chooser.isInGame()) {
                continue;
            }
            final CardCollection top = new CardCollection();
            final CardCollection rest = new CardCollection();
            final PlayerZone sourceZone = chooser.getZone(srcZone);

            int numToDig = Math.min(digNum, sourceZone.size());
            for (int i = 0; i < numToDig; i++) {
                top.add(sourceZone.get(i));
            }

            if (top.isEmpty()) {
                continue;
            }

            rest.addAll(top);

            if (sa.hasParam("Reveal")) {
                game.getAction().reveal(top, chooser, false);
            } else {
                // reveal cards first
                game.getAction().revealTo(top, chooser);
            }

            Map<String, CardCollection> validMap = Maps.newHashMap();

            for (final String valid : changeValid.split(",")) {
                CardCollection list = CardLists.getValidCards(top, valid, host.getController(), host, sa);
                if (!list.isEmpty()) {
                    validMap.put(valid, list);
                }
            }

            if (validMap.isEmpty()) {
                chooser.getController().notifyOfValue(sa, null, Localizer.getInstance().getMessage("lblNoValidCards"));
            } else {
                CardCollection chosen;
                //ensure choosing something when possible and not optional
                while (true) {
                    chosen = chooser.getController().chooseCardsForEffectMultiple(validMap, sa,
                            Localizer.getInstance().getMessage("lblChooseCards"), chooseOptional);

                    if (!chosen.isEmpty()) {
                        game.getAction().reveal(chosen, chooser, true,
                                Localizer.getInstance().getMessage("lblPlayerPickedCardFrom", chooser.getName()));
                        break;
                    }
                    if (chooseOptional) break;
                    chooser.getController().notifyOfValue(sa, null,
                            Localizer.getInstance().getMessage("lblMustChoose"));
                }

                if (sa.hasParam("ChooseAmount") || sa.hasParam("ChosenZone")) {
                    int amount = AbilityUtils.calculateAmount(host, sa.getParamOrDefault("ChooseAmount", "1"), sa);
                    final ZoneType chosenZone = sa.hasParam("ChosenZone") ? ZoneType.smartValueOf(sa.getParam("ChosenZone")) : ZoneType.Battlefield;

                    CardCollectionView extraChosen = chooser.getController().chooseCardsForEffect(chosen, sa, Localizer.getInstance().getMessage("lblChooseCards"), amount, amount, false, null);
                    if (!extraChosen.isEmpty()) {
                        game.getAction().reveal(extraChosen, chooser, true, Localizer.getInstance().getMessage("lblPlayerPickedCardFrom", chooser.getName()));
                    }

                    for (Card c : extraChosen) {
                        final ZoneType origin = c.getZone().getZoneType();
                        final PlayerZone zone = c.getOwner().getZone(chosenZone);
                        chosen.remove(c);
                        rest.remove(c);
                        c = game.getAction().moveTo(zone, c, sa);
                        if (!origin.equals(c.getZone().getZoneType())) {
                            table.put(origin, c.getZone().getZoneType(), c);
                        }
                    }
                }

                for (Card c : chosen) {
                    final ZoneType origin = c.getZone().getZoneType();
                    final PlayerZone zone = c.getOwner().getZone(destZone1);

                    if (!sa.hasParam("ChangeLater")) {
                        if (zone.is(ZoneType.Library) || zone.is(ZoneType.PlanarDeck) || zone.is(ZoneType.SchemeDeck)) {
                            c = game.getAction().moveTo(destZone1, c, libraryPosition, sa, AbilityKey.newMap());
                        } else {
                            if (destZone1.equals(ZoneType.Battlefield)) {
                                if (sa.hasParam("Tapped")) {
                                    c.setTapped(true);
                                }
                            }
                            c = game.getAction().moveTo(zone, c, sa);
                        }
                        if (!origin.equals(c.getZone().getZoneType())) {
                            table.put(origin, c.getZone().getZoneType(), c);
                        }
                    }

                    if (sa.hasParam("ExileFaceDown")) {
                        c.turnFaceDown(true);
                    }
                    if (sa.hasParam("Imprint")) {
                        host.addImprintedCard(c);
                    }
                    if (sa.hasParam("ForgetOtherRemembered")) {
                        host.clearRemembered();
                    }
                    if (sa.hasParam("RememberChanged")) {
                        host.addRemembered(c);
                    }
                    rest.remove(c);
                }
            }

            // now, move the rest to destZone2
            if (!sa.hasParam("ChangeLater")) {
                if (destZone2 == ZoneType.Library || destZone2 == ZoneType.PlanarDeck
                        || destZone2 == ZoneType.SchemeDeck || destZone2 == ZoneType.Graveyard) {
                    CardCollection afterOrder = rest;
                    if (sa.hasParam("RestRandomOrder")) {
                        CardLists.shuffle(afterOrder);
                    }
                    if (libraryPosition2 != -1) {
                        // Closest to top
                        Collections.reverse(afterOrder);
                    }
                    for (final Card c : afterOrder) {
                        final ZoneType origin = c.getZone().getZoneType();
                        Card m = game.getAction().moveTo(destZone2, c, libraryPosition2, sa, AbilityKey.newMap());
                        if (m != null && !origin.equals(m.getZone().getZoneType())) {
                            table.put(origin, m.getZone().getZoneType(), m);
                        }
                    }
                } else {
                    // just move them randomly
                    for (int i = 0; i < rest.size(); i++) {
                        Card c = rest.get(i);
                        final ZoneType origin = c.getZone().getZoneType();
                        final PlayerZone toZone = c.getOwner().getZone(destZone2);
                        c = game.getAction().moveTo(toZone, c, sa);
                        if (!origin.equals(c.getZone().getZoneType())) {
                            table.put(origin, c.getZone().getZoneType(), c);
                        }
                    }
                }
            }
            if (sa.hasParam("ImprintRest")) {
                host.addImprintedCards(rest);
            }
        }
        //table trigger there
        table.triggerChangesZoneAll(game, sa);
    }

}
